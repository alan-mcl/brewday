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

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

public interface IProcessStep
{
	/*-------------------------------------------------------------------------*/
	/**
	 * Apply this process step to the current recipe state as represented by the
	 * volumes parameter.
	 *
	 * @param volumes
	 * 	the current state of volumes in this recipe or batch.
	 * @param equipmentProfile
	 * 	the equipment in use
	 * @param log
	 * 	a log to append errors and warning to
	 */
	void apply(
		Volumes volumes,
		EquipmentProfile equipmentProfile,
		ProcessLog log);

	/*-------------------------------------------------------------------------*/
	/**
	 * Set up the output volumes, no actual processing
	 */
	void dryRun(Recipe recipe, ProcessLog log);

	/*-------------------------------------------------------------------------*/
	String describe(Volumes v);

	/*-------------------------------------------------------------------------*/
	String getName();

	/*-------------------------------------------------------------------------*/
	String getDescription();

	/*-------------------------------------------------------------------------*/
	ProcessStep.Type getType();

	/*-------------------------------------------------------------------------*/
	List<IngredientAddition> getIngredients();

	/*-------------------------------------------------------------------------*/
	void setIngredients(
		List<IngredientAddition> ingredients);

	/*-------------------------------------------------------------------------*/
	void addIngredientAddition(IngredientAddition item);

	/*-------------------------------------------------------------------------*/
	void addIngredientAdditions(List<IngredientAddition> additions);

	/*-------------------------------------------------------------------------*/
	void removeIngredientAddition(IngredientAddition item);
}
