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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.ui.swing;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class BackendSettingsPanel extends JTabbedPane implements ActionListener
{
	private int dirtyFlag;
	private JButton restoreLocalStorageBackup;

	/*-------------------------------------------------------------------------*/
	public BackendSettingsPanel(int dirtyFlag)
	{
		super(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		this.dirtyFlag = dirtyFlag;

		this.add(StringUtils.getUiString("settings.local.storage"), getLocalStoragePanel());
		this.add(StringUtils.getUiString("settings.google.drive"), getGoogleDrivePanel());
//		this.add("Dropbox", new JPanel());
//		this.add("Github", new JPanel());

		refresh();
	}

	private Container getGoogleDrivePanel()
	{
		JPanel result = new JPanel();

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
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == restoreLocalStorageBackup)
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
}
