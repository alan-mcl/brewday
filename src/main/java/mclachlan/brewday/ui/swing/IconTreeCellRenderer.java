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

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Custom tree cell renderer that displays icons with tree nodes.
 */
public class IconTreeCellRenderer extends DefaultTreeCellRenderer
{
	/** Map of node text to icons */
	private final Map<String, Icon> iconMap = new HashMap<>();
	
	/**
	 * Constructor.
	 */
	public IconTreeCellRenderer()
	{
		// Load icons
		loadIcons();
	}
	
	/**
	 * Load icons for different node types.
	 */
	private void loadIcons()
	{
		// Initialize the SwingIcons
		SwingIcons.init();
		
		// Main categories
		iconMap.put(getUiString("tab.brewing"), SwingIcons.resizeIcon(SwingIcons.beerIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.inventory"), SwingIcons.resizeIcon(SwingIcons.inventoryIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.reference.database"), SwingIcons.resizeIcon(SwingIcons.databaseIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.tools"), SwingIcons.resizeIcon(SwingIcons.toolsIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.settings"), SwingIcons.resizeIcon(SwingIcons.settingsIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("ui.help"), SwingIcons.resizeIcon(SwingIcons.helpIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		
		// Brewing subcategories
		iconMap.put(getUiString("ui.recipes"), SwingIcons.resizeIcon(SwingIcons.recipeIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("ui.batches"), SwingIcons.resizeIcon(SwingIcons.beerIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.process.templates"), SwingIcons.resizeIcon(SwingIcons.processTemplateIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.equipment.profiles"), SwingIcons.resizeIcon(SwingIcons.equipmentIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		
		// Reference data subcategories
		iconMap.put(getUiString("tab.water"), SwingIcons.resizeIcon(SwingIcons.waterIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.water.parameters"), SwingIcons.resizeIcon(SwingIcons.waterParametersIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.fermentables"), SwingIcons.resizeIcon(SwingIcons.fermentableIconGeneric, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.hops"), SwingIcons.resizeIcon(SwingIcons.hopsIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.yeast"), SwingIcons.resizeIcon(SwingIcons.yeastIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.misc"), SwingIcons.resizeIcon(SwingIcons.miscIconGeneric, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tab.styles"), SwingIcons.resizeIcon(SwingIcons.stylesIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		
		// Tools subcategories
		iconMap.put(getUiString("tools.import"), SwingIcons.resizeIcon(SwingIcons.importIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("tools.water.builder"), SwingIcons.resizeIcon(SwingIcons.waterBuilderIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		
		// Settings subcategories
		iconMap.put(getUiString("settings.brewing"), SwingIcons.resizeIcon(SwingIcons.settingsIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("settings.backend"), SwingIcons.resizeIcon(SwingIcons.settingsIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("settings.ui"), SwingIcons.resizeIcon(SwingIcons.settingsIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		
		// Brewing settings subcategories
		iconMap.put(getUiString("settings.brewing.general"), SwingIcons.resizeIcon(SwingIcons.settingsIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("settings.brewing.mash"), SwingIcons.resizeIcon(SwingIcons.mashIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("settings.brewing.ibu"), SwingIcons.resizeIcon(SwingIcons.hopsIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		
		// Backend settings subcategories
		iconMap.put(getUiString("settings.backend.local.filesystem"), SwingIcons.resizeIcon(SwingIcons.databaseIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		iconMap.put(getUiString("settings.backend.git"), SwingIcons.resizeIcon(SwingIcons.gitIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
		
		// Help subcategories
		iconMap.put(getUiString("ui.about"), SwingIcons.resizeIcon(SwingIcons.brewdayIcon, SwingIcons.NAV_ICON_SIZE, SwingIcons.NAV_ICON_SIZE));
	}
	
	@Override
	public Component getTreeCellRendererComponent(
		JTree tree, Object value, boolean selected, boolean expanded,
		boolean leaf, int row, boolean hasFocus)
	{
		// Get the default renderer component
		Component c = super.getTreeCellRendererComponent(
			tree, value, selected, expanded, leaf, row, hasFocus);
		
		// If the node is a DefaultMutableTreeNode, get its text and look up the icon
		if (value instanceof DefaultMutableTreeNode)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object userObject = node.getUserObject();
			
			if (userObject instanceof String)
			{
				String text = (String)userObject;
				Icon icon = iconMap.get(text);
				
				if (icon != null)
				{
					setIcon(icon);
				}
			}
		}
		
		return c;
	}
}
