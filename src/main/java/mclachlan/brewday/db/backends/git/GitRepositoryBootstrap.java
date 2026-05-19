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
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.util.Log;
import mclachlan.brewday.util.StringUtils;

/**
 * Workflow 1 (new repo) and Workflow 2 (adopt existing) git setup.
 */
final class GitRepositoryBootstrap
{
	private GitRepositoryBootstrap()
	{
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @deprecated Use {@link #setupNewGitBackup} or {@link #adoptExistingRepository}.
	 */
	@Deprecated
	static void enable(
		File localRepo,
		String remoteRepo,
		GitBackend.OutputCollector outputCollector) throws IOException, InterruptedException
	{
		if (GitRepositoryInspector.isGitRepository(localRepo))
		{
			adoptExistingRepository(localRepo, outputCollector, true);
		}
		else
		{
			setupNewGitBackup(localRepo, remoteRepo, outputCollector);
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Workflow 1: initialize a new repository in {@code localRepo}.
	 */
	static void setupNewGitBackup(
		File localRepo,
		String remoteRepo,
		GitBackend.OutputCollector outputCollector) throws IOException, InterruptedException
	{
		Brewday.getInstance().getLog().log(Log.DEBUG, "setup new git backup");
		outputCollector.append("-----------------------------------------\n");

		GitCommandExecutor.verifyGitAvailable();

		if (GitRepositoryInspector.isGitRepository(localRepo))
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.setup.already.repo"));
		}

		GitCommandExecutor.run(localRepo, outputCollector, "init", "-b", "main");
		GitIgnoreManager.mergeRequiredLines(localRepo);
		warnIfIdentityMissing(localRepo, outputCollector);

		if (GitRepositoryInspector.hasPorcelainChanges(localRepo))
		{
			try
			{
				GitLocalCommitService.commitInitial(localRepo, outputCollector);
			}
			catch (BrewdayException e)
			{
				outputCollector.append(StringUtils.getUiString("settings.git.enable.no.initial.commit"));
				outputCollector.append("\n");
			}
		}

		String trimmedRemote = trimToNull(remoteRepo);
		if (trimmedRemote != null)
		{
			GitRemoteTestResult test = GitRemoteConnectivityService.testRemote(
				localRepo, trimmedRemote, outputCollector);
			if (test == GitRemoteTestResult.OkHasCommits)
			{
				throw new BrewdayException(StringUtils.getUiString("settings.git.remote.has.commits"));
			}
			if (test == GitRemoteTestResult.AuthFailed)
			{
				throw new BrewdayException(StringUtils.getUiString("settings.git.remote.auth.failed"));
			}
			if (test == GitRemoteTestResult.Unreachable)
			{
				throw new BrewdayException(StringUtils.getUiString("settings.git.remote.unreachable"));
			}

			GitRemoteSyncService.configureOrigin(localRepo, trimmedRemote, outputCollector);
			GitCommandExecutor.run(localRepo, outputCollector, "remote", "-v");
			GitRemoteSyncService.verifySafeInitialPush(localRepo, trimmedRemote);
			GitRemoteSyncService.pushSafely(localRepo, outputCollector, true);
		}

		markGitEnabledInMemory(outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Workflow 2: adopt an existing repository (no remote changes).
	 *
	 * @param saveSettings when false, only prepares repo on disk (caller repoints cfg and restarts)
	 */
	static void adoptExistingRepository(
		File localRepo,
		GitBackend.OutputCollector outputCollector,
		boolean saveSettings) throws IOException, InterruptedException
	{
		Brewday.getInstance().getLog().log(Log.DEBUG, "adopt existing git repository");
		outputCollector.append("-----------------------------------------\n");

		GitCommandExecutor.verifyGitAvailable();

		GitRepositoryAdoptionValidator.Result validation = GitRepositoryAdoptionValidator.validate(localRepo);
		if (!validation.isValid())
		{
			throw new BrewdayException(validation.getMessage());
		}

		enableExistingRepo(localRepo, outputCollector);

		if (saveSettings)
		{
			markGitEnabledInMemory(outputCollector);
		}
		else
		{
			GitSettingsOnDiskWriter.setGitBackendEnabled(localRepo, true);
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Adds {@code origin} and pushes to an empty remote on an already-enabled local-only repo.
	 */
	static void addRemoteBackup(
		File localRepo,
		String remoteUrl,
		GitBackend.OutputCollector outputCollector) throws IOException, InterruptedException
	{
		GitCommandExecutor.verifyGitAvailable();
		String trimmedRemote = trimToNull(remoteUrl);
		if (trimmedRemote == null)
		{
			throw new BrewdayException("remote URL required");
		}

		if (GitRepositoryInspector.hasOrigin(localRepo))
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.origin.already.configured"));
		}

		GitRemoteTestResult test = GitRemoteConnectivityService.testRemote(
			localRepo, trimmedRemote, outputCollector);
		if (test == GitRemoteTestResult.OkHasCommits)
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.remote.has.commits"));
		}
		if (test == GitRemoteTestResult.AuthFailed)
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.remote.auth.failed"));
		}
		if (test == GitRemoteTestResult.Unreachable)
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.remote.unreachable"));
		}

		GitRemoteSyncService.configureOrigin(localRepo, trimmedRemote, outputCollector);
		GitCommandExecutor.run(localRepo, outputCollector, "remote", "-v");
		GitRemoteSyncService.verifySafeInitialPush(localRepo, trimmedRemote);
		GitRemoteSyncService.pushSafely(localRepo, outputCollector, true);
		GitStatusService.invalidateCache();
	}

	/*-------------------------------------------------------------------------*/
	private static void enableExistingRepo(
		File localRepo,
		GitBackend.OutputCollector outputCollector) throws IOException, InterruptedException
	{
		if (!GitRepositoryInspector.looksLikeBrewdayDatabase(localRepo))
		{
			Brewday.getInstance().getLog().log(
				Log.LOUD, StringUtils.getUiString("settings.git.not.brewday.db"));
		}

		GitIgnoreManager.mergeRequiredLines(localRepo);
		warnIfIdentityMissing(localRepo, outputCollector);

		outputCollector.append(StringUtils.getUiString("settings.git.adopted.existing"));
		outputCollector.append("\n");
	}

	/*-------------------------------------------------------------------------*/
	private static void markGitEnabledInMemory(GitBackend.OutputCollector outputCollector)
		throws IOException
	{
		Settings settings = Database.getInstance().getSettings();
		settings.set(Settings.GIT_BACKEND_ENABLED, "true");
		settings.set(Settings.GIT_AUTO_PUSH, "false");
		settings.set(Settings.GIT_REMOTE_REPO, null);

		Database.getInstance().saveSettings(true);
	}

	/*-------------------------------------------------------------------------*/
	private static void warnIfIdentityMissing(
		File localRepo,
		GitBackend.OutputCollector outputCollector)
	{
		if (!GitRepositoryInspector.hasCommitterIdentity(localRepo))
		{
			String msg = StringUtils.getUiString("settings.git.identity.missing");
			Brewday.getInstance().getLog().log(Log.LOUD, msg);
			outputCollector.append(msg);
			outputCollector.append("\n");
		}
	}

	/*-------------------------------------------------------------------------*/
	private static String trimToNull(String s)
	{
		if (s == null)
		{
			return null;
		}
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}
}
