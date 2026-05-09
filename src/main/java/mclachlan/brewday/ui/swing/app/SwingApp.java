package mclachlan.brewday.ui.swing.app;

import javax.swing.SwingUtilities;

public class SwingApp
{
	public static void main(String[] args)
	{
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
			SwingUtilities.invokeLater(() ->
				SwingUiErrors.showUncaught(null, throwable)));
		SwingUtilities.invokeLater(() ->
		{
			SwingAppFrame frame = new SwingAppFrame();
			frame.setVisible(true);
		});
	}
}
