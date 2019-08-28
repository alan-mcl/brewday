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
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.MiscAddition;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class MiscAdditionPanel extends JPanel implements ActionListener, ChangeListener
{
	private JComboBox misc;
	private JSpinner time, weight;
	private JButton increaseAmount, decreaseAmount;
	private MiscAddition item;

	public MiscAdditionPanel()
	{
		setLayout(new MigLayout());

		Vector<String> vec = new Vector<>(
			Database.getInstance().getMiscs().keySet());
		Collections.sort(vec);
		misc = new JComboBox(vec);

		weight = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1D));
		weight.addChangeListener(this);

		time = new JSpinner(new SpinnerNumberModel(14, 0, 9999, 1D));
		time.addChangeListener(this);

		JPanel topPanel = new JPanel(new MigLayout());
		topPanel.add(new JLabel(StringUtils.getUiString("misc.misc")));
		topPanel.add(misc, "wrap");

		topPanel.add(new JLabel(StringUtils.getUiString("misc.weight")));
		topPanel.add(weight, "wrap");

		topPanel.add(new JLabel(StringUtils.getUiString("misc.time")));
		topPanel.add(time, "wrap");

		this.add(topPanel, "wrap");

		JPanel buttons = new JPanel();

		increaseAmount = new JButton(StringUtils.getUiString("additions.+1g"));
		increaseAmount.addActionListener(this);

		decreaseAmount = new JButton(StringUtils.getUiString("additions.-1g"));
		decreaseAmount.addActionListener(this);

		buttons.add(increaseAmount);
		buttons.add(decreaseAmount);

		this.add(buttons, "wrap");
	}

	public void refresh(MiscAddition item)
	{
		this.item = item;

		this.misc.removeActionListener(this);
		this.time.removeChangeListener(this);
		this.weight.removeChangeListener(this);

		this.misc.setSelectedItem(item.getName());
		this.weight.setValue(item.getQuantity().get(Quantity.Unit.GRAMS));
		this.time.setValue(item.getTime());

		this.misc.addActionListener(this);
		this.time.addChangeListener(this);
		this.weight.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == increaseAmount)
		{
			item.getQuantity().set(item.getQuantity().get(Quantity.Unit.GRAMS) +1);
		}
		else if (e.getSource() == decreaseAmount)
		{
			item.getQuantity().set(Math.max(0, item.getQuantity().get(Quantity.Unit.GRAMS) -1));
		}
		else if (e.getSource() == misc)
		{
			Misc newMisc = Database.getInstance().getMiscs().get(
				(String)misc.getSelectedItem());
			((MiscAddition)item).setMisc(newMisc);
		}

		SwingUi.instance.refreshProcessSteps();
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == time)
		{
			this.item.setTime((Double)time.getValue());
		}
		else if (e.getSource() == weight)
		{
			this.item.getQuantity().set((Double)weight.getValue());
		}
		SwingUi.instance.refreshProcessSteps();
	}
}
