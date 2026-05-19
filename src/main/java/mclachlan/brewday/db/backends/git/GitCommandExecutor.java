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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import mclachlan.brewday.BrewdayException;

/**
 * Runs allowlisted {@code git} commands via {@link ProcessBuilder}.
 */
public final class GitCommandExecutor
{
	private static final long DEFAULT_TIMEOUT_MINUTES = 30;
	private static final long STATUS_TIMEOUT_MINUTES = 5;

	private static volatile String gitExecutable;

	private GitCommandExecutor()
	{
	}

	/*-------------------------------------------------------------------------*/
	public static GitBackend.OutputCollector combinedCollector(GitBackend.OutputCollector ui)
	{
		GitBackend.OutputCollector session = GitCommandSessionLog.asCollector();
		if (ui == null)
		{
			return session;
		}
		return s ->
		{
			session.append(s);
			ui.append(s);
		};
	}

	/*-------------------------------------------------------------------------*/
	static void verifyGitAvailable() throws IOException
	{
		resolveGitExecutable();
	}

	/*-------------------------------------------------------------------------*/
	static String resolveGitExecutable() throws IOException
	{
		String cached = gitExecutable;
		if (cached != null)
		{
			return cached;
		}

		synchronized (GitCommandExecutor.class)
		{
			cached = gitExecutable;
			if (cached != null)
			{
				return cached;
			}

			for (String candidate : gitCandidates())
			{
				if (isRunnableGit(candidate))
				{
					gitExecutable = candidate;
					return candidate;
				}
			}

			throw new IOException(
				"git executable not found on PATH (install Git and ensure it is available to the Brewday process)");
		}
	}

	/*-------------------------------------------------------------------------*/
	public static void validateGitArgs(String... gitArgs)
	{
		if (gitArgs.length == 0)
		{
			throw new BrewdayException("empty git command");
		}

		String command = gitArgs[0];
		switch (command)
		{
			case "init":
			case "status":
			case "add":
			case "commit":
			case "fetch":
			case "branch":
			case "rev-parse":
			case "diff":
			case "symbolic-ref":
			case "var":
			case "rev-list":
			case "ls-remote":
			case "merge-base":
				validateNoForbiddenFlags(gitArgs);
				return;

			case "clone":
				validateClone(gitArgs);
				return;

			case "push":
				validatePush(gitArgs);
				return;

			case "pull":
				validatePull(gitArgs);
				return;

			case "remote":
				validateRemote(gitArgs);
				return;

			case "config":
				validateConfigReadOnly(gitArgs);
				return;

			default:
				throw new BrewdayException("git command not allowed: " + command);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void validateClone(String[] gitArgs)
	{
		if (gitArgs.length != 3)
		{
			throw new BrewdayException("git clone requires remote URL and destination directory name");
		}
		for (String arg : gitArgs)
		{
			if ("--recursive".equals(arg)
				|| "--recurse-submodules".equals(arg)
				|| arg.startsWith("--depth"))
			{
				throw new BrewdayException("git clone option not allowed: " + arg);
			}
		}
		validateNoForbiddenFlags(gitArgs);
	}

	/*-------------------------------------------------------------------------*/
	private static void validatePush(String[] gitArgs)
	{
		for (String arg : gitArgs)
		{
			if ("--force".equals(arg) || "-f".equals(arg))
			{
				throw new BrewdayException("git push --force is not allowed");
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void validatePull(String[] gitArgs)
	{
		boolean ffOnly = false;
		for (String arg : gitArgs)
		{
			if ("--ff-only".equals(arg))
			{
				ffOnly = true;
			}
		}
		if (!ffOnly)
		{
			throw new BrewdayException("git pull requires --ff-only");
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void validateRemote(String[] gitArgs)
	{
		if (gitArgs.length < 2)
		{
			throw new BrewdayException("git remote requires a subcommand");
		}
		String sub = gitArgs[1];
		switch (sub)
		{
			case "get-url":
			case "add":
			case "set-url":
			case "-v":
				return;
			default:
				throw new BrewdayException("git remote subcommand not allowed: " + sub);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void validateConfigReadOnly(String[] gitArgs)
	{
		if (gitArgs.length < 2)
		{
			throw new BrewdayException("git config requires a subcommand");
		}
		String sub = gitArgs[1];
		if (!"--get".equals(sub) && !"--get-regexp".equals(sub))
		{
			throw new BrewdayException("git config writes are not allowed");
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void validateNoForbiddenFlags(String[] gitArgs)
	{
		Set<String> forbidden = Set.of(
			"reset", "clean", "checkout", "switch", "merge", "rebase",
			"cherry-pick", "stash", "apply", "revert", "restore", "worktree");
		for (String arg : gitArgs)
		{
			if (forbidden.contains(arg))
			{
				throw new BrewdayException("git command not allowed: " + arg);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private static List<String> gitCandidates()
	{
		List<String> candidates = new ArrayList<>();
		candidates.add("git");

		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("win"))
		{
			candidates.add("C:\\Program Files\\Git\\cmd\\git.exe");
			candidates.add("C:\\Program Files (x86)\\Git\\cmd\\git.exe");
		}

		return candidates;
	}

	/*-------------------------------------------------------------------------*/
	private static boolean isRunnableGit(String executable)
	{
		try
		{
			ProcessBuilder pb = new ProcessBuilder(executable, "--version");
			pb.redirectErrorStream(true);
			Process p = pb.start();
			String output = readAll(p.getInputStream());
			boolean finished = p.waitFor(1, TimeUnit.MINUTES);
			if (!finished)
			{
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0 && output.toLowerCase(Locale.ROOT).contains("git version");
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
	private static List<String> buildCommand(String... gitArgs) throws IOException
	{
		validateGitArgs(gitArgs);
		String git = resolveGitExecutable();
		List<String> command = new ArrayList<>();
		command.add(git);
		command.addAll(Arrays.asList(gitArgs));
		return command;
	}

	/*-------------------------------------------------------------------------*/
	private static void logCommandLine(String commandLine, GitBackend.OutputCollector collector)
	{
		collector.append(GitCommandSessionLog.formatLogTimestamp());
		collector.append(" ");
		collector.append(commandLine);
		collector.append("\n");
	}

	/*-------------------------------------------------------------------------*/
	static int run(
		File workingDir,
		GitBackend.OutputCollector outputCollector,
		String... gitArgs) throws IOException, InterruptedException
	{
		return run(workingDir, outputCollector, DEFAULT_TIMEOUT_MINUTES, gitArgs);
	}

	/*-------------------------------------------------------------------------*/
	static int run(
		File workingDir,
		GitBackend.OutputCollector outputCollector,
		long timeoutMinutes,
		String... gitArgs) throws IOException, InterruptedException
	{
		List<String> command = buildCommand(gitArgs);
		String commandLine = String.join(" ", command);
		GitBackend.OutputCollector collector = combinedCollector(outputCollector);
		logCommandLine(commandLine, collector);

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(workingDir);
		pb.redirectErrorStream(false);
		configureNonInteractiveGit(pb);

		Process process = pb.start();

		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();

		Thread outThread = streamCopier(process.getInputStream(), stdout, collector);
		Thread errThread = streamCopier(process.getErrorStream(), stderr, collector);

		boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
		if (!finished)
		{
			process.destroyForcibly();
			outThread.join(TimeUnit.MINUTES.toMillis(1));
			errThread.join(TimeUnit.MINUTES.toMillis(1));
			throw new IOException("git command timed out after " + timeoutMinutes + " minutes: " + commandLine);
		}

		outThread.join(TimeUnit.MINUTES.toMillis(5));
		errThread.join(TimeUnit.MINUTES.toMillis(5));

		int exitCode = process.exitValue();
		if (exitCode != 0)
		{
			logExitCode(exitCode, collector);
			throw gitCommandFailed(commandLine, exitCode, stdout, stderr);
		}

		return exitCode;
	}

	/*-------------------------------------------------------------------------*/
	static int runAllowNonZero(
		File workingDir,
		GitBackend.OutputCollector outputCollector,
		String... gitArgs) throws IOException, InterruptedException
	{
		List<String> command = buildCommand(gitArgs);
		String commandLine = String.join(" ", command);
		GitBackend.OutputCollector collector = combinedCollector(outputCollector);
		logCommandLine(commandLine, collector);

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(workingDir);
		configureNonInteractiveGit(pb);
		Process process = pb.start();

		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();

		Thread outThread = streamCopier(process.getInputStream(), stdout, collector);
		Thread errThread = streamCopier(process.getErrorStream(), stderr, collector);

		boolean finished = process.waitFor(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
		if (!finished)
		{
			process.destroyForcibly();
			throw new IOException("git command timed out: " + commandLine);
		}

		outThread.join(TimeUnit.MINUTES.toMillis(5));
		errThread.join(TimeUnit.MINUTES.toMillis(5));

		int exitCode = process.exitValue();
		if (exitCode != 0)
		{
			logExitCode(exitCode, collector);
		}

		return exitCode;
	}

	/*-------------------------------------------------------------------------*/
	static int runAllowNonZeroStatus(
		File workingDir,
		GitBackend.OutputCollector outputCollector,
		String... gitArgs) throws IOException, InterruptedException
	{
		List<String> command = buildCommand(gitArgs);
		String commandLine = String.join(" ", command);
		GitBackend.OutputCollector collector = combinedCollector(outputCollector);
		logCommandLine(commandLine, collector);

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(workingDir);
		configureNonInteractiveGit(pb);
		Process process = pb.start();

		drainToCollector(process.getInputStream(), collector);
		drainToCollector(process.getErrorStream(), collector);

		boolean finished = process.waitFor(STATUS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
		if (!finished)
		{
			process.destroyForcibly();
			throw new IOException("git command timed out: " + commandLine);
		}

		int exitCode = process.exitValue();
		if (exitCode != 0)
		{
			logExitCode(exitCode, collector);
		}

		return exitCode;
	}

	/*-------------------------------------------------------------------------*/
	static String capture(File workingDir, String... gitArgs) throws IOException, InterruptedException
	{
		return capture(workingDir, null, gitArgs);
	}

	/*-------------------------------------------------------------------------*/
	static String capture(
		File workingDir,
		GitBackend.OutputCollector outputCollector,
		String... gitArgs) throws IOException, InterruptedException
	{
		List<String> command = buildCommand(gitArgs);
		String commandLine = String.join(" ", command);
		GitBackend.OutputCollector collector = combinedCollector(outputCollector);
		logCommandLine(commandLine, collector);

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(workingDir);
		configureNonInteractiveGit(pb);
		Process process = pb.start();

		String stdout = readAll(process.getInputStream());
		String stderr = readAll(process.getErrorStream());
		appendCapturedOutput(stdout, collector);
		appendCapturedOutput(stderr, collector);

		boolean finished = process.waitFor(STATUS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
		if (!finished)
		{
			process.destroyForcibly();
			throw new IOException("git command timed out: " + commandLine);
		}

		int exitCode = process.exitValue();
		if (exitCode != 0)
		{
			logExitCode(exitCode, collector);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write(stdout.getBytes(StandardCharsets.UTF_8));
			ByteArrayOutputStream err = new ByteArrayOutputStream();
			err.write(stderr.getBytes(StandardCharsets.UTF_8));
			throw gitCommandFailed(commandLine, exitCode, out, err);
		}

		return stdout.trim();
	}

	/*-------------------------------------------------------------------------*/
	private static void appendCapturedOutput(String text, GitBackend.OutputCollector collector)
	{
		if (text == null || text.isEmpty())
		{
			return;
		}
		for (String line : text.split("\n"))
		{
			collector.append(line);
			collector.append("\n");
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void logExitCode(int exitCode, GitBackend.OutputCollector collector)
	{
		collector.append(GitCommandSessionLog.formatLogTimestamp());
		collector.append(" (exit ");
		collector.append(String.valueOf(exitCode));
		collector.append(")\n");
	}

	/*-------------------------------------------------------------------------*/
	private static void drainToCollector(InputStream stream, GitBackend.OutputCollector outputCollector)
		throws IOException
	{
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(stream, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				outputCollector.append(line);
				outputCollector.append("\n");
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private static Thread streamCopier(
		InputStream stream,
		ByteArrayOutputStream capture,
		GitBackend.OutputCollector outputCollector)
	{
		Thread t = new Thread(() ->
		{
			try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream, StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					capture.write(line.getBytes(StandardCharsets.UTF_8));
					capture.write('\n');
					outputCollector.append(line);
					outputCollector.append("\n");
				}
			}
			catch (IOException e)
			{
				throw new BrewdayException(e);
			}
		}, "git-stream-reader");
		t.setDaemon(true);
		t.start();
		return t;
	}

	/*-------------------------------------------------------------------------*/
	private static String readAll(InputStream stream) throws IOException
	{
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(stream, StandardCharsets.UTF_8)))
		{
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (sb.length() > 0)
				{
					sb.append('\n');
				}
				sb.append(line);
			}
			return sb.toString();
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void configureNonInteractiveGit(ProcessBuilder pb)
	{
		Map<String, String> env = pb.environment();
		env.put("GIT_TERMINAL_PROMPT", "0");
		env.put("GCM_INTERACTIVE", "Never");
	}

	/*-------------------------------------------------------------------------*/
	private static BrewdayException gitCommandFailed(
		String commandLine,
		int exitCode,
		ByteArrayOutputStream stdout,
		ByteArrayOutputStream stderr)
	{
		String out = stdout.toString(StandardCharsets.UTF_8).trim();
		String err = stderr.toString(StandardCharsets.UTF_8).trim();
		StringBuilder msg = new StringBuilder();
		msg.append("git command failed (exit ").append(exitCode).append("): ").append(commandLine);
		if (!out.isEmpty())
		{
			msg.append("\n").append(out);
		}
		if (!err.isEmpty())
		{
			msg.append("\n").append(err);
		}
		return new BrewdayException(msg.toString());
	}
}
