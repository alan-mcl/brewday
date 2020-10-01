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

package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class BrewingSettingsGeneralPane extends MigPane
{
	private final ComboBox<String> defaultEquipmentProfile;
	private boolean refreshing;

	/*-------------------------------------------------------------------------*/
	public BrewingSettingsGeneralPane()
	{
		defaultEquipmentProfile = new ComboBox<>();
		this.add(new Label(StringUtils.getUiString("settings.default.equipment.profile")));
		this.add(defaultEquipmentProfile, "wrap");

		refresh();

		// ----

		Settings settings = Database.getInstance().getSettings();
		defaultEquipmentProfile.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) ->
			{
				if (newValue != null && !newValue.equals(oldValue) && !refreshing)
				{
					settings.set(Settings.DEFAULT_EQUIPMENT_PROFILE,
						(String)defaultEquipmentProfile.getSelectionModel().getSelectedItem());
					Database.getInstance().saveSettings();
				}
			});
	}

	/*-------------------------------------------------------------------------*/
	public void refresh()
	{
		this.refreshing = true;

		Settings settings = Database.getInstance().getSettings();

		ArrayList<String> equipmentProfiles = new ArrayList<>(Database.getInstance().getEquipmentProfiles().keySet());
		equipmentProfiles.sort(Comparator.comparing(String::toString));
		this.defaultEquipmentProfile.setItems(FXCollections.observableList(equipmentProfiles));

		this.defaultEquipmentProfile.getSelectionModel().select(
			settings.get(Settings.DEFAULT_EQUIPMENT_PROFILE));

		this.refreshing = false;
	}

}
