package mclachlan.brewday.ui.swing.app;

import java.awt.GraphicsEnvironment;
import java.awt.Font;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
