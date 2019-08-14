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
