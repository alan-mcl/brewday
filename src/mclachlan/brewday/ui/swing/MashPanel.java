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

package mclachlan.brewday.ui.swing;

import java.awt.GridBagConstraints;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class MashPanel extends ProcessStepPanel
{
	private JLabel mashTemp;
	private JSpinner duration, grainTemp;
	private ComputedVolumePanel outputMashPanel, outputFirstRunnings;

	public MashPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		setLayout (new MigLayout());

		grainTemp = new JSpinner(new SpinnerNumberModel(66, 0, 100, 1D));
		grainTemp.addChangeListener(this);
		this.add(new JLabel(StringUtils.getUiString("mash.grain.temp")));
		this.add(grainTemp, "wrap");

		mashTemp = new JLabel();
		this.add(new JLabel(StringUtils.getUiString("mash.temp")));
		this.add(mashTemp, "wrap");

		duration = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1D));
		duration.addChangeListener(this);
		this.add(new JLabel(StringUtils.getUiString("mash.duration")));
		this.add(duration, "wrap");

		outputMashPanel = new ComputedVolumePanel(StringUtils.getUiString("mash.volume.created"));
		this.add(outputMashPanel, "span, wrap");

		outputFirstRunnings = new ComputedVolumePanel(StringUtils.getUiString("mash.first.runnings"));
		this.add(outputFirstRunnings, "span, wrap");
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		Mash mash = (Mash)step;
		
		duration.removeChangeListener(this);
		grainTemp.removeChangeListener(this);

		if (step != null)
		{
			duration.setValue(mash.getDuration());
			grainTemp.setValue(mash.getGrainTemp().get(Quantity.Unit.CELSIUS));

			outputMashPanel.refresh(mash.getOutputMashVolume(), recipe);
			outputFirstRunnings.refresh(mash.getOutputFirstRunnings(), recipe);
			double v = mash.getMashTemp()==null ? Double.NaN : mash.getMashTemp().get(Quantity.Unit.CELSIUS);
			mashTemp.setText(
				StringUtils.getUiString(
					"mash.temp.format",
					v));
		}

		duration.addChangeListener(this);
		grainTemp.addChangeListener(this);
	}

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
			step.setDuration((Double)duration.getValue());
			triggerUiRefresh();
		}
	}
}
