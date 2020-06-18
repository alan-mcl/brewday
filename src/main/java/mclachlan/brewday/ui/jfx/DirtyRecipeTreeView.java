package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.*;

/**
 *
 */
class DirtyRecipeTreeView extends TreeView<Label>
{
	private TreeItem<Label> root;
	private Map<Object, Label> nodes = new HashMap<>();
	private Map<Label, Object> values = new HashMap<>();

	/*-------------------------------------------------------------------------*/
	public void refresh(Recipe recipe)
	{
		this.setRoot(null);
		root = getTreeItem(recipe.getName(), recipe, JfxUi.recipeIcon);
		this.setRoot(root);
		root.setExpanded(true);

		List<ProcessStep> steps = recipe.getSteps();

		for (ProcessStep step : steps)
		{
			addStep(step);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void addStep(ProcessStep step)
	{
		TreeItem<Label> stepItem = getTreeItem(getLabelText(step), step, JfxUi.stepIcon);

		for (IngredientAddition addition : step.getIngredients())
		{
			Image icon;
			if (addition instanceof WaterAddition)
			{
				icon = JfxUi.waterIcon;
			}
			else if (addition instanceof FermentableAddition)
			{
				icon = JfxUi.grainsIcon;
			}
			else if (addition instanceof HopAddition)
			{
				icon = JfxUi.hopsIcon;
			}
			else if (addition instanceof YeastAddition)
			{
				icon = JfxUi.yeastIcon;
			}
			else if (addition instanceof MiscAddition)
			{
				icon = JfxUi.miscIcon;
			}
			else
			{
				throw new BrewdayException("unrecognised: " + addition);
			}

			TreeItem<Label> additionItem = getTreeItem(getLabelText(addition), addition, icon);
			stepItem.getChildren().add(additionItem);
		}

		root.getChildren().add(stepItem);
	}

	public Object getValue(Label label)
	{
		return this.values.get(label);
	}

	public void setDirty(Object obj)
	{
		Label label = nodes.get(obj);

		if (label != null)
		{
			getRoot().getValue().setStyle("-fx-font-weight: bold;");
			label.setStyle("-fx-font-weight: bold;");

			refreshNode(obj);
		}
	}

	public void clearAllDirty()
	{
		for (Label l : nodes.values())
		{
			l.setStyle("");
		}
	}

	public void refreshNode(Object value)
	{
		Label label = nodes.get(value);
		label.setText(getLabelText(value));
	}

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

	private TreeItem<Label> getTreeItem(String text, Object value, Image icon)
	{
		Label label = new Label(text);
		TreeItem<Label> result = new TreeItem<>(label, JfxUi.getImageView(icon, 24));

		nodes.put(value, label);
		values.put(label, value);

		return result;
	}
}
