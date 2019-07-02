package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

public interface IProcessStep
{
	/*-------------------------------------------------------------------------*/
	/**
	 * Apply this process step to the current recipe state.
	 */
	void apply(Volumes volumes, Recipe recipe, ErrorsAndWarnings log);

	/*-------------------------------------------------------------------------*/
	/**
	 * Set up the output volumes, no actual processing
	 */
	void dryRun(Recipe recipe, ErrorsAndWarnings log);

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
