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

/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the yeaste that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the yeaste that it will be useful,
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
import mclachlan.brewday.database.Database;
import mclachlan.brewday.ingredients.Yeast;

/**
 *
 */
public class YeastsReferencePanel extends JPanel
{
	private JTable table;
	private YeastsTableModel model;
	private int dirtyFlag;

	public YeastsReferencePanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		model = new YeastsTableModel();
		table = new JTable(model);

		this.add(new JScrollPane(table));

		refresh();
	}

	public void refresh()
	{
		Map<String, Yeast> dbYeasts = Database.getInstance().getReferenceYeasts();

		List<Yeast> yeasts = new ArrayList<Yeast>(dbYeasts.values());
		Collections.sort(yeasts, new Comparator<Yeast>()
		{
			@Override
			public int compare(Yeast o1, Yeast o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		});

		model.data.clear();
		model.data.addAll(yeasts);
	}

	public static class YeastsTableModel implements TableModel
	{
		private List<Yeast> data;

		public YeastsTableModel()
		{
			data = new ArrayList<Yeast>();
		}

		@Override
		public int getRowCount()
		{
			return data.size();
		}

		@Override
		public int getColumnCount()
		{
			return 6;
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0: return "Name";
				case 1: return "Laboratory";
				case 2: return "Product ID";
				case 3: return "Type";
				case 4: return "Form";
				case 5: return "Recommended Styles";
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
			Yeast cur = data.get(rowIndex);

			switch (columnIndex)
			{
				case 0: return cur.getName();
				case 1: return cur.getLaboratory();
				case 2: return cur.getProductId();
				case 3: return cur.getType();
				case 4: return cur.getForm();
				case 5: return cur.getRecommendedStyles();
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