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
import mclachlan.brewday.database.Database;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.HopAddition;

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
		List<ProcessStep> p = new ArrayList<ProcessStep>();

		Map<String, Fermentable> ferms = Database.getInstance().getReferenceFermentables();
		Map<String, Hop> hops = Database.getInstance().getReferenceHops();

		FermentableAddition baseMalt = new FermentableAddition(ferms.get("Pale Malt (2 Row) UK"), 6000);
		IngredientAddition<FermentableAddition> grainBill = new IngredientAddition<FermentableAddition>(baseMalt);

		Water mashWater = new Water(15000, 70);
		Water spargeWater = new Water(10000, 75);

		HopAddition cascade20g = new HopAddition(hops.get("Cascade"), 20);
		IngredientAddition<HopAddition> hopCharge60 = new IngredientAddition<HopAddition>(cascade20g);

		Volumes brew = new Volumes();
		brew.addInputVolume("Grain Bill 1", grainBill);
		brew.addInputVolume("Mash Water", mashWater);
		brew.addInputVolume("Sparge Water 1", spargeWater);
		brew.addInputVolume("Hop Charge 60m", hopCharge60);

		p.add(new SingleInfusionMash("single infusion mash", "my mash desc", "Grain Bill 1", "Mash Water", "The Mash", 60D, 66));
		p.add(new MashOut("mash out, drain", "gather first runnings", "The Mash", "First Runnings", 3));

		p.add(new BatchSparge("batch sparge", "my batch sparge", "The Mash", "Sparge Water 1", "First Runnings", "Pre-boil"));

		p.add(new Boil("boil 60 min", "60 minute rolling boil", "Pre-boil", "Post-boil", "Hop Charge 60m", 60D));
		p.add(new Stand("hop stand", "30 minute hop stand", "Post-boil", "Post hop stand", 30D));
		p.add(new Dilute("dilute to 30l", "top up and chill", "Post hop stand", "Post dilution", 30000, 5));
		p.add(new Cool("cool to 20C", "drop to fermentation temp", "Post dilution", "Post cool", 20));
		p.add(new Ferment("ferment to 1010", "primary fermentation", "Post cool", "My Pale Ale", 10));
		p.add(new Cool("cold crash", "cold crash prior to packaging", "My Pale Ale", "Post Cold Crash", 1));


		return new Batch("Test Batch 1", p, brew);
	}
}
