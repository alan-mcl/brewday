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
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.process.Batch;

/**
 *
 */
public class WaterPanel extends JPanel implements ActionListener, ChangeListener
{
	private JSpinner volume, temperature;
	private Batch batch;
	private Water water;

	public WaterPanel()
	{
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = EditorPanel.createGridBagConstraints();

		volume = new JSpinner(new SpinnerNumberModel(0, 0, 999, 0.1));
		volume.addChangeListener(this);
		EditorPanel.dodgyGridBagShite(this, new JLabel("Volume (l):"), volume, gbc);

		temperature = new JSpinner(new SpinnerNumberModel(0,0,100,0.1));
		temperature.addChangeListener(this);
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.gridx=0;
		gbc.gridy++;
		add(new JLabel("Temperature (C):"), gbc);
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.gridx++;
		add(temperature, gbc);
	}

	public void refresh(Water water, Batch batch)
	{
		this.water = water;
		this.batch = batch;

		this.volume.removeChangeListener(this);
		this.temperature.removeChangeListener(this);

		this.volume.setValue(this.water.getVolume() /1000);
		this.temperature.setValue(this.water.getTemperature());

		this.volume.addChangeListener(this);
		this.temperature.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		this.water.setVolume((Double)volume.getValue() *1000);
		this.water.setTemperature((Double)temperature.getValue());

		SwingUi.instance.refreshComputedVolumes();
	}
}
