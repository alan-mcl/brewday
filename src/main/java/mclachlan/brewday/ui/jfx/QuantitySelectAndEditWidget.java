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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.util.Log;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 *
 */
public class QuantitySelectAndEditWidget extends HBox
{
	private double lastValidValue;
	private final TextField textfield;
	private Quantity.Unit unit;
	private final ComboBox<Quantity.Unit> unitChoice;

	/*-------------------------------------------------------------------------*/
	public QuantitySelectAndEditWidget(Quantity.Unit unit, Quantity.Type... typesAllowed)
	{
		super(5);
		this.setAlignment(Pos.CENTER_LEFT);

		textfield = new TextField();
		unitChoice = new ComboBox<>();

		setUnitOptions(unit, typesAllowed);

		textfield.setAlignment(Pos.CENTER);
		textfield.setPrefWidth(75);

		this.unit = unit;
		unitChoice.getSelectionModel().select(unit);
		unitChoice.setMaxHeight(textfield.getHeight());

		this.getChildren().add(textfield);
		this.getChildren().add(unitChoice);

		// ---

		textfield.focusedProperty().addListener((observable, oldValue, newValue) ->
		{
			if (oldValue && !newValue)
			{
				// text field has lost focus
				try
				{
					double v = Double.parseDouble(textfield.getText());
					lastValidValue = v;
					this.refresh(v);
				}
				catch (NumberFormatException e)
				{
					// ignore parse errors in input text
					Brewday.getInstance().getLog().log(Log.DEBUG, e);
				}
			}
		});

		unitChoice.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) ->
		{
			this.unit = newValue;
		});
	}

	/*-------------------------------------------------------------------------*/
	public void setUnitOptions(Quantity.Unit selected, Quantity.Type... typesAllowed)
	{
		List<Quantity.Unit> unitOptions = new ArrayList<>();

		// consume duplicates
		Set<Quantity.Type> set = new HashSet<>(Arrays.asList(typesAllowed));

		for (Quantity.Type type : set)
		{
			switch (type)
			{
				case WEIGHT:
					unitOptions.add(GRAMS);
					unitOptions.add(KILOGRAMS);
					unitOptions.add(OUNCES);
					unitOptions.add(POUNDS);
					unitOptions.add(PACKET_11_G);
					break;

				case LENGTH:
					unitOptions.add(MILLIMETRE);
					unitOptions.add(CENTIMETRE);
					unitOptions.add(METRE);
					unitOptions.add(KILOMETER);
					unitOptions.add(INCH);
					unitOptions.add(FOOT);
					unitOptions.add(YARD);
					unitOptions.add(MILE);
					break;

				case VOLUME:
					unitOptions.add(MILLILITRES);
					unitOptions.add(LITRES);
					unitOptions.add(US_FLUID_OUNCE);
					unitOptions.add(US_GALLON);
					break;

				case TEMPERATURE:
					unitOptions.add(CELSIUS);
					unitOptions.add(KELVIN);
					unitOptions.add(FAHRENHEIT);
					break;

				case FLUID_DENSITY:
					unitOptions.add(GU);
					unitOptions.add(SPECIFIC_GRAVITY);
					unitOptions.add(PLATO);
					break;

				case COLOUR:
					unitOptions.add(SRM);
					unitOptions.add(LOVIBOND);
					unitOptions.add(EBC);
					break;

				case BITTERNESS:
					unitOptions.add(IBU);
					break;

				case CARBONATION:
					unitOptions.add(GRAMS_PER_LITRE);
					unitOptions.add(VOLUMES);
					break;

				case PRESSURE:
					unitOptions.add(KPA);
					unitOptions.add(PSI);
					unitOptions.add(BAR);
					break;

				case TIME:
					unitOptions.add(SECONDS);
					unitOptions.add(MINUTES);
					unitOptions.add(HOURS);
					unitOptions.add(DAYS);
					break;

				case SPECIFIC_HEAT:
					unitOptions.add(JOULE_PER_KG_CELSIUS);
					break;

				case DIASTATIC_POWER:
					unitOptions.add(LINTNER);
					break;

				case POWER:
					unitOptions.add(KILOWATT);
					break;

				case OTHER:
					unitOptions.add(PPM);
					unitOptions.add(PH);
					unitOptions.add(MEQ_PER_KILOGRAM);
					break;

				default: throw new BrewdayException("invalid "+type);
			}
		}

		ObservableList<Quantity.Unit> units = FXCollections.observableList(unitOptions);

		unitChoice.setItems(units);

		// this will fire the listener
		unitChoice.getSelectionModel().select(selected);
	}

	/*-------------------------------------------------------------------------*/
	public QuantitySelectAndEditWidget(Quantity.Unit unit, double value)
	{
		this(unit);

		this.refresh(value);
	}

	/*-------------------------------------------------------------------------*/
	public QuantitySelectAndEditWidget(Quantity.Unit unit, Quantity value)
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
		lastValidValue = v;
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Quantity quantity, Quantity.Unit unit, Quantity.Type... types)
	{
		if (textfield.isFocused())
		{
			// user is busy typing
			return;
		}

		this.unit = unit;

		setUnitOptions(unit, types);
		unitChoice.getSelectionModel().select(unit);
		refresh(quantity);
	}

	/*-------------------------------------------------------------------------*/
	public Quantity getQuantity()
	{
		try
		{
			return Quantity.parseQuantity(textfield.getText(), unit);
		}
		catch (Exception e)
		{
			// suppress parse errors
			Brewday.getInstance().getLog().log(Log.MEDIUM, e);
		}

		return Quantity.parseQuantity(String.valueOf(lastValidValue), unit);
	}

	/*-------------------------------------------------------------------------*/
	public Quantity.Unit getUnit()
	{
		return this.unit;
	}

	/*-------------------------------------------------------------------------*/
	public void addQuantityListener(ChangeListener<String> listener)
	{
		textfield.textProperty().addListener(listener);
	}

	/*-------------------------------------------------------------------------*/
	public void addUnitListener(ChangeListener<Quantity.Unit> listener)
	{
		unitChoice.getSelectionModel().selectedItemProperty().addListener(listener);
	}
}
