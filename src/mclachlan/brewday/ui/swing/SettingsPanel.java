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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class SettingsPanel extends JPanel implements ActionListener
{
	private int dirtyFlag;
	private JComboBox<String> defaultEquipmentProfile;

	/*-------------------------------------------------------------------------*/
	public SettingsPanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new MigLayout());

		Vector<String> vec = new Vector<>(Database.getInstance().getEquipmentProfiles().keySet());
		vec.sort(Comparator.comparing(String::toString));
		defaultEquipmentProfile = new JComboBox<>(vec);
		defaultEquipmentProfile.addActionListener(this);

		this.add(new JLabel("Default Equipment Profile:"));
		this.add(defaultEquipmentProfile, "wrap");

		refresh();
	}

	/*-------------------------------------------------------------------------*/
	public void refresh()
	{
		this.defaultEquipmentProfile.removeActionListener(this);

		Settings settings = Database.getInstance().getSettings();

		this.defaultEquipmentProfile.setSelectedItem(
			settings.get(Settings.DEFAULT_EQUIPMENT_PROFILE));

		this.defaultEquipmentProfile.addActionListener(this);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == defaultEquipmentProfile)
		{
			Database.getInstance().getSettings().set(
				Settings.DEFAULT_EQUIPMENT_PROFILE, (String)defaultEquipmentProfile.getSelectedItem());
			SwingUi.instance.setDirty(dirtyFlag);
		}
	}
}
