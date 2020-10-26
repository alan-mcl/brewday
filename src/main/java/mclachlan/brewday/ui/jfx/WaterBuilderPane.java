
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
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.WaterAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class WaterBuilderPane extends MigPane
{
	private final QuantityEditWidget<VolumeUnit> sourceVol;
	private final QuantityEditWidget<VolumeUnit> dilutionVol;
	private final QuantityEditWidget<VolumeUnit> targetVol;
	private final QuantityEditWidget<PpmUnit> sourceCa;
	private final QuantityEditWidget<PpmUnit> sourceMg;
	private final QuantityEditWidget<PpmUnit> sourceNa;
	private final QuantityEditWidget<PpmUnit> sourceSO4;
	private final QuantityEditWidget<PpmUnit> sourceCl;
	private final QuantityEditWidget<PpmUnit> sourceHCO3;
	private final QuantityEditWidget<PpmUnit> dilutionCa;
	private final QuantityEditWidget<PpmUnit> dilutionMg;
	private final QuantityEditWidget<PpmUnit> dilutionNa;
	private final QuantityEditWidget<PpmUnit> dilutionSO4;
	private final QuantityEditWidget<PpmUnit> dilutionCl;
	private final QuantityEditWidget<PpmUnit> dilutionHCO3;
	private final QuantityEditWidget<PpmUnit> targetCa;
	private final QuantityEditWidget<PpmUnit> targetMg;
	private final QuantityEditWidget<PpmUnit> targetNa;
	private final QuantityEditWidget<PpmUnit> targetSO4;
	private final QuantityEditWidget<PpmUnit> targetCl;
	private final QuantityEditWidget<PpmUnit> targetHCO3;
	private final QuantityEditWidget<PpmUnit> resultCa;
	private final QuantityEditWidget<PpmUnit> resultMg;
	private final QuantityEditWidget<PpmUnit> resultNa;
	private final QuantityEditWidget<PpmUnit> resultSO4;
	private final QuantityEditWidget<PpmUnit> resultCl;
	private final QuantityEditWidget<PpmUnit> resultHCO3;
	private final QuantityEditWidget<PpmUnit> deltaCa;
	private final QuantityEditWidget<PpmUnit> deltaMg;
	private final QuantityEditWidget<PpmUnit> deltaNa;
	private final QuantityEditWidget<PpmUnit> deltaSO4;
	private final QuantityEditWidget<PpmUnit> deltaCl;
	private final QuantityEditWidget<PpmUnit> deltaHCO3;
	private final TextField mse;
	private final ComboBox<WaterBuilder.Constraint> caConstraint;
	private final ComboBox<WaterBuilder.Constraint> mgConstraint;
	private final ComboBox<WaterBuilder.Constraint> naConstraint;
	private final ComboBox<WaterBuilder.Constraint> so4Constraint;
	private final ComboBox<WaterBuilder.Constraint> clConstraint;
	private final ComboBox<WaterBuilder.Constraint> hco3Constraint;
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

	public WaterBuilderPane()
	{
		CheckBox[] allowed = new CheckBox[9];
		for (int i = 0; i < allowed.length; i++)
		{
			allowed[i] = new CheckBox();
			allowed[i].setSelected(true);
		}

		sourceVol = new QuantityEditWidget<>(Quantity.Unit.LITRES, 0);
		dilutionVol = new QuantityEditWidget<>(Quantity.Unit.LITRES, 0);
		targetVol = new QuantityEditWidget<>(Quantity.Unit.LITRES, 0);
		targetVol.setDisable(true);

		sourceCa = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		sourceMg = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		sourceNa = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		sourceSO4 = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		sourceCl = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		sourceHCO3 = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);

		dilutionCa = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		dilutionMg = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		dilutionNa = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		dilutionSO4 = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		dilutionCl = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		dilutionHCO3 = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);

		targetCa = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		targetMg = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		targetNa = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		targetSO4 = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		targetCl = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		targetHCO3 = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);

		resultCa = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		resultMg = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		resultNa = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		resultSO4 = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		resultCl = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		resultHCO3 = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		resultCa.setDisable(true);
		resultMg.setDisable(true);
		resultNa.setDisable(true);
		resultSO4.setDisable(true);
		resultCl.setDisable(true);
		resultHCO3.setDisable(true);

		mse = new TextField();
		mse.setAlignment(Pos.CENTER);
		deltaCa = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		deltaMg = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		deltaNa = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		deltaSO4 = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		deltaCl = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		deltaHCO3 = new QuantityEditWidget<>(Quantity.Unit.PPM, 0);
		mse.setDisable(true);
		deltaCa.setDisable(true);
		deltaMg.setDisable(true);
		deltaNa.setDisable(true);
		deltaSO4.setDisable(true);
		deltaCl.setDisable(true);
		deltaHCO3.setDisable(true);

		caConstraint = new ComboBox<>(FXCollections.observableArrayList(WaterBuilder.Constraint.values()));
		mgConstraint = new ComboBox<>(FXCollections.observableArrayList(WaterBuilder.Constraint.values()));
		naConstraint = new ComboBox<>(FXCollections.observableArrayList(WaterBuilder.Constraint.values()));
		so4Constraint = new ComboBox<>(FXCollections.observableArrayList(WaterBuilder.Constraint.values()));
		clConstraint = new ComboBox<>(FXCollections.observableArrayList(WaterBuilder.Constraint.values()));
		hco3Constraint = new ComboBox<>(FXCollections.observableArrayList(WaterBuilder.Constraint.values()));

		caConstraint.getSelectionModel().select(WaterBuilder.Constraint.LEQ);
		mgConstraint.getSelectionModel().select(WaterBuilder.Constraint.LEQ);
		naConstraint.getSelectionModel().select(WaterBuilder.Constraint.LEQ);
		so4Constraint.getSelectionModel().select(WaterBuilder.Constraint.LEQ);
		clConstraint.getSelectionModel().select(WaterBuilder.Constraint.LEQ);
		hco3Constraint.getSelectionModel().select(WaterBuilder.Constraint.LEQ);

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

		MigPane waters = new MigPane();
		MigPane additions = new MigPane();
		MigPane buttons = new MigPane();

		// waters

		waters.add(new Label());
		waters.add(new Label(StringUtils.getUiString("tools.water.builder.water.volume")));
		waters.add(new Label(StringUtils.getUiString("water.calcium.abbr")));
		waters.add(new Label(StringUtils.getUiString("water.magnesium.abbr")));
		waters.add(new Label(StringUtils.getUiString("water.sodium.abbr")));
		waters.add(new Label(StringUtils.getUiString("water.sulfate.abbr")));
		waters.add(new Label(StringUtils.getUiString("water.chloride.abbr")));
		waters.add(new Label(StringUtils.getUiString("water.bicarbonate.abbr")), "wrap");

		// source water profile
		waters.add(new Label(StringUtils.getUiString("tools.water.builder.starting.water")));
		waters.add(sourceVol);
		waters.add(sourceCa);
		waters.add(sourceMg);
		waters.add(sourceNa);
		waters.add(sourceSO4);
		waters.add(sourceCl);
		waters.add(sourceHCO3, "wrap");

		// dilution water profile
		waters.add(new Label(StringUtils.getUiString("tools.water.builder.dilution.water")));
		waters.add(dilutionVol);
		waters.add(dilutionCa);
		waters.add(dilutionMg);
		waters.add(dilutionNa);
		waters.add(dilutionSO4);
		waters.add(dilutionCl);
		waters.add(dilutionHCO3, "wrap");

		// target water profile
		waters.add(new Label(StringUtils.getUiString("tools.water.builder.target.water")));
		waters.add(targetVol);
		waters.add(targetCa);
		waters.add(targetMg);
		waters.add(targetNa);
		waters.add(targetSO4);
		waters.add(targetCl);
		waters.add(targetHCO3, "wrap");

		waters.add(new Label(StringUtils.getUiString("tools.water.builder.target.water.constraints")));
		waters.add(new Label());
		waters.add(caConstraint);
		waters.add(mgConstraint);
		waters.add(naConstraint);
		waters.add(so4Constraint);
		waters.add(clConstraint);
		waters.add(hco3Constraint, "wrap");

		// result water profile
		waters.add(new Label(StringUtils.getUiString("tools.water.builder.resulting.water")));
		waters.add(new Label());
		waters.add(resultCa);
		waters.add(resultMg);
		waters.add(resultNa);
		waters.add(resultSO4);
		waters.add(resultCl);
		waters.add(resultHCO3, "wrap");

		// deltas
		waters.add(new Label(StringUtils.getUiString("tools.water.builder.mse")));
		waters.add(mse);
		waters.add(deltaCa);
		waters.add(deltaMg);
		waters.add(deltaNa);
		waters.add(deltaSO4);
		waters.add(deltaCl);
		waters.add(deltaHCO3, "wrap");

		// additions

		addCaCO3UndissolvedMisc = new ComboBox<String>(getIngredientOptions(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_UNDISSOLVED));
		addCaCO3DissolvedMisc = new ComboBox<String>(getIngredientOptions(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_DISSOLVED));
		addCaSO4Misc = new ComboBox<String>(getIngredientOptions(Misc.WaterAdditionFormula.CALCIUM_SULPHATE_DIHYDRATE));
		addCaClMisc = new ComboBox<String>(getIngredientOptions(Misc.WaterAdditionFormula.CALCIUM_CHLORIDE_DIHYDRATE));
		addMgSO4Misc = new ComboBox<String>(getIngredientOptions(Misc.WaterAdditionFormula.MAGNESIUM_SULFATE_HEPTAHYDRATE));
		addNaClMisc = new ComboBox<String>(getIngredientOptions(Misc.WaterAdditionFormula.SODIUM_CHLORIDE));
		addNaHCO3Misc = new ComboBox<String>(getIngredientOptions(Misc.WaterAdditionFormula.SODIUM_BICARBONATE));
		addCa_HCO3_2Misc = new ComboBox<String>(getIngredientOptions(Misc.WaterAdditionFormula.CALCIUM_BICARBONATE));
		addMgClMisc = new ComboBox<String>(getIngredientOptions(Misc.WaterAdditionFormula.MAGNESIUM_CHLORIDE_HEXAHYDRATE));

		checkAdditionAvailability(addCaCO3UndissolvedMisc, addCaCO3Dissolved, allowed[0]);
		checkAdditionAvailability(addCaCO3DissolvedMisc, addCaCO3Dissolved, allowed[1]);
		checkAdditionAvailability(addCaSO4Misc, addCaSO4, allowed[2]);
		checkAdditionAvailability(addCaClMisc, addCaCl, allowed[3]);
		checkAdditionAvailability(addMgSO4Misc, addMgSO4, allowed[4]);
		checkAdditionAvailability(addNaClMisc, addNaCl, allowed[5]);
		checkAdditionAvailability(addNaHCO3Misc, addNaHCO3, allowed[6]);
		checkAdditionAvailability(addCa_HCO3_2Misc, addCa_HCO3_2, allowed[7]);
		checkAdditionAvailability(addMgClMisc, addMgCl, allowed[8]);

		additions.add(allowed[0]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.CALCIUM_CARBONATE_UNDISSOLVED")));
		additions.add(addCaCO3Undissolved);
		additions.add(this.addCaCO3UndissolvedMisc, "wrap");
		additions.add(allowed[1]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.CALCIUM_CARBONATE_DISSOLVED")));
		additions.add(addCaCO3Dissolved);
		additions.add(addCaCO3DissolvedMisc, "wrap");
		additions.add(allowed[2]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.CALCIUM_SULPHATE_DIHYDRATE")));
		additions.add(addCaSO4);
		additions.add(addCaSO4Misc, "wrap");
		additions.add(allowed[3]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.CALCIUM_CHLORIDE_DIHYDRATE")));
		additions.add(addCaCl);
		additions.add(addCaClMisc, "wrap");
		additions.add(allowed[4]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.MAGNESIUM_SULFATE_HEPTAHYDRATE")));
		additions.add(addMgSO4);
		additions.add(addMgSO4Misc, "wrap");
		additions.add(allowed[5]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.SODIUM_BICARBONATE")));
		additions.add(addNaHCO3);
		additions.add(addNaHCO3Misc, "wrap");
		additions.add(allowed[6]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.SODIUM_CHLORIDE")));
		additions.add(addNaCl);
		additions.add(addNaClMisc, "wrap");
		additions.add(allowed[7]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.CALCIUM_BICARBONATE")));
		additions.add(addCa_HCO3_2);
		additions.add(addCa_HCO3_2Misc, "wrap");
		additions.add(allowed[8]);
		additions.add(new Label(StringUtils.getUiString("misc.water.addition.formula.MAGNESIUM_CHLORIDE_HEXAHYDRATE")));
		additions.add(addMgCl);
		additions.add(addMgClMisc, "wrap");

		// buttons

		goal = new ComboBox<>(FXCollections.observableArrayList(WaterBuilder.AdditionGoal.values()));
		goal.getSelectionModel().select(WaterBuilder.AdditionGoal.MAXIMISE);
		Button solve = new Button(StringUtils.getUiString("tools.water.builder.solve"));
		Button bestFit = new Button(StringUtils.getUiString("tools.water.builder.best.fit"));

		buttons.add(goal);
		buttons.add(solve);
//		buttons.add(bestFit); todo best fit LP

		// messages

		MigPane messages = new MigPane();
		Label message = new Label();
		messages.add(message);

		this.add(waters, "wrap");
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

		bestFit.setOnAction(actionEvent ->
		{
			bestFit(allowed, message);
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
				refreshAdditionsFromWidgets(getStartingWater(), getTargetWater());
			});
		}
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
	private ObservableList getIngredientOptions(
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
		// combine all the given water additions
		Water startingWater = new Water();
		startingWater.setCalcium(new PpmUnit(0));
		startingWater.setMagnesium(new PpmUnit(0));
		startingWater.setSodium(new PpmUnit(0));
		startingWater.setSulfate(new PpmUnit(0));
		startingWater.setChloride(new PpmUnit(0));
		startingWater.setBicarbonate(new PpmUnit(0));
		VolumeUnit startingVolume = new VolumeUnit(0);

		for (WaterAddition wa : waterAdditions)
		{
			startingWater = Equations.calcCombinedWaterProfile(
				startingWater,
				startingVolume,
				wa.getWater(),
				wa.getVolume());

			startingVolume = startingVolume.add(wa.getVolume());
		}

		sourceCa.refresh(startingWater.getCalcium());
		sourceMg.refresh(startingWater.getMagnesium());
		sourceNa.refresh(startingWater.getSodium());
		sourceSO4.refresh(startingWater.getSulfate());
		sourceCl.refresh(startingWater.getChloride());
		sourceHCO3.refresh(startingWater.getBicarbonate());

		sourceVol.refresh(startingVolume);
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
	private void bestFit(CheckBox[] allowed, Label message)
	{
		WaterBuilder wb = new WaterBuilder();

		Water startingWater = getStartingWater();
		Water targetWater = getTargetWater();
		HashMap<Misc.WaterAdditionFormula, Boolean> allowedAdditions = getAllowedAdditions(allowed);

		WaterBuilder.BestFitResults bestFitResults = wb.bestFit(startingWater, targetWater, allowedAdditions);
		Map<Misc.WaterAdditionFormula, Double> result = bestFitResults.getAdditions();

		if (result == null)
		{
			message.setText(StringUtils.getUiString("tools.water.builder.no.solution"));
		}
		else
		{
			message.setText(StringUtils.getUiString("tools.water.builder.found.a.solution"));

			refreshAdditionsFromResults(startingWater, targetWater, result);

			// set constraints and goal

			goal.getSelectionModel().select(bestFitResults.getAdditionGoal());

			caConstraint.getSelectionModel().select(bestFitResults.getTargetConstraints().get(Water.Component.CALCIUM));
			mgConstraint.getSelectionModel().select(bestFitResults.getTargetConstraints().get(Water.Component.MAGNESIUM));
			naConstraint.getSelectionModel().select(bestFitResults.getTargetConstraints().get(Water.Component.SODIUM));
			so4Constraint.getSelectionModel().select(bestFitResults.getTargetConstraints().get(Water.Component.SULFATE));
			clConstraint.getSelectionModel().select(bestFitResults.getTargetConstraints().get(Water.Component.CHLORIDE));
			hco3Constraint.getSelectionModel().select(bestFitResults.getTargetConstraints().get(Water.Component.BICARBONATE));
		}

	}

	/*-------------------------------------------------------------------------*/
	private Water getTargetWater()
	{
		Water targetWater = new Water();
		targetWater.setCalcium(targetCa.getQuantity());
		targetWater.setMagnesium(targetMg.getQuantity());
		targetWater.setSodium(targetNa.getQuantity());
		targetWater.setSulfate(targetSO4.getQuantity());
		targetWater.setChloride(targetCl.getQuantity());
		targetWater.setBicarbonate(targetHCO3.getQuantity());
		return targetWater;
	}

	/*-------------------------------------------------------------------------*/
	private Water getStartingWater()
	{
		Water startingWater = new Water();
		startingWater.setCalcium(sourceCa.getQuantity());
		startingWater.setMagnesium(sourceMg.getQuantity());
		startingWater.setSodium(sourceNa.getQuantity());
		startingWater.setSulfate(sourceSO4.getQuantity());
		startingWater.setChloride(sourceCl.getQuantity());
		startingWater.setBicarbonate(sourceHCO3.getQuantity());

		Water dilutionWater = new Water();
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
		Water targetWater = getTargetWater();
		HashMap<Misc.WaterAdditionFormula, Boolean> allowedAdditions = getAllowedAdditions(allowed);

		HashMap<Water.Component, WaterBuilder.Constraint> targetConstraints = new HashMap<>();
		targetConstraints.put(Water.Component.CALCIUM, caConstraint.getValue());
		targetConstraints.put(Water.Component.MAGNESIUM, mgConstraint.getValue());
		targetConstraints.put(Water.Component.SODIUM, naConstraint.getValue());
		targetConstraints.put(Water.Component.SULFATE, so4Constraint.getValue());
		targetConstraints.put(Water.Component.CHLORIDE, clConstraint.getValue());
		targetConstraints.put(Water.Component.BICARBONATE, hco3Constraint.getValue());


		Map<Misc.WaterAdditionFormula, Double> result =
			wb.calcAdditions(startingWater, targetWater, allowedAdditions, targetConstraints, goal.getValue());

		if (result == null)
		{
			message.setText(StringUtils.getUiString("tools.water.builder.no.solution"));
		}
		else
		{
			message.setText(StringUtils.getUiString("tools.water.builder.found.a.solution"));

			refreshAdditionsFromResults(startingWater, targetWater, result);
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
	private void refreshAdditionsFromResults(
		Water startingWater,
		Water targetWater,
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

		refreshAdditions(startingWater, targetWater, caCo3UnG, caCo3DisG, caSo4G, caClG, mgSo4G, naHco3G, naClG, caHCO3G, mgClG);
	}

	/*-------------------------------------------------------------------------*/
	private void refreshAdditionsFromWidgets(
		Water startingWater,
		Water targetWater)
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

		refreshAdditions(startingWater, targetWater, caCo3UnG, caCo3DisG, caSo4G, caClG, mgSo4G, naHco3G, naClG, caHCO3G, mgClG);
	}

	/*-------------------------------------------------------------------------*/
	private void refreshAdditions(
		Water startingWater,
		Water targetWater,
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

		double dca = w.getCalcium().get(Quantity.Unit.PPM) - targetWater.getCalcium().get(Quantity.Unit.PPM);
		double dmg = w.getMagnesium().get(Quantity.Unit.PPM) - targetWater.getMagnesium().get(Quantity.Unit.PPM);
		double dna = w.getSodium().get(Quantity.Unit.PPM) - targetWater.getSodium().get(Quantity.Unit.PPM);
		double dso4 = w.getSulfate().get(Quantity.Unit.PPM) - targetWater.getSulfate().get(Quantity.Unit.PPM);
		double dcl = w.getChloride().get(Quantity.Unit.PPM) - targetWater.getChloride().get(Quantity.Unit.PPM);
		double dhco3 = w.getBicarbonate().get(Quantity.Unit.PPM) - targetWater.getBicarbonate().get(Quantity.Unit.PPM);

		deltaCa.refresh(dca);
		deltaMg.refresh(dmg);
		deltaNa.refresh(dna);
		deltaSO4.refresh(dso4);
		deltaCl.refresh(dcl);
		deltaHCO3.refresh(dhco3);

		double error = Math.pow(dca, 2) + Math.pow(dmg, 2) + Math.pow(dna, 2) + Math.pow(dso4, 2) + Math.pow(dcl, 2) + Math.pow(dhco3, 2);

		error /= 6;

		mse.setText(String.format("%.2f", error));
	}
}
