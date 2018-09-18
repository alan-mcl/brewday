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
import mclachlan.brewday.process.Dilute;
import mclachlan.brewday.process.ProcessStep;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class DilutePanel extends ProcessStepPanel
{
	private JComboBox<String> inputVolume, outputVolume;
	private JSpinner volTarget, additionTemp;

	public DilutePanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		inputVolume = new JComboBox<String>();
		inputVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("In:"), inputVolume, gbc);

		volTarget = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 0.1));
		volTarget.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Volume target (l):"), volTarget, gbc);

		additionTemp = new JSpinner(new SpinnerNumberModel(20, 0, 9999, 0.1));
		additionTemp.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Water addition temp (C):"), additionTemp, gbc);

		outputVolume = new JComboBox<String>();
		outputVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Out:"), outputVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		Dilute dilute = (Dilute)step;

		inputVolume.setModel(getVolumesOptions(batch));
		outputVolume.setModel(getVolumesOptions(batch));

		if (step != null)
		{
			inputVolume.setSelectedItem(dilute.getInputVolume());
			outputVolume.setSelectedItem(dilute.getOutputVolume());
			additionTemp.setValue(dilute.getAdditionTemp());
			volTarget.setValue(dilute.getVolumeTarget() /1000);
		}
	}
}
