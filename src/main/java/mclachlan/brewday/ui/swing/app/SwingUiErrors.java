package mclachlan.brewday.ui.swing.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.util.Log;

/**
 * Central entry for Swing error {@link JOptionPane}s: logs to {@link Brewday} log, prints to
 * {@link System#out}, then shows the dialog unless headless or dialog suppression is active.
 */
public final class SwingUiErrors
{
	private static final int STACK_SCROLL_PREF_WIDTH = 640;
	private static final int STACK_SCROLL_PREF_HEIGHT = 320;

	private static volatile boolean suppressDialogs;

	private SwingUiErrors()
	{
	}

	/**
	 * When {@code true}, error reporting still logs and prints to stdout but does not show
	 * {@link JOptionPane}s. Used by JUnit ({@code -Dbrewday.ui.suppressDialogs=true}) and
	 * {@link #setSuppressDialogs(boolean)}.
	 */
	public static void setSuppressDialogs(boolean suppress)
	{
		suppressDialogs = suppress;
	}

	public static boolean isSuppressDialogs()
	{
		return suppressDialogs || Boolean.getBoolean("brewday.ui.suppressDialogs");
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
		catch (Throwable logEx)
		{
			logEx.printStackTrace(System.out);
		}
		if (shouldShowDialogs())
		{
			JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Logs the throwable (full stack to log file), prints stack trace to {@link System#out}, then
	 * shows an error dialog with summary and a scrollable stack trace.
	 */
	public static void showError(Component parent, Throwable throwable, String title)
	{
		try
		{
			Brewday.getInstance().getLog().log(Log.LOUD, throwable);
		}
		catch (Throwable logEx)
		{
			logEx.printStackTrace(System.out);
		}
		throwable.printStackTrace(System.out);
		String summary = throwable.getMessage();
		if (summary == null || summary.isEmpty())
		{
			summary = throwable.toString();
		}
		System.out.println("[Swing UI error] " + title + ": " + summary);
		showScrollableErrorDialog(parent, summary, stackTraceString(throwable), title);
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
		catch (Throwable logEx)
		{
			logEx.printStackTrace(System.out);
		}
		throwable.printStackTrace(System.out);
		String summary = throwable.getMessage();
		if (summary == null || summary.isEmpty())
		{
			summary = throwable.toString();
		}
		System.out.println("[Swing UI error] Uncaught: " + summary);
		showScrollableErrorDialog(parent, summary, stackTraceString(throwable), "Error");
	}

	/*-------------------------------------------------------------------------*/

	private static boolean shouldShowDialogs()
	{
		return !GraphicsEnvironment.isHeadless() && !isSuppressDialogs();
	}

	private static String stackTraceString(Throwable throwable)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}

	private static void showScrollableErrorDialog(Component parent, String summary, String detailBody,
		String title)
	{
		if (!shouldShowDialogs())
		{
			return;
		}
		JPanel panel = new JPanel(new BorderLayout(0, 8));
		panel.add(new JLabel(summary), BorderLayout.NORTH);
		JTextArea area = new JTextArea(detailBody);
		area.setEditable(false);
		area.setRows(18);
		area.setColumns(80);
		JScrollPane scroll = new JScrollPane(area);
		scroll.setPreferredSize(new Dimension(STACK_SCROLL_PREF_WIDTH, STACK_SCROLL_PREF_HEIGHT));
		panel.add(scroll, BorderLayout.CENTER);
		JOptionPane.showMessageDialog(parent, panel, title, JOptionPane.ERROR_MESSAGE);
	}
}
