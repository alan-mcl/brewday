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
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.recipe.Recipe;

/**
 * Splits the input volume in two. Can split by percentage or by absolute/remainder
 */
public class Split extends FluidVolumeProcessStep
{
	public enum Type
	{
		/** Split into a % and a remainder */
		PERCENTAGE,
		/** Split into a specified amount and a remainder */
		ABSOLUTE
	}

	private Type splitType;

	/** the proportion of the first volume, in the case of a PERCENTAGE split  */
	private PercentageUnit splitPercent;

	/** the absolute amount of the first volume, in the case of an ABSOLUTE split */
	private VolumeUnit splitVolume;

	/** Name of the output volume */
	private String outputVolume2;

	/*-------------------------------------------------------------------------*/
	public Split()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Split(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		Type splitType,
		PercentageUnit splitPercent,
		VolumeUnit splitVolume,
		String outputVolume2)
	{
		super(name, description, ProcessStep.Type.SPLIT,  inputVolume, outputVolume);
		this.splitType = splitType;
		this.splitPercent = splitPercent;
		this.splitVolume = splitVolume;
		this.outputVolume2 = outputVolume2;
	}

	/*-------------------------------------------------------------------------*/
	public Split(Recipe recipe)
	{
		super(recipe.getUniqueStepName(ProcessStep.Type.SPLIT), StringUtils.getProcessString("split.desc"),
			ProcessStep.Type.SPLIT, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT, recipe));
		setOutputVolume(StringUtils.getProcessString("split.output.1", getName()));

		this.outputVolume2 = StringUtils.getProcessString("split.output.2", getName());

		// default to a 50/50 split
		this.splitType = Type.PERCENTAGE;
		this.splitPercent = new PercentageUnit(0.5D);
	}

	/*-------------------------------------------------------------------------*/
	public Split(Split other)
	{
		super(other.getName(), other.getDescription(), ProcessStep.Type.SPLIT, other.getInputVolume(), other.getOutputVolume());

		this.splitType = other.splitType;
		this.splitPercent = other.splitPercent;
		this.splitVolume = other.splitVolume;
		this.outputVolume2 = other.outputVolume2;
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		super.dryRun(recipe, log);
		recipe.getVolumes().addVolume(outputVolume2, recipe.getVolumes().getVolume(getInputVolume()));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		Volume inputVolume = volumes.getVolume(this.getInputVolume());

		VolumeUnit volume1Out;
		VolumeUnit volume2Out;
		switch (this.splitType)
		{
			case PERCENTAGE:
				volume1Out = new VolumeUnit(inputVolume.getVolume().get() * splitPercent.get());
				volume2Out = new VolumeUnit(inputVolume.getVolume().get() * (1-splitPercent.get()));
				break;
			case ABSOLUTE:
				volume1Out = new VolumeUnit(this.splitVolume);
				volume2Out = new VolumeUnit(inputVolume.getVolume().get() - this.splitVolume.get());
				break;
			default:
				throw new BrewdayException("invalid "+ splitType);
		}

		Volume v1 = new Volume(getOutputVolume(), inputVolume);
		v1.setVolume(volume1Out);

		Volume v2 = new Volume(getOutputVolume2(), inputVolume);
		v2.setVolume(volume2Out);

		volumes.addOrUpdateVolume(getOutputVolume(), v1);
		volumes.addOrUpdateVolume(getOutputVolume2(), v2);
	}

	public Type getSplitType()
	{
		return splitType;
	}

	public void setSplitType(Type splitType)
	{
		this.splitType = splitType;
	}

	public VolumeUnit getSplitVolume()
	{
		return splitVolume;
	}

	public void setSplitVolume(VolumeUnit splitVolume)
	{
		this.splitVolume = splitVolume;
	}

	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("split.perc.step.name");
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		List<String> result = new ArrayList<>(super.getOutputVolumes());
		result.add(outputVolume2);
		return result;
	}

	public String getOutputVolume2()
	{
		return outputVolume2;
	}

	public PercentageUnit getSplitPercent()
	{
		return splitPercent;
	}

	public void setSplitPercent(PercentageUnit splitPercent)
	{
		this.splitPercent = splitPercent;
	}

	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		switch (this.splitType)
		{
			case PERCENTAGE:
				result.add(StringUtils.getDocString(
					"split.perc",
					this.getInputVolume(),
					this.splitPercent.get(Quantity.Unit.PERCENTAGE_DISPLAY),
					100-this.splitPercent.get(Quantity.Unit.PERCENTAGE_DISPLAY)));
				break;
			case ABSOLUTE:
				result.add(StringUtils.getDocString(
					"split.abs",
					this.getInputVolume(),
					this.splitVolume.describe(Quantity.Unit.LITRES)));
				break;
			default:
				throw new BrewdayException("Invalid "+ splitType);
		}

		Volume outVol1 = getRecipe().getVolumes().getVolume(getOutputVolume());
		Volume outVol2 = getRecipe().getVolumes().getVolume(getOutputVolume2());

		result.add(StringUtils.getDocString(
			"split.output",
			outVol1.getName(),
			outVol1.getVolume().describe(Quantity.Unit.LITRES)));

		result.add(StringUtils.getDocString(
			"split.output",
			outVol2.getName(),
			outVol2.getVolume().describe(Quantity.Unit.LITRES)));

		return result;
	}

	@Override
	public ProcessStep clone(String newName)
	{
		return new Split(
			newName,
			this.getDescription(),
			this.getInputVolume(),
			StringUtils.getProcessString("split.output.1", newName),
			this.splitType,
			this.splitPercent,
			this.splitVolume,
			StringUtils.getProcessString("split.output.2", newName));
	}
}
