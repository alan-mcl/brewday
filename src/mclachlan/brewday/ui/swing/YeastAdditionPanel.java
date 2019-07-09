/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the yeaste that it will be useful,
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
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.YeastAddition;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class YeastAdditionPanel extends JPanel implements ActionListener, ChangeListener
{
	private JComboBox yeast;
	private JSpinner time, weight;
	private JButton increaseAmount, decreaseAmount;
	private IngredientAddition item;

	public YeastAdditionPanel()
	{
		setLayout(new MigLayout());

		Vector<String> vec = new Vector<String>(
			Database.getInstance().getReferenceYeasts().keySet());
		Collections.sort(vec);
		yeast = new JComboBox(vec);

		weight = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1D));
		weight.addChangeListener(this);

		time = new JSpinner(new SpinnerNumberModel(14, 0, 9999, 1D));
		time.addChangeListener(this);

		JPanel topPanel = new JPanel(new MigLayout());
		topPanel.add(new JLabel("Yeast:"));
		topPanel.add(yeast, "wrap");

		topPanel.add(new JLabel("Weight (g):"));
		topPanel.add(weight, "wrap");

		topPanel.add(new JLabel("Time (days):"));
		topPanel.add(time, "wrap");

		this.add(topPanel, "wrap");

		JPanel buttons = new JPanel();

		increaseAmount = new JButton("+1g");
		increaseAmount.addActionListener(this);

		decreaseAmount = new JButton("-1g");
		decreaseAmount.addActionListener(this);

		buttons.add(increaseAmount);
		buttons.add(decreaseAmount);

		this.add(buttons, "wrap");
	}

	public void refresh(IngredientAddition item)
	{
		this.item = item;

		this.yeast.removeActionListener(this);
		this.time.removeChangeListener(this);
		this.weight.removeChangeListener(this);

		this.yeast.setSelectedItem(item.getName());
		this.weight.setValue(item.getWeight());
		this.time.setValue(item.getTime());

		this.yeast.addActionListener(this);
		this.time.addChangeListener(this);
		this.weight.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == increaseAmount)
		{
			item.setWeight(item.getWeight() +1);
		}
		else if (e.getSource() == decreaseAmount)
		{
			item.setWeight(Math.max(0, item.getWeight() -1));
		}
		else if (e.getSource() == yeast)
		{
			Yeast newYeast = Database.getInstance().getReferenceYeasts().get(yeast.getSelectedItem());
			((YeastAddition)item).setYeast(newYeast);
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
			this.item.setWeight((Double)weight.getValue());
		}
		SwingUi.instance.refreshProcessSteps();
	}
}
