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
import java.util.function.*;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import net.miginfocom.layout.AC;

/**
 *
 */
public class RefWaterPane extends V2DataObjectPane<Water>
{
	/*-------------------------------------------------------------------------*/
	public RefWaterPane(String dirtyFlag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "water", JfxUi.waterIcon, JfxUi.addWater);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected V2ObjectEditor<Water> editItemDialog(Water water, TrackDirty parent)
	{
		return new V2ObjectEditor<>(water, parent)
		{
			@Override
			protected void buildUi(Water obj, TrackDirty parent)
			{
				this.setColumnConstraints(new AC().count(4).gap("20",1));

				this.add(new Label(StringUtils.getUiString("water.name")));
				this.add(new Label(obj.getName()), "wrap");

				addQuantityWidget(obj, parent, "water.calcium",
					Water::getCalcium, (BiConsumer<Water, PpmUnit>)Water::setCalcium,
					Quantity.Unit.PPM, null);

				addQuantityWidget(obj, parent, "water.bicarbonate",
					Water::getBicarbonate, (BiConsumer<Water, PpmUnit>)Water::setBicarbonate,
					Quantity.Unit.PPM, "wrap");

				addQuantityWidget(obj, parent, "water.sulfate",
					Water::getSulfate, (BiConsumer<Water, PpmUnit>)Water::setSulfate,
					Quantity.Unit.PPM, null);

				addQuantityWidget(obj, parent, "water.chloride",
					Water::getChloride, (BiConsumer<Water, PpmUnit>)Water::setChloride,
					Quantity.Unit.PPM, "wrap");

				addQuantityWidget(obj, parent, "water.sodium",
					Water::getSodium, (BiConsumer<Water, PpmUnit>)Water::setSodium,
					Quantity.Unit.PPM, null);

				addQuantityWidget(obj, parent, "water.magnesium",
					Water::getMagnesium, (BiConsumer<Water, PpmUnit>)Water::setMagnesium,
					Quantity.Unit.PPM, "wrap");

				addQuantityWidget(obj, parent, "water.ph",
					Water::getPh, (BiConsumer<Water, PhUnit>)Water::setPh,
					Quantity.Unit.PH, "wrap");

				addTextArea(obj, parent, "water.desc", Water::getDescription, Water::setDescription, "span, wrap");
			}
		};
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
				getQuantityPropertyValueCol(labelPrefix + ".calcium.abbr", Water::getCalcium, Quantity.Unit.PPM),
				getQuantityPropertyValueCol(labelPrefix + ".bicarbonate.abbr", Water::getBicarbonate, Quantity.Unit.PPM),
				getQuantityPropertyValueCol(labelPrefix + ".sulfate.abbr", Water::getSulfate, Quantity.Unit.PPM),
				getQuantityPropertyValueCol(labelPrefix + ".chloride.abbr", Water::getChloride, Quantity.Unit.PPM),
				getQuantityPropertyValueCol(labelPrefix + ".sodium.abbr", Water::getSodium, Quantity.Unit.PPM),
				getQuantityPropertyValueCol(labelPrefix + ".magnesium.abbr", Water::getMagnesium, Quantity.Unit.PPM),
				getQuantityPropertyValueCol(labelPrefix + ".ph", Water::getPh, Quantity.Unit.PH),
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

							JfxUi.getInstance().setDirty(JfxUi.RECIPES);
							JfxUi.getInstance().setDirty(recipe);
							JfxUi.getInstance().setDirty(step);
							JfxUi.getInstance().setDirty(addition);
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

							JfxUi.getInstance().setDirty(JfxUi.RECIPES);
							JfxUi.getInstance().setDirty(recipe);
							JfxUi.getInstance().setDirty(step);
							JfxUi.getInstance().setDirty(addition);

						}
					}
				}
			}
		}

		// inventory
		// todo
	}
}
