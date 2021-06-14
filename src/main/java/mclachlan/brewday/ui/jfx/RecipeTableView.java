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

import java.util.*;
import java.util.function.*;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;

public class RecipeTableView extends Pane
{
	private final TableView<IngredientAddition> table;
	private final V2DataObjectTableModel<IngredientAddition> tableModel;
	private final DirtyTableViewRowFactory<IngredientAddition> rowFactory;
	private final TableBuilder<IngredientAddition> tableBuilder = new TableBuilder<>();


	/*-------------------------------------------------------------------------*/
	public RecipeTableView(Supplier<Recipe> recipeSupplier)
	{
		table = new TableView<>();
		tableModel = new V2DataObjectTableModel<>(table);
		rowFactory = new DirtyTableViewRowFactory<>(table);
		table.setRowFactory(rowFactory);

		TableColumn<IngredientAddition, IngredientAddition> iconCol =
			tableBuilder.getIconColumn(this::getIcon);
		iconCol.setComparator(UiUtils.getIngredientAdditionComparator());
		iconCol.setSortType(TableColumn.SortType.ASCENDING);

		TableColumn<IngredientAddition, String> nameCol = tableBuilder.getStringPropertyValueCol(
			"batch.tab.recipe.ingredient", "name");

		TableColumn<IngredientAddition, String> quantity =
			tableBuilder.getQuantityAndUnitPropertyValueCol(
				"batch.tab.recipe.quantity",
				IngredientAddition::getQuantity, IngredientAddition::getUnit);

		table.getColumns().add(iconCol);
		table.getColumns().add(nameCol);
		table.getColumns().add(quantity);

		table.setPrefSize(650, 650);

		this.getChildren().add(table);
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Recipe recipe)
	{
		Map<String, IngredientAddition> map = new HashMap<>();
		for (IngredientAddition ia : recipe.getIngredientsBillOfMaterials())
		{
			map.put(ia.getName(), ia);
		}
		tableModel.refresh(map);
		TableColumn<IngredientAddition, ?> iconCol = table.getColumns().get(0);
		iconCol.setSortType(TableColumn.SortType.ASCENDING);
		table.getSortOrder().setAll(iconCol);
	}

	/*-------------------------------------------------------------------------*/
	private Image getIcon(IngredientAddition item)
	{
		return switch (item.getType())
			{
				case FERMENTABLES -> UiUtils.getFermentableIcon(
					Database.getInstance().getFermentables().get(item.getName()));
				case HOPS -> Icons.hopsIcon;
				case WATER -> Icons.waterIcon;
				case YEAST -> Icons.yeastIcon;
				case MISC -> UiUtils.getMiscIcon(
					Database.getInstance().getMiscs().get(item.getName()));
				default -> throw new BrewdayException("Unexpected value: " + item.getType());
			};
	}
}
