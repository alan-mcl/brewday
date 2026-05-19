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

/**
 * High-level git repository state for UI and orchestration.
 */
public enum GitRepoStatus
{
	Disabled,
	GitUnavailable,
	NotARepository,
	RepoValid,
	Clean,
	Uncommitted,
	LocalCommitsUnpushed,
	PullRequired,
	Diverged,
	NoUpstream,
	DetachedHead,
	OperationInProgress,
	IdentityMissing,
	RemoteUnreachable,
	PushRejected,
	RepoError
}
