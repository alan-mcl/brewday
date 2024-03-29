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
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.v2.V2DataObject;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
abstract class DuplicateDialog<T extends V2DataObject> extends Dialog<T>
{
	private final Label warningLabel;
	private T output;

	public DuplicateDialog(T current)
	{
		Stage stage = (Stage)this.getDialogPane().getScene().getWindow();
		JfxUi.styleScene(stage.getScene());
		stage.getIcons().add(Icons.duplicateIcon);

		ButtonType okButtonType = new ButtonType(
			StringUtils.getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			StringUtils.getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(StringUtils.getUiString("common.duplicate"));
		this.setGraphic(JfxUi.getImageView(Icons.duplicateIcon, 32));

		MigPane content = new MigPane();

		TextField newName = new TextField("");
		content.add(new Label(StringUtils.getUiString("common.duplicate.name")));
		content.add(newName, "wrap");
		warningLabel = new Label(StringUtils.getUiString("common.new.dialog.not.empty"));

		content.add(warningLabel, "span");

		this.getDialogPane().setContent(content);
		this.getDialogPane().lookupButton(okButtonType).setDisable(true);

		newName.requestFocus();

		// -----

		final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
		btOk.addEventFilter(ActionEvent.ACTION, event ->
		{
			output = createDuplicate(current, newName.getText());
		});

		newName.textProperty().addListener((observable, oldValue, newValue) ->
		{
			boolean empty = newValue == null || newValue.length() == 0;
			boolean exists = !empty && isDuplicate(newValue);

			if (empty)
			{
				warningLabel.setText(StringUtils.getUiString("common.new.dialog.not.empty"));
			}
			else if (exists)
			{
				warningLabel.setText(StringUtils.getUiString("common.new.dialog.already.exists"));
			}
			else
			{
				warningLabel.setText("");
			}

			this.getDialogPane().lookupButton(okButtonType).setDisable(empty || exists);
		});
	}

	protected abstract boolean isDuplicate(String newValue);

	protected abstract T createDuplicate(T current, String newName);

	public T getOutput()
	{
		return output;
	}
}
