/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.util.StringUtils;

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

		this.mashVolume = recipe.getVolumes().getVolumeByType(Volume.Type.MASH, recipe);
		this.wortVolume = recipe.getVolumes().getVolumeByType(Volume.Type.WORT, recipe);

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
		//
		// Require mash (and optionally accumulated wort) volumes before simulating a sparge.
		//
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		WaterAddition spargeWater = getCombinedWaterProfile(null);
		List<FermentableAddition> topUpGrains = new ArrayList<>();

		//
		// Grain or adjunct additions on this step are treated as top-up grains in the sparge liquor,
		// contributing extract and colour like a mini mash during the sparge.
		//
		for (IngredientAddition item : getFermentableAdditions())
		{
			FermentableAddition fa = (FermentableAddition)item;
			Fermentable fermentable = fa.getFermentable();
			if (fermentable.getType() == Fermentable.Type.GRAIN || fermentable.getType() == Fermentable.Type.ADJUNCT)
			{
				topUpGrains.add(fa);
			}
		}

		if (spargeWater == null)
		{
			log.addError(StringUtils.getProcessString("batch.sparge.no.water.additions"));
			return;
		}

		//
		// Start from existing kettle wort when named, or an empty wort placeholder on the first sparge
		// of a session; always read the current lautered mash volume for runnings calculations.
		//
		Volume inputWort;
		if (wortVolume == null)
		{
			inputWort = new Volume(
				"zero volume",
				Volume.Type.WORT,
				new VolumeUnit(0, LITRES, false),
				new TemperatureUnit(0, CELSIUS, false),
				new PercentageUnit(0, false),
				new DensityUnit(0, GU, false),
				new PercentageUnit(0, false),
				new ColourUnit(0, SRM, false),
				new BitternessUnit(0, IBU, false));
		}
		else
		{
			inputWort = volumes.getVolume(wortVolume);
		}
		Volume mash = volumes.getVolume(mashVolume);

		//
		// Sparge runnings gravity: sweet wort drawn from the grain bed is diluted by sparge liquor but
		// still reflects the soluble extract remaining in the mash.
		//
		DensityUnit mashGravity = mash.getGravity();
		VolumeUnit mashVolume = mash.getVolume();

		DensityUnit spargeGravity = Equations.getSpargeRunningGravity(spargeWater, mashGravity, mashVolume);

		VolumeUnit volumeOut = new VolumeUnit(
			inputWort.getVolume().get(Quantity.Unit.MILLILITRES) +
			spargeWater.getVolume().get(Quantity.Unit.MILLILITRES),
			Quantity.Unit.MILLILITRES,
			inputWort.getVolume().isEstimated() || spargeWater.getVolume().isEstimated());

		TemperatureUnit tempOut =
			Equations.calcCombinedTemperature(
				inputWort.getVolume(),
				inputWort.getTemperature(),
				spargeWater.getVolume(),
				spargeWater.getTemperature());

		ColourUnit addedColour = Equations.calcColourSrmMoreyFormula(topUpGrains, volumeOut);

		//
		// Top-up grains in the sparge are assumed to fully convert at equipment conversion
		// efficiency, boosting runnings gravity before the sparge stream is blended into kettle wort.
		//
		if (!topUpGrains.isEmpty())
		{
			DensityUnit addedGravity = Equations.calcMashExtractContentFromYield(
				topUpGrains,
				equipmentProfile.getConversionEfficiency().get(PERCENTAGE),
				spargeWater);

			log.addVerboseMessage(StringUtils.getProcessString("batch.sparge.top.up.grains.gravity", addedGravity.describe(SPECIFIC_GRAVITY)));

			spargeGravity = new DensityUnit(spargeGravity.get(PLATO) + addedGravity.get(PLATO), PLATO);
		}

		DensityUnit gravityOut = Equations.calcCombinedGravity(
			inputWort.getVolume(),
			inputWort.getGravity(),
			spargeWater.getVolume(),
			spargeGravity);

		//
		// Colour: existing kettle wort is diluted by the larger combined volume, then grain-derived
		// colour from top-up grains is added to model the sparge runnings appearance.
		//
		ColourUnit dilutedColour = Equations.calcColourWithVolumeChange(
			inputWort.getVolume(),
			inputWort.getColour(),
			volumeOut);

		ColourUnit spargeColour = new ColourUnit(dilutedColour.get() + addedColour.get());

		List<Settings.HopBitternessFormula> reportedFormulas =
			Settings.parseReportedFormulas(Database.getInstance().getSettings());

		//
		// Update the spent mash volume with post-sparge gravity and colour for another batch sparge pass.
		//
		Volume lauteredMashVolume = new Volume(
			outputMashVolume,
			Volume.Type.MASH,
			mashVolume,
			mash.getIngredientAdditions(IngredientAddition.Type.FERMENTABLES),
			(WaterAddition)mash.getIngredientAddition(IngredientAddition.Type.WATER),
			mash.getTemperature(),
			spargeGravity,
			spargeColour,
			null); // todo: sparge impact on pH
		PhVolumes.copyAll(mash, lauteredMashVolume);
		BitternessVolumes.applyVolumeChange(mash, lauteredMashVolume, volumeOut, reportedFormulas);

		volumes.addOrUpdateVolume(outputMashVolume, lauteredMashVolume);

		//
		// Isolated sparge runnings support parti-gyle or blending workflows without forcing a combine step.
		//
		Volume isolatedSpargeRunnings = new Volume(
			outputSpargeRunnings,
			Volume.Type.WORT,
			spargeWater.getVolume(),
			spargeWater.getTemperature(),
			inputWort.getFermentability(),
			spargeGravity,
			inputWort.getAbv(),
			spargeColour,
			BitternessVolumes.zero());
		BitternessVolumes.applyVolumeChange(mash, isolatedSpargeRunnings, spargeWater.getVolume(), reportedFormulas);
		HopAcidVolumes.applySplit(
			mash,
			mash.getVolume(),
			mashVolume,
			lauteredMashVolume,
			spargeWater.getVolume(),
			isolatedSpargeRunnings);
		BitternessVolumes.syncReportedDerived(lauteredMashVolume, reportedFormulas);
		BitternessVolumes.syncReportedDerived(isolatedSpargeRunnings, reportedFormulas);

		volumes.addOrUpdateVolume(outputSpargeRunnings, isolatedSpargeRunnings);

		//
		// Combined kettle wort merges prior wort with this sparge stream (volume, gravity, colour, IBU,
		// hop acids) so the recipe can proceed directly to boil.
		//
		ColourUnit combinedColour = Equations.calcCombinedColour(
			inputWort.getVolume(), inputWort.getColour(),
			isolatedSpargeRunnings.getVolume(), isolatedSpargeRunnings.getColour());

		Volume combinedWort = new Volume(
			outputCombinedWortVolume,
			Volume.Type.WORT,
			volumeOut,
			tempOut,
			inputWort.getFermentability(),
			gravityOut,
			new PercentageUnit(0D),
			combinedColour,
			BitternessVolumes.zero());
		BitternessVolumes.applyCombined(
			inputWort.getVolume(),
			inputWort,
			isolatedSpargeRunnings.getVolume(),
			isolatedSpargeRunnings,
			combinedWort,
			reportedFormulas);
		HopAcidVolumes.applyCombined(
			inputWort,
			inputWort.getVolume(),
			isolatedSpargeRunnings,
			isolatedSpargeRunnings.getVolume(),
			combinedWort);
		combinedWort.setIngredientAdditions(inputWort.getIngredientAdditions());
		BitternessVolumes.syncReportedDerived(combinedWort, reportedFormulas);

		volumes.addOrUpdateVolume(outputCombinedWortVolume, combinedWort);
	}

	/*-------------------------------------------------------------------------*/
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		if (wortVolume != null && !volumes.contains(wortVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", wortVolume));
			return false;
		}
		return true;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		if (!validateInputVolumes(recipe.getVolumes(), log))
		{
			return;
		}

		recipe.getVolumes().addVolume(outputMashVolume, new Volume(Volume.Type.MASH));
		recipe.getVolumes().addVolume(outputSpargeRunnings, new Volume(Volume.Type.WORT));
		recipe.getVolumes().addVolume(outputCombinedWortVolume, new Volume(Volume.Type.WORT));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Map<String, String> describeProperties()
	{
		Map<String, String> result = new LinkedHashMap<>();
		result.put("mashVolume", String.valueOf(mashVolume));
		result.put("wortVolume", String.valueOf(wortVolume));
		result.put("outputCombinedWortVolume", String.valueOf(outputCombinedWortVolume));
		result.put("outputSpargeRunnings", String.valueOf(outputSpargeRunnings));
		result.put("outputMashVolume", String.valueOf(outputMashVolume));
		return result;
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
		ArrayList<String> result = new ArrayList<>();
		if (mashVolume != null)
		{
			result.add(mashVolume);
		}
		if (wortVolume != null)
		{
			result.add(wortVolume);
		}
		return result;
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		ArrayList<String> result = new ArrayList<>();
		if (outputCombinedWortVolume != null)
		{
			result.add(outputCombinedWortVolume);
		}
		if (outputMashVolume != null)
		{
			result.add(outputMashVolume);
		}
		if (outputSpargeRunnings != null)
		{
			result.add(outputSpargeRunnings);
		}
		return result;
	}

	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(IngredientAddition.Type.values());
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

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.WATER)
			{
				WaterAddition wa = (WaterAddition)ia;
				result.add(
					StringUtils.getDocString(
						"batch.sparge.water",
						wa.describe(),
						wa.getTemperature().describe(Quantity.Unit.CELSIUS)));
			}
			else if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add(
					StringUtils.getDocString(
						"batch.sparge.fermentable.addition",
						ia.describe()));
			}
			else if (ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"batch.sparge.misc.addition",
						ia.describe()));
			}
			else
			{
				result.add(StringUtils.getDocString("additions.generic", ia.describe()));
			}
		}

		String spargeRunnings = this.getOutputSpargeRunnings();
		Volume spargeVol = getRecipe().getVolumes().getVolume(spargeRunnings);

		result.add(StringUtils.getDocString(
			"batch.sparge.sparge.runnings",
			spargeVol.getVolume().describe(LITRES),
			spargeVol.getGravity().describe(SPECIFIC_GRAVITY)));

		String combinedWort = this.getOutputCombinedWortVolume();
		Volume wortVol = getRecipe().getVolumes().getVolume(combinedWort);

		result.add(StringUtils.getDocString(
			"batch.sparge.collected.wort",
			wortVol.getVolume().describe(LITRES),
			wortVol.getGravity().describe(SPECIFIC_GRAVITY)));

		return result;
	}

	@Override
	public ProcessStep clone(String newName)
	{
		return new BatchSparge(
			newName,
			this.getDescription(),
			this.mashVolume,
			this.wortVolume,
			StringUtils.getProcessString("batch.sparge.combined.wort", newName),
			StringUtils.getProcessString("batch.sparge.sparge.runnings", newName),
			StringUtils.getProcessString("batch.sparge.lautered.mash", newName),
			cloneIngredients(this.getIngredientAdditions())
		);
	}
}
