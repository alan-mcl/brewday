package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
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
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		SwingWaterBuilderPanel content = new SwingWaterBuilderPanel(null);
		content.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.setAlignmentY(Component.TOP_ALIGNMENT);
		JScrollPane scroll = new JScrollPane(content);
		scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		add(scroll, BorderLayout.CENTER);
	}
}
