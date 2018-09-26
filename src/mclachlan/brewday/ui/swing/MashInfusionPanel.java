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
import mclachlan.brewday.process.*;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class MashInfusionPanel extends ProcessStepPanel
{
	private JComboBox<String> inputMashVolume, waterVolume;
	private JLabel mashTemp;
	private JSpinner duration;
	private ComputedVolumePanel outputPanel;

	public MashInfusionPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		setLayout (new MigLayout());

		inputMashVolume = new JComboBox<String>();
		inputMashVolume.addActionListener(this);
		this.add(new JLabel("Mash volume in:"));
		this.add(inputMashVolume, "wrap");

		waterVolume = new JComboBox<String>();
		waterVolume.addActionListener(this);
		this.add(new JLabel("Infusion water:"));
		this.add(waterVolume, "wrap");

		mashTemp = new JLabel();
		this.add(new JLabel("Mash temp (C):"));
		this.add(mashTemp, "wrap");

		duration = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1D));
		duration.addChangeListener(this);
		this.add(new JLabel("Duration (min):"));
		this.add(duration, "wrap");

		outputPanel = new ComputedVolumePanel("Mash volume created");

		this.add(outputPanel, "span, wrap");
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		MashInfusion mash = (MashInfusion)step;
		
		inputMashVolume.removeActionListener(this);
		waterVolume.removeActionListener(this);
		duration.removeChangeListener(this);

		inputMashVolume.setModel(getVolumesOptions(recipe, Volume.Type.MASH));
		waterVolume.setModel(getVolumesOptions(recipe, Volume.Type.WATER));

		if (step != null)
		{
			inputMashVolume.setSelectedItem(mash.getInputMashVolume());
			waterVolume.setSelectedItem(mash.getWaterVol());
			duration.setValue(mash.getDuration());

			outputPanel.refresh(mash.getOutputMashVolume(), recipe);
			mashTemp.setText(String.format("%.1fC", mash.getMashTemp()));
		}

		inputMashVolume.addActionListener(this);
		waterVolume.addActionListener(this);
		duration.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		MashInfusion step = (MashInfusion)getStep();

		if (e.getSource() == inputMashVolume)
		{
			step.setInputMashVolume((String)inputMashVolume.getSelectedItem());
			triggerUiRefresh();
		}
		else if (e.getSource() == waterVolume)
		{
			step.setWaterVolume((String)waterVolume.getSelectedItem());
			triggerUiRefresh();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		MashInfusion step = (MashInfusion)getStep();

		if (e.getSource() == duration)
		{
			step.setDuration((Double)duration.getValue());
			triggerUiRefresh();
		}
	}
}
