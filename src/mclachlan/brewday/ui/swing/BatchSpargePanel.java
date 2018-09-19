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
import mclachlan.brewday.process.Batch;
import mclachlan.brewday.process.BatchSparge;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class BatchSpargePanel extends ProcessStepPanel
{
	private JComboBox<String> mashVolume, spargeWaterVolume, wortVolume;
	private JLabel outputVolume;

	public BatchSpargePanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		mashVolume = new JComboBox<String>();
		mashVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Mash to sparge:"), mashVolume, gbc);

		spargeWaterVolume = new JComboBox<String>();
		spargeWaterVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Sparge water:"), spargeWaterVolume, gbc);

		wortVolume = new JComboBox<String>();
		wortVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Wort already collected:"), wortVolume, gbc);

		outputVolume = new JLabel();
		dodgyGridBagShite(this, new JLabel("Combined wort out:"), outputVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		spargeWaterVolume.setModel(getVolumesOptions(batch, Volume.Type.WATER));
		mashVolume.setModel(getVolumesOptions(batch, Volume.Type.MASH));
		wortVolume.setModel(getVolumesOptions(batch, Volume.Type.WORT));

		spargeWaterVolume.removeActionListener(this);
		mashVolume.removeActionListener(this);
		wortVolume.removeActionListener(this);

		if (step != null)
		{
			BatchSparge bs = (BatchSparge)step;
			spargeWaterVolume.setSelectedItem(bs.getSpargeWaterVolume());
			wortVolume.setSelectedItem(bs.getWortVolume());
			mashVolume.setSelectedItem(bs.getMashVolume());
			outputVolume.setText("'" + bs.getOutputVolume() + "'");
		}

		spargeWaterVolume.addActionListener(this);
		mashVolume.addActionListener(this);
		wortVolume.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		BatchSparge step = (BatchSparge)getStep();

		if (e.getSource() == spargeWaterVolume)
		{
			step.setSpargeWaterVolume((String)spargeWaterVolume.getSelectedItem());
			triggerUiRefresh();
		}
		else if (e.getSource() == mashVolume)
		{
			step.setMashVolume((String)mashVolume.getSelectedItem());
			triggerUiRefresh();
		}
		else if (e.getSource() == wortVolume)
		{
			step.setWortVolume((String)wortVolume.getSelectedItem());
			triggerUiRefresh();
		}
	}
}
