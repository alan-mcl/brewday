/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the fermentablee that it will be useful,
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
 * Brewday is distributed in the fermentablee that it will be useful,
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
import mclachlan.brewday.ingredients.Fermentable;

/**
 *
 */
public class FermentablesPanel extends JPanel
{
	private JTable table;
	private FermentablesTableModel model;
	private int dirtyFlag;

	public FermentablesPanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		model = new FermentablesTableModel();
		table = new JTable(model);

		this.add(new JScrollPane(table));

		refresh();
	}

	public void refresh()
	{
		Map<String, Fermentable> dbFermentables = Database.getInstance().getReferenceFermentables();

		List<Fermentable> fermentables = new ArrayList<Fermentable>(dbFermentables.values());
		Collections.sort(fermentables, new Comparator<Fermentable>()
		{
			@Override
			public int compare(Fermentable o1, Fermentable o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		});

		model.data.clear();
		model.data.addAll(fermentables);
	}

	public static class FermentablesTableModel implements TableModel
	{
		private List<Fermentable> data;

		public FermentablesTableModel()
		{
			data = new ArrayList<Fermentable>();
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
				case 1: return "Type";
				case 2: return "Origin";
				case 3: return "Colour";
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
			Fermentable f = data.get(rowIndex);

			switch (columnIndex)
			{
				case 0: return f.getName();
				case 1: return f.getType();
				case 2: return f.getOrigin();
				case 3: return String.format("%.2f", f.getColour());
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
