package mclachlan.brewday.ui.swing.app;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class SwingApp
{
	public static void main(String[] args)
	{
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
			SwingUtilities.invokeLater(() ->
				JOptionPane.showMessageDialog(
					null,
					throwable.getMessage(),
					"Error",
					JOptionPane.ERROR_MESSAGE)));
		SwingUtilities.invokeLater(() ->
		{
			SwingAppFrame frame = new SwingAppFrame();
			frame.setVisible(true);
		});
	}
}
