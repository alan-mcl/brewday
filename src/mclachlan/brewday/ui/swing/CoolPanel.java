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
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.process.Cool;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class CoolPanel extends ProcessStepPanel
{
	private JComboBox<String> inputVolume;
	private ComputedVolumePanel outputVolume;
	private JSpinner targetTemp;

	public CoolPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		setLayout(new MigLayout());

		inputVolume = new JComboBox<String>();
		inputVolume.addActionListener(this);
		add(new JLabel(StringUtils.getUiString("volumes.in")));
		add(inputVolume, "wrap");

		targetTemp = new JSpinner(new SpinnerNumberModel(20, 0, 9999, 0.1));
		targetTemp.addChangeListener(this);
		add(new JLabel(StringUtils.getUiString("cool.target.temp")));
		add(targetTemp, "wrap");

		outputVolume = new ComputedVolumePanel(StringUtils.getUiString("volumes.out"));
		add(outputVolume, "span, wrap");
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		Cool cool = (Cool)step;

		inputVolume.setModel(getVolumesOptions(recipe, Volume.Type.WORT, Volume.Type.BEER));

		inputVolume.removeActionListener(this);
		targetTemp.removeChangeListener(this);

		if (step != null)
		{
			inputVolume.setSelectedItem(cool.getInputVolume());
			outputVolume.refresh(cool.getOutputVolume(), recipe);
			targetTemp.setValue(cool.getTargetTemp());
		}

		inputVolume.addActionListener(this);
		targetTemp.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Cool cool = (Cool)getStep();

		if (e.getSource() == inputVolume)
		{
			cool.setInputVolume((String)inputVolume.getSelectedItem());
			triggerUiRefresh();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		Cool cool = (Cool)getStep();

		if (e.getSource() == targetTemp)
		{
			cool.setTargetTemp((Double)targetTemp.getValue());
			triggerUiRefresh();
		}
	}
}
