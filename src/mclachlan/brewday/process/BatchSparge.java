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
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class BatchSparge extends ProcessStep
{
	private String spargeWaterVol;

	public BatchSparge(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		String spargeWaterVol)
	{
		super(name, description, inputVolume, outputVolume);
		this.spargeWaterVol = spargeWaterVol;
	}

	@Override
	public java.util.List<String> apply(Volumes volumes)
	{
		WortVolume input = (WortVolume)getInputVolume(volumes);
		Water spargeWater = (Water)volumes.getVolume(spargeWaterVol);

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
			getOutputVolume(),
			new WortVolume(
				volumeOut,
				tempOut,
				gravityOut,
				0D,
				colourOut,
				0D));

		ArrayList<String> result = new ArrayList<String>();
		result.add(getOutputVolume());
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Batch sparge with '%s'", spargeWaterVol);
	}

	public String getSpargeWaterVolume()
	{
		return spargeWaterVol;
	}
}
