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
import javax.swing.*;
import mclachlan.brewday.process.Batch;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.ProcessStep;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class PackagePanel extends ProcessStepPanel
{
	private JComboBox<String> inputVolume, outputVolume;
	private JSpinner packagingLoss;

	public PackagePanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		inputVolume = new JComboBox<String>();
		inputVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("In:"), inputVolume, gbc);

		packagingLoss = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 0.01));
		packagingLoss.addChangeListener(this);
		dodgyGridBagShite(this, new JLabel("Packaging loss (l):"), packagingLoss, gbc);

		outputVolume = new JComboBox<String>();
		outputVolume.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Out:"), outputVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		PackageStep pkg = (PackageStep)step;

		inputVolume.setModel(getVolumesOptions(batch));
		outputVolume.setModel(getVolumesOptions(batch));

		if (step != null)
		{
			inputVolume.setSelectedItem(pkg.getInputVolume());
			outputVolume.setSelectedItem(pkg.getOutputVolume());
			packagingLoss.setValue(pkg.getPackagingLoss() /1000);
		}
	}
}
