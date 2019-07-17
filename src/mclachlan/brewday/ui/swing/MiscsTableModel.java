package mclachlan.brewday.ui.swing;

import java.util.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import mclachlan.brewday.BrewdayException;
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
				return "Name";
			case 1:
				return "Type";
			case 2:
				return "Use";
			case 3:
				return "Usage Recommendation";
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
