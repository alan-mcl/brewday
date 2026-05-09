package mclachlan.brewday.ui.swing.app;

import java.awt.Component;
import javax.swing.JOptionPane;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.util.Log;

/**
 * Central entry for Swing error {@link JOptionPane}s: logs to {@link Brewday} log, prints to
 * {@link System#out}, then shows the dialog.
 */
public final class SwingUiErrors
{
	private SwingUiErrors()
	{
	}

	/**
	 * Shows an error dialog and records the same text to the application log and standard output.
	 */
	public static void showError(Component parent, String message, String title)
	{
		String line = "[Swing UI error] " + title + ": " + message;
		System.out.println(line);
		try
		{
			Brewday.getInstance().getLog().log(Log.LOUD, line);
		}
		catch (Throwable ignored)
		{
		}
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Logs the throwable, prints stack trace to {@link System#out}, then shows the error dialog
	 * (used by uncaught-exception handler).
	 */
	public static void showUncaught(Component parent, Throwable throwable)
	{
		try
		{
			Brewday.getInstance().getLog().log(Log.LOUD, throwable);
		}
		catch (Throwable ignored)
		{
		}
		throwable.printStackTrace(System.out);
		String msg = throwable.getMessage();
		if (msg == null || msg.isEmpty())
		{
			msg = throwable.toString();
		}
		System.out.println("[Swing UI error] Uncaught: " + msg);
		JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
	}
}
