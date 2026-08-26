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
	private final SwingWaterBuilderPanel panel;

	public WaterBuilderScreen()
	{
		super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		panel = new SwingWaterBuilderPanel(null);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setAlignmentY(Component.TOP_ALIGNMENT);
		JScrollPane scroll = new JScrollPane(panel);
		scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		add(scroll, BorderLayout.CENTER);
	}

	@Override
	public void refresh()
	{
		panel.refreshDisplayUnits();
	}
}
