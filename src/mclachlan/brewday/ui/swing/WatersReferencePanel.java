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
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Water;

/**
 *
 */
public class WatersReferencePanel extends JPanel
{
	private JTable table;
	private WatersTableModel model;
	private int dirtyFlag;

	public WatersReferencePanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		model = new WatersTableModel();
		table = new JTable(model);

		this.add(new JScrollPane(table));

		refresh();
	}

	public void refresh()
	{
		Map<String, Water> dbWaters = Database.getInstance().getWaters();

		List<Water> waters = new ArrayList<>(dbWaters.values());
		waters.sort(Comparator.comparing(Water::getName));

		model.data.clear();
		model.data.addAll(waters);
	}

	public static class WatersTableModel implements TableModel
	{
		private List<Water> data;

		public WatersTableModel()
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
			return 8;
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0: return StringUtils.getUiString("water.name");
				case 1: return StringUtils.getUiString("water.calcium");
				case 2: return StringUtils.getUiString("water.bicarbonate");
				case 3: return StringUtils.getUiString("water.sulfate");
				case 4: return StringUtils.getUiString("water.chloride");
				case 5: return StringUtils.getUiString("water.sodium");
				case 6: return StringUtils.getUiString("water.magnesium");
				case 7: return StringUtils.getUiString("water.ph");
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
			Water cur = data.get(rowIndex);

			switch (columnIndex)
			{
				case 0: return cur.getName();
				case 1: return cur.getCalcium();
				case 2: return cur.getBicarbonate();
				case 3: return cur.getSulfate();
				case 4: return cur.getChloride();
				case 5: return cur.getSodium();
				case 6: return cur.getMagnesium();
				case 7: return cur.getPh();
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
