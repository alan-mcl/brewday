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

package mclachlan.brewday;

import java.util.*;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;

/**
 *
 */
public class StringUtils
{
	public static String getUiString(String key)
	{
		String result = Database.getInstance().getStrings("ui").getProperty(key);

		if (result == null)
		{
			throw new BrewdayException("UI label not found: [" + key + "]");
		}

		return result;
	}

	public static String getUiString(String key, Object... args)
	{
		String result = getUiString(key);

		return String.format(result, args);
	}

	public static String getProcessString(String key)
	{
		String result = Database.getInstance().getStrings("process").getProperty(key);

		if (result == null)
		{
			throw new BrewdayException("UI label not found: [" + key + "]");
		}

		return result;
	}

	public static String getProcessString(String key, Object... args)
	{
		String result = getProcessString(key);

		return String.format(result, args);
	}

	public static String getDocString(String key)
	{
		String result = Database.getInstance().getStrings("document").getProperty(key);

		if (result == null)
		{
			throw new BrewdayException("Document label not found: [" + key + "]");
		}

		return result;
	}

	public static String getDocString(String key, Object... args)
	{
		String result = getDocString(key);

		return String.format(result, args);
	}

	public static String format(double v)
	{
		// passing Locale.ROOT here to force a '.' decimal separator.

		String format = Database.getInstance().getSettings().getStringFormatter(v);
		return String.format(Locale.ROOT, format, v);
	}

	public static String format(double v, Quantity.Unit unit)
	{
		// passing Locale.ROOT here to force a '.' decimal separator.

		String format = Database.getInstance().getSettings().getStringFormatter(v)+"%s";
		return String.format(Locale.ROOT, format, v, unit.abbr());
	}
}
