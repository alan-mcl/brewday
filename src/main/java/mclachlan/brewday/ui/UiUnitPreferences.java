/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mclachlan.brewday.Settings;
import mclachlan.brewday.math.Quantity;

import static mclachlan.brewday.math.Quantity.Unit.BAR;
import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.CENTIMETRE;
import static mclachlan.brewday.math.Quantity.Unit.EBC;
import static mclachlan.brewday.math.Quantity.Unit.FAHRENHEIT;
import static mclachlan.brewday.math.Quantity.Unit.FOOT;
import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.math.Quantity.Unit.GRAMS_PER_LITRE;
import static mclachlan.brewday.math.Quantity.Unit.GU;
import static mclachlan.brewday.math.Quantity.Unit.INCH;
import static mclachlan.brewday.math.Quantity.Unit.KELVIN;
import static mclachlan.brewday.math.Quantity.Unit.KILOGRAMS;
import static mclachlan.brewday.math.Quantity.Unit.KILOMETER;
import static mclachlan.brewday.math.Quantity.Unit.KPA;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.LOVIBOND;
import static mclachlan.brewday.math.Quantity.Unit.METRE;
import static mclachlan.brewday.math.Quantity.Unit.MILE;
import static mclachlan.brewday.math.Quantity.Unit.MILLILITRES;
import static mclachlan.brewday.math.Quantity.Unit.MILLIMETRE;
import static mclachlan.brewday.math.Quantity.Unit.OUNCES;
import static mclachlan.brewday.math.Quantity.Unit.PACKET_11_G;
import static mclachlan.brewday.math.Quantity.Unit.PLATO;
import static mclachlan.brewday.math.Quantity.Unit.POUNDS;
import static mclachlan.brewday.math.Quantity.Unit.PSI;
import static mclachlan.brewday.math.Quantity.Unit.SPECIFIC_GRAVITY;
import static mclachlan.brewday.math.Quantity.Unit.SRM;
import static mclachlan.brewday.math.Quantity.Unit.US_FLUID_OUNCE;
import static mclachlan.brewday.math.Quantity.Unit.US_GALLON;
import static mclachlan.brewday.math.Quantity.Unit.VOLUMES;
import static mclachlan.brewday.math.Quantity.Unit.YARD;

/**
 * Cosmetic UI display units for brewing metrics, persisted in {@link Settings}.
 * Does not change canonical quantity storage or JSON field semantics.
 */
public final class UiUnitPreferences
{
	public enum Slot
	{
		FERMENTABLE_WEIGHT(Settings.UX_UNIT_FERMENTABLE_WEIGHT, KILOGRAMS),
		HOP_MISC_WEIGHT(Settings.UX_UNIT_HOP_MISC_WEIGHT, GRAMS),
		YEAST_WEIGHT(Settings.UX_UNIT_YEAST_WEIGHT, PACKET_11_G),
		BATCH_VOLUME(Settings.UX_UNIT_BATCH_VOLUME, LITRES),
		SMALL_VOLUME(Settings.UX_UNIT_SMALL_VOLUME, MILLILITRES),
		TEMPERATURE(Settings.UX_UNIT_TEMPERATURE, CELSIUS),
		DENSITY(Settings.UX_UNIT_DENSITY, SPECIFIC_GRAVITY),
		COLOUR(Settings.UX_UNIT_COLOUR, SRM),
		PRESSURE(Settings.UX_UNIT_PRESSURE, KPA),
		CARBONATION(Settings.UX_UNIT_CARBONATION, VOLUMES),
		LENGTH(Settings.UX_UNIT_LENGTH, METRE);

		private final String settingsKey;
		private final Quantity.Unit defaultUnit;

		Slot(String settingsKey, Quantity.Unit defaultUnit)
		{
			this.settingsKey = settingsKey;
			this.defaultUnit = defaultUnit;
		}

		public String getSettingsKey()
		{
			return settingsKey;
		}

		public Quantity.Unit getDefaultUnit()
		{
			return defaultUnit;
		}

		public Quantity.Type getQuantityType()
		{
			return switch (this)
			{
				case FERMENTABLE_WEIGHT, HOP_MISC_WEIGHT, YEAST_WEIGHT -> Quantity.Type.WEIGHT;
				case BATCH_VOLUME, SMALL_VOLUME -> Quantity.Type.VOLUME;
				case TEMPERATURE -> Quantity.Type.TEMPERATURE;
				case DENSITY -> Quantity.Type.FLUID_DENSITY;
				case COLOUR -> Quantity.Type.COLOUR;
				case PRESSURE -> Quantity.Type.PRESSURE;
				case CARBONATION -> Quantity.Type.CARBONATION;
				case LENGTH -> Quantity.Type.LENGTH;
			};
		}
	}

	private static final Map<Slot, Quantity.Unit> METRIC_UNITS = Map.ofEntries(
		Map.entry(Slot.FERMENTABLE_WEIGHT, KILOGRAMS),
		Map.entry(Slot.HOP_MISC_WEIGHT, GRAMS),
		Map.entry(Slot.YEAST_WEIGHT, PACKET_11_G),
		Map.entry(Slot.BATCH_VOLUME, LITRES),
		Map.entry(Slot.SMALL_VOLUME, MILLILITRES),
		Map.entry(Slot.TEMPERATURE, CELSIUS),
		Map.entry(Slot.DENSITY, SPECIFIC_GRAVITY),
		Map.entry(Slot.COLOUR, SRM),
		Map.entry(Slot.PRESSURE, KPA),
		Map.entry(Slot.CARBONATION, VOLUMES),
		Map.entry(Slot.LENGTH, METRE));

	private static final Map<Slot, Quantity.Unit> IMPERIAL_UNITS = Map.ofEntries(
		Map.entry(Slot.FERMENTABLE_WEIGHT, POUNDS),
		Map.entry(Slot.HOP_MISC_WEIGHT, OUNCES),
		Map.entry(Slot.YEAST_WEIGHT, PACKET_11_G),
		Map.entry(Slot.BATCH_VOLUME, US_GALLON),
		Map.entry(Slot.SMALL_VOLUME, US_FLUID_OUNCE),
		Map.entry(Slot.TEMPERATURE, FAHRENHEIT),
		Map.entry(Slot.DENSITY, SPECIFIC_GRAVITY),
		Map.entry(Slot.COLOUR, SRM),
		Map.entry(Slot.PRESSURE, PSI),
		Map.entry(Slot.CARBONATION, VOLUMES),
		Map.entry(Slot.LENGTH, FOOT));

	private final EnumMap<Slot, Quantity.Unit> units;

	public UiUnitPreferences(EnumMap<Slot, Quantity.Unit> units)
	{
		this.units = new EnumMap<>(units);
		for (Slot slot : Slot.values())
		{
			if (!this.units.containsKey(slot))
			{
				this.units.put(slot, slot.getDefaultUnit());
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public static UiUnitPreferences defaults()
	{
		EnumMap<Slot, Quantity.Unit> map = new EnumMap<>(Slot.class);
		for (Slot slot : Slot.values())
		{
			map.put(slot, slot.getDefaultUnit());
		}
		return new UiUnitPreferences(map);
	}

	/*-------------------------------------------------------------------------*/
	public static UiUnitPreferences metric()
	{
		return new UiUnitPreferences(new EnumMap<>(METRIC_UNITS));
	}

	/*-------------------------------------------------------------------------*/
	public static UiUnitPreferences imperial()
	{
		return new UiUnitPreferences(new EnumMap<>(IMPERIAL_UNITS));
	}

	/*-------------------------------------------------------------------------*/
	public static UiUnitPreferences from(Settings settings)
	{
		EnumMap<Slot, Quantity.Unit> map = new EnumMap<>(Slot.class);
		for (Slot slot : Slot.values())
		{
			map.put(slot, parseUnit(
				settings.get(slot.getSettingsKey()),
				slot.getDefaultUnit(),
				slotOptions(slot)));
		}
		return new UiUnitPreferences(map);
	}

	/*-------------------------------------------------------------------------*/
	public void persist(Settings settings)
	{
		for (Slot slot : Slot.values())
		{
			settings.set(slot.getSettingsKey(), get(slot).name());
		}
	}

	/*-------------------------------------------------------------------------*/
	public static void clearPersisted(Settings settings)
	{
		for (Slot slot : Slot.values())
		{
			settings.set(slot.getSettingsKey(), null);
		}
	}

	/*-------------------------------------------------------------------------*/
	public Quantity.Unit get(Slot slot)
	{
		return units.get(slot);
	}

	/*-------------------------------------------------------------------------*/
	public UiUnitPreferences with(Slot slot, Quantity.Unit unit)
	{
		EnumMap<Slot, Quantity.Unit> copy = new EnumMap<>(units);
		copy.put(slot, parseUnit(unit == null ? null : unit.name(), slot.getDefaultUnit(), slotOptions(slot)));
		return new UiUnitPreferences(copy);
	}

	/*-------------------------------------------------------------------------*/
	public static List<Quantity.Unit> slotOptions(Slot slot)
	{
		return switch (slot)
		{
			case FERMENTABLE_WEIGHT, HOP_MISC_WEIGHT, YEAST_WEIGHT -> List.of(
				GRAMS, KILOGRAMS, OUNCES, POUNDS, PACKET_11_G);
			case BATCH_VOLUME, SMALL_VOLUME -> List.of(
				MILLILITRES, LITRES, US_FLUID_OUNCE, US_GALLON);
			case TEMPERATURE -> List.of(CELSIUS, KELVIN, FAHRENHEIT);
			case DENSITY -> List.of(GU, SPECIFIC_GRAVITY, PLATO);
			case COLOUR -> List.of(SRM, LOVIBOND, EBC);
			case PRESSURE -> List.of(KPA, PSI, BAR);
			case CARBONATION -> List.of(GRAMS_PER_LITRE, VOLUMES);
			case LENGTH -> List.of(
				MILLIMETRE, CENTIMETRE, METRE, KILOMETER, INCH, FOOT, YARD, MILE);
		};
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * For fields that are normally mm or cm: use inches when the length preference is imperial.
	 */
	public Quantity.Unit getSmallLengthUnit(boolean currentlyCentimetre)
	{
		if (isImperialLength(get(Slot.LENGTH)))
		{
			return INCH;
		}
		return currentlyCentimetre ? CENTIMETRE : MILLIMETRE;
	}

	/*-------------------------------------------------------------------------*/
	public static boolean isImperialLength(Quantity.Unit unit)
	{
		return unit == INCH || unit == FOOT || unit == YARD || unit == MILE;
	}

	/*-------------------------------------------------------------------------*/
	public static Quantity.Unit parseUnit(String raw, Quantity.Unit fallback, List<Quantity.Unit> allowed)
	{
		if (raw == null || raw.isBlank())
		{
			return fallback;
		}
		try
		{
			Quantity.Unit parsed = Quantity.Unit.valueOf(raw.trim());
			if (allowed.contains(parsed))
			{
				return parsed;
			}
		}
		catch (IllegalArgumentException e)
		{
			// fall through
		}
		return fallback;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Preferred display unit for a fixed-unit quantity field based on its {@link Quantity.Type}.
	 */
	public Quantity.Unit displayUnitFor(Quantity quantity)
	{
		if (quantity == null)
		{
			return null;
		}
		return switch (quantity.getType())
		{
			case TEMPERATURE -> get(Slot.TEMPERATURE);
			case VOLUME -> get(Slot.BATCH_VOLUME);
			case FLUID_DENSITY -> get(Slot.DENSITY);
			case COLOUR -> get(Slot.COLOUR);
			case PRESSURE -> get(Slot.PRESSURE);
			case CARBONATION -> get(Slot.CARBONATION);
			case LENGTH -> get(Slot.LENGTH);
			case WEIGHT -> get(Slot.FERMENTABLE_WEIGHT);
			default -> quantity.getUnit();
		};
	}

	/*-------------------------------------------------------------------------*/
	public static List<Slot> orderedSlots()
	{
		return List.of(Slot.values());
	}

	/*-------------------------------------------------------------------------*/
	private static Quantity.Unit parseUnit(String raw, Quantity.Unit fallback, Quantity.Unit... allowed)
	{
		List<Quantity.Unit> list = new ArrayList<>(allowed.length);
		for (Quantity.Unit unit : allowed)
		{
			list.add(unit);
		}
		return parseUnit(raw, fallback, list);
	}
}
