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
import javafx.scene.control.*;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class FermentableAdditionPane extends MigPane
{
	private TextField weight;
	private TextField time;
	private ComboBox fermentable;
	private Button increaseAmount, decreaseAmount;
	private IngredientAddition item;

	public FermentableAdditionPane()
	{
		Vector<String> vec = new Vector<String>(
			Database.getInstance().getFermentables().keySet());
		Collections.sort(vec);
		fermentable = new ComboBox(FXCollections.observableList(vec));

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
	}

	public void refresh(IngredientAddition item)
	{
		this.item = item;

		if (item != null)
		{
			weight.setText(""+item.getQuantity().get(Quantity.Unit.KILOGRAMS));
			time.setText(""+item.getTime().get(Quantity.Unit.MINUTES));
			fermentable.getSelectionModel().select(item);
		}
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
