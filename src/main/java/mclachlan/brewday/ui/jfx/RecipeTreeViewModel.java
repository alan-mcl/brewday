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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.ui.UiUtils;

/**
 *
 */
class RecipeTreeViewModel
{
	private Recipe recipe;
	private TreeView<Label> treeView;
	private TreeItem<Label> root;
	private final Map<Object, Label> nodes = new HashMap<>();
	private final Map<Label, Object> values = new HashMap<>();
	private final Map<Object, TreeItem<Label>> treeItems = new HashMap<>();

	private final Map<ProcessStep.Type, Image> stepIcons = new HashMap<>();

	{
		stepIcons.put(ProcessStep.Type.MASH, Icons.mashIcon);
		stepIcons.put(ProcessStep.Type.MASH_INFUSION, Icons.mashInfusionIcon);
		stepIcons.put(ProcessStep.Type.LAUTER, Icons.lauterIcon);
		stepIcons.put(ProcessStep.Type.BATCH_SPARGE, Icons.batchSpargeIcon);
		stepIcons.put(ProcessStep.Type.BOIL, Icons.boilIcon);
		stepIcons.put(ProcessStep.Type.HEAT, Icons.heatIcon);
		stepIcons.put(ProcessStep.Type.COOL, Icons.coolIcon);
		stepIcons.put(ProcessStep.Type.SPLIT, Icons.splitIcon);
		stepIcons.put(ProcessStep.Type.COMBINE, Icons.combineIcon);
		stepIcons.put(ProcessStep.Type.DILUTE, Icons.diluteIcon);
		stepIcons.put(ProcessStep.Type.STAND, Icons.standIcon);
		stepIcons.put(ProcessStep.Type.FERMENT, Icons.fermentIcon);
		stepIcons.put(ProcessStep.Type.PACKAGE, Icons.packageIcon);
	}


	/*-------------------------------------------------------------------------*/
	public RecipeTreeViewModel(TreeView<Label> treeView)
	{
		this.treeView = treeView;
		this.root = treeView.getRoot();
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Recipe recipe)
	{
		this.recipe = recipe;

		this.treeView.setRoot(null);
		root = getTreeItem(recipe.getName(), recipe, Icons.recipeIcon);
		this.treeView.setRoot(root);
		root.setExpanded(true);

		if (JfxUi.getInstance().isDirty(recipe))
		{
			this.setDirty(recipe);
		}

		List<ProcessStep> steps = new ArrayList<>(recipe.getSteps());

		for (ProcessStep step : steps)
		{
			addStep(step);
			if (JfxUi.getInstance().isDirty(step))
			{
				this.setDirty(step);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public void sortTree()
	{
		TreeItem<Label> selectedItem = treeView.getSelectionModel().getSelectedItem();

		List<ProcessStep> recipeStepsOrder = recipe.getSteps();

		root.getChildren().sort((o1, o2) ->
			{
				ProcessStep step1 = (ProcessStep)values.get(o1.getValue());
				ProcessStep step2 = (ProcessStep)values.get(o2.getValue());

				return recipeStepsOrder.indexOf(step1) - recipeStepsOrder.indexOf(step2);
			});

		for (ProcessStep step : recipeStepsOrder)
		{
			step.getIngredientAdditions().sort(UiUtils.getIngredientAdditionComparator());
		}

		treeView.getSelectionModel().select(selectedItem);
	}

	/*-------------------------------------------------------------------------*/
	public void addStep(ProcessStep step)
	{
		TreeItem<Label> stepItem = getTreeItem(getLabelText(step), step, getStepIconImage(step));

		List<IngredientAddition> ingredients = new ArrayList<>(step.getIngredientAdditions());
		for (IngredientAddition addition : ingredients)
		{
			addIngredientAddition(step, addition);
			if (JfxUi.getInstance().isDirty(addition))
			{
				this.setDirty(addition);
			}
		}

		root.getChildren().add(stepItem);

		sortTree();
	}

	/*-------------------------------------------------------------------------*/
	protected Image getStepIconImage(ProcessStep step)
	{
		Image image = stepIcons.get(step.getType());

		if (image == null)
		{
			image = Icons.stepIcon;
		}

		return image;
	}

	/*-------------------------------------------------------------------------*/
	public void removeStep(ProcessStep step)
	{
		TreeItem<Label> stepItem = treeItems.get(step);

		root.getChildren().remove(stepItem);
	}

	/*-------------------------------------------------------------------------*/
	public void addIngredientAddition(ProcessStep step, IngredientAddition addition)
	{
		TreeItem<Label> stepItem = treeItems.get(step);

		Image icon;
		if (addition instanceof WaterAddition)
		{
			icon = Icons.waterIcon;
		}
		else if (addition instanceof FermentableAddition)
		{
			icon = UiUtils.getFermentableIcon(((FermentableAddition)addition).getFermentable());
		}
		else if (addition instanceof HopAddition)
		{
			icon = Icons.hopsIcon;
		}
		else if (addition instanceof YeastAddition)
		{
			icon = Icons.yeastIcon;
		}
		else if (addition instanceof MiscAddition)
		{
			icon = UiUtils.getMiscIcon(((MiscAddition)addition).getMisc());
		}
		else
		{
			throw new BrewdayException("unrecognised: " + addition);
		}

		TreeItem<Label> additionItem = getTreeItem(getLabelText(addition), addition, icon);
		stepItem.getChildren().add(additionItem);
	}

	/*-------------------------------------------------------------------------*/
	public void removeIngredientAddition(ProcessStep step, IngredientAddition addition)
	{
		TreeItem<Label> stepItem = treeItems.get(step);
		TreeItem<Label> additionItem = treeItems.get(addition);
		stepItem.getChildren().remove(additionItem);
	}

	/*-------------------------------------------------------------------------*/
	public Object getValue(Label label)
	{
		return this.values.get(label);
	}

	/*-------------------------------------------------------------------------*/
	public void setDirty(Object obj)
	{
		Label label = nodes.get(obj);

		if (label != null)
		{
			treeView.getRoot().getValue().setStyle("-fx-font-weight: bold;");
			label.setStyle("-fx-font-weight: bold;");

			refreshNode(obj);
		}

		if (obj instanceof IngredientAddition)
		{
			TreeItem<Label> parent = treeItems.get(obj).getParent();
			parent.getValue().setStyle("-fx-font-weight: bold;");
		}

		sortTree();
	}

	/*-------------------------------------------------------------------------*/
	public void clearAllDirty()
	{
		for (Label l : nodes.values())
		{
			l.setStyle("");
		}
	}

	/*-------------------------------------------------------------------------*/
	public void refreshNode(Object value)
	{
		Label label = nodes.get(value);
		label.setText(getLabelText(value));
	}

	/*-------------------------------------------------------------------------*/
	private String getLabelText(Object value)
	{
		if (value instanceof IngredientAddition)
		{
			return value.toString();
		}
		else if (value instanceof ProcessStep)
		{
			return ((ProcessStep)value).getName();
		}
		else if (value instanceof Recipe)
		{
			return ((Recipe)value).getName();
		}
		else
		{
			throw new BrewdayException("invalid "+value);
		}
	}

	/*-------------------------------------------------------------------------*/
	private TreeItem<Label> getTreeItem(String text, Object value, Image icon)
	{
		Label label = new Label(text);
		TreeItem<Label> result = new TreeItem<>(label, JfxUi.getImageView(icon, 24));

		nodes.put(value, label);
		values.put(label, value);
		treeItems.put(value, result);

		return result;
	}

	/*-------------------------------------------------------------------------*/
	private static class IngredientAdditionComparator implements Comparator<IngredientAddition>
	{
		private static final Map<IngredientAddition.Type, Integer> additionTypeOrder = new HashMap<>();

		static
		{
			additionTypeOrder.put(IngredientAddition.Type.WATER, 1);
			additionTypeOrder.put(IngredientAddition.Type.FERMENTABLES, 2);
			additionTypeOrder.put(IngredientAddition.Type.YEAST, 3);
			additionTypeOrder.put(IngredientAddition.Type.MISC, 4);
			additionTypeOrder.put(IngredientAddition.Type.HOPS, 5);
		}

		@Override
		public int compare(IngredientAddition a1, IngredientAddition a2)
		{
			// order by ingredient type, ascending
			int x = additionTypeOrder.get(a1.getType()) - additionTypeOrder.get(a2.getType());

			if (x != 0) return x;

			// order by time, descending
			x = (int)a2.getTime().get() - (int)a1.getTime().get();

			if (x != 0) return x;

			// order by weight, ascending
			x = (int)a1.getQuantity().get() - (int)a2.getQuantity().get();

			if (x != 0) return x;

			// otherwise, order by name
			return a1.getName().compareTo(a2.getName());
		}
	}
}
