/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.*;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * A panel that displays large buttons for child nodes in the navigation tree.
 * Each button contains an icon and text, and clicking it navigates to the corresponding node.
 */
public class ButtonNavigationPanel extends JPanel
{
	/** The number of buttons per row */
	private static final int BUTTONS_PER_ROW = 3;
	
	/** The preferred button size */
	private static final Dimension BUTTON_SIZE = new Dimension(200, 80);
	
	/**
	 * Constructor.
	 * 
	 * @param title The title for this navigation panel
	 * @param childNodes The list of child nodes to create buttons for
	 * @param nodeSelectAction The action to perform when a button is clicked
	 */
	public ButtonNavigationPanel(String title, List<DefaultMutableTreeNode> childNodes, Consumer<DefaultMutableTreeNode> nodeSelectAction)
	{
		setLayout(new BorderLayout(10, 10));
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		// Add title
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18));
		add(titleLabel, BorderLayout.NORTH);
		
		// Create a panel for the buttons with a GridBagLayout
		JPanel buttonsPanel = new JPanel(new GridBagLayout());
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
		
		// Configure GridBagConstraints
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 8, 8, 8);     // Add spacing between buttons
		gbc.anchor = GridBagConstraints.CENTER;  // Center the buttons
		gbc.fill = GridBagConstraints.NONE;      // Don't resize the buttons
		gbc.weightx = 0.0;                       // Don't distribute extra horizontal space
		gbc.weighty = 0.0;                       // Don't distribute extra vertical space
		
		// Add buttons for each child node
		int row = 0;
		int col = 0;
		for (DefaultMutableTreeNode childNode : childNodes)
		{
			String nodeName = childNode.getUserObject().toString();
			Icon icon = getIconForNode(nodeName);
			
			JButton button = createLargeButton(nodeName, icon);
			button.addActionListener(e -> nodeSelectAction.accept(childNode));
			
			// Set the position in the grid
			gbc.gridx = col;
			gbc.gridy = row;
			
			// Add the button with constraints
			buttonsPanel.add(button, gbc);
			
			// Move to the next column
			col++;
			
			// If we've reached the end of a row, move to the next row
			if (col >= BUTTONS_PER_ROW)
			{
				col = 0;
				row++;
			}
		}
		
		// Add a filler component to push buttons to the top-left
		gbc.gridx = 0;
		gbc.gridy = row + 1;
		gbc.gridwidth = BUTTONS_PER_ROW;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		buttonsPanel.add(Box.createGlue(), gbc);
		
		// Add the buttons panel to a scroll pane in case there are many buttons
		JScrollPane scrollPane = new JScrollPane(buttonsPanel);
		scrollPane.setBorder(null);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		add(scrollPane, BorderLayout.CENTER);
	}
	
	/**
	 * Create a large button with an icon and text.
	 */
	private JButton createLargeButton(String text, Icon icon)
	{
		JButton button = new JButton(text);
		button.setIcon(icon);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setPreferredSize(BUTTON_SIZE);
		button.setFont(button.getFont().deriveFont(Font.BOLD, 14));
		button.setFocusPainted(false);
		
		return button;
	}
	
	/**
	 * Get the icon for a node based on its name.
	 */
	private Icon getIconForNode(String nodeName)
	{
		// Use the SwingIcons class to get the appropriate icon
		// and resize it to a larger size for the buttons
		Icon icon = null;
		
		if (nodeName.equals(getUiString("ui.recipes")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.recipeIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("ui.batches")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.beerIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("tab.process.templates")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.processTemplateIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("tab.equipment.profiles")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.equipmentIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("tab.water")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.waterIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("tab.water.parameters")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.waterParametersIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("tab.fermentables")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.fermentableIconGeneric, 32, 32);
		}
		else if (nodeName.equals(getUiString("tab.hops")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.hopsIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("tab.yeast")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.yeastIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("tab.misc")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.miscIconGeneric, 32, 32);
		}
		else if (nodeName.equals(getUiString("tab.styles")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.stylesIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("tools.import")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.importIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("tools.water.builder")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.waterBuilderIcon, 32, 32);
		}
		// Settings subcategories
		else if (nodeName.equals(getUiString("settings.brewing")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.settingsIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("settings.backend")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.settingsIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("settings.ui")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.settingsIcon, 32, 32);
		}
		// Brewing settings subcategories
		else if (nodeName.equals(getUiString("settings.brewing.general")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.settingsIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("settings.brewing.mash")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.mashIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("settings.brewing.ibu")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.hopsIcon, 32, 32);
		}
		// Backend settings subcategories
		else if (nodeName.equals(getUiString("settings.backend.local.filesystem")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.databaseIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("settings.backend.git")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.gitIcon, 32, 32);
		}
		else if (nodeName.equals(getUiString("ui.about")))
		{
			icon = SwingIcons.resizeIcon(SwingIcons.brewdayIcon, 32, 32);
		}
		
		return icon;
	}
}
