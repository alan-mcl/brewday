package mclachlan.brewday.ui.jfx;

import java.util.ArrayList;
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
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;
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

	private ProcessStep step;

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

		QuantityEditWidget<WeightUnit> amount = new QuantityEditWidget<>(Quantity.Unit.GRAMS);
		bottom.add(new Label(StringUtils.getUiString("recipe.amount")));
		bottom.add(amount, "wrap");

		QuantityEditWidget<TimeUnit> time = new QuantityEditWidget<>(Quantity.Unit.MINUTES);
		bottom.add(new Label(StringUtils.getUiString("recipe.time")));
		bottom.add(time, "wrap");

		addUiStuffs(bottom);

		content.add(top, "dock north");
		content.add(tableview, "dock center");
		content.add(bottom, "dock south");

		ArrayList<S> refIngredients = new ArrayList<>(getReferenceIngredients().values());
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
			output = createIngredientAddition(selectedItem, amount.getQuantity(), time.getQuantity());
		});
	}

	/*-------------------------------------------------------------------------*/
	public ProcessStep getStep()
	{
		return step;
	}

	/*-------------------------------------------------------------------------*/
	protected void addUiStuffs(MigPane pane)
	{
	}

	/*-------------------------------------------------------------------------*/
	protected abstract T createIngredientAddition(S selectedItem, WeightUnit additionAmount, TimeUnit additionTime);

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
