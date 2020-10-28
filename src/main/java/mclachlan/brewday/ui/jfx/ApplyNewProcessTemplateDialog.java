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

import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.ProcessStep;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class ApplyNewProcessTemplateDialog extends Dialog<ProcessStep.Type>
{
	private String output;

	public ApplyNewProcessTemplateDialog()
	{
		Scene scene = this.getDialogPane().getScene();
		JfxUi.styleScene(scene);
		Stage stage = (Stage)scene.getWindow();
		stage.getIcons().add(Icons.processTemplateIcon);

		ButtonType okButtonType = new ButtonType(
			StringUtils.getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			StringUtils.getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(StringUtils.getUiString("recipe.apply.process.template"));
		this.setGraphic(JfxUi.getImageView(Icons.processTemplateIcon, 32));

		MigPane content = new MigPane();
		ComboBox<String> processTemplate = new ComboBox<>();

		List<String> processTemplates = new ArrayList<>(Database.getInstance().getProcessTemplates().keySet());

		processTemplates.sort(String::compareTo);
		processTemplate.setItems(FXCollections.observableList(processTemplates));

		Label blurb = new Label();
		blurb.setWrapText(true);
		blurb.setPrefWidth(300);
		blurb.setText(StringUtils.getUiString("recipe.apply.process.template.msg2"));

		content.add(new Label(StringUtils.getUiString("recipe.process.template")));
		content.add(processTemplate, "wrap");

		content.add(blurb, "span");

		this.getDialogPane().setContent(content);
		processTemplate.getSelectionModel().select(0);

		// -----

		final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
		btOk.addEventFilter(ActionEvent.ACTION, event ->
		{
			output = processTemplate.getSelectionModel().getSelectedItem();
		});
	}

	public String getOutput()
	{
		return output;
	}
}
