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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import javax.swing.*;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.process.Dilute;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class DilutePanel extends ProcessStepPanel
{
	private JComboBox<String> inputVolume;
	private ComputedVolumePanel outputVolume;

	public DilutePanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		setLayout(new MigLayout());

		inputVolume = new JComboBox<>();
		inputVolume.addActionListener(this);
		add(new JLabel(StringUtils.getUiString("volumes.in")));
		add(inputVolume, "wrap");

		outputVolume = new ComputedVolumePanel(StringUtils.getUiString("volumes.out"));
		add(outputVolume, "span, wrap");
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		Dilute dilute = (Dilute)step;

		inputVolume.setModel(getVolumesOptions(recipe, Volume.Type.WORT));

		inputVolume.removeActionListener(this);

		if (step != null)
		{
			inputVolume.setSelectedItem(dilute.getInputVolume());

			outputVolume.refresh(dilute.getOutputVolume(), recipe);
		}

		inputVolume.addActionListener(this);
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
}
