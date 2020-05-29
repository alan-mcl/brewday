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

	public MashPane(String dirtyFlag)
	{
		super(dirtyFlag);
	}

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
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		Mash mash = (Mash)step;
		
		if (step != null)
		{
			duration.setText(""+mash.getDuration().get(Quantity.Unit.MINUTES));
			grainTemp.setText(""+mash.getGrainTemp().get(Quantity.Unit.CELSIUS));

			outputMashPanel.refresh(mash.getOutputMashVolume(), recipe);
			outputFirstRunnings.refresh(mash.getOutputFirstRunnings(), recipe);
			double v = mash.getMashTemp()==null ? Double.NaN : mash.getMashTemp().get(Quantity.Unit.CELSIUS);
			mashTemp.setText(
				StringUtils.getUiString(
					"mash.temp.format",
					v));
		}
	}

/*
	@Override
	public void stateChanged(ChangeEvent e)
	{
		Mash step = (Mash)getStep();

		if (e.getSource() == grainTemp)
		{
			step.setGrainTemp(new TemperatureUnit((Double)grainTemp.getValue()));
			triggerUiRefresh();
		}
		else if (e.getSource() == duration)
		{
			step.setDuration(new TimeUnit((Double)duration.getValue(), Quantity.Unit.MINUTES, false));
			triggerUiRefresh();
		}
	}
*/
}
