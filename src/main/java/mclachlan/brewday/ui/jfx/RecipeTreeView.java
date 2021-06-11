/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import java.util.function.*;
import javafx.scene.control.Label;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;

public class RecipeTreeView extends Pane
{
	private final RecipeTreeViewModel stepsTreeModel;

	/*-------------------------------------------------------------------------*/
	public RecipeTreeView(
		Supplier<Recipe> recipeSupplier,
		CardGroup stepCards)
	{
		TreeView<Label> stepsTree = new TreeView<>();
		stepsTreeModel = new RecipeTreeViewModel(stepsTree);
		stepsTree.setPrefSize(300, 650);

		this.getChildren().add(stepsTree);

		// ---- listeners

		stepsTree.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				if (newValue != null && newValue != oldValue)
				{
					Object value = RecipeTreeView.this.getValue(newValue.getValue());

					if (value instanceof ProcessStep)
					{
						String key = ((ProcessStep)value).getType().toString();

						if (stepCards != null)
						{
							stepCards.setVisible(key);
							ProcessStepPane child = (ProcessStepPane)stepCards.getChild(key);
							child.refresh((ProcessStep)value, recipeSupplier.get());
						}
					}
					else if (value instanceof IngredientAddition)
					{
						String key = ((IngredientAddition)value).getType().toString();
						if (stepCards != null)
						{
							stepCards.setVisible(key);
							IngredientAdditionPane child = (IngredientAdditionPane)stepCards.getChild(key);
							child.refresh((IngredientAddition)value, recipeSupplier.get());
						}
					}
					else
					{
						if (stepCards != null)
						{
							stepCards.setVisible(UiUtils.NONE);
						}
					}
				}
			});

	}

	/*-------------------------------------------------------------------------*/
	private Object getValue(Label value)
	{
		return stepsTreeModel.getValue(value);
	}

	public void setDirty(Object step)
	{
		stepsTreeModel.setDirty(step);
	}

	public void refresh(Recipe recipe)
	{
		stepsTreeModel.refresh(recipe);
	}

	public void removeStep(ProcessStep step)
	{
		stepsTreeModel.removeStep(step);
	}

	public void addStep(ProcessStep step)
	{
		stepsTreeModel.addStep(step);
	}

	public void addIngredientAddition(ProcessStep step, IngredientAddition ingredientAddition)
	{
		stepsTreeModel.addIngredientAddition(step, ingredientAddition);
	}

	public void removeIngredientAddition(ProcessStep step, IngredientAddition ingredientAddition)
	{
		stepsTreeModel.removeIngredientAddition(step, ingredientAddition);
	}
}
