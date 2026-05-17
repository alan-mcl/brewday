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
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.SwingIcons;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Scrollable host for {@link SwingProcessStepGraphPanel} (recipe editor Process Graph tab).
 */
public class SwingProcessStepGraphScrollPane extends JPanel
{
	private static final int PROCESS_GRAPH_TAB_INDEX = 1;

	private final JFrame ownerFrame;
	private final SwingProcessStepGraphPanel graphPanel;
	private final JScrollPane scroll;
	private final JButton refreshButton;

	private Recipe currentRecipe;

	/*-------------------------------------------------------------------------*/
	public SwingProcessStepGraphScrollPane(JFrame ownerFrame)
	{
		this.ownerFrame = ownerFrame;
		setLayout(new BorderLayout());
		setBackground(Color.WHITE);

		graphPanel = new SwingProcessStepGraphPanel();
		scroll = new JScrollPane(graphPanel);
		scroll.setBorder(null);
		scroll.setBackground(Color.WHITE);
		scroll.getViewport().setBackground(Color.WHITE);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setWheelScrollingEnabled(false);

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);

		refreshButton = toolbarButton(
			getUiString("recipe.process.graph.refresh"),
			getUiString("recipe.process.graph.refresh.tooltip"),
			SwingIcons.IconKey.GRAPH,
			this::onRefresh);
		bar.add(refreshButton);

		bar.add(toolbarButton(
			getUiString("recipe.process.graph.zoom.out"),
			getUiString("recipe.process.graph.zoom.out.tooltip"),
			null,
			e -> graphPanel.zoomOut()));

		bar.add(toolbarButton(
			getUiString("recipe.process.graph.zoom.in"),
			getUiString("recipe.process.graph.zoom.in.tooltip"),
			null,
			e -> graphPanel.zoomIn()));

		bar.add(toolbarButton(
			getUiString("recipe.process.graph.export"),
			getUiString("recipe.process.graph.export.tooltip"),
			SwingIcons.IconKey.EXPORT_CSV,
			this::onExport));

		add(bar, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Updates recipe/volume data after {@link Recipe#run()} / {@link Recipe#dryRun()}
	 * without recomputing layout.
	 */
	public void updateAfterRun(Recipe r)
	{
		currentRecipe = r;
		graphPanel.updateAfterRun(r);
		updateRefreshTooltip();
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Rebuilds graph layout from the draft recipe.
	 */
	public void relayout(Recipe r)
	{
		currentRecipe = r;
		graphPanel.relayout(r);
		updateRefreshTooltip();
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Rebuilds graph layout from the draft recipe.
	 */
	public void refresh(Recipe r)
	{
		relayout(r);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Lays out the graph once when the tab is first opened.
	 */
	public void ensureLaidOut(Recipe r)
	{
		currentRecipe = r;
		if (!graphPanel.hasLayout())
		{
			graphPanel.relayout(r);
		}
		updateRefreshTooltip();
	}

	/*-------------------------------------------------------------------------*/
	public static int getProcessGraphTabIndex()
	{
		return PROCESS_GRAPH_TAB_INDEX;
	}

	/*-------------------------------------------------------------------------*/
	JScrollPane getScrollPane()
	{
		return scroll;
	}

	/*-------------------------------------------------------------------------*/
	private void onRefresh(ActionEvent e)
	{
		if (currentRecipe != null)
		{
			graphPanel.relayout(currentRecipe);
			updateRefreshTooltip();
		}
	}

	/*-------------------------------------------------------------------------*/
	private void onExport(ActionEvent e)
	{
		Component parent = ownerFrame != null ? ownerFrame : this;
		graphPanel.exportToPng(parent);
	}

	/*-------------------------------------------------------------------------*/
	private void updateRefreshTooltip()
	{
		String tip = getUiString("recipe.process.graph.refresh.tooltip");
		if (graphPanel.isLayoutStale())
		{
			tip = tip + " " + getUiString("recipe.process.graph.stale");
		}
		refreshButton.setToolTipText(tip);
	}

	/*-------------------------------------------------------------------------*/
	private static JButton toolbarButton(
		String label,
		String tooltip,
		SwingIcons.IconKey iconKey,
		java.awt.event.ActionListener action)
	{
		JButton b = iconKey != null
			? new JButton(SwingIcons.toolbarIcon(iconKey))
			: new JButton(label);
		if (iconKey != null)
		{
			b.setToolTipText(tooltip);
		}
		else
		{
			b.setText(label);
			b.setToolTipText(tooltip);
		}
		b.addActionListener(action);
		return b;
	}
}
