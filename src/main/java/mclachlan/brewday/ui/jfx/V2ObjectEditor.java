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

import java.util.function.*;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.*;
import org.tbee.javafx.scene.layout.MigPane;

public abstract class V2ObjectEditor<T extends V2DataObject> extends MigPane
{
	/*-------------------------------------------------------------------------*/
	public V2ObjectEditor(T obj, TrackDirty parent)
	{
		super("gap 3");

		buildUi(obj, parent);
	}

	/*-------------------------------------------------------------------------*/
	protected abstract void buildUi(T obj, TrackDirty parent);

	/*-------------------------------------------------------------------------*/
	protected void addTextField(
		T obj,
		TrackDirty parent,
		String uiLabelKey,
		Function<T, String> getter,
		BiConsumer<T, String> setter,
		String columnConstraints)
	{
		// JFX bug seems to happen for text fields beyond length 100.
		// https://github.com/javafxports/openjdk-jfx/issues/603
		int maxLen = 98;

		TextField tf = new TextField(getter.apply(obj));
		tf.setTextFormatter(new TextFormatter<>(c -> {
			if (c.isContentChange())
			{
				int newLength = c.getControlNewText().length();
				if (newLength > maxLen)
				{
					// replace the input text with the last len chars
					String tail = c.getControlNewText().substring(newLength - maxLen, newLength);
					c.setText(tail);
					// replace the range to complete text
					// valid coordinates for range is in terms of old text
					int oldLength = c.getControlText().length();
					c.setRange(0, oldLength);
				}
			}
			return c;
		}));

		this.add(new Label(StringUtils.getUiString(uiLabelKey)));
		this.add(tf, columnConstraints);

		tf.textProperty().addListener((observable, oldValue, newValue) ->
		{
			setter.accept(obj, tf.getText());
			parent.setDirty(obj);
		});
	}

	/*-------------------------------------------------------------------------*/
	protected void addQuantityWidget(
		T obj,
		TrackDirty parent,
		String uiLabelKey,
		Function<T, Quantity> getter,
		BiConsumer setter,
		Quantity.Unit unit,
		String columnContraints)
	{
		QuantityEditWidget qew;

		switch (unit)
		{
			case GRAMS:
			case KILOGRAMS:
			case OUNCES:
			case POUNDS:
			case PACKET_11_G:
				qew = new QuantityEditWidget<WeightUnit>(unit);
				break;
			case MILLIMETRE:
			case CENTIMETRE:
			case METRE:
			case KILOMETER:
			case INCH:
			case FOOT:
			case YARD:
			case MILE:
				qew = new QuantityEditWidget<LengthUnit>(unit);
				break;
			case MILLILITRES:
			case LITRES:
			case US_FLUID_OUNCE:
			case US_GALLON:
				qew = new QuantityEditWidget<VolumeUnit>(unit);
				break;
			case CELSIUS:
			case KELVIN:
			case FAHRENHEIT:
				qew = new QuantityEditWidget<TemperatureUnit>(unit);
				break;

			case GU:
			case SPECIFIC_GRAVITY:
			case PLATO:
				qew = new QuantityEditWidget<DensityUnit>(unit);
				break;

			case SRM:
			case LOVIBOND:
			case EBC:
				qew = new QuantityEditWidget<ColourUnit>(unit);
				break;

			case IBU:
				qew = new QuantityEditWidget<BitternessUnit>(unit);
				break;

			case GRAMS_PER_LITRE:
			case VOLUMES:
				qew = new QuantityEditWidget<CarbonationUnit>(unit);
				break;

			case KPA:
			case PSI:
			case BAR:
				qew = new QuantityEditWidget<PressureUnit>(unit);
				break;

			case KILOWATT:
				qew = new QuantityEditWidget<PowerUnit>(unit);
				break;

			case SECONDS:
			case MINUTES:
			case HOURS:
			case DAYS:
				qew = new QuantityEditWidget<TimeUnit>(unit);
				break;

			case JOULE_PER_KG_CELSIUS:
			case MEQ_PER_KILOGRAM:
				qew = new QuantityEditWidget<ArbitraryPhysicalQuantity>(unit);
				break;

			case LINTNER:
				qew = new QuantityEditWidget<DiastaticPowerUnit>(unit);
				break;

			case PERCENTAGE:
			case PERCENTAGE_DISPLAY:
				qew = new QuantityEditWidget<PercentageUnit>(unit);
				break;
			case PPM:
				qew = new QuantityEditWidget<PpmUnit>(unit);
				break;
			case PH:
				qew = new QuantityEditWidget<PhUnit>(unit);
				break;

			default:
				throw new BrewdayException("invalid: " + unit);
		}

		qew.refresh(getter.apply(obj));

		this.add(new Label(StringUtils.getUiString(uiLabelKey)));
		this.add(qew, columnContraints);

		qew.addListener((observable, oldValue, newValue) ->
		{
			setter.accept(obj, qew.getQuantity());
			parent.setDirty(obj);
		});
	}

	/*-------------------------------------------------------------------------*/
	protected void addTextArea(
		T obj,
		TrackDirty parent,
		String uiLabelKey,
		Function<T, String> getter,
		BiConsumer<T, String> setter, String colContraints)
	{
		TextArea desc = new TextArea(getter.apply(obj));
		desc.setWrapText(true);
		this.add(new Label(StringUtils.getUiString(uiLabelKey)), "wrap");
		this.add(desc, colContraints);
		desc.textProperty().addListener((observable, oldValue, newValue) ->
		{
			setter.accept(obj, desc.getText());
			parent.setDirty(obj);
		});
	}

	/*-------------------------------------------------------------------------*/
	protected void addCheckBox(
		T obj,
		TrackDirty parent,
		String uiLabelKey,
		Function<T, Boolean> getter,
		BiConsumer<T, Boolean> setter,
		String colConstraints)
	{
		CheckBox addAfterBoil = new CheckBox(StringUtils.getUiString(uiLabelKey));
		addAfterBoil.setSelected(getter.apply(obj));
		this.add(addAfterBoil, colConstraints);
		addAfterBoil.selectedProperty().addListener((observable, oldValue, newValue) ->
		{
			setter.accept(obj, addAfterBoil.isSelected());
			parent.setDirty(obj);
		});
	}

	/*-------------------------------------------------------------------------*/
	protected void addComboBox(
		T obj,
		TrackDirty parent,
		String uiLabelKey,
		Function getter,
		BiConsumer setter,
		Object[] values,
		String colConstraints)
	{
		ComboBox cb = new ComboBox<>(FXCollections.observableArrayList(values));
		cb.getSelectionModel().select(getter.apply(obj));

		this.add(new Label(StringUtils.getUiString(uiLabelKey)));
		this.add(cb, colConstraints);
		cb.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
		{
			setter.accept(obj, cb.getSelectionModel().getSelectedItem());
			parent.setDirty(obj);
		});
	}
}
