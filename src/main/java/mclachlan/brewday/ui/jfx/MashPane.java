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
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ButtonType.*;

/**
 *
 */
public class MashPane extends ProcessStepPane<Mash>
{
	private QuantityEditWidget<TemperatureUnit> mashTemp;
	private QuantityEditWidget<PhUnit> mashPh;

	/*-------------------------------------------------------------------------*/
	public MashPane(TrackDirty parent, RecipeTreeViewModel stepsTreeModel,
		boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void buildUiInternal()
	{
		addToolbar(ADD_FERMENTABLE, ADD_HOP, ADD_MISC, ADD_WATER, DUPLICATE, DELETE);

		getUnitControlUtils().addTemperatureUnitControl(
			this,
			"mash.grain.temp",
			Mash::getGrainTemp,
			Mash::setGrainTemp,
			Quantity.Unit.CELSIUS);

		getUnitControlUtils().addTimeUnitControl(
			this,
			"mash.duration",
			Mash::getDuration,
			Mash::setDuration,
			Quantity.Unit.MINUTES);

		Quantity.Unit tempUnit = Database.getInstance().getSettings().getUnitForStepAndIngredient(
			Quantity.Type.TEMPERATURE, ProcessStep.Type.MASH, IngredientAddition.Type.WATER);

		mashTemp = new QuantityEditWidget<>(tempUnit);
		mashTemp.setDisable(true);
		this.add(new Label(StringUtils.getUiString("mash.temp")));
		this.add(mashTemp, "wrap");

		mashPh = new QuantityEditWidget<>(Quantity.Unit.PH);
		mashPh.setDisable(true);
		this.add(new Label(StringUtils.getUiString("mash.ph")));
		this.add(mashPh, "wrap");

		ToolBar utils = new ToolBar();
		utils.setPadding(new Insets(3,3,3,3));

		Button waterBuilder = new Button(
			StringUtils.getUiString("tools.water.builder"),
			JfxUi.getImageView(Icons.waterBuilderIcon, Icons.ICON_SIZE));
		Button acidifier = new Button(
			StringUtils.getUiString("tools.acidifier"),
			JfxUi.getImageView(Icons.acidifierIcon, Icons.ICON_SIZE));
		Button mashTempTarget = new Button(
			StringUtils.getUiString("tools.mash.temp"),
			JfxUi.getImageView(Icons.temperatureIcon, Icons.ICON_SIZE));

		utils.getItems().add(waterBuilder);
		utils.getItems().add(acidifier);
		utils.getItems().add(mashTempTarget);

		this.add(utils, "span, wrap");

		addComputedVolumePane("mash.volume.created", Mash::getOutputMashVolume);

		// --------

		waterBuilder.setOnAction(actionEvent ->
		{
			WaterBuilderDialog wbd = new WaterBuilderDialog(getStep());
			wbd.showAndWait();

			if (wbd.getOutput())
			{
				List<MiscAddition> waterAdditions = wbd.getWaterAdditions();

				// remove all current water additions
				for (MiscAddition ma : getStep().getMiscAdditions())
				{
					if (ma.getMisc().getWaterAdditionFormula() != null &&
						ma.getMisc().getWaterAdditionFormula() != Misc.WaterAdditionFormula.LACTIC_ACID)
					{
						getStep().removeIngredientAddition(ma);
						getModel().removeIngredientAddition(getStep(), ma);
					}
				}

				// add these
				for (MiscAddition ma : waterAdditions)
				{
					getStep().addIngredientAddition(ma);
					getModel().addIngredientAddition(getStep(), ma);
					getParentTrackDirty().setDirty(ma);
				}
				getParentTrackDirty().setDirty(getStep());
			}
		});

		acidifier.setOnAction(actionEvent ->
		{
			Mash mash = getStep();
			AcidifierDialog acd  = new AcidifierDialog(
				mash.getMashPh(),
				mash.getCombinedWaterProfile(mash.getDuration()),
				mash.getFermentableAdditions());
			acd.showAndWait();

			if (acd.getOutput())
			{
				List<MiscAddition> acidAdditions = acd.getAcidAdditions();

				// do not remove all current acids, because the current ph already
				// accounts for them

				// add these
				for (MiscAddition ma : acidAdditions)
				{
					ma.setTime(mash.getDuration());
					getStep().addIngredientAddition(ma);
					getModel().addIngredientAddition(getStep(), ma);
					getParentTrackDirty().setDirty(ma);
				}
				getParentTrackDirty().setDirty(getStep());
			}
		});

		mashTempTarget.setOnAction(actionEvent ->
		{
			Mash mash = getStep();
			TargetMashTempDialog dialog  = new TargetMashTempDialog(
				mash.getMashPh(),
				mash.getCombinedWaterProfile(mash.getDuration()),
				mash.getFermentableAdditions(),
				mash.getGrainTemp());
			dialog.showAndWait();

			if (dialog.getOutput())
			{
				TemperatureUnit temp = dialog.getTemp();

				// set water temps
				for (WaterAddition wa : mash.getWaterAdditions())
				{
					wa.setTemperature(temp);

					getParentTrackDirty().setDirty(wa);
				}
				getParentTrackDirty().setDirty(getStep());
			}
		});

	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void refreshInternal(Mash step, Recipe recipe)
	{
		if (step != null)
		{
			mashTemp.refresh(step.getMashTemp());
			mashPh.refresh(step.getMashPh());
		}
	}
}
