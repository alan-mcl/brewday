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
import mclachlan.brewday.process.SingleInfusionMash;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class SingleInfusionMashPanel extends ProcessStepPanel
{
	private JComboBox<String> grainBillVolume, waterVolume, outputMashVolume;
	private JSpinner duration, mashTemp;

	public SingleInfusionMashPanel(boolean addMode)
	{
		super(addMode);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc, boolean addMode)
	{
		grainBillVolume = new JComboBox<String>();
		grainBillVolume.addActionListener(this);
		grainBillVolume.setEditable(addMode);
		dodgyGridBagShite(this, new JLabel("Grain Bill:"), grainBillVolume, gbc);

		waterVolume = new JComboBox<String>();
		waterVolume.addActionListener(this);
		waterVolume.setEditable(addMode);
		dodgyGridBagShite(this, new JLabel("Strike Water:"), waterVolume, gbc);

		mashTemp = new JSpinner(new SpinnerNumberModel(66, 0, 100, 1));
		mashTemp.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Mash Temp:"), mashTemp, gbc);

		duration = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1));
		duration.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Duration:"), duration, gbc);

		outputMashVolume = new JComboBox<String>();
		outputMashVolume.addActionListener(this);
		outputMashVolume.setEditable(addMode);
		dodgyGridBagShite(this, new JLabel("Mash Volume Created:"), outputMashVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		Boil boil = (Boil)step;

		grainBillVolume.setModel(getVolumesOptions(batch));
		waterVolume.setModel(getVolumesOptions(batch));
		outputMashVolume.setModel(getVolumesOptions(batch));

		if (step != null)
		{
			grainBillVolume.setSelectedItem(boil.getHopAdditionVolume());
			waterVolume.setSelectedItem(boil.getInputWortVolume());
			outputMashVolume.setSelectedItem(boil.getOutputWortVolume());
			duration.setValue(boil.getDuration());
		}
	}

	@Override
	public ProcessStep getStep()
	{
		return new SingleInfusionMash(
			name.getText(),
			desc.getText(),
			getSelectedString(grainBillVolume),
			getSelectedString(waterVolume),
			getSelectedString(outputMashVolume),
			getDuration(),
			getMashTemp());
	}

	public String getGrainBillVolume()
	{
		return getSelectedString(grainBillVolume);
	}

	public double getDuration()
	{
		return Double.valueOf((Integer)(duration.getValue()));
	}

	public double getMashTemp()
	{
		return Double.valueOf((Integer)(mashTemp.getValue()));
	}
}
