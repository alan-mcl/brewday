package mclachlan.brewday.ui.swing.screens;

import java.awt.GraphicsEnvironment;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.RecipeEditorNavPort;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RecipesScreenEditNavTest
{
	@Test
	public void editInvokesRecipeEditorNavPortWithRecipeName() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		AtomicReference<String> opened = new AtomicReference<>();
		RecipeEditorNavPort nav = new RecipeEditorNavPort()
		{
			@Override
			public void openRecipeEditor(String recipeName)
			{
				opened.set(recipeName);
			}

		};

		FakeDbPort db = new FakeDbPort();
		db.recipes.put("NavTestR", new Recipe("NavTestR"));
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();

		final RecipesScreen[] holder = new RecipesScreen[1];
		SwingUtilities.invokeAndWait(() ->
			holder[0] = new RecipesScreen(new JFrame(), dirty, () -> {}, dialog, db,
				new RecipesScreen.NoOpRenameHook(), new RecipesScreen.NoOpDeleteHook(), nav));
		RecipesScreen screen = holder[0];

		SwingUtilities.invokeAndWait(() ->
		{
			screen.getTable().setRowSelectionInterval(0, 0);
			screen.getEditAction().actionPerformed(null);
		});

		assertEquals("NavTestR", opened.get());
		assertNull(dialog.comingSoonTarget);
	}

	private static final class FakeDbPort implements RecipesScreen.DbPort
	{
		private final Map<String, Recipe> recipes = new LinkedHashMap<>();

		@Override
		public Map<String, Recipe> recipes()
		{
			return recipes;
		}

		@Override
		public void saveAll()
		{
		}

		@Override
		public void loadAll()
		{
		}
	}

	private static final class FakeDialogPort implements RecipesScreen.DialogPort
	{
		private javax.swing.JFrame comingSoonTarget;

		@Override
		public Recipe showNewRecipeDialog(javax.swing.JFrame parent)
		{
			return null;
		}

		@Override
		public void showRecipeEditorComingSoon(javax.swing.JFrame parent)
		{
			this.comingSoonTarget = parent;
		}

		@Override
		public String promptName(javax.swing.JFrame parent, String message, String title, String currentName)
		{
			return null;
		}

		@Override
		public boolean confirm(javax.swing.JFrame parent, String message, String title)
		{
			return false;
		}

		@Override
		public java.io.File chooseExportFile(javax.swing.JFrame parent, java.io.File defaultFile)
		{
			return null;
		}

		@Override
		public void writeRecipeCsv(java.io.File target, java.util.Collection<Recipe> recipes)
		{
		}

		@Override
		public void showError(javax.swing.JFrame parent, String message, String title)
		{
		}
	}
}
