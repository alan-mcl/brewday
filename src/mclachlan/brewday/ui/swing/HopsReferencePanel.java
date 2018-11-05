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
import mclachlan.brewday.database.Database;
import mclachlan.brewday.ingredients.Hop;

/**
 *
 */
public class HopsReferencePanel extends JPanel
{
	private JTable table;
	private HopsTableModel model;
	private int dirtyFlag;

	public HopsReferencePanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		model = new HopsTableModel();
		table = new JTable(model);

		this.add(new JScrollPane(table));

		refresh();
	}

	public void refresh()
	{
		Map<String, Hop> dbHops = Database.getInstance().getReferenceHops();

		List<Hop> hops = new ArrayList<Hop>(dbHops.values());
		Collections.sort(hops, new Comparator<Hop>()
		{
			@Override
			public int compare(Hop o1, Hop o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		});

		model.data.clear();
		model.data.addAll(hops);
	}

	public static class HopsTableModel implements TableModel
	{
		private List<Hop> data;

		public HopsTableModel()
		{
			data = new ArrayList<Hop>();
		}

		@Override
		public int getRowCount()
		{
			return data.size();
		}

		@Override
		public int getColumnCount()
		{
			return 3;
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0: return "Name";
				case 1: return "Origin";
				case 2: return "AA";
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
			Hop h = data.get(rowIndex);

			switch (columnIndex)
			{
				case 0: return h.getName();
				case 1: return h.getOrigin();
				case 2: return String.format("%.2f%%", h.getAlphaAcid() * 100);
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
