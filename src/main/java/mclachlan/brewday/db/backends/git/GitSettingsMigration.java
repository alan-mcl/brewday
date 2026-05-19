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
import mclachlan.brewday.Brewday;
import mclachlan.brewday.Settings;
import mclachlan.brewday.util.Log;

/**
 * One-time migration of legacy Brewday git settings into git config.
 */
public final class GitSettingsMigration
{
	private GitSettingsMigration()
	{
	}

	/*-------------------------------------------------------------------------*/
	public static void migrate(Settings settings, File repoDir)
	{
		String legacyRemote = settings.get(Settings.GIT_REMOTE_REPO);
		if (legacyRemote == null || legacyRemote.isBlank())
		{
			if (settings.get(Settings.GIT_AUTO_PUSH) == null)
			{
				settings.set(Settings.GIT_AUTO_PUSH, "false");
			}
			return;
		}

		if (GitRepositoryInspector.isGitRepository(repoDir)
			&& !GitRepositoryInspector.hasOrigin(repoDir))
		{
			try
			{
				GitRemoteSyncService.configureOrigin(repoDir, legacyRemote.trim(), null);
				Brewday.getInstance().getLog().log(
					Log.DEBUG, "migrated legacy git remote URL to origin");
			}
			catch (IOException | InterruptedException e)
			{
				if (e instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
				Brewday.getInstance().getLog().log(
					Log.LOUD, "could not migrate legacy git remote: " + e.getMessage());
			}
		}

		settings.set(Settings.GIT_REMOTE_REPO, null);
		if (settings.get(Settings.GIT_AUTO_PUSH) == null)
		{
			settings.set(Settings.GIT_AUTO_PUSH, "false");
		}
	}
}
