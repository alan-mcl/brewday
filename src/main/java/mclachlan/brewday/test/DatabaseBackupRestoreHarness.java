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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.util.AppContentRoot;

/**
 * Manual harness for {@link Database} backup/restore behaviour.
 * <p>
 * From repo root after {@code ant compile}:
 * {@code java -cp build/classes mclachlan.brewday.test.DatabaseBackupRestoreHarness}
 */
public class DatabaseBackupRestoreHarness
{
	private static int failures;

	public static void main(String[] args) throws Exception
	{
		AppContentRoot.install();

		Path tempRoot = Files.createTempDirectory("brewday_db_harness");
		try
		{
			Path dbPath = tempRoot.resolve("db");
			copyDbTree(Paths.get("test_data/test_db"), dbPath);

			Database db = new Database(dbPath.toAbsolutePath().toString());
			db.loadAll();
			// GitBackend.syncToRemote uses Database.getInstance(); avoid git during harness saves.
			db.getSettings().set(Settings.GIT_BACKEND_ENABLED, "false");

			testNoBackupOnEmpty(tempRoot.resolve("empty"));
			testHasBackupAfterSaveAll(db);
			testRestoreDbRevertsCorruptedFile(db);
			testSaveSettingsLeavesOtherSilosUntouched(db);
			testRestoreDbThrowsWithoutBackup(tempRoot.resolve("no_backup_db"));
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
		System.out.println("DatabaseBackupRestoreHarness: all checks passed");
	}

	private static void testNoBackupOnEmpty(Path emptyDb) throws IOException
	{
		Files.createDirectories(emptyDb);
		Database db = new Database(emptyDb.toAbsolutePath().toString());
		check("empty db has no backup", !db.hasLocalStorageBackup());
	}

	private static void testHasBackupAfterSaveAll(Database db) throws IOException
	{
		db.saveAll();
		check("backup exists after saveAll", db.hasLocalStorageBackup());
	}

	private static void testRestoreDbRevertsCorruptedFile(Database db) throws Exception
	{
		Path recipes = db.getLocalStorageDirectory().toPath().resolve(Database.RECIPES_JSON);
		String intact = Files.readString(recipes, StandardCharsets.UTF_8);

		db.saveAll();
		String backupRecipes = Files.readString(
			db.getLocalStorageBackupDirectory().toPath().resolve(Database.RECIPES_JSON),
			StandardCharsets.UTF_8);
		check("backup recipes matches pre-save snapshot", intact.equals(backupRecipes));

		Files.writeString(recipes, "CORRUPT", StandardCharsets.UTF_8);

		db.restoreDb();
		String restored = Files.readString(recipes, StandardCharsets.UTF_8);
		check("restoreDb reverts corrupted recipes.json", backupRecipes.equals(restored));
	}

	private static void testSaveSettingsLeavesOtherSilosUntouched(Database db) throws Exception
	{
		db.loadAll();
		Path recipes = db.getLocalStorageDirectory().toPath().resolve(Database.RECIPES_JSON);
		String recipesBefore = Files.readString(recipes, StandardCharsets.UTF_8);

		db.saveAll();

		String priorUtil = db.getSettings().get(Settings.MASH_HOP_UTILISATION);
		String changed = "0.99".equals(priorUtil) ? "0.98" : "0.99";
		db.getSettings().set(Settings.MASH_HOP_UTILISATION, changed);
		db.saveSettings();

		String recipesAfter = Files.readString(recipes, StandardCharsets.UTF_8);
		check("saveSettings does not modify recipes.json", recipesBefore.equals(recipesAfter));

		Path backupSettings = db.getLocalStorageBackupDirectory().toPath().resolve(Database.SETTINGS_JSON);
		check("settings backup file exists after saveSettings", Files.isRegularFile(backupSettings));
	}

	private static void testRestoreDbThrowsWithoutBackup(Path dbPath) throws IOException
	{
		Files.createDirectories(dbPath);
		Files.writeString(
			dbPath.resolve(Database.SETTINGS_JSON),
			"{}\n",
			StandardCharsets.UTF_8);
		Database db = new Database(dbPath.toAbsolutePath().toString());
		check("no backup before first saveAll", !db.hasLocalStorageBackup());

		boolean threw = false;
		try
		{
			db.restoreDb();
		}
		catch (IOException expected)
		{
			threw = true;
		}
		check("restoreDb throws when backup missing", threw);
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

	private static void check(String name, boolean condition)
	{
		if (condition)
		{
			System.out.println("  OK: " + name);
		}
		else
		{
			System.err.println("FAIL: " + name);
			failures++;
		}
	}
}
