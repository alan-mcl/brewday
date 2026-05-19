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
 * along with brewday.  If not, see https://www.gnu.org/licenses/.
 */

package mclachlan.brewday.db.backends.git;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.util.AppContentRoot;

/**
 * Updates {@code brewday.cfg} database path (Workflow 2 repoint).
 */
public final class BrewdayConfigWriter
{
	private BrewdayConfigWriter()
	{
	}

	/*-------------------------------------------------------------------------*/
	public static File resolveConfigFile()
	{
		File cfg = AppContentRoot.resolveFile("brewday.cfg");
		if (!cfg.isFile())
		{
			cfg = new File("brewday.cfg");
		}
		if (!cfg.isFile())
		{
			throw new BrewdayException("brewday.cfg not found");
		}
		return cfg;
	}

	/*-------------------------------------------------------------------------*/
	public static String getDatabasePath()
	{
		Properties props = loadConfig();
		return props.getProperty(Brewday.BREWDAY_DB);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Writes {@code mclachlan.brewday.db} using a relative path when under the content root.
	 */
	public static void setDatabasePath(File absoluteDatabaseDir) throws IOException
	{
		File cfg = resolveConfigFile();
		Properties props = loadConfig();
		props.setProperty(Brewday.BREWDAY_DB, toConfigPathValue(absoluteDatabaseDir));
		storeConfig(cfg, props);
	}

	/*-------------------------------------------------------------------------*/
	public static String toConfigPathValue(File absoluteDatabaseDir)
	{
		File abs = absoluteDatabaseDir.getAbsoluteFile();
		try
		{
			File contentRoot = AppContentRoot.resolveFile(".").getAbsoluteFile();
			String rootPath = contentRoot.getCanonicalPath();
			String dbPath = abs.getCanonicalPath();
			if (dbPath.startsWith(rootPath + File.separator))
			{
				String relative = dbPath.substring(rootPath.length() + 1);
				return relative.replace(File.separatorChar, '/');
			}
		}
		catch (IOException ignored)
		{
			// use absolute path below
		}
		return abs.getPath().replace(File.separatorChar, '/');
	}

	/*-------------------------------------------------------------------------*/
	private static Properties loadConfig()
	{
		Properties props = new Properties();
		try (FileInputStream in = new FileInputStream(resolveConfigFile()))
		{
			props.load(in);
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}
		return props;
	}

	/*-------------------------------------------------------------------------*/
	private static void storeConfig(File cfg, Properties props) throws IOException
	{
		List<String> lines = new ArrayList<>();
		if (cfg.isFile())
		{
			lines.addAll(Files.readAllLines(cfg.toPath(), StandardCharsets.UTF_8));
		}

		boolean replaced = false;
		String prefix = Brewday.BREWDAY_DB + "=";
		String newLine = prefix + props.getProperty(Brewday.BREWDAY_DB);
		for (int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i).trim();
			if (line.startsWith(prefix) || line.equals(Brewday.BREWDAY_DB))
			{
				lines.set(i, newLine);
				replaced = true;
				break;
			}
		}
		if (!replaced)
		{
			lines.add(newLine);
		}

		Files.write(cfg.toPath(), lines, StandardCharsets.UTF_8);
	}
}
