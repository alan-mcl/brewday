package mclachlan.brewday.ui.swing.app;

import javax.swing.SwingUtilities;

public class SwingApp
{
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(() ->
		{
			SwingAppFrame frame = new SwingAppFrame();
			frame.setVisible(true);
		});
	}
}
