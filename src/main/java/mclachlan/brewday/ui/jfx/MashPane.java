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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.ui.jfx;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class MashPane extends ProcessStepPane
{
	private Label mashTemp;
	private TextField duration, grainTemp;
	private ComputedVolumePane outputMashPanel, outputFirstRunnings;

	private Mash mash;

	/*-------------------------------------------------------------------------*/
	public MashPane(TrackDirty parent)
	{
		super(parent);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void buildUiInternal()
	{
		grainTemp = new TextField();
		this.add(new Label(StringUtils.getUiString("mash.grain.temp")));
		this.add(grainTemp, "wrap");

		mashTemp = new Label();
		this.add(new Label(StringUtils.getUiString("mash.temp")));
		this.add(mashTemp, "wrap");

		duration = new TextField();
		this.add(new Label(StringUtils.getUiString("mash.duration")));
		this.add(duration, "wrap");

		outputMashPanel = new ComputedVolumePane(StringUtils.getUiString("mash.volume.created"));
		this.add(outputMashPanel, "span, wrap");

		outputFirstRunnings = new ComputedVolumePane(StringUtils.getUiString("mash.first.runnings"));
		this.add(outputFirstRunnings, "span, wrap");

		//---------

		grainTemp.textProperty().addListener((observable, oldValue, newValue) -> {
			if (mash != null && newValue != null)
			{
				mash.setGrainTemp(new TemperatureUnit(Double.valueOf(newValue)));
				if (detectDirty)
				{
					getParentTrackDirty().setDirty(this.mash);
				}
			}
		});

		duration.textProperty().addListener((observable, oldValue, newValue) -> {
			if (mash != null && newValue != null)
			{
				mash.setDuration(new TimeUnit(Double.valueOf(newValue),  Quantity.Unit.MINUTES, false));
				if (detectDirty)
				{
					getParentTrackDirty().setDirty(this.mash);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		this.mash = (Mash)step;

		if (this.mash != null)
		{
			duration.setText(""+mash.getDuration().get(Quantity.Unit.MINUTES));
			grainTemp.setText(""+mash.getGrainTemp().get(Quantity.Unit.CELSIUS));

			outputMashPanel.refresh(mash.getOutputMashVolume(), recipe);
			outputFirstRunnings.refresh(mash.getOutputFirstRunnings(), recipe);

			double v = mash.getMashTemp()==null ? Double.NaN : mash.getMashTemp().get(Quantity.Unit.CELSIUS);
			mashTemp.setText(StringUtils.getUiString("mash.temp.format", v));
		}
	}
}
