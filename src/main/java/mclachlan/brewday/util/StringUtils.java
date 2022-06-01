/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import mclachlan.brewday.BrewdayException;
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
		String format = Database.getInstance().getSettings().getDecimalFormatter(v);
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setGroupingSeparator(' ');
		dfs.setDecimalSeparator('.');
		return new DecimalFormat(format, dfs).format(v);
	}

	public static String format(double v, Quantity.Unit unit)
	{
		String format = Database.getInstance().getSettings().getDecimalFormatter(v);
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setGroupingSeparator(' ');
		dfs.setDecimalSeparator('.');
		return new DecimalFormat(format, dfs).format(v) + unit.abbr();
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args)
	{
		Database.getInstance().loadAll();

		System.out.println(format(1D));
		System.out.println(format(1.1D));
		System.out.println(format(1.12D));
		System.out.println(format(1.123D));
		System.out.println(format(1.1234D));
		System.out.println(format(1.88888D));

		System.out.println(format(11D));
		System.out.println(format(11.1D));
		System.out.println(format(11.12D));
		System.out.println(format(11.123D));
		System.out.println(format(11.1234D));
		System.out.println(format(11.88888D));

		System.out.println(format(101D));
		System.out.println(format(101.1D));
		System.out.println(format(101.12D));
		System.out.println(format(101.123D));
		System.out.println(format(101.1234D));
		System.out.println(format(101.88888D));

		System.out.println(format(1001D));
		System.out.println(format(1001.1D));
		System.out.println(format(1001.12D));
		System.out.println(format(1001.123D));
		System.out.println(format(1001.1234D));
		System.out.println(format(1001.88888D));

	}
}
