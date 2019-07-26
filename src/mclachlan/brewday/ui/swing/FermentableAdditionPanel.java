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
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class FermentableAdditionPanel extends JPanel implements ActionListener, ChangeListener
{
	private JSpinner weight;
	private JSpinner time;
	private JComboBox fermentable;
	private JButton increaseAmount, decreaseAmount;
	private IngredientAddition item;

	public FermentableAdditionPanel()
	{
		setLayout(new MigLayout());

		Vector<String> vec = new Vector<String>(
			Database.getInstance().getFermentables().keySet());
		Collections.sort(vec);
		fermentable = new JComboBox(vec);
		fermentable.addActionListener(this);

		weight = new JSpinner(new SpinnerNumberModel(0, 0, 99999, 1D));
		weight.addChangeListener(this);

		time = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1D));
		time.addChangeListener(this);

		JPanel topPanel = new JPanel(new MigLayout());

		topPanel.add(new JLabel(StringUtils.getUiString("fermentable.addition.name")));
		topPanel.add(fermentable, "wrap");

		topPanel.add(new JLabel(StringUtils.getUiString("fermentable.addition.weight")));
		topPanel.add(weight, "wrap");

		topPanel.add(new JLabel(StringUtils.getUiString("fermentable.addition.time")));
		topPanel.add(time, "wrap");

		this.add(topPanel, "wrap");

		JPanel buttons = new JPanel();

		increaseAmount = new JButton(StringUtils.getUiString("additions.+250g"));
		increaseAmount.addActionListener(this);

		decreaseAmount = new JButton(StringUtils.getUiString("additions.-250g"));
		decreaseAmount.addActionListener(this);

		buttons.add(increaseAmount);
		buttons.add(decreaseAmount);

		this.add(buttons, "wrap");
	}

	public void refresh(IngredientAddition item)
	{
		this.item = item;

		this.weight.removeChangeListener(this);
		this.fermentable.removeActionListener(this);
		this.time.removeChangeListener(this);

		this.weight.setValue(item.getWeight().get(Quantity.Unit.KILOGRAMS));
		this.fermentable.setSelectedItem(item.getName());
		this.time.setValue(item.getTime());

		this.weight.addChangeListener(this);
		this.fermentable.addActionListener(this);
		this.time.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == increaseAmount)
		{
			item.getWeight().set(item.getWeight().get(Quantity.Unit.GRAMS) +250);
		}
		else if (e.getSource() == decreaseAmount)
		{
			item.getWeight().set(Math.max(0, item.getWeight().get(Quantity.Unit.GRAMS) -250));
		}
		else if (e.getSource() == fermentable)
		{
			Fermentable newFermentable = Database.getInstance().getFermentables().get(
				fermentable.getSelectedItem());
			((FermentableAddition)item).setFermentable(newFermentable);
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
			this.item.getWeight().set((Double)weight.getValue(), Quantity.Unit.GRAMS);
		}
		SwingUi.instance.refreshProcessSteps();
	}
}
