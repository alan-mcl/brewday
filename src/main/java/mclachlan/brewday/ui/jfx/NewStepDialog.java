package mclachlan.brewday.ui.jfx;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.process.ProcessStep;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class NewStepDialog extends Dialog<ProcessStep.Type>
{
	private ProcessStep.Type output;

	public NewStepDialog()
	{
		Scene scene = this.getDialogPane().getScene();
		JfxUi.styleScene(scene);
		Stage stage = (Stage)scene.getWindow();
		stage.getIcons().add(JfxUi.addStep);

		ButtonType okButtonType = new ButtonType(
			StringUtils.getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			StringUtils.getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(StringUtils.getUiString("recipe.add.step"));
		this.setGraphic(JfxUi.getImageView(JfxUi.addStep, 32));

		MigPane content = new MigPane();
		ComboBox<ProcessStep.Type> stepType = new ComboBox<>();

		List<ProcessStep.Type> stepTypes = Arrays.asList(ProcessStep.Type.values());

		stepTypes.sort(Comparator.comparingInt(ProcessStep.Type::getSortOrder));
		stepType.setItems(FXCollections.observableList(stepTypes));

		Label stepDesc = new Label();
		stepDesc.setWrapText(true);
		stepDesc.setPrefWidth(300);
		stepDesc.setText(StringUtils.getUiString(stepTypes.get(0).getDescKey()));

		content.add(new Label(StringUtils.getUiString("process.step.type")));
		content.add(stepType, "wrap");

		content.add(stepDesc, "span");

		this.getDialogPane().setContent(content);
		stepType.getSelectionModel().select(0);

		// -----

		final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
		btOk.addEventFilter(ActionEvent.ACTION, event ->
		{
			output = stepType.getSelectionModel().getSelectedItem();
		});

		stepType.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null)
			{
				stepDesc.setText(StringUtils.getUiString(newValue.getDescKey()));
			}
		});
	}

	public ProcessStep.Type getOutput()
	{
		return output;
	}
}
