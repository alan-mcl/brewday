package mclachlan.brewday.ui.swing.widgets;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

/**
 * Swing analogue of JFX {@code UnitControlUtils}: time, temperature, and quantity-select widgets.
 */
public class SwingUnitControlUtils<T>
{
	private final DirtyStateService dirtyState;
	private final BooleanSupplier allowMarkDirty;

	private T target;
	private boolean refreshing;

	private final Map<SwingQuantityEditWidget<TimeUnit>, TimeInfo<T>> timeWidgets = new HashMap<>();
	private final Map<SwingQuantityEditWidget<TemperatureUnit>, TempInfo<T>> tempWidgets = new HashMap<>();
	private final Map<SwingQuantitySelectAndEditWidget, QtyInfo<T>> qtyWidgets = new HashMap<>();

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

	public void registerQuantitySelect(SwingQuantitySelectAndEditWidget w,
		Function<T, Quantity> qGet, BiConsumer<T, Quantity> qSet,
		Function<T, Quantity.Unit> unitGet, BiConsumer<T, Quantity.Unit> unitSet,
		Quantity.Type... allowedTypes)
	{
		if (allowedTypes == null || allowedTypes.length == 0)
		{
			throw new IllegalArgumentException("allowedTypes required");
		}
		qtyWidgets.put(w, new QtyInfo<>(qGet, qSet, unitGet, unitSet, allowedTypes));
		Runnable apply = () ->
		{
			if (target == null || refreshing || !allowMarkDirty.getAsBoolean())
			{
				return;
			}
			qSet.accept(target, w.getQuantity());
			Quantity.Unit u = (Quantity.Unit)w.getUnitCombo().getSelectedItem();
			if (u != null)
			{
				unitSet.accept(target, u);
			}
			dirtyState.markDirty(target);
		};
		JTextField tf = w.getTextField();
		tf.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				apply.run();
			}
		});
		JComboBox<Quantity.Unit> combo = w.getUnitCombo();
		combo.addActionListener(e -> apply.run());
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
		for (var e : qtyWidgets.entrySet())
		{
			SwingQuantitySelectAndEditWidget w = e.getKey();
			QtyInfo<T> info = e.getValue();
			if (newTarget != null)
			{
				Quantity.Unit u = info.unitGetter().apply(newTarget);
				w.setUnitOptions(u, info.allowedTypes());
				w.setQuantity(info.qtyGetter().apply(newTarget));
			}
			else
			{
				Quantity.Type[] types = info.allowedTypes();
				List<Quantity.Unit> opts = QuantityUnitOptions.unitsForTypes(types);
				w.setUnitOptions(opts.isEmpty() ? Quantity.Unit.GRAMS : opts.get(0), types);
				w.setQuantity(null);
			}
		}
		refreshing = false;
	}

	private record TimeInfo<T>(Function<T, TimeUnit> getter, BiConsumer<T, TimeUnit> setter, Quantity.Unit unit)
	{
	}

	private record TempInfo<T>(Function<T, TemperatureUnit> getter, BiConsumer<T, TemperatureUnit> setter, Quantity.Unit unit)
	{
	}

	private record QtyInfo<T>(
		Function<T, Quantity> qtyGetter,
		BiConsumer<T, Quantity> qtySetter,
		Function<T, Quantity.Unit> unitGetter,
		BiConsumer<T, Quantity.Unit> unitSetter,
		Quantity.Type[] allowedTypes)
	{
	}
}
