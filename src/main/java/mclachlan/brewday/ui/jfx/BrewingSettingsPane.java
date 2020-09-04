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
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class BrewingSettingsPane extends MigPane
{
	private final ComboBox<String> defaultEquipmentProfile;
	private final QuantityEditWidget<PercentageUnit>
		mashHopUtilisaton, firstWortHopUtilisation,
		leafHopAdjustment, plugHopAdjustment, pelletHopAdjustment;
	private final ComboBox<Settings.HopBitternessFormula> hopBitternessFormula;
	private boolean refreshing;

	/*-------------------------------------------------------------------------*/
	public BrewingSettingsPane()
	{
		defaultEquipmentProfile = new ComboBox<>();
		this.add(new Label(StringUtils.getUiString("settings.default.equipment.profile")));
		this.add(defaultEquipmentProfile, "wrap");

		hopBitternessFormula = new ComboBox<>(FXCollections.observableList(Arrays.asList(Settings.HopBitternessFormula.values())));
		this.add(new Label(StringUtils.getUiString("settings.hop.bitterness.formula")));
		this.add(hopBitternessFormula, "wrap");

		mashHopUtilisaton = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		this.add(new Label(StringUtils.getUiString("settings.mash.hop.utilisation")));
		this.add(mashHopUtilisaton, "wrap");

		firstWortHopUtilisation = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		this.add(new Label(StringUtils.getUiString("settings.first.wort.hop.utilisation")));
		this.add(firstWortHopUtilisation, "wrap");

		leafHopAdjustment = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		this.add(new Label(StringUtils.getUiString("settings.leaf.hop.adjustment")));
		this.add(leafHopAdjustment, "wrap");

		plugHopAdjustment = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		this.add(new Label(StringUtils.getUiString("settings.plug.hop.adjustment")));
		this.add(plugHopAdjustment, "wrap");

		pelletHopAdjustment = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		this.add(new Label(StringUtils.getUiString("settings.pellet.hop.adjustment")));
		this.add(pelletHopAdjustment, "wrap");



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

		hopBitternessFormula.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) ->
			{
				if (newValue != null && !newValue.equals(oldValue) && !refreshing)
				{
					settings.set(Settings.HOP_BITTERNESS_FORMULA,
						hopBitternessFormula.getSelectionModel().getSelectedItem().name());
					Database.getInstance().saveSettings();
				}
			});

		mashHopUtilisaton.addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null && !newValue.equals(oldValue) && !refreshing)
			{
				settings.set(Settings.MASH_HOP_UTILISATION,
					""+mashHopUtilisaton.getQuantity().get(Quantity.Unit.PERCENTAGE));
				Database.getInstance().saveSettings();
			}
		});

		firstWortHopUtilisation.addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null && !newValue.equals(oldValue) && !refreshing)
			{
				settings.set(Settings.FIRST_WORT_HOP_UTILISATION,
					""+firstWortHopUtilisation.getQuantity().get(Quantity.Unit.PERCENTAGE));
				Database.getInstance().saveSettings();
			}
		});

		leafHopAdjustment.addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null && !newValue.equals(oldValue) && !refreshing)
			{
				settings.set(Settings.LEAF_HOP_ADJUSTMENT,
					""+leafHopAdjustment.getQuantity().get(Quantity.Unit.PERCENTAGE));
				Database.getInstance().saveSettings();
			}
		});
		plugHopAdjustment.addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null && !newValue.equals(oldValue) && !refreshing)
			{
				settings.set(Settings.PLUG_HOP_ADJUSTMENT,
					""+plugHopAdjustment.getQuantity().get(Quantity.Unit.PERCENTAGE));
				Database.getInstance().saveSettings();
			}
		});
		pelletHopAdjustment.addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null && !newValue.equals(oldValue) && !refreshing)
			{
				settings.set(Settings.PELLET_HOP_ADJUSTMENT,
					""+pelletHopAdjustment.getQuantity().get(Quantity.Unit.PERCENTAGE));
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

		this.hopBitternessFormula.getSelectionModel().select(Settings.HopBitternessFormula.valueOf(settings.get(Settings.HOP_BITTERNESS_FORMULA)));
		this.mashHopUtilisaton.refresh(new PercentageUnit(Double.valueOf(settings.get(Settings.MASH_HOP_UTILISATION))));
		this.firstWortHopUtilisation.refresh(new PercentageUnit(Double.valueOf(settings.get(Settings.FIRST_WORT_HOP_UTILISATION))));
		this.leafHopAdjustment.refresh(new PercentageUnit(Double.valueOf(settings.get(Settings.LEAF_HOP_ADJUSTMENT))));
		this.plugHopAdjustment.refresh(new PercentageUnit(Double.valueOf(settings.get(Settings.PLUG_HOP_ADJUSTMENT))));
		this.pelletHopAdjustment.refresh(new PercentageUnit(Double.valueOf(settings.get(Settings.PELLET_HOP_ADJUSTMENT))));

		this.refreshing = false;
	}

}
