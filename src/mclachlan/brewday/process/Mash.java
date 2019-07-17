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
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.recipe.*;

public class Mash extends ProcessStep
{
	private String outputFirstRunnings;
	private String outputMashVolume;

	/** duration in minutes */
	private double duration;

	/** grain volume temp in C */
	private double grainTemp;

	// calculated from strike water
	private double mashTemp;

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
		double duration,
		double grainTemp)
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
		super(recipe.getUniqueStepName(Type.MASH), "Initial mash infusion", Type.MASH);

		duration = 60;
		grainTemp = 20;

		outputMashVolume = getName()+" mash vol";
		outputFirstRunnings = getName()+" first runnings";
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
	public void apply(Volumes volumes, Recipe recipe, ErrorsAndWarnings log)
	{
		EquipmentProfile equipmentProfile =
			Database.getInstance().getEquipmentProfiles().get(recipe.getEquipmentProfile());
		if (equipmentProfile == null)
		{
			log.addError("invalid equipment profile ["+equipmentProfile+"]");
			return;
		}

		List<IngredientAddition> grainBill = new ArrayList<>();
		WaterAddition strikeWater = null;

		for (IngredientAddition item : getIngredients())
		{
			// seek the grains and water with the same time as the mash,
			// these are the initial combination

			if (item.getTime() == this.getDuration())
			{
				if (item instanceof FermentableAddition)
				{
					grainBill.add(item);
				}
				else if (item instanceof WaterAddition)
				{
					strikeWater = (WaterAddition)item;
				}
			}
		}

		if (grainBill == null)
		{
			log.addError("No initial fermentable addition to mash");
			return;
		}
		if (strikeWater == null)
		{
			log.addError("No strike water for mash");
			return;
		}

		MashVolume mashVolumeOut = getMashVolumeOut(
			recipe, equipmentProfile, grainBill, strikeWater);
		volumes.addVolume(outputMashVolume, mashVolumeOut);

		if (mashVolumeOut.getVolume() *1.1 > equipmentProfile.getMashTunVolume())
		{
			log.addWarning(
				String.format(
					"Mash tun (%.2f l) may not be large enough for mash volume (%.2f l)",
					equipmentProfile.getMashTunVolume()/1000, mashVolumeOut.getVolume()/1000));
		}

		WortVolume firstRunningsOut = getFirstRunningsOut(
			mashVolumeOut, grainBill, equipmentProfile);
		volumes.addVolume(outputFirstRunnings, firstRunningsOut);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void dryRun(Recipe recipe, ErrorsAndWarnings log)
	{
		recipe.getVolumes().addVolume(outputMashVolume, new MashVolume());
		recipe.getVolumes().addVolume(outputFirstRunnings, new WortVolume());
	}

	/*-------------------------------------------------------------------------*/
	private WortVolume getFirstRunningsOut(
		MashVolume mashVolume,
		List<IngredientAddition> grainBill,
		EquipmentProfile equipmentProfile)
	{
		double grainWeight = getTotalGrainWeight(grainBill);

		double volumeOut =
			Equations.calcWortVolume(
				grainWeight,
				mashVolume.getWater().getVolume())
			- equipmentProfile.getLauterLoss();

		WortVolume.Fermentability fermentabilityOut;
		if (mashVolume.getTemperature() < 65.5D)
		{
			fermentabilityOut = WortVolume.Fermentability.HIGH;
		}
		else if (mashVolume.getTemperature() < 67.5D)
		{
			fermentabilityOut = WortVolume.Fermentability.MEDIUM;
		}
		else
		{
			fermentabilityOut = WortVolume.Fermentability.LOW;
		}

		return new WortVolume(
			volumeOut,
			mashVolume.getTemperature(),
			fermentabilityOut,
			mashVolume.getGravity(),
			0D,
			mashVolume.getColour(),
			0D);
	}

	/*-------------------------------------------------------------------------*/
	private double getTotalGrainWeight(List<IngredientAddition> grainBill)
	{
		double result = 0D;
		for (IngredientAddition item : grainBill)
		{
			result += item.getWeight();
		}
		return result;
	}

	/*-------------------------------------------------------------------------*/
	private MashVolume getMashVolumeOut(
		Recipe recipe,
		EquipmentProfile equipmentProfile,
		List<IngredientAddition> grainBill,
		WaterAddition strikeWater)
	{
		double grainWeight = getTotalGrainWeight(grainBill);

		mashTemp = Equations.calcMashTemp(grainWeight, strikeWater, grainTemp);

		double volumeOut = Equations.calcMashVolume(grainWeight, strikeWater.getVolume());

		double mashEfficiency =
			Database.getInstance().getEquipmentProfiles().get(
				recipe.getEquipmentProfile()).getMashEfficiency();

		DensityUnit gravityOut = Equations.calcMashExtractContent(grainBill, grainWeight, mashEfficiency, strikeWater);

		double colourOut = Equations.calcSrmMoreyFormula(grainBill, volumeOut);

		return new MashVolume(
			volumeOut,
			grainBill,
			strikeWater,
			mashTemp,
			gravityOut,
			colourOut,
			equipmentProfile.getLauterLoss());
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return String.format("Mash: '%s'", getName());
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

	public void setMashTemp(double mashTemp)
	{
		this.mashTemp = mashTemp;
	}

	public double getDuration()
	{
		return duration;
	}

	public double getGrainTemp()
	{
		return grainTemp;
	}

	public void setGrainTemp(double grainTemp)
	{
		this.grainTemp = grainTemp;
	}

	public double getMashTemp()
	{
		return mashTemp;
	}

	public void setDuration(double duration)
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
		return Collections.singletonList(outputMashVolume);
	}

	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		// todo: mash hops
		return Arrays.asList(
			IngredientAddition.Type.FERMENTABLES,
			IngredientAddition.Type.MISC,
			IngredientAddition.Type.WATER);
	}
}
