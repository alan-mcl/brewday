package mclachlan.brewday.ui.swing.app;

import java.awt.GraphicsEnvironment;
import java.util.EnumMap;
import java.util.Map;
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
