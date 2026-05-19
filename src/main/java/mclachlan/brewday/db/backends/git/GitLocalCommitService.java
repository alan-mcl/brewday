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
 * Stages and commits local database changes.
 */
final class GitLocalCommitService
{
	public static final String SAVE_COMMIT_MESSAGE = "Brewday save";
	public static final String INITIAL_COMMIT_MESSAGE = "Initial Brewday repository";

	private GitLocalCommitService()
	{
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @return true if a new commit was created
	 */
	static boolean commitLocal(
		File localRepo,
		GitBackend.OutputCollector outputCollector) throws IOException, InterruptedException
	{
		return commitWithMessage(localRepo, SAVE_COMMIT_MESSAGE, outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @return true if a new commit was created
	 */
	static boolean commitInitial(
		File localRepo,
		GitBackend.OutputCollector outputCollector) throws IOException, InterruptedException
	{
		return commitWithMessage(localRepo, INITIAL_COMMIT_MESSAGE, outputCollector);
	}

	/*-------------------------------------------------------------------------*/
	private static boolean commitWithMessage(
		File localRepo,
		String message,
		GitBackend.OutputCollector outputCollector) throws IOException, InterruptedException
	{
		if (!GitRepositoryInspector.hasCommitterIdentity(localRepo))
		{
			String msg = StringUtils.getUiString("settings.git.identity.missing");
			Brewday.getInstance().getLog().log(Log.LOUD, msg);
			if (outputCollector != null)
			{
				outputCollector.append(msg);
				outputCollector.append("\n");
			}
			throw new BrewdayException(msg);
		}

		GitCommandExecutor.run(localRepo, outputCollector, "add", "-A");

		int stagedChanges = GitCommandExecutor.runAllowNonZero(
			localRepo, outputCollector, "diff", "--cached", "--quiet");
		if (stagedChanges == 0)
		{
			if (outputCollector != null)
			{
				outputCollector.append("(no changes to commit)\n");
			}
			return false;
		}
		if (stagedChanges != 1)
		{
			throw new BrewdayException(
				"git diff --cached --quiet failed with exit code " + stagedChanges);
		}

		GitCommandExecutor.run(localRepo, outputCollector, "commit", "-m", message);
		return true;
	}
}
