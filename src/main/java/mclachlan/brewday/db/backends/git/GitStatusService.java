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
import mclachlan.brewday.util.StringUtils;

/**
 * Builds {@link GitStatusSnapshot} for UI display with short-lived cache.
 */
public final class GitStatusService
{
	private static final long CACHE_TTL_MS = 8_000;

	private static volatile GitStatusSnapshot cachedSnapshot;
	private static volatile long cachedAtMs;
	private static volatile File cachedRepo;

	private GitStatusService()
	{
	}

	/*-------------------------------------------------------------------------*/
	public static void invalidateCache()
	{
		cachedSnapshot = null;
		cachedRepo = null;
	}

	/*-------------------------------------------------------------------------*/
	public static GitStatusSnapshot refresh(File localRepo, boolean gitEnabled)
	{
		return refresh(localRepo, gitEnabled, null);
	}

	/*-------------------------------------------------------------------------*/
	public static GitStatusSnapshot refresh(
		File localRepo,
		boolean gitEnabled,
		GitBackend.OutputCollector outputCollector)
	{
		if (outputCollector != null)
		{
			invalidateCache();
			return buildSnapshot(localRepo, gitEnabled, outputCollector);
		}

		long now = System.currentTimeMillis();
		if (cachedSnapshot != null
			&& cachedRepo != null
			&& cachedRepo.equals(localRepo)
			&& (now - cachedAtMs) < CACHE_TTL_MS)
		{
			return cachedSnapshot;
		}

		GitStatusSnapshot snapshot = buildSnapshot(localRepo, gitEnabled, null);
		cachedSnapshot = snapshot;
		cachedRepo = localRepo;
		cachedAtMs = now;
		return snapshot;
	}

	/*-------------------------------------------------------------------------*/
	private static GitStatusSnapshot buildSnapshot(
		File localRepo,
		boolean gitEnabled,
		GitBackend.OutputCollector outputCollector)
	{
		if (!gitEnabled)
		{
			return new GitStatusSnapshot(GitRepoStatus.Disabled, null, null, 0, 0, null);
		}

		try
		{
			GitCommandExecutor.verifyGitAvailable();
		}
		catch (IOException e)
		{
			return new GitStatusSnapshot(
				GitRepoStatus.GitUnavailable, null, null, 0, 0, e.getMessage());
		}

		if (!GitRepositoryInspector.isGitRepository(localRepo)
			|| !GitRepositoryInspector.isInsideWorkTree(localRepo, outputCollector))
		{
			return new GitStatusSnapshot(
				GitRepoStatus.NotARepository, null, null, 0, 0, null);
		}

		if (GitRepositoryInspector.isOperationInProgress(localRepo))
		{
			return snapshotWithBranch(
				localRepo, GitRepoStatus.OperationInProgress, outputCollector,
				StringUtils.getUiString("settings.git.status.operation.in.progress"));
		}

		if (!GitRepositoryInspector.hasCommitterIdentity(localRepo, outputCollector))
		{
			return snapshotWithBranch(
				localRepo, GitRepoStatus.IdentityMissing, outputCollector,
				StringUtils.getUiString("settings.git.identity.missing"));
		}

		if (GitRepositoryInspector.isDetachedHead(localRepo, outputCollector))
		{
			return snapshotWithBranch(
				localRepo, GitRepoStatus.DetachedHead, outputCollector,
				StringUtils.getUiString("settings.git.detached.head"));
		}

		String pushError = GitRemoteSyncService.getLastPushError();
		if (pushError != null && !pushError.isBlank())
		{
			return snapshotWithBranch(localRepo, GitRepoStatus.PushRejected, outputCollector, pushError);
		}

		String branch = safeBranch(localRepo, outputCollector);
		String remoteUrl = GitRepositoryInspector.originUrl(localRepo, outputCollector);
		boolean dirty = GitRepositoryInspector.hasPorcelainChanges(localRepo, outputCollector);

		if (!GitRepositoryInspector.hasOrigin(localRepo, outputCollector))
		{
			GitRepoStatus status = dirty ? GitRepoStatus.Uncommitted : GitRepoStatus.NoUpstream;
			return new GitStatusSnapshot(status, branch, null, 0, 0, null);
		}

		int[] ab = GitRepositoryInspector.aheadBehind(localRepo, outputCollector);
		int ahead = ab[0];
		int behind = ab[1];

		if (ahead == 0 && behind == 0 && !dirty)
		{
			return new GitStatusSnapshot(
				GitRepoStatus.Clean, branch, remoteUrl, 0, 0, null);
		}

		if (ahead > 0 && behind > 0)
		{
			return new GitStatusSnapshot(
				GitRepoStatus.Diverged, branch, remoteUrl, ahead, behind,
				StringUtils.getUiString("settings.git.status.diverged"));
		}

		if (behind > 0)
		{
			return new GitStatusSnapshot(
				GitRepoStatus.PullRequired, branch, remoteUrl, ahead, behind, null);
		}

		if (ahead > 0)
		{
			return new GitStatusSnapshot(
				GitRepoStatus.LocalCommitsUnpushed, branch, remoteUrl, ahead, behind, null);
		}

		if (dirty)
		{
			return new GitStatusSnapshot(
				GitRepoStatus.Uncommitted, branch, remoteUrl, 0, 0, null);
		}

		return new GitStatusSnapshot(
			GitRepoStatus.RepoValid, branch, remoteUrl, ahead, behind, null);
	}

	/*-------------------------------------------------------------------------*/
	private static GitStatusSnapshot snapshotWithBranch(
		File localRepo,
		GitRepoStatus status,
		GitBackend.OutputCollector outputCollector,
		String detail)
	{
		return new GitStatusSnapshot(
			status,
			safeBranch(localRepo, outputCollector),
			GitRepositoryInspector.originUrl(localRepo, outputCollector),
			0, 0, detail);
	}

	/*-------------------------------------------------------------------------*/
	private static String safeBranch(File localRepo, GitBackend.OutputCollector outputCollector)
	{
		try
		{
			return GitRepositoryInspector.currentBranch(localRepo, outputCollector);
		}
		catch (IOException | InterruptedException e)
		{
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			return null;
		}
	}

	/*-------------------------------------------------------------------------*/
	public static String statusMessageKey(GitRepoStatus status)
	{
		return "settings.git.status." + status.name();
	}
}
