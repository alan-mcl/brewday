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
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.Boil;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class BoilPanel extends ProcessStepPanel
{
	private JComboBox<String> inputWortVolume;
	private ComputedVolumePanel outputWortVolume;
	private JSpinner duration;

	public BoilPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		setLayout(new MigLayout());

		inputWortVolume = new JComboBox<String>();
		inputWortVolume.addActionListener(this);
		add(new JLabel(StringUtils.getUiString("boil.wort.in")));
		add(inputWortVolume, "wrap");

		duration = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1.0));
		duration.addChangeListener(this);
		add(new JLabel(StringUtils.getUiString("boil.duration")));
		add(duration, "wrap");

		outputWortVolume = new ComputedVolumePanel(StringUtils.getUiString("boil.wort.out"));
		add(outputWortVolume, "span, wrap");
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		Boil boil = (Boil)step;

		inputWortVolume.setModel(getVolumesOptions(recipe, Volume.Type.WORT, Volume.Type.BEER));

		inputWortVolume.removeActionListener(this);
		duration.removeChangeListener(this);

		if (step != null)
		{
			inputWortVolume.setSelectedItem(boil.getInputWortVolume());
			outputWortVolume.refresh(boil.getOutputWortVolume(), recipe);
			duration.setValue(boil.getDuration().get(Quantity.Unit.MINUTES));
		}

		inputWortVolume.addActionListener(this);
		duration.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Boil step = (Boil)getStep();

		if (e.getSource() == inputWortVolume)
		{
			step.setInputWortVolume((String)inputWortVolume.getSelectedItem());
			triggerUiRefresh();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		Boil step = (Boil)getStep();

		if (e.getSource() == duration)
		{
			step.setDuration(new TimeUnit((Double)duration.getValue(), Quantity.Unit.MINUTES, false));
			triggerUiRefresh();
		}
	}
}
