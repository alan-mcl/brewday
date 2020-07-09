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

import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class RecipesPane3 extends MigPane implements TrackDirty
{
	private Map<String, Recipe> map;
	private final String dirtyFlag;
	private boolean detectDirty = true;
	private final TrackDirty parent;

	private final TableView<Recipe> recipeTable;
	private final V2DataObjectTableModel<Recipe> recipeTableModel;
	private final DirtyTableViewRowFactory<Recipe> rowFactory;

	public static final int ICON_SIZE = 32;

	/*-------------------------------------------------------------------------*/

	public RecipesPane3(String dirtyFlag, TrackDirty parent)
	{
		super("insets 3");

		this.parent = parent;
		this.dirtyFlag = dirtyFlag;

		this.recipeTable = new TableView<>();
		this.recipeTableModel = new V2DataObjectTableModel<>(recipeTable);
		TableColumn<Recipe, String> nameColumn = new TableColumn<>("Name");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		TableColumn<Recipe, String> equipmentProfileColumn = new TableColumn<>("Equipment Profile");
		equipmentProfileColumn.setCellValueFactory(new PropertyValueFactory<>("equipmentProfile"));

		recipeTable.getColumns().add(nameColumn);
		recipeTable.getColumns().add(equipmentProfileColumn);

		rowFactory = new DirtyTableViewRowFactory<>(recipeTable);
		recipeTable.setRowFactory(rowFactory);

		ToolBar recipeToolBar = new ToolBar();
		recipeToolBar.setPadding(new Insets(3, 3, 6, 3));

		Button saveAllButton = new Button(StringUtils.getUiString("editor.apply.all"), JfxUi.getImageView(JfxUi.saveIcon, ICON_SIZE));
		Button discardAllButton = new Button(StringUtils.getUiString("editor.discard.all"), JfxUi.getImageView(JfxUi.undoIcon, ICON_SIZE));
		// recipe operation buttons
		Button addButton = new Button(StringUtils.getUiString("recipe.add"), JfxUi.getImageView(JfxUi.addRecipe, ICON_SIZE));
		Button duplicateButton = new Button(StringUtils.getUiString("recipe.copy"), JfxUi.getImageView(JfxUi.duplicateIcon, ICON_SIZE));
		Button renameButton = new Button(StringUtils.getUiString("recipe.rename"), JfxUi.getImageView(JfxUi.renameIcon, ICON_SIZE));
		Button deleteButton = new Button(StringUtils.getUiString("recipe.delete"), JfxUi.getImageView(JfxUi.deleteIcon, ICON_SIZE));

		saveAllButton.setTooltip(new Tooltip(StringUtils.getUiString("editor.apply.all")));
		discardAllButton.setTooltip(new Tooltip(StringUtils.getUiString("editor.discard.all")));
		addButton.setTooltip(new Tooltip(StringUtils.getUiString("recipe.add")));
		duplicateButton.setTooltip(new Tooltip(StringUtils.getUiString("recipe.copy")));
		renameButton.setTooltip(new Tooltip(StringUtils.getUiString("recipe.rename")));
		deleteButton.setTooltip(new Tooltip(StringUtils.getUiString("recipe.delete")));

		recipeToolBar.getItems().add(saveAllButton);
		recipeToolBar.getItems().add(discardAllButton);
		recipeToolBar.getItems().add(new Separator());
		recipeToolBar.getItems().add(addButton);
		recipeToolBar.getItems().add(duplicateButton);
		recipeToolBar.getItems().add(renameButton);
		recipeToolBar.getItems().add(deleteButton);

		recipeTable.setPrefWidth(1100);
		recipeTable.setPrefHeight(700);

		this.add(recipeToolBar, "dock north, alignx left");
		this.add(recipeTable, "aligny top");

		//-------------

		recipeTable.setOnMouseClicked(event ->
		{
			if (event.getClickCount() == 2)
			{
				Recipe recipe = (Recipe)recipeTable.getSelectionModel().getSelectedItem();
				if (recipe != null)
				{
					recipe.run();

					Stage dialogStage = new Stage();
					dialogStage.initModality(Modality.APPLICATION_MODAL);
					dialogStage.initOwner(((Node)event.getSource()).getScene().getWindow());
					dialogStage.setTitle(recipe.getName());
					dialogStage.getIcons().add(JfxUi.recipeIcon);

					RecipeEditor recipeEditor = new RecipeEditor(recipe, this);

					Scene scene = new Scene(recipeEditor, 1200, 750);
					JfxUi.styleScene(scene);
					dialogStage.setScene(scene);
					dialogStage.show();
				}
			}
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
			NewRecipeDialog dialog = new NewRecipeDialog();

			dialog.showAndWait();
			Recipe result = dialog.getOutput();
			if (result != null)
			{
				recipeTableModel.add(result);
				setDirty(result);
			}
		});

		deleteButton.setOnAction(event ->
		{
			Recipe recipe = (Recipe)recipeTable.getSelectionModel().getSelectedItem();

			if (recipe != null)
			{
				Alert alert = new Alert(
					Alert.AlertType.NONE,
					StringUtils.getUiString("editor.delete.msg"),
					ButtonType.OK, ButtonType.CANCEL);

				Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
				stage.getIcons().add(JfxUi.deleteIcon);
				alert.setTitle(StringUtils.getUiString("recipe.delete"));
				alert.setGraphic(JfxUi.getImageView(JfxUi.deleteIcon, 32));

				JfxUi.styleScene(stage.getScene());

				alert.showAndWait();

				if (alert.getResult() == ButtonType.OK)
				{
					recipeTableModel.remove(recipe);
				}
			}
		});

		duplicateButton.setOnAction(event ->
		{
			Recipe recipe = (Recipe)recipeTable.getSelectionModel().getSelectedItem();

			if (recipe != null)
			{
				DuplicateRecipeDialog dialog = new DuplicateRecipeDialog(recipe);

				dialog.showAndWait();
				Recipe result = dialog.getOutput();
				if (result != null)
				{
					recipeTableModel.add(result);
					setDirty(result);
				}
			}
		});

		renameButton.setOnAction(event ->
		{
			Recipe recipe = (Recipe)recipeTable.getSelectionModel().getSelectedItem();

			if (recipe != null)
			{
				RenameRecipeDialog dialog = new RenameRecipeDialog(recipe);

				dialog.showAndWait();
				String result = dialog.getOutput();
				if (result != null)
				{
					recipeTableModel.remove(recipe);
					recipe.setName(result);
					recipeTableModel.add(recipe);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	public Map<String, Recipe> getMap()
	{
		return map;
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Database db)
	{
		this.detectDirty = false;

		this.map = db.getRecipes();
		if (map.size() > 0)
		{
			recipeTableModel.refresh(map);

			// start sorted by name
			TableColumn<Recipe, ?> nameColumn = recipeTable.getColumns().get(0);
			nameColumn.setSortType(TableColumn.SortType.ASCENDING);
			recipeTable.getSortOrder().setAll(nameColumn);

			parent.clearDirty();
		}

		clearDirty();

		this.detectDirty = true;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void setDirty(Object... objs)
	{
		if (detectDirty)
		{
			for (Object obj : objs)
			{
				if (obj instanceof Recipe)
				{
					Recipe recipe = (Recipe)obj;
					if (recipe != null)
					{
						rowFactory.setDirty(recipe);
					}
				}
				parent.setDirty(this.dirtyFlag, obj);
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
