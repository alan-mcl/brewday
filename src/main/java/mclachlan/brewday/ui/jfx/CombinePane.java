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

import mclachlan.brewday.process.Combine;
import mclachlan.brewday.process.Volume;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ToolbarButtonType.*;

/**
 *
 */
public class CombinePane extends ProcessStepPane<Combine>
{
	public CombinePane(TrackDirty parent,
		RecipeTreeView stepsTreeModel, boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(new Combine().getSupportedIngredientAdditions(), RENAME_STEP, DUPLICATE, DELETE);

		addInputVolumeComboBox("volumes.in",
			Combine::getInputVolume,
			Combine::setInputVolume,
			Volume.Type.BEER, Volume.Type.WORT, Volume.Type.MASH);

		addInputVolumeComboBox("combine.input.2",
			Combine::getInputVolume2,
			Combine::setInputVolume2,
			Volume.Type.BEER, Volume.Type.WORT, Volume.Type.MASH);


		addComputedVolumePane("volumes.out", Combine::getOutputVolume);
	}
}
