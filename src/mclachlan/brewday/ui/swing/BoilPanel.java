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
import mclachlan.brewday.process.Batch;
import mclachlan.brewday.process.Boil;
import mclachlan.brewday.process.ProcessStep;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class BoilPanel extends ProcessStepPanel
{
	private JComboBox<String> hopAdditionVolume, inputWortVolume, outputWortVolume;
	private JSpinner duration;

	public BoilPanel()
	{
		super();
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

		duration = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1));
		duration.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Duration:"), duration, gbc);

		outputWortVolume = new JComboBox<String>();
		outputWortVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Wort Out:"), outputWortVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		Boil boil = (Boil)step;

		hopAdditionVolume.setModel(getVolumesOptions(batch));
		inputWortVolume.setModel(getVolumesOptions(batch));
		outputWortVolume.setModel(getVolumesOptions(batch));

		if (step != null)
		{
			hopAdditionVolume.setSelectedItem(boil.getHopAdditionVolume());
			inputWortVolume.setSelectedItem(boil.getInputWortVolume());
			outputWortVolume.setSelectedItem(boil.getOutputWortVolume());
			duration.setValue(boil.getDuration());
		}
	}

	@Override
	public ProcessStep getStep()
	{
		return new Boil(
			name.getText(),
			desc.getText(),
			getSelectedString(inputWortVolume),
			getSelectedString(outputWortVolume),
			getSelectedString(hopAdditionVolume),
			getDuration());
	}

	public String getHopAdditionVolume()
	{
		return getSelectedString(hopAdditionVolume);
	}

	public double getDuration()
	{
		return Double.valueOf((Integer)(duration.getValue()));
	}
}
