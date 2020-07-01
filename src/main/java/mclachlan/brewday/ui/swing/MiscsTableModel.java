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
import mclachlan.brewday.ingredients.Misc;

/**
 *
 */
public class MiscsTableModel implements TableModel
{
	private List<Misc> data;

	public MiscsTableModel(List<Misc> data)
	{
		this.data = data;
	}

	public MiscsTableModel()
	{
		data = new ArrayList<>();
	}

	public List<Misc> getData()
	{
		return data;
	}

	public void setData(List<Misc> data)
	{
		this.data = data;
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
			case 0:
				return StringUtils.getUiString("misc.name");
			case 1:
				return StringUtils.getUiString("misc.type");
			case 2:
				return StringUtils.getUiString("misc.use");
			case 3:
				return StringUtils.getUiString("misc.usage.recommendation");
			default:
				throw new BrewdayException("Invalid column [" + columnIndex + "]");
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
		Misc cur = data.get(rowIndex);

		switch (columnIndex)
		{
			case 0:
				return cur.getName();
			case 1:
				return cur.getType();
			case 2:
				return cur.getUse();
			case 3:
				return cur.getUsageRecommendation();
			default:
				throw new BrewdayException("Invalid column [" + columnIndex + "]");
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
