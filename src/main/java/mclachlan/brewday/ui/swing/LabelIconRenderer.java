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

import java.awt.Component;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

class LabelIconRenderer extends DefaultTableCellRenderer
{
	public LabelIconRenderer()
	{
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object
		value, boolean isSelected, boolean hasFocus, int row, int col)
	{
		JLabel r = (JLabel)super.getTableCellRendererComponent(
			table, value, isSelected, hasFocus, row, col);
		setIcon(((LabelIcon)value).getIcon());
		setText(((LabelIcon)value).getLabel());
		return r;
	}
}
