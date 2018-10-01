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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mclachlan.brewday.process.Recipe;
import mclachlan.brewday.recipe.AdditionSchedule;
import mclachlan.brewday.recipe.WaterAddition;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class WaterAdditionPanel extends JPanel implements ActionListener, ChangeListener
{
	private JTextField name;
	private JSpinner volume, temperature, time;
	private Recipe recipe;
	private WaterAddition water;
	private AdditionSchedule schedule;

	/*-------------------------------------------------------------------------*/
	public WaterAdditionPanel()
	{
		setLayout(new MigLayout());

		name = new JTextField();
		name.addActionListener(this);
		name.setEditable(false);
		add(new JLabel("Name:"));
		add(name, "wrap");

		time = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1D));
		time.addChangeListener(this);
		add(new JLabel("Time (min):"));
		add(time, "wrap");

		volume = new JSpinner(new SpinnerNumberModel(0, 0, 999, 0.1));
		volume.addChangeListener(this);
		add(new JLabel("Volume (l):"));
		add(volume, "wrap");

		temperature = new JSpinner(new SpinnerNumberModel(0,0,100,0.1));
		temperature.addChangeListener(this);
		add(new JLabel("Temperature (C):"));
		add(temperature, "wrap");
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(AdditionSchedule schedule, Recipe recipe)
	{
		this.schedule = schedule;
		this.water = (WaterAddition)recipe.getVolumes().getVolume(schedule.getIngredientAddition());
		this.recipe = recipe;

		this.name.removeActionListener(this);
		this.volume.removeChangeListener(this);
		this.temperature.removeChangeListener(this);
		this.time.removeChangeListener(this);

		this.name.setText(this.water.getName());
		this.volume.setValue(this.water.getVolume() /1000);
		this.temperature.setValue(this.water.getTemperature());
		this.time.setValue(schedule.getTime());

		this.name.addActionListener(this);
		this.volume.addChangeListener(this);
		this.temperature.addChangeListener(this);
		this.time.addChangeListener(this);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == volume)
		{
			this.water.setVolume((Double)volume.getValue() * 1000);
			SwingUi.instance.refreshRecipesPanel();
		}
		else if (e.getSource() == temperature)
		{
			this.water.setTemperature((Double)temperature.getValue());
			SwingUi.instance.refreshRecipesPanel();
		}
		else if (e.getSource() == time)
		{
			this.schedule.setTime((Double)time.getValue());
			SwingUi.instance.refreshRecipesPanel();
		}
	}
}
