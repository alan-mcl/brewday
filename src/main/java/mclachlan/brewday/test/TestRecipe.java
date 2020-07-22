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

package mclachlan.brewday.test;

import java.util.*;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class TestRecipe
{
	private static void testSort()
	{
		Recipe recipe = new Recipe();

		ProcessStep[] steps =
			{
				new Mash(null, null, null, "mash_out", null, null),
				new Lauter(null, null, "mash_inf_out", "lautered_mash", "lauter_out"),
				new BatchSparge(null, null, "lautered_mash", "lauter_out", "sparge_out", "sparge_runnings", "sparge_mash", null),
				new Boil(null, null, "sparge_out", "boil_out", null, null),
				new Cool(null, null, "boil_out", "cool_out", null),
				new Ferment(null, null, "cool_out", "ferment_out", null, null, null),
				new PackageStep(null, null, null, "ferment_out", "package_out", null, null),
				new MashInfusion(null, null, "mash_out", "mash_inf_out", null, null)
			};

		recipe.setSteps(Arrays.asList(steps));

		ProcessLog log = new ProcessLog();
		recipe.sortSteps(log);
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		testSort();
	}
}
