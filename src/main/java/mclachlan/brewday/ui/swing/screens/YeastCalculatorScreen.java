/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.widgets.SwingYeastCalculatorPanel;

/**
 * Swing Tools &gt; Yeast Calculator surface.
 */
public class YeastCalculatorScreen extends JPanel implements SwingScreen
{
	public YeastCalculatorScreen()
	{
		super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		SwingYeastCalculatorPanel content = new SwingYeastCalculatorPanel();
		content.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.setAlignmentY(Component.TOP_ALIGNMENT);
		JScrollPane scroll = new JScrollPane(content);
		scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		add(scroll, BorderLayout.CENTER);
	}
}
