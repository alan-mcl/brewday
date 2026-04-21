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

import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.util.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.function.Function;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Utility class for building table columns in Swing.
 * Equivalent to the JavaFX TableBuilder.
 */
public class TableBuilder<T>
{
	/**
	 * Create a column that displays a string property.
	 */
	public TableColumn getStringPropertyValueCol(
		String heading,
		String property)
	{
		TableColumn column = new TableColumn();
		column.setHeaderValue(getUiString(heading));
		column.setCellRenderer(new DefaultTableCellRenderer());
		return column;
	}
	
	/**
	 * Create a column that displays a string property using a getter function.
	 */
	public TableColumn getStringPropertyValueCol(
		String heading,
		Function<T, String> getter)
	{
		TableColumn column = new TableColumn();
		column.setHeaderValue(getUiString(heading));
		column.setCellRenderer(new StringCellRenderer<>(getter));
		return column;
	}

	/**
	 * Create a column that displays a quantity property.
	 */
	public TableColumn getQuantityPropertyValueCol(
		String heading,
		Function<T, Quantity> getter,
		Quantity.Unit unit)
	{
		TableColumn column = new TableColumn();
		column.setHeaderValue(getUiString(heading));
		column.setCellRenderer(new QuantityCellRenderer<>(getter, unit));
		return column;
	}

	/**
	 * Create a column that displays a quantity property with its unit.
	 */
	public TableColumn getQuantityAndUnitPropertyValueCol(
		String heading,
		Function<T, Quantity> getterQuantity,
		Function<T, Quantity.Unit> getterUnit)
	{
		TableColumn column = new TableColumn();
		column.setHeaderValue(getUiString(heading));
		column.setCellRenderer(new QuantityAndUnitCellRenderer<>(getterQuantity, getterUnit));
		return column;
	}

	/**
	 * Create a column that displays an icon.
	 */
	public TableColumn getIconColumn(Function<T, Icon> getter)
	{
		TableColumn column = new TableColumn();
		column.setCellRenderer(new IconCellRenderer<>(getter));
		return column;
	}

	/**
	 * Cell renderer for displaying quantities.
	 */
	private static class QuantityCellRenderer<T> extends DefaultTableCellRenderer
	{
		private final Function<T, Quantity> getter;
		private final Quantity.Unit unit;

		public QuantityCellRenderer(Function<T, Quantity> getter, Quantity.Unit unit)
		{
			this.getter = getter;
			this.unit = unit;
		}

		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			// We need to get the actual model object from the table model
			// This is a simplified implementation - in a real app, we'd need to get the model object
			// from the table model based on the row index
			if (value != null)
			{
				try
				{
					@SuppressWarnings("unchecked")
					T item = (T)value;
					Quantity quantity = getter.apply(item);
					if (quantity != null)
					{
						double v = quantity.get(unit);
						value = StringUtils.format(v, unit);
					}
else
{
						value = "";
					}
				}
catch (ClassCastException e)
{
					// Not the expected type, just use the value as is
				}
			}
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
	}

	/**
	 * Cell renderer for displaying quantities with their units.
	 */
	private static class QuantityAndUnitCellRenderer<T> extends DefaultTableCellRenderer
	{
		private final Function<T, Quantity> getterQuantity;
		private final Function<T, Quantity.Unit> getterUnit;

		public QuantityAndUnitCellRenderer(
			Function<T, Quantity> getterQuantity,
			Function<T, Quantity.Unit> getterUnit)
		{
			this.getterQuantity = getterQuantity;
			this.getterUnit = getterUnit;
		}

		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			if (value != null)
			{
				try
				{
					@SuppressWarnings("unchecked")
					T item = (T)value;
					Quantity quantity = getterQuantity.apply(item);
					Quantity.Unit unit = getterUnit.apply(item);
					
					if (quantity != null)
					{
						if (unit == null)
						{
							unit = quantity.getUnit();
						}
						
						double v = quantity.get(unit);
						value = StringUtils.format(v, unit);
					}
else
{
						value = "";
					}
				}
catch (ClassCastException e)
{
					// Not the expected type, just use the value as is
				}
			}
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
	}

	/**
	 * Cell renderer for displaying string properties.
	 */
	private static class StringCellRenderer<T> extends DefaultTableCellRenderer
	{
		private final Function<T, String> getter;

		public StringCellRenderer(Function<T, String> getter)
		{
			this.getter = getter;
		}

		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			if (value != null)
			{
				try
				{
					@SuppressWarnings("unchecked")
					T item = (T)value;
					String text = getter.apply(item);
					value = text != null ? text : "";
				}
catch (ClassCastException e)
{
					// Not the expected type, just use the value as is
				}
			}
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
	}

	/**
	 * Cell renderer for displaying icons.
	 */
	private static class IconCellRenderer<T> implements TableCellRenderer
	{
		private final Function<T, Icon> getter;
		private final JLabel label = new JLabel();

		public IconCellRenderer(Function<T, Icon> getter)
		{
			this.getter = getter;
			label.setHorizontalAlignment(JLabel.CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			if (value != null)
			{
				try
				{
					@SuppressWarnings("unchecked")
					T item = (T)value;
					Icon icon = getter.apply(item);
					label.setIcon(icon);
				}
catch (ClassCastException e)
{
					// Not the expected type
					label.setIcon(null);
				}
			}
else
{
				label.setIcon(null);
			}
			
			if (isSelected)
			{
				label.setBackground(table.getSelectionBackground());
				label.setForeground(table.getSelectionForeground());
			}
else
{
				label.setBackground(table.getBackground());
				label.setForeground(table.getForeground());
			}
			
			label.setOpaque(true);
			return label;
		}
	}
}
