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
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.util.Log;
import mclachlan.brewday.util.StringUtils;

/**
 * Conservative remote sync: fetch, pull --ff-only, push (never force).
 */
final class GitRemoteSyncService
{
	private static volatile String lastPushError;

	private GitRemoteSyncService()
	{
	}

	/*-------------------------------------------------------------------------*/
	static String getLastPushError()
	{
		return lastPushError;
	}

	/*-------------------------------------------------------------------------*/
	static void clearLastPushError()
	{
		lastPushError = null;
	}

	/*-------------------------------------------------------------------------*/
	static void configureOrigin(
		File localRepo,
		String remoteUrl,
		GitBackend.OutputCollector outputCollector) throws IOException, InterruptedException
	{
		int hasOrigin = GitCommandExecutor.runAllowNonZero(
			localRepo, outputCollector, "remote", "get-url", "origin");
		if (hasOrigin == 0)
		{
			GitCommandExecutor.run(localRepo, outputCollector, "remote", "set-url", "origin", remoteUrl);
		}
		else
		{
			GitCommandExecutor.run(localRepo, outputCollector, "remote", "add", "origin", remoteUrl);
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Verifies remote is empty or shares history with local before first push.
	 */
	static void verifySafeInitialPush(File localRepo, String remoteUrl)
		throws IOException, InterruptedException
	{
		if (!GitRepositoryInspector.remoteHasCommits(localRepo, remoteUrl))
		{
			return;
		}

		if (!GitRepositoryInspector.localHasCommits(localRepo))
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.remote.has.commits"));
		}

		try
		{
			GitCommandExecutor.run(localRepo, null, "fetch", "origin");
			String base = GitCommandExecutor.capture(localRepo, "merge-base", "HEAD", "origin/HEAD");
			if (base == null || base.isBlank())
			{
				throw new BrewdayException(StringUtils.getUiString("settings.git.remote.unrelated"));
			}
		}
		catch (BrewdayException e)
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.remote.unrelated"));
		}
	}

	/*-------------------------------------------------------------------------*/
	static void pushSafely(
		File localRepo,
		GitBackend.OutputCollector outputCollector,
		boolean setUpstream)
	{
		if (!GitRepositoryInspector.hasOrigin(localRepo))
		{
			if (outputCollector != null)
			{
				outputCollector.append(StringUtils.getUiString("settings.git.no.origin"));
				outputCollector.append("\n");
			}
			return;
		}

		try
		{
			if (setUpstream)
			{
				GitCommandExecutor.run(localRepo, outputCollector, "push", "-u", "origin", "HEAD");
			}
			else
			{
				GitCommandExecutor.run(localRepo, outputCollector, "push", "origin", "HEAD");
			}
			clearLastPushError();
		}
		catch (BrewdayException e)
		{
			lastPushError = e.getMessage();
			reportPushFailure(outputCollector, e);
		}
		catch (IOException | InterruptedException e)
		{
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			lastPushError = e.getMessage();
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	static void syncWithRemote(
		File localRepo,
		GitBackend.OutputCollector outputCollector) throws IOException, InterruptedException
	{
		if (!GitRepositoryInspector.hasOrigin(localRepo))
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.no.origin"));
		}

		if (GitRepositoryInspector.isOperationInProgress(localRepo))
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.operation.in.progress"));
		}

		GitCommandExecutor.run(localRepo, outputCollector, "fetch", "origin");

		try
		{
			GitCommandExecutor.run(localRepo, outputCollector, "pull", "--ff-only");
		}
		catch (BrewdayException e)
		{
			outputCollector.append(StringUtils.getUiString("settings.git.pull.ff.failed"));
			outputCollector.append("\n");
			throw e;
		}

		pushSafely(localRepo, outputCollector, false);
	}

	/*-------------------------------------------------------------------------*/
	private static void reportPushFailure(GitBackend.OutputCollector outputCollector, BrewdayException e)
	{
		String msg = "Git push failed: " + e.getMessage();
		Brewday.getInstance().getLog().log(Log.LOUD, msg);
		if (outputCollector != null)
		{
			outputCollector.append("\n");
			outputCollector.append(StringUtils.getUiString("settings.git.push.failed.hint"));
			outputCollector.append("\n");
		}
	}
}
