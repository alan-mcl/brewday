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
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 *
 */
public class BatchSparge extends ProcessStep
{
	private String mashVolume;
	private String wortVolume;
	private String outputCombinedWortVolume;
	private String outputMashVolume;
	private String outputSpargeRunnings;

	/*-------------------------------------------------------------------------*/
	public BatchSparge()
	{
	}

	/*-------------------------------------------------------------------------*/
	public BatchSparge(
		String name,
		String description,
		String mashVolume,
		String wortVolume,
		String outputCombinedWortVolume,
		String outputSpargeRunnings,
		String outputMashVolume,
		List<IngredientAddition> ingredients)
	{
		super(name, description, Type.BATCH_SPARGE);
		this.mashVolume = mashVolume;
		this.wortVolume = wortVolume;
		this.outputCombinedWortVolume = outputCombinedWortVolume;
		this.outputSpargeRunnings = outputSpargeRunnings;
		this.outputMashVolume = outputMashVolume;
		setIngredients(ingredients);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Constructor that sets the fields appropriately for the given batch.
	 */
	public BatchSparge(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.BATCH_SPARGE), StringUtils.getProcessString("batch.sparge.desc"), Type.BATCH_SPARGE);

		this.mashVolume = recipe.getVolumes().getVolumeByType(Volume.Type.MASH);
		this.wortVolume = recipe.getVolumes().getVolumeByType(Volume.Type.WORT);

		this.outputCombinedWortVolume = StringUtils.getProcessString("batch.sparge.combined.wort", getName());
		this.outputSpargeRunnings = StringUtils.getProcessString("batch.sparge.sparge.runnings", getName());
		this.outputMashVolume = StringUtils.getProcessString("batch.sparge.lautered.mash", getName());
	}

	/*-------------------------------------------------------------------------*/
	public BatchSparge(BatchSparge step)
	{
		super(step.getName(), step.getDescription(), Type.BATCH_SPARGE);

		this.mashVolume = step.mashVolume;
		this.wortVolume = step.wortVolume;

		this.outputMashVolume = step.outputMashVolume;
		this.outputSpargeRunnings = step.outputSpargeRunnings;
		this.outputCombinedWortVolume = step.outputCombinedWortVolume;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!volumes.contains(wortVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", wortVolume));
			return;
		}

		WaterAddition spargeWater = null;
		List<IngredientAddition> topUpGrains = new ArrayList<>();

		for (IngredientAddition item : getIngredients())
		{
			if (item instanceof WaterAddition)
			{
				spargeWater = (WaterAddition)item;
			}
			else if (item instanceof FermentableAddition)
			{
				FermentableAddition fa = (FermentableAddition)item;
				Fermentable fermentable = fa.getFermentable();
				if (fermentable.getType() == Fermentable.Type.GRAIN || fermentable.getType() == Fermentable.Type.ADJUNCT)
				{
					topUpGrains.add(fa);
				}
			}
		}

		if (spargeWater == null)
		{
			log.addError(StringUtils.getProcessString("batch.sparge.no.water.additions"));
			return;
		}

		Volume inputWort = volumes.getVolume(wortVolume);
		Volume mash = volumes.getVolume(mashVolume);

		// work out the total grist weight
		double totalGristWeight = 0;
		for (IngredientAddition f : mash.getIngredientAdditions(IngredientAddition.Type.FERMENTABLES))
		{
			totalGristWeight += ((FermentableAddition)f).getQuantity().get(Quantity.Unit.GRAMS);
		}
		DensityUnit mashExtract = mash.getGravity();
		WeightUnit grainWeight = new WeightUnit(totalGristWeight, Quantity.Unit.GRAMS, false);

		// work out the absorbed water
		VolumeUnit absorbedWater = Equations.calcAbsorbedWater(grainWeight);

		// add the dead space, because that is still left over
		VolumeUnit totalMashWater = new VolumeUnit(
			absorbedWater.get(Quantity.Unit.MILLILITRES) +
			equipmentProfile.getLauterLoss(),
			Quantity.Unit.MILLILITRES,
			false);


		// model the batch sparge as a dilution of the extract remaining

		DensityUnit spargeGravity = Equations.calcGravityWithVolumeChange(
			totalMashWater,
			mashExtract,
			new VolumeUnit(
				totalMashWater.get() + spargeWater.getVolume().get()));

		VolumeUnit volumeOut = new VolumeUnit(
			inputWort.getVolume().get(Quantity.Unit.MILLILITRES) +
			spargeWater.getVolume().get(Quantity.Unit.MILLILITRES),
			Quantity.Unit.MILLILITRES,
			inputWort.getVolume().isEstimated() || spargeWater.getVolume().isEstimated());

		// todo: account for topUpGrains gravity
		DensityUnit gravityOut = Equations.calcCombinedGravity(
			inputWort.getVolume(),
			inputWort.getGravity(),
			spargeWater.getVolume(),
			spargeGravity);

		TemperatureUnit tempOut =
			Equations.calcNewFluidTemperature(
				inputWort.getVolume(),
				inputWort.getTemperature(),
				spargeWater.getVolume(),
				spargeWater.getTemperature());

		// account for any topup grains
		ColourUnit addedColour = Equations.calcColourSrmMoreyFormula(topUpGrains, volumeOut);

		// calc the dilution of the existing wort colour
		ColourUnit dilutedColour = Equations.calcColourWithVolumeChange(
			inputWort.getVolume(),
			inputWort.getColour(),
			volumeOut);

		// model the sparge runnings colour as:
		//  the existing wort colour, diluted by the sparge water, plus an top up grains colour
		ColourUnit colourOut = new ColourUnit(dilutedColour.get() + addedColour.get());

		// output the lautered mash volume, in case it needs to be input into further batch sparge steps
		Volume lauteredMashVolume = new Volume(
			outputMashVolume,
			Volume.Type.MASH,
			mash.getVolume(),
			mash.getIngredientAdditions(IngredientAddition.Type.FERMENTABLES),
			(WaterAddition)mash.getIngredientAddition(IngredientAddition.Type.WATER),
			mash.getTemperature(),
			spargeGravity,
			mash.getColour() // todo replace with sparge colour
		);

		volumes.addOrUpdateVolume(outputMashVolume, lauteredMashVolume);

		// output the isolated sparge runnings, in case of partigyle brews
		Volume isolatedSpargeRunnings = new Volume(
			outputSpargeRunnings,
			Volume.Type.WORT,
			spargeWater.getVolume(),
			spargeWater.getTemperature(),
			inputWort.getFermentability(),
			spargeGravity,
			inputWort.getAbv(),
			colourOut, // todo replace with sparge colour
			inputWort.getBitterness());

		volumes.addOrUpdateVolume(outputSpargeRunnings, isolatedSpargeRunnings);

		// output the combined worts, for convenience to avoid a combine step
		// right after every batch sparge step
		Volume combinedWort = new Volume(
			outputCombinedWortVolume,
			Volume.Type.WORT,
			volumeOut,
			tempOut,
			inputWort.getFermentability(),
			gravityOut,
			new PercentageUnit(0D),
			colourOut,
			new BitternessUnit(0D));

		volumes.addOrUpdateVolume(outputCombinedWortVolume, combinedWort);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		recipe.getVolumes().addVolume(outputMashVolume, new Volume(Volume.Type.MASH));
		recipe.getVolumes().addVolume(outputSpargeRunnings, new Volume(Volume.Type.WORT));
		recipe.getVolumes().addVolume(outputCombinedWortVolume, new Volume(Volume.Type.WORT));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("batch.sparge.step.desc");
	}

	@Override
	public Collection<String> getInputVolumes()
	{
		return Arrays.asList(mashVolume, wortVolume);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		return Collections.singletonList(outputCombinedWortVolume);
	}

	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return List.of(IngredientAddition.Type.WATER, IngredientAddition.Type.FERMENTABLES);
	}

	/*-------------------------------------------------------------------------*/

	public String getMashVolume()
	{
		return mashVolume;
	}

	public String getWortVolume()
	{
		return wortVolume;
	}

	public String getOutputCombinedWortVolume()
	{
		return outputCombinedWortVolume;
	}

	public void setMashVolume(String mashVolume)
	{
		this.mashVolume = mashVolume;
	}

	public void setWortVolume(String wortVolume)
	{
		this.wortVolume = wortVolume;
	}

	public void setOutputCombinedWortVolume(String outputCombinedWortVolume)
	{
		this.outputCombinedWortVolume = outputCombinedWortVolume;
	}

	public String getOutputMashVolume()
	{
		return outputMashVolume;
	}

	public String getOutputSpargeRunnings()
	{
		return outputSpargeRunnings;
	}

	public void setOutputMashVolume(String outputMashVolume)
	{
		this.outputMashVolume = outputMashVolume;
	}

	public void setOutputSpargeRunnings(String outputSpargeRunnings)
	{
		this.outputSpargeRunnings = outputSpargeRunnings;
	}

	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		for (IngredientAddition ia : getIngredientAdditions(IngredientAddition.Type.WATER))
		{
			if (ia.getType() == IngredientAddition.Type.WATER)
			{
				WaterAddition wa = (WaterAddition)ia;
				result.add(
					StringUtils.getDocString(
						"batch.sparge.water",
						wa.getQuantity().get(LITRES),
						wa.getName(),
						wa.getTemperature().get(Quantity.Unit.CELSIUS)));
			}
			else if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add(
					StringUtils.getDocString(
						"batch.sparge.fermentable.addition",
						ia.getQuantity().get(Quantity.Unit.GRAMS),
						ia.getName()));
			}
			else
			{
				throw new BrewdayException("invalid "+ia.getType());
			}
		}

		String combinedWort = this.getOutputCombinedWortVolume();
		Volume wortVol = getRecipe().getVolumes().getVolume(combinedWort);

		result.add(StringUtils.getDocString(
			"batch.sparge.collected.wort",
			wortVol.getMetric(Volume.Metric.VOLUME).get(LITRES),
			wortVol.getMetric(Volume.Metric.GRAVITY).get(SPECIFIC_GRAVITY)));

		return result;
	}

	@Override
	public ProcessStep clone()
	{
		return new BatchSparge(
			this.getName(),
			this.getDescription(),
			this.mashVolume,
			this.wortVolume,
			this.outputCombinedWortVolume,
			this.outputSpargeRunnings,
			this.outputMashVolume,
			cloneIngredients(this.getIngredients())
		);
	}
}
