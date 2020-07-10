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
import javafx.scene.control.TextField;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.process.SplitByPercent;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ButtonType.*;

/**
 *
 */
public class SplitByPercentPane extends ProcessStepPane<SplitByPercent>
{
	private TextField percent;

	public SplitByPercentPane(TrackDirty parent,
		RecipeTreeViewModel stepsTreeModel, boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(DUPLICATE, DELETE);

		addInputVolumeComboBox("volumes.in",
			SplitByPercent::getInputVolume,
			SplitByPercent::setInputVolume,
			Volume.Type.BEER);

		percent = new TextField();
		this.add(new Label(StringUtils.getUiString("split.split%")));
		this.add(percent, "wrap");

		addComputedVolumePane("volumes.out.1", SplitByPercent::getOutputVolume);
		addComputedVolumePane("volumes.out.2", SplitByPercent::getOutputVolume2);

		// -----
		percent.textProperty().addListener((observable, oldValue, newValue) ->
		{
			if (getStep() != null && newValue != null)
			{
				if (!refreshing)
				{
					getStep().setSplitPercent(Double.valueOf(newValue));
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(getStep());
				}
			}
		});
	}

	@Override
	protected void refreshInternal(SplitByPercent step, Recipe recipe)
	{
		if (step != null)
		{
			percent.setText(""+step.getSplitPercent());
		}
	}
}
