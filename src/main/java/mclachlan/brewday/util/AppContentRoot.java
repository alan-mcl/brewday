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

package mclachlan.brewday.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves the directory that contains {@code brewday.cfg} and bundled {@code data/}
 * (jpackage Linux app-image runs with cwd {@code .../Brewday/bin}, content lives in {@code .../Brewday/lib/app/}).
 */
public final class AppContentRoot
{
	public static final String PROPERTY = "brewday.content.root";

	private AppContentRoot()
	{
	}

	/**
	 * If {@link #PROPERTY} is not already set, detect {@code brewday.cfg} and set the property
	 * to its parent directory's absolute path. Call from {@code SwingApp.main} before any
	 * class that triggers {@link mclachlan.brewday.Brewday} static init.
	 */
	public static void install()
	{
		if (System.getProperty(PROPERTY) != null && !System.getProperty(PROPERTY).isEmpty())
		{
			return;
		}

		Path cwd = Paths.get("").toAbsolutePath().normalize();
		Path[] candidates = new Path[]
			{
				cwd.resolve("brewday.cfg"),
				cwd.resolve("lib").resolve("app").resolve("brewday.cfg"),
				cwd.resolve("..").resolve("lib").resolve("app").resolve("brewday.cfg").normalize(),
			};

		for (Path cfg : candidates)
		{
			if (Files.isRegularFile(cfg))
			{
				String root = cfg.getParent().toString();
				System.setProperty(PROPERTY, root);
				return;
			}
		}
	}

	public static Path resolvePath(String relative)
	{
		if (relative == null || relative.isEmpty())
		{
			return Paths.get("");
		}
		File asFile = new File(relative);
		if (asFile.isAbsolute())
		{
			return asFile.toPath();
		}
		String root = System.getProperty(PROPERTY);
		if (root == null || root.isEmpty())
		{
			return Paths.get(relative);
		}
		return Paths.get(root).resolve(relative).normalize();
	}

	public static File resolveFile(String relative)
	{
		return resolvePath(relative).toFile();
	}
}
