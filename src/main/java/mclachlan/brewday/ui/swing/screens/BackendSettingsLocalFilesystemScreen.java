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

package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ui.swing.app.SwingAppFrame;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.util.AppContentRoot;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Local file-system backend settings: database and backup paths, manual restore of the
 * previous full-save snapshot ({@link Database#restoreDb()}).
 */
public class BackendSettingsLocalFilesystemScreen extends JPanel implements SwingScreen
{
	private final SwingAppFrame appFrame;

	private final JTextArea intro = new JTextArea();
	private final JTextField dbPathField = new JTextField();
	private final JTextField backupPathField = new JTextField();
	private final JTextField configPathField = new JTextField();
	private final JTextField contentRootField = new JTextField();
	private final JButton restoreButton = new JButton(getUiString("settings.local.storage.restore.backup"));

	public BackendSettingsLocalFilesystemScreen(SwingAppFrame appFrame)
	{
		super(new BorderLayout());
		this.appFrame = appFrame;
		setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(new EmptyBorder(4, 4, 8, 8));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		intro.setEditable(false);
		intro.setOpaque(false);
		intro.setLineWrap(true);
		intro.setWrapStyleWord(true);
		intro.setText(getUiString("settings.local.storage.intro"));
		intro.setBorder(javax.swing.BorderFactory.createEmptyBorder());
		form.add(intro, gbc);

		gbc.gridy++;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		form.add(new JLabel(getUiString("settings.local.storage.path.label")), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		configureReadOnlyField(dbPathField, "local.storage.db.path");
		form.add(dbPathField, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		form.add(new JLabel(getUiString("settings.local.storage.backup.path.label")), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		configureReadOnlyField(backupPathField, "local.storage.backup.path");
		form.add(backupPathField, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		form.add(new JLabel(getUiString("settings.local.storage.config.path.label")), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		configureReadOnlyField(configPathField, "local.storage.config.path");
		form.add(configPathField, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		form.add(new JLabel(getUiString("settings.local.storage.content.root.label")), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		configureReadOnlyField(contentRootField, "local.storage.content.root");
		form.add(contentRootField, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.NONE;
		restoreButton.setToolTipText(getUiString("settings.local.storage.restore.backup.tooltip"));
		form.add(restoreButton, gbc);

		add(form, BorderLayout.NORTH);

		restoreButton.addActionListener(this::onRestoreBackup);
		refresh();
	}

	private void configureReadOnlyField(JTextField field, String name)
	{
		field.setEditable(false);
		field.setName(name);
		field.setCaretPosition(0);
		field.setToolTipText(getUiString("ui.readonly.copy.tooltip"));
	}

	private Window windowAncestor()
	{
		return SwingUtilities.getWindowAncestor(this);
	}

	private void onRestoreBackup(ActionEvent e)
	{
		int opt = JOptionPane.showConfirmDialog(
			windowAncestor(),
			getUiString("settings.local.storage.restore.backup.msg"),
			getUiString("settings.local.storage.restore.backup.title"),
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if (opt != JOptionPane.OK_OPTION)
		{
			return;
		}

		appFrame.reloadAfterLocalBackupRestore(windowAncestor());
	}

	@Override
	public void refresh()
	{
		Database db = Database.getInstance();
		dbPathField.setText(db.getLocalStorageDirectory().getAbsolutePath());
		backupPathField.setText(db.getLocalStorageBackupDirectory().getAbsolutePath());

		String cfgDb = Brewday.getInstance().getAppConfig().getProperty(Brewday.BREWDAY_DB);
		configPathField.setText(cfgDb != null ? cfgDb : "");

		String contentRoot = System.getProperty(AppContentRoot.PROPERTY);
		contentRootField.setText(contentRoot != null ? contentRoot : "");

		boolean canRestore = db.hasLocalStorageBackup();
		restoreButton.setEnabled(canRestore);
		if (!canRestore)
		{
			restoreButton.setToolTipText(getUiString("settings.local.storage.restore.backup.none"));
		}
		else
		{
			restoreButton.setToolTipText(getUiString("settings.local.storage.restore.backup.tooltip"));
		}
	}
}
