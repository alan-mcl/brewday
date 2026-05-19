/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.backends.git.GitBackend;
import mclachlan.brewday.db.backends.git.GitCommandExecutor;
import mclachlan.brewday.db.backends.git.GitCommandSessionLog;
import mclachlan.brewday.util.AppContentRoot;

/**
 * Manual harness for {@link GitBackend}. Skips all checks if {@code git} is not on PATH.
 * <p>
 * From repo root after {@code ant compile}:
 * {@code java -cp "build/classes:lib/gson/gson-2.8.6.jar:..." ...} (see {@code project.class.path} in build.xml)
 */
public class GitBackendHarness
{
	private static int failures;

	public static void main(String[] args) throws Exception
	{
		AppContentRoot.install();
		Brewday.getInstance();

		if (!isGitOnPath())
		{
			System.out.println("GitBackendHarness: skipped (git not available on PATH)");
			return;
		}

		testAllowlistRejectsReset();
		testAllowlistRejectsForcePush();
		testAllowlistAllowsClone();
		testNestedGitRepositoryDetection();

		GitCommandSessionLog.clear();

		Path tempRoot = Files.createTempDirectory("brewday_git_harness");
		try
		{
			Path dbPath = tempRoot.resolve("db");
			copyDbTree(Paths.get("test_data/test_db"), dbPath);

			installDatabaseSingleton(dbPath);
			Database db = Database.getInstance();
			db.loadAll();
			db.getSettings().set(Settings.GIT_BACKEND_ENABLED, "false");
			db.getSettings().set(Settings.GIT_REMOTE_REPO, null);
			db.getSettings().set(Settings.GIT_AUTO_PUSH, "false");

			File repoDir = db.getLocalStorageDirectory();
			StringBuilder log = new StringBuilder();
			GitBackend.OutputCollector collector = log::append;

			GitBackend backend = new GitBackend();
			backend.setupNewGitBackup(repoDir, null, collector);
			String sessionLog = GitCommandSessionLog.snapshot();
			check("session log records git init",
				sessionLog.contains("git init"));
			check("session log records main branch init",
				sessionLog.contains("-b") && sessionLog.contains("main"));
			check(".git directory exists", new File(repoDir, ".git").isDirectory());
			check("git backend enabled in settings",
				"true".equalsIgnoreCase(db.getSettings().get(Settings.GIT_BACKEND_ENABLED)));
			check("auto-push defaults off",
				!"true".equalsIgnoreCase(db.getSettings().get(Settings.GIT_AUTO_PUSH)));
			check("remote URL not stored in settings",
				db.getSettings().get(Settings.GIT_REMOTE_REPO) == null
					|| db.getSettings().get(Settings.GIT_REMOTE_REPO).isBlank());

			String gitignore = Files.readString(repoDir.toPath().resolve(".gitignore"), StandardCharsets.UTF_8);
			check("gitignore excludes backup", gitignore.contains("/backup"));
			check("gitignore excludes sensitive", gitignore.contains("/sensitive"));

			backend.commitLocalAfterSave(repoDir, collector);
			check("empty commit does not fail", true);

			Path marker = repoDir.toPath().resolve("harness_marker.txt");
			Files.writeString(marker, "v2\n", StandardCharsets.UTF_8);
			backend.commitLocalAfterSave(repoDir, collector);
			check("session log records commit after save",
				GitCommandSessionLog.snapshot().contains("git commit"));
			check("save commit message",
				gitLogOneline(repoDir).contains("Brewday save"));
			check("initial commit message present in history",
				gitLogContains(repoDir, "Initial Brewday repository"));

			db.getSettings().set(Settings.GIT_AUTO_PUSH, "false");
			backend.commitLocalAfterSave(repoDir, collector);
			check("commit without auto-push succeeds", true);

			backend.disable(collector);
			check("git backend disabled in settings",
				!"true".equalsIgnoreCase(db.getSettings().get(Settings.GIT_BACKEND_ENABLED)));
		}
		finally
		{
			deleteRecursive(tempRoot);
		}

		if (failures > 0)
		{
			System.err.println(failures + " test(s) failed");
			System.exit(1);
		}
		System.out.println("GitBackendHarness: all checks passed");
	}

	private static void testAllowlistRejectsReset()
	{
		try
		{
			GitCommandExecutor.validateGitArgs("reset", "--hard", "HEAD");
			check("allowlist rejects reset", false);
		}
		catch (BrewdayException e)
		{
			check("allowlist rejects reset", true);
		}
	}

	private static void testAllowlistRejectsForcePush()
	{
		try
		{
			GitCommandExecutor.validateGitArgs("push", "--force", "origin", "HEAD");
			check("allowlist rejects force push", false);
		}
		catch (BrewdayException e)
		{
			check("allowlist rejects force push", true);
		}
	}

	private static void testAllowlistAllowsClone()
	{
		try
		{
			GitCommandExecutor.validateGitArgs("clone", "git@example.com:u/r.git", "dest");
			check("allowlist allows clone", true);
		}
		catch (BrewdayException e)
		{
			check("allowlist allows clone", false);
		}
		try
		{
			GitCommandExecutor.validateGitArgs("clone", "--recursive", "u", "d");
			check("allowlist rejects clone --recursive", false);
		}
		catch (BrewdayException e)
		{
			check("allowlist rejects clone --recursive", true);
		}
	}

	private static void testNestedGitRepositoryDetection() throws Exception
	{
		Path root = Files.createTempDirectory("brewday_nested_git");
		try
		{
			Files.createDirectories(root.resolve("subdir/.git"));
			check("nested .git detected", hasNestedGitDir(root.toFile()));
		}
		finally
		{
			deleteRecursive(root);
		}
	}

	private static boolean isGitOnPath()
	{
		try
		{
			Process p = new ProcessBuilder("git", "--version").start();
			boolean finished = p.waitFor(1, TimeUnit.MINUTES);
			return finished && p.exitValue() == 0;
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

	private static String gitLogOneline(File repoDir) throws IOException, InterruptedException
	{
		return gitLog(repoDir, "-1", "--oneline");
	}

	private static boolean gitLogContains(File repoDir, String substring)
		throws IOException, InterruptedException
	{
		return gitLog(repoDir, "--oneline").contains(substring);
	}

	private static String gitLog(File repoDir, String... extraArgs)
		throws IOException, InterruptedException
	{
		List<String> cmd = new ArrayList<>();
		cmd.add("git");
		cmd.add("log");
		cmd.addAll(java.util.Arrays.asList(extraArgs));
		Process p = new ProcessBuilder(cmd)
			.directory(repoDir)
			.start();
		String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		if (!p.waitFor(1, TimeUnit.MINUTES) || p.exitValue() != 0)
		{
			throw new IOException("git log failed");
		}
		return out;
	}

	private static boolean hasNestedGitDir(File localRepo)
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

	private static void installDatabaseSingleton(Path dbPath) throws Exception
	{
		Database db = new Database(dbPath.toAbsolutePath().toString());
		Field instanceField = Database.class.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, db);
	}

	private static void copyDbTree(Path src, Path dest) throws IOException
	{
		Files.walk(src)
			.sorted(Comparator.reverseOrder())
			.forEach(path ->
			{
				try
				{
					Path rel = src.relativize(path);
					String relStr = rel.toString();
					if (relStr.contains(".git") || relStr.startsWith("sensitive"))
					{
						return;
					}
					Path target = dest.resolve(rel);
					if (Files.isDirectory(path))
					{
						Files.createDirectories(target);
					}
					else
					{
						Files.createDirectories(target.getParent());
						Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
					}
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			});
	}

	private static void deleteRecursive(Path root) throws IOException
	{
		if (!Files.exists(root))
		{
			return;
		}
		Files.walk(root)
			.sorted(Comparator.reverseOrder())
			.forEach(path ->
			{
				try
				{
					Files.deleteIfExists(path);
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			});
	}

	private static void check(String label, boolean ok)
	{
		if (!ok)
		{
			failures++;
			System.err.println("FAIL: " + label);
		}
		else
		{
			System.out.println("OK: " + label);
		}
	}
}
