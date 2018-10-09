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
import mclachlan.brewday.process.Recipe;
import mclachlan.brewday.process.BatchSparge;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class BatchSpargePanel extends ProcessStepPanel
{
	private JComboBox<String> mashVolume, wortVolume;
	private ComputedVolumePanel outputSpargeRunnings, outputMash, outputCombinedWortVolume;

	public BatchSpargePanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		setLayout(new MigLayout());

		mashVolume = new JComboBox<String>();
		mashVolume.addActionListener(this);
		add(new JLabel("Mash to sparge:"));
		add(mashVolume, "wrap");

		wortVolume = new JComboBox<String>();
		wortVolume.addActionListener(this);
		add(new JLabel("Wort already collected:"));
		add(wortVolume, "wrap");

		outputSpargeRunnings = new ComputedVolumePanel("Sparge runnings");
		add(outputSpargeRunnings, "span, wrap");

		outputCombinedWortVolume = new ComputedVolumePanel("Combined wort out");
		add(outputCombinedWortVolume, "span, wrap");

		outputMash = new ComputedVolumePanel("Lautered mash");
		add(outputMash, "span, wrap");
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		mashVolume.setModel(getVolumesOptions(recipe, Volume.Type.MASH));
		wortVolume.setModel(getVolumesOptions(recipe, Volume.Type.WORT));

		mashVolume.removeActionListener(this);
		wortVolume.removeActionListener(this);

		if (step != null)
		{
			BatchSparge bs = (BatchSparge)step;
			wortVolume.setSelectedItem(bs.getWortVolume());
			mashVolume.setSelectedItem(bs.getMashVolume());

			outputSpargeRunnings.refresh(bs.getOutputSpargeRunnings(), recipe);
			outputCombinedWortVolume.refresh(bs.getOutputCombinedWortVolume(), recipe);
			outputMash.refresh(bs.getOutputMashVolume(), recipe);
		}

		mashVolume.addActionListener(this);
		wortVolume.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		BatchSparge step = (BatchSparge)getStep();

		if (e.getSource() == mashVolume)
		{
			step.setMashVolume((String)mashVolume.getSelectedItem());
			triggerUiRefresh();
		}
		else if (e.getSource() == wortVolume)
		{
			step.setWortVolume((String)wortVolume.getSelectedItem());
			triggerUiRefresh();
		}
	}
}
