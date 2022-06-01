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
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class BrewingSettingsMashPane extends MigPane
{
	private final ComboBox<Settings.MashPhModel> mashPhModel;
	private final Label mashPhModelDesc;
	private boolean refreshing;

	private final CardGroup settingsCards;
	private final QuantityEditWidget<PercentageUnit> mphMaltCorrectionFactor;

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

		settingsCards = new CardGroup();

		MigPane mphSettings = new MigPane();

		Label settingsHeading = new Label(StringUtils.getUiString("settings.advanced"));
		settingsHeading.setStyle("-fx-font-weight: bold");
		mphSettings.add(settingsHeading, "wrap");
		mphSettings.add(new Label(StringUtils.getUiString("settings.dont.muck")), "span, wrap");
		mphSettings.add(new Label(StringUtils.getUiString("mash.ph.model.mph.malt.correction.factor")));
		mphMaltCorrectionFactor = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);
		mphSettings.add(mphMaltCorrectionFactor);

		settingsCards.add(Settings.MashPhModel.MPH.name(), mphSettings);
		settingsCards.add(Settings.MashPhModel.EZ_WATER.name(), new MigPane());

		this.add(settingsCards, "span, wrap");

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

					settingsCards.setVisible(name);
				}
			});

		mphMaltCorrectionFactor.addListener((observable, oldV, newV) ->
		{
			if (!refreshing)
			{
				double v = mphMaltCorrectionFactor.getQuantity().get(Quantity.Unit.PERCENTAGE);
				settings.set(Settings.MPH_MALT_BUFFERING_CORRECTION_FACTOR, String.valueOf(v));
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

		Settings.MashPhModel model = Settings.MashPhModel.valueOf(settings.get(Settings.MASH_PH_MODEL));
		mashPhModel.getSelectionModel().select(model);
		mashPhModelDesc.setText(StringUtils.getUiString("mash.ph.model.desc."+model.name()));
		settingsCards.setVisible(model.name());

		mphMaltCorrectionFactor.refresh(
			Double.valueOf(settings.get(Settings.MPH_MALT_BUFFERING_CORRECTION_FACTOR)));

		this.refreshing = false;
	}

}
