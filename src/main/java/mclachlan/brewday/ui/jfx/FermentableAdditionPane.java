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

package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javax.swing.*;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.IngredientAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class FermentableAdditionPane extends MigPane
{
	private TextField weight;
	private TextField time;
	private ChoiceBox fermentable;
	private Button increaseAmount, decreaseAmount;
	private IngredientAddition item;

	public FermentableAdditionPane()
	{
		Vector<String> vec = new Vector<String>(
			Database.getInstance().getFermentables().keySet());
		Collections.sort(vec);
		fermentable = new ChoiceBox(FXCollections.observableList(vec));

		weight = new TextField();

		time = new TextField();

		MigPane topPane = new MigPane();

		topPane.add(new Label(StringUtils.getUiString("fermentable.addition.name")));
		topPane.add(fermentable, "wrap");

		topPane.add(new Label(StringUtils.getUiString("fermentable.addition.weight")));
		topPane.add(weight, "wrap");

		topPane.add(new Label(StringUtils.getUiString("fermentable.addition.time")));
		topPane.add(time, "wrap");

		this.add(topPane, "wrap");

		JPanel buttons = new JPanel();

//		increaseAmount = new Button(StringUtils.getUiString("additions.+250g"));
//		decreaseAmount = new Button(StringUtils.getUiString("additions.-250g"));

//		buttons.add(increaseAmount);
//		buttons.add(decreaseAmount);
//
//		this.add(buttons, "wrap");
	}

	public void refresh(IngredientAddition item)
	{
		this.item = item;

//		this.weight.removeChangeListener(this);
//		this.fermentable.removeActionListener(this);
//		this.time.removeChangeListener(this);
//
//		this.weight.setValue(item.getQuantity().get(Quantity.Unit.KILOGRAMS));
//		this.fermentable.setSelectedItem(item.getName());
//		this.time.setValue(item.getTime().get(Quantity.Unit.MINUTES));
//
//		this.weight.addChangeListener(this);
//		this.fermentable.addActionListener(this);
//		this.time.addChangeListener(this);
	}

	/*@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == increaseAmount)
		{
			item.getQuantity().set(item.getQuantity().get(Quantity.Unit.GRAMS) +250);
		}
		else if (e.getSource() == decreaseAmount)
		{
			item.getQuantity().set(Math.max(0, item.getQuantity().get(Quantity.Unit.GRAMS) -250));
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
			this.item.setTime(new TimeUnit((Double)time.getValue(), Quantity.Unit.MINUTES, false));
		}
		else if (e.getSource() == weight)
		{
			this.item.getQuantity().set((Double)weight.getValue(), Quantity.Unit.KILOGRAMS);
		}
		SwingUi.instance.refreshProcessSteps();
	}*/
}
