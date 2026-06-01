package mclachlan.brewday.ui.swing.screens;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecipesScreenTest
{
	@Before
	public void assumeDisplay()
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
	}

	@Test
	public void addDuplicateRenameDeleteSaveUndoAndExport() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();
		AtomicInteger navRefresh = new AtomicInteger();
		RecipesScreen screen = createScreen(dbPort, dialog, dirty, navRefresh,
			new RecipesScreen.NoOpRenameHook(), new RecipesScreen.NoOpDeleteHook());

		dialog.nextNewRecipe = recipe("R1");
		invokeEdt(() -> screen.getAddAction().actionPerformed(null));
		assertEquals(1, dbPort.recipes.size());
		assertTrue(dirty.hasDirty());
		assertEquals(1, navRefresh.get());

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.renameResult = "R1Copy";
		invokeEdt(() -> screen.getDuplicateAction().actionPerformed(null));
		assertEquals(2, dbPort.recipes.size());
		assertEquals(2, navRefresh.get());

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.comingSoonCount = 0;
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(1, dialog.comingSoonCount);

		File csv = File.createTempFile("recipes-screen-test", ".csv");
		csv.deleteOnExit();
		dialog.exportFile = csv;
		invokeEdt(() -> screen.getExportAction().actionPerformed(null));
		assertFalse(Files.readAllLines(csv.toPath(), StandardCharsets.UTF_8).isEmpty());

		dialog.confirm = true;
		invokeEdt(() -> screen.getSaveAction().actionPerformed(null));
		assertEquals(1, dbPort.saveCount);
		assertFalse(dirty.hasDirty());
		assertEquals(3, navRefresh.get());

		dirty.markDirty("recipes");
		invokeEdt(() -> screen.getUndoAction().actionPerformed(null));
		assertEquals(1, dbPort.loadCount);
		assertFalse(dirty.hasDirty());
		assertEquals(4, navRefresh.get());
	}

	@Test
	public void hotkeysFilterAndTagCombo() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.recipes.put("A", tagged("A", "ipa"));
		dbPort.recipes.put("B", tagged("B", "lager"));
		AtomicInteger navRefresh = new AtomicInteger();
		RecipesScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService(), navRefresh,
			new RecipesScreen.NoOpRenameHook(), new RecipesScreen.NoOpDeleteHook());

		KeyStroke filterKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F);
		Object filterMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(filterKs);
		assertEquals("recipe.hotkey.filterCtrl", filterMap);

		invokeEdt(() -> screen.getFilterAction().actionPerformed(null));
		invokeEdt(() -> screen.getTagCombo().setSelectedItem("ipa"));
		invokeEdt(() -> screen.getFilterField().setText("B"));
		assertEquals(0, screen.getTable().getRowCount());

		invokeEdt(() -> screen.getFilterField().setText("A"));
		assertEquals(1, screen.getTable().getRowCount());
		assertEquals("A", screen.getTable().getValueAt(0, 0));
	}

	@Test
	public void dirtyRowsAreBold() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.recipes.put("A", recipe("A"));
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();
		AtomicInteger navRefresh = new AtomicInteger();
		RecipesScreen screen = createScreen(dbPort, dialog, dirty, navRefresh,
			new RecipesScreen.NoOpRenameHook(), new RecipesScreen.NoOpDeleteHook());

		assertEquals(Font.PLAIN, rowFontStyle(screen, 0));
		dirty.markDirty(dbPort.recipes.get("A"));
		invokeEdt(screen::refresh);
		assertEquals(Font.BOLD, rowFontStyle(screen, 0));
	}

	@Test
	public void renameAndDeleteInvokeHooks() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.recipes.put("Old", recipe("Old"));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.renameResult = "New";
		RecordingRenameHook renameHook = new RecordingRenameHook();
		RecordingDeleteHook deleteHook = new RecordingDeleteHook();
		AtomicInteger navRefresh = new AtomicInteger();
		RecipesScreen screen = createScreen(dbPort, dialog, new DirtyStateService(), navRefresh, renameHook, deleteHook);

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getRenameAction().actionPerformed(null));
		assertEquals("Old", renameHook.oldName);
		assertEquals("New", renameHook.newName);
		assertTrue(dbPort.recipes.containsKey("New"));

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.confirm = true;
		invokeEdt(() -> screen.getDeleteAction().actionPerformed(null));
		assertEquals("New", deleteHook.deletedName);
		assertFalse(dbPort.recipes.containsKey("New"));
	}

	private RecipesScreen createScreen(FakeDbPort dbPort, FakeDialogPort dialog, DirtyStateService dirty,
		AtomicInteger navRefresh, RecipesScreen.RenameHook renameHook, RecipesScreen.DeleteHook deleteHook) throws Exception
	{
		final RecipesScreen[] holder = new RecipesScreen[1];
		invokeEdt(() -> holder[0] = new RecipesScreen(null, dirty, navRefresh::incrementAndGet, dialog, dbPort, renameHook, deleteHook, null));
		return holder[0];
	}

	private int rowFontStyle(RecipesScreen screen, int row) throws Exception
	{
		final int[] style = new int[1];
		invokeEdt(() -> style[0] = screen.rowFontStyle(row));
		return style[0];
	}

	private static void invokeEdt(Runnable runnable) throws Exception
	{
		SwingUtilities.invokeAndWait(runnable);
	}

	private static Recipe recipe(String name)
	{
		return new Recipe(name);
	}

	private static Recipe tagged(String name, String tag)
	{
		Recipe r = new Recipe(name);
		r.getTags().add(tag);
		return r;
	}

	private static class RecordingRenameHook implements RecipesScreen.RenameHook
	{
		String oldName;
		String newName;

		@Override
		public void onRecipeRenamed(String oldName, String newName)
		{
			this.oldName = oldName;
			this.newName = newName;
		}
	}

	private static class RecordingDeleteHook implements RecipesScreen.DeleteHook
	{
		String deletedName;

		@Override
		public void onRecipeDeleted(String name)
		{
			this.deletedName = name;
		}
	}

	private static class FakeDbPort implements RecipesScreen.DbPort
	{
		private final Map<String, Recipe> recipes = new LinkedHashMap<>();
		private int saveCount;
		private int loadCount;

		@Override
		public Map<String, Recipe> recipes()
		{
			return recipes;
		}

		@Override
		public void saveAll()
		{
			saveCount++;
		}

		@Override
		public void loadAll()
		{
			loadCount++;
		}
	}

	private static class FakeDialogPort implements RecipesScreen.DialogPort
	{
		private Recipe nextNewRecipe;
		private String renameResult;
		private boolean confirm = true;
		private File exportFile;
		private int comingSoonCount;

		@Override
		public Recipe showNewRecipeDialog(javax.swing.JFrame parent)
		{
			return nextNewRecipe;
		}

		@Override
		public void showRecipeEditorComingSoon(javax.swing.JFrame parent)
		{
			comingSoonCount++;
		}

		@Override
		public String promptName(javax.swing.JFrame parent, String message, String title, String currentName)
		{
			return renameResult;
		}

		@Override
		public boolean confirm(javax.swing.JFrame parent, String message, String title)
		{
			return confirm;
		}

		@Override
		public File chooseExportFile(javax.swing.JFrame parent, File defaultFile)
		{
			return exportFile;
		}

		@Override
		public void writeRecipeCsv(File target, java.util.Collection<Recipe> recipes) throws IOException
		{
			List<String> lines = new ArrayList<>();
			lines.add("Name,Est OG,Est FG,Est ABV,IBU (Tinseth),Color");
			for (Recipe r : recipes)
			{
				lines.add(r.getName() + ",,,,,");
			}
			Files.write(target.toPath(), lines, StandardCharsets.UTF_8);
		}

		@Override
		public void writeRecipeReport(File target, java.util.Collection<Recipe> recipes) throws IOException
		{
			List<String> lines = new ArrayList<>();
			lines.add("# Packaged Beers");
			for (Recipe r : recipes)
			{
				lines.add("## " + r.getName());
			}
			Files.write(target.toPath(), lines, StandardCharsets.UTF_8);
		}

		@Override
		public void showError(javax.swing.JFrame parent, String message, String title)
		{
		}

		@Override
		public void showError(javax.swing.JFrame parent, Throwable throwable, String title)
		{
		}
	}
}
