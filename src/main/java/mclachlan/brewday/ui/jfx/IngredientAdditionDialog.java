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
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import org.tbee.javafx.scene.layout.MigPane;

import static mclachlan.brewday.StringUtils.getUiString;

/**
 *
 */
abstract class IngredientAdditionDialog<T extends IngredientAddition, S extends V2DataObject> extends Dialog<IngredientAddition>
{
	private T output;

	private final ProcessStep step;

	private final boolean captureTime;

	/*-------------------------------------------------------------------------*/
	public IngredientAdditionDialog(Image icon, String titleKey, ProcessStep step, boolean captureTime)
	{
		this.step = step;
		this.captureTime = captureTime;
		Scene scene = this.getDialogPane().getScene();
		JfxUi.styleScene(scene);
		Stage stage = (Stage)scene.getWindow();
		stage.getIcons().add(icon);

		ButtonType okButtonType = new ButtonType(
			getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(getUiString(titleKey));

		MigPane content = new MigPane();

		TableView<S> tableView = new TableView<>();
		tableView.setPrefWidth(800);

		TableColumn<S, String>[] columns = getColumns();
		tableView.getColumns().addAll(columns);

		MigPane top = new MigPane();

		Label searchIcon = new Label(null, JfxUi.getImageView(Icons.searchIcon, 32));
		top.getChildren().add(searchIcon);

		TextField searchString = new TextField();
		searchString.setPrefWidth(400);
		top.getChildren().add(searchString);

		MigPane bottom = new MigPane();

		addUiStuffs(bottom);

		content.add(top, "dock north");
		content.add(tableView, "dock center");
		content.add(bottom, "dock south");

		ArrayList<S> refIngredients = new ArrayList<>(getReferenceIngredients().values());

		ObservableList<S> unfilteredList = FXCollections.observableArrayList();
		FilteredList<S> filteredList = new FilteredList<>(unfilteredList);
		SortedList<S> sortedList = new SortedList<>(filteredList);
		sortedList.comparatorProperty().bind(tableView.comparatorProperty());

		unfilteredList.addAll(refIngredients);

		tableView.setItems(sortedList);

		// initial table sort order
		TableColumn<S, ?> pk = tableView.getColumns().get(0);
		pk.setSortType(TableColumn.SortType.ASCENDING);
		tableView.getSortOrder().setAll(pk);
		tableView.sort();

		this.getDialogPane().setContent(content);

		// todo this is not working, maybe the dialog superclass is putting focus on the OK button?
		searchString.requestFocus();

		// -------

		searchString.textProperty().addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null)
			{
				filteredList.setPredicate(s -> getFilterPredicate(newValue, s));
			}
		});

		final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
		btOk.addEventFilter(ActionEvent.ACTION, event ->
		{
			S selectedItem = tableView.getSelectionModel().getSelectedItem();
			if (selectedItem != null)
			{
				output = createIngredientAddition(selectedItem);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	protected boolean getFilterPredicate(String searchText, S s)
	{
		return s.getName().toLowerCase().contains(searchText.toLowerCase());
	}

	/*-------------------------------------------------------------------------*/
	public ProcessStep getStep()
	{
		return step;
	}

	/*-------------------------------------------------------------------------*/
	protected abstract IngredientAddition.Type getIngredientType();

	/*-------------------------------------------------------------------------*/
	protected void addUiStuffs(MigPane pane)
	{
	}

	/*-------------------------------------------------------------------------*/
	protected abstract T createIngredientAddition(S selectedItem);

	/*-------------------------------------------------------------------------*/
	protected abstract Map<String, S> getReferenceIngredients();

	/*-------------------------------------------------------------------------*/
	/**
	 * @return the columns of this table. The initial sort column is expected to be in the first position.
	 */
	protected abstract TableColumn<S, String>[] getColumns();

	/*-------------------------------------------------------------------------*/
	public T getOutput()
	{
		return output;
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<S, String> getPropertyValueTableColumn(String heading, String property)
	{
		TableColumn<S, String> name = new TableColumn<>(getUiString(heading));
		name.setCellValueFactory(new PropertyValueFactory<>(property));
		return name;
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<S, Double> getQuantityPropertyValueCol(
		String heading,
		Function<S, Quantity> getter,
		Quantity.Unit unit)
	{
		TableColumn<S, Double> col = new TableColumn<>(getUiString(heading));
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
	public boolean isCaptureTime()
	{
		return captureTime;
	}
}
