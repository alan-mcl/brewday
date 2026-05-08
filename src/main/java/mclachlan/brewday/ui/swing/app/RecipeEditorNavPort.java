package mclachlan.brewday.ui.swing.app;

/**
 * Opens the recipe editor from {@code RecipesScreen} (modal dialog in the live app).
 */
public interface RecipeEditorNavPort
{
	void openRecipeEditor(String recipeName);
}
