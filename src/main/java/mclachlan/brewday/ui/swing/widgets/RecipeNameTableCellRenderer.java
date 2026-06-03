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
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.table.TableCellRenderer;

/**
 * Renders table name column as 0–3 overlapping SRM-tinted beer icons plus name (recipes and batches).
 */
public class RecipeNameTableCellRenderer implements TableCellRenderer
{
	private static final int ICON_OVERLAP_HGAP = 0;
	private static final int NAME_GAP_PX = 6;

	private final Function<Integer, List<Icon>> iconsForModelRow;
	private final BiPredicate<JTable, Integer> isDirtyViewRow;
	private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, NAME_GAP_PX, 0));
	private final JPanel iconStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, ICON_OVERLAP_HGAP, 0));

	public RecipeNameTableCellRenderer(
		Function<Integer, List<Icon>> iconsForModelRow,
		BiPredicate<JTable, Integer> isDirtyViewRow)
	{
		this.iconsForModelRow = iconsForModelRow;
		this.isDirtyViewRow = isDirtyViewRow;
		iconStrip.setOpaque(false);
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
		panel.removeAll();
		iconStrip.removeAll();
		panel.setOpaque(true);
		if (isSelected)
		{
			panel.setBackground(table.getSelectionBackground());
		}
		else
		{
			panel.setBackground(table.getBackground());
		}

		int modelRow = table.convertRowIndexToModel(row);
		List<Icon> icons = iconsForModelRow.apply(modelRow);
		if (icons != null)
		{
			for (Icon icon : icons)
			{
				if (icon != null)
				{
					iconStrip.add(new JLabel(icon));
				}
			}
		}
		if (iconStrip.getComponentCount() > 0)
		{
			panel.add(iconStrip);
		}

		JLabel name = new JLabel(value == null ? "" : value.toString());
		if (isSelected)
		{
			name.setForeground(table.getSelectionForeground());
		}
		else
		{
			name.setForeground(table.getForeground());
		}
		Font base = table.getFont();
		name.setFont(base.deriveFont(isDirtyViewRow.test(table, row) ? Font.BOLD : Font.PLAIN));
		panel.add(name);
		return panel;
	}
}
