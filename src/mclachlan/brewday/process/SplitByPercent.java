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
import mclachlan.brewday.recipe.Recipe;

public class SplitByPercent extends FluidVolumeProcessStep
{
	/** the proportion of the first volume from, second volume is the remainder */
	private double outputPercent;

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
		double outputPercent, String outputVolume2)
	{
		super(name, description, Type.SPLIT_BY_PERCENT,  inputVolume, outputVolume);
		this.outputPercent = outputPercent;
		this.outputVolume2 = outputVolume2;
	}

	/*-------------------------------------------------------------------------*/
	public SplitByPercent(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.SPLIT_BY_PERCENT), "Split (%)", Type.SPLIT_BY_PERCENT, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT));
		setOutputVolume(getName()+" output #1");

		this.outputVolume2 = getName() + " output #2";
		this.outputPercent = 0.5D;
	}

	/*-------------------------------------------------------------------------*/
	public SplitByPercent(SplitByPercent step)
	{
		super(step.getName(), step.getDescription(), Type.SPLIT_BY_PERCENT, step.getInputVolume(), step.getOutputVolume());

		this.outputPercent = step.outputPercent;
		this.outputVolume2 = step.outputVolume2;
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public void dryRun(Recipe recipe, ErrorsAndWarnings log)
	{
		super.dryRun(recipe, log);
		recipe.getVolumes().addVolume(outputVolume2, recipe.getVolumes().getVolume(getInputVolume()));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, Recipe recipe, ErrorsAndWarnings log)
	{
		if (!validateInputVolume(volumes, log))
		{
			return;
		}

		FluidVolume inputVolume = (FluidVolume)(volumes.getVolume(this.getInputVolume()));

		double volume1Out = inputVolume.getVolume() * outputPercent;
		double volume2Out = inputVolume.getVolume() - volume1Out;

		FluidVolume v1, v2;

		if (inputVolume instanceof WortVolume)
		{
			v1 = new WortVolume(
				volume1Out,
				inputVolume.getTemperature(),
				((WortVolume)inputVolume).getFermentability(),
				inputVolume.getGravity(),
				inputVolume.getAbv(),
				inputVolume.getColour(),
				inputVolume.getBitterness());

			v2 = new WortVolume(
				volume2Out,
				inputVolume.getTemperature(),
				((WortVolume)inputVolume).getFermentability(),
				inputVolume.getGravity(),
				inputVolume.getAbv(),
				inputVolume.getColour(),
				inputVolume.getBitterness());
		}
		else if (inputVolume instanceof BeerVolume)
		{
			v1 = new BeerVolume(
				volume1Out,
				inputVolume.getTemperature(),
				((BeerVolume)inputVolume).getOriginalGravity(),
				inputVolume.getGravity(),
				inputVolume.getAbv(),
				inputVolume.getColour(),
				inputVolume.getBitterness());

			v2 = new BeerVolume(
				volume2Out,
				inputVolume.getTemperature(),
				((BeerVolume)inputVolume).getOriginalGravity(),
				inputVolume.getGravity(),
				inputVolume.getAbv(),
				inputVolume.getColour(),
				inputVolume.getBitterness());
		}
		else
		{
			throw new BrewdayException("Invalid volume type: " + inputVolume);
		}

		volumes.addVolume(getOutputVolume(), v1);
		volumes.addVolume(getOutputVolume2(), v2);
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Split (%%)");
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		List<String> result = new ArrayList<String>(super.getOutputVolumes());
		result.add(outputVolume2);
		return result;
	}

	public String getOutputVolume2()
	{
		return outputVolume2;
	}

	public double getSplitPercent()
	{
		return outputPercent;
	}

	public void setOutputPercent(double outputPercent)
	{
		this.outputPercent = outputPercent;
	}
}
