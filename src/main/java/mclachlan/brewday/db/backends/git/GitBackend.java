/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.db.backends.git;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.util.Log;
import mclachlan.brewday.util.StringUtils;

/**
 *
 */
public class GitBackend
{
	/*-------------------------------------------------------------------------*/

	/**
	 * @param localRepo  The directory to be the local git repo
	 * @param remoteRepo The URL of the remote repo, or null if there is no
	 *                   remote repo
	 */
	public void enable(
		File localRepo,
		String remoteRepo,
		OutputCollector outputCollector)
	{
		// https://superuser.com/questions/1412078/bring-a-local-folder-to-remote-git-repo

		// git init
		// git add .
		// git commit -m "Brewday enabling git backend"
		// git remote add origin <URL>
		// git remote -v
		// git push origin master (--force?????)

		Brewday.getInstance().getLog().log(Log.DEBUG, "enabling git backend");

		outputCollector.append("-----------------------------------------\n");

		try
		{
//			runCmd(localRepo, "git config --list --show-origin", envp);
			runCmd(localRepo, "git init", outputCollector);

			// write gitignore file
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(localRepo, ".gitignore")));
			pw.println("/sensitive");
			pw.flush();
			pw.close();

			runCmd(localRepo, "git add .", outputCollector);
			runCmd(localRepo, "git commit -m \"Brewday enabling git backend\"", outputCollector);
			if (remoteRepo != null)
			{
				runCmd(localRepo, "git remote rm origin", outputCollector);
				runCmd(localRepo, "git remote add origin " + remoteRepo, outputCollector);
				runCmd(localRepo, "git remote -v", outputCollector);
				runCmd(localRepo, "git push origin master --force", outputCollector);
			}

			// At this point the git backend is successfully enabled
			// Store the settings related to it

			Settings settings = Database.getInstance().getSettings();
			settings.set(Settings.GIT_BACKEND_ENABLED, "true");
			settings.set(Settings.GIT_REMOTE_REPO, remoteRepo);

			Database.getInstance().saveSettings();
		}
		catch (Exception e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void disable(OutputCollector outputCollector)
	{
		outputCollector.append("-----------------------------------------\n");

		Settings settings = Database.getInstance().getSettings();
		settings.set(Settings.GIT_BACKEND_ENABLED, "false");
		settings.set(Settings.GIT_REMOTE_REPO, null);

		Database.getInstance().saveSettings();

		outputCollector.append(StringUtils.getUiString("settings.git.disable.complete"));
		outputCollector.append("\n");
	}

	/*-------------------------------------------------------------------------*/
	public void syncToRemote(File localRepo, OutputCollector outputCollector)
	{
		// git push origin master

		try
		{
			outputCollector.append("-----------------------------------------\n");

			String remoteRepo = Database.getInstance().getSettings().get(Settings.GIT_REMOTE_REPO);

			runCmd(localRepo, "git add .", outputCollector);
			runCmd(localRepo, "git commit -m \"Brewday auto commit\"", outputCollector);
			if (remoteRepo != null)
			{
				runCmd(localRepo, "git push origin master", outputCollector);
			}
		}
		catch (Exception e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void syncFromRemote(File localRepo, OutputCollector outputCollector)
	{
		// git fetch origin
		// git reset --hard origin/master

		try
		{
			outputCollector.append("-----------------------------------------\n");

			String remoteRepo = Database.getInstance().getSettings().get(Settings.GIT_REMOTE_REPO);

			if (remoteRepo != null)
			{
				runCmd(localRepo, "git fetch origin", outputCollector);
				runCmd(localRepo, "git reset --hard origin/master", outputCollector);
			}
			else
			{
				runCmd(localRepo, "git reset --hard HEAD", outputCollector);
			}
		}
		catch (Exception e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
//	public void rollback()
//	{
	// git reset --hard HEAD~1
//	}

	/*-------------------------------------------------------------------------*/
	private static String[] getEnvP()
	{
		// set up the env vars to use spawning the process
		Map<String, String> env = System.getenv();
		String[] envp = new String[env.size()];

		int i = 0;
		for (String key : env.keySet())
		{
			envp[i++] = key + "=" + env.get(key);
		}
		return envp;
	}

	/*-------------------------------------------------------------------------*/
	private void runCmd(File workingDir, String cmd,
		OutputCollector outputCollector) throws Exception
	{
		outputCollector.append(cmd);
		outputCollector.append("\n");
		Process p = Runtime.getRuntime().exec(cmd, getEnvP(), workingDir);

		new Appender(outputCollector, p.getInputStream()).start();
		new Appender(outputCollector, p.getErrorStream()).start();

		p.waitFor(100, TimeUnit.MILLISECONDS);
//		outputCollector.append(getResults( p.getInputStream()));
//		outputCollector.append(getResults(p.getErrorStream()));
	}

	/*-------------------------------------------------------------------------*/
/*
	private static String getResults(InputStream stream) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		StringBuilder sb = new StringBuilder();

		String line = "";
		while ((line = reader.readLine()) != null)
		{
			sb.append(line).append("\n");
		}

		return sb.toString();
	}
 */

	/*-------------------------------------------------------------------------*/

	/**
	 * Interface to collect cmd output
	 */
	public interface OutputCollector
	{
		void append(String s);
	}

	/*-------------------------------------------------------------------------*/

	private static class Appender extends Thread
	{
		private final OutputCollector oc;
		private final InputStream is;

		public Appender(OutputCollector oc, InputStream is)
		{
			this.oc = oc;
			this.is = is;
		}

		@Override
		public void run()
		{
			try
			{
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null)
				{
					oc.append(line);
					oc.append("\n");
				}
			}
			catch (IOException ioe)
			{
				throw new BrewdayException(ioe);
			}
		}
	}

	/*-------------------------------------------------------------------------*/

	public static void main(String[] args)
	{
		Database.getInstance().loadAll();
		GitBackend b = new GitBackend();

		b.enable(new File(args[0]), "https://github.com/alan-mcl/brewday_data_test.git", System.out::println);

		b.syncToRemote(new File(args[0]), System.out::println);
	}
}
