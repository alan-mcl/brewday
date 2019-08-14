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
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class PackagePanel extends ProcessStepPanel
{
	private JComboBox<String> inputVolume, style;
	private JTextField outputVolume;
	private JSpinner packagingLoss;

	public PackagePanel(int dirtyFlag)
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

		style = new JComboBox<>();
		style.addActionListener(this);
		add(new JLabel(StringUtils.getUiString("recipe.style")));
		add(style, "wrap");

		packagingLoss = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 0.01));
		packagingLoss.addChangeListener(this);
		add(new JLabel(StringUtils.getUiString("package.loss")));
		add(packagingLoss, "wrap");

		outputVolume = new JTextField(30);
		outputVolume.addActionListener(this);
		add(new JLabel(StringUtils.getUiString("package.beer.name")), "wrap");
		add(outputVolume, "span, wrap");
	}

	@Override
	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{
		PackageStep pkg = (PackageStep)step;

		inputVolume.setModel(getVolumesOptions(recipe, Volume.Type.BEER));
		inputVolume.removeActionListener(this);
		outputVolume.removeActionListener(this);
		style.removeActionListener(this);

		outputVolume.setText("");

		Vector<String> vec = new Vector<>(
			Database.getInstance().getStyles().keySet());
		Collections.sort(vec);
		this.style.setModel(new DefaultComboBoxModel<>(vec));

		if (step != null)
		{
			style.setSelectedItem(pkg.getStyleId());
			inputVolume.setSelectedItem(pkg.getInputVolume());
			outputVolume.setText(pkg.getOutputVolume());
			packagingLoss.setValue(pkg.getPackagingLoss().get(Quantity.Unit.LITRES));
		}

		inputVolume.addActionListener(this);
		outputVolume.addActionListener(this);
		style.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		PackageStep pkg = (PackageStep)getStep();

		if (e.getSource() == inputVolume)
		{
			pkg.setInputVolume((String)inputVolume.getSelectedItem());
			triggerUiRefresh();
		}
		else if (e.getSource() == outputVolume)
		{
			pkg.setOutputVolume(outputVolume.getText());
			triggerUiRefresh();
		}
		else if (e.getSource() == style)
		{
			pkg.setStyleId((String)style.getSelectedItem());
			triggerUiRefresh();
		}

	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		PackageStep pkg = (PackageStep)getStep();

		if (e.getSource() == packagingLoss)
		{
			pkg.setPackagingLoss(new VolumeUnit((Double)packagingLoss.getValue()*1000));
			triggerUiRefresh();
		}
	}
}
