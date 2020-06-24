package mclachlan.brewday.ui.jfx;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javafx.scene.control.Label;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.Recipe;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class ControlUtils<T>
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
	public ControlUtils(TrackDirty trackDirty)
	{
		this.trackDirty = trackDirty;
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(T t, Recipe recipe)
	{
		detectDirty = false;
		refreshing = true;

		this.target = t;

		for (QuantityEditWidget<TimeUnit> widget : timeUnitControls.keySet())
		{
			if (target != null)
			{
				TimeUnitInfo info = timeUnitControls.get(widget);
				widget.refresh(info.getMethod.apply(target));
			}
		}
		for (QuantityEditWidget<TemperatureUnit> widget : tempUnitControls.keySet())
		{
			if (target != null)
			{
				TempUnitInfo info = tempUnitControls.get(widget);
				widget.refresh(info.getMethod.apply(target));
			}
		}
		for (QuantityEditWidget<VolumeUnit> widget : volumeUnitControls.keySet())
		{
			if (target != null)
			{
				VolUnitInfo info = volumeUnitControls.get(widget);
				widget.refresh(info.getMethod.apply(target));
			}
		}
		for (QuantityEditWidget<WeightUnit> widget : weightUnitControls.keySet())
		{
			if (target != null)
			{
				WeightUnitInfo info = weightUnitControls.get(widget);
				widget.refresh(info.getMethod.apply(target));
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
