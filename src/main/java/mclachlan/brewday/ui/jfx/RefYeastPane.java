/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the yeaste that it will be useful,
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
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.YeastAddition;
import net.miginfocom.layout.AC;

/**
 *
 */
public class RefYeastPane extends V2DataObjectPane<Yeast>
{
	/*-------------------------------------------------------------------------*/
	public RefYeastPane(String dirtyFlag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "yeast", Icons.yeastIcon, Icons.addYeast);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected V2ObjectEditor<Yeast> editItemDialog(Yeast obj, TrackDirty parent)
	{
		return new V2ObjectEditor<>(obj, parent)
		{
			@Override
			protected void buildUi(Yeast obj, TrackDirty parent)
			{
				this.setColumnConstraints(new AC().count(4).gap("20",1));

				this.add(new Label(StringUtils.getUiString("yeast.name")));
				this.add(new Label(obj.getName()), "wrap");

				// Type
				addComboBox(obj, parent, "yeast.type",
					(Function<Yeast, Yeast.Type>)Yeast::getType,
					(BiConsumer<Yeast, Yeast.Type>)Yeast::setType,
					Yeast.Type.values(),
					"wrap");

				// Lab
				addTextField(obj, parent, "yeast.laboratory",
					Yeast::getLaboratory, Yeast::setLaboratory, null);

				// Prod ID
				addTextField(obj, parent, "yeast.product.id",
					Yeast::getProductId, Yeast::setProductId, "wrap");

				// Attenuation
				addQuantityWidget(obj, parent, "yeast.attenuation",
					Yeast::getAttenuation, (BiConsumer<Yeast, PercentageUnit>)Yeast::setAttenuation,
					Quantity.Unit.PERCENTAGE_DISPLAY, null);

				// Flocc
				addComboBox(obj, parent, "yeast.flocculation",
					(Function<Yeast, Yeast.Flocculation>)Yeast::getFlocculation,
					(BiConsumer<Yeast, Yeast.Flocculation>)Yeast::setFlocculation,
					Yeast.Flocculation.values(), "wrap");

				// Min temp
				addQuantityWidget(obj, parent, "yeast.min.temp",
					Yeast::getMinTemp, (BiConsumer<Yeast, TemperatureUnit>)Yeast::setMinTemp,
					Quantity.Unit.CELSIUS, null);

				// Max temp
				addQuantityWidget(obj, parent, "yeast.max.temp",
					Yeast::getMaxTemp, (BiConsumer<Yeast, TemperatureUnit>)Yeast::setMaxTemp,
					Quantity.Unit.CELSIUS, "wrap");

				// Styles
				addTextField(obj, parent, "yeast.styles",
					Yeast::getRecommendedStyles, Yeast::setRecommendedStyles, "span, grow, wrap");

				// Desc
				addTextArea(obj, parent, "yeast.desc", Yeast::getDescription, Yeast::setDescription, "span, wrap");
			}
		};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Yeast createDuplicateItem(Yeast current, String newName)
	{
		Yeast result = new Yeast(current);
		result.setName(newName);
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Yeast createNewItem(String name)
	{
		return new Yeast(name);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Map<String, Yeast> getMap(Database database)
	{
		return database.getYeasts();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected TableColumn<Yeast, String>[] getTableColumns(String labelPrefix)
	{
		return (TableColumn<Yeast, String>[])new TableColumn[]
			{
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".laboratory", "laboratory"),
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".product.id", "productId"),
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".type", "type"),
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".form", "form"),
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
					if (addition instanceof YeastAddition)
					{
						if (((YeastAddition)addition).getYeast().getName().equals(oldName))
						{
//							((YeastAddition)addition).getYeast().setName(newName);

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
					if (addition instanceof YeastAddition)
					{
						if (((YeastAddition)addition).getYeast().getName().equals(deletedName))
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
	protected Image getIcon(Yeast yeast)
	{
		return Icons.yeastIcon;
	}
}
