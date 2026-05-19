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
import mclachlan.brewday.BrewdayException;

/**
 * Read-only inspection of a git working tree.
 */
final class GitRepositoryInspector
{
	private static final String[] EXPECTED_DB_FILES = {
		"settings.json",
		"recipes.json"
	};

	private GitRepositoryInspector()
	{
	}

	/*-------------------------------------------------------------------------*/
	static boolean isGitRepository(File localRepo)
	{
		return new File(localRepo, ".git").exists();
	}

	/*-------------------------------------------------------------------------*/
	static boolean isInsideWorkTree(File localRepo)
	{
		return isInsideWorkTree(localRepo, null);
	}

	/*-------------------------------------------------------------------------*/
	static boolean isInsideWorkTree(File localRepo, GitBackend.OutputCollector outputCollector)
	{
		try
		{
			String result = GitCommandExecutor.capture(
				localRepo, outputCollector, "rev-parse", "--is-inside-work-tree");
			return "true".equalsIgnoreCase(result);
		}
		catch (IOException | InterruptedException e)
		{
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			return false;
		}
	}

	/*-------------------------------------------------------------------------*/
	static String currentBranch(File localRepo) throws IOException, InterruptedException
	{
		return currentBranch(localRepo, null);
	}

	/*-------------------------------------------------------------------------*/
	static String currentBranch(File localRepo, GitBackend.OutputCollector outputCollector)
		throws IOException, InterruptedException
	{
		return GitCommandExecutor.capture(localRepo, outputCollector, "branch", "--show-current");
	}

	/*-------------------------------------------------------------------------*/
	static boolean isDetachedHead(File localRepo)
	{
		return isDetachedHead(localRepo, null);
	}

	/*-------------------------------------------------------------------------*/
	static boolean isDetachedHead(File localRepo, GitBackend.OutputCollector outputCollector)
	{
		try
		{
			String branch = currentBranch(localRepo, outputCollector);
			return branch.isEmpty();
		}
		catch (IOException | InterruptedException e)
		{
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			return false;
		}
	}

	/*-------------------------------------------------------------------------*/
	static boolean hasOrigin(File localRepo)
	{
		return hasOrigin(localRepo, null);
	}

	/*-------------------------------------------------------------------------*/
	static boolean hasOrigin(File localRepo, GitBackend.OutputCollector outputCollector)
	{
		try
		{
			int code = GitCommandExecutor.runAllowNonZeroStatus(
				localRepo, outputCollector, "remote", "get-url", "origin");
			return code == 0;
		}
		catch (IOException | InterruptedException e)
		{
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			return false;
		}
	}

	/*-------------------------------------------------------------------------*/
	static String originUrl(File localRepo)
	{
		return originUrl(localRepo, null);
	}

	/*-------------------------------------------------------------------------*/
	static String originUrl(File localRepo, GitBackend.OutputCollector outputCollector)
	{
		try
		{
			return GitCommandExecutor.capture(localRepo, outputCollector, "remote", "get-url", "origin");
		}
		catch (BrewdayException | IOException | InterruptedException e)
		{
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			return null;
		}
	}

	/*-------------------------------------------------------------------------*/
	static boolean hasCommitterIdentity(File localRepo)
	{
		return hasCommitterIdentity(localRepo, null);
	}

	/*-------------------------------------------------------------------------*/
	static boolean hasCommitterIdentity(File localRepo, GitBackend.OutputCollector outputCollector)
	{
		try
		{
			String ident = GitCommandExecutor.capture(localRepo, outputCollector, "var", "GIT_COMMITTER_IDENT");
			return ident != null && !ident.isBlank();
		}
		catch (BrewdayException | IOException | InterruptedException e)
		{
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			try
			{
				String name = GitCommandExecutor.capture(
					localRepo, outputCollector, "config", "--get", "user.name");
				String email = GitCommandExecutor.capture(
					localRepo, outputCollector, "config", "--get", "user.email");
				return name != null && !name.isBlank() && email != null && !email.isBlank();
			}
			catch (IOException | InterruptedException ex)
			{
				if (ex instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
				return false;
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	static boolean hasPorcelainChanges(File localRepo)
	{
		return hasPorcelainChanges(localRepo, null);
	}

	/*-------------------------------------------------------------------------*/
	static boolean hasPorcelainChanges(File localRepo, GitBackend.OutputCollector outputCollector)
	{
		try
		{
			String status = GitCommandExecutor.capture(localRepo, outputCollector, "status", "--porcelain");
			return status != null && !status.isBlank();
		}
		catch (IOException | InterruptedException e)
		{
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			return false;
		}
	}

	/*-------------------------------------------------------------------------*/
	static boolean isOperationInProgress(File localRepo)
	{
		File gitDir = new File(localRepo, ".git");
		return new File(gitDir, "MERGE_HEAD").isFile()
			|| new File(gitDir, "rebase-merge").isDirectory()
			|| new File(gitDir, "rebase-apply").isDirectory()
			|| new File(gitDir, "CHERRY_PICK_HEAD").isFile();
	}

	/*-------------------------------------------------------------------------*/
	static boolean looksLikeBrewdayDatabase(File localRepo)
	{
		for (String name : EXPECTED_DB_FILES)
		{
			if (!new File(localRepo, name).isFile())
			{
				return false;
			}
		}
		return true;
	}

	/*-------------------------------------------------------------------------*/
	static int[] aheadBehind(File localRepo)
	{
		return aheadBehind(localRepo, null);
	}

	/*-------------------------------------------------------------------------*/
	static int[] aheadBehind(File localRepo, GitBackend.OutputCollector outputCollector)
	{
		try
		{
			String counts = GitCommandExecutor.capture(
				localRepo, outputCollector, "rev-list", "--left-right", "--count", "HEAD...@{u}");
			String[] parts = counts.trim().split("\\s+");
			if (parts.length >= 2)
			{
				return new int[] {
					Integer.parseInt(parts[0]),
					Integer.parseInt(parts[1])
				};
			}
		}
		catch (BrewdayException | IOException | InterruptedException | NumberFormatException e)
		{
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
		}
		return new int[] { 0, 0 };
	}

	/*-------------------------------------------------------------------------*/
	static boolean remoteHasCommits(File localRepo, String remoteUrl)
		throws IOException, InterruptedException
	{
		return remoteHasCommits(localRepo, remoteUrl, null);
	}

	/*-------------------------------------------------------------------------*/
	static boolean remoteHasCommits(
		File localRepo,
		String remoteUrl,
		GitBackend.OutputCollector outputCollector) throws IOException, InterruptedException
	{
		try
		{
			String refs = GitCommandExecutor.capture(localRepo, outputCollector, "ls-remote", remoteUrl, "HEAD");
			return refs != null && !refs.isBlank();
		}
		catch (BrewdayException e)
		{
			return false;
		}
	}

	/*-------------------------------------------------------------------------*/
	static boolean localHasCommits(File localRepo)
	{
		return localHasCommits(localRepo, null);
	}

	/*-------------------------------------------------------------------------*/
	static boolean localHasCommits(File localRepo, GitBackend.OutputCollector outputCollector)
	{
		try
		{
			GitCommandExecutor.capture(localRepo, outputCollector, "rev-parse", "HEAD");
			return true;
		}
		catch (BrewdayException | IOException | InterruptedException e)
		{
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			return false;
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * True if a child directory (one level deep) contains its own {@code .git} directory.
	 */
	public static boolean hasNestedGitRepositories(File localRepo)
	{
		File[] children = localRepo.listFiles(File::isDirectory);
		if (children == null)
		{
			return false;
		}
		for (File child : children)
		{
			if (new File(child, ".git").exists())
			{
				return true;
			}
		}
		return false;
	}
}
