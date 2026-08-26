package mclachlan.brewday.ui.swing;

import java.awt.GraphicsEnvironment;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import org.junit.Assume;

/**
 * Shared helpers for Swing JUnit tests: suppress error dialogs and skip when no display.
 */
public final class SwingTestSupport
{
	private SwingTestSupport()
	{
	}

	/**
	 * Enables dialog suppression for the JVM and skips the test when AWT is headless.
	 */
	public static void assumeDisplay()
	{
		SwingUiErrors.setSuppressDialogs(true);
		Assume.assumeFalse("Swing UI test requires a display", GraphicsEnvironment.isHeadless());
	}
}
