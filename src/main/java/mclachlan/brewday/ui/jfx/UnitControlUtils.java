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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javafx.scene.control.Label;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class UnitControlUtils<T>
{
	private boolean detectDirty = true, refreshing = false;
	private final TrackDirty trackDirty;

	// target for the set and get methods
	private T target;

	// various unit controls
	private final Map<QuantityEditWidget<TimeUnit>, TimeUnitInfo> timeUnitControls = new HashMap<>();
	private final Map<QuantityEditWidget<TemperatureUnit>, TempUnitInfo> tempUnitControls = new HashMap<>();
	private final Map<QuantityEditWidget<VolumeUnit>, VolUnitInfo> volumeUnitControls = new HashMap<>();
	private final Map<QuantityEditWidget<WeightUnit>, WeightUnitInfo> weightUnitControls = new HashMap<>();

	/*-------------------------------------------------------------------------*/
	public UnitControlUtils(TrackDirty trackDirty)
	{
		this.trackDirty = trackDirty;
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(T t, IngredientAddition.Type ingType, ProcessStep.Type stepType)
	{
		detectDirty = false;
		refreshing = true;

		this.target = t;

		Settings settings = Database.getInstance().getSettings();

		Quantity.Unit weightUnit = settings.getUnitForStepAndIngredient(Quantity.Type.WEIGHT, stepType, ingType);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, stepType, ingType);
		Quantity.Unit volUnit = settings.getUnitForStepAndIngredient(Quantity.Type.VOLUME, stepType, ingType);
		Quantity.Unit tempUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TEMPERATURE, stepType, ingType);

		for (QuantityEditWidget<TimeUnit> widget : timeUnitControls.keySet())
		{
			if (target != null)
			{
				TimeUnitInfo info = timeUnitControls.get(widget);
				widget.refresh(info.getMethod.apply(target), timeUnit);
			}
		}
		for (QuantityEditWidget<TemperatureUnit> widget : tempUnitControls.keySet())
		{
			if (target != null)
			{
				TempUnitInfo info = tempUnitControls.get(widget);
				widget.refresh(info.getMethod.apply(target), tempUnit);
			}
		}
		for (QuantityEditWidget<VolumeUnit> widget : volumeUnitControls.keySet())
		{
			if (target != null)
			{
				VolUnitInfo info = volumeUnitControls.get(widget);
				widget.refresh(info.getMethod.apply(target), volUnit);
			}
		}
		for (QuantityEditWidget<WeightUnit> widget : weightUnitControls.keySet())
		{
			if (target != null)
			{
				WeightUnitInfo info = weightUnitControls.get(widget);
				widget.refresh(info.getMethod.apply(target), weightUnit);
			}
		}

		detectDirty = true;
		refreshing = false;
	}

	/*-------------------------------------------------------------------------*/
	protected void addTimeUnitControl(
		MigPane pane,
		String uiLabelKey,
		Function<T, TimeUnit> getMethod,
		BiConsumer<T, TimeUnit> setMethod,
		Quantity.Unit unit)
	{
		QuantityEditWidget<TimeUnit> widget = new QuantityEditWidget<>(unit);
		pane.add(new Label(StringUtils.getUiString(uiLabelKey)));
		pane.add(widget, "wrap");

		this.timeUnitControls.put(widget, new TimeUnitInfo(getMethod, setMethod, unit));

		this.addTimeUnitListener(setMethod, widget, unit);
	}

	/*-------------------------------------------------------------------------*/
	protected void addTemperatureUnitControl(
		MigPane pane,
		String uiLabelKey,
		Function<T, TemperatureUnit> getMethod,
		BiConsumer<T, TemperatureUnit> setMethod,
		Quantity.Unit unit)
	{
		QuantityEditWidget<TemperatureUnit> widget = new QuantityEditWidget<>(unit);
		pane.add(new Label(StringUtils.getUiString(uiLabelKey)));
		pane.add(widget, "wrap");

		this.tempUnitControls.put(widget, new TempUnitInfo(getMethod, setMethod, unit));

		this.addTemperatureUnitListener(setMethod, widget, unit);
	}

	/*-------------------------------------------------------------------------*/
	protected void addVolumeUnitControl(
		MigPane pane,
		String uiLabelKey,
		Function<T, VolumeUnit> getMethod,
		BiConsumer<T, VolumeUnit> setMethod,
		Quantity.Unit unit)
	{
		QuantityEditWidget<VolumeUnit> widget = new QuantityEditWidget<>(unit);
		pane.add(new Label(StringUtils.getUiString(uiLabelKey)));
		pane.add(widget, "wrap");

		this.volumeUnitControls.put(widget, new VolUnitInfo(getMethod, setMethod, unit));

		this.addVolumeUnitListener(setMethod, widget, unit);
	}

	/*-------------------------------------------------------------------------*/
	protected void addWeightUnitControl(
		MigPane pane,
		String uiLabelKey,
		Function<T, ? extends Quantity> getMethod,
		BiConsumer<T, WeightUnit> setMethod,
		Quantity.Unit unit)
	{
		QuantityEditWidget<WeightUnit> widget = new QuantityEditWidget<>(unit);
		pane.add(new Label(StringUtils.getUiString(uiLabelKey)));
		pane.add(widget, "wrap");

		this.weightUnitControls.put(widget, new WeightUnitInfo(getMethod, setMethod, unit));

		this.addWeightUnitListener(setMethod, widget, unit);
	}

	/*-------------------------------------------------------------------------*/
	private void addTemperatureUnitListener(
		BiConsumer<T, TemperatureUnit> setMethod,
		QuantityEditWidget<TemperatureUnit> widget,
		Quantity.Unit unit)
	{
		widget.addListener((observable, oldValue, newValue) ->
		{
			if (target != null && newValue != null)
			{
				if (!refreshing)
				{
					setMethod.accept(target, widget.getQuantity());
				}

				if (detectDirty)
				{
					trackDirty.setDirty(target);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	private void addVolumeUnitListener(
		BiConsumer<T, VolumeUnit> setMethod,
		QuantityEditWidget<VolumeUnit> widget,
		Quantity.Unit unit)
	{
		widget.addListener((observable, oldValue, newValue) ->
		{
			if (target != null && newValue != null)
			{
				if (!refreshing)
				{
					setMethod.accept(target, widget.getQuantity());
				}

				if (detectDirty)
				{
					trackDirty.setDirty(target);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	private void addWeightUnitListener(
		BiConsumer<T, WeightUnit> setMethod,
		QuantityEditWidget<WeightUnit> widget,
		Quantity.Unit unit)
	{
		widget.addListener((observable, oldValue, newValue) ->
		{
			if (target != null && newValue != null)
			{
				if (!refreshing)
				{
					setMethod.accept(target, widget.getQuantity());
				}

				if (detectDirty)
				{
					trackDirty.setDirty(target);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	private void addTimeUnitListener(
		BiConsumer<T, TimeUnit> setMethod,
		QuantityEditWidget<TimeUnit> widget,
		Quantity.Unit unit)
	{
		widget.addListener((observable, oldValue, newValue) ->
		{
			if (target != null && newValue != null)
			{
				if (!refreshing)
				{
					setMethod.accept(target, widget.getQuantity());
				}

				if (detectDirty)
				{
					trackDirty.setDirty(target);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	private class TimeUnitInfo
	{
		private final Function<T, TimeUnit> getMethod;
		private final BiConsumer<T, TimeUnit> setMethod;
		private final Quantity.Unit unit;

		public TimeUnitInfo(Function<T, TimeUnit> getMethod,
			BiConsumer<T, TimeUnit> setMethod,
			Quantity.Unit unit)
		{
			this.getMethod = getMethod;
			this.setMethod = setMethod;
			this.unit = unit;
		}
	}

	/*-------------------------------------------------------------------------*/
	private class TempUnitInfo
	{
		private final Function<T, TemperatureUnit> getMethod;
		private final BiConsumer<T, TemperatureUnit> setMethod;
		private final Quantity.Unit unit;

		public TempUnitInfo(
			Function<T, TemperatureUnit> getMethod,
			BiConsumer<T, TemperatureUnit> setMethod,
			Quantity.Unit unit)
		{
			this.getMethod = getMethod;
			this.setMethod = setMethod;
			this.unit = unit;
		}
	}

	/*-------------------------------------------------------------------------*/
	private class VolUnitInfo
	{
		private final Function<T, VolumeUnit> getMethod;
		private final BiConsumer<T, VolumeUnit> setMethod;
		private final Quantity.Unit unit;

		public VolUnitInfo(
			Function<T, VolumeUnit> getMethod,
			BiConsumer<T, VolumeUnit> setMethod,
			Quantity.Unit unit)
		{
			this.getMethod = getMethod;
			this.setMethod = setMethod;
			this.unit = unit;
		}
	}

	/*-------------------------------------------------------------------------*/
	private class WeightUnitInfo
	{
		private final Function<T, ? extends Quantity> getMethod;
		private final BiConsumer<T, WeightUnit> setMethod;
		private final Quantity.Unit unit;

		public WeightUnitInfo(
			Function<T, ? extends Quantity> getMethod,
			BiConsumer<T, WeightUnit> setMethod,
			Quantity.Unit unit)
		{
			this.getMethod = getMethod;
			this.setMethod = setMethod;
			this.unit = unit;
		}
	}
}
