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

package mclachlan.brewday.test;

import java.util.*;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.process.*;

/**
 *
 */
public class ProcessRunner
{
	public static void main(String[] args) throws Exception
	{
		Batch batch = getBatch();

		for (ProcessStep s : batch.getSteps())
		{
			System.out.println("["+s.getName()+"]");
			List<String> outputVolumes = s.apply(batch.getVolumes());
			for (String vs : outputVolumes)
			{
				System.out.println(batch.getVolumes().getVolume(vs));
			}
		}
	}

	public static Batch getBatch()
	{
		List<ProcessStep> process = new ArrayList<ProcessStep>();

		ArrayList<Grain> grains = new ArrayList<Grain>();
		grains.add(new Grain(34, 10, 6000));
		GrainBill grainBill = new GrainBill(grains);
		Water mashWater = new Water(15000, 70);
		Water spargeWater = new Water(10000, 75);
		ArrayList<Hop> hops = new ArrayList<Hop>();
		hops.add(new Hop(0.05, 20));
		HopAddition hopAddition = new HopAddition(hops);

		Volumes brew = new Volumes();
		brew.addInputVolume("Grain Bill 1", grainBill);
		brew.addInputVolume("Mash Water", mashWater);
		brew.addInputVolume("Sparge Water 1", spargeWater);
		brew.addInputVolume("Hop Charge 60m", hopAddition);

		process.add(new MashIn("single infusion mash", null, "The Mash", "Grain Bill 1", "Mash Water", 66));
		process.add(new MashOut("mash out, drain", null, "The Mash", "First Runnings", 3));

		process.add(new BatchSparge("batch sparge", null, "First Runnings", "Pre-boil", "Sparge Water 1"));

		process.add(new Boil("boil 60m", null, "Pre-boil", "Post-boil", "Hop Charge 60m", 60D));
		process.add(new Stand("hop stand 30m", null, "Post-boil", "Post hop stand", 30D));
		process.add(new Dilute("dilute to 30l", null, "Post hop stand", "Post dilution", 30000, 5));
		process.add(new Cool("cool to 20C", null, "Post dilution", "Post cool", 20));
		process.add(new Ferment("ferment to 1010", null, "Post cool", "My Pale Ale", 10));
		process.add(new Cool("cold crash", null, "My Pale Ale", "Post Cold Crash", 1));


		return new Batch(process, brew);
	}
}
