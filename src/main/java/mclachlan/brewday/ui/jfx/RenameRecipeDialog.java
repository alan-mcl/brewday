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

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class RenameRecipeDialog extends Dialog<String>
{
	private final Label warningLabel;
	private String output;

	public RenameRecipeDialog(Recipe current)
	{
		Stage stage = (Stage)this.getDialogPane().getScene().getWindow();
		JfxUi.styleScene(stage.getScene());
		stage.getIcons().add(JfxUi.renameIcon);

		ButtonType okButtonType = new ButtonType(
			StringUtils.getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			StringUtils.getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(StringUtils.getUiString("recipe.rename"));
		this.setGraphic(JfxUi.getImageView(JfxUi.renameIcon, 32));

		MigPane content = new MigPane();

		TextField recipeName = new TextField(current.getName());
		recipeName.selectAll();
		content.add(new Label(StringUtils.getUiString("recipe.name")));
		content.add(recipeName, "wrap");
		warningLabel = new Label();

		content.add(warningLabel, "span");

		this.getDialogPane().setContent(content);
		this.getDialogPane().lookupButton(okButtonType).setDisable(true);

		recipeName.requestFocus();

		// -----

		final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
		btOk.addEventFilter(ActionEvent.ACTION, event ->
		{
			output = recipeName.getText();
		});

		recipeName.textProperty().addListener((observable, oldValue, newValue) ->
		{
			boolean empty = newValue == null || newValue.length() == 0;
			boolean exists = !empty && Database.getInstance().getRecipes().containsKey(newValue);

			if (empty)
			{
				warningLabel.setText(StringUtils.getUiString("recipe.new.dialog.not.empty"));
			}
			else if (exists)
			{
				warningLabel.setText(StringUtils.getUiString("recipe.new.dialog.already.exists"));
			}
			else
			{
				warningLabel.setText("");
			}

			this.getDialogPane().lookupButton(okButtonType).setDisable(empty || exists);


		});
	}

	public String getOutput()
	{
		return output;
	}
}
