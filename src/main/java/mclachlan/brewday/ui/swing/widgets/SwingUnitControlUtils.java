package mclachlan.brewday.ui.swing.widgets;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
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
import mclachlan.brewday.ui.UiUnitPreferences;
import mclachlan.brewday.ui.swing.UiUnitDisplaySupport;
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
	/** Generic {@link Quantity} subtypes (pH, volume, density, carbonation, percentage, ...). */
	private final Map<SwingQuantityEditWidget<?>, GenericQtyInfo<T>> genericQtyWidgets = new HashMap<>();

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
		registerQuantitySelect(w, qGet, qSet, unitGet, unitSet, null, allowedTypes);
	}

	/**
	 * Like {@link #registerQuantitySelect} but merges {@code extraMeasurementType.apply(target)} into
	 * allowed {@link Quantity.Type}s on refresh (for misc lines whose reference ingredient adds a type
	 * beyond weight/volume).
	 */
	public void registerQuantitySelect(SwingQuantitySelectAndEditWidget w,
		Function<T, Quantity> qGet, BiConsumer<T, Quantity> qSet,
		Function<T, Quantity.Unit> unitGet, BiConsumer<T, Quantity.Unit> unitSet,
		Function<T, Quantity.Type> extraMeasurementType,
		Quantity.Type... allowedTypes)
	{
		if (allowedTypes == null || allowedTypes.length == 0)
		{
			throw new IllegalArgumentException("allowedTypes required");
		}
		qtyWidgets.put(w, new QtyInfo<>(qGet, qSet, unitGet, unitSet, allowedTypes, extraMeasurementType));
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

	/**
	 * Registers a {@link SwingQuantityEditWidget} for any {@link Quantity} subtype.
	 *
	 * @param set pass {@code null} for read-only display widgets (no dirty on change).
	 */
	public <Q extends Quantity> void registerQuantityEdit(SwingQuantityEditWidget<Q> w,
		Function<T, Q> get, BiConsumer<T, Q> set)
	{
		genericQtyWidgets.put(w, new GenericQtyInfo<>(get, set));
		if (set != null)
		{
			w.addQuantityChangeListener(v ->
			{
				if (target != null && !refreshing && allowMarkDirty.getAsBoolean())
				{
					set.accept(target, v);
					dirtyState.markDirty(target);
				}
			});
		}
	}

	public void refresh(T newTarget)
	{
		refreshing = true;
		this.target = newTarget;
		UiUnitPreferences uiUnits = UiUnitDisplaySupport.current();
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
			w.setUnit(uiUnits.get(UiUnitPreferences.Slot.TEMPERATURE));
			w.setQuantity(newTarget != null ? info.getter().apply(newTarget) : null);
		}
		for (var e : qtyWidgets.entrySet())
		{
			SwingQuantitySelectAndEditWidget w = e.getKey();
			QtyInfo<T> info = e.getValue();
			if (newTarget != null)
			{
				Quantity.Unit u = info.unitGetter().apply(newTarget);
				Quantity.Type[] types = mergedQuantityTypes(info, newTarget);
				w.setUnitOptions(u, types);
				w.setQuantity(info.qtyGetter().apply(newTarget));
			}
			else
			{
				Quantity.Type[] types = mergedQuantityTypes(info, null);
				List<Quantity.Unit> opts = QuantityUnitOptions.unitsForTypes(types);
				w.setUnitOptions(opts.isEmpty() ? Quantity.Unit.GRAMS : opts.get(0), types);
				w.setQuantity(null);
			}
		}
		for (var e : genericQtyWidgets.entrySet())
		{
			SwingQuantityEditWidget<?> raw = e.getKey();
			GenericQtyInfo<T> info = e.getValue();
			Quantity q = newTarget != null ? info.getter().apply(newTarget) : null;
			if (q != null)
			{
				raw.setUnit(uiUnits.displayUnitFor(q));
			}
			putGenericQuantity(raw, q);
		}
		refreshing = false;
	}

	@SuppressWarnings("unchecked")
	private static <Q extends Quantity> void putGenericQuantity(SwingQuantityEditWidget<?> w, Quantity q)
	{
		((SwingQuantityEditWidget<Q>)w).setQuantity((Q)q);
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
		Quantity.Type[] allowedTypes,
		Function<T, Quantity.Type> extraMeasurementType)
	{
	}

	private static <T> Quantity.Type[] mergedQuantityTypes(QtyInfo<T> info, T newTarget)
	{
		Quantity.Type[] base = info.allowedTypes();
		if (info.extraMeasurementType() == null || newTarget == null)
		{
			return base;
		}
		Quantity.Type extra = info.extraMeasurementType().apply(newTarget);
		if (extra == null)
		{
			return base;
		}
		for (Quantity.Type t : base)
		{
			if (t == extra)
			{
				return base;
			}
		}
		Quantity.Type[] out = Arrays.copyOf(base, base.length + 1);
		out[base.length] = extra;
		return out;
	}

	private record GenericQtyInfo<T>(Function<T, ? extends Quantity> getter, BiConsumer<T, ?> setter)
	{
	}
}
