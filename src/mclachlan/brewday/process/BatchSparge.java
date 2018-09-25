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
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class BatchSparge extends ProcessStep
{
	private String spargeWaterVolume;
	private String mashVolume;
	private String wortVolume;
	private String outputVolume;

	public BatchSparge(
		String name,
		String description,
		String mashVolume,
		String spargeWaterVolume,
		String wortVolume,
		String outputVolume)
	{
		super(name, description, Type.BATCH_SPARGE);
		this.mashVolume = mashVolume;
		this.wortVolume = wortVolume;
		this.outputVolume = outputVolume;
		this.spargeWaterVolume = spargeWaterVolume;
	}

	/**
	 * Constructor that sets the fields appropriately for the given batch.
	 */
	public BatchSparge(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.BATCH_SPARGE), "Batch Sparge", Type.BATCH_SPARGE);

		this.mashVolume = recipe.getVolumes().getVolumeByType(Volume.Type.MASH);
		this.wortVolume = recipe.getVolumes().getVolumeByType(Volume.Type.WORT);
		this.spargeWaterVolume = recipe.getVolumes().getVolumeByType(Volume.Type.WATER);

		this.outputVolume = getName()+" output";
	}

	@Override
	public void apply(Volumes volumes, Recipe recipe,
		ErrorsAndWarnings log)
	{
		if (!volumes.contains(wortVolume))
		{
			log.addError("volume does not exist ["+wortVolume+"]");
			return;
		}
		if (!volumes.contains(spargeWaterVolume))
		{
			log.addError("volume does not exist ["+spargeWaterVolume+"]");
			return;
		}

		WortVolume input = (WortVolume)(volumes.getVolume(wortVolume));
		WaterAddition spargeWater = (WaterAddition)volumes.getVolume(spargeWaterVolume);

		double volumeOut = input.getVolume() + spargeWater.getVolume();

		double tempOut =
			Equations.calcNewFluidTemperature(
				input.getVolume(),
				input.getTemperature(),
				spargeWater.getVolume(),
				spargeWater.getTemperature());

		// todo: incorrect, fix for sparging!
		double gravityOut = input.getGravity();

		// todo: incorrect, fix for sparging!
		double colourOut = input.getColour();

		volumes.addVolume(
			outputVolume,
			new WortVolume(
				volumeOut,
				tempOut,
				gravityOut,
				0D,
				colourOut,
				0D));
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Batch sparge");
	}

	@Override
	public Collection<String> getInputVolumes()
	{
		return Arrays.asList(spargeWaterVolume, mashVolume, wortVolume);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		return Arrays.asList(outputVolume);
	}

	public String getSpargeWaterVolume()
	{
		return spargeWaterVolume;
	}

	public String getMashVolume()
	{
		return mashVolume;
	}

	public String getWortVolume()
	{
		return wortVolume;
	}

	public String getOutputVolume()
	{
		return outputVolume;
	}

	public void setSpargeWaterVolume(String spargeWaterVolume)
	{
		this.spargeWaterVolume = spargeWaterVolume;
	}

	public void setMashVolume(String mashVolume)
	{
		this.mashVolume = mashVolume;
	}

	public void setWortVolume(String wortVolume)
	{
		this.wortVolume = wortVolume;
	}

	public void setOutputVolume(String outputVolume)
	{
		this.outputVolume = outputVolume;
	}
}
