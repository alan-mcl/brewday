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
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * Separates a Mash volume into a Lautered Mash and a First Runnings volume.
 * Supports first wort hops.
 */
public class Lauter extends ProcessStep
{
	private String inputMashVolume;
	private String outputFirstRunnings;
	private String outputLauteredMashVolume;

	/*-------------------------------------------------------------------------*/
	public Lauter()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Lauter(
		String name,
		String description,
		String inputMashVolume, String outputLauteredMashVolume,
		String outputFirstRunnings)
	{
		super(name, description, Type.LAUTER);
		this.inputMashVolume = inputMashVolume;
		this.outputFirstRunnings = outputFirstRunnings;
		this.outputLauteredMashVolume = outputLauteredMashVolume;
	}

	/*-------------------------------------------------------------------------*/
	public Lauter(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.LAUTER), StringUtils.getProcessString("lauter.desc"), Type.LAUTER);

		inputMashVolume = recipe.getVolumes().getVolumeByType(Volume.Type.MASH);
		outputLauteredMashVolume = StringUtils.getProcessString("lauter.mash.vol", getName());
		outputFirstRunnings = StringUtils.getProcessString("lauter.first.runnings", getName());
	}

	/*-------------------------------------------------------------------------*/
	public Lauter(Lauter step)
	{
		super(step.getName(), step.getDescription(), Type.LAUTER);

		this.inputMashVolume = step.getInputMashVolume();
		this.outputLauteredMashVolume = step.getOutputLauteredMashVolume();
		this.outputFirstRunnings = step.getOutputFirstRunnings();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		Volume mashVolumeOut = volumes.getVolume(inputMashVolume);
		Volume firstRunningsOut = getFirstRunningsOut(mashVolumeOut, equipmentProfile);

		// FWH
		// We return only the extra bitterness from the hop "stand" here. Ingredient
		// additions are passed along and future Boil steps will add the remainder
		// of the bitterness.
		// There are better ways of doing this, see here for inspiration:
		// https://alchemyoverlord.wordpress.com/2016/03/06/an-analysis-of-sub-boiling-hop-utilization/

		List<HopAddition> hopCharges = new ArrayList<>();
		for (IngredientAddition ia : getIngredientAdditions(IngredientAddition.Type.HOPS))
		{
			if (ia instanceof HopAddition)
			{
				// we fudge this to roughly the usual boil time, after it is
				// double-fudged by just using the brewing setting
				ia.setTime(new TimeUnit(60, MINUTES));
				hopCharges.add((HopAddition)ia);
			}
		}

		if (!hopCharges.isEmpty())
		{
			BitternessUnit fwhIbu = Equations.calcTotalIbuTinseth(
				hopCharges,
				firstRunningsOut.getGravity(),
				firstRunningsOut.getVolume(),
				Double.valueOf(Database.getInstance().getSettings().get(Settings.FIRST_WORT_HOP_UTILISATION)));

			BitternessUnit bitternessIn = firstRunningsOut.getBitterness();

			if (bitternessIn == null)
			{
				bitternessIn = new BitternessUnit(0, IBU);
			}

			firstRunningsOut.setBitterness(
				new BitternessUnit(
					bitternessIn.get() + fwhIbu.get()));
			firstRunningsOut.getIngredientAdditions().addAll(hopCharges);
		}

		// stick the volumes in there
		volumes.addOrUpdateVolume(outputFirstRunnings, firstRunningsOut);
		volumes.addOrUpdateVolume(outputLauteredMashVolume, mashVolumeOut);
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

	/*-------------------------------------------------------------------------*/
	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		if (!validateInputVolumes(recipe.getVolumes(), log))
		{
			return;
		}

		recipe.getVolumes().addVolume(outputLauteredMashVolume, new Volume(Volume.Type.MASH));
		recipe.getVolumes().addVolume(outputFirstRunnings, new Volume(Volume.Type.WORT));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Collections.singletonList(IngredientAddition.Type.HOPS);
	}

	/*-------------------------------------------------------------------------*/
	private Volume getFirstRunningsOut(
		Volume mashVolume,
		EquipmentProfile equipmentProfile)
	{
		List<IngredientAddition> grainBill = getGrainBill(mashVolume);

		WeightUnit grainWeight = Equations.getTotalGrainWeight(grainBill);

		WaterAddition waterAddition =
			(WaterAddition)mashVolume.getIngredientAddition(IngredientAddition.Type.WATER);

		VolumeUnit volumeOutMl = Equations.calcWortVolume(grainWeight, waterAddition.getVolume());

		// Always assume that the first running volume is estimated, despite the
		// grain and water additions being measured. We're doing this to ensure that
		// the chain of estimated quantities starts here.
		volumeOutMl.setEstimated(true);

		volumeOutMl.set(volumeOutMl.get(Quantity.Unit.MILLILITRES)
			- equipmentProfile.getLauterLoss().get(MILLILITRES));

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
			mashVolume.getBitterness());
	}

	/*-------------------------------------------------------------------------*/
	private List<IngredientAddition> getGrainBill(Volume mashVolume)
	{
		List<IngredientAddition> result = new ArrayList<>();
		for (IngredientAddition ia : mashVolume.getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add(ia);
			}
		}
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("mash.step.desc", getName());
	}

	public String getOutputLauteredMashVolume()
	{
		return outputLauteredMashVolume;
	}

	public String getOutputFirstRunnings()
	{
		return outputFirstRunnings;
	}

	public void setOutputFirstRunnings(String outputFirstRunnings)
	{
		this.outputFirstRunnings = outputFirstRunnings;
	}

	public void setOutputLauteredMashVolume(String outputLauteredMashVolume)
	{
		this.outputLauteredMashVolume = outputLauteredMashVolume;
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
	public Collection<String> getInputVolumes()
	{
		return inputMashVolume==null?Collections.emptyList():Collections.singletonList(inputMashVolume);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		ArrayList<String> result = new ArrayList<>();

		if (outputLauteredMashVolume != null)
		{
			result.add(outputLauteredMashVolume);
		}
		if (outputFirstRunnings != null)
		{
			result.add(outputFirstRunnings);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		result.add(StringUtils.getDocString("lauter.doc"));

		return result;
	}

	@Override
	public ProcessStep clone()
	{
		return new Lauter(
			this.getName(),
			this.getDescription(),
			this.getInputMashVolume(),
			this.getOutputLauteredMashVolume(),
			this.getOutputFirstRunnings());
	}
}

