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
import mclachlan.brewday.util.StringUtils;

/**
 * Validates a directory before Workflow 2 adoption.
 */
public final class GitRepositoryAdoptionValidator
{
	public enum Failure
	{
		NotWorkTree,
		NotBrewdayDatabase,
		DetachedHead,
		OperationInProgress,
		NestedRepositories
	}

	public static final class Result
	{
		private final boolean valid;
		private final Failure failure;
		private final boolean dirty;
		private final String message;

		private Result(boolean valid, Failure failure, boolean dirty, String message)
		{
			this.valid = valid;
			this.failure = failure;
			this.dirty = dirty;
			this.message = message;
		}

		public static Result ok(boolean dirty)
		{
			return new Result(true, null, dirty, null);
		}

		public static Result fail(Failure failure)
		{
			String key = switch (failure)
			{
				case NotWorkTree -> "settings.git.not.work.tree";
				case NotBrewdayDatabase -> "settings.git.adopt.not.brewday";
				case DetachedHead -> "settings.git.detached.head";
				case OperationInProgress -> "settings.git.operation.in.progress";
				case NestedRepositories -> "settings.git.nested.repo";
			};
			return new Result(false, failure, false, StringUtils.getUiString(key));
		}

		public boolean isValid()
		{
			return valid;
		}

		public Failure getFailure()
		{
			return failure;
		}

		public boolean isDirty()
		{
			return dirty;
		}

		public String getMessage()
		{
			return message;
		}
	}

	private GitRepositoryAdoptionValidator()
	{
	}

	/*-------------------------------------------------------------------------*/
	public static Result validate(File repoDir) throws IOException
	{
		GitCommandExecutor.verifyGitAvailable();

		if (!GitRepositoryInspector.isGitRepository(repoDir)
			|| !GitRepositoryInspector.isInsideWorkTree(repoDir))
		{
			return Result.fail(Failure.NotWorkTree);
		}

		if (!GitRepositoryInspector.looksLikeBrewdayDatabase(repoDir))
		{
			return Result.fail(Failure.NotBrewdayDatabase);
		}

		if (GitRepositoryInspector.isDetachedHead(repoDir))
		{
			return Result.fail(Failure.DetachedHead);
		}

		if (GitRepositoryInspector.isOperationInProgress(repoDir))
		{
			return Result.fail(Failure.OperationInProgress);
		}

		if (GitRepositoryInspector.hasNestedGitRepositories(repoDir))
		{
			return Result.fail(Failure.NestedRepositories);
		}

		boolean dirty = GitRepositoryInspector.hasPorcelainChanges(repoDir);
		return Result.ok(dirty);
	}
}
