
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
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.ui.UiUtils;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;

import static mclachlan.brewday.ui.jfx.Icons.ICON_SIZE;

/**
 *
 */
public class InventoryPane extends V2DataObjectPane<InventoryLineItem>
{
	public InventoryPane(TrackDirty parent)
	{
		super(JfxUi.INVENTORY, parent, "inventory", Icons.inventoryIcon, Icons.inventoryIcon);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected ToolBar buildToolBar(String dirtyFlag, TrackDirty parent,
		String labelPrefix, Image addIcon)
	{
		ToolBar toolbar = new ToolBar();
		toolbar.setPadding(new Insets(3, 3, 6, 3));

		Button saveAllButton = new Button(StringUtils.getUiString("editor.apply.all"), JfxUi.getImageView(Icons.saveIcon, ICON_SIZE));
		Button discardAllButton = new Button(StringUtils.getUiString("editor.discard.all"), JfxUi.getImageView(Icons.undoIcon, ICON_SIZE));
		// operation buttons
		Button addWaterButton = new Button(""/*StringUtils.getUiString(labelPrefix + ".add.water")*/, JfxUi.getImageView(Icons.addWater, ICON_SIZE));
		Button addFermButton = new Button(""/*StringUtils.getUiString(labelPrefix + ".add.fermentable")*/, JfxUi.getImageView(Icons.addFermentable, ICON_SIZE));
		Button addHopButton = new Button(""/*StringUtils.getUiString(labelPrefix + ".add.hop")*/, JfxUi.getImageView(Icons.addHops, ICON_SIZE));
		Button addYeastButton = new Button(""/*StringUtils.getUiString(labelPrefix + ".add.yeast")*/, JfxUi.getImageView(Icons.addYeast, ICON_SIZE));
		Button addMiscButton = new Button(""/*StringUtils.getUiString(labelPrefix + ".add.misc")*/, JfxUi.getImageView(Icons.addMisc, ICON_SIZE));
		Button deleteButton = new Button(""/*StringUtils.getUiString(labelPrefix + ".delete")*/, JfxUi.getImageView(Icons.deleteIcon, ICON_SIZE));
		// export buttons
		Button exportCsv = new Button(StringUtils.getUiString("common.export.csv"), JfxUi.getImageView(Icons.exportCsv, ICON_SIZE));

		saveAllButton.setTooltip(new Tooltip(StringUtils.getUiString("editor.apply.all")));
		discardAllButton.setTooltip(new Tooltip(StringUtils.getUiString("editor.discard.all")));
		addWaterButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".add.water")));
		addFermButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".add.fermentable")));
		addHopButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".add.hop")));
		addYeastButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".add.yeast")));
		addMiscButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".add.misc")));
		deleteButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".delete")));
		exportCsv.setTooltip(new Tooltip(StringUtils.getUiString("common.export.csv")));

		toolbar.getItems().add(saveAllButton);
		toolbar.getItems().add(discardAllButton);
		toolbar.getItems().add(new Separator());
		toolbar.getItems().add(addWaterButton);
		toolbar.getItems().add(addFermButton);
		toolbar.getItems().add(addHopButton);
		toolbar.getItems().add(addYeastButton);
		toolbar.getItems().add(addMiscButton);
		toolbar.getItems().add(deleteButton);
		toolbar.getItems().add(new Separator());
		toolbar.getItems().add(exportCsv);

		exportCsv.setOnAction(event ->
		{
			ObservableList<InventoryLineItem> selectedCells = getTable().getSelectionModel().getSelectedItems();
			if (selectedCells != null && !selectedCells.isEmpty())
			{
				exportCsv(selectedCells);
			}
		});

		discardAllButton.setOnAction(event ->
		{
			Alert alert = new Alert(
				Alert.AlertType.NONE,
				StringUtils.getUiString("editor.discard.all.msg"),
				ButtonType.OK, ButtonType.CANCEL);

			Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
			stage.getIcons().add(Icons.undoIcon);
			alert.setTitle(StringUtils.getUiString("editor.discard.all"));
			alert.setGraphic(JfxUi.getImageView(Icons.undoIcon, 32));

			JfxUi.styleScene(stage.getScene());

			alert.showAndWait();

			if (alert.getResult() == ButtonType.OK)
			{
				Database.getInstance().loadAll();

				parent.clearDirty();
				clearDirty();

				refresh(Database.getInstance());
			}
		});

		saveAllButton.setOnAction(event ->
		{
			Alert alert = new Alert(
				Alert.AlertType.NONE,
				StringUtils.getUiString("editor.apply.all.msg"),
				ButtonType.OK, ButtonType.CANCEL);

			Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
			stage.getIcons().add(Icons.saveIcon);
			alert.setTitle(StringUtils.getUiString("editor.apply.all"));
			alert.setGraphic(JfxUi.getImageView(Icons.saveIcon, 32));

			JfxUi.styleScene(stage.getScene());

			alert.showAndWait();

			if (alert.getResult() == ButtonType.OK)
			{
				Database.getInstance().saveAll();

				parent.clearDirty();
				clearDirty();

				refresh(Database.getInstance());
			}
		});

		addFermButton.setOnAction(event ->
		{
			FermentableAdditionDialog dialog = new FermentableAdditionDialog(null, null, false);

			dialog.showAndWait();

			FermentableAddition addition = dialog.getOutput();
			if (addition != null)
			{
				InventoryLineItem result = new InventoryLineItem(
					addition.getFermentable().getName(),
					IngredientAddition.Type.FERMENTABLES,
					addition.getQuantity(),
					addition.getUnit());

				getTableModel().add(result);
				setDirty(result);
				getTable().getSelectionModel().select(result);
			}
		});

		addHopButton.setOnAction(event ->
		{
			HopAdditionDialog dialog = new HopAdditionDialog(null, null, false);

			dialog.showAndWait();

			HopAddition addition = dialog.getOutput();
			if (addition != null)
			{
				InventoryLineItem result = new InventoryLineItem(
					addition.getHop().getName(),
					IngredientAddition.Type.HOPS,
					addition.getQuantity(),
					addition.getUnit());

				getTableModel().add(result);
				setDirty(result);
				getTable().getSelectionModel().select(result);
			}
		});

		addYeastButton.setOnAction(event ->
		{
			YeastAdditionDialog dialog = new YeastAdditionDialog(null, null, false);

			dialog.showAndWait();

			YeastAddition addition = dialog.getOutput();
			if (addition != null)
			{
				InventoryLineItem result = new InventoryLineItem(
					addition.getYeast().getName(),
					IngredientAddition.Type.YEAST,
					addition.getQuantity(),
					addition.getUnit());

				getTableModel().add(result);
				setDirty(result);
				getTable().getSelectionModel().select(result);
			}
		});

		addMiscButton.setOnAction(event ->
		{
			MiscAdditionDialog dialog = new MiscAdditionDialog(null, null, false);

			dialog.showAndWait();

			MiscAddition addition = dialog.getOutput();
			if (addition != null)
			{
				InventoryLineItem result = new InventoryLineItem(
					addition.getMisc().getName(),
					IngredientAddition.Type.MISC,
					addition.getQuantity(),
					addition.getUnit());

				getTableModel().add(result);
				setDirty(result);
				getTable().getSelectionModel().select(result);
			}
		});

		addWaterButton.setOnAction(event ->
		{
			WaterAdditionDialog dialog = new WaterAdditionDialog(null, null, false);

			dialog.showAndWait();

			WaterAddition addition = dialog.getOutput();
			if (addition != null)
			{
				InventoryLineItem result = new InventoryLineItem(
					addition.getWater().getName(),
					IngredientAddition.Type.WATER,
					addition.getQuantity(),
					addition.getUnit());

				getTableModel().add(result);
				setDirty(result);
				getTable().getSelectionModel().select(result);
			}
		});

		deleteButton.setOnAction(event -> delete(dirtyFlag, labelPrefix));

		return toolbar;
	}

	@Override
	protected V2ObjectEditor<InventoryLineItem> editItemDialog(
		InventoryLineItem obj,
		TrackDirty parent)
	{
		return new V2ObjectEditor<>(obj, parent)
		{
			@Override
			protected void buildUi(InventoryLineItem obj, TrackDirty parent)
			{
				this.setPrefWidth(400);
//				this.setPrefHeight(80);

				this.setLayoutConstraints(new LC().gridGap("5", "5").insetsAll("10"));
				this.setColumnConstraints(new AC().count(4).gap("20",1));

				Label lab = new Label(obj.getIngredient());
				lab.setAlignment(Pos.CENTER);
				lab.setStyle("-fx-font-weight: bold;");
				this.add(lab);

				this.add(new Label());

				addQuantityWidget(obj, parent, "inventory.quantity",
					InventoryLineItem::getQuantity,
					(BiConsumer<InventoryLineItem, Quantity>)InventoryLineItem::setQuantity,
					obj.getUnit(),
					"wrap");
			}
		};
	}

	@Override
	protected void tableInitialSort(TableView<InventoryLineItem> table)
	{
		// start sorted by ingredient
		TableColumn<InventoryLineItem, InventoryLineItem> col1 =
			(TableColumn<InventoryLineItem, InventoryLineItem>)table.getColumns().get(0);

		col1.setSortable(true);
		col1.setSortType(TableColumn.SortType.ASCENDING);
		col1.setComparator(UiUtils.getInventoryLineItemComparator());

		table.getSortOrder().setAll(col1);

	}

	@Override
	protected InventoryLineItem createDuplicateItem(InventoryLineItem current,
		String newName)
	{
		throw new BrewdayException("not supported");
	}

	@Override
	protected InventoryLineItem createNewItem(String name)
	{
		// not used
		return null;
	}

	@Override
	protected Map<String, InventoryLineItem> getMap(Database database)
	{
		return database.getInventory();
	}

	@Override
	protected TableColumn<InventoryLineItem, String>[] getTableColumns(
		String labelPrefix)
	{
		return new TableColumn[]
			{
//				getStringPropertyValueCol(labelPrefix+".ingredient", "ingredient"),
//				getStringPropertyValueCol(labelPrefix+".item.type", "type"),
				getTableBuilder().getQuantityAndUnitPropertyValueCol(
					labelPrefix+".quantity",
					InventoryLineItem::getQuantity,
					InventoryLineItem::getUnit),
			};
	}

	@Override
	protected void cascadeRename(String oldName, String newName)
	{
		throw new BrewdayException("not supported");
	}

	@Override
	protected void cascadeDelete(String deletedName)
	{
		// no op
	}

	@Override
	protected Image getIcon(InventoryLineItem item)
	{
		switch (item.getType())
		{
			case FERMENTABLES:
				return UiUtils.getFermentableIcon(
					Database.getInstance().getFermentables().get(item.getIngredient()));
			case HOPS:
				return Icons.hopsIcon;
			case WATER:
				return Icons.waterIcon;
			case YEAST:
				return Icons.yeastIcon;
			case MISC:
				return UiUtils.getMiscIcon(
					Database.getInstance().getMiscs().get(item.getIngredient()));
			default:
				throw new BrewdayException("Unexpected value: " + item.getType());
		}
	}
}
