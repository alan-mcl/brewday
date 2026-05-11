package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.widgets.SwingWaterBuilderPanel;

/**
 * Swing Tools > Water Builder surface.
 */
public class WaterBuilderScreen extends JPanel implements SwingScreen
{
	public WaterBuilderScreen()
	{
		super(new BorderLayout());
		add(new JScrollPane(new SwingWaterBuilderPanel(null)), BorderLayout.CENTER);
	}
}
