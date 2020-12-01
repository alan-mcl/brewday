/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.*;

/**
 *
 */
public class Boil extends ProcessStep
{
	/** boil duration */
	private TimeUnit duration;

	private String inputWortVolume;
	private String outputWortVolume;
	private String outputTrubVolume;

	// calculated
	private TimeUnit timeToBoil;

	/** should this step remove the equipment profile trub & chiller loss? */
	private boolean removeTrubAndChillerLoss;


	/*-------------------------------------------------------------------------*/
	public Boil()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Boil(
		String name,
		String description,
		String inputWortVolume,
		String outputWortVolume,
		String outputTrubVolume,
		List<IngredientAddition> ingredientAdditions,
		TimeUnit duration,
		boolean removeTrubAndChillerLoss)
	{
		super(name, description, Type.BOIL);
		this.inputWortVolume = inputWortVolume;
		this.outputWortVolume = outputWortVolume;
		this.outputTrubVolume = outputTrubVolume;
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
		setIngredients(ingredientAdditions);
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public Boil(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.BOIL), StringUtils.getProcessString("boil.desc"), Type.BOIL);

		// todo: find last wort vol?
		this.inputWortVolume = recipe.getVolumes().getVolumeByType(Volume.Type.WORT);
		this.outputWortVolume = StringUtils.getProcessString("boil.output", getName());
		this.outputTrubVolume = StringUtils.getProcessString("boil.output.trub", getName());
		this.duration = new TimeUnit(60, Quantity.Unit.MINUTES, false);
		this.removeTrubAndChillerLoss = false;
	}

	/*-------------------------------------------------------------------------*/
	public Boil(Boil other)
	{
		super(other.getName(), other.getDescription(), Type.BOIL);

		this.inputWortVolume = other.inputWortVolume;
		this.outputWortVolume = other.outputWortVolume;
		this.outputTrubVolume = other.outputTrubVolume;
		this.duration = other.duration;
		this.removeTrubAndChillerLoss = other.removeTrubAndChillerLoss;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		if (equipmentProfile == null)
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile", equipmentProfile));
			return;
		}

		// Boil step supports being the first one, for e.g. during an extract batch
		Volume inputVolume = null;

		if (inputWortVolume != null)
		{
			inputVolume = volumes.getVolume(inputWortVolume);
		}
		else
		{
			// fake it and let the water additions save us

			inputVolume = new Volume("water volume",
				Volume.Type.WORT,
				new VolumeUnit(0),
				new TemperatureUnit(20, Quantity.Unit.CELSIUS),
				new DensityUnit(1.000, Quantity.Unit.SPECIFIC_GRAVITY),
				new DensityUnit(1.000, Quantity.Unit.SPECIFIC_GRAVITY),
				new PercentageUnit(0),
				new ColourUnit(0, Quantity.Unit.SRM),
				new BitternessUnit(0, Quantity.Unit.IBU));
		}

		boolean foundWaterAddition = false;
		// collect up water additions
		for (WaterAddition ia : getWaterAdditions())
		{
			foundWaterAddition = true;
			inputVolume = Equations.dilute(inputVolume, ia, inputVolume.getName());
		}

		// if this is the first step in the recipe then we must have a water addition
		if (inputWortVolume==null && !foundWaterAddition)
		{
			log.addError(StringUtils.getProcessString("boil.no.water.additions"));
			return;
		}

		// check for boilover risk
		if (inputVolume.getVolume().get(Quantity.Unit.MILLILITRES) * 1.2D >=
			equipmentProfile.getBoilKettleVolume().get(Quantity.Unit.MILLILITRES))
		{
			log.addWarning(
				StringUtils.getProcessString("boil.kettle.too.small",
					equipmentProfile.getBoilKettleVolume().get(Quantity.Unit.LITRES),
					inputVolume.getVolume().get(Quantity.Unit.LITRES)));
		}

		// gather up hop charges
		List<HopAddition> hopCharges = new ArrayList<>(getHopAdditions());

		for (IngredientAddition ia : inputVolume.getIngredientAdditions(IngredientAddition.Type.HOPS))
		{
			if (ia instanceof HopAddition)
			{
				// These are probably FWH, treat them as if they are present at
				// the start of the boil too.
				HopAddition ha = new HopAddition(
					((HopAddition)ia).getHop(),
					((HopAddition)ia).getForm(),
					ia.getQuantity(),
					ia.getUnit(),
					this.getDuration());

				ha.setBoiledTime(new TimeUnit(((HopAddition)ia).getBoiledTime()));

				hopCharges.add(ha);
			}
		}

		DensityUnit gravityIn = inputVolume.getGravity();
		ColourUnit colourIn = inputVolume.getColour();
		BitternessUnit bitternessIn = inputVolume.getBitterness();
		if (bitternessIn == null)
		{
			bitternessIn = new BitternessUnit(0, Quantity.Unit.IBU);
		}

		// gather up fermentable additions and add their contributions
		for (FermentableAddition fa : getFermentableAdditions())
		{
			// ignore GRAIN and ADJUNCT additions
			if (fa.getFermentable().getType() == Fermentable.Type.JUICE ||
				fa.getFermentable().getType() == Fermentable.Type.SUGAR ||
				fa.getFermentable().getType() == Fermentable.Type.LIQUID_EXTRACT ||
				fa.getFermentable().getType() == Fermentable.Type.DRY_EXTRACT)
			{
				// gravity impact
				DensityUnit gravity = Equations.calcSteepedFermentableAdditionGravity(fa, inputVolume.getVolume());
				gravityIn = new DensityUnit(gravityIn.get() + gravity.get());

				// colour impact
				ColourUnit col = Equations.calcSolubleFermentableAdditionColourContribution(fa, inputVolume.getVolume());
				colourIn = new ColourUnit(colourIn.get() + col.get());

				// bitterness impact
				BitternessUnit ibu = Equations.calcSolubleFermentableAdditionBitternessContribution(fa, inputVolume.getVolume());
				bitternessIn = new BitternessUnit(bitternessIn.get() + ibu.get());

				log.addMessage(StringUtils.getProcessString("boil.fermentable.gravity",
					fa.getFermentable().getName(),
					gravity.get(Quantity.Unit.GU),
					col.get(Quantity.Unit.SRM),
					ibu.get(Quantity.Unit.IBU)));
			}
		}

		//
		// Output volume construction
		//

		// Boil step will exit at 100C
		TemperatureUnit tempOut = new TemperatureUnit(100D, Quantity.Unit.CELSIUS, false);

		// Volume out (ignoring trub & chiller loss because we need to include it in
		// other calculations below)
		double boilEvapourationRatePerHour = equipmentProfile.getBoilEvapourationRate().get();

		double boiledOff = inputVolume.getVolume().get(Quantity.Unit.MILLILITRES) *
			boilEvapourationRatePerHour * (duration.get(Quantity.Unit.MINUTES)/60D);

		log.addMessage(StringUtils.getProcessString("boil.boil.off.vol", boiledOff/1000D));

		VolumeUnit volumeOut = new VolumeUnit(inputVolume.getVolume().get(Quantity.Unit.MILLILITRES) - boiledOff);

		// Gravity out
		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			inputVolume.getVolume(), gravityIn, volumeOut);

		// ABV out, if for some reason you are boiling beer.
		// This isn't really correct but we are notrunning a distilling simulation
		// here so let's just roll with this.
		PercentageUnit abvOut = Equations.calcAbvWithVolumeChange(
			inputVolume.getVolume(), inputVolume.getAbv(), volumeOut);

		// colour changes
		ColourUnit colourOut = Equations.calcColourAfterBoil(colourIn);
		colourOut = Equations.calcColourWithVolumeChange(
			inputVolume.getVolume(), colourOut, volumeOut);

		// Bitterness out
		BitternessUnit bitternessOut = new BitternessUnit(bitternessIn);
		for (HopAddition hopCharge : hopCharges)
		{
			BitternessUnit hopAdditionIbu = getHopAdditionIBU(
				equipmentProfile,
				inputVolume,
				gravityIn,
				volumeOut,
				gravityOut,
				hopCharge);

			log.addMessage(StringUtils.getProcessString("boil.hop.charge.ibu",
				hopCharge.getName(), hopAdditionIbu.get(Quantity.Unit.IBU)));

			bitternessOut.add(hopAdditionIbu);
		}

		// Finally, remove trub & chiller loss here if need be
		if (removeTrubAndChillerLoss)
		{
			volumeOut = new VolumeUnit(volumeOut.get() - equipmentProfile.getTrubAndChillerLoss().get());
		}

		//
		// Create the output wort volume
		//
		Volume postBoilOut = new Volume(
			outputWortVolume,
			inputVolume.getType(),
			volumeOut,
			tempOut,
			inputVolume.getFermentability(),
			gravityOut,
			abvOut,
			colourOut,
			bitternessOut);
		volumes.addOrUpdateVolume(outputWortVolume, postBoilOut);

		List<HopAddition> hopsInVolume;

		//
		// If necessary create the output trub volume
		//
		ArrayList<IngredientAddition> ingredientAdditions = new ArrayList<>(inputVolume.getIngredientAdditions());
		ingredientAdditions.addAll(this.getIngredientAdditions());
		if (removeTrubAndChillerLoss)
		{
			Volume trubOut = new Volume(
				outputTrubVolume,
				inputVolume.getType(),
				new VolumeUnit(equipmentProfile.getTrubAndChillerLoss()),
				new TemperatureUnit(tempOut),
				new PercentageUnit(inputVolume.getFermentability()),
				new DensityUnit(gravityIn),
				new PercentageUnit(abvOut),
				new ColourUnit(colourOut),
				new BitternessUnit(bitternessOut));

			// assume that all ingredients remain in the trub
			trubOut.setIngredientAdditions(ingredientAdditions);
			hopsInVolume = new ArrayList(trubOut.getIngredientAdditions(IngredientAddition.Type.HOPS));
			volumes.addOrUpdateVolume(outputTrubVolume, trubOut);
		}
		else
		{
			// assume that all ingredients stay in the wort for now
			postBoilOut.setIngredientAdditions(ingredientAdditions);
			hopsInVolume = new ArrayList(postBoilOut.getIngredientAdditions(IngredientAddition.Type.HOPS));
		}

		for (HopAddition ha : hopsInVolume)
		{
			ha.setBoiledTime(new TimeUnit(ha.getBoiledTime().get() + ha.getTime().get()));
		}

		// calculated fields
		timeToBoil = Equations.calcHeatingTime(
			inputVolume.getVolume(),
			inputVolume.getTemperature(),
			new TemperatureUnit(100, Quantity.Unit.CELSIUS),
			equipmentProfile.getBoilElementPower());
	}

	/*-------------------------------------------------------------------------*/
	protected BitternessUnit getHopAdditionIBU(
		EquipmentProfile equipmentProfile,
		Volume inputVolume,
		DensityUnit gravityIn,
		VolumeUnit volumeOut,
		DensityUnit gravityOut,
		HopAddition hopCharge)
	{
		VolumeUnit trubAndChillerLoss = equipmentProfile.getTrubAndChillerLoss();

		Settings.HopBitternessFormula hopBitternessFormula =
			Settings.HopBitternessFormula.valueOf(
				Database.getInstance().getSettings().get(
					Settings.HOP_BITTERNESS_FORMULA));

		BitternessUnit hopAdditionIbu;

		//
		// Note that the 'reduce the contribution if already boiled' steps are not
		// really accurate since we do not know the gravity/volume conditions of
		// the earlier boil(s). Some kind of tracking of utilisation rather than
		// just boiled time might be better, but let's just roll with this for now.
		//

		switch (hopBitternessFormula)
		{
			case TINSETH_BEERSMITH:
				// Tinseth's equation is based on the "volume of finished beer"
				// BeerSmith interprets this as "Pre boil vol - trub&chiller loss"
				// which is franky odd. And it also uses the pre-boil gravity, instead
				// of the average wort gravity.

				// see http://www.beersmith.com/forum/index.php/topic,21613.0.html
				VolumeUnit tinsethVolume = new VolumeUnit(inputVolume.getVolume().get() - trubAndChillerLoss.get());

				DensityUnit tinsethGravity = new DensityUnit(gravityIn.get());

				hopAdditionIbu = Equations.calcIbuTinseth(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					tinsethGravity,
					tinsethVolume,
					equipmentProfile.getHopUtilisation().get());

				// reduce the contribution if already boiled
				if (hopCharge.getBoiledTime().get(Quantity.Unit.MINUTES) > 0)
				{
					BitternessUnit temp = Equations.calcIbuTinseth(
						(HopAddition)hopCharge,
						new TimeUnit(hopCharge.getTime().get() + hopCharge.getBoiledTime().get()),
						tinsethGravity,
						tinsethVolume,
						equipmentProfile.getHopUtilisation().get());

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;
			case TINSETH:
				// Tinseth's article is not entirely clear about which volume to
				// use, but we have word from the Prof himself:
				// "We are concerned with the mg/L and any portions of a liter lost
				// post boil doesn’t affect the calculation. Post boil volume is correct."
				// See the comments here: https://alchemyoverlord.wordpress.com/2015/05/12/a-modified-ibu-measurement-especially-for-late-hopping/

				// post-boil volume
				tinsethVolume = new VolumeUnit(volumeOut.get());
				// we assume that Prof Tinseth would have cooled this batch to 20C
				tinsethVolume = Equations.calcCoolingShrinkage(
					tinsethVolume, new TemperatureUnit(80, Quantity.Unit.CELSIUS));

				// "Use an average gravity value for the entire boil to account for changes in the wort volume"
				tinsethGravity = new DensityUnit((gravityOut.get() + gravityIn.get()) / 2);

				hopAdditionIbu = Equations.calcIbuTinseth(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					tinsethGravity,
					tinsethVolume,
					equipmentProfile.getHopUtilisation().get());

				// reduce the contribution if already boiled
				if (hopCharge.getBoiledTime().get(Quantity.Unit.MINUTES) > 0)
				{
					BitternessUnit temp = Equations.calcIbuTinseth(
						(HopAddition)hopCharge,
						new TimeUnit(hopCharge.getTime().get() + hopCharge.getBoiledTime().get()),
						tinsethGravity,
						tinsethVolume,
						equipmentProfile.getHopUtilisation().get());

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;

			case RAGER:

				// Here's another one that uses "batch volume". Let's go with the
				// same post-boil vol that Prof Tinseth suggests

				VolumeUnit ragerVol = new VolumeUnit(volumeOut.get());
				ragerVol = Equations.calcCoolingShrinkage(
					ragerVol, new TemperatureUnit(80, Quantity.Unit.CELSIUS));

				// Suggestion is that one uses the pre-boil gravity.
				// See here: https://straighttothepint.com/ibu-calculator/
				// Wish I could find Jackie Rager's original Zymurgy article to work it out.
				DensityUnit ragerGravity = new DensityUnit(gravityIn.get());

				hopAdditionIbu = Equations.calcIbuRager(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					ragerGravity,
					ragerVol,
					equipmentProfile.getHopUtilisation().get());

				// reduce the contribution if already boiled
				if (hopCharge.getBoiledTime().get(Quantity.Unit.MINUTES) > 0)
				{
					BitternessUnit temp = Equations.calcIbuRager(
						(HopAddition)hopCharge,
						new TimeUnit(hopCharge.getTime().get() + hopCharge.getBoiledTime().get()),
						ragerGravity,
						ragerVol,
						equipmentProfile.getHopUtilisation().get());

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;

			case GARETZ:

				// Even more wacky, Mark Garetz wants us to pass in the "final volume"
				// to account for concentrated extract batch brews.
				// No way to get that here so just use the post-boil vol minus losses.
				// This makes this worse for extract brews, but Garetz already
				// produces estimates on the low end so WTF YOLO

				// pre boil
				VolumeUnit boilVol = new VolumeUnit(inputVolume.getVolume().get());
				// post boil
				VolumeUnit finalVol = new VolumeUnit(
					volumeOut.get() - equipmentProfile.getTrubAndChillerLoss().get());

				DensityUnit garetzGravity = new DensityUnit(gravityIn.get());

				hopAdditionIbu = Equations.calcIbuGaretz(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					garetzGravity,
					finalVol,
					boilVol,
					equipmentProfile.getHopUtilisation().get(),
					equipmentProfile.getElevation().get(Quantity.Unit.FOOT));

				// reduce the contribution if already boiled
				if (hopCharge.getBoiledTime().get(Quantity.Unit.MINUTES) > 0)
				{
					BitternessUnit temp = Equations.calcIbuGaretz(
						(HopAddition)hopCharge,
						new TimeUnit(hopCharge.getTime().get() + hopCharge.getBoiledTime().get()),
						garetzGravity,
						finalVol,
						boilVol,
						equipmentProfile.getHopUtilisation().get(),
						equipmentProfile.getElevation().get(Quantity.Unit.FOOT));

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;

			case DANIELS:

				// Lets use the same approach to volume and gravity as Tinseth

				// post-boil volume
				tinsethVolume = new VolumeUnit(volumeOut.get());
				// we assume that Prof Tinseth would have cooled this batch to 20C
				tinsethVolume = Equations.calcCoolingShrinkage(
					tinsethVolume, new TemperatureUnit(80, Quantity.Unit.CELSIUS));

				// "Use an average gravity value for the entire boil to account for changes in the wort volume"
				tinsethGravity = new DensityUnit((gravityOut.get() + gravityIn.get()) / 2);

				hopAdditionIbu = Equations.calcIbuDaniels(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					tinsethGravity,
					tinsethVolume,
					equipmentProfile.getHopUtilisation().get());

				// reduce the contribution if already boiled
				if (hopCharge.getBoiledTime().get(Quantity.Unit.MINUTES) > 0)
				{
					BitternessUnit temp = Equations.calcIbuRager(
						(HopAddition)hopCharge,
						new TimeUnit(hopCharge.getTime().get() + hopCharge.getBoiledTime().get()),
						tinsethGravity,
						tinsethVolume,
						equipmentProfile.getHopUtilisation().get());

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;

			default:
				throw new BrewdayException("invalid: "+hopBitternessFormula);
		}
		return hopAdditionIbu;
	}

	/*-------------------------------------------------------------------------*/
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		if (inputWortVolume!=null && !volumes.contains(inputWortVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputWortVolume));
			return false;
		}
		return true;
	}

	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		if (!validateInputVolumes(recipe.getVolumes(), log))
		{
			return;
		}

		recipe.getVolumes().addVolume(outputWortVolume, new Volume(Volume.Type.WORT));
		if (removeTrubAndChillerLoss)
		{
			recipe.getVolumes().addVolume(outputTrubVolume, new Volume(Volume.Type.WORT));
		}
	}

	@Override
	protected void sortIngredients()
	{
		// sort ascending by time
		getIngredientAdditions().sort((o1, o2) -> (int)(o2.getTime().get() - o1.getTime().get()));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("boil.step.desc", duration.get(Quantity.Unit.MINUTES));
	}

	/*-------------------------------------------------------------------------*/
	public String getInputWortVolume()
	{
		return inputWortVolume;
	}

	public String getOutputWortVolume()
	{
		return outputWortVolume;
	}

	public String getOutputTrubVolume()
	{
		return outputTrubVolume;
	}

	public TimeUnit getDuration()
	{
		return duration;
	}

	public void setDuration(TimeUnit duration)
	{
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public void setInputWortVolume(String inputWortVolume)
	{
		this.inputWortVolume = inputWortVolume;
	}

	/*-------------------------------------------------------------------------*/
	public void setOutputWortVolume(String outputWortVolume)
	{
		this.outputWortVolume = outputWortVolume;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Collection<String> getInputVolumes()
	{
		return inputWortVolume==null?Collections.emptyList():Collections.singletonList(inputWortVolume);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Collection<String> getOutputVolumes()
	{
		return outputWortVolume==null?Collections.emptyList():Arrays.asList(outputWortVolume, outputTrubVolume);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(
			IngredientAddition.Type.FERMENTABLES,
			IngredientAddition.Type.HOPS,
			IngredientAddition.Type.MISC,
			IngredientAddition.Type.WATER);
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		Volume preBoilVol = getRecipe().getVolumes().getVolume(this.getInputWortVolume());
		result.add(StringUtils.getDocString(
			"boil.pre.boil",
			preBoilVol.getName(),
			preBoilVol.getVolume().describe(Quantity.Unit.LITRES),
			preBoilVol.getGravity().describe(Quantity.Unit.SPECIFIC_GRAVITY)));

		result.add(StringUtils.getDocString("boil.duration", this.duration.describe(Quantity.Unit.MINUTES)));

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.HOPS || ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"boil.hop.addition",
						ia.describe(),
						ia.getTime().describe(Quantity.Unit.MINUTES)));
			}
			else if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add(
					StringUtils.getDocString(
						"boil.fermentable.addition",
						ia.describe(),
						ia.getTime().describe(Quantity.Unit.MINUTES)));
			}
			else
			{
				throw new BrewdayException("invalid "+ia.getType());
			}
		}

		Volume postBoilVol = getRecipe().getVolumes().getVolume(this.getOutputWortVolume());
		result.add(StringUtils.getDocString(
			"boil.post.boil",
			postBoilVol.getVolume().describe(Quantity.Unit.LITRES),
			postBoilVol.getGravity().describe(Quantity.Unit.SPECIFIC_GRAVITY)));

		return result;
	}

	@Override
	public ProcessStep clone()
	{
		return new Boil(
			this.getName(),
			this.getDescription(),
			this.getInputWortVolume(),
			this.getOutputWortVolume(),
			this.getOutputTrubVolume(),
			cloneIngredients(this.getIngredientAdditions()),
			new TimeUnit(this.getDuration().get()),
			this.removeTrubAndChillerLoss);
	}

	/*-------------------------------------------------------------------------*/

	public TimeUnit getTimeToBoil()
	{
		return timeToBoil;
	}

	/*-------------------------------------------------------------------------*/

	public boolean isRemoveTrubAndChillerLoss()
	{
		return removeTrubAndChillerLoss;
	}

	public void setRemoveTrubAndChillerLoss(boolean removeTrubAndChillerLoss)
	{
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
	}
}
