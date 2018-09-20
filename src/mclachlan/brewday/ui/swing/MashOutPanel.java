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
import mclachlan.brewday.process.Recipe;
import mclachlan.brewday.process.MashOut;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class MashOutPanel extends ProcessStepPanel
{
	private JComboBox<String> mashVolume;
	private ComputedVolumePanel outputWortVolume;
	private JSpinner tunLoss;

	public MashOutPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected void buildUiInternal(GridBagConstraints gbc)
	{
		setLayout(new MigLayout());

		mashVolume = new JComboBox<String>();
		mashVolume.addActionListener(this);
		add(new JLabel("Mash:"));
		add(mashVolume, "wrap");

		tunLoss = new JSpinner(new SpinnerNumberModel(3, 0, 9999, 0.1));
		tunLoss.addChangeListener(this);
		add(new JLabel("Tun Loss (l):"));
		add(tunLoss, "wrap");

		outputWortVolume = new ComputedVolumePanel("Wort volume created");
		add(outputWortVolume, "span, wrap");
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		MashOut mashOut = (MashOut)step;

		mashVolume.setModel(getVolumesOptions(recipe, Volume.Type.MASH));

		mashVolume.removeActionListener(this);
		tunLoss.removeChangeListener(this);

		if (step != null)
		{
			mashVolume.setSelectedItem(mashOut.getMashVolume());
			outputWortVolume.refresh(mashOut.getOutputWortVolume(), recipe);
			tunLoss.setValue(mashOut.getTunLoss() / 1000);
		}

		mashVolume.addActionListener(this);
		tunLoss.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		MashOut step = (MashOut)getStep();

		if (e.getSource() == mashVolume)
		{
			step.setMashVolume((String)mashVolume.getSelectedItem());
			triggerUiRefresh();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		MashOut step = (MashOut)getStep();

		if (e.getSource() == tunLoss)
		{
			step.setTunLoss((Double)tunLoss.getValue() * 1000);
			triggerUiRefresh();
		}
	}
}
