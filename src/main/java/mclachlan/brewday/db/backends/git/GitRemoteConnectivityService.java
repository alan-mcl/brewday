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

/**
 * Pre-flight remote checks via {@code git ls-remote}.
 */
public final class GitRemoteConnectivityService
{
	private GitRemoteConnectivityService()
	{
	}

	/*-------------------------------------------------------------------------*/
	public static GitRemoteTestResult testRemote(
		File contextDir,
		String remoteUrl,
		GitBackend.OutputCollector outputCollector)
	{
		try
		{
			GitCommandExecutor.verifyGitAvailable();
			String refs = GitCommandExecutor.capture(
				contextDir, outputCollector, "ls-remote", remoteUrl, "HEAD");
			if (refs == null || refs.isBlank())
			{
				return GitRemoteTestResult.OkEmpty;
			}
			return GitRemoteTestResult.OkHasCommits;
		}
		catch (BrewdayException e)
		{
			String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
			if (msg.contains("permission denied")
				|| msg.contains("authentication")
				|| msg.contains("auth"))
			{
				return GitRemoteTestResult.AuthFailed;
			}
			return GitRemoteTestResult.Unreachable;
		}
		catch (IOException | InterruptedException e)
		{
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			return GitRemoteTestResult.Unreachable;
		}
	}
}
