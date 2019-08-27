package mclachlan.brewday.ui.swing;

import java.util.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.ingredients.Water;

/**
 *
 */
public class WatersTableModel implements TableModel
{
	private List<Water> data;

	public WatersTableModel()
	{
		data = new ArrayList<>();
	}

	public WatersTableModel(List<Water> waters)
	{
		this();
		addAll(waters);
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
			case 0:
				return StringUtils.getUiString("water.name");
			case 1:
				return StringUtils.getUiString("water.calcium");
			case 2:
				return StringUtils.getUiString("water.bicarbonate");
			case 3:
				return StringUtils.getUiString("water.sulfate");
			case 4:
				return StringUtils.getUiString("water.chloride");
			case 5:
				return StringUtils.getUiString("water.sodium");
			case 6:
				return StringUtils.getUiString("water.magnesium");
			case 7:
				return StringUtils.getUiString("water.ph");
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
		Water cur = data.get(rowIndex);

		switch (columnIndex)
		{
			case 0:
				return cur.getName();
			case 1:
				return cur.getCalcium();
			case 2:
				return cur.getBicarbonate();
			case 3:
				return cur.getSulfate();
			case 4:
				return cur.getChloride();
			case 5:
				return cur.getSodium();
			case 6:
				return cur.getMagnesium();
			case 7:
				return cur.getPh();
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

	public void clear()
	{
		this.data.clear();
	}

	public void addAll(List<Water> waters)
	{
		this.data.addAll(waters);
	}

	public Water get(int selectedRow)
	{
		return data.get(selectedRow);
	}
}
