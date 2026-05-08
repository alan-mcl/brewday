package mclachlan.brewday.ui.swing.app;

import java.awt.GraphicsEnvironment;
import java.awt.Font;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.screens.RecipesScreen;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwingAppFrameNavigationTest
{
	@Test
	public void selectScreenRoutesAndCallsLifecycle() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Database.getInstance().loadAll();

		final TestableSwingAppFrame[] holder = new TestableSwingAppFrame[1];
		invokeEdt(() -> holder[0] = new TestableSwingAppFrame());

		TestableSwingAppFrame frame = holder[0];
		invokeEdt(() -> frame.selectScreen(ScreenKey.INVENTORY));

		CountingScreen screen = frame.screen(ScreenKey.INVENTORY);
		assertEquals(ScreenKey.INVENTORY, frame.getCurrentScreenKey());
		assertEquals(1, screen.activations);
		assertEquals(1, screen.refreshes);

		invokeEdt(frame::dispose);
	}

	@Test
	public void helpAboutHotkeyActionRoutesToAbout() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Database.getInstance().loadAll();

		final TestableSwingAppFrame[] holder = new TestableSwingAppFrame[1];
		invokeEdt(() -> holder[0] = new TestableSwingAppFrame());

		TestableSwingAppFrame frame = holder[0];
		invokeEdt(() ->
		{
			Action action = frame.getRootPane().getActionMap().get("helpAbout");
			action.actionPerformed(null);
		});

		CountingScreen screen = frame.screen(ScreenKey.ABOUT);
		assertEquals(ScreenKey.ABOUT, frame.getCurrentScreenKey());
		assertEquals(1, screen.activations);
		assertEquals(1, screen.refreshes);

		invokeEdt(frame::dispose);
	}

	@Test
	public void initialSelectionDefaultsToRecipes() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Database.getInstance().loadAll();

		final TestableSwingAppFrame[] holder = new TestableSwingAppFrame[1];
		invokeEdt(() -> holder[0] = new TestableSwingAppFrame());
		TestableSwingAppFrame frame = holder[0];

		assertEquals(ScreenKey.RECIPES, frame.getCurrentScreenKey());
		assertEquals(1, frame.screen(ScreenKey.RECIPES).activations);
		invokeEdt(frame::dispose);
	}

	@Test
	public void refreshHotkeyRefreshesCurrentScreen() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Database.getInstance().loadAll();

		final TestableSwingAppFrame[] holder = new TestableSwingAppFrame[1];
		invokeEdt(() -> holder[0] = new TestableSwingAppFrame());
		TestableSwingAppFrame frame = holder[0];

		invokeEdt(() -> frame.selectScreen(ScreenKey.INVENTORY));
		int before = frame.screen(ScreenKey.INVENTORY).refreshes;
		invokeEdt(() ->
		{
			Action action = frame.getRootPane().getActionMap().get("refreshCurrent");
			action.actionPerformed(null);
		});
		assertEquals(before + 1, frame.screen(ScreenKey.INVENTORY).refreshes);
		invokeEdt(frame::dispose);
	}

	@Test
	public void dirtyTokenBoldsLeafAndAncestorAndClears() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Database.getInstance().loadAll();

		final SwingAppFrame[] holder = new SwingAppFrame[1];
		invokeEdt(() -> holder[0] = new SwingAppFrame(false));
		SwingAppFrame frame = holder[0];

		invokeEdt(() ->
		{
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.WATER));
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.REFERENCE_DATABASE));

			frame.getDirtyStateService().markDirty("water");
			assertEquals(Font.BOLD, frame.navNodeFontStyle(ScreenKey.WATER));
			assertEquals(Font.BOLD, frame.navNodeFontStyle(ScreenKey.REFERENCE_DATABASE));

			frame.getDirtyStateService().clear();
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.WATER));
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.REFERENCE_DATABASE));
		});

		invokeEdt(frame::dispose);
	}

	@Test
	public void equipmentProfilesDirtyBoldsBrewingAndEquipmentLeaves() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Database.getInstance().loadAll();

		final SwingAppFrame[] holder = new SwingAppFrame[1];
		invokeEdt(() -> holder[0] = new SwingAppFrame(false));
		SwingAppFrame frame = holder[0];

		invokeEdt(() ->
		{
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.BREWING));
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.EQUIPMENT_PROFILES));

			frame.getDirtyStateService().markDirty("equipment.profiles");
			assertEquals(Font.BOLD, frame.navNodeFontStyle(ScreenKey.BREWING));
			assertEquals(Font.BOLD, frame.navNodeFontStyle(ScreenKey.EQUIPMENT_PROFILES));

			frame.getDirtyStateService().clear();
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.BREWING));
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.EQUIPMENT_PROFILES));
		});

		invokeEdt(frame::dispose);
	}

	@Test
	public void recipeTagSubNodesAppearAndSelectFiltersList() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Database db = Database.getInstance();
		db.loadAll();
		String rName = "ZZ_NavTagR_" + System.nanoTime();
		String tag = "ZZ_NavTagU_" + System.nanoTime();
		try
		{
			Recipe r = new Recipe(rName);
			r.getTags().add(tag);
			db.getRecipes().put(rName, r);

			final SwingAppFrame[] holder = new SwingAppFrame[1];
			invokeEdt(() -> holder[0] = new SwingAppFrame(false));
			SwingAppFrame frame = holder[0];
			invokeEdt(() ->
			{
				DefaultMutableTreeNode recipes = frame.getRecipesNavNodeForTest();
				assertTrue(recipes.getChildCount() >= 1);
				DefaultMutableTreeNode tagNode = findTagChild(recipes, tag);
				assertNotNull(tagNode);
				JTree tree = frame.getNavigationTreeForTest();
				tree.expandPath(new TreePath(((DefaultMutableTreeNode)recipes.getParent()).getPath()));
				tree.expandPath(new TreePath(recipes.getPath()));
				tree.setSelectionPath(new TreePath(tagNode.getPath()));
			});
			invokeEdt(() -> {});
			RecipesScreen rs = frame.getRecipesScreen();
			assertNotNull(rs);
			assertEquals(tag, rs.getActiveTagFilter());
			invokeEdt(frame::dispose);
		}
		finally
		{
			db.getRecipes().remove(rName);
		}
	}

	private static DefaultMutableTreeNode findTagChild(DefaultMutableTreeNode recipes, String tag)
	{
		for (int i = 0; i < recipes.getChildCount(); i++)
		{
			DefaultMutableTreeNode ch = (DefaultMutableTreeNode)recipes.getChildAt(i);
			if (tag.equals(ch.getUserObject()))
			{
				return ch;
			}
		}
		return null;
	}

	@Test
	public void waterParametersDirtyDoesNotBoldWaterLeaf() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Database.getInstance().loadAll();

		final SwingAppFrame[] holder = new SwingAppFrame[1];
		invokeEdt(() -> holder[0] = new SwingAppFrame(false));
		SwingAppFrame frame = holder[0];

		invokeEdt(() ->
		{
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.WATER));
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.WATER_PARAMETERS));
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.REFERENCE_DATABASE));

			frame.getDirtyStateService().markDirty("water.parameters");
			assertEquals(Font.PLAIN, frame.navNodeFontStyle(ScreenKey.WATER));
			assertEquals(Font.BOLD, frame.navNodeFontStyle(ScreenKey.WATER_PARAMETERS));
			assertEquals(Font.BOLD, frame.navNodeFontStyle(ScreenKey.REFERENCE_DATABASE));
		});

		invokeEdt(frame::dispose);
	}

	private static void invokeEdt(Runnable runnable) throws Exception
	{
		SwingUtilities.invokeAndWait(runnable);
	}

	private static class TestableSwingAppFrame extends SwingAppFrame
	{
		private Map<ScreenKey, CountingScreen> testScreens;

		TestableSwingAppFrame()
		{
			super(false);
		}

		@Override
		SwingScreen createScreen(ScreenKey key)
		{
			if (testScreens == null)
			{
				testScreens = new EnumMap<>(ScreenKey.class);
			}
			CountingScreen screen = new CountingScreen();
			testScreens.put(key, screen);
			return screen;
		}

		CountingScreen screen(ScreenKey key)
		{
			return testScreens.get(key);
		}
	}

	private static class CountingScreen extends JPanel implements SwingScreen
	{
		private int activations;
		private int refreshes;

		@Override
		public void onActivate()
		{
			activations++;
		}

		@Override
		public void refresh()
		{
			refreshes++;
		}
	}
}
