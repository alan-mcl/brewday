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
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.SingleInfusionMash;
import mclachlan.brewday.process.Volume;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class SingleInfusionMashPanel extends ProcessStepPanel
{
	private JComboBox<String> grainBillVolume, waterVolume;
	private JLabel outputMashVolume;
	private JLabel mashTemp;
	private JSpinner duration, grainTemp;

	public SingleInfusionMashPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		grainBillVolume = new JComboBox<String>();
		grainBillVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Grain Bill:"), grainBillVolume, gbc);

		waterVolume = new JComboBox<String>();
		waterVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Strike Water:"), waterVolume, gbc);

		grainTemp = new JSpinner(new SpinnerNumberModel(66, 0, 100, 1));
		grainTemp.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Grain Temp (C):"), grainTemp, gbc);

		mashTemp = new JLabel();
		dodgyGridBagShite(this, new JLabel("Mash Temp (C):"), mashTemp, gbc);

		duration = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1));
		duration.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Duration (min):"), duration, gbc);

		outputMashVolume = new JLabel();
		dodgyGridBagShite(this, new JLabel("Mash Volume Created:"), outputMashVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		SingleInfusionMash mash = (SingleInfusionMash)step;
		
		grainBillVolume.removeActionListener(this);
		waterVolume.removeActionListener(this);
		duration.removeChangeListener(this);
		grainTemp.removeChangeListener(this);

		grainBillVolume.setModel(getVolumesOptions(batch, Volume.Type.FERMENTABLES));
		waterVolume.setModel(getVolumesOptions(batch, Volume.Type.WATER));
		outputMashVolume.setText("");

		if (step != null)
		{
			grainBillVolume.setSelectedItem(mash.getGrainBillVol());
			waterVolume.setSelectedItem(mash.getWaterVol());
			duration.setValue(mash.getDuration());
			grainTemp.setValue(mash.getGrainTemp());

			outputMashVolume.setText("'"+mash.getOutputMashVolume()+"'");
			mashTemp.setText(String.format("%.1fC", mash.getMashTemp()));
		}

		grainBillVolume.addActionListener(this);
		waterVolume.addActionListener(this);
		duration.addChangeListener(this);
		grainTemp.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		SingleInfusionMash step = (SingleInfusionMash)getStep();

		if (e.getSource() == grainBillVolume)
		{
			step.setGrainBillVolume((String)grainBillVolume.getSelectedItem());
			triggerUiRefresh();
		}
		else if (e.getSource() == waterVolume)
		{
			step.setWaterVolume((String)waterVolume.getSelectedItem());
			triggerUiRefresh();
		}
		else if (e.getSource() == duration)
		{
			step.setDuration((Double)duration.getValue());
			triggerUiRefresh();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		SingleInfusionMash step = (SingleInfusionMash)getStep();

		if (e.getSource() == grainTemp)
		{
			step.setGrainTemp((Double)grainTemp.getValue());
			triggerUiRefresh();
		}
	}
}
