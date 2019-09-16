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
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;
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

		Vector<String> vec = new Vector<>(
			Database.getInstance().getYeasts().keySet());
		Collections.sort(vec);
		yeast = new JComboBox(vec);

		weight = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1D));
		weight.addChangeListener(this);

		time = new JSpinner(new SpinnerNumberModel(14, 0, 9999, 1D));
		time.addChangeListener(this);

		JPanel topPanel = new JPanel(new MigLayout());
		topPanel.add(new JLabel(StringUtils.getUiString("yeast.yeast")));
		topPanel.add(yeast, "wrap");

		topPanel.add(new JLabel(StringUtils.getUiString("yeast.weight")));
		topPanel.add(weight, "wrap");

		topPanel.add(new JLabel(StringUtils.getUiString("yeast.time")));
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

	public void refresh(IngredientAddition item)
	{
		this.item = item;

		this.yeast.removeActionListener(this);
		this.time.removeChangeListener(this);
		this.weight.removeChangeListener(this);

		this.yeast.setSelectedItem(item.getName());
		this.weight.setValue(item.getQuantity().get(Quantity.Unit.GRAMS));
		this.time.setValue(item.getTime().get(Quantity.Unit.DAYS));

		this.yeast.addActionListener(this);
		this.time.addChangeListener(this);
		this.weight.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == increaseAmount)
		{
			item.setQuantity(new WeightUnit(item.getQuantity().get(Quantity.Unit.GRAMS) +1));
		}
		else if (e.getSource() == decreaseAmount)
		{
			item.setQuantity(new WeightUnit(Math.max(0, item.getQuantity().get(Quantity.Unit.GRAMS) -1)));
		}
		else if (e.getSource() == yeast)
		{
			Yeast newYeast = Database.getInstance().getYeasts().get(
				(String)yeast.getSelectedItem());
			((YeastAddition)item).setYeast(newYeast);
		}

		SwingUi.instance.refreshProcessSteps();
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == time)
		{
			this.item.setTime(new TimeUnit((Double)time.getValue(), Quantity.Unit.DAYS, false));
		}
		else if (e.getSource() == weight)
		{
			this.item.setQuantity(new WeightUnit((Double)weight.getValue()));
		}
		SwingUi.instance.refreshProcessSteps();
	}
}
