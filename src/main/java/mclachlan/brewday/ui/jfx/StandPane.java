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

import mclachlan.brewday.process.Stand;
import mclachlan.brewday.process.Volume;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ToolbarButtonType.*;

/**
 *
 */
public class StandPane extends ProcessStepPane<Stand>
{
	public StandPane(TrackDirty parent, RecipeTreeView stepsTreeModel,
		boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(new Stand().getSupportedIngredientAdditions(), RENAME_STEP, DUPLICATE, DELETE);

		addInputVolumeComboBox("volumes.in",
			Stand::getInputVolume,
			Stand::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);

		addComputedVolumePane("volumes.out", Stand::getOutputVolume);
	}
}
