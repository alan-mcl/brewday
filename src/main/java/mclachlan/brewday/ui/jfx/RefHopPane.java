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
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import net.miginfocom.layout.AC;

/**
 *
 */
public class RefHopPane extends RefIngredientPane<Hop>
{
	/*-------------------------------------------------------------------------*/
	public RefHopPane(String dirtyFlag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "hop", JfxUi.hopsIcon, JfxUi.addHops);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected V2ObjectEditor<Hop> newItemDialog(Hop obj, TrackDirty parent)
	{
		return new V2ObjectEditor<>(obj, parent)
		{
			@Override
			protected void buildUi(Hop obj, TrackDirty parent)
			{
				this.setColumnConstraints(new AC().count(4).gap("20",1));

				this.add(new Label(StringUtils.getUiString("hop.name")));
				this.add(new Label(obj.getName()), "wrap");

				// Type
				addComboBox(obj, parent, "hop.type",
					(Function<Hop, Hop.Type>)Hop::getType,
					(BiConsumer<Hop, Hop.Type>)Hop::setType,
					Hop.Type.values(),
					"wrap");

				// Origin
				addTextField(obj, parent, "hop.origin",
					Hop::getOrigin, Hop::setOrigin, "wrap");

				// Alpha
				addQuantityWidget(obj, parent, "hop.alpha",
					Hop::getAlphaAcid, (BiConsumer<Hop, PercentageUnit>)Hop::setAlphaAcid,
					Quantity.Unit.PERCENTAGE, null);

				// Beta
				addQuantityWidget(obj, parent, "hop.beta",
					Hop::getBetaAcid, (BiConsumer<Hop, PercentageUnit>)Hop::setBetaAcid,
					Quantity.Unit.PERCENTAGE, "wrap");

				// Humulene
				addQuantityWidget(obj, parent, "hop.humulene",
					Hop::getHumulene, (BiConsumer<Hop, PercentageUnit>)Hop::setHumulene,
					Quantity.Unit.PERCENTAGE, null);

				// Caryopyllene
				addQuantityWidget(obj, parent, "hop.caryophyllene",
					Hop::getCaryophyllene, (BiConsumer<Hop, PercentageUnit>)Hop::setCaryophyllene,
					Quantity.Unit.PERCENTAGE, "wrap");

				// Cohumulone
				addQuantityWidget(obj, parent, "hop.cohumulone",
					Hop::getCohumulone, (BiConsumer<Hop, PercentageUnit>)Hop::setCohumulone,
					Quantity.Unit.PERCENTAGE, null);

				// Myrcene
				addQuantityWidget(obj, parent, "hop.myrcene",
					Hop::getMyrcene, (BiConsumer<Hop, PercentageUnit>)Hop::setMyrcene,
					Quantity.Unit.PERCENTAGE, "wrap");

				// Storage Index
				addQuantityWidget(obj, parent, "hop.storage.index",
					Hop::getHopStorageIndex, (BiConsumer<Hop, PercentageUnit>)Hop::setHopStorageIndex,
					Quantity.Unit.PERCENTAGE, "wrap");

				// Substitutes
				addTextField(obj, parent, "hop.substitutes",
					Hop::getSubstitutes, Hop::setSubstitutes, "span, grow, wrap");

				// Desc
				addTextArea(obj, parent, "hop.desc", Hop::getDescription, Hop::setDescription, "span, wrap");
			}
		};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Hop createDuplicateItem(Hop current, String newName)
	{
		Hop result = new Hop(current);
		result.setName(newName);
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Hop createNewItem(String name)
	{
		return new Hop(name);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Map<String, Hop> getMap(Database database)
	{
		return database.getHops();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected TableColumn<Hop, String>[] getTableColumns(String labelPrefix)
	{
		return (TableColumn<Hop, String>[])new TableColumn[]
			{
				getStringPropertyValueCol(labelPrefix + ".type", "type"),
				getStringPropertyValueCol(labelPrefix + ".origin", "origin"),
				getQuantityPropertyValueCol(labelPrefix+".alpha", Hop::getAlphaAcid),
				getQuantityPropertyValueCol(labelPrefix+".beta", Hop::getBetaAcid),
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
					if (addition instanceof HopAddition)
					{
						if (((HopAddition)addition).getHop().getName().equals(oldName))
						{
							((HopAddition)addition).getHop().setName(newName);

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
					if (addition instanceof HopAddition)
					{
						if (((HopAddition)addition).getHop().getName().equals(deletedName))
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
