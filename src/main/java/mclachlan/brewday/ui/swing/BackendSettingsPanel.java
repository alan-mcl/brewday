/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.remote.gdrive.GoogleDriveBackend;
import mclachlan.brewday.db.v2.sensitive.SensitiveStore;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class BackendSettingsPanel extends JTabbedPane implements ActionListener
{
	private int dirtyFlag;

	// local storage
	private JButton restoreLocalStorageBackup;

	// google drive
	private JButton enableGoogleDrive, disableGoogleDrive, syncToGoogleDrive;
	private JLabel googleDriveDirectory;
	private JCheckBox autoSync;

	/*-------------------------------------------------------------------------*/
	public BackendSettingsPanel(int dirtyFlag)
	{
		super(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		this.dirtyFlag = dirtyFlag;

		this.add(StringUtils.getUiString("settings.local.storage"), getLocalStoragePanel());
		this.add(StringUtils.getUiString("settings.google.drive"), getGoogleDrivePanel());

		refresh();
	}

	/*-------------------------------------------------------------------------*/
	private Container getGoogleDrivePanel()
	{
		JPanel result = new JPanel(new MigLayout());

		enableGoogleDrive = new JButton(
			StringUtils.getUiString("settings.google.drive.enable"));
		enableGoogleDrive.addActionListener(this);

		disableGoogleDrive = new JButton(
			StringUtils.getUiString("settings.google.drive.disable"));
		disableGoogleDrive.addActionListener(this);

		googleDriveDirectory = new JLabel("-");
		googleDriveDirectory.setBorder(BorderFactory.createLoweredSoftBevelBorder());

		autoSync = new JCheckBox(
			StringUtils.getUiString("settings.google.drive.auto.sync"));
		autoSync.addActionListener(this);

		syncToGoogleDrive = new JButton(
			StringUtils.getUiString("settings.google.drive.sync"));
		syncToGoogleDrive.addActionListener(this);

		result.add(enableGoogleDrive);
		result.add(disableGoogleDrive, "wrap");

		result.add(new JLabel(StringUtils.getUiString("settings.google.drive.directory")));
		result.add(googleDriveDirectory, "wrap");

		result.add(autoSync, "wrap");
		result.add(syncToGoogleDrive, "wrap");

		return result;
	}

	/*-------------------------------------------------------------------------*/
	private Container getLocalStoragePanel()
	{
		JPanel result = new JPanel(new MigLayout());

		JLabel line1 = new JLabel(
			StringUtils.getUiString("settings.local.storage.location",
				Database.getInstance().getLocalStorageDirectory().getAbsolutePath()));
		JLabel line2 = new JLabel(
			StringUtils.getUiString("settings.local.storage.backup",
				Database.getInstance().getLocalStorageBackupDirectory().getAbsolutePath()));

		restoreLocalStorageBackup = new JButton(
			StringUtils.getUiString("settings.local.storage.restore.backup"));
		restoreLocalStorageBackup.addActionListener(this);

		result.add(line1, "wrap");
		result.add(line2, "wrap");
		result.add(restoreLocalStorageBackup, "wrap");

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public void refresh()
	{
		Settings settings = Database.getInstance().getSettings();

		String googleDriveDir = settings.get(Settings.GOOGLE_DRIVE_DIRECTORY_NAME);

		if (googleDriveDir != null)
		{
			enableGoogleDrive.setEnabled(false);

			disableGoogleDrive.setEnabled(true);
			syncToGoogleDrive.setEnabled(true);
			autoSync.setEnabled(true);
			googleDriveDirectory.setText(googleDriveDir);

			boolean autoSyncB = Boolean.valueOf(settings.get(Settings.GOOGLE_DRIVE_AUTO_SYNC));

			autoSync.removeActionListener(this);
			autoSync.setSelected(autoSyncB);
			autoSync.addActionListener(this);
		}
		else
		{
			enableGoogleDrive.setEnabled(true);

			disableGoogleDrive.setEnabled(false);
			syncToGoogleDrive.setEnabled(false);
			autoSync.setEnabled(false);
			googleDriveDirectory.setText("-");

			autoSync.removeActionListener(this);
			autoSync.setSelected(false);
			autoSync.addActionListener(this);
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == restoreLocalStorageBackup)
		{
			localStorageRestore();
		}
		else if (e.getSource() == enableGoogleDrive)
		{
			enableGoogleDrive();
			refresh();
		}
		else if (e.getSource() == syncToGoogleDrive)
		{
			syncToGoogleDrive();
		}
	}

	/*-------------------------------------------------------------------------*/
	private void enableGoogleDrive()
	{
		String remoteDirectoryName = JOptionPane.showInputDialog(
			SwingUi.instance,
			StringUtils.getUiString("settings.google.drive.enable.msg"),
			StringUtils.getUiString("settings.google.drive.enable.title"),
			JOptionPane.QUESTION_MESSAGE);

		if (remoteDirectoryName != null && remoteDirectoryName.length()>0)
		{
			if (SwingUi.instance.isAnyDirty())
			{
				if (!SwingUi.instance.confirmAndSaveAllChanges())
				{
					return;
				}
			}

			try
			{
				GoogleDriveBackend backend = new GoogleDriveBackend();

				Properties appConfig = Brewday.getInstance().getAppConfig();
				SensitiveStore ss = new SensitiveStore("db/sensitive", "brewday");
				ss.init(appConfig.getProperty("mclachlan.brewday.app.key"));
				String credentials = ss.get("google.api.credentials");

				String appName = appConfig.getProperty("mclachlan.brewday.google.drive.app.name");
				String tokensDirectoryPath = "db/sensitive";
				String folderId = backend.enable(appName, credentials, remoteDirectoryName, tokensDirectoryPath);

				java.io.File[] files =
					new java.io.File("./db").listFiles((dir, name) -> name.endsWith(".json"));

				backend.syncToRemote(
					Arrays.asList(files),
					credentials,
					folderId,
					appName,
					tokensDirectoryPath);

				Database.getInstance().getSettings().set(Settings.GOOGLE_DRIVE_DIRECTORY_NAME, remoteDirectoryName);
				Database.getInstance().getSettings().set(Settings.GOOGLE_DRIVE_DIRECTORY_ID, folderId);

				Database.getInstance().saveAll();
			}
			catch (Exception x)
			{
				throw new BrewdayException(x);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private void syncToGoogleDrive()
	{
		Settings settings = Database.getInstance().getSettings();
		String remoteDirectoryName = settings.get(Settings.GOOGLE_DRIVE_DIRECTORY_NAME);
		String remoteFolderId = settings.get(Settings.GOOGLE_DRIVE_DIRECTORY_ID);

		if (remoteDirectoryName != null && remoteFolderId != null)
		{
			if (SwingUi.instance.isAnyDirty())
			{
				if (!SwingUi.instance.confirmAndSaveAllChanges())
				{
					return;
				}
			}

			try
			{
				GoogleDriveBackend backend = new GoogleDriveBackend();

				Properties appConfig = Brewday.getInstance().getAppConfig();
				SensitiveStore ss = new SensitiveStore("db/sensitive", "brewday");
				ss.init(appConfig.getProperty("mclachlan.brewday.app.key"));
				String credentials = ss.get("google.api.credentials");

				String appName = appConfig.getProperty("mclachlan.brewday.google.drive.app.name");
				String tokensDirectoryPath = "db/sensitive";

				java.io.File[] files =
					new java.io.File("./db").listFiles((dir, name) -> name.endsWith(".json"));

				backend.syncToRemote(
					Arrays.asList(files),
					credentials,
					remoteFolderId,
					appName,
					tokensDirectoryPath);
			}
			catch (Exception x)
			{
				throw new BrewdayException(x);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private void localStorageRestore()
	{
		int dialogResult = JOptionPane.showConfirmDialog(
			SwingUi.instance,
			StringUtils.getUiString("settings.local.storage.restore.backup.msg"),
			StringUtils.getUiString("settings.local.storage.restore.backup.title"),
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE);

		if (dialogResult == JOptionPane.OK_OPTION)
		{
			try
			{
				Database.getInstance().restoreDb();

				JOptionPane.showMessageDialog(
					SwingUi.instance,
					StringUtils.getUiString("settings.local.storage.restore.backup.success"),
					StringUtils.getUiString("settings.local.storage.restore.backup.title"),
					JOptionPane.INFORMATION_MESSAGE);

				SwingUi.instance.discardChanges();
			}
			catch (Exception x)
			{
				throw new BrewdayException(x);
			}
		}
	}
}
