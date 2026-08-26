package mclachlan.brewday.ui.swing.screens;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.EquipmentProfileRecipeCascade;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EquipmentRecipeCascadeTest
{
	@Test
	public void renameEquipmentUpdatesMatchingRecipes() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		Database db = Database.getInstance();
		db.loadAll();
		final String epOld = "ZZ_SwingCascadeEpOld";
		final String epNew = "ZZ_SwingCascadeEpNew";
		final String recipeName = "ZZ_SwingCascadeRecipe";
		try
		{
			db.getEquipmentProfiles().put(epOld, new EquipmentProfile(epOld));
			db.getEquipmentProfiles().put(epNew, new EquipmentProfile(epNew));
			Recipe r = new Recipe(recipeName);
			r.setEquipmentProfile(epOld);
			db.getRecipes().put(recipeName, r);

			DirtyStateService dirty = new DirtyStateService();
			RecipesScreen recipes = new RecipesScreen(new JFrame(), dirty, () -> {});
			EquipmentProfileRecipeCascade cascade = new EquipmentProfileRecipeCascade(recipes, dirty);
			invokeEdt(() -> cascade.onEquipmentProfileRenamed(epOld, epNew));

			assertEquals(epNew, db.getRecipes().get(recipeName).getEquipmentProfile());
			assertTrue(dirty.isDirty(r));
		}
		finally
		{
			db.getRecipes().remove(recipeName);
			db.getEquipmentProfiles().remove(epOld);
			db.getEquipmentProfiles().remove(epNew);
		}
	}

	@Test
	public void deleteEquipmentResetsRecipeToDefaultProfile() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		Database db = Database.getInstance();
		db.loadAll();
		final String epDel = "ZZ_SwingCascadeEpDel";
		final String recipeName = "ZZ_SwingCascadeRecipeDel";
		try
		{
			db.getEquipmentProfiles().put(epDel, new EquipmentProfile(epDel));
			Recipe r = new Recipe(recipeName);
			r.setEquipmentProfile(epDel);
			db.getRecipes().put(recipeName, r);
			String expectedDefault = db.getSettings().get(Settings.DEFAULT_EQUIPMENT_PROFILE);

			DirtyStateService dirty = new DirtyStateService();
			RecipesScreen recipes = new RecipesScreen(new JFrame(), dirty, () -> {});
			EquipmentProfileRecipeCascade cascade = new EquipmentProfileRecipeCascade(recipes, dirty);
			invokeEdt(() -> cascade.onEquipmentProfileDeleted(epDel));

			assertEquals(expectedDefault, db.getRecipes().get(recipeName).getEquipmentProfile());
			assertTrue(dirty.isDirty(r));
		}
		finally
		{
			db.getRecipes().remove(recipeName);
			db.getEquipmentProfiles().remove(epDel);
		}
	}

	private static void invokeEdt(Runnable runnable) throws Exception
	{
		SwingUtilities.invokeAndWait(runnable);
	}
}
