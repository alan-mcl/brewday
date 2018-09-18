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
import mclachlan.brewday.process.Ferment;
import mclachlan.brewday.process.ProcessStep;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class FermentPanel extends ProcessStepPanel
{
	private JComboBox<String> inputVolume, outputVolume;
	private JSpinner targetGravity;

	public FermentPanel()
	{
		super();
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		inputVolume = new JComboBox<String>();
		inputVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("In:"), inputVolume, gbc);

		targetGravity = new JSpinner(new SpinnerNumberModel(1010, 900, 9999, 1));
		targetGravity.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Target gravity:"), targetGravity, gbc);

		outputVolume = new JComboBox<String>();
		outputVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Out:"), outputVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		Ferment ferment = (Ferment)step;

		inputVolume.setModel(getVolumesOptions(batch));
		outputVolume.setModel(getVolumesOptions(batch));

		if (step != null)
		{
			inputVolume.setSelectedItem(ferment.getInputVolume());
			outputVolume.setSelectedItem(ferment.getOutputVolume());
			targetGravity.setValue(1000+ferment.getTargetGravity());
		}
	}
}
