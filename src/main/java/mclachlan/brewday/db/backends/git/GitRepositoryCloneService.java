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
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.util.StringUtils;

/**
 * Clones a remote Brewday repository into a new directory (Workflow 2).
 */
final class GitRepositoryCloneService
{
	private GitRepositoryCloneService()
	{
	}

	/*-------------------------------------------------------------------------*/
	static File cloneRepository(
		String remoteUrl,
		File destinationDir,
		GitBackend.OutputCollector outputCollector) throws IOException, InterruptedException
	{
		GitCommandExecutor.verifyGitAvailable();

		if (destinationDir.exists())
		{
			if (destinationDir.isDirectory())
			{
				File[] contents = destinationDir.listFiles();
				if (contents != null && contents.length > 0)
				{
					throw new BrewdayException(
						StringUtils.getUiString("settings.git.clone.dest.not.empty"));
				}
			}
			else
			{
				throw new BrewdayException(
					StringUtils.getUiString("settings.git.clone.dest.exists"));
			}
		}

		GitRemoteTestResult test = GitRemoteConnectivityService.testRemote(
			destinationDir.getParentFile() != null
				? destinationDir.getParentFile()
				: destinationDir,
			remoteUrl,
			outputCollector);
		if (test == GitRemoteTestResult.Unreachable || test == GitRemoteTestResult.AuthFailed)
		{
			throw new BrewdayException(StringUtils.getUiString(
				test == GitRemoteTestResult.AuthFailed
					? "settings.git.remote.auth.failed"
					: "settings.git.remote.unreachable"));
		}
		if (test == GitRemoteTestResult.OkEmpty)
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.clone.remote.empty"));
		}

		File parent = destinationDir.getParentFile();
		if (parent == null)
		{
			throw new BrewdayException("invalid clone destination (no parent directory)");
		}
		if (!parent.isDirectory() && !parent.mkdirs())
		{
			throw new BrewdayException("could not create parent directory: " + parent);
		}

		GitCommandExecutor.run(
			parent,
			outputCollector,
			"clone",
			remoteUrl,
			destinationDir.getName());

		if (!GitRepositoryInspector.looksLikeBrewdayDatabase(destinationDir))
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.adopt.not.brewday"));
		}

		return destinationDir.getAbsoluteFile();
	}
}
