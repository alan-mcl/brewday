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
public class BrewingSettingsMashPane extends MigPane
{
	private final ComboBox<Settings.MashPhModel> mashPhModel;
	private final Label mashPhModelDesc;
	private boolean refreshing;

	/*-------------------------------------------------------------------------*/
	public BrewingSettingsMashPane()
	{
		mashPhModel = new ComboBox<>(FXCollections.observableList(Arrays.asList(Settings.MashPhModel.values())));
		this.add(new Label(StringUtils.getUiString("settings.mash.ph.model")));
		this.add(mashPhModel, "wrap");

		mashPhModelDesc = new Label();
		mashPhModelDesc.setWrapText(true);
		mashPhModelDesc.setMaxWidth(500);
		this.add(mashPhModelDesc, "span, wrap");

		refresh();

		// ----

		Settings settings = Database.getInstance().getSettings();

		mashPhModel.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) ->
			{
				if (newValue != null && !newValue.equals(oldValue) && !refreshing)
				{
					String name = mashPhModel.getSelectionModel().getSelectedItem().name();
					settings.set(Settings.MASH_PH_MODEL, name);
					Database.getInstance().saveSettings();

					mashPhModelDesc.setText(StringUtils.getUiString("mash.ph.model.desc."+name));
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

		Settings.MashPhModel model = Settings.MashPhModel.valueOf(settings.get(Settings.MASH_PH_MODEL));
		mashPhModel.getSelectionModel().select(model);
		mashPhModelDesc.setText(StringUtils.getUiString("mash.ph.model.desc."+model.name()));

		this.refreshing = false;
	}

}
