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
import javafx.scene.image.Image;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;
import net.miginfocom.layout.AC;

/**
 *
 */
public class RefFermentablePane extends V2DataObjectPane<Fermentable>
{
	public RefFermentablePane(String dirtyFlag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "fermentable", Icons.fermentableIconGeneric, Icons.addFermentable);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected V2ObjectEditor<Fermentable> editItemDialog(
		Fermentable fermentable,
		TrackDirty parent)
	{
		return new V2ObjectEditor<>(fermentable, parent)
		{
			@Override
			protected void buildUi(Fermentable obj, TrackDirty parent)
			{
				this.setColumnConstraints(new AC().count(4).gap("20",1));

				// Name
				this.add(new Label(StringUtils.getUiString("fermentable.name")));
				this.add(new Label(obj.getName()), "wrap");

				// Type
				addComboBox(obj, parent, "fermentable.type",
					(Function<Fermentable, Fermentable.Type>)Fermentable::getType,
					(BiConsumer<Fermentable, Fermentable.Type>)Fermentable::setType,
					Fermentable.Type.values(), "wrap");

				// Origin
				addTextField(obj, parent, "fermentable.origin",
					Fermentable::getOrigin, Fermentable::setOrigin, null);

				// Supplier
				addTextField(obj, parent, "fermentable.supplier",
					Fermentable::getSupplier, Fermentable::setSupplier, "wrap");

				// Yield
				addQuantityWidget(obj, parent, "fermentable.yield",
					Fermentable::getYield, (BiConsumer<Fermentable, PercentageUnit>)Fermentable::setYield,
					Quantity.Unit.PERCENTAGE_DISPLAY, null);

				// Colour
				addQuantityWidget(obj, parent, "fermentable.colour",
					Fermentable::getColour, (BiConsumer<Fermentable, ColourUnit>)Fermentable::setColour,
					Quantity.Unit.LOVIBOND, "wrap");

				// Coarse/Fine Diff
				addQuantityWidget(obj, parent, "fermentable.coarse.fine.diff",
					Fermentable::getCoarseFineDiff, (BiConsumer<Fermentable, PercentageUnit>)Fermentable::setCoarseFineDiff,
					Quantity.Unit.PERCENTAGE_DISPLAY, null);

				// Moisture
				addQuantityWidget(obj, parent, "fermentable.moisture",
					Fermentable::getMoisture, (BiConsumer<Fermentable, PercentageUnit>)Fermentable::setMoisture,
					Quantity.Unit.PERCENTAGE_DISPLAY, "wrap");

				// Diastatic Power
				addQuantityWidget(obj, parent, "fermentable.diastatic.power",
					Fermentable::getDiastaticPower, (BiConsumer<Fermentable, DiastaticPowerUnit>)Fermentable::setDiastaticPower,
					Quantity.Unit.LINTNER, null);

				// Max in batch
				addQuantityWidget(obj, parent, "fermentable.max.in.batch",
					Fermentable::getMaxInBatch, (BiConsumer<Fermentable, PercentageUnit>)Fermentable::setMaxInBatch,
					Quantity.Unit.PERCENTAGE_DISPLAY, "wrap");

				// Distilled Water Ph
				addQuantityWidget(obj, parent, "fermentable.distilled.water.ph",
					Fermentable::getDistilledWaterPh, (BiConsumer<Fermentable, PhUnit>)Fermentable::setDistilledWaterPh,
					Quantity.Unit.PH, null);

				// Acid content
				addQuantityWidget(obj, parent, "fermentable.lactic.acid.content",
					Fermentable::getLacticAcidContent, (BiConsumer<Fermentable, PercentageUnit>)Fermentable::setLacticAcidContent,
					Quantity.Unit.PERCENTAGE_DISPLAY, "wrap");

				// todo... ibuGalPerLb

				// Add after boil?
				addCheckBox(obj, parent, "fermentable.add.after.boil",
					Fermentable::isAddAfterBoil, Fermentable::setAddAfterBoil, "span, wrap");

				// Recommend Mash?
				addCheckBox(obj, parent, "fermentable.recommend.mash",
					Fermentable::isRecommendMash, Fermentable::setRecommendMash, "span, wrap");


				// Description
				addTextArea(obj, parent, "fermentable.desc",
					Fermentable::getDescription, Fermentable::setDescription, "span, wrap");

			}
		};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Fermentable createDuplicateItem(Fermentable current, String newName)
	{
		Fermentable result = new Fermentable(current);
		result.setName(newName);
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Fermentable createNewItem(String name)
	{
		return new Fermentable(name);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Map<String, Fermentable> getMap(Database database)
	{
		return database.getFermentables();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected TableColumn<Fermentable, String>[] getTableColumns(String labelPrefix)
	{

		return (TableColumn<Fermentable, String>[])new TableColumn[]
			{
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".type", "type"),
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".origin", "origin"),
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".supplier", "supplier"),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".colour", Fermentable::getColour, Quantity.Unit.LOVIBOND),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".yield", Fermentable::getYield, Quantity.Unit.PERCENTAGE_DISPLAY),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".distilled.water.ph", Fermentable::getDistilledWaterPh, Quantity.Unit.PH),
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
				for (IngredientAddition addition : step.getIngredientAdditions())
				{
					if (addition instanceof FermentableAddition)
					{
						if (((FermentableAddition)addition).getFermentable().getName().equals(oldName))
						{
//							((FermentableAddition)addition).getFermentable().setName(newName);

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
				for (IngredientAddition addition : new ArrayList<>(step.getIngredientAdditions()))
				{
					if (addition instanceof FermentableAddition)
					{
						if (((FermentableAddition)addition).getFermentable().getName().equals(deletedName))
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

	@Override
	protected Image getIcon(Fermentable fermentable)
	{
		return UiUtils.getFermentableIcon(fermentable);
	}
}
