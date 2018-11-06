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
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.process.Ferment;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class FermentPanel extends ProcessStepPanel
{
	private JComboBox<String> inputVolume;
	private ComputedVolumePanel outputVolume;
	private JSpinner fermTemp;
	private JLabel estFG;

	public FermentPanel(int dirtyFlag)
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

		fermTemp = new JSpinner(new SpinnerNumberModel(20.0, 0.0, 100.0, .1D));
		fermTemp.addChangeListener(this);
		add(new JLabel("Fermentation Temp (C):"));
		add(fermTemp, "wrap");

		estFG = new JLabel();
		add(new JLabel("Estimated FG:"));
		add(estFG, "wrap");

		outputVolume = new ComputedVolumePanel("Out");
		add(outputVolume, "span, wrap");
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		Ferment ferment = (Ferment)step;

		inputVolume.setModel(getVolumesOptions(recipe, Volume.Type.WORT));

		inputVolume.removeActionListener(this);
		fermTemp.removeChangeListener(this);

		if (step != null)
		{
			inputVolume.setSelectedItem(ferment.getInputVolume());
			outputVolume.refresh(ferment.getOutputVolume(), recipe);
			fermTemp.setValue(ferment.getTemperature());
			estFG.setText(
				String.format("%.3f",
					ferment.getEstimatedFinalGravity().get(
						DensityUnit.Unit.SPECIFIC_GRAVITY)));
		}

		inputVolume.addActionListener(this);
		fermTemp.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Ferment ferment = (Ferment)getStep();

		if (e.getSource() == inputVolume)
		{
			ferment.setInputVolume((String)inputVolume.getSelectedItem());
			triggerUiRefresh();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		Ferment ferment = (Ferment)getStep();

		if (e.getSource() == fermTemp)
		{
			ferment.setTemperature((Double)fermTemp.getValue());
			triggerUiRefresh();
		}
	}
}
