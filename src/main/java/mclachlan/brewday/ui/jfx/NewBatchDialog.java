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
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class NewBatchDialog extends Dialog<Batch>
{
	private final Label warningLabel;
	private Batch output;

	public NewBatchDialog()
	{
		Scene scene = this.getDialogPane().getScene();
		JfxUi.styleScene(scene);
		Stage stage = (Stage)scene.getWindow();
		stage.getIcons().add(Icons.newIcon);

		ButtonType okButtonType = new ButtonType(
			StringUtils.getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			StringUtils.getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(StringUtils.getUiString("batch.add"));
		this.setGraphic(JfxUi.getImageView(Icons.newIcon, 32));

		MigPane content = new MigPane();
		ComboBox<String> recipe = new ComboBox<>();

		ArrayList<String> recipes = new ArrayList<>(
			Database.getInstance().getRecipes().keySet());
		recipes.sort(String::compareTo);
		recipe.setItems(FXCollections.observableList(recipes));
		recipe.getSelectionModel().select(0);

		DatePicker datePicker = new DatePicker();

		content.add(new Label(StringUtils.getUiString("batch.date")));
		content.add(datePicker, "wrap");
		content.add(new Label(StringUtils.getUiString("batch.recipe")));
		content.add(recipe, "wrap");
		warningLabel = new Label();
		warningLabel.setWrapText(true);

		content.add(warningLabel, "span");

		this.getDialogPane().setContent(content);
		Button okButton = (Button)this.getDialogPane().lookupButton(okButtonType);
		okButton.setDisable(true);

		datePicker.requestFocus();

		// -----

		okButton.addEventFilter(ActionEvent.ACTION, event ->
		{
			output = Brewday.getInstance().createNewBatch(
				recipe.getSelectionModel().getSelectedItem(),
				datePicker.getValue());
		});

		datePicker.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null)
			{
				okButton.setDisable(false);
			}
		});
	}

	public Batch getOutput()
	{
		return output;
	}
}
