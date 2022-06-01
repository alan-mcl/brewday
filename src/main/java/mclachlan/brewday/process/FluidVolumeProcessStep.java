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
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public abstract class FluidVolumeProcessStep extends ProcessStep
{
	private String inputVolume;
	private String outputVolume;

	/*-------------------------------------------------------------------------*/
	protected FluidVolumeProcessStep()
	{
	}

	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		if (!validateInputVolumes(recipe.getVolumes(), log))
		{
			return;
		}

		// hack to get the same volume type
		recipe.getVolumes().addVolume(outputVolume,
			new Volume(outputVolume, recipe.getVolumes().getVolume(inputVolume)));
	}

	/*-------------------------------------------------------------------------*/
	public FluidVolumeProcessStep(
		String name,
		String description,
		Type type,
		String inputVolume,
		String outputVolume)
	{
		super(name, description, type);
		this.inputVolume = inputVolume;
		this.outputVolume = outputVolume;
	}

	/*-------------------------------------------------------------------------*/
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		if (!volumes.contains(inputVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputVolume));
			return false;
		}
		return true;
	}

	public String getInputVolume()
	{
		return inputVolume;
	}

	public Volume getInputVolume(Volumes volumes)
	{
		return volumes.getVolume(getInputVolume());
	}

	public void setInputVolume(String inputVolume)
	{
		this.inputVolume = inputVolume;
	}

	public String getOutputVolume()
	{
		return outputVolume;
	}

	public void setOutputVolume(String outputVolume)
	{
		this.outputVolume = outputVolume;
	}

	@Override
	public Collection<String> getInputVolumes()
	{
		return inputVolume==null?Collections.emptyList():Collections.singletonList(inputVolume);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		return outputVolume==null?Collections.emptyList():Collections.singletonList(outputVolume);
	}

}
