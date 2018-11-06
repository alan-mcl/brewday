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
import mclachlan.brewday.recipe.Recipe;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class SplitByPercentPanel extends ProcessStepPanel
{
	private JComboBox<String> inputVolume;
	private ComputedVolumePanel outputVolume, outputVolume2;
	private JSpinner percent;

	/*-------------------------------------------------------------------------*/
	public SplitByPercentPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		setLayout(new MigLayout());

		inputVolume = new JComboBox<String>();
		inputVolume.addActionListener(this);
		add(new JLabel("In:"));
		add(inputVolume, "wrap");

		percent = new JSpinner(new SpinnerNumberModel(50, 0, 100, 1D));
		percent.addChangeListener(this);
		add(new JLabel("Split %:"));
		add(percent, "wrap");

		outputVolume = new ComputedVolumePanel("Out #1");
		add(outputVolume, "span, wrap");

		outputVolume2 = new ComputedVolumePanel("Out #2");
		add(outputVolume2, "span, wrap");
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		SplitByPercent split = (SplitByPercent)step;

		inputVolume.setModel(getVolumesOptions(recipe, Volume.Type.WORT, Volume.Type.BEER));

		inputVolume.removeActionListener(this);
		percent.removeChangeListener(this);

		if (step != null)
		{
			inputVolume.setSelectedItem(split.getInputVolume());
			percent.setValue(split.getSplitPercent()*100);

			outputVolume.refresh(split.getOutputVolume(), recipe);
			outputVolume2.refresh(split.getOutputVolume2(), recipe);
		}

		inputVolume.addActionListener(this);
		percent.addChangeListener(this);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		SplitByPercent step = (SplitByPercent)getStep();

		if (e.getSource() == inputVolume)
		{
			step.setInputVolume((String)inputVolume.getSelectedItem());
			triggerUiRefresh();
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void stateChanged(ChangeEvent e)
	{
		SplitByPercent step = (SplitByPercent)getStep();

		if (e.getSource() == percent)
		{
			step.setOutputPercent((Double)percent.getValue() / 100);
			triggerUiRefresh();
		}
	}
}
