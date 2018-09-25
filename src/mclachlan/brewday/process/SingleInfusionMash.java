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
import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.Convert;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.FermentableAdditionList;

/**
 *
 */
public class SingleInfusionMash extends ProcessStep
{
	private String outputMashVolume;
	private String grainBillVol;
	private String waterVol;

	/** duration in minutes */
	private double duration;

	/** grain volume temp in C */
	private double grainTemp;

	// calculated from strike water
	private double mashTemp;

	public SingleInfusionMash(
		String name,
		String description,
		String grainBillVol,
		String waterVol,
		String outputMashVolume,
		double duration,
		double grainTemp)
	{
		super(name, description, Type.MASH_IN);
		this.outputMashVolume = outputMashVolume;

		this.grainBillVol = grainBillVol;
		this.waterVol = waterVol;
		this.duration = duration;
		this.grainTemp = grainTemp;
	}

	public SingleInfusionMash(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.MASH_IN), "Single infusion mash", Type.MASH_IN);

		grainBillVol = recipe.getVolumes().getVolumeByType(Volume.Type.FERMENTABLES);
		waterVol = recipe.getVolumes().getVolumeByType(Volume.Type.WATER);
		duration = 60;
		grainTemp = 20;

		outputMashVolume = getName()+" mash vol";
	}

	@Override
	public List<String> apply(Volumes v, Recipe recipe)
	{
		FermentableAdditionList grainBill = (FermentableAdditionList)v.getVolume(grainBillVol);
		WaterAddition strikeWater = (WaterAddition)v.getVolume(waterVol);

		double grainWeight = grainBill.getCombinedWeight();

		mashTemp = Equations.calcMashTemp(grainBill, strikeWater, grainTemp);

		// todo: account for different grains in the grain bill
		double volumeOut = Equations.calcMashVolume(grainWeight, strikeWater.getVolume());

		// source: https://byo.com/article/hitting-target-original-gravity-and-volume-advanced-homebrewing/
		double extractPoints = 0D;
		for (FermentableAddition g : grainBill.getIngredients())
		{
			extractPoints += Convert.gramsToLbs(g.getWeight()) * g.getFermentable().getExtractPotential();
		}

		// todo: externalise mash efficiency
		double actualExtract = extractPoints * Const.MASH_EFFICIENCY;

		double gravityOut = actualExtract / Convert.mlToGallons(volumeOut);

		double colourOut = Equations.calcSrmMoreyFormula(grainBill, volumeOut);

		v.addVolume(
			outputMashVolume,
			new MashVolume(
				volumeOut,
				grainBill,
				strikeWater,
				mashTemp,
				gravityOut,
				colourOut));

		ArrayList<String> result = new ArrayList<String>();
		result.add(outputMashVolume);
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		WaterAddition w = (WaterAddition)v.getVolume(waterVol);

		return String.format("Mash: single infusion %.1fL at %.1fC", w.getVolume()/1000, mashTemp);
	}

	public String getOutputMashVolume()
	{
		return outputMashVolume;
	}

	public String getGrainBillVol()
	{
		return grainBillVol;
	}

	public String getWaterVol()
	{
		return waterVol;
	}

	public double getDuration()
	{
		return duration;
	}

	public double getGrainTemp()
	{
		return grainTemp;
	}

	public void setGrainBillVolume(String grainBillVolume)
	{
		this.grainBillVol = grainBillVolume;
	}

	public void setWaterVolume(String waterVolume)
	{
		this.waterVol = waterVolume;
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
		return Arrays.asList(grainBillVol, waterVol);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		return Arrays.asList(outputMashVolume);
	}
}
