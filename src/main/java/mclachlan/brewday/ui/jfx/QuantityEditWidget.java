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

package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.util.Log;

/**
 *
 */
public class QuantityEditWidget<T extends Quantity> extends HBox
{
	private T quantity;
	private final TextField textfield;
	private Quantity.Unit unit;
	private final Label unitLabel;

	private List<ChangeListener<T>> listeners = new ArrayList<>();
	private final boolean displayUnit;

	/*-------------------------------------------------------------------------*/
	public QuantityEditWidget(Quantity.Unit unit, boolean displayUnit)
	{
		this.setAlignment(Pos.CENTER_LEFT);

		textfield = new TextField();
//		textfield.setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0d));

		unitLabel = new Label();

		textfield.setAlignment(Pos.CENTER);
		unitLabel.setAlignment(Pos.CENTER_LEFT);

		textfield.setPrefWidth(75);

		this.unit = unit;
		this.displayUnit = displayUnit;
		if (displayUnit)
		{
			unitLabel.setText(" " + StringUtils.getUiString("unit." + unit.name()));
		}

		this.getChildren().add(textfield);
		this.getChildren().add(unitLabel);

		// ---

		textfield.focusedProperty().addListener((observable, oldValue, newValue) ->
		{
			if (oldValue && !newValue)
			{
				// text field has lost focus
				try
				{
					this.refresh(Double.parseDouble(textfield.getText()));
				}
				catch (NumberFormatException e)
				{
					// ignore number format errors
					Brewday.getInstance().getLog().log(Log.MEDIUM, e);
					this.refresh(this.getQuantity());
				}
			}
		});

		this.disableProperty().addListener((ob, oldValue, newValue) ->
		{
			if (newValue)
			{
				textfield.setStyle("-fx-text-fill: black");
			}
			else
			{
				textfield.setStyle(null);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	public QuantityEditWidget(Quantity.Unit unit)
	{
		this(unit, true);
	}

	/*-------------------------------------------------------------------------*/
	public QuantityEditWidget(Quantity.Unit unit, double value, boolean displayUnit)
	{
		this(unit, displayUnit);

		this.refresh(value);
	}

	/*-------------------------------------------------------------------------*/
	public QuantityEditWidget(Quantity.Unit unit, double value)
	{
		this(unit, value, true);
	}

	/*-------------------------------------------------------------------------*/
	public QuantityEditWidget(Quantity.Unit unit, T value)
	{
		this(unit);
		refresh(value);
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Quantity quantity)
	{
		if (quantity != null)
		{
			double v = quantity.get(unit);

			refresh(v);
		}
		else
		{
			this.textfield.clear();
		}
	}

	/*-------------------------------------------------------------------------*/
	protected void refresh(double v)
	{
		if (textfield.isFocused())
		{
			// user is busy typing
			return;
		}

		this.textfield.setText(StringUtils.format(v));
		this.setQuantity((T)Quantity.parseQuantity(String.valueOf(v), unit));
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Quantity quantity, Quantity.Unit unit)
	{
		if (textfield.isFocused())
		{
			// user is busy typing
			return;
		}

		this.unit = unit;
		refresh(quantity);

		if (displayUnit)
		{
			unitLabel.setText(" " + StringUtils.getUiString("unit." + unit.name()));
		}
	}

	/*-------------------------------------------------------------------------*/
	public T getQuantity()
	{
		try
		{
			T old = quantity;
			quantity = (T)Quantity.parseQuantity(textfield.getText(), unit);

			if (!old.equals(quantity))
			{
				notifyListeners(old, quantity);
			}
		}
		catch (Exception e)
		{
			// suppress parse errors
			Brewday.getInstance().getLog().log(Log.MEDIUM, e);
		}
		return (T)quantity;
	}

	/*-------------------------------------------------------------------------*/
	public Quantity.Unit getUnit()
	{
		return this.unit;
	}

	/*-------------------------------------------------------------------------*/
	public void addListener(ChangeListener<T> listener)
	{
		listeners.add(listener);
	}

	/*-------------------------------------------------------------------------*/
	private void notifyListeners(T oldValue, T newValue)
	{
		for (ChangeListener<T> listener : listeners)
		{
			listener.changed(null, oldValue, newValue);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void setEditable(boolean b)
	{
		textfield.setEditable(b);
	}

	/*-------------------------------------------------------------------------*/
	private void setQuantity(T quantity)
	{
		T old = this.quantity;
		this.quantity = quantity;
		notifyListeners(old, quantity);
	}
}
