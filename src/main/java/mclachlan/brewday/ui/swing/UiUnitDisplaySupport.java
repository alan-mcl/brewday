package mclachlan.brewday.ui.swing;

import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.UiUnitPreferences;

/**
 * Resolves persisted UI display-unit preferences for Swing widgets.
 */
public final class UiUnitDisplaySupport
{
	private UiUnitDisplaySupport()
	{
	}

	public static UiUnitPreferences current()
	{
		return UiUnitPreferences.from(Database.getInstance().getSettings());
	}

	public static Quantity.Unit temperature()
	{
		return current().get(UiUnitPreferences.Slot.TEMPERATURE);
	}

	public static Quantity.Unit batchVolume()
	{
		return current().get(UiUnitPreferences.Slot.BATCH_VOLUME);
	}

	public static Quantity.Unit smallVolume()
	{
		return current().get(UiUnitPreferences.Slot.SMALL_VOLUME);
	}

	public static Quantity.Unit density()
	{
		return current().get(UiUnitPreferences.Slot.DENSITY);
	}

	public static Quantity.Unit colour()
	{
		return current().get(UiUnitPreferences.Slot.COLOUR);
	}

	public static Quantity.Unit pressure()
	{
		return current().get(UiUnitPreferences.Slot.PRESSURE);
	}

	public static Quantity.Unit carbonation()
	{
		return current().get(UiUnitPreferences.Slot.CARBONATION);
	}

	public static Quantity.Unit length()
	{
		return current().get(UiUnitPreferences.Slot.LENGTH);
	}
}