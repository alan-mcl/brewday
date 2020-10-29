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
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.Boil;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ToolbarButtonType.*;

/**
 *
 */
public class BoilPane extends ProcessStepPane<Boil>
{
	private QuantityEditWidget<TimeUnit> timeToBoil;

	public BoilPane(TrackDirty parent, RecipeTreeViewModel stepsTreeModel,
		boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ADD_FERMENTABLE, ADD_HOP, ADD_MISC, ADD_WATER, DUPLICATE, DELETE);

		addInputVolumeComboBox("boil.wort.in",
			Boil::getInputWortVolume,
			Boil::setInputWortVolume,
			Volume.Type.WORT, Volume.Type.MASH);

		Quantity.Unit tempUnit = Database.getInstance().getSettings().getUnitForStepAndIngredient(
			Quantity.Type.TIME, ProcessStep.Type.BOIL, IngredientAddition.Type.WATER);

		timeToBoil = new QuantityEditWidget<>(tempUnit);
		timeToBoil.setDisable(true);
		this.add(new Label(StringUtils.getUiString("boil.time.to.boil")));
		this.add(timeToBoil, "wrap");

		getUnitControlUtils().addTimeUnitControl(this, "boil.duration", Boil::getDuration, Boil::setDuration, Quantity.Unit.MINUTES);

		addComputedVolumePane("boil.wort.out", Boil::getOutputWortVolume);
	}

	@Override
	protected void refreshInternal(Boil step, Recipe recipe)
	{
		if (step != null)
		{
			timeToBoil.refresh(step.getTimeToBoil());
		}
	}
}
