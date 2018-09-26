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
import mclachlan.brewday.process.MashIn;
import mclachlan.brewday.process.Recipe;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class MashInPanel extends ProcessStepPanel
{
	private JComboBox<String> grainBillVolume, waterVolume;
	private JLabel mashTemp;
	private JSpinner duration, grainTemp;
	private ComputedVolumePanel outputPanel;

	public MashInPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		setLayout (new MigLayout());

		grainBillVolume = new JComboBox<String>();
		grainBillVolume.addActionListener(this);
		this.add(new JLabel("Grain Bill:"));
		this.add(grainBillVolume, "wrap");

		waterVolume = new JComboBox<String>();
		waterVolume.addActionListener(this);
		this.add(new JLabel("Strike water:"));
		this.add(waterVolume, "wrap");

		grainTemp = new JSpinner(new SpinnerNumberModel(66, 0, 100, 1D));
		grainTemp.addChangeListener(this);
		this.add(new JLabel("Grain temp (C):"));
		this.add(grainTemp, "wrap");

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
		MashIn mash = (MashIn)step;
		
		grainBillVolume.removeActionListener(this);
		waterVolume.removeActionListener(this);
		duration.removeChangeListener(this);
		grainTemp.removeChangeListener(this);

		grainBillVolume.setModel(getVolumesOptions(recipe, Volume.Type.FERMENTABLES));
		waterVolume.setModel(getVolumesOptions(recipe, Volume.Type.WATER));

		if (step != null)
		{
			grainBillVolume.setSelectedItem(mash.getGrainBillVol());
			waterVolume.setSelectedItem(mash.getWaterVol());
			duration.setValue(mash.getDuration());
			grainTemp.setValue(mash.getGrainTemp());

			outputPanel.refresh(mash.getOutputMashVolume(), recipe);
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
		MashIn step = (MashIn)getStep();

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
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		MashIn step = (MashIn)getStep();

		if (e.getSource() == grainTemp)
		{
			step.setGrainTemp((Double)grainTemp.getValue());
			triggerUiRefresh();
		}
		else if (e.getSource() == duration)
		{
			step.setDuration((Double)duration.getValue());
			triggerUiRefresh();
		}
	}
}
