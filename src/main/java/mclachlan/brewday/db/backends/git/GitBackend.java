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
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.util.Log;
import mclachlan.brewday.util.StringUtils;

/**
 * Optional git backend: version-control the local database directory after save.
 * Remote sync is explicit; push on save is opt-in via {@link Settings#GIT_AUTO_PUSH}.
 */
public class GitBackend
{
	private static final ConcurrentMap<String, ReentrantLock> REPO_LOCKS = new ConcurrentHashMap<>();

	/*-------------------------------------------------------------------------*/

	/**
	 * @param localRepo  The directory to be the local git repo
	 * @param remoteRepo Optional remote URL at enable time only (not persisted)
	 */
	public void enable(
		File localRepo,
		String remoteRepo,
		OutputCollector outputCollector)
	{
		withRepoLock(localRepo, () ->
		{
			try
			{
				GitRepositoryBootstrap.enable(localRepo, remoteRepo, outputCollector);
				GitStatusService.invalidateCache();
			}
			catch (IOException | InterruptedException e)
			{
				if (e instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
				throw new BrewdayException(e);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	/** Workflow 1: new repository in the current database directory. */
	public void setupNewGitBackup(
		File localRepo,
		String remoteRepo,
		OutputCollector outputCollector)
	{
		withRepoLock(localRepo, () ->
		{
			try
			{
				GitRepositoryBootstrap.setupNewGitBackup(localRepo, remoteRepo, outputCollector);
				GitStatusService.invalidateCache();
			}
			catch (IOException | InterruptedException e)
			{
				if (e instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
				throw new BrewdayException(e);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	/** Workflow 2: adopt repository at {@code localRepo} (same as current db dir). */
	public void adoptExistingRepository(
		File localRepo,
		OutputCollector outputCollector)
	{
		withRepoLock(localRepo, () ->
		{
			try
			{
				GitRepositoryBootstrap.adoptExistingRepository(localRepo, outputCollector, true);
				GitStatusService.invalidateCache();
			}
			catch (IOException | InterruptedException e)
			{
				if (e instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
				throw new BrewdayException(e);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Workflow 2: clone remote into {@code destinationDir}, enable git on disk, repoint cfg.
	 *
	 * @return true if {@code brewday.cfg} was updated (restart required)
	 */
	public boolean cloneAndPrepareRepository(
		String remoteUrl,
		File destinationDir,
		OutputCollector outputCollector)
	{
		final boolean[] restartRequired = { false };
		File parent = destinationDir.getParentFile();
		File lockDir = parent != null ? parent : destinationDir;
		withRepoLock(lockDir, () ->
		{
			try
			{
				File cloned = GitRepositoryCloneService.cloneRepository(
					remoteUrl, destinationDir, outputCollector);
				GitRepositoryBootstrap.adoptExistingRepository(cloned, outputCollector, false);
				BrewdayConfigWriter.setDatabasePath(cloned);
				restartRequired[0] = true;
				GitStatusService.invalidateCache();
			}
			catch (IOException | InterruptedException e)
			{
				if (e instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
				throw new BrewdayException(e);
			}
		});
		return restartRequired[0];
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Workflow 2: adopt repo at {@code repoDir} and repoint {@code brewday.cfg} if needed.
	 *
	 * @return true if restart is required
	 */
	public boolean adoptRepositoryAtPath(
		File repoDir,
		OutputCollector outputCollector)
	{
		final boolean[] restartRequired = { false };
		withRepoLock(repoDir, () ->
		{
			try
			{
				File currentDb = Database.getInstance().getLocalStorageDirectory();
				if (repoDir.getAbsoluteFile().equals(currentDb.getAbsoluteFile()))
				{
					GitRepositoryBootstrap.adoptExistingRepository(repoDir, outputCollector, true);
				}
				else
				{
					GitRepositoryBootstrap.adoptExistingRepository(repoDir, outputCollector, false);
					BrewdayConfigWriter.setDatabasePath(repoDir);
					restartRequired[0] = true;
				}
				GitStatusService.invalidateCache();
			}
			catch (IOException | InterruptedException e)
			{
				if (e instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
				throw new BrewdayException(e);
			}
		});
		return restartRequired[0];
	}

	/*-------------------------------------------------------------------------*/
	public void addRemoteBackup(
		File localRepo,
		String remoteUrl,
		OutputCollector outputCollector)
	{
		withRepoLock(localRepo, () ->
		{
			try
			{
				GitRepositoryBootstrap.addRemoteBackup(localRepo, remoteUrl, outputCollector);
			}
			catch (IOException | InterruptedException e)
			{
				if (e instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
				throw new BrewdayException(e);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	public GitRemoteTestResult testRemoteConnectivity(
		File contextDir,
		String remoteUrl,
		OutputCollector outputCollector)
	{
		return GitRemoteConnectivityService.testRemote(contextDir, remoteUrl, outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	public void disable(OutputCollector outputCollector)
	{
		outputCollector.append("-----------------------------------------\n");

		Settings settings = Database.getInstance().getSettings();
		settings.set(Settings.GIT_BACKEND_ENABLED, "false");
		settings.set(Settings.GIT_AUTO_PUSH, "false");
		settings.set(Settings.GIT_REMOTE_REPO, null);

		Database.getInstance().saveSettings();

		GitStatusService.invalidateCache();
		outputCollector.append(StringUtils.getUiString("settings.git.disable.complete"));
		outputCollector.append("\n");
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Commits local changes after a successful save. Does not push unless auto-push is on.
	 */
	public void commitLocalAfterSave(File localRepo, OutputCollector outputCollector)
	{
		withRepoLock(localRepo, () ->
		{
			try
			{
				outputCollector.append(GitCommandSessionLog.formatLogTimestamp());
				outputCollector.append(" --- Save All (git) ---\n");
				GitCommandExecutor.verifyGitAvailable();

				if (!GitRepositoryInspector.isGitRepository(localRepo))
				{
					outputCollector.append(StringUtils.getUiString("settings.git.not.a.repo"));
					outputCollector.append("\n");
					return;
				}

				commitLocal(localRepo, outputCollector);

				Settings settings = Database.getInstance().getSettings();
				if (Boolean.parseBoolean(settings.get(Settings.GIT_AUTO_PUSH)))
				{
					GitRemoteSyncService.pushSafely(localRepo, outputCollector, false);
				}

				GitStatusService.invalidateCache();
			}
			catch (IOException | InterruptedException e)
			{
				if (e instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
				throw new BrewdayException(e);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Commits then syncs with remote (fetch, pull --ff-only, push).
	 */
	public void syncWithRemote(File localRepo, OutputCollector outputCollector)
	{
		withRepoLock(localRepo, () ->
		{
			try
			{
				GitCommandExecutor.verifyGitAvailable();
				commitLocal(localRepo, outputCollector);
				GitRemoteSyncService.syncWithRemote(localRepo, outputCollector);
				GitStatusService.invalidateCache();
			}
			catch (IOException | InterruptedException e)
			{
				if (e instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
				throw new BrewdayException(e);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	public GitStatusSnapshot getStatus(File localRepo, boolean gitEnabled)
	{
		return getStatus(localRepo, gitEnabled, null);
	}

	/*-------------------------------------------------------------------------*/
	public GitStatusSnapshot getStatus(
		File localRepo,
		boolean gitEnabled,
		OutputCollector outputCollector)
	{
		return GitStatusService.refresh(localRepo, gitEnabled, outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Configures {@code origin} remote URL in git only (not Brewday settings).
	 */
	public void configureOrigin(File localRepo, String remoteUrl, OutputCollector outputCollector)
	{
		withRepoLock(localRepo, () ->
		{
			try
			{
				GitCommandExecutor.verifyGitAvailable();
				GitRemoteSyncService.configureOrigin(localRepo, remoteUrl, outputCollector);
				GitStatusService.invalidateCache();
			}
			catch (IOException | InterruptedException e)
			{
				if (e instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
				throw new BrewdayException(e);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Pushes to origin only (no fetch, pull, or commit).
	 */
	public void pushToRemoteSafely(File localRepo, OutputCollector outputCollector)
	{
		withRepoLock(localRepo, () ->
		{
			try
			{
				GitCommandExecutor.verifyGitAvailable();
				GitRemoteSyncService.pushSafely(localRepo, outputCollector, false);
				GitStatusService.invalidateCache();
			}
			catch (IOException e)
			{
				throw new BrewdayException(e);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	private static void commitLocal(File localRepo, OutputCollector outputCollector)
		throws IOException, InterruptedException
	{
		if (GitRepositoryInspector.isOperationInProgress(localRepo))
		{
			throw new BrewdayException(StringUtils.getUiString("settings.git.operation.in.progress"));
		}

		GitLocalCommitService.commitLocal(localRepo, outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	private static void withRepoLock(File localRepo, Runnable action)
	{
		String key = localRepo.getAbsolutePath();
		ReentrantLock lock = REPO_LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
		lock.lock();
		try
		{
			action.run();
		}
		finally
		{
			lock.unlock();
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Interface to collect cmd output
	 */
	public interface OutputCollector
	{
		void append(String s);
	}

	/*-------------------------------------------------------------------------*/

	public static void main(String[] args) throws Exception
	{
		if (args.length < 1)
		{
			System.err.println("Usage: GitBackend <localRepoDir> [remoteUrl]");
			System.exit(1);
		}

		File repo = new File(args[0]);
		String remote = args.length > 1 ? args[1] : null;
		GitBackend b = new GitBackend();
		b.enable(repo, remote, System.out::print);
		b.commitLocalAfterSave(repo, System.out::print);
	}
}
