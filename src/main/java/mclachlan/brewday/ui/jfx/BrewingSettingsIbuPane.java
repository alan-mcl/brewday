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
public class BrewingSettingsIbuPane extends MigPane
{
	private final ComboBox<Settings.HopBitternessFormula> hopBitternessModel;
	private final Label hopModelDesc;
	private boolean refreshing;

	private final CardGroup settingsCards;
	private final QuantityEditWidget<PercentageUnit> tinsethMaxUtilFactor,
		tinsethBSMaxUtilFactor, garetzYeastFactor, garetzPelletFactor,
		garetzBagFactor, garetzFilterFactor;

	/*-------------------------------------------------------------------------*/
	public BrewingSettingsIbuPane()
	{
		hopBitternessModel = new ComboBox<>(FXCollections.observableList(Arrays.asList(Settings.HopBitternessFormula.values())));
		this.add(new Label(StringUtils.getUiString("settings.hop.bitterness.formula")));
		this.add(hopBitternessModel, "wrap");

		hopModelDesc = new Label();
		hopModelDesc.setWrapText(true);
		hopModelDesc.setMaxWidth(500);
		this.add(hopModelDesc, "span, wrap");

		MigPane tinsethSettings = new MigPane();
		Label settingsHeading = new Label(StringUtils.getUiString("settings.advanced"));
		settingsHeading.setStyle("-fx-font-weight: bold");
		tinsethSettings.add(settingsHeading, "wrap");
		tinsethSettings.add(new Label(StringUtils.getUiString("settings.dont.muck")), "span, wrap");
		tinsethMaxUtilFactor = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);
		tinsethSettings.add(new Label(StringUtils.getUiString("settings.tinseth.max.utilisation")));
		tinsethSettings.add(tinsethMaxUtilFactor, "wrap");

		MigPane tinsethBSSettings = new MigPane();
		settingsHeading = new Label(StringUtils.getUiString("settings.advanced"));
		settingsHeading.setStyle("-fx-font-weight: bold");
		tinsethBSSettings.add(settingsHeading, "wrap");
		tinsethBSSettings.add(new Label(StringUtils.getUiString("settings.dont.muck")), "span, wrap");
		tinsethBSMaxUtilFactor = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);
		tinsethBSSettings.add(new Label(StringUtils.getUiString("settings.tinseth.max.utilisation")));
		tinsethBSSettings.add(tinsethBSMaxUtilFactor, "wrap");


		MigPane garetzSettings = new MigPane();
		settingsHeading = new Label(StringUtils.getUiString("settings.advanced"));
		settingsHeading.setStyle("-fx-font-weight: bold");
		garetzSettings.add(settingsHeading, "wrap");
		garetzSettings.add(new Label(StringUtils.getUiString("settings.dont.muck")), "span, wrap");

		garetzYeastFactor = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);
		garetzPelletFactor = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);
		garetzBagFactor = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);
		garetzFilterFactor = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);

		garetzSettings.add(new Label(StringUtils.getUiString("settings.garetz.yeast.factor")));
		garetzSettings.add(garetzYeastFactor, "wrap");
		garetzSettings.add(new Label(StringUtils.getUiString("settings.garetz.pellet.factor")));
		garetzSettings.add(garetzPelletFactor, "wrap");
		garetzSettings.add(new Label(StringUtils.getUiString("settings.garetz.bag.factor")));
		garetzSettings.add(garetzBagFactor, "wrap");
		garetzSettings.add(new Label(StringUtils.getUiString("settings.garetz.filter.factor")));
		garetzSettings.add(garetzFilterFactor, "wrap");

		settingsCards = new CardGroup();

		settingsCards.add(Settings.HopBitternessFormula.RAGER.name(), new MigPane());
		settingsCards.add(Settings.HopBitternessFormula.TINSETH_BEERSMITH.name(), tinsethBSSettings);
		settingsCards.add(Settings.HopBitternessFormula.TINSETH.name(), tinsethSettings);
		settingsCards.add(Settings.HopBitternessFormula.DANIELS.name(), new MigPane());
		settingsCards.add(Settings.HopBitternessFormula.GARETZ.name(), garetzSettings);

		this.add(settingsCards, "span, wrap");

		refresh();

		// ----

		Settings settings = Database.getInstance().getSettings();

		hopBitternessModel.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) ->
			{
				if (newValue != null && !newValue.equals(oldValue) && !refreshing)
				{
					String name = hopBitternessModel.getSelectionModel().getSelectedItem().name();
					settings.set(Settings.HOP_BITTERNESS_FORMULA, name);
					Database.getInstance().saveSettings();

					hopModelDesc.setText(StringUtils.getUiString("bitterness.model.desc." + name));

					settingsCards.setVisible(name);
				}
			});

		tinsethMaxUtilFactor.addListener((obs, oldV, newV) ->
			{
				if (!refreshing)
				{
					settings.set(Settings.TINSETH_MAX_UTILISATION,
						String.valueOf(tinsethMaxUtilFactor.getQuantity().get(Quantity.Unit.PERCENTAGE)));

					Database.getInstance().saveSettings();
				}
			}
		);
		tinsethBSMaxUtilFactor.addListener((obs, oldV, newV) ->
			{
				if (!refreshing)
				{
					settings.set(Settings.TINSETH_MAX_UTILISATION,
						String.valueOf(tinsethMaxUtilFactor.getQuantity().get(Quantity.Unit.PERCENTAGE)));

					Database.getInstance().saveSettings();
				}
			}
		);

		garetzYeastFactor.addListener((obs, oldV, newV) ->
			{
				if (!refreshing)
				{
					settings.set(Settings.GARETZ_YEAST_FACTOR,
						String.valueOf(garetzYeastFactor.getQuantity().get(Quantity.Unit.PERCENTAGE)));
					Database.getInstance().saveSettings();
				}
			}
		);
		garetzPelletFactor.addListener((obs, oldV, newV) ->
			{
				if (!refreshing)
				{
					settings.set(Settings.GARETZ_PELLET_FACTOR,
						String.valueOf(garetzPelletFactor.getQuantity().get(Quantity.Unit.PERCENTAGE)));
					Database.getInstance().saveSettings();
				}
			}
		);
		garetzBagFactor.addListener((obs, oldV, newV) ->
			{
				if (!refreshing)
				{
					settings.set(Settings.GARETZ_BAG_FACTOR,
						String.valueOf(garetzBagFactor.getQuantity().get(Quantity.Unit.PERCENTAGE)));
					Database.getInstance().saveSettings();
				}
			}
		);
		garetzYeastFactor.addListener((obs, oldV, newV) ->
			{
				if (!refreshing)
				{
					settings.set(Settings.GARETZ_FILTER_FACTOR,
						String.valueOf(garetzFilterFactor.getQuantity().get(Quantity.Unit.PERCENTAGE)));
					Database.getInstance().saveSettings();
				}
			}
		);
	}

	/*-------------------------------------------------------------------------*/
	public void refresh()
	{
		this.refreshing = true;

		Settings settings = Database.getInstance().getSettings();

		Settings.HopBitternessFormula model = Settings.HopBitternessFormula.valueOf(settings.get(Settings.HOP_BITTERNESS_FORMULA));
		this.hopBitternessModel.getSelectionModel().select(model);

		hopModelDesc.setText(StringUtils.getUiString("bitterness.model.desc." + model.name()));

		tinsethMaxUtilFactor.refresh(Double.valueOf(settings.get(Settings.TINSETH_MAX_UTILISATION)));
		tinsethBSMaxUtilFactor.refresh(Double.valueOf(settings.get(Settings.TINSETH_MAX_UTILISATION)));

		garetzYeastFactor.refresh(Double.valueOf(settings.get(Settings.GARETZ_YEAST_FACTOR)));
		garetzPelletFactor.refresh(Double.valueOf(settings.get(Settings.GARETZ_PELLET_FACTOR)));
		garetzBagFactor.refresh(Double.valueOf(settings.get(Settings.GARETZ_BAG_FACTOR)));
		garetzFilterFactor.refresh(Double.valueOf(settings.get(Settings.GARETZ_FILTER_FACTOR)));

		settingsCards.setVisible(model.name());

		this.refreshing = false;
	}

}
