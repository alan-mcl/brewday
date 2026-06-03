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

import java.awt.Component;
import java.awt.Font;
import java.util.function.BiPredicate;
import java.util.function.Function;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renders table column 0 as icon + ingredient name (reference DB and inventory lists).
 */
public class IngredientNameTableCellRenderer extends DefaultTableCellRenderer
{
	private final Function<Integer, Icon> iconForModelRow;
	private final BiPredicate<JTable, Integer> isDirtyViewRow;

	public IngredientNameTableCellRenderer(
		Function<Integer, Icon> iconForModelRow,
		BiPredicate<JTable, Integer> isDirtyViewRow)
	{
		this.iconForModelRow = iconForModelRow;
		this.isDirtyViewRow = isDirtyViewRow;
	}

	@Override
	public Component getTableCellRendererComponent(
		JTable table,
		Object value,
		boolean isSelected,
		boolean hasFocus,
		int row,
		int column)
	{
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		int modelRow = table.convertRowIndexToModel(row);
		setIcon(iconForModelRow.apply(modelRow));
		setText(value == null ? "" : value.toString());
		setIconTextGap(6);
		Font base = table.getFont();
		setFont(base.deriveFont(isDirtyViewRow.test(table, row) ? Font.BOLD : Font.PLAIN));
		return this;
	}
}
