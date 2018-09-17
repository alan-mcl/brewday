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

/**
 * Creates and output volume for this batch.
 */
public class Package extends FluidVolumeProcessStep
{
	/** packaging loss in ml */
	private double packagingLoss;

	public Package(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		double packagingLoss)
	{
		super(name, description, inputVolume, outputVolume);
		this.setOutputVolume(outputVolume);
		this.packagingLoss = packagingLoss;
	}

	@Override
	public List<String> apply(Volumes v)
	{
		FluidVolume input = (FluidVolume)getInputVolume(v);

		double volumeOut = input.getVolume() - packagingLoss;

		double gravityOut = input.getGravity();

		double tempOut = input.getTemperature();

		// todo: carbonation change in ABV
		double abvOut = input.getAbv();

		double colourOut = input.getColour();

		FluidVolume volOut;
		if (input instanceof WortVolume)
		{
			volOut = new WortVolume(
				volumeOut,
				tempOut,
				gravityOut,
				abvOut,
				colourOut,
				input.getBitterness());
		}
		else if (input instanceof BeerVolume)
		{
			volOut = new BeerVolume(
				volumeOut,
				tempOut,
				gravityOut,
				abvOut,
				colourOut,
				input.getBitterness());
		}
		else
		{
			throw new BrewdayException("Invalid volume type "+input);
		}

		v.addOutputVolume(getOutputVolume(), volOut);

		ArrayList<String> result = new ArrayList<String>();
		result.add(getOutputVolume());
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Package '%s'", getOutputVolume());
	}
}
