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
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.recipe.Recipe;

public class SplitByPercent extends FluidVolumeProcessStep
{
	/** the proportion of the first volume from, second volume is the remainder */
	private double splitPercent;

	private String outputVolume2;

	/*-------------------------------------------------------------------------*/
	public SplitByPercent()
	{
	}

	/*-------------------------------------------------------------------------*/
	public SplitByPercent(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		double splitPercent,
		String outputVolume2)
	{
		super(name, description, Type.SPLIT_BY_PERCENT,  inputVolume, outputVolume);
		this.splitPercent = splitPercent;
		this.outputVolume2 = outputVolume2;
	}

	/*-------------------------------------------------------------------------*/
	public SplitByPercent(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.SPLIT_BY_PERCENT), StringUtils.getProcessString("split.perc.desc"), Type.SPLIT_BY_PERCENT, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(StringUtils.getProcessString("split.perc.output.1", getName()));

		this.outputVolume2 = StringUtils.getProcessString("split.perc.output.2", getName());
		this.splitPercent = 0.5D;
	}

	/*-------------------------------------------------------------------------*/
	public SplitByPercent(SplitByPercent step)
	{
		super(step.getName(), step.getDescription(), Type.SPLIT_BY_PERCENT, step.getInputVolume(), step.getOutputVolume());

		this.splitPercent = step.splitPercent;
		this.outputVolume2 = step.outputVolume2;
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
		if (!validateInputVolume(volumes, log))
		{
			return;
		}

		Volume inputVolume = volumes.getVolume(this.getInputVolume());

		VolumeUnit volume1Out = new VolumeUnit(inputVolume.getVolume().get() * splitPercent);
		VolumeUnit volume2Out = new VolumeUnit(inputVolume.getVolume().get() * (1-splitPercent));

		Volume v1 = new Volume(getOutputVolume(), inputVolume);
		v1.setVolume(volume1Out);

		Volume v2 = new Volume(getOutputVolume2(), inputVolume);
		v2.setVolume(volume2Out);

		volumes.addOrUpdateVolume(getOutputVolume(), v1);
		volumes.addOrUpdateVolume(getOutputVolume2(), v2);
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

	public double getSplitPercent()
	{
		return splitPercent;
	}

	public void setSplitPercent(double splitPercent)
	{
		this.splitPercent = splitPercent;
	}
}
