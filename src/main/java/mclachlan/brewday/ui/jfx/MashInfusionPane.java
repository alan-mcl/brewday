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

import javafx.scene.control.Label;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.process.MashInfusion;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ButtonType.*;

/**
 *
 */
public class MashInfusionPane extends ProcessStepPane<MashInfusion>
{
	private QuantityEditWidget<TemperatureUnit> mashTemp;

	/*-------------------------------------------------------------------------*/
	public MashInfusionPane(TrackDirty parent, RecipeTreeViewModel stepsTreeModel,
		boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void buildUiInternal()
	{
		addToolbar(ADD_WATER, DUPLICATE, DELETE);

		addInputVolumeComboBox(
			"mash.infusion.volume.in",
			MashInfusion::getInputMashVolume,
			MashInfusion::setInputMashVolume,
			Volume.Type.MASH);

		Quantity.Unit tempUnit = Database.getInstance().getSettings().getUnitForStepAndIngredient(
			Quantity.Type.TEMPERATURE, ProcessStep.Type.MASH, IngredientAddition.Type.WATER);

		mashTemp = new QuantityEditWidget<>(tempUnit);
		mashTemp.setDisable(true);
		this.add(new Label(StringUtils.getUiString("mash.temp")));
		this.add(mashTemp, "wrap");

		getUnitControlUtils().addTimeUnitControl(
			this,
			"mash.infusion.ramp.time",
			MashInfusion::getRampTime,
			MashInfusion::setRampTime,
			Quantity.Unit.MINUTES);

		getUnitControlUtils().addTimeUnitControl(
			this,
			"mash.infusion.duration",
			MashInfusion::getStandTime,
			MashInfusion::setStandTime,
			Quantity.Unit.MINUTES);

		addComputedVolumePane("mash.infusion.mash.volume.out", MashInfusion::getOutputMashVolume);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void refreshInternal(MashInfusion step, Recipe recipe)
	{
		if (step != null)
		{
			mashTemp.refresh(step.getMashTemp());
		}
	}
}
