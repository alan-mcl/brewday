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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.ui.swing;

import java.util.*;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.style.Style;

/**
 *
 */
public class StylesReferencePanel extends JPanel
{
	private JTable table;
	private StylesTableModel model;
	private int dirtyFlag;

	public StylesReferencePanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		model = new StylesTableModel();
		table = new JTable(model);

		this.add(new JScrollPane(table));

		refresh();
	}

	public void refresh()
	{
		Map<String, Style> dbStyles = Database.getInstance().getStyles();

		List<Style> styles = new ArrayList<>(dbStyles.values());
		styles.sort(Comparator.comparing(Style::getName));

		model.data.clear();
		model.data.addAll(styles);
	}

	public static class StylesTableModel implements TableModel
	{
		private List<Style> data;

		public StylesTableModel()
		{
			data = new ArrayList<>();
		}

		@Override
		public int getRowCount()
		{
			return data.size();
		}

		@Override
		public int getColumnCount()
		{
			return 4;
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0: return "Name";
				case 1: return "Style Guide";
				case 2: return "Style";
				case 3: return "Category Number";
				default: throw new BrewdayException("Invalid column ["+columnIndex+"]");
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			Style cur = data.get(rowIndex);

			switch (columnIndex)
			{
				case 0: return cur.getName();
				case 1: return cur.getStyleGuide();
				case 2: return cur.getStyleLetter();
				case 3: return cur.getCategoryNumber();
				default: throw new BrewdayException("Invalid column ["+columnIndex+"]");
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{

		}

		@Override
		public void addTableModelListener(TableModelListener l)
		{

		}

		@Override
		public void removeTableModelListener(TableModelListener l)
		{

		}
	}
}
