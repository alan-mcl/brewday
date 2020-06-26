package mclachlan.brewday.ui.jfx;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import mclachlan.brewday.StringUtils;
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
		stage.getIcons().add(JfxUi.duplicateIcon);

		ButtonType okButtonType = new ButtonType(
			StringUtils.getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			StringUtils.getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(StringUtils.getUiString("common.duplicate"));
		this.setGraphic(JfxUi.getImageView(JfxUi.duplicateIcon, 32));

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
