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
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.process.Dilute;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class DilutePanel extends ProcessStepPanel
{
	private JComboBox<String> inputVolume;
	private ComputedVolumePanel outputVolume;
	private JSpinner volTarget, additionTemp;

	public DilutePanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		setLayout(new MigLayout());

		inputVolume = new JComboBox<String>();
		inputVolume.addActionListener(this);
		add(new JLabel("In:"));
		add(inputVolume, "wrap");

		volTarget = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 0.1));
		volTarget.addChangeListener(this);
		add(new JLabel("Volume target (l):"));
		add(volTarget, "wrap");

		additionTemp = new JSpinner(new SpinnerNumberModel(20, 0, 9999, 0.1));
		additionTemp.addChangeListener(this);
		add(new JLabel("Water addition temp (C):"));
		add(additionTemp, "wrap");

		outputVolume = new ComputedVolumePanel("Out");
		add(outputVolume, "span, wrap");
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		Dilute dilute = (Dilute)step;

		inputVolume.setModel(getVolumesOptions(recipe, Volume.Type.WORT));

		inputVolume.removeActionListener(this);
		additionTemp.removeChangeListener(this);
		volTarget.removeChangeListener(this);

		if (step != null)
		{
			inputVolume.setSelectedItem(dilute.getInputVolume());
			additionTemp.setValue(dilute.getAdditionTemp());
			volTarget.setValue(dilute.getVolumeTarget() /1000);

			outputVolume.refresh(dilute.getOutputVolume(), recipe);
		}

		inputVolume.addActionListener(this);
		additionTemp.addChangeListener(this);
		volTarget.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Dilute step = (Dilute)getStep();

		if (e.getSource() == inputVolume)
		{
			step.setInputVolume((String)inputVolume.getSelectedItem());
			triggerUiRefresh();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		Dilute step = (Dilute)getStep();

		if (e.getSource() == volTarget)
		{
			step.setVolumeTarget((Double)volTarget.getValue() *1000);
			triggerUiRefresh();
		}
		else if (e.getSource() == additionTemp)
		{
			step.setAdditionTemp((Double)additionTemp.getValue());
			triggerUiRefresh();
		}
	}
}
