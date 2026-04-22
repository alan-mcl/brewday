package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import mclachlan.brewday.ui.swing.app.SwingScreen;

public class PlaceholderScreen extends JPanel implements SwingScreen
{
	public PlaceholderScreen(String title)
	{
		super(new BorderLayout());
		add(new JLabel(title + " - Not implemented in MVP", SwingConstants.CENTER), BorderLayout.CENTER);
	}
}
