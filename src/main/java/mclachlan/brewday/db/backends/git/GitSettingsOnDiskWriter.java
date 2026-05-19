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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2Utils;

/**
 * Updates {@code settings.json} in a database directory without loading that database.
 */
public final class GitSettingsOnDiskWriter
{
	private GitSettingsOnDiskWriter()
	{
	}

	/*-------------------------------------------------------------------------*/
	@SuppressWarnings("unchecked")
	public static void setGitBackendEnabled(File databaseDir, boolean enabled) throws IOException
	{
		File settingsFile = new File(databaseDir, Database.SETTINGS_JSON);
		if (!settingsFile.isFile())
		{
			throw new BrewdayException("settings.json not found in " + databaseDir);
		}

		Map<String, Object> map;
		try (Reader reader = Files.newBufferedReader(settingsFile.toPath(), StandardCharsets.UTF_8))
		{
			map = new Gson().fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
		}
		if (map == null)
		{
			map = new LinkedHashMap<>();
		}

		map.put(Settings.GIT_BACKEND_ENABLED, enabled ? "true" : "false");
		if (!enabled)
		{
			map.put(Settings.GIT_AUTO_PUSH, "false");
		}

		String json = V2Utils.getJson(map);
		Files.writeString(settingsFile.toPath(), json, StandardCharsets.UTF_8);
	}
}
