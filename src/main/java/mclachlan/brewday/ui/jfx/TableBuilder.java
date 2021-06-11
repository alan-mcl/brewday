
/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import java.util.function.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.Quantity;

import static mclachlan.brewday.StringUtils.getUiString;

/**
 *
 */
public class TableBuilder<T>
{

	/*-------------------------------------------------------------------------*/
	protected TableColumn<T, String> getStringPropertyValueCol(
		String heading,
		String property)
	{
		TableColumn<T, String> col = new TableColumn<>(getUiString(heading));
		col.setCellValueFactory(new PropertyValueFactory<>(property));
		return col;
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<T, String> getQuantityPropertyValueCol(
		String heading,
		Function<T, Quantity> getter,
		Quantity.Unit unit)
	{
		TableColumn<T, String> col = new TableColumn<>(getUiString(heading));
		col.setCellValueFactory(param ->
		{
			Quantity quantity = getter.apply(param.getValue());
			if (quantity != null)
			{
				double v = quantity.get(unit);
				return new SimpleStringProperty(StringUtils.format(v, unit));
			}
			else
			{
				return new SimpleObjectProperty<>(null);
			}
		});

		return col;
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<T, String> getQuantityAndUnitPropertyValueCol(
		String heading,
		Function<T, Quantity> getterQuantity,
		Function<T, Quantity.Unit> getterUnit)
	{
		TableColumn<T, String> col = new TableColumn<>(getUiString(heading));
		col.setCellValueFactory(param ->
		{
			Quantity quantity = getterQuantity.apply(param.getValue());
			Quantity.Unit unit = getterUnit.apply(param.getValue());
			if (quantity != null)
			{
				if (unit == null)
				{
					unit = quantity.getUnit();
				}

				double v = quantity.get(unit);
				return new SimpleStringProperty(StringUtils.format(v, unit));
			}
			else
			{
				return new SimpleObjectProperty<>(null);
			}
		});

		return col;
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<T, T> getIconColumn(Function<T, Image> getter)
	{
		TableColumn<T, T> iconCol = new TableColumn<>();
		iconCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
		iconCol.setCellFactory(c -> new V2DataObjectPane.ImageTableCell<>(getter));
		iconCol.setSortable(true);
		return iconCol;
	}

}
