/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.db.backends.git;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * In-memory log of all git subprocess invocations for the current Brewday session.
 */
public final class GitCommandSessionLog
{
	private static final int MAX_CHARS = 512 * 1024;

	/** e.g. {@code 19May2026 14:30:45} */
	private static final DateTimeFormatter LOG_TIMESTAMP = DateTimeFormatter.ofPattern(
		"ddMMMyyyy HH:mm:ss", Locale.ENGLISH);

	private static final StringBuilder buffer = new StringBuilder();
	private static final Object lock = new Object();

	private GitCommandSessionLog()
	{
	}

	/*-------------------------------------------------------------------------*/
	public static String formatLogTimestamp()
	{
		return LOG_TIMESTAMP.format(LocalDateTime.now());
	}

	/*-------------------------------------------------------------------------*/
	public static void append(String s)
	{
		if (s == null || s.isEmpty())
		{
			return;
		}
		synchronized (lock)
		{
			buffer.append(s);
			trimIfNeeded();
		}
	}

	/*-------------------------------------------------------------------------*/
	public static void appendLine(String line)
	{
		append(line);
		if (line == null || line.isEmpty() || line.endsWith("\n"))
		{
			return;
		}
		append("\n");
	}

	/*-------------------------------------------------------------------------*/
	public static String snapshot()
	{
		synchronized (lock)
		{
			return buffer.toString();
		}
	}

	/*-------------------------------------------------------------------------*/
	public static void clear()
	{
		synchronized (lock)
		{
			buffer.setLength(0);
		}
	}

	/*-------------------------------------------------------------------------*/
	static GitBackend.OutputCollector asCollector()
	{
		return GitCommandSessionLog::append;
	}

	/*-------------------------------------------------------------------------*/
	private static void trimIfNeeded()
	{
		if (buffer.length() <= MAX_CHARS)
		{
			return;
		}
		int excess = buffer.length() - MAX_CHARS;
		buffer.delete(0, excess);
		int nl = buffer.indexOf("\n");
		if (nl >= 0 && nl < buffer.length() - 1)
		{
			buffer.delete(0, nl + 1);
		}
	}
}
