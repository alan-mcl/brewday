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

import mclachlan.brewday.process.BatchSparge;
import mclachlan.brewday.process.Volume;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ToolbarButtonType.*;

/**
 *
 */
public class BatchSpargePane extends ProcessStepPane<BatchSparge>
{
	public BatchSpargePane(TrackDirty parent, RecipeTreeViewModel stepsTreeModel,
		boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	protected void buildUiInternal()
	{
		addToolbar(ADD_WATER, ADD_FERMENTABLE, ADD_MISC, DUPLICATE, DELETE);

		addInputVolumeComboBox("batch.sparge.mash",
			BatchSparge::getMashVolume,
			BatchSparge::setMashVolume,
			Volume.Type.MASH);

		addInputVolumeComboBox("batch.sparge.existing.wort",
			BatchSparge::getWortVolume,
			BatchSparge::setWortVolume,
			Volume.Type.WORT);

		addUtilityBar(UtilityType.WATER_BUILDER/*, UtilityType.ACIDIFIER*/);

		addComputedVolumePane("batch.sparge.sparge.runnings", BatchSparge::getOutputSpargeRunnings);
		addComputedVolumePane("batch.sparge.sparge.runnings.existing.wort", BatchSparge::getOutputCombinedWortVolume);
		addComputedVolumePane("batch.sparge.lautered.mash", BatchSparge::getOutputMashVolume);
	}
}
