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
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.recipe.WaterAddition;

/**
 *
 */
public class BatchSparge extends ProcessStep
{
	private String spargeWaterVolume;
	private String mashVolume;
	private String wortVolume;
	private String outputWortVolume, outputMashVolume;

	/*-------------------------------------------------------------------------*/
	public BatchSparge()
	{
	}

	/*-------------------------------------------------------------------------*/
	public BatchSparge(
		String name,
		String description,
		String mashVolume,
		String spargeWaterVolume,
		String wortVolume,
		String outputWortVolume,
		String outputMashVolume)
	{
		super(name, description, Type.BATCH_SPARGE);
		this.mashVolume = mashVolume;
		this.wortVolume = wortVolume;
		this.outputWortVolume = outputWortVolume;
		this.spargeWaterVolume = spargeWaterVolume;
		this.outputMashVolume = outputMashVolume;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Constructor that sets the fields appropriately for the given batch.
	 */
	public BatchSparge(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.BATCH_SPARGE), "Batch Sparge", Type.BATCH_SPARGE);

		this.mashVolume = recipe.getVolumes().getVolumeByType(Volume.Type.MASH);
		this.wortVolume = recipe.getVolumes().getVolumeByType(Volume.Type.WORT);
		this.spargeWaterVolume = recipe.getVolumes().getVolumeByType(Volume.Type.WATER);

		this.outputWortVolume = getName()+" output";
		this.outputMashVolume = getName()+" mash output";
	}

	/*-------------------------------------------------------------------------*/
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
		MashVolume mash = (MashVolume)volumes.getVolume(mashVolume);

		double totalGristWeight = mash.getFermentables().getCombinedWeight();
		DensityUnit mashExtract = mash.getGravity();
		double absorbedWater = Equations.calcAbsorbedWater(totalGristWeight);

		double totalMashWater = absorbedWater + mash.getTunDeadSpace();

		// model the batch sparge as a dilution of the extract remaining

		DensityUnit spargeGravity = Equations.calcGravityWithVolumeChange(
			totalMashWater,
			mashExtract,
			totalMashWater + spargeWater.getVolume());

		double volumeOut = input.getVolume() + spargeWater.getVolume();

		DensityUnit gravityOut = Equations.calcCombinedGravity(
			input.getVolume(),
			input.getGravity(),
			spargeWater.getVolume(),
			spargeGravity);

		double tempOut =
			Equations.calcNewFluidTemperature(
				input.getVolume(),
				input.getTemperature(),
				spargeWater.getVolume(),
				spargeWater.getTemperature());

		// todo: incorrect, fix for sparging!
		double colourOut = input.getColour();

		// output the lautered mash volume, in case it needs to be input into further batch sparge steps
		volumes.addVolume(
			outputMashVolume,
			new MashVolume(
				mash.getVolume(),
				mash.getFermentables(),
				mash.getWater(),
				mash.getTemperature(),
				spargeGravity,
				mash.getColour(), // todo replace with sparge colour
				mash.getTunDeadSpace()));

		// output the combined worts
		volumes.addVolume(
			outputWortVolume,
			new WortVolume(
				volumeOut,
				tempOut,
				input.getFermentability(),
				gravityOut,
				0D,
				colourOut,
				0D));
	}

	/*-------------------------------------------------------------------------*/
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
		return Arrays.asList(outputWortVolume);
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

	public String getOutputWortVolume()
	{
		return outputWortVolume;
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

	public void setOutputWortVolume(String outputWortVolume)
	{
		this.outputWortVolume = outputWortVolume;
	}

	public String getOutputMashVolume()
	{
		return outputMashVolume;
	}
}
