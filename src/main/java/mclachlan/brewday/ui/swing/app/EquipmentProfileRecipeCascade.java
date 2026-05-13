package mclachlan.brewday.ui.swing.app;

import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.screens.EquipmentProfilesScreen;
import mclachlan.brewday.ui.swing.screens.RecipesScreen;

/**
 * Mirrors JFX {@code EquipmentProfilePane} cascade into {@link Recipe} equipment profile names.
 */
public class EquipmentProfileRecipeCascade implements EquipmentProfilesScreen.RenameHook, EquipmentProfilesScreen.DeleteHook
{
	private final RecipesScreen recipesScreen;
	private final DirtyStateService dirtyState;

	public EquipmentProfileRecipeCascade(RecipesScreen recipesScreen, DirtyStateService dirtyState)
	{
		this.recipesScreen = recipesScreen;
		this.dirtyState = dirtyState;
	}

	@Override
	public void onEquipmentProfileRenamed(String oldName, String newName)
	{
		Database db = Database.getInstance();
		for (Recipe recipe : db.getRecipes().values())
		{
			String ep = recipe.getEquipmentProfile();
			if (ep != null && ep.equalsIgnoreCase(oldName))
			{
				recipe.setEquipmentProfile(newName);
				dirtyState.markDirty(recipe, "recipes");
			}
		}
		if (recipesScreen != null)
		{
			recipesScreen.refresh();
		}
	}

	@Override
	public void onEquipmentProfileDeleted(String deletedName)
	{
		Database db = Database.getInstance();
		String fallback = db.getSettings().get(Settings.DEFAULT_EQUIPMENT_PROFILE);
		for (Recipe recipe : db.getRecipes().values())
		{
			String ep = recipe.getEquipmentProfile();
			if (ep != null && ep.equalsIgnoreCase(deletedName))
			{
				recipe.setEquipmentProfile(fallback);
				dirtyState.markDirty(recipe, "recipes");
			}
		}
		if (recipesScreen != null)
		{
			recipesScreen.refresh();
		}
	}
}
