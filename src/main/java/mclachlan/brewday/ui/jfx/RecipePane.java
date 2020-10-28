/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the recipee that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class RecipePane extends V2DataObjectPane<Recipe>
{
	private String tag;

	/*-------------------------------------------------------------------------*/
	public RecipePane(String dirtyFlag, String tag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "recipe", Icons.recipeIcon, Icons.addRecipe);
		this.tag = tag;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Parent editItemDialog(Recipe obj, TrackDirty parent)
	{
		return new RecipeEditor(obj, this, false);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Recipe createDuplicateItem(Recipe current, String newName)
	{
		Recipe result = new Recipe(current);
		result.setName(newName);
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Recipe createNewItem(String name)
	{
		// no op because we override the whole dialog instead
		return null;
	}

	@Override
	protected Recipe newItemDialog(String labelPrefix, Image addIcon)
	{
		NewRecipeDialog dialog = new NewRecipeDialog();

		dialog.showAndWait();
		return dialog.getOutput();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Map<String, Recipe> getMap(Database database)
	{
		return database.getRecipes();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void filterTable()
	{
		filterTable(s -> tag==null || s.getTags().contains(tag));
	}

	/*-------------------------------------------------------------------------*/

	@Override
	protected String[] getCsvHeaders()
	{
		return new String[]{"Name", "Est OG", "Est FG", "Est ABV", "IBU (Tinseth)", "Color"};
	}

	@Override
	protected String[] getCsvColumns(Recipe recipe)
	{
		List<Volume> beers = null;
		try
		{
			recipe.run();
			beers = recipe.getBeers();
		}
		catch (Exception e)
		{
			// if the recipe is poked just return this blank row
			return new String[]{recipe.getName(), "", "", "", "", ""};
		}

		if (beers != null && !beers.isEmpty())
		{
			// find the main batch
			Volume mainBeer = beers.get(0);

			for (Volume beer : beers)
			{
				if (beer.getVolume().get() > mainBeer.getVolume().get())
				{
					mainBeer = beer;
				}
			}

			return new String[]
				{
					recipe.getName(),
					""+mainBeer.getOriginalGravity().get(Quantity.Unit.SPECIFIC_GRAVITY),
					""+mainBeer.getGravity().get(Quantity.Unit.SPECIFIC_GRAVITY),
					""+mainBeer.getAbv().get(Quantity.Unit.PERCENTAGE_DISPLAY),
					""+mainBeer.getBitterness().get(Quantity.Unit.IBU),
					""+mainBeer.getColour().get(Quantity.Unit.SRM)
				};
		}
		else
		{
			return new String[]{recipe.getName(), "", "", "", "", ""};
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected TableColumn<Recipe, String>[] getTableColumns(String labelPrefix)
	{
		return (TableColumn<Recipe, String>[])new TableColumn[]
			{
				getStringPropertyValueCol(labelPrefix + ".equipment.profile", "equipmentProfile"),
				getStringPropertyValueCol(labelPrefix + ".tags", "tags"),
			};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeRename(String oldName, String newName)
	{
		Database db = Database.getInstance();

		// batches
		for (Batch batch : db.getBatches().values())
		{
			if (batch.getRecipe().equalsIgnoreCase(oldName))
			{
				batch.setRecipe(newName);

				JfxUi.getInstance().setDirty(JfxUi.BATCHES);
				JfxUi.getInstance().setDirty(batch);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeDelete(String deletedName)
	{
		Database db = Database.getInstance();

		// batches
		for (Batch batch : new ArrayList<>(db.getBatches().values()))
		{
			if (batch.getRecipe().equalsIgnoreCase(deletedName))
			{
				db.getBatches().remove(batch.getName());

				JfxUi.getInstance().setDirty(JfxUi.BATCHES);
				JfxUi.getInstance().setDirty(batch);
			}
		}
	}

	@Override
	protected Image getIcon(Recipe recipe)
	{
		return Icons.recipeIcon;
	}

	/*-------------------------------------------------------------------------*/
	public void setTag(String tag)
	{
		this.tag = tag;

		filterTable();
	}
}
