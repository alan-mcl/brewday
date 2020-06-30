package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import org.tbee.javafx.scene.layout.MigPane;

import static mclachlan.brewday.StringUtils.getUiString;
import static mclachlan.brewday.ui.jfx.RecipesPane3.ICON_SIZE;

/**
 *
 */
public abstract class RefIngredientPane<T extends V2DataObject> extends MigPane implements TrackDirty
{
	private final TableView<T> table;
	private final V2DataObjectTableModel<T> tableModel;
	private final DirtyTableViewRowFactory<T> rowFactory;

	private final String dirtyFlag;
	private boolean detectDirty = true;
	private final TrackDirty parent;

	/*-------------------------------------------------------------------------*/
	public RefIngredientPane(
		String dirtyFlag,
		TrackDirty parent,
		final String labelPrefix,
		final Image addIcon)
	{
		super("insets 3");

		this.dirtyFlag = dirtyFlag;
		this.parent = parent;

		table = new TableView<>();
		tableModel = new V2DataObjectTableModel<>(table);
		rowFactory = new DirtyTableViewRowFactory<>(table);
		table.setRowFactory(rowFactory);

		TableColumn<T, String> name = getPropertyValueColumn(labelPrefix + ".name", "name");
		name.setSortType(TableColumn.SortType.ASCENDING);
		table.getColumns().add(name);

		table.getColumns().addAll(getTableColumns(labelPrefix));

		ToolBar toolbar = new ToolBar();
		toolbar.setPadding(new Insets(3, 3, 6, 3));

		Button saveAllButton = new Button(StringUtils.getUiString("editor.apply.all"), JfxUi.getImageView(JfxUi.saveIcon, ICON_SIZE));
		Button discardAllButton = new Button(StringUtils.getUiString("editor.discard.all"), JfxUi.getImageView(JfxUi.undoIcon, ICON_SIZE));
		// operation buttons
		Button addButton = new Button(StringUtils.getUiString(labelPrefix + ".add"), JfxUi.getImageView(addIcon, ICON_SIZE));
		Button duplicateButton = new Button(StringUtils.getUiString(labelPrefix + ".copy"), JfxUi.getImageView(JfxUi.duplicateIcon, ICON_SIZE));
		Button renameButton = new Button(StringUtils.getUiString(labelPrefix + ".rename"), JfxUi.getImageView(JfxUi.renameIcon, ICON_SIZE));
		Button deleteButton = new Button(StringUtils.getUiString(labelPrefix + ".delete"), JfxUi.getImageView(JfxUi.deleteIcon, ICON_SIZE));

		saveAllButton.setTooltip(new Tooltip(StringUtils.getUiString("editor.apply.all")));
		discardAllButton.setTooltip(new Tooltip(StringUtils.getUiString("editor.discard.all")));
		addButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".add")));
		duplicateButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".copy")));
		renameButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".rename")));
		deleteButton.setTooltip(new Tooltip(StringUtils.getUiString(labelPrefix + ".delete")));

		toolbar.getItems().add(saveAllButton);
		toolbar.getItems().add(discardAllButton);
		toolbar.getItems().add(new Separator());
		toolbar.getItems().add(addButton);
		toolbar.getItems().add(duplicateButton);
		toolbar.getItems().add(renameButton);
		toolbar.getItems().add(deleteButton);

		table.setPrefWidth(1100);
		table.setPrefHeight(700);

		this.add(toolbar, "dock north, alignx left");
		this.add(table, "aligny top");

		//-------------

		table.setOnMouseClicked(event ->
		{
//			if (event.getClickCount() == 2)
//			{
//				Recipe recipe = (Recipe)table.getSelectionModel().getSelectedItem();
//				if (recipe != null)
//				{
//					recipe.run();
//
//					Stage dialogStage = new Stage();
//					dialogStage.initModality(Modality.APPLICATION_MODAL);
//					dialogStage.initOwner(((Node)event.getSource()).getScene().getWindow());
//					dialogStage.setTitle(recipe.getName());
//					dialogStage.getIcons().add(JfxUi.recipeIcon);
//
//					RecipeEditor recipeEditor = new RecipeEditor(recipe, this);
//
//					Scene scene = new Scene(recipeEditor, 1200, 750);
//					JfxUi.styleScene(scene);
//					dialogStage.setScene(scene);
//					dialogStage.show();

			// todo
//				}
//			}
		});

		discardAllButton.setOnAction(event ->
		{
			Alert alert = new Alert(
				Alert.AlertType.NONE,
				StringUtils.getUiString("editor.discard.all.msg"),
				ButtonType.OK, ButtonType.CANCEL);

			Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
			stage.getIcons().add(JfxUi.undoIcon);
			alert.setTitle(StringUtils.getUiString("editor.discard.all"));
			alert.setGraphic(JfxUi.getImageView(JfxUi.undoIcon, 32));

			JfxUi.styleScene(stage.getScene());

			alert.showAndWait();

			if (alert.getResult() == ButtonType.OK)
			{
				Database.getInstance().loadAll();
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
			stage.getIcons().add(JfxUi.saveIcon);
			alert.setTitle(StringUtils.getUiString("editor.apply.all"));
			alert.setGraphic(JfxUi.getImageView(JfxUi.saveIcon, 32));

			JfxUi.styleScene(stage.getScene());

			alert.showAndWait();

			if (alert.getResult() == ButtonType.OK)
			{
				Database.getInstance().saveAll();
				refresh(Database.getInstance());
			}
		});

		addButton.setOnAction(event ->
		{
			NewItemDialog<T> dialog = new NewItemDialog<>(labelPrefix, addIcon)
			{
				@Override
				public Map<String, T> getMap()
				{
					return RefIngredientPane.this.getMap(Database.getInstance());
				}

				@Override
				public T createNewItem(String name)
				{
					return RefIngredientPane.this.createNewItem(name);
				}
			};

			dialog.showAndWait();
			T result = dialog.getOutput();
			if (result != null)
			{
				tableModel.add(result);
				setDirty(result);
				table.getSelectionModel().select(result);
			}
		});

		deleteButton.setOnAction(event ->
		{
			T item = table.getSelectionModel().getSelectedItem();

			if (item != null)
			{
				Alert alert = new Alert(
					Alert.AlertType.NONE,
					StringUtils.getUiString(labelPrefix+".delete.msg"),
					ButtonType.OK, ButtonType.CANCEL);

				Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
				stage.getIcons().add(JfxUi.deleteIcon);
				alert.setTitle(StringUtils.getUiString(labelPrefix + ".delete"));
				alert.setGraphic(JfxUi.getImageView(JfxUi.deleteIcon, 32));

				JfxUi.styleScene(stage.getScene());

				alert.showAndWait();

				if (alert.getResult() == ButtonType.OK)
				{
					// cascading delete
					cascadeDelete(item.getName());

					tableModel.remove(item);
					setDirty(null);
				}
			}
		});

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
						return RefIngredientPane.this.getMap(Database.getInstance());
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
						return RefIngredientPane.this.getMap(Database.getInstance());
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
	}

	/*-------------------------------------------------------------------------*/

	protected abstract T createDuplicateItem(T current, String newName);

	protected abstract T createNewItem(String name);

	protected abstract Map<String, T> getMap(
		Database database);

	protected abstract TableColumn<T, String>[] getTableColumns(String labelPrefix);

	protected abstract void cascadeRename(String oldName, String newName);

	protected abstract void cascadeDelete(String deletedName);

	/*-------------------------------------------------------------------------*/
	public void refresh(Database database)
	{
		this.detectDirty = false;

		Map<String, T> ref = getMap(database);

		if (ref.size() > 0)
		{
			tableModel.refresh(ref);

			// start sorted by name
			TableColumn<T, ?> nameColumn = table.getColumns().get(0);
			nameColumn.setSortType(TableColumn.SortType.ASCENDING);
			table.getSortOrder().setAll(nameColumn);

			parent.clearDirty();
		}

		clearDirty();

		this.detectDirty = true;
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<T, String> getPropertyValueColumn(String heading,
		String property)
	{
		TableColumn<T, String> name = new TableColumn<>(getUiString(heading));
		name.setCellValueFactory(new PropertyValueFactory<>(property));
		return name;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void setDirty(Object obj)
	{
		T dirty = (T)obj;

		if (detectDirty)
		{
			if (dirty != null)
			{
				rowFactory.setDirty(dirty);
			}

			parent.setDirty(this.dirtyFlag);
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void clearDirty()
	{
		rowFactory.clearAllDirty();
	}
}
