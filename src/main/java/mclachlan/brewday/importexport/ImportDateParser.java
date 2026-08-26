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

package mclachlan.brewday.importexport;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Shared date parsing for import formats (BeerXML, batch CSV, etc.).
 */
public final class ImportDateParser
{
	private static final DateTimeFormatter[] FORMATTERS =
		{
			DateTimeFormatter.ISO_DATE,
			DateTimeFormatter.ISO_LOCAL_DATE,
			DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("MM dd yy", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("dd MM yy", Locale.ENGLISH),
			DateTimeFormatter.ofPattern("yyyy MM dd", Locale.ENGLISH),
		};

	private ImportDateParser()
	{
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Parses a date string using known import formats. Throws if none match.
	 */
	public static LocalDate parseLocalDate(String text)
	{
		if (text == null || text.isBlank())
		{
			throw new DateTimeException("blank date");
		}

		String toParse = normalizeSeparators(text.trim());
		DateTimeException last = null;

		for (DateTimeFormatter formatter : FORMATTERS)
		{
			try
			{
				return LocalDate.parse(toParse, formatter);
			}
			catch (DateTimeException e)
			{
				last = e;
			}
		}

		throw last != null ? last : new DateTimeException("unparseable date [" + text + "]");
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Parses a date string when the field is optional; returns {@code null} for blank or unknown formats.
	 */
	public static LocalDate tryParseLocalDate(String text)
	{
		if (text == null || text.isBlank())
		{
			return null;
		}

		try
		{
			return parseLocalDate(text);
		}
		catch (DateTimeException ignored)
		{
			return null;
		}
	}

	/*-------------------------------------------------------------------------*/
	private static String normalizeSeparators(String text)
	{
		return text.replaceAll("/-\\\\", " ");
	}
}
