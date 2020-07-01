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

package mclachlan.brewday.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.Ferment;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class FermentPanel extends ProcessStepPanel
{
	private JComboBox<String> inputVolume;
	private ComputedVolumePanel outputVolume;
	private JSpinner fermTemp, duration;
	private JLabel estFG;

	public FermentPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		setLayout(new MigLayout());

		inputVolume = new JComboBox<String>();
		inputVolume.addActionListener(this);
		add(new JLabel(StringUtils.getUiString("volumes.in")));
		add(inputVolume, "wrap");

		fermTemp = new JSpinner(new SpinnerNumberModel(20.0, 0.0, 100.0, .1D));
		fermTemp.addChangeListener(this);
		add(new JLabel(StringUtils.getUiString("ferment.temp")));
		add(fermTemp, "wrap");

		duration = new JSpinner(new SpinnerNumberModel(14.0, 0.0, 999.0, 1D));
		duration.addChangeListener(this);
		add(new JLabel(StringUtils.getUiString("ferment.duration")));
		add(duration, "wrap");

		estFG = new JLabel();
		add(new JLabel(StringUtils.getUiString("ferment.fg")));
		add(estFG, "wrap");

		outputVolume = new ComputedVolumePanel(StringUtils.getUiString("volumes.out"));
		add(outputVolume, "span, wrap");
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		Ferment ferment = (Ferment)step;

		inputVolume.setModel(getVolumesOptions(recipe, Volume.Type.WORT));

		inputVolume.removeActionListener(this);
		fermTemp.removeChangeListener(this);
		duration.removeChangeListener(this);

		if (step != null)
		{
			inputVolume.setSelectedItem(ferment.getInputVolume());
			outputVolume.refresh(ferment.getOutputVolume(), recipe);
			fermTemp.setValue(ferment.getTemperature().get(Quantity.Unit.CELSIUS));
			duration.setValue(ferment.getDuration().get(Quantity.Unit.DAYS));
			estFG.setText(
				String.format("%.3f",
					ferment.getEstimatedFinalGravity().get(
						DensityUnit.Unit.SPECIFIC_GRAVITY)));
		}

		inputVolume.addActionListener(this);
		fermTemp.addChangeListener(this);
		duration.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Ferment ferment = (Ferment)getStep();

		if (e.getSource() == inputVolume)
		{
			ferment.setInputVolume((String)inputVolume.getSelectedItem());
			SwingUi.instance.setDirty(this.dirtyFlag);
			triggerUiRefresh();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		Ferment ferment = (Ferment)getStep();

		if (e.getSource() == fermTemp)
		{
			ferment.setTemperature(new TemperatureUnit((Double)fermTemp.getValue()));
			SwingUi.instance.setDirty(this.dirtyFlag);
			triggerUiRefresh();
		}
		else if (e.getSource() == duration)
		{
			ferment.setDuration(new TimeUnit((Double)duration.getValue(), Quantity.Unit.DAYS, false));
			SwingUi.instance.setDirty(this.dirtyFlag);
			triggerUiRefresh();
		}
	}
}
