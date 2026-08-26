package mclachlan.brewday.ui.swing.app;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SwingUiErrorsTest
{
	private final PrintStream originalOut = System.out;
	private ByteArrayOutputStream capturedOut;
	private boolean priorSuppress;

	@Before
	public void captureStdout()
	{
		capturedOut = new ByteArrayOutputStream();
		System.setOut(new PrintStream(capturedOut));
		priorSuppress = SwingUiErrors.isSuppressDialogs();
		SwingUiErrors.setSuppressDialogs(true);
	}

	@After
	public void restoreStdout()
	{
		System.setOut(originalOut);
		SwingUiErrors.setSuppressDialogs(priorSuppress);
	}

	@Test
	public void showErrorStringLogsWithoutBlockingWhenSuppressed()
	{
		SwingUiErrors.showError(null, "validation failed", "Error");
		String out = capturedOut.toString();
		assertTrue(out.contains("[Swing UI error]"));
		assertTrue(out.contains("validation failed"));
	}

	@Test
	public void showErrorThrowableLogsWithoutBlockingWhenSuppressed()
	{
		SwingUiErrors.showError(null, new IllegalStateException("boom"), "Error");
		String out = capturedOut.toString();
		assertTrue(out.contains("[Swing UI error]"));
		assertTrue(out.contains("boom"));
		assertTrue(out.contains("IllegalStateException"));
	}

	@Test
	public void showUncaughtLogsWithoutBlockingWhenSuppressed()
	{
		SwingUiErrors.showUncaught(null, new RuntimeException("uncaught"));
		String out = capturedOut.toString();
		assertTrue(out.contains("[Swing UI error] Uncaught"));
		assertTrue(out.contains("uncaught"));
	}

	@Test
	public void setSuppressDialogsEnablesSuppression()
	{
		SwingUiErrors.setSuppressDialogs(true);
		assertTrue(SwingUiErrors.isSuppressDialogs());
	}
}
