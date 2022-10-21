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

import mclachlan.brewday.process.Dilute;
import mclachlan.brewday.process.Volume;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ToolbarButtonType.*;

/**
 *
 */
public class DilutePane extends ProcessStepPane<Dilute>
{
	public DilutePane(TrackDirty parent, RecipeTreeView stepsTreeModel,
		boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(new Dilute().getSupportedIngredientAdditions(), DUPLICATE, DELETE);

		addInputVolumeComboBox("volumes.in",
			Dilute::getInputVolume,
			Dilute::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);

		addComputedVolumePane("volumes.out", Dilute::getOutputVolume);
	}
}
