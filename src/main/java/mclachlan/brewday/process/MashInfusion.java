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
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.ui.UiQuantityDisplay;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.ui.UiUnitPreferences;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * Represents an infusion into an existing mash.
 */
public class MashInfusion extends ProcessStep
{
	private String inputMashVolume;
	private String outputMashVolume;

	private TimeUnit rampTime, standTime;

	// calculated from water infusion
	private TemperatureUnit mashTemp;

	/*-------------------------------------------------------------------------*/
	public MashInfusion()
	{
	}

	/*-------------------------------------------------------------------------*/
	public MashInfusion(
		String name,
		String description,
		String inputMashVolume,
		String outputMashVolume,
		TimeUnit rampTime,
		TimeUnit standTime)
	{
		super(name, description, Type.MASH_INFUSION);
		this.inputMashVolume = inputMashVolume;
		this.outputMashVolume = outputMashVolume;
		this.rampTime = rampTime;
		this.standTime = standTime;
	}

	/*-------------------------------------------------------------------------*/
	public MashInfusion(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.MASH_INFUSION), StringUtils.getProcessString("mash.infusion.desc"), Type.MASH_INFUSION);

		inputMashVolume = recipe.getVolumes().getVolumeByType(Volume.Type.MASH, recipe);
		rampTime = new TimeUnit(5, MINUTES);
		standTime = new TimeUnit(15, MINUTES);

		outputMashVolume = StringUtils.getProcessString("mash.mash.vol", getName());
	}

	/*-------------------------------------------------------------------------*/
	public MashInfusion(MashInfusion step)
	{
		super(step.getName(), step.getDescription(), Type.MASH_INFUSION);

		inputMashVolume = step.getInputMashVolume();
		rampTime = step.getRampTime();
		standTime = step.getStandTime();
		outputMashVolume = step.getOutputMashVolume();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		//
		// Require an existing mash volume before adding infusion liquor.
		//
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		Volume inputMash = volumes.getVolume(inputMashVolume);
		WaterAddition infusionWater;
		IngredientAddition rli = getIngredientAddition(IngredientAddition.Type.WATER);

		if (rli == null)
		{
			log.addError(StringUtils.getProcessString("mash.infusion.no.water.addition"));
			return;
		}
		else
		{
			infusionWater = (WaterAddition)rli;
		}

		//
		// Step-infusion liquor raises mash temperature using the Palmer / How to Brew
		// grain thermal-mass model (same energy balance as strike water), not a simple
		// two-fluid volume-weighted mix.
		//
		List<FermentableAddition> grainBill =
			(List<FermentableAddition>)(List<?>)inputMash.getIngredientAdditions(
				IngredientAddition.Type.FERMENTABLES);
		WaterAddition mashWater =
			(WaterAddition)inputMash.getIngredientAddition(IngredientAddition.Type.WATER);

		mashTemp = Equations.calcMashInfusionTemp(
			Equations.calcTotalGrainWeight(grainBill),
			mashWater.getVolume(),
			inputMash.getTemperature(),
			infusionWater.getVolume(),
			infusionWater.getTemperature());

		//
		// Liquor volume increases by the infusion amount with no further grain absorption; gravity
		// and colour dilute accordingly. Strike and infusion waters merge for later sparge chemistry.
		//
		VolumeUnit volumeOut = new VolumeUnit(
			inputMash.getVolume().get()
				+ infusionWater.getVolume().get());

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			inputMash.getVolume(),
			inputMash.getGravity(),
			volumeOut);

		ColourUnit colourOut = Equations.calcColourWithVolumeChange(
			inputMash.getVolume(),
			inputMash.getColour(),
			volumeOut);

		String combinedWaterName = StringUtils.getProcessString("mash.infusion.combined.water", getName());

		WaterAddition combinedWater = mashWater.getCombination(infusionWater);
		combinedWater.setName(combinedWaterName);

		Volume outputVolume = new Volume(
			outputMashVolume,
			Volume.Type.MASH,
			volumeOut,
			inputMash.getIngredientAdditions(IngredientAddition.Type.FERMENTABLES),
			combinedWater,
			mashTemp,
			gravityOut,
			colourOut,
			null);

		//
		// Treat infusion liquor as a pH-bearing fluid: blend the established mash pH with the
		// infusion water pH by hydrogen-ion concentration weighted by liquor volume. This is a
		// practical estimate only; the mash buffering chemistry is not re-solved. If the infusion
		// water has no pH, the mash pH carries through unchanged.
		//
		PhUnit infusionPh = infusionWater.getWater() == null
			? null
			: infusionWater.getWater().getPh();
		PhVolumes.applyWaterBlend(
			inputMash,
			inputMash.getVolume(),
			infusionPh,
			infusionWater.getVolume(),
			outputVolume);

		//
		// Hop-acid inventory and IBU stay with the mash volume unchanged by the added liquor volume.
		//
		HopAcidVolumes.applyVolumeUnchanged(inputMash, outputVolume);
		BitternessVolumes.syncReportedDerived(
			outputVolume,
			Settings.parseReportedFormulas(Database.getInstance().getSettings()));

		//
		// Replace the input mash with the stepped mash under the output volume name.
		//
		volumes.addOrUpdateVolume(
			outputMashVolume,
			outputVolume);
	}

	/*-------------------------------------------------------------------------*/
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		if (!volumes.contains(inputMashVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputMashVolume));
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

		recipe.getVolumes().addVolume(outputMashVolume, new Volume(Volume.Type.MASH));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Map<String, String> describeProperties()
	{
		Map<String, String> result = new LinkedHashMap<>();
		result.put("inputMashVolume", String.valueOf(inputMashVolume));
		result.put("outputMashVolume", String.valueOf(outputMashVolume));
		result.put("rampTime", rampTime == null ? "null" : rampTime.get(MINUTES) + "min");
		result.put("standTime", standTime == null ? "null" : standTime.get(MINUTES) + "min");
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		Quantity.Unit tempUnit = UiUnitPreferences.from(Database.getInstance().getSettings())
			.get(UiUnitPreferences.Slot.TEMPERATURE);
		return StringUtils.getProcessString("mash.infusion.step.desc",
			getName(),
			mashTemp.describe(tempUnit));
	}

	public String getOutputMashVolume()
	{
		return outputMashVolume;
	}

	public void setOutputMashVolume(String outputMashVolume)
	{
		this.outputMashVolume = outputMashVolume;
	}

	public TimeUnit getStandTime()
	{
		return standTime;
	}

	public TimeUnit getRampTime()
	{
		return rampTime;
	}

	public void setRampTime(TimeUnit rampTime)
	{
		this.rampTime = rampTime;
	}

	public TemperatureUnit getMashTemp()
	{
		return mashTemp;
	}

	public void setStandTime(TimeUnit standTime)
	{
		this.standTime = standTime;
	}

	@Override
	public Collection<String> getInputVolumes()
	{
		return inputMashVolume==null?Collections.emptyList():Collections.singletonList(inputMashVolume);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		return outputMashVolume==null?Collections.emptyList():Collections.singletonList(outputMashVolume);
	}

	public String getInputMashVolume()
	{
		return inputMashVolume;
	}

	public void setInputMashVolume(String inputMashVolume)
	{
		this.inputMashVolume = inputMashVolume;
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(IngredientAddition.Type.values());
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.WATER)
			{
				result.add(
					StringUtils.getDocString(
						"mash.water.addition",
						ia.describe()));
			}
			else
			{
				result.add(StringUtils.getDocString("additions.generic", ia.describe()));
			}
		}

		String outputMashVolume = this.getOutputMashVolume();
		Volume mashVol = getRecipe().getVolumes().getVolume(outputMashVolume);

		result.add(StringUtils.getDocString(
			"mash.volume",
			UiQuantityDisplay.describe(mashVol.getVolume()),
			UiQuantityDisplay.describe(mashVol.getTemperature())));

		result.add(StringUtils.getDocString("mash.rest", this.standTime.describe(MINUTES)));

		return result;
	}

	@Override
	public ProcessStep clone(String newName)
	{
		return new MashInfusion(
			newName,
			this.getDescription(),
			this.getInputMashVolume(),
			StringUtils.getProcessString("mash.mash.vol", newName),
			new TimeUnit(this.rampTime),
			new TimeUnit(this.standTime));
	}
}
