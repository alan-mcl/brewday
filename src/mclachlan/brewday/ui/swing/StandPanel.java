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
import mclachlan.brewday.process.Stand;
import mclachlan.brewday.process.Volume;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class StandPanel extends ProcessStepPanel
{
	private JComboBox<String> inputVolume;
	private JLabel outputVolume;
	private JSpinner duration;

	public StandPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		inputVolume = new JComboBox<String>();
		inputVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("In:"), inputVolume, gbc);

		duration = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1.0));
		duration.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Duration (min):"), duration, gbc);

		outputVolume = new JLabel();
		dodgyGridBagShite(this, new JLabel("Out:"), outputVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		Stand stand = (Stand)step;

		inputVolume.setModel(getVolumesOptions(batch, Volume.Type.WORT));

		inputVolume.removeActionListener(this);
		duration.removeChangeListener(this);

		if (step != null)
		{
			inputVolume.setSelectedItem(stand.getInputVolume());
			outputVolume.setText("'" + stand.getOutputVolume() + "'");
			duration.setValue(stand.getDuration());
		}

		inputVolume.addActionListener(this);
		duration.removeChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Stand stand = (Stand)getStep();

		if (e.getSource() == inputVolume)
		{
			stand.setInputVolume((String)inputVolume.getSelectedItem());
			triggerUiRefresh();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		Stand stand = (Stand)getStep();

		if (e.getSource() == duration)
		{
			stand.setDuration((Double)duration.getValue());
		}
	}
}
