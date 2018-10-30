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
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.ingredients.Hop;

/**
*
*/
public class HopsTableModel implements TableModel
{
	private List<Hop> data = new ArrayList<Hop>();;

	/*-------------------------------------------------------------------------*/
	public HopsTableModel()
	{
	}

	/*-------------------------------------------------------------------------*/
	public HopsTableModel(List<Hop> data)
	{
		addAll(data);
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
			case 1: return "Type";
			case 2: return "AA%";
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
		Hop f = data.get(rowIndex);

		switch (columnIndex)
		{
			case 0: return f.getName();
			case 1: return f.getType();
			case 2: return String.format("%.2f%%", f.getAlphaAcid() * 100);
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

	public void clear()
	{
		this.data.clear();
	}

	public void addAll(List<Hop> hops)
	{
		this.data.addAll(hops);
	}

	public List<Hop> getData()
	{
		return data;
	}
}
