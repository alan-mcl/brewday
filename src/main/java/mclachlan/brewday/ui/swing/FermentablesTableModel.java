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

import java.util.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.ingredients.Fermentable;

/**
*
*/
public class FermentablesTableModel implements TableModel
{
	private List<Fermentable> data = new ArrayList<Fermentable>();;

	/*-------------------------------------------------------------------------*/
	public FermentablesTableModel()
	{
	}

	/*-------------------------------------------------------------------------*/
	public FermentablesTableModel(List<Fermentable> data)
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
		return 4;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		switch (columnIndex)
		{
			case 0: return StringUtils.getUiString("fermentable.name");
			case 1: return StringUtils.getUiString("fermentable.type");
			case 2: return StringUtils.getUiString("fermentable.origin");
			case 3: return StringUtils.getUiString("fermentable.colour");
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

	public void clear()
	{
		this.data.clear();
	}

	public void addAll(List<Fermentable> fermentables)
	{
		this.data.addAll(fermentables);
	}

	public List<Fermentable> getData()
	{
		return data;
	}
}
