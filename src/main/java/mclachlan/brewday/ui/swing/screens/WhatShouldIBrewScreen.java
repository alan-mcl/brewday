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
import java.awt.Point;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import mclachlan.brewday.ui.swing.app.RecipeEditorNavPort;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.dialogs.NewBatchDialog;
import mclachlan.brewday.ui.swing.widgets.SwingWhatShouldIBrewPanel;

/**
 * Swing Tools &gt; What Should I Brew? surface.
 */
public class WhatShouldIBrewScreen extends JPanel implements SwingScreen
{
	private final SwingWhatShouldIBrewPanel panel;

	public WhatShouldIBrewScreen(JFrame frame, RecipeEditorNavPort recipeEditorNav)
	{
		super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		panel = new SwingWhatShouldIBrewPanel(
			recipeName -> recipeEditorNav.openRecipeEditor(recipeName),
			recipeName ->
			{
				NewBatchDialog dialog = new NewBatchDialog(frame, recipeName);
				dialog.setVisible(true);
			});
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setAlignmentY(Component.TOP_ALIGNMENT);
		JScrollPane scroll = new JScrollPane(panel);
		scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		scroll.getViewport().setViewPosition(new Point(0, 0));
		scroll.getVerticalScrollBar().setUnitIncrement(40);
		scroll.getVerticalScrollBar().setBlockIncrement(160);
		add(scroll, BorderLayout.CENTER);
	}

	@Override
	public void onActivate()
	{
		panel.refreshRecommendations();
	}

	@Override
	public void refresh()
	{
		panel.refreshRecommendations();
	}
}
