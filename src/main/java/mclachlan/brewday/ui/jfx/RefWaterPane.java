package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.scene.control.TableColumn;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;

/**
 *
 */
public class RefWaterPane extends RefIngredientPane<Water>
{
	public RefWaterPane(String dirtyFlag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "water", JfxUi.addWater);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Water createDuplicateItem(Water current, String newName)
	{
		Water result = new Water(current);
		result.setName(newName);
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Water createNewItem(String name)
	{
		return new Water(name);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Map<String, Water> getMap(Database database)
	{
		return database.getWaters();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected TableColumn<Water, String>[] getTableColumns(String labelPrefix)
	{
		return (TableColumn<Water, String>[])new TableColumn[]
			{
				getPropertyValueColumn(labelPrefix + ".calcium.abbr", "calcium"),
				getPropertyValueColumn(labelPrefix + ".bicarbonate.abbr", "bicarbonate"),
				getPropertyValueColumn(labelPrefix + ".sulfate.abbr", "sulfate"),
				getPropertyValueColumn(labelPrefix + ".chloride.abbr", "chloride"),
				getPropertyValueColumn(labelPrefix + ".sodium.abbr", "sodium"),
				getPropertyValueColumn(labelPrefix + ".magnesium.abbr", "magnesium"),
				getPropertyValueColumn(labelPrefix + ".ph", "ph"),
			};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeRename(String oldName, String newName)
	{
		Database db = Database.getInstance();

		// recipes
		for (Recipe recipe : db.getRecipes().values())
		{
			for (ProcessStep step : recipe.getSteps())
			{
				for (IngredientAddition addition : step.getIngredients())
				{
					if (addition instanceof WaterAddition)
					{
						if (((WaterAddition)addition).getWater().getName().equals(oldName))
						{
							((WaterAddition)addition).getWater().setName(newName);
							// todo set dirty
						}
					}
				}
			}
		}

		// inventory
		// todo
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeDelete(String deletedName)
	{
		Database db = Database.getInstance();

		// recipes
		for (Recipe recipe : db.getRecipes().values())
		{
			for (ProcessStep step : recipe.getSteps())
			{
				for (IngredientAddition addition : new ArrayList<>(step.getIngredients()))
				{
					if (addition instanceof WaterAddition)
					{
						if (((WaterAddition)addition).getWater().getName().equals(deletedName))
						{
							step.removeIngredientAddition(addition);
							// todo set dirty
						}
					}
				}
			}
		}
	}
}
