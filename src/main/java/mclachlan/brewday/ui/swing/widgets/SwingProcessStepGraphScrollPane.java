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

package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import mclachlan.brewday.recipe.Recipe;

/**
 * Scrollable host for {@link SwingProcessStepGraphPanel} (recipe editor Process Graph tab).
 */
public class SwingProcessStepGraphScrollPane extends JPanel
{
	private final SwingProcessStepGraphPanel graphPanel;
	private final JScrollPane scroll;

	/*-------------------------------------------------------------------------*/
	public SwingProcessStepGraphScrollPane()
	{
		setLayout(new BorderLayout());
		setBackground(Color.WHITE);

		graphPanel = new SwingProcessStepGraphPanel();
		scroll = new JScrollPane(graphPanel);
		scroll.setBorder(null);
		scroll.setBackground(Color.WHITE);
		scroll.getViewport().setBackground(Color.WHITE);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setWheelScrollingEnabled(true);

		add(scroll, BorderLayout.CENTER);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Rebuilds graph layout from the draft recipe (after {@link Recipe#run()} /
	 * {@link Recipe#dryRun()} for volume tooltips).
	 */
	public void refresh(Recipe r)
	{
		graphPanel.refresh(r);
	}

	/*-------------------------------------------------------------------------*/
	JScrollPane getScrollPane()
	{
		return scroll;
	}
}
