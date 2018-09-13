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
import mclachlan.brewday.process.BatchSparge;
import mclachlan.brewday.process.ProcessStep;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class BatchSpargePanel extends ProcessStepPanel
{
	private JComboBox<String> mashVolume, spargeWaterVolume, wortVolume, outputVolume;

	public BatchSpargePanel(boolean addMode)
	{
		super(addMode);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc, boolean addMode)
	{
		mashVolume = new JComboBox<String>();
		mashVolume.addActionListener(this);
		mashVolume.setEditable(addMode);
		dodgyGridBagShite(this, new JLabel("Mash:"), mashVolume, gbc);

		spargeWaterVolume = new JComboBox<String>();
		spargeWaterVolume.addActionListener(this);
		spargeWaterVolume.setEditable(addMode);
		dodgyGridBagShite(this, new JLabel("Sparge Water:"), spargeWaterVolume, gbc);

		wortVolume = new JComboBox<String>();
		wortVolume.addActionListener(this);
		wortVolume.setEditable(addMode);
		dodgyGridBagShite(this, new JLabel("Destination Wort:"), wortVolume, gbc);

		outputVolume = new JComboBox<String>();
		outputVolume.addActionListener(this);
		outputVolume.setEditable(addMode);
		dodgyGridBagShite(this, new JLabel("Combined Wort:"), outputVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		spargeWaterVolume.setModel(getVolumesOptions(batch));
		mashVolume.setModel(getVolumesOptions(batch));
		wortVolume.setModel(getVolumesOptions(batch));
		outputVolume.setModel(getVolumesOptions(batch));

		if (step != null)
		{
			BatchSparge bs = (BatchSparge)step;
			spargeWaterVolume.setSelectedItem(bs.getSpargeWaterVolume());
			wortVolume.setSelectedItem(bs.getWortVolume());
			mashVolume.setSelectedItem(bs.getMashVolume());
			outputVolume.setSelectedItem(bs.getOutputVolume());
		}
	}

	public ProcessStep getStep()
	{
		return new BatchSparge(
			name.getText(),
			desc.getText(),
			getSelectedString(mashVolume),
			getSelectedString(spargeWaterVolume),
			getSelectedString(wortVolume),
			getSelectedString(outputVolume));
	}

	public String getSpargeWaterVolume()
	{
		return getSelectedString(spargeWaterVolume);
	}
}
