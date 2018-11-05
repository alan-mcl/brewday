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
import mclachlan.brewday.database.json.JsonLoader;
import mclachlan.brewday.database.json.JsonSaver;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.*;

/**
 *
 */
public class ProcessRunner
{
	public static void main(String[] args) throws Exception
	{
		JsonLoader loader = new JsonLoader();

		Recipe recipe = getRecipe(loader);

		recipe.run();

		if (recipe.getErrors().size() > 0)
		{
			System.out.println("Errors:");
			for (String error : recipe.getErrors())
			{
				System.out.println(error);
			}
			System.out.println();
		}
		if (recipe.getWarnings().size() > 0)
		{
			System.out.println("Warnings:");
			for (String warning : recipe.getWarnings())
			{
				System.out.println(warning);
			}
			System.out.println();
		}

		for (ProcessStep s : recipe.getSteps())
		{
			System.out.println("["+s.getName()+"]");
			Collection<String> outputVolumes = s.getOutputVolumes();
//			s.apply(recipe.getVolumes(), recipe, log);
			for (String vs : outputVolumes)
			{
				System.out.println(recipe.getVolumes().getVolume(vs));
			}
		}

		System.out.println("Saving to db...");

		Map<String, Recipe> map = new HashMap<String, Recipe>();
		map.put(recipe.getName(), recipe);

		JsonSaver saver = new JsonSaver();
		saver.saveRecipes(map);

		System.out.println("Loading from db...");

		Map<String, Recipe> stringRecipeMap = loader.loadRecipes();
		System.out.println("stringRecipeMap = [" + stringRecipeMap + "]");
	}

	public static Recipe getRecipe(JsonLoader loader)
	{
		List<ProcessStep> p = new ArrayList<ProcessStep>();

		Map<String, Fermentable> ferms = loader.getReferenceFermentables();
		Map<String, Hop> hops = loader.getReferenceHops();
		Map<String, Yeast> yeasts = loader.getReferenceYeasts();

		FermentableAddition baseMalt = new FermentableAddition(ferms.get("Pale Malt (2 Row) UK"), 6000, 60);

		WaterAddition mashWater = new WaterAddition("Mash Water", 15000, 70, 60);
		WaterAddition spargeWater = new WaterAddition("Sparge Water 1", 10000, 75, 0);

		List<IngredientAddition> hopCharges = new ArrayList<IngredientAddition>();
		HopAddition cascade20g = new HopAddition(hops.get("Cascade"), 20, 60);
		HopAddition citra20g = new HopAddition(hops.get("Citra"), 20, 20);
		hopCharges.add(cascade20g);
		hopCharges.add(citra20g);

		List<IngredientAddition> mashAdditions = new ArrayList<IngredientAddition>();
		mashAdditions.add(baseMalt);
		mashAdditions.add(mashWater);

		YeastAddition yeast = new YeastAddition(yeasts.get("Safale American"), 11, 14);

		Volumes brew = new Volumes();

		p.add(new Mash("single infusion mash", "my mash desc", mashAdditions, "The Mash", "First Runnings", 60D, 20D, 3000D));

		p.add(new BatchSparge("batch sparge #1", "my batch sparge", "The Mash", "First Runnings", "Pre-boil", "sparge 1 runnings", "Post Sparge Mash", spargeWater));

		p.add(new Boil("boil 60 min", "60 minute rolling boil", "Pre-boil", "Post-boil", hopCharges, 60D));
		p.add(new Stand("hop stand", "30 minute hop stand", "Post-boil", "Post hop stand", 30D));
		p.add(new Dilute("dilute to 30l", "top up and chill", "Post hop stand", "Post dilution", 30000, 5));
		p.add(new Cool("cool to 20C", "drop to fermentation temp", "Post dilution", "Post cool", 20));
		p.add(new Ferment("ferment to 1010", "primary fermentation", "Post cool", "Post fermentation", 20, yeast));
		p.add(new Cool("cold crash", "cold crash prior to packaging", "Post fermentation", "Post Cold Crash", 1));
		p.add(new PackageStep("package", "package", "Post Cold Crash", "My Pale Ale", 500));


		return new Recipe("Test Batch 1", p, brew);
	}
}
