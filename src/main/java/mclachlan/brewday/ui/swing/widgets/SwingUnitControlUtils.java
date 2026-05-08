package mclachlan.brewday.ui.swing.widgets;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

/**
 * Swing analogue of JFX {@code UnitControlUtils} (time + temperature only for Phase 13b).
 */
public class SwingUnitControlUtils<T extends ProcessStep>
{
	private final DirtyStateService dirtyState;
	private final BooleanSupplier allowMarkDirty;

	private T target;
	private boolean refreshing;

	private final Map<SwingQuantityEditWidget<TimeUnit>, TimeInfo<T>> timeWidgets = new HashMap<>();
	private final Map<SwingQuantityEditWidget<TemperatureUnit>, TempInfo<T>> tempWidgets = new HashMap<>();

	public SwingUnitControlUtils(DirtyStateService dirtyState, BooleanSupplier allowMarkDirty)
	{
		this.dirtyState = dirtyState;
		this.allowMarkDirty = allowMarkDirty;
	}

	public void registerTimeUnit(SwingQuantityEditWidget<TimeUnit> w,
		Function<T, TimeUnit> get, BiConsumer<T, TimeUnit> set, Quantity.Unit unit)
	{
		timeWidgets.put(w, new TimeInfo<>(get, set, unit));
		w.addQuantityChangeListener(v ->
		{
			if (target != null && !refreshing && allowMarkDirty.getAsBoolean())
			{
				set.accept(target, v);
				dirtyState.markDirty(target);
			}
		});
	}

	public void registerTemperatureUnit(SwingQuantityEditWidget<TemperatureUnit> w,
		Function<T, TemperatureUnit> get, BiConsumer<T, TemperatureUnit> set, Quantity.Unit unit)
	{
		tempWidgets.put(w, new TempInfo<>(get, set, unit));
		w.addQuantityChangeListener(v ->
		{
			if (target != null && !refreshing && allowMarkDirty.getAsBoolean())
			{
				set.accept(target, v);
				dirtyState.markDirty(target);
			}
		});
	}

	public void refresh(T newTarget)
	{
		refreshing = true;
		this.target = newTarget;
		for (var e : timeWidgets.entrySet())
		{
			SwingQuantityEditWidget<TimeUnit> w = e.getKey();
			TimeInfo<T> info = e.getValue();
			w.setQuantity(newTarget != null ? info.getter().apply(newTarget) : null);
		}
		for (var e : tempWidgets.entrySet())
		{
			SwingQuantityEditWidget<TemperatureUnit> w = e.getKey();
			TempInfo<T> info = e.getValue();
			w.setQuantity(newTarget != null ? info.getter().apply(newTarget) : null);
		}
		refreshing = false;
	}

	private record TimeInfo<T>(Function<T, TimeUnit> getter, BiConsumer<T, TimeUnit> setter, Quantity.Unit unit)
	{
	}

	private record TempInfo<T>(Function<T, TemperatureUnit> getter, BiConsumer<T, TemperatureUnit> setter, Quantity.Unit unit)
	{
	}
}
