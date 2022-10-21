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

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Ferment;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ToolbarButtonType.*;

/**
 *
 */
public class FermentPane extends ProcessStepPane<Ferment>
{
	private CheckBox removeTrubAndChillerLoss;
	private QuantityEditWidget<DensityUnit> estFG;

	public FermentPane(TrackDirty parent, RecipeTreeView stepsTreeModel,
		boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(new Ferment().getSupportedIngredientAdditions(), DUPLICATE, DELETE);

		addInputVolumeComboBox("volumes.in",
			Ferment::getInputVolume,
			Ferment::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);

		getUnitControlUtils().addTemperatureUnitControl(this, "ferment.temp", Ferment::getTemperature, Ferment::setTemperature, Quantity.Unit.CELSIUS);

		getUnitControlUtils().addTimeUnitControl(this, "ferment.duration", Ferment::getDuration, Ferment::setDuration, Quantity.Unit.DAYS);

		removeTrubAndChillerLoss = new CheckBox(StringUtils.getUiString("ferment.remove.trub.and.chiller.loss"));
		this.add(removeTrubAndChillerLoss, "span, wrap");

		Quantity.Unit densityUnit = Database.getInstance().getSettings().getUnitForStepAndIngredient(
			Quantity.Type.FLUID_DENSITY, ProcessStep.Type.MASH, IngredientAddition.Type.WATER);

		estFG = new QuantityEditWidget<>(densityUnit);
		estFG.setDisable(true);

		this.add(new Label(StringUtils.getUiString("ferment.fg")));
		this.add(estFG, "wrap");

		addComputedVolumePane("volumes.out", Ferment::getOutputVolume);

		// --------

		removeTrubAndChillerLoss.setOnAction(actionEvent ->
		{
			if (!refreshing)
			{
				getStep().setRemoveTrubAndChillerLoss(removeTrubAndChillerLoss.isSelected());

				getParentTrackDirty().setDirty(getStep());
			}
		});
	}

	@Override
	protected void refreshInternal(Ferment step, Recipe recipe)
	{
		if (step != null)
		{
			estFG.refresh(step.getEstimatedFinalGravity());
			removeTrubAndChillerLoss.setSelected(step.isRemoveTrubAndChillerLoss());
		}
	}
}
