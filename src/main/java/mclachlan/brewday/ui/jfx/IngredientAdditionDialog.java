package mclachlan.brewday.ui.jfx;

import java.util.ArrayList;
import java.util.Collection;
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
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import org.tbee.javafx.scene.layout.MigPane;

import static mclachlan.brewday.StringUtils.getUiString;

/**
 *
 */
class IngredientAdditionDialog<T extends IngredientAddition, S extends V2DataObject> extends Dialog<IngredientAddition>
{
	private T output;

	public IngredientAdditionDialog(Image icon, String titleKey)
	{
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
//		this.setGraphic(JfxUi.getImageView(icon, 32));

		MigPane content = new MigPane();

		TableView<S> tableview = new TableView<>();
		tableview.setPrefWidth(800);

		TableColumn<S, String> name = getPropertyValueTableColumn("fermentable.name", "name");
		TableColumn<S, String> type = getPropertyValueTableColumn("fermentable.type", "type");
		TableColumn<S, String> origin = getPropertyValueTableColumn("fermentable.origin", "origin");
		TableColumn<S, String> colour = getPropertyValueTableColumn("fermentable.colour", "colour");

		tableview.getColumns().add(name);
		tableview.getColumns().add(type);
		tableview.getColumns().add(origin);
		tableview.getColumns().add(colour);

		MigPane top = new MigPane();

		Label searchIcon = new Label(null, JfxUi.getImageView(JfxUi.searchIcon, 32));
		top.getChildren().add(searchIcon);

		TextField searchString = new TextField();
		searchString.setPrefWidth(400);
		top.getChildren().add(searchString);

		MigPane bottom = new MigPane();

		TextField amount = new TextField();
		bottom.getChildren().add(new Label(StringUtils.getUiString("recipe.amount")));
		bottom.getChildren().add(amount);

		TextField time = new TextField();
		bottom.getChildren().add(new Label(StringUtils.getUiString("recipe.time")));
		bottom.getChildren().add(time);

		content.add(top, "dock north");
		content.add(tableview, "dock center");
		content.add(bottom, "dock south");

		ArrayList<S> fermentables = new ArrayList<S>((Collection<? extends S>)Database.getInstance().getFermentables().values());
		ObservableList<S> observableList = FXCollections.observableList(fermentables);
		FilteredList<S> filteredList = new FilteredList<S>(observableList);

		tableview.setItems(filteredList);

		name.setSortType(TableColumn.SortType.ASCENDING);
		tableview.getSortOrder().setAll(name);

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
			Fermentable fermentable = (Fermentable)tableview.getSelectionModel().getSelectedItem();
			double additionAmount = Double.parseDouble(amount.getText());
			double additionTime = Double.parseDouble(time.getText());

			output = (T)new FermentableAddition(fermentable,
				new WeightUnit(additionAmount, Quantity.Unit.GRAMS, false),
				new TimeUnit(additionTime, Quantity.Unit.MINUTES, false));
		});
	}

	public T getOutput()
	{
		return output;
	}

	protected TableColumn<S, String> getPropertyValueTableColumn(String heading, String property)
	{
		TableColumn<S, String> name = new TableColumn<>(getUiString(heading));
		name.setCellValueFactory(new PropertyValueFactory<>(property));
		return name;
	}
}
