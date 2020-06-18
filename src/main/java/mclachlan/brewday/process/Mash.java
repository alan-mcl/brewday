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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.*;

import static mclachlan.brewday.math.Quantity.Unit.*;

public class Mash extends ProcessStep
{
	private String outputFirstRunnings;
	private String outputMashVolume;

	/** duration of mash */
	private TimeUnit duration;

	/** grain volume temp */
	private TemperatureUnit grainTemp;

	// calculated from strike water
	private TemperatureUnit mashTemp;

	/*-------------------------------------------------------------------------*/
	public Mash()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Mash(
		String name,
		String description,
		List<IngredientAddition> mashAdditions,
		String outputMashVolume,
		String outputFirstRunnings,
		TimeUnit duration,
		TemperatureUnit grainTemp)
	{
		super(name, description, Type.MASH);
		this.outputFirstRunnings = outputFirstRunnings;
		setIngredients(mashAdditions);

		this.outputMashVolume = outputMashVolume;
		this.duration = duration;
		this.grainTemp = grainTemp;
	}

	/*-------------------------------------------------------------------------*/
	public Mash(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.MASH), StringUtils.getProcessString("mash.desc"), Type.MASH);

		duration = new TimeUnit(60, MINUTES, false);
		grainTemp = new TemperatureUnit(20);

		outputMashVolume = StringUtils.getProcessString("mash.mash.vol", getName());
		outputFirstRunnings = StringUtils.getProcessString("mash.first.runnings", getName());
	}

	/*-------------------------------------------------------------------------*/
	public Mash(Mash step)
	{
		super(step.getName(), step.getDescription(), Type.MASH);

		this.duration = step.getDuration();
		this.grainTemp = step.getGrainTemp();

		this.outputMashVolume = step.getOutputMashVolume();
		this.outputFirstRunnings = step.getOutputFirstRunnings();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (equipmentProfile == null)
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile", equipmentProfile));
			return;
		}

		List<IngredientAddition> grainBill = new ArrayList<>();
		List<HopAddition> hopCharges = new ArrayList<>();
		WaterAddition strikeWater = null;

		for (IngredientAddition item : getIngredients())
		{
			// seek the grains and water with the same time as the mash,
			// these are the initial combination

			if ((int)item.getTime().get(MINUTES) == (int)this.getDuration().get(MINUTES))
			{
				if (item instanceof FermentableAddition)
				{
					grainBill.add(item);
				}
				else if (item instanceof WaterAddition)
				{
					strikeWater = (WaterAddition)item;
				}
				else if (item instanceof HopAddition)
				{
					hopCharges.add((HopAddition)item);
				}
			}
		}

		if (grainBill == null || grainBill.isEmpty())
		{
			log.addError(StringUtils.getProcessString("mash.no.fermentable.addition"));
			return;
		}
		if (strikeWater == null)
		{
			log.addError(StringUtils.getProcessString("mash.no.strike.water"));
			return;
		}

		Volume mashVolumeOut = getMashVolumeOut(equipmentProfile, grainBill, strikeWater);
		volumes.addOrUpdateVolume(outputMashVolume, mashVolumeOut);

		if (mashVolumeOut.getVolume().get() *1.1 > equipmentProfile.getMashTunVolume())
		{
			log.addWarning(
					StringUtils.getProcessString("mash.mash.tun.not.large.enough",
					equipmentProfile.getMashTunVolume()/1000,
					mashVolumeOut.getVolume().get(LITRES)));
		}

		Volume firstRunningsOut = getFirstRunningsOut(mashVolumeOut, grainBill, equipmentProfile);
		volumes.addOrUpdateVolume(outputFirstRunnings, firstRunningsOut);

		// mash hops
		BitternessUnit bitterness = Equations.calcMashHopIbu(
			hopCharges,
			firstRunningsOut.getGravity(),
			firstRunningsOut.getVolume(),
			equipmentProfile.getHopUtilisation());

		mashVolumeOut.setBitterness(new BitternessUnit(bitterness));
		firstRunningsOut.setBitterness(new BitternessUnit(bitterness));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		recipe.getVolumes().addVolume(outputMashVolume, new Volume(Volume.Type.MASH));
		recipe.getVolumes().addVolume(outputFirstRunnings, new Volume(Volume.Type.WORT));
	}

	/*-------------------------------------------------------------------------*/
	private Volume getFirstRunningsOut(
		Volume mashVolume,
		List<IngredientAddition> grainBill,
		EquipmentProfile equipmentProfile)
	{
		WeightUnit grainWeight = Equations.getTotalGrainWeight(grainBill);

		WaterAddition waterAddition =
			(WaterAddition)mashVolume.getIngredientAddition(IngredientAddition.Type.WATER);

		VolumeUnit volumeOutMl = Equations.calcWortVolume(grainWeight, waterAddition.getVolume());

		// Always assume that the first running volume is estimated, despite the
		// grain and water additions being measured. We're doing this to ensure that
		// the chain of estimated quantities starts here.
		volumeOutMl.setEstimated(true);

		volumeOutMl.set(volumeOutMl.get(Quantity.Unit.MILLILITRES) - equipmentProfile.getLauterLoss());

		PercentageUnit fermentabilityOut = Equations.getWortAttenuationLimit(mashVolume.getTemperature());

		return new Volume(
			null,
			Volume.Type.WORT,
			volumeOutMl,
			mashVolume.getTemperature(),
			fermentabilityOut,
			mashVolume.getGravity(),
			new PercentageUnit(0D),
			mashVolume.getColour(),
			new BitternessUnit(0));
	}

	/*-------------------------------------------------------------------------*/
	private Volume getMashVolumeOut(
		EquipmentProfile equipmentProfile,
		List<IngredientAddition> grainBill,
		WaterAddition strikeWater)
	{
		WeightUnit grainWeight = Equations.getTotalGrainWeight(grainBill);

		mashTemp = Equations.calcMashTemp(grainWeight, strikeWater, grainTemp);

		VolumeUnit volumeOut = Equations.calcMashVolume(grainWeight, strikeWater.getVolume());
		VolumeUnit wortVolOut = Equations.calcWortVolume(grainWeight, strikeWater.getVolume());

		double mashEfficiency = equipmentProfile.getMashEfficiency();

		//
		// So currently I have a disagreement with the two gravity calculation methods
		// that I do not understand. The PPPG is consistent with BeerSmith output,
		// so I am using it currently. In theory the Yield method should return the
		// same result but it does not. I suspect some kind of difference in the
		// volume impact: the PPPG method returns the same as the Yield method when
		// the entire mash volume is the input, not the first runnings output volume.
		//

//		DensityUnit gravityOut = Equations.calcMashExtractContentFromYield(grainBill, mashEfficiency, strikeWater);
		DensityUnit gravityOut = Equations.calcMashExtractContentFromPppg(grainBill, mashEfficiency, wortVolOut);

		ColourUnit colourOut = Equations.calcColourSrmMoreyFormula(grainBill, volumeOut);

		return new Volume(
			null,
			Volume.Type.MASH,
			volumeOut,
			grainBill,
			strikeWater,
			mashTemp,
			gravityOut,
			colourOut);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("mash.step.desc", getName());
	}

	public String getOutputMashVolume()
	{
		return outputMashVolume;
	}

	public String getOutputFirstRunnings()
	{
		return outputFirstRunnings;
	}

	public void setOutputFirstRunnings(String outputFirstRunnings)
	{
		this.outputFirstRunnings = outputFirstRunnings;
	}

	public void setOutputMashVolume(String outputMashVolume)
	{
		this.outputMashVolume = outputMashVolume;
	}

	public void setMashTemp(TemperatureUnit mashTemp)
	{
		this.mashTemp = mashTemp;
	}

	public TimeUnit getDuration()
	{
		return duration;
	}

	public TemperatureUnit getGrainTemp()
	{
		return grainTemp;
	}

	public void setGrainTemp(TemperatureUnit grainTemp)
	{
		this.grainTemp = grainTemp;
	}

	public TemperatureUnit getMashTemp()
	{
		return mashTemp;
	}

	public void setDuration(TimeUnit duration)
	{
		this.duration = duration;
	}

	@Override
	public Collection<String> getInputVolumes()
	{
		return Collections.emptyList();
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		ArrayList<String> result = new ArrayList<>();

		if (outputMashVolume != null)
		{
			result.add(outputMashVolume);
		}
		if (outputFirstRunnings != null)
		{
			result.add(outputFirstRunnings);
		}

		return result;
	}

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

		for (IngredientAddition ia : getIngredientAdditions(IngredientAddition.Type.WATER))
		{
			WaterAddition wa = (WaterAddition)ia;

			result.add(
				StringUtils.getDocString(
					"mash.water.addition",
					wa.getQuantity().get(LITRES),
					wa.getName(),
					wa.getTemperature().get(Quantity.Unit.CELSIUS)));
		}

		for (IngredientAddition ia : getIngredientAdditions(IngredientAddition.Type.FERMENTABLES))
		{
			result.add(
				StringUtils.getDocString(
					"mash.fermentable.addition",
					ia.getQuantity().get(Quantity.Unit.KILOGRAMS),
					ia.getName(),
					this.grainTemp.get(Quantity.Unit.CELSIUS)));
		}

		for (IngredientAddition ia : getIngredientAdditions(IngredientAddition.Type.HOPS))
		{
			result.add(
				StringUtils.getDocString(
					"mash.hop.addition",
					ia.getQuantity().get(GRAMS),
					ia.getName(),
					ia.getTime().get(MINUTES)));
		}

		for (IngredientAddition ia : getIngredientAdditions(IngredientAddition.Type.MISC))
		{
			result.add(
				StringUtils.getDocString(
					"mash.misc.addition",
					ia.getQuantity().get(GRAMS),
					ia.getName(),
					ia.getTime().get(MINUTES)));
		}

		String outputMashVolume = this.getOutputMashVolume();
		Volume mashVol = getRecipe().getVolumes().getVolume(outputMashVolume);

		result.add(StringUtils.getDocString(
			"mash.volume",
			mashVol.getMetric(Volume.Metric.VOLUME).get(LITRES),
			mashVol.getMetric(Volume.Metric.TEMPERATURE).get(CELSIUS)));

		result.add(StringUtils.getDocString("mash.rest", this.duration.get(MINUTES)));

		return result;
	}

	@Override
	public ProcessStep clone()
	{
		return new Mash(
			this.getName(),
			this.getDescription(),
			cloneIngredients(getIngredients()),
			this.getOutputMashVolume(),
			this.getOutputFirstRunnings(),
			new TimeUnit(this.duration.get()),
			new TemperatureUnit(this.grainTemp.get()));
	}
}

