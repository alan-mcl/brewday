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
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import mclachlan.brewday.process.Batch;
import mclachlan.brewday.process.Boil;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class BoilPanel extends ProcessStepPanel
{
	private JComboBox<String> hopAdditionVolume, inputWortVolume;
	private JLabel outputWortVolume;
	private JSpinner duration;

	public BoilPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		inputWortVolume = new JComboBox<String>();
		inputWortVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Wort In:"), inputWortVolume, gbc);

		hopAdditionVolume = new JComboBox<String>();
		hopAdditionVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Hop Addition:"), hopAdditionVolume, gbc);

		duration = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1.0));
		duration.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Duration (min):"), duration, gbc);

		outputWortVolume = new JLabel();
		dodgyGridBagShite(this, new JLabel("Wort Out:"), outputWortVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		Boil boil = (Boil)step;

		hopAdditionVolume.setModel(getVolumesOptions(batch, Volume.Type.HOPS));
		inputWortVolume.setModel(getVolumesOptions(batch, Volume.Type.WORT, Volume.Type.BEER));

		hopAdditionVolume.removeActionListener(this);
		inputWortVolume.removeActionListener(this);
		duration.removeChangeListener(this);

		if (step != null)
		{
			hopAdditionVolume.setSelectedItem(boil.getHopAdditionVolume());
			inputWortVolume.setSelectedItem(boil.getInputWortVolume());
			outputWortVolume.setText("'" + boil.getOutputWortVolume() + "'");
			duration.setValue(boil.getDuration());
		}

		hopAdditionVolume.addActionListener(this);
		inputWortVolume.addActionListener(this);
		duration.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Boil step = (Boil)getStep();

		if (e.getSource() == hopAdditionVolume)
		{
			step.setHopAdditionVolume((String)hopAdditionVolume.getSelectedItem());
			triggerUiRefresh();
		}
		else if (e.getSource() == inputWortVolume)
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
			step.setDuration((Double)duration.getValue());
			triggerUiRefresh();
		}
	}
}
