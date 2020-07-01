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

package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.db.v2.V2Utils;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class RecipeSerialiser implements V2SerialiserMap<Recipe>
{
	private StepSerialiser stepSerialiser = new StepSerialiser();

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(Recipe recipe)
	{
		Map result = new HashMap();

		result.put("name", recipe.getName());
		result.put("equipmentProfile", recipe.getEquipmentProfile());
		result.put("steps", V2Utils.serialiseList(recipe.getSteps(), stepSerialiser));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Recipe fromMap(Map<String, ?> map)
	{
		String name = (String)map.get("name");
		String equipmentProfile = (String)map.get("equipmentProfile");
		List<ProcessStep> steps = V2Utils.deserialiseList((List)map.get("steps"), stepSerialiser);

		return new Recipe(name, equipmentProfile, steps);
	}
}
