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
import mclachlan.brewday.process.*;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class MashOutPanel extends ProcessStepPanel
{
	private JComboBox<String> mashVolume, outputWortVolume;
	private JSpinner tunLoss;

	public MashOutPanel()
	{
		super();
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		mashVolume = new JComboBox<String>();
		mashVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Mash:"), mashVolume, gbc);

		tunLoss = new JSpinner(new SpinnerNumberModel(3, 0, 9999, 1));
		tunLoss.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Tun Loss:"), tunLoss, gbc);

		outputWortVolume = new JComboBox<String>();
		outputWortVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Wort Volume Created:"), outputWortVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		MashOut mashOut = (MashOut)step;

		mashVolume.setModel(getVolumesOptions(batch));
		outputWortVolume.setModel(getVolumesOptions(batch));

		if (step != null)
		{
			mashVolume.setSelectedItem(mashOut.getMashVolume());
			outputWortVolume.setSelectedItem(mashOut.getOutputWortVolume());
			tunLoss.setValue(mashOut.getTunLoss());
		}
	}

	@Override
	public ProcessStep getStep()
	{
		return new MashOut(
			name.getText(),
			desc.getText(),
			getSelectedString(mashVolume),
			getSelectedString(outputWortVolume),
			getTunLoss());
	}

	public String getMashVolume()
	{
		return getSelectedString(mashVolume);
	}

	public double getTunLoss()
	{
		return Double.valueOf((Integer)(tunLoss.getValue()));
	}
}
