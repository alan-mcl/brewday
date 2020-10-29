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

import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.Split;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ToolbarButtonType.DELETE;
import static mclachlan.brewday.ui.jfx.ProcessStepPane.ToolbarButtonType.DUPLICATE;

/**
 *
 */
public class SplitPane extends ProcessStepPane<Split>
{
	private RadioButton byPerc, byVol;
	private QuantityEditWidget<PercentageUnit> splitPercent;
	private QuantityEditWidget<VolumeUnit> splitVolume;


	public SplitPane(TrackDirty parent,
		RecipeTreeViewModel stepsTreeModel, boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(DUPLICATE, DELETE);

		addInputVolumeComboBox("volumes.in",
			Split::getInputVolume,
			Split::setInputVolume,
			Volume.Type.BEER, Volume.Type.WORT, Volume.Type.MASH);

		byPerc = new RadioButton(StringUtils.getUiString("split.by.percentage"));
		byVol = new RadioButton(StringUtils.getUiString("split.by.volume"));
		ToggleGroup tg = new ToggleGroup();
		byPerc.setToggleGroup(tg);
		byVol.setToggleGroup(tg);

		this.add(byPerc);
		splitPercent = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY, 0);
//		this.add(new Label(StringUtils.getUiString("split.percentage")));
		this.add(splitPercent, "wrap");

		this.add(byVol);
		splitVolume = new QuantityEditWidget<>(Quantity.Unit.LITRES, 0);
//		this.add(new Label(StringUtils.getUiString("split.volume")));
		this.add(splitVolume, "wrap");

		addComputedVolumePane("volumes.out.1", Split::getOutputVolume);
		addComputedVolumePane("volumes.out.2", Split::getOutputVolume2);

		// -----
		splitPercent.addListener((observable, oldValue, newValue) ->
		{
			if (getStep() != null && newValue != null)
			{
				if (!refreshing)
				{
					getStep().setSplitPercent(splitPercent.getQuantity());
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(getStep());
				}
			}
		});

		splitVolume.addListener((observable, oldValue, newValue) ->
		{
			if (getStep() != null && newValue != null)
			{
				if (!refreshing)
				{
					getStep().setSplitVolume(splitVolume.getQuantity());
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(getStep());
				}
			}
		});

		byPerc.setOnAction(event ->
		{
			if (getStep() != null)
			{
				splitPercent.setDisable(false);
				splitVolume.setDisable(true);

				if (!refreshing)
				{
					getStep().setSplitType(Split.Type.PERCENTAGE);
					getStep().setSplitPercent(splitPercent.getQuantity());
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(getStep());
				}
			}
		});

		byVol.setOnAction(event ->
		{
			splitPercent.setDisable(true);
			splitVolume.setDisable(false);

			if (getStep() != null)
			{
				if (!refreshing)
				{
					getStep().setSplitType(Split.Type.ABSOLUTE);
					getStep().setSplitVolume(splitVolume.getQuantity());
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(getStep());
				}
			}
		});

	}

	@Override
	protected void refreshInternal(Split step, Recipe recipe)
	{
		if (step != null)
		{
			switch (step.getSplitType())
			{
				case PERCENTAGE:
					byPerc.setSelected(true);
					splitPercent.setDisable(false);
					splitVolume.setDisable(true);
					break;
				case ABSOLUTE:
					byVol.setSelected(true);
					splitPercent.setDisable(true);
					splitVolume.setDisable(false);
					break;
				default:
					throw new BrewdayException("Invalid "+step.getSplitType());
			}

			if (step.getSplitPercent() != null)
			{
				splitPercent.refresh(step.getSplitPercent());
			}

			if (step.getSplitVolume() != null)
			{
				splitVolume.refresh(step.getSplitVolume());
			}
		}
	}
}
