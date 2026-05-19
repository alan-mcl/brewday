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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Appends Brewday-required {@code .gitignore} entries without overwriting user content.
 */
final class GitIgnoreManager
{
	private static final String MARKER = "# Brewday-managed ignores (do not remove)";

	private static final List<String> REQUIRED_LINES = List.of(
		"/backup/",
		"/sensitive/",
		"*.tmp",
		"*.swp",
		".DS_Store",
		"Thumbs.db");

	private GitIgnoreManager()
	{
	}

	/*-------------------------------------------------------------------------*/
	static void mergeRequiredLines(File localRepo) throws IOException
	{
		File gitignore = new File(localRepo, ".gitignore");
		String existing = gitignore.isFile()
			? Files.readString(gitignore.toPath(), StandardCharsets.UTF_8)
			: "";

		StringBuilder required = new StringBuilder();
		boolean needMarker = !existing.contains(MARKER);

		if (needMarker)
		{
			if (!existing.isEmpty() && !existing.endsWith("\n"))
			{
				required.append('\n');
			}
			required.append(MARKER).append('\n');
		}

		for (String line : REQUIRED_LINES)
		{
			if (!linePresent(existing, line))
			{
				required.append(line).append('\n');
			}
		}

		if (required.length() == 0)
		{
			return;
		}

		try (PrintWriter pw = new PrintWriter(new FileOutputStream(gitignore, true)))
		{
			pw.print(required);
			pw.flush();
		}
	}

	/*-------------------------------------------------------------------------*/
	private static boolean linePresent(String content, String line)
	{
		for (String l : content.split("\n"))
		{
			String trimmed = l.trim();
			if (trimmed.equals(line) || trimmed.equals(line.replace("/", "")))
			{
				return true;
			}
		}
		return false;
	}
}
