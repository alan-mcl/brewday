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
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

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

		addComputedVolumePane("mash.volume.created", Mash::getOutputMashVolume);
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
