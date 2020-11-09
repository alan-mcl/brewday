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

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.Quantity;
import org.tbee.javafx.scene.layout.MigPane;

import static mclachlan.brewday.StringUtils.getUiString;
import static mclachlan.brewday.ui.jfx.Icons.ICON_SIZE;

/**
 *
 */
public abstract class V2DataObjectPane<T extends V2DataObject> extends MigPane implements TrackDirty
{
	private final TableView<T> table;
	private final V2DataObjectTableModel<T> tableModel;
	private final DirtyTableViewRowFactory<T> rowFactory;

	private final String dirtyFlag;
	private boolean detectDirty = true;
	private final TrackDirty parent;

	public static class ImageTableCell<T> extends TableCell<T, T>
	{
		private final ImageView imageView;
		private final Function<T, Image> imageFactory;

		public ImageTableCell(Function<T, Image> imageFactory)
		{
			this.imageFactory = imageFactory;
			imageView = new ImageView();
			imageView.setPreserveRatio(true);
			imageView.setFitHeight(24);
			imageView.setFitWidth(24);

			setGraphic(imageView);
		}

		@Override
		protected void updateItem(T t, boolean empty)
		{
			super.updateItem(t, empty);

			if (empty || t == null)
			{
				imageView.setImage(null);
			}
			else
			{
				imageView.setImage(imageFactory.apply(t));
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public V2DataObjectPane(
		String dirtyFlag,
		TrackDirty parent,
		final String labelPrefix,
		final Image icon,
		final Image addIcon)
	{
		super("insets 3");

		this.dirtyFlag = dirtyFlag;
		this.parent = parent;

		table = new TableView<>();
		tableModel = new V2DataObjectTableModel<>(table);
		rowFactory = new DirtyTableViewRowFactory<>(table);
		table.setRowFactory(rowFactory);

		TableColumn<T, T> iconCol = new TableColumn<>();
		iconCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
		iconCol.setCellFactory(c -> new ImageTableCell<>(this::getIcon));

		table.getColumns().add(iconCol);

		TableColumn<T, String> name = getStringPropertyValueCol(labelPrefix + ".name", "name");
		name.setSortType(TableColumn.SortType.ASCENDING);
		table.getColumns().add(name);

		table.getColumns().addAll(getTableColumns(labelPrefix));

		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		ToolBar toolbar = buildToolBar(dirtyFlag, parent, labelPrefix, addIcon);

		table.setPrefWidth(1100);
		table.setPrefHeight(700);

		this.add(toolbar, "dock north, alignx left, wrap");
		this.add(table, "aligny top");

		tableInitialSort(table);

		//-------------

		table.setOnMouseClicked(event ->
		{
			if (event.getClickCount() == 2)
			{
				T selectedItem = table.getSelectionModel().getSelectedItem();
				if (selectedItem != null)
				{
					Stage dialogStage = new Stage();
					dialogStage.initModality(Modality.APPLICATION_MODAL);
					dialogStage.initOwner(((Node)event.getSource()).getScene().getWindow());
					dialogStage.setTitle(selectedItem.getName());
					dialogStage.getIcons().add(icon);

					Parent editor = editItemDialog(selectedItem, this);

					Scene scene = new Scene(editor);
					JfxUi.styleScene(scene);
					dialogStage.setScene(scene);
					dialogStage.showAndWait();
				}
			}
		});

		table.setOnKeyPressed(keyEvent ->
		{
			if (keyEvent.getCode() == KeyCode.DELETE)
			{
				delete(dirtyFlag, labelPrefix);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	protected ToolBar buildToolBar(String dirtyFlag, TrackDirty parent,
		String labelPrefix, Image addIcon)
	{
		ToolBar toolbar = new ToolBar();
		toolbar.setPadding(new Insets(3, 3, 6, 3));

		Button saveAllButton = new Button(StringUtils.getUiString("editor.apply.all"), JfxUi.getImageView(Icons.saveIcon, ICON_SIZE));
		Button discardAllButton = new Button(StringUtils.getUiString("editor.discard.all"), JfxUi.getImageView(Icons.undoIcon, ICON_SIZE));
		// operation buttons
		Button addButton = new Button(StringUtils.getUiString(labelPrefix + ".add"), JfxUi.getImageView(addIcon, ICON_SIZE));
		Button duplicateButton = new Button(StringUtils.getUiString(labelPrefix + ".copy"), JfxUi.getImageView(Icons.duplicateIcon, ICON_SIZE));
		Button renameButton = new Button(StringUtils.getUiString(labelPrefix + ".rename"), JfxUi.getImageView(Icons.renameIcon, ICON_SIZE));
		Button deleteButton = new Button(StringUtils.getUiString(labelPrefix + ".delete"), JfxUi.getImageView(Icons.deleteIcon, ICON_SIZE));
		// export buttons
		Button exportCsv = new Button(StringUtils.getUiString("common.export.csv"), JfxUi.getImageView(Icons.exportCsv, ICON_SIZE));

		saveAllButton.setTooltip(new Tooltip(StringUtils.getUiString("editor.apply.all")));
		discardAllButton.setTooltip(new Tooltip(StringUtils.getUiString("editor.discard.all")));
		addButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".add")));
		duplicateButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".copy")));
		renameButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".rename")));
		deleteButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".delete")));
		exportCsv.setTooltip(new Tooltip(StringUtils.getUiString("common.export.csv")));

		toolbar.getItems().add(saveAllButton);
		toolbar.getItems().add(discardAllButton);
		toolbar.getItems().add(new Separator());
		toolbar.getItems().add(addButton);
		toolbar.getItems().add(duplicateButton);
		toolbar.getItems().add(renameButton);
		toolbar.getItems().add(deleteButton);
		toolbar.getItems().add(new Separator());
		toolbar.getItems().add(exportCsv);

		exportCsv.setOnAction(event ->
		{
			ObservableList<T> selectedCells = table.getSelectionModel().getSelectedItems();
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

		addButton.setOnAction(event ->
		{
			T result = newItemDialog(labelPrefix, addIcon);
			if (result != null)
			{
				tableModel.add(result);
				setDirty(result);
				table.getSelectionModel().select(result);
			}
		});

		deleteButton.setOnAction(event -> delete(dirtyFlag, labelPrefix));

		duplicateButton.setOnAction(event ->
		{
			T item = table.getSelectionModel().getSelectedItem();

			if (item != null)
			{
				DuplicateItemDialog<T> dialog = new DuplicateItemDialog<>(
					item, labelPrefix, addIcon)
				{
					@Override
					public Map<String, T> getMap()
					{
						return V2DataObjectPane.this.getMap(Database.getInstance());
					}

					@Override
					public T createItem(T current, TextField name)
					{
						return createDuplicateItem(current, name.getText());
					}
				};

				dialog.showAndWait();
				T result = dialog.getOutput();
				if (result != null)
				{
					tableModel.add(result);
					setDirty(result);
				}
			}
		});

		renameButton.setOnAction(event ->
		{
			T item = table.getSelectionModel().getSelectedItem();

			if (item != null)
			{
				RenameItemDialog<T> dialog = new RenameItemDialog<>(item, labelPrefix)
				{
					@Override
					protected Map<String, T> getMap()
					{
						return V2DataObjectPane.this.getMap(Database.getInstance());
					}
				};

				dialog.showAndWait();
				String result = dialog.getOutput();
				if (result != null)
				{
					// deep link rename
					cascadeRename(item.getName(), result);

					tableModel.remove(item);
					item.setName(result);
					tableModel.add(item);

					setDirty(item);
				}
			}
		});
		return toolbar;
	}

	/*-------------------------------------------------------------------------*/
	protected void delete(String dirtyFlag, String labelPrefix)
	{
		List<T> items = table.getSelectionModel().getSelectedItems();

		if (items != null)
		{
			Alert alert = new Alert(
				Alert.AlertType.NONE,
				StringUtils.getUiString(labelPrefix + ".delete.msg"),
				ButtonType.OK, ButtonType.CANCEL);

			Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
			stage.getIcons().add(Icons.deleteIcon);
			alert.setTitle(StringUtils.getUiString(labelPrefix + ".delete"));
			alert.setGraphic(JfxUi.getImageView(Icons.deleteIcon, 32));

			JfxUi.styleScene(stage.getScene());

			alert.showAndWait();

			if (alert.getResult() == ButtonType.OK)
			{
				for (T item : new ArrayList<>(items))
				{
					// cascading delete
					cascadeDelete(item.getName());

					tableModel.remove(item);
				}
				setDirty(dirtyFlag);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	protected void exportCsv(ObservableList<T> selectedItems)
	{
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(StringUtils.getUiString("tools.export.csv.title"));

		Settings settings = Database.getInstance().getSettings();
		String dir = settings.get(Settings.LAST_EXPORT_DIRECTORY);
		if (dir != null && new File(dir).exists())
		{
			fileChooser.setInitialDirectory(new File(dir));
		}

		File file = fileChooser.showSaveDialog(
			JfxUi.getInstance().getMainScene().getWindow());

		if (file != null)
		{
			String parent = file.getParent();
			if (parent != null)
			{
				settings.set(Settings.LAST_EXPORT_DIRECTORY, parent);
				Database.getInstance().saveSettings();
			}

			try (PrintWriter pw = new PrintWriter(file))
			{
				pw.println(convertToCSV(getCsvHeaders()));

				for (T t : selectedItems)
				{
					pw.println(convertToCSV(getCsvColumns(t)));
				}
			}
			catch (Exception x)
			{
				throw new BrewdayException(x);
			}
		}
	}

	protected String[] getCsvHeaders()
	{
		return new String[]{"Name"};
	}

	protected String[] getCsvColumns(T t)
	{
		return new String[]{t.getName()};
	}

	private String convertToCSV(String[] data)
	{
		return Stream.of(data)
			.map(this::escapeSpecialCharacters)
			.collect(Collectors.joining(","));
	}

	private String escapeSpecialCharacters(String data)
	{
		String escapedData = data.replaceAll("\\R", " ");
		if (data.contains(",") || data.contains("\"") || data.contains("'"))
		{
			data = data.replace("\"", "\"\"");
			escapedData = "\"" + data + "\"";
		}
		return escapedData;
	}

	/*-------------------------------------------------------------------------*/
	protected T newItemDialog(String labelPrefix, Image addIcon)
	{
		NewItemDialog<T> dialog = new NewItemDialog<>(labelPrefix, addIcon)
		{
			@Override
			public Map<String, T> getMap()
			{
				return V2DataObjectPane.this.getMap(Database.getInstance());
			}

			@Override
			public T createNewItem(String name)
			{
				return V2DataObjectPane.this.createNewItem(name);
			}
		};

		dialog.showAndWait();
		return dialog.getOutput();
	}

	/*-------------------------------------------------------------------------*/

	protected abstract Parent editItemDialog(T selectedItem,
		TrackDirty parent);

	protected abstract T createDuplicateItem(T current, String newName);

	protected abstract T createNewItem(String name);

	protected abstract Map<String, T> getMap(Database database);

	protected abstract TableColumn<T, String>[] getTableColumns(
		String labelPrefix);

	protected abstract void cascadeRename(String oldName, String newName);

	protected abstract void cascadeDelete(String deletedName);

	protected abstract Image getIcon(T t);

	/*-------------------------------------------------------------------------*/
	public void refresh(Database database)
	{
		this.detectDirty = false;

		Map<String, T> ref = getMap(database);

		ObservableList<T> selectedItems = table.getSelectionModel().getSelectedItems();

		tableModel.refresh(ref);

		for (T t : selectedItems)
		{
			table.getSelectionModel().select(t);
		}

		filterTable();

		for (T t : ref.values())
		{
			if (JfxUi.getInstance().isDirty(t))
			{
				rowFactory.setDirty(t);
			}
		}

		this.detectDirty = true;
	}

	/*-------------------------------------------------------------------------*/
	protected TableView<T> getTable()
	{
		return table;
	}

	/*-------------------------------------------------------------------------*/
	protected V2DataObjectTableModel<T> getTableModel()
	{
		return tableModel;
	}

	/*-------------------------------------------------------------------------*/
	protected void tableInitialSort(TableView<T> table)
	{
		// start sorted by name
		TableColumn<T, ?> nameColumn = table.getColumns().get(1);
		nameColumn.setSortType(TableColumn.SortType.ASCENDING);
		table.getSortOrder().setAll(nameColumn);
	}

	/*-------------------------------------------------------------------------*/
	protected void filterTable()
	{
	}

	protected void filterTable(Predicate<T> predicate)
	{
		tableModel.filter(predicate);
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<T, String> getStringPropertyValueCol(
		String heading,
		String property)
	{
		TableColumn<T, String> col = new TableColumn<>(getUiString(heading));
		col.setCellValueFactory(new PropertyValueFactory<>(property));
		return col;
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<T, Double> getQuantityPropertyValueCol(
		String heading,
		Function<T, Quantity> getter,
		Quantity.Unit unit)
	{
		TableColumn<T, Double> col = new TableColumn<>(getUiString(heading));
		col.setCellValueFactory(param ->
		{
			Quantity quantity = getter.apply(param.getValue());
			if (quantity != null)
			{
				return new SimpleObjectProperty<>(quantity.get(unit));
			}
			else
			{
				return new SimpleObjectProperty<>(null);
			}
		});

		return col;
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<T, String> getQuantityAndUnitPropertyValueCol(
		String heading,
		Function<T, Quantity> getterQuantity,
		Function<T, Quantity.Unit> getterUnit)
	{
		TableColumn<T, String> col = new TableColumn<>(getUiString(heading));
		col.setCellValueFactory(param ->
		{
			Quantity quantity = getterQuantity.apply(param.getValue());
			Quantity.Unit unit = getterUnit.apply(param.getValue());
			if (quantity != null)
			{
				if (unit == null)
				{
					unit = quantity.getUnit();
				}

				return new SimpleStringProperty(quantity.get(unit)+" "+StringUtils.getUiString("unit."+unit.name()));
			}
			else
			{
				return new SimpleObjectProperty<>(null);
			}
		});

		return col;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void setDirty(Object... objs)
	{
		if (detectDirty)
		{
			for (Object obj : objs)
			{
				if (!JfxUi.getInstance().isDirty(obj))
				{
					if (!(obj instanceof String))
					{
						T dirty = (T)obj;

						if (dirty != null)
						{
							rowFactory.setDirty(dirty);
						}
					}

					parent.setDirty(this.dirtyFlag, obj);
				}
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void clearDirty()
	{
		rowFactory.clearAllDirty();
	}
}
