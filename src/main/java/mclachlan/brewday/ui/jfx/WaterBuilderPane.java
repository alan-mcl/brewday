
/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.WaterAddition;
import org.tbee.javafx.scene.layout.MigPane;

import static mclachlan.brewday.math.Quantity.Unit.PPM;

/**
 *
 */
public class WaterBuilderPane extends MigPane
{
	private final ComboBox<String> sourceWaterName;
	private final ComboBox<String> dilutionWaterName;
	private final QuantityEditWidget<VolumeUnit> sourceVol;
	private final QuantityEditWidget<VolumeUnit> dilutionVol;
	private final QuantityEditWidget<VolumeUnit> targetVol;

	private final QuantityEditWidget<PpmUnit> sourceCa;
	private final QuantityEditWidget<PpmUnit> sourceMg;
	private final QuantityEditWidget<PpmUnit> sourceNa;
	private final QuantityEditWidget<PpmUnit> sourceSO4;
	private final QuantityEditWidget<PpmUnit> sourceCl;
	private final QuantityEditWidget<PpmUnit> sourceHCO3;
	private final QuantityEditWidget<PhUnit> sourcePh;
	private final QuantityEditWidget<PpmUnit> sourceAlk;
	private final QuantityEditWidget<PpmUnit> sourceRA;

	private final QuantityEditWidget<PpmUnit> dilutionCa;
	private final QuantityEditWidget<PpmUnit> dilutionMg;
	private final QuantityEditWidget<PpmUnit> dilutionNa;
	private final QuantityEditWidget<PpmUnit> dilutionSO4;
	private final QuantityEditWidget<PpmUnit> dilutionCl;
	private final QuantityEditWidget<PpmUnit> dilutionHCO3;
	private final QuantityEditWidget<PhUnit> dilutionPh;
	private final QuantityEditWidget<PpmUnit> dilutionAlk;
	private final QuantityEditWidget<PpmUnit> dilutionRA;

	private final QuantityEditWidget<PpmUnit> targetMinCa;
	private final QuantityEditWidget<PpmUnit> targetMinMg;
	private final QuantityEditWidget<PpmUnit> targetMinNa;
	private final QuantityEditWidget<PpmUnit> targetMinSO4;
	private final QuantityEditWidget<PpmUnit> targetMinCl;
	private final QuantityEditWidget<PpmUnit> targetMinHCO3;
	private final QuantityEditWidget<PpmUnit> targetMinAlk;
	private final QuantityEditWidget<PpmUnit> targetMinRA;

	private final QuantityEditWidget<PpmUnit> targetMaxCa;
	private final QuantityEditWidget<PpmUnit> targetMaxMg;
	private final QuantityEditWidget<PpmUnit> targetMaxNa;
	private final QuantityEditWidget<PpmUnit> targetMaxSO4;
	private final QuantityEditWidget<PpmUnit> targetMaxCl;
	private final QuantityEditWidget<PpmUnit> targetMaxHCO3;
	private final QuantityEditWidget<PpmUnit> targetMaxAlk;
	private final QuantityEditWidget<PpmUnit> targetMaxRA;

	private final QuantityEditWidget<PpmUnit> resultCa;
	private final QuantityEditWidget<PpmUnit> resultMg;
	private final QuantityEditWidget<PpmUnit> resultNa;
	private final QuantityEditWidget<PpmUnit> resultSO4;
	private final QuantityEditWidget<PpmUnit> resultCl;
	private final QuantityEditWidget<PpmUnit> resultHCO3;
	private final QuantityEditWidget<PpmUnit> resultAlk;
	private final QuantityEditWidget<PpmUnit> resultRA;

	private final QuantityEditWidget<PpmUnit> deltaCa;
	private final QuantityEditWidget<PpmUnit> deltaMg;
	private final QuantityEditWidget<PpmUnit> deltaNa;
	private final QuantityEditWidget<PpmUnit> deltaSO4;
	private final QuantityEditWidget<PpmUnit> deltaCl;
	private final QuantityEditWidget<PpmUnit> deltaHCO3;
	private final QuantityEditWidget<PpmUnit> deltaAlk;
	private final QuantityEditWidget<PpmUnit> deltaRA;

	private final TextField mse;

	private final QuantityEditWidget<WeightUnit> addCaCO3Undissolved;
	private final QuantityEditWidget<WeightUnit> addCaCO3Dissolved;
	private final QuantityEditWidget<WeightUnit> addCaSO4;
	private final QuantityEditWidget<WeightUnit> addCaCl;
	private final QuantityEditWidget<WeightUnit> addMgSO4;
	private final QuantityEditWidget<WeightUnit> addNaHCO3;
	private final QuantityEditWidget<WeightUnit> addNaCl;
	private final QuantityEditWidget<WeightUnit> addCa_HCO3_2;
	private final QuantityEditWidget<WeightUnit> addMgCl;
	private final ComboBox<String> addCaCO3UndissolvedMisc;
	private final ComboBox<String> addCaCO3DissolvedMisc;
	private final ComboBox<String> addCaSO4Misc;
	private final ComboBox<String> addCaClMisc;
	private final ComboBox<String> addMgSO4Misc;
	private final ComboBox<String> addNaHCO3Misc;
	private final ComboBox<String> addNaClMisc;
	private final ComboBox<String> addCa_HCO3_2Misc;
	private final ComboBox<String> addMgClMisc;
	private final ComboBox<WaterBuilder.AdditionGoal> goal;
	private final String unspecifiedWater;

	/*-------------------------------------------------------------------------*/

	/**
	 * @param step can be null
	 */
	public WaterBuilderPane(ProcessStep step)
	{
		CheckBox[] allowed = new CheckBox[9];
		for (int i = 0; i < allowed.length; i++)
		{
			allowed[i] = new CheckBox();
			allowed[i].setSelected(true);
		}

		sourceWaterName = new ComboBox<>();
		dilutionWaterName = new ComboBox<>();
		ComboBox<String> targetWaterName = new ComboBox<>();

		unspecifiedWater = StringUtils.getUiString("tools.water.builder.water.name.none");

		ArrayList<String> waters = new ArrayList<>(Database.getInstance().getWaters().keySet());
		waters.sort(String::compareTo);
		waters.add(0, unspecifiedWater);
		sourceWaterName.setItems(FXCollections.observableList(waters));
		dilutionWaterName.setItems(FXCollections.observableList(waters));

		ArrayList<String> waterParams = new ArrayList<>(Database.getInstance().getWaterParameters().keySet());
		waterParams.sort(String::compareTo);
		waterParams.add(0, unspecifiedWater);
		targetWaterName.setItems(FXCollections.observableList(waterParams));

		sourceWaterName.getSelectionModel().select(0);
		dilutionWaterName.getSelectionModel().select(0);
		targetWaterName.getSelectionModel().select(0);

		sourceVol = new QuantityEditWidget<>(Quantity.Unit.LITRES, 0);
		dilutionVol = new QuantityEditWidget<>(Quantity.Unit.LITRES, 0);
		targetVol = new QuantityEditWidget<>(Quantity.Unit.LITRES, 0);
		targetVol.setDisable(true);

		sourceCa = new QuantityEditWidget<>(PPM, 0, false);
		sourceMg = new QuantityEditWidget<>(PPM, 0, false);
		sourceNa = new QuantityEditWidget<>(PPM, 0, false);
		sourceSO4 = new QuantityEditWidget<>(PPM, 0, false);
		sourceCl = new QuantityEditWidget<>(PPM, 0, false);
		sourceHCO3 = new QuantityEditWidget<>(PPM, 0, false);
		sourcePh = new QuantityEditWidget<>(Quantity.Unit.PH, 0);
		sourceAlk = new QuantityEditWidget<>(PPM, 0, false);
		sourceRA = new QuantityEditWidget<>(PPM, 0, false);

		dilutionCa = new QuantityEditWidget<>(PPM, 0, false);
		dilutionMg = new QuantityEditWidget<>(PPM, 0, false);
		dilutionNa = new QuantityEditWidget<>(PPM, 0, false);
		dilutionSO4 = new QuantityEditWidget<>(PPM, 0, false);
		dilutionCl = new QuantityEditWidget<>(PPM, 0, false);
		dilutionHCO3 = new QuantityEditWidget<>(PPM, 0, false);
		dilutionPh = new QuantityEditWidget<>(Quantity.Unit.PH, 0);
		dilutionAlk = new QuantityEditWidget<>(PPM, 0, false);
		dilutionRA = new QuantityEditWidget<>(PPM, 0, false);

		targetMinCa = new QuantityEditWidget<>(PPM, 0, false);
		targetMinMg = new QuantityEditWidget<>(PPM, 0, false);
		targetMinNa = new QuantityEditWidget<>(PPM, 0, false);
		targetMinSO4 = new QuantityEditWidget<>(PPM, 0, false);
		targetMinCl = new QuantityEditWidget<>(PPM, 0, false);
		targetMinHCO3 = new QuantityEditWidget<>(PPM, 0, false);
		targetMinAlk = new QuantityEditWidget<>(PPM, 0, false);
		targetMinRA = new QuantityEditWidget<>(PPM, 0, false);

		targetMaxCa = new QuantityEditWidget<>(PPM, 0, false);
		targetMaxMg = new QuantityEditWidget<>(PPM, 0, false);
		targetMaxNa = new QuantityEditWidget<>(PPM, 0, false);
		targetMaxSO4 = new QuantityEditWidget<>(PPM, 0, false);
		targetMaxCl = new QuantityEditWidget<>(PPM, 0, false);
		targetMaxHCO3 = new QuantityEditWidget<>(PPM, 0, false);
		targetMaxAlk = new QuantityEditWidget<>(PPM, 0, false);
		targetMaxRA = new QuantityEditWidget<>(PPM, 0, false);

		resultCa = new QuantityEditWidget<>(PPM, 0, false);
		resultMg = new QuantityEditWidget<>(PPM, 0, false);
		resultNa = new QuantityEditWidget<>(PPM, 0, false);
		resultSO4 = new QuantityEditWidget<>(PPM, 0, false);
		resultCl = new QuantityEditWidget<>(PPM, 0, false);
		resultHCO3 = new QuantityEditWidget<>(PPM, 0, false);
		resultAlk = new QuantityEditWidget<>(PPM, 0, false);
		resultRA = new QuantityEditWidget<>(PPM, 0, false);
		resultCa.setDisable(true);
		resultMg.setDisable(true);
		resultNa.setDisable(true);
		resultSO4.setDisable(true);
		resultCl.setDisable(true);
		resultHCO3.setDisable(true);
		resultAlk.setDisable(true);
		resultRA.setDisable(true);

		mse = new TextField();
		mse.setAlignment(Pos.CENTER);
//		mse.setPrefWidth(100);

		deltaCa = new QuantityEditWidget<>(PPM, 0, false);
		deltaMg = new QuantityEditWidget<>(PPM, 0, false);
		deltaNa = new QuantityEditWidget<>(PPM, 0, false);
		deltaSO4 = new QuantityEditWidget<>(PPM, 0, false);
		deltaCl = new QuantityEditWidget<>(PPM, 0, false);
		deltaHCO3 = new QuantityEditWidget<>(PPM, 0, false);
		deltaAlk = new QuantityEditWidget<>(PPM, 0, false);
		deltaRA = new QuantityEditWidget<>(PPM, 0, false);
		mse.setDisable(true);
		deltaCa.setDisable(true);
		deltaMg.setDisable(true);
		deltaNa.setDisable(true);
		deltaSO4.setDisable(true);
		deltaCl.setDisable(true);
		deltaHCO3.setDisable(true);
		deltaAlk.setDisable(true);
		deltaRA.setDisable(true);

		addCaCO3Undissolved = new QuantityEditWidget<>(Quantity.Unit.GRAMS, 0);
		addCaCO3Dissolved = new QuantityEditWidget<>(Quantity.Unit.GRAMS, 0);
		addCaSO4 = new QuantityEditWidget<>(Quantity.Unit.GRAMS, 0);
		addCaCl = new QuantityEditWidget<>(Quantity.Unit.GRAMS, 0);
		addMgSO4 = new QuantityEditWidget<>(Quantity.Unit.GRAMS, 0);
		addNaHCO3 = new QuantityEditWidget<>(Quantity.Unit.GRAMS, 0);
		addNaCl = new QuantityEditWidget<>(Quantity.Unit.GRAMS, 0);
		addCa_HCO3_2 = new QuantityEditWidget<>(Quantity.Unit.GRAMS, 0);
		addMgCl = new QuantityEditWidget<>(Quantity.Unit.GRAMS, 0);

		QuantityEditWidget<?>[] allowedQuantities = {addCaCO3Undissolved, addCaCO3Dissolved, addCaSO4,
			addCaCl, addMgSO4, addNaHCO3, addNaCl, addCa_HCO3_2, addMgCl,};

		goal = new ComboBox<>(FXCollections.observableArrayList(WaterBuilder.AdditionGoal.values()));
		goal.getSelectionModel().select(WaterBuilder.AdditionGoal.MAXIMISE_ADDITIONS);
		Button solve = new Button(
			StringUtils.getUiString("tools.water.builder.solve"),
			JfxUi.getImageView(Icons.graphIcon, 24));
//		Button bestFit = new Button(StringUtils.getUiString("tools.water.builder.best.fit"));

		MigPane waterSelections = new MigPane();
		MigPane waterPane = new MigPane();
		MigPane additions = new MigPane();
		MigPane buttons = new MigPane();

		// water selections

		waterSelections.add(new Label(StringUtils.getUiString("tools.water.builder.starting.water")));
		waterSelections.add(sourceWaterName/*, "wrap"*/);
		waterSelections.add(new Label(StringUtils.getUiString("tools.water.builder.dilution.water")));
		waterSelections.add(dilutionWaterName, "wrap");
		waterSelections.add(new Label(StringUtils.getUiString("tools.water.builder.target.water")));
		waterSelections.add(targetWaterName, "wrap");

		// waters

		waterPane.add(new Label());
		waterPane.add(new Label(StringUtils.getUiString("tools.water.builder.water.volume")));
		waterPane.add(new Label(StringUtils.getUiString("water.ph")));
		waterPane.add(new Label(StringUtils.getUiString("water.calcium.ppm")));
		waterPane.add(new Label(StringUtils.getUiString("water.magnesium.ppm")));
		waterPane.add(new Label(StringUtils.getUiString("water.sodium.ppm")));
		waterPane.add(new Label(StringUtils.getUiString("water.sulfate.ppm")));
		waterPane.add(new Label(StringUtils.getUiString("water.chloride.ppm")));
		waterPane.add(new Label(StringUtils.getUiString("water.bicarbonate.ppm")));
		waterPane.add(new Label(StringUtils.getUiString("water.alkalinity.abbr")));
		waterPane.add(new Label(StringUtils.getUiString("water.ra.abbr")), "wrap");

		// source water profile
		waterPane.add(new Label(StringUtils.getUiString("tools.water.builder.starting.water")));
		waterPane.add(sourceVol);
		waterPane.add(sourcePh);
		waterPane.add(sourceCa);
		waterPane.add(sourceMg);
		waterPane.add(sourceNa);
		waterPane.add(sourceSO4);
		waterPane.add(sourceCl);
		waterPane.add(sourceHCO3);
		waterPane.add(sourceAlk);
		waterPane.add(sourceRA, "wrap");


		// dilution water profile
		waterPane.add(new Label(StringUtils.getUiString("tools.water.builder.dilution.water")));
		waterPane.add(dilutionVol);
		waterPane.add(dilutionPh);
		waterPane.add(dilutionCa);
		waterPane.add(dilutionMg);
		waterPane.add(dilutionNa);
		waterPane.add(dilutionSO4);
		waterPane.add(dilutionCl);
		waterPane.add(dilutionHCO3);
		waterPane.add(dilutionAlk);
		waterPane.add(dilutionRA, "wrap");

		// target water profile
		waterPane.add(new Label(StringUtils.getUiString("tools.water.builder.target.water")));
		waterPane.add(new Label());
		waterPane.add(new Label(StringUtils.getUiString("tools.water.builder.min")));
		waterPane.add(targetMinCa);
		waterPane.add(targetMinMg);
		waterPane.add(targetMinNa);
		waterPane.add(targetMinSO4);
		waterPane.add(targetMinCl);
		waterPane.add(targetMinHCO3);
		waterPane.add(targetMinAlk);
		waterPane.add(targetMinRA, "wrap");

		waterPane.add(new Label());
		waterPane.add(new Label());
		waterPane.add(new Label(StringUtils.getUiString("tools.water.builder.max")));
		waterPane.add(targetMaxCa);
		waterPane.add(targetMaxMg);
		waterPane.add(targetMaxNa);
		waterPane.add(targetMaxSO4);
		waterPane.add(targetMaxCl);
		waterPane.add(targetMaxHCO3);
		waterPane.add(targetMaxAlk);
		waterPane.add(targetMaxRA, "wrap");

		// result water profile
		waterPane.add(new Label(StringUtils.getUiString("tools.water.builder.resulting.water")));
		waterPane.add(targetVol);
		waterPane.add(new Label());
		waterPane.add(resultCa);
		waterPane.add(resultMg);
		waterPane.add(resultNa);
		waterPane.add(resultSO4);
		waterPane.add(resultCl);
		waterPane.add(resultHCO3);
		waterPane.add(resultAlk);
		waterPane.add(resultRA, "wrap");

		// deltas
		waterPane.add(new Label(StringUtils.getUiString("tools.water.builder.deltas")));
		waterPane.add(mse, "span 2");
		waterPane.add(deltaCa);
		waterPane.add(deltaMg);
		waterPane.add(deltaNa);
		waterPane.add(deltaSO4);
		waterPane.add(deltaCl);
		waterPane.add(deltaHCO3);
		waterPane.add(deltaAlk);
		waterPane.add(deltaRA, "wrap");

		// additions

		addCaCO3UndissolvedMisc = new ComboBox<>(getIngredientOptions(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_UNDISSOLVED));
		addCaCO3DissolvedMisc = new ComboBox<>(getIngredientOptions(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_DISSOLVED));
		addCaSO4Misc = new ComboBox<>(getIngredientOptions(Misc.WaterAdditionFormula.CALCIUM_SULPHATE_DIHYDRATE));
		addCaClMisc = new ComboBox<>(getIngredientOptions(Misc.WaterAdditionFormula.CALCIUM_CHLORIDE_DIHYDRATE));
		addMgSO4Misc = new ComboBox<>(getIngredientOptions(Misc.WaterAdditionFormula.MAGNESIUM_SULFATE_HEPTAHYDRATE));
		addNaClMisc = new ComboBox<>(getIngredientOptions(Misc.WaterAdditionFormula.SODIUM_CHLORIDE));
		addNaHCO3Misc = new ComboBox<>(getIngredientOptions(Misc.WaterAdditionFormula.SODIUM_BICARBONATE));
		addCa_HCO3_2Misc = new ComboBox<>(getIngredientOptions(Misc.WaterAdditionFormula.CALCIUM_BICARBONATE));
		addMgClMisc = new ComboBox<>(getIngredientOptions(Misc.WaterAdditionFormula.MAGNESIUM_CHLORIDE_HEXAHYDRATE));

		checkAdditionAvailability(addCaCO3UndissolvedMisc, addCaCO3Dissolved, allowed[0]);
		checkAdditionAvailability(addCaCO3DissolvedMisc, addCaCO3Dissolved, allowed[1]);
		checkAdditionAvailability(addCaSO4Misc, addCaSO4, allowed[2]);
		checkAdditionAvailability(addCaClMisc, addCaCl, allowed[3]);
		checkAdditionAvailability(addMgSO4Misc, addMgSO4, allowed[4]);
		checkAdditionAvailability(addNaClMisc, addNaCl, allowed[5]);
		checkAdditionAvailability(addNaHCO3Misc, addNaHCO3, allowed[6]);
		checkAdditionAvailability(addCa_HCO3_2Misc, addCa_HCO3_2, allowed[7]);
		checkAdditionAvailability(addMgClMisc, addMgCl, allowed[8]);

		// decreases pH

		additions.add(new Label(StringUtils.getUiString("tools.water.builder.dec.ph")));
		additions.add(allowed[3]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.CALCIUM_CHLORIDE_DIHYDRATE")));
		additions.add(addCaCl);
		additions.add(addCaClMisc, "wrap");

		additions.add(new Label(StringUtils.getUiString("tools.water.builder.dec.ph")));
		additions.add(allowed[2]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.CALCIUM_SULPHATE_DIHYDRATE")));
		additions.add(addCaSO4);
		additions.add(addCaSO4Misc, "wrap");

		additions.add(new Label(StringUtils.getUiString("tools.water.builder.dec.ph")));
		additions.add(allowed[8]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.MAGNESIUM_CHLORIDE_HEXAHYDRATE")));
		additions.add(addMgCl);
		additions.add(addMgClMisc, "wrap");

		additions.add(new Label(StringUtils.getUiString("tools.water.builder.dec.ph")));
		additions.add(allowed[4]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.MAGNESIUM_SULFATE_HEPTAHYDRATE")));
		additions.add(addMgSO4);
		additions.add(addMgSO4Misc, "wrap");

		// pH neutral

		additions.add(new Label(StringUtils.getUiString("tools.water.builder.ph.neutral")));
		additions.add(allowed[6]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.SODIUM_CHLORIDE")));
		additions.add(addNaCl);
		additions.add(addNaClMisc, "wrap");

		// increases pH

		additions.add(new Label(StringUtils.getUiString("tools.water.builder.inc.ph")));
		additions.add(allowed[5]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.SODIUM_BICARBONATE")));
		additions.add(addNaHCO3);
		additions.add(addNaHCO3Misc, "wrap");

		additions.add(new Label(StringUtils.getUiString("tools.water.builder.inc.ph")));
		additions.add(allowed[0]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.CALCIUM_CARBONATE_UNDISSOLVED")));
		additions.add(addCaCO3Undissolved);
		additions.add(this.addCaCO3UndissolvedMisc, "wrap");

		additions.add(new Label(StringUtils.getUiString("tools.water.builder.inc.ph")));
		additions.add(allowed[1]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.CALCIUM_CARBONATE_DISSOLVED")));
		additions.add(addCaCO3Dissolved);
		additions.add(addCaCO3DissolvedMisc, "wrap");

		additions.add(new Label(StringUtils.getUiString("tools.water.builder.inc.ph")));
		additions.add(allowed[7]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.CALCIUM_BICARBONATE")));
		additions.add(addCa_HCO3_2);
		additions.add(addCa_HCO3_2Misc, "wrap");

		// buttons

		buttons.add(goal);
		buttons.add(solve);

		// messages

		MigPane messages = new MigPane();
		Label message = new Label();
		messages.add(message);

		this.add(waterSelections, "wrap");
		this.add(waterPane, "wrap");
		this.add(additions, "wrap");
		this.add(buttons, "wrap");
		this.add(messages, "wrap");

		// --------

		sourceVol.addListener((observableValue, oldValue, newValue) ->
		{
			VolumeUnit quantity = sourceVol.getQuantity().add(dilutionVol.getQuantity());
			targetVol.refresh(quantity);
		});

		dilutionVol.addListener((observableValue, oldValue, newValue) ->
		{
			VolumeUnit quantity = sourceVol.getQuantity().add(dilutionVol.getQuantity());
			targetVol.refresh(quantity);
		});

		solve.setOnAction(actionEvent ->
		{
			solve(allowed, message);
		});

		for (int i = 0; i < allowed.length; i++)
		{
			final int index = i;

			allowed[i].setOnAction(actionEvent ->
			{
				allowedQuantities[index].setDisable(!allowed[index].isSelected());

				if (!allowed[index].isSelected())
				{
					allowedQuantities[index].refresh(0);
				}
			});
		}

		for (int i = 0; i < allowedQuantities.length; i++)
		{
			allowedQuantities[i].addListener((observableValue, oldValue, newValue) ->
			{
				refreshAdditionsFromAdditionWidgets(getStartingWater(), getTargetWater());
			});
		}

		sourceWaterName.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) ->
			{
				if (newValue != null && !unspecifiedWater.equals(newValue) && !newValue.equals(oldValue))
				{
					Water water = Database.getInstance().getWaters().get(newValue);

					sourcePh.refresh(water.getPh());
					sourceCa.refresh(water.getCalcium());
					sourceMg.refresh(water.getMagnesium());
					sourceNa.refresh(water.getSodium());
					sourceSO4.refresh(water.getSulfate());
					sourceCl.refresh(water.getChloride());
					sourceHCO3.refresh(water.getBicarbonate());
					sourceAlk.refresh(water.getAlkalinity());
					sourceRA.refresh(water.getResidualAlkalinity());
				}
			});
		dilutionWaterName.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) ->
			{
				if (newValue != null && !unspecifiedWater.equals(newValue) && !newValue.equals(oldValue))
				{
					Water water = Database.getInstance().getWaters().get(newValue);

					dilutionPh.refresh(water.getPh());
					dilutionCa.refresh(water.getCalcium());
					dilutionMg.refresh(water.getMagnesium());
					dilutionNa.refresh(water.getSodium());
					dilutionSO4.refresh(water.getSulfate());
					dilutionCl.refresh(water.getChloride());
					dilutionHCO3.refresh(water.getBicarbonate());
					dilutionAlk.refresh(water.getAlkalinity());
					dilutionRA.refresh(water.getResidualAlkalinity());
				}
			});
		targetWaterName.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) ->
			{
				if (newValue != null && !unspecifiedWater.equals(newValue) && !newValue.equals(oldValue))
				{
					WaterParameters water = Database.getInstance().getWaterParameters().get(newValue);

					targetMinCa.refresh(water.getMinCalcium());
					targetMinMg.refresh(water.getMinMagnesium());
					targetMinNa.refresh(water.getMinSodium());
					targetMinSO4.refresh(water.getMinSulfate());
					targetMinCl.refresh(water.getMinChloride());
					targetMinHCO3.refresh(water.getMinBicarbonate());
					targetMinAlk.refresh(water.getMinAlkalinity());
					targetMinRA.refresh(water.getMinResidualAlkalinity());

					targetMaxCa.refresh(water.getMaxCalcium());
					targetMaxMg.refresh(water.getMaxMagnesium());
					targetMaxNa.refresh(water.getMaxSodium());
					targetMaxSO4.refresh(water.getMaxSulfate());
					targetMaxCl.refresh(water.getMaxChloride());
					targetMaxHCO3.refresh(water.getMaxBicarbonate());
					targetMaxAlk.refresh(water.getMaxAlkalinity());
					targetMaxRA.refresh(water.getMaxResidualAlkalinity());
				}
			});
	}

	/*-------------------------------------------------------------------------*/

	protected void checkAdditionAvailability(ComboBox<String> combo,
		QuantityEditWidget<WeightUnit> qew,
		CheckBox allowed)
	{
		if (combo.getItems().isEmpty())
		{
			allowed.setDisable(true);
			allowed.setSelected(false);
			combo.setDisable(true);
			qew.setDisable(true);
		}
		else
		{
			combo.getSelectionModel().select(0);
		}
	}

	/*-------------------------------------------------------------------------*/
	private ObservableList<String> getIngredientOptions(
		Misc.WaterAdditionFormula formula)
	{
		List<String> options = new ArrayList<>();

		for (Misc m : Database.getInstance().getMiscs().values())
		{
			if (m.getWaterAdditionFormula() == formula)
			{
				options.add(m.getName());
			}
		}

		options.sort(Comparator.comparing(String::toString));

		return FXCollections.observableList(options);
	}

	/*-------------------------------------------------------------------------*/
	public void init(List<WaterAddition> waterAdditions)
	{
		Water startingWater, dilutionWater;
		VolumeUnit startingVolume, dilutionVolume;

		if (waterAdditions.size() == 1)
		{
			startingWater = waterAdditions.get(0).getWater();
			startingVolume = waterAdditions.get(0).getVolume();
			dilutionWater = null;
			dilutionVolume = null;

			sourceWaterName.getSelectionModel().select(waterAdditions.get(0).getWater().getName());
			dilutionWaterName.getSelectionModel().select(unspecifiedWater);
		}
		else if (waterAdditions.size() == 2)
		{
			startingWater = waterAdditions.get(0).getWater();
			startingVolume = waterAdditions.get(0).getVolume();
			dilutionWater = waterAdditions.get(1).getWater();
			dilutionVolume = waterAdditions.get(1).getVolume();

			sourceWaterName.getSelectionModel().select(waterAdditions.get(0).getWater().getName());
			dilutionWaterName.getSelectionModel().select(waterAdditions.get(1).getWater().getName());
		}
		else
		{
			// bundle them all into the starting water

			sourceWaterName.getSelectionModel().select(unspecifiedWater);
			dilutionWaterName.getSelectionModel().select(unspecifiedWater);
			dilutionWater = null;
			dilutionVolume = null;

			startingWater = new Water();
			startingWater.setCalcium(new PpmUnit(0));
			startingWater.setMagnesium(new PpmUnit(0));
			startingWater.setSodium(new PpmUnit(0));
			startingWater.setSulfate(new PpmUnit(0));
			startingWater.setChloride(new PpmUnit(0));
			startingWater.setBicarbonate(new PpmUnit(0));
			startingVolume = new VolumeUnit(0);

			for (WaterAddition wa : waterAdditions)
			{
				startingWater = Equations.calcCombinedWaterProfile(
					startingWater,
					startingVolume,
					wa.getWater(),
					wa.getVolume());

				startingVolume = startingVolume.add(wa.getVolume());
			}
		}

		sourceCa.refresh(startingWater.getCalcium());
		sourceMg.refresh(startingWater.getMagnesium());
		sourceNa.refresh(startingWater.getSodium());
		sourceSO4.refresh(startingWater.getSulfate());
		sourceCl.refresh(startingWater.getChloride());
		sourceHCO3.refresh(startingWater.getBicarbonate());
		sourceVol.refresh(startingVolume);

		if (dilutionWater != null)
		{
			dilutionCa.refresh(dilutionWater.getCalcium());
			dilutionMg.refresh(dilutionWater.getMagnesium());
			dilutionNa.refresh(dilutionWater.getSodium());
			dilutionSO4.refresh(dilutionWater.getSulfate());
			dilutionCl.refresh(dilutionWater.getChloride());
			dilutionHCO3.refresh(dilutionWater.getBicarbonate());
			dilutionVol.refresh(dilutionVolume);
		}
	}

	/*-------------------------------------------------------------------------*/
	public List<MiscAddition> getAdditions()
	{
		List<MiscAddition> result = new ArrayList<>();

		addAddition(result, this.addCaCO3Undissolved, this.addCaCO3UndissolvedMisc);
		addAddition(result, this.addCaCO3Dissolved, this.addCaCO3DissolvedMisc);
		addAddition(result, this.addCaSO4, this.addCaSO4Misc);
		addAddition(result, this.addCaCl, this.addCaClMisc);
		addAddition(result, this.addMgSO4, this.addMgSO4Misc);
		addAddition(result, this.addNaHCO3, this.addNaHCO3Misc);
		addAddition(result, this.addNaCl, this.addNaClMisc);
		addAddition(result, this.addCa_HCO3_2, this.addCa_HCO3_2Misc);
		addAddition(result, this.addMgCl, this.addMgClMisc);

		return result;
	}

	/*-------------------------------------------------------------------------*/
	protected void addAddition(List<MiscAddition> result,
		QuantityEditWidget<WeightUnit> qew, ComboBox<String> combo)
	{
		if (qew.getQuantity().get() > 0)
		{
			Misc misc = Database.getInstance().getMiscs().get(combo.getSelectionModel().getSelectedItem());
			result.add(new MiscAddition(misc, qew.getQuantity(), qew.getUnit(), new TimeUnit(0)));
		}
	}

	/*-------------------------------------------------------------------------*/
	private WaterParameters getTargetWater()
	{
		WaterParameters targetWater = new WaterParameters();

		targetWater.setMinCalcium(targetMinCa.getQuantity());
		targetWater.setMinMagnesium(targetMinMg.getQuantity());
		targetWater.setMinSodium(targetMinNa.getQuantity());
		targetWater.setMinSulfate(targetMinSO4.getQuantity());
		targetWater.setMinChloride(targetMinCl.getQuantity());
		targetWater.setMinBicarbonate(targetMinHCO3.getQuantity());
		targetWater.setMinAlkalinity(targetMinAlk.getQuantity());
		targetWater.setMinResidualAlkalinity(targetMinRA.getQuantity());

		targetWater.setMaxCalcium(targetMaxCa.getQuantity());
		targetWater.setMaxMagnesium(targetMaxMg.getQuantity());
		targetWater.setMaxSodium(targetMaxNa.getQuantity());
		targetWater.setMaxSulfate(targetMaxSO4.getQuantity());
		targetWater.setMaxChloride(targetMaxCl.getQuantity());
		targetWater.setMaxBicarbonate(targetMaxHCO3.getQuantity());
		targetWater.setMaxAlkalinity(targetMaxAlk.getQuantity());
		targetWater.setMaxResidualAlkalinity(targetMaxRA.getQuantity());

		return targetWater;
	}

	/*-------------------------------------------------------------------------*/
	private Water getStartingWater()
	{
		Water startingWater = new Water();
		startingWater.setPh(sourcePh.getQuantity());
		startingWater.setCalcium(sourceCa.getQuantity());
		startingWater.setMagnesium(sourceMg.getQuantity());
		startingWater.setSodium(sourceNa.getQuantity());
		startingWater.setSulfate(sourceSO4.getQuantity());
		startingWater.setChloride(sourceCl.getQuantity());
		startingWater.setBicarbonate(sourceHCO3.getQuantity());

		Water dilutionWater = new Water();
		dilutionWater.setPh(dilutionPh.getQuantity());
		dilutionWater.setCalcium(dilutionCa.getQuantity());
		dilutionWater.setMagnesium(dilutionMg.getQuantity());
		dilutionWater.setSodium(dilutionNa.getQuantity());
		dilutionWater.setSulfate(dilutionSO4.getQuantity());
		dilutionWater.setChloride(dilutionCl.getQuantity());
		dilutionWater.setBicarbonate(dilutionHCO3.getQuantity());

		// do any dilution
		if (dilutionVol.getQuantity().get() > 0)
		{
			startingWater = Equations.calcCombinedWaterProfile(
				startingWater, sourceVol.getQuantity(),
				dilutionWater, dilutionVol.getQuantity());
		}
		return startingWater;
	}

	/*-------------------------------------------------------------------------*/
	private void solve(CheckBox[] allowed, Label message)
	{
		WaterBuilder wb = new WaterBuilder();

		Water startingWater = getStartingWater();
		WaterParameters targetWater = getTargetWater();
		HashMap<Misc.WaterAdditionFormula, Boolean> allowedAdditions = getAllowedAdditions(allowed);

		Map<Misc.WaterAdditionFormula, Double> result =
			wb.calcAdditions(startingWater, targetWater, allowedAdditions, goal.getValue());

		if (result == null)
		{
			message.setText(StringUtils.getUiString("tools.water.builder.no.solution"));
		}
		else
		{
			message.setText(StringUtils.getUiString("tools.water.builder.found.a.solution"));

			refreshAdditionsFromSolving(startingWater, targetWater, result);
		}
	}

	/*-------------------------------------------------------------------------*/
	private HashMap<Misc.WaterAdditionFormula, Boolean> getAllowedAdditions(
		CheckBox[] allowed)
	{
		WaterBuilder wb = new WaterBuilder();

		HashMap<Misc.WaterAdditionFormula, Boolean> allowedAdditions = new HashMap<>();
		allowedAdditions.put(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_UNDISSOLVED, allowed[0].isSelected());
		allowedAdditions.put(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_DISSOLVED, allowed[1].isSelected());
		allowedAdditions.put(Misc.WaterAdditionFormula.CALCIUM_SULPHATE_DIHYDRATE, allowed[2].isSelected());
		allowedAdditions.put(Misc.WaterAdditionFormula.CALCIUM_CHLORIDE_DIHYDRATE, allowed[3].isSelected());
		allowedAdditions.put(Misc.WaterAdditionFormula.MAGNESIUM_SULFATE_HEPTAHYDRATE, allowed[4].isSelected());
		allowedAdditions.put(Misc.WaterAdditionFormula.SODIUM_BICARBONATE, allowed[5].isSelected());
		allowedAdditions.put(Misc.WaterAdditionFormula.SODIUM_CHLORIDE, allowed[6].isSelected());
		allowedAdditions.put(Misc.WaterAdditionFormula.CALCIUM_BICARBONATE, allowed[7].isSelected());
		allowedAdditions.put(Misc.WaterAdditionFormula.MAGNESIUM_CHLORIDE_HEXAHYDRATE, allowed[8].isSelected());
		return allowedAdditions;
	}

	/*-------------------------------------------------------------------------*/
	private void refreshAdditionsFromSolving(
		Water startingWater,
		WaterParameters targetWater,
		Map<Misc.WaterAdditionFormula, Double> result)
	{
		double vol = targetVol.getQuantity().get(Quantity.Unit.LITRES);

		double caCo3UnG = result.get(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_UNDISSOLVED) * vol / 1000D;
		double caCo3DisG = result.get(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_DISSOLVED) * vol / 1000D;
		double caSo4G = result.get(Misc.WaterAdditionFormula.CALCIUM_SULPHATE_DIHYDRATE) * vol / 1000D;
		double caClG = result.get(Misc.WaterAdditionFormula.CALCIUM_CHLORIDE_DIHYDRATE) * vol / 1000D;
		double mgSo4G = result.get(Misc.WaterAdditionFormula.MAGNESIUM_SULFATE_HEPTAHYDRATE) * vol / 1000D;
		double naHco3G = result.get(Misc.WaterAdditionFormula.SODIUM_BICARBONATE) * vol / 1000D;
		double naClG = result.get(Misc.WaterAdditionFormula.SODIUM_CHLORIDE) * vol / 1000D;
		double caHCO3G = result.get(Misc.WaterAdditionFormula.CALCIUM_BICARBONATE) * vol / 1000D;
		double mgClG = result.get(Misc.WaterAdditionFormula.MAGNESIUM_CHLORIDE_HEXAHYDRATE) * vol / 1000D;

		addCaCO3Undissolved.refresh(caCo3UnG);
		addCaCO3Dissolved.refresh(caCo3DisG);
		addCaSO4.refresh(caSo4G);
		addCaCl.refresh(caClG);
		addMgSO4.refresh(mgSo4G);
		addNaHCO3.refresh(naHco3G);
		addNaCl.refresh(naClG);
		addCa_HCO3_2.refresh(caHCO3G);
		addMgCl.refresh(mgClG);

		refreshResult(startingWater, targetWater, caCo3UnG, caCo3DisG, caSo4G, caClG, mgSo4G, naHco3G, naClG, caHCO3G, mgClG);
	}

	/*-------------------------------------------------------------------------*/
	private void refreshAdditionsFromAdditionWidgets(
		Water startingWater,
		WaterParameters targetWater)
	{
		double vol = targetVol.getQuantity().get(Quantity.Unit.LITRES);

		double caCo3UnG = addCaCO3Undissolved.getQuantity().get(Quantity.Unit.GRAMS);
		double caCo3DisG = addCaCO3Dissolved.getQuantity().get(Quantity.Unit.GRAMS);
		double caSo4G = addCaSO4.getQuantity().get(Quantity.Unit.GRAMS);
		double caClG = addCaCl.getQuantity().get(Quantity.Unit.GRAMS);
		double mgSo4G = addMgSO4.getQuantity().get(Quantity.Unit.GRAMS);
		double naHco3G = addNaHCO3.getQuantity().get(Quantity.Unit.GRAMS);
		double naClG = addNaCl.getQuantity().get(Quantity.Unit.GRAMS);
		double caHCO3G = addCa_HCO3_2.getQuantity().get(Quantity.Unit.GRAMS);
		double mgClG = addMgCl.getQuantity().get(Quantity.Unit.GRAMS);

		refreshResult(startingWater, targetWater, caCo3UnG, caCo3DisG, caSo4G, caClG, mgSo4G, naHco3G, naClG, caHCO3G, mgClG);
	}

	/*-------------------------------------------------------------------------*/
	private void refreshResult(
		Water startingWater,
		WaterParameters targetWater,
		double caCo3UnG, double caCo3DisG, double caSo4G, double caClG,
		double mgSo4G, double naHco3G, double naClG, double caHCO3G, double mgClG)
	{
		WaterBuilder wb = new WaterBuilder();

		Water w = wb.buildWater(
			startingWater,
			targetVol.getQuantity(),
			caCo3UnG,
			caCo3DisG,
			caSo4G,
			caClG,
			mgSo4G,
			naHco3G,
			naClG,
			caHCO3G,
			mgClG);

		resultCa.refresh(w.getCalcium());
		resultMg.refresh(w.getMagnesium());
		resultNa.refresh(w.getSodium());
		resultSO4.refresh(w.getSulfate());
		resultCl.refresh(w.getChloride());
		resultHCO3.refresh(w.getBicarbonate());
		resultAlk.refresh(w.getAlkalinity());
		resultRA.refresh(w.getResidualAlkalinity());

		double dca;
		double dmg;
		double dna;
		double dso4;
		double dcl;
		double dhco3;
		double dalk;
		double dra;

		dca = getDelta(w.getCalcium().get(PPM), targetWater.getMinCalcium().get(PPM), targetWater.getMaxCalcium().get(PPM));
		dmg = getDelta(w.getMagnesium().get(PPM), targetWater.getMinMagnesium().get(PPM), targetWater.getMaxMagnesium().get(PPM));
		dna = getDelta(w.getSodium().get(PPM), targetWater.getMinSodium().get(PPM), targetWater.getMaxSodium().get(PPM));
		dso4 = getDelta(w.getSulfate().get(PPM), targetWater.getMinSulfate().get(PPM), targetWater.getMaxSulfate().get(PPM));
		dcl = getDelta(w.getChloride().get(PPM), targetWater.getMinChloride().get(PPM), targetWater.getMaxChloride().get(PPM));
		dhco3 = getDelta(w.getBicarbonate().get(PPM), targetWater.getMinBicarbonate().get(PPM), targetWater.getMaxBicarbonate().get(PPM));
		dalk = getDelta(w.getAlkalinity().get(PPM), targetWater.getMinAlkalinity().get(PPM), targetWater.getMaxAlkalinity().get(PPM));
		dra = getDelta(w.getResidualAlkalinity().get(PPM), targetWater.getMinResidualAlkalinity().get(PPM), targetWater.getMaxResidualAlkalinity().get(PPM));

		deltaCa.refresh(dca);
		deltaMg.refresh(dmg);
		deltaNa.refresh(dna);
		deltaSO4.refresh(dso4);
		deltaCl.refresh(dcl);
		deltaHCO3.refresh(dhco3);
		deltaAlk.refresh(dalk);
		deltaRA.refresh(dra);

		double error = Math.pow(dca, 2) + Math.pow(dmg, 2) + Math.pow(dna, 2) + Math.pow(dso4, 2) + Math.pow(dcl, 2) + Math.pow(dhco3, 2);

		error = error/6D;

		mse.setText(String.format(StringUtils.getUiString("tools.water.builder.mse"), error));
	}

	/*-------------------------------------------------------------------------*/
	private double getDelta(double current, double min, double max)
	{
		if (current < min)
		{
			return current - min;
		}
		else if (current > max)
		{
			return max - current;
		}
		else
		{
			return 0;
		}
	}
}
