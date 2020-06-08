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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.TilePane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.recipe.Recipe;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class RecipesPane3 extends MigPane implements TrackDirty
{
	private final DirtyTableViewRowFactory dirtyTableViewRowFactory;
	private Map<String, Recipe> map;
	private String dirtyFlag;
	private boolean detectDirty = true;
	private TrackDirty parent;

	private TableView recipeTable;

	// recipe operation buttons
	private Button addRecipeButton, copyButton, renameButton, deleteButton,
		saveAllButton, discardAllButton;
	public static final int SIZE = 32;

	/*-------------------------------------------------------------------------*/

	public RecipesPane3(String dirtyFlag, TrackDirty parent)
	{
		this.parent = parent;
		this.dirtyFlag = dirtyFlag;

		this.recipeTable = new TableView<>();
		TableColumn<String, Recipe> name = new TableColumn<>("Name");
		name.setCellValueFactory(new PropertyValueFactory<>("name"));
		TableColumn<String, Recipe> equipmentProfile = new TableColumn<>("Equipment Profile");
		equipmentProfile.setCellValueFactory(new PropertyValueFactory<>("equipmentProfile"));

		recipeTable.getColumns().add(name);
		recipeTable.getColumns().add(equipmentProfile);
		dirtyTableViewRowFactory = new DirtyTableViewRowFactory(recipeTable);
		recipeTable.setRowFactory(dirtyTableViewRowFactory);

		TilePane recipeActionsBar = new TilePane(3, 3);
		recipeActionsBar.setPadding(new Insets(0, 0, 3, 0));

		saveAllButton = new Button(null, JfxUi.getImageView(JfxUi.saveIcon, SIZE));
		discardAllButton = new Button(null, JfxUi.getImageView(JfxUi.undoIcon, SIZE));
		addRecipeButton = new Button(null, JfxUi.getImageView(JfxUi.addRecipe, SIZE));
		copyButton = new Button(null, JfxUi.getImageView(JfxUi.duplicateIcon, SIZE));
		renameButton = new Button(null, JfxUi.getImageView(JfxUi.renameIcon, SIZE));
		deleteButton = new Button(null, JfxUi.getImageView(JfxUi.deleteIcon, SIZE));

		recipeActionsBar.getChildren().add(addRecipeButton);
		recipeActionsBar.getChildren().add(copyButton);
		recipeActionsBar.getChildren().add(renameButton);
		recipeActionsBar.getChildren().add(deleteButton);

		recipeTable.setPrefWidth(1100);
		recipeTable.setPrefHeight(700);

		this.add(recipeActionsBar, "dock north, alignx left");
		this.add(recipeTable, "alignx left, aligny top");

		//-------------

		recipeTable.setOnMouseClicked(event ->
		{
			if (event.getClickCount() == 2)
			{
				Recipe recipe = (Recipe)recipeTable.getSelectionModel().getSelectedItem();
				recipe.run();

				Stage dialogStage = new Stage();
				dialogStage.initModality(Modality.WINDOW_MODAL);
				dialogStage.setTitle(recipe.getName());
				dialogStage.getIcons().add(JfxUi.recipeIcon);

				RecipeEditor recipeEditor = new RecipeEditor(recipe, this);

				dialogStage.setScene(new Scene(recipeEditor, 1200, 750));
				dialogStage.show();
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
			List<Recipe> recipes = new ArrayList<>(this.map.values());
			recipes.sort(Comparator.comparing(Recipe::getName));

			recipeTable.getItems().clear();
			recipeTable.getItems().addAll(recipes);
		}

		this.detectDirty = true;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void setDirty(Object dirty)
	{
		Recipe recipe = (Recipe)dirty;

		if (detectDirty)
		{
			if (recipe != null)
			{
				dirtyTableViewRowFactory.setDirty(recipe);
			}

			parent.setDirty(this.dirtyFlag);
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void clearDirty()
	{
		dirtyTableViewRowFactory.clearAllDirty();
	}

	/*-------------------------------------------------------------------------*/
	private static class DirtyList<T extends V2DataObject> extends ListView<Label>
	{
		private Map<T, Label> nodes = new HashMap<>();
		private Map<Label, T> values = new HashMap<>();

		public void add(T t, Image graphic)
		{
			Label label = new DirtyLabel<>(t, t.getName(), JfxUi.getImageView(graphic, 24));
			nodes.put(t, label);
			values.put(label, t);
			super.getItems().add(label);
		}

		public void addAll(List<T> ts, Image graphic)
		{
			for (T t : ts)
			{
				add(t, graphic);
			}
		}

		public T getValue(Label label)
		{
			return values.get(label);
		}

		public void removeAll()
		{
			this.getChildren().clear();
		}

		public void select(T t)
		{
			getSelectionModel().select(nodes.get(t));
		}

		public void setDirty(T t)
		{
			Label label = nodes.get(t);
			label.setStyle("-fx-font-weight: bold;");
		}

		public void clearAllDirty()
		{
			for (Label l : nodes.values())
			{
				l.setStyle("");
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private static class DirtyLabel<T> extends Label
	{
		private T t;

		public DirtyLabel(T t, String text, Node graphic)
		{
			super(text, graphic);
			this.t = t;
		}
	}

	/*-------------------------------------------------------------------------*/
	private class DirtyTableViewRowFactory implements Callback<TableView, TableRow>
	{
		private TableView tableView;
		private Map<Recipe, TableRow> map = new HashMap<>();

		public DirtyTableViewRowFactory(TableView tableView)
		{
			this.tableView = tableView;
		}

		public void setDirty(Recipe recipe)
		{
			TableRow tableRow = map.get(recipe);
			tableRow.setStyle("-fx-font-weight: bold");
			refreshTableView();
		}

		private void refreshTableView()
		{
//			tableView.refresh();
			// this dodgy hack required because the above does not do the trick
			((TableColumn)(tableView.getColumns().get(0))).setVisible(false);
			((TableColumn)(tableView.getColumns().get(0))).setVisible(true);
		}

		public void clearAllDirty()
		{
			map.clear();
			for (TableRow row : map.values())
			{
				row.setStyle("");
			}
			refreshTableView();
		}

		@Override
		public TableRow call(TableView param)
		{
			TableRow row = new TableRow()
			{
				@Override
				protected void updateItem(Object item, boolean empty)
				{
					super.updateItem(item, empty);
					map.put((Recipe)item, this);
				}
			};
			return row;
		}
	}
}
