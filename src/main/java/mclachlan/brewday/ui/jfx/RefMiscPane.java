/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the misce that it will be useful,
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
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;
import net.miginfocom.layout.AC;

/**
 *
 */
public class RefMiscPane extends V2DataObjectPane<Misc>
{
	/*-------------------------------------------------------------------------*/
	public RefMiscPane(String dirtyFlag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "misc", Icons.miscIconGeneric, Icons.addMisc);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected V2ObjectEditor<Misc> editItemDialog(Misc obj, TrackDirty parent)
	{
		return new V2ObjectEditor<>(obj, parent)
		{
			@Override
			protected void buildUi(Misc obj, TrackDirty parent)
			{
				this.setColumnConstraints(new AC().count(4).gap("20", 1));

				this.add(new Label(StringUtils.getUiString("misc.name")));
				this.add(new Label(obj.getName()), "wrap");

				// Type
				addComboBox(obj, parent, "misc.type",
					(Function<Misc, Misc.Type>)Misc::getType,
					(BiConsumer<Misc, Misc.Type>)Misc::setType,
					Misc.Type.values(),
					"wrap");

				// Use
				addComboBox(obj, parent, "misc.use",
					(Function<Misc, Misc.Use>)Misc::getUse,
					(BiConsumer<Misc, Misc.Use>)Misc::setUse,
					Misc.Use.values(),
					"wrap");

				// Measurement Type
				addComboBox(obj, parent, "misc.measurementType",
					(Function<Misc, Quantity.Type>)Misc::getMeasurementType,
					(BiConsumer<Misc, Quantity.Type>)Misc::setMeasurementType,
					Quantity.Type.values(),
					"wrap");

				// Water addition formula
				addComboBox(obj, parent, "misc.water.addition.formula",
					(Function<Misc, Misc.WaterAdditionFormula>)Misc::getWaterAdditionFormula,
					(BiConsumer<Misc, Misc.WaterAdditionFormula>)Misc::setWaterAdditionFormula,
					Misc.WaterAdditionFormula.values(),
					null);

				addQuantityWidget(obj, parent, "misc.acid.content",
					Misc::getAcidContent,
					(BiConsumer<Misc, PercentageUnit>)Misc::setAcidContent,
					Quantity.Unit.PERCENTAGE_DISPLAY,
					"wrap");

				// Usage Rec
				addTextField(obj, parent, "misc.usage.recommendation",
					Misc::getUsageRecommendation, Misc::setUsageRecommendation,
					"span, grow, wrap");

				// Desc
				addTextArea(obj, parent, "misc.desc", Misc::getDescription, Misc::setDescription, "span, wrap");
			}
		};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Misc createDuplicateItem(Misc current, String newName)
	{
		Misc result = new Misc(current);
		result.setName(newName);
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Misc createNewItem(String name)
	{
		Misc misc = new Misc(name);
		misc.setType(Misc.Type.OTHER);
		return misc;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Map<String, Misc> getMap(Database database)
	{
		return database.getMiscs();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected TableColumn<Misc, String>[] getTableColumns(String labelPrefix)
	{
		return (TableColumn<Misc, String>[])new TableColumn[]
			{
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".type", "type"),
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".use", "use"),
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".usage.recommendation", "usageRecommendation"),
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
					if (addition instanceof MiscAddition)
					{
						if (((MiscAddition)addition).getMisc().getName().equals(oldName))
						{
//							((MiscAddition)addition).getMisc().setName(newName);

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
					if (addition instanceof MiscAddition)
					{
						if (((MiscAddition)addition).getMisc().getName().equals(deletedName))
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

		// batches
		for (Batch batch : db.getBatches().values())
		{
			for (Volume v : batch.getActualVolumes().getVolumes().values())
			{
				for (IngredientAddition ia : new ArrayList<>(v.getIngredientAdditions()))
				{
					if (ia instanceof MiscAddition)
					{
						if (((MiscAddition)ia).getMisc().getName().equals(deletedName))
						{
							v.removeIngredientAddition(ia);

							JfxUi.getInstance().setDirty(JfxUi.BATCHES);
							JfxUi.getInstance().setDirty(batch);
							JfxUi.getInstance().setDirty(ia);
						}
					}
				}
			}
		}


		// inventory
		for (InventoryLineItem ili : new ArrayList<>(db.getInventory().values()))
		{
			if (ili.getType() == IngredientAddition.Type.MISC &&
				ili.getIngredient().equals(deletedName))
			{
				db.getInventory().remove(InventoryLineItem.getUniqueId(deletedName, IngredientAddition.Type.MISC));

				JfxUi.getInstance().setDirty(JfxUi.INVENTORY);
			}
		}

	}

	@Override
	protected Image getIcon(Misc misc)
	{
		return UiUtils.getMiscIcon(misc);
	}
}
