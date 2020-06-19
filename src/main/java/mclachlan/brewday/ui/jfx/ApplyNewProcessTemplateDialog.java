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
		stage.getIcons().add(JfxUi.processTemplateIcon);

		ButtonType okButtonType = new ButtonType(
			StringUtils.getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			StringUtils.getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(StringUtils.getUiString("recipe.apply.process.template"));
		this.setGraphic(JfxUi.getImageView(JfxUi.processTemplateIcon, 32));

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
