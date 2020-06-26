package mclachlan.brewday.ui.jfx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.v2.V2DataObject;
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

	/*-------------------------------------------------------------------------*/
	public IngredientAdditionDialog(Image icon, String titleKey, ProcessStep step)
	{
		this.step = step;
		Scene scene = this.getDialogPane().getScene();
		JfxUi.styleScene(scene);
		Stage stage = (Stage)scene.getWindow();
		stage.getIcons().add(icon);

		ButtonType okButtonType = new ButtonType(
			StringUtils.getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			StringUtils.getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(StringUtils.getUiString(titleKey));

		MigPane content = new MigPane();

		TableView<S> tableview = new TableView<>();
		tableview.setPrefWidth(800);

		TableColumn<S, String>[] columns = getColumns();
		tableview.getColumns().addAll(columns);

		MigPane top = new MigPane();

		Label searchIcon = new Label(null, JfxUi.getImageView(JfxUi.searchIcon, 32));
		top.getChildren().add(searchIcon);

		TextField searchString = new TextField();
		searchString.setPrefWidth(400);
		top.getChildren().add(searchString);

		MigPane bottom = new MigPane();

		addUiStuffs(bottom);

		content.add(top, "dock north");
		content.add(tableview, "dock center");
		content.add(bottom, "dock south");

		ArrayList<S> refIngredients = new ArrayList<>(getReferenceIngredients().values());
		refIngredients.sort(Comparator.comparing(V2DataObject::getName));

		ObservableList<S> observableList = FXCollections.observableList(refIngredients);
		FilteredList<S> filteredList = new FilteredList<>(observableList);
		tableview.setItems(filteredList);

		TableColumn<S, String> pk = columns[0];
		pk.setSortType(TableColumn.SortType.ASCENDING);
		tableview.getSortOrder().setAll(pk);

		this.getDialogPane().setContent(content);

		searchString.requestFocus();

		// -------

		searchString.textProperty().addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null)
			{
				filteredList.setPredicate(s -> s.getName().toLowerCase().contains(newValue.toLowerCase()));
			}
		});

		final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
		btOk.addEventFilter(ActionEvent.ACTION, event ->
		{
			S selectedItem = (S)tableview.getSelectionModel().getSelectedItem();
			if (selectedItem != null)
			{
				output = createIngredientAddition(selectedItem);
			}
		});
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
}
