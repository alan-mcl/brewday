package mclachlan.brewday.ui.jfx;

import java.util.Map;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.v2.V2DataObject;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
abstract class DuplicateItemDialog<T extends V2DataObject> extends Dialog<T>
{
	private final Label warningLabel;
	private T output;

	public DuplicateItemDialog(T current, final String labelPrefix, Image icon)
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

		this.setTitle(StringUtils.getUiString(labelPrefix+".duplicate"));
		this.setGraphic(JfxUi.getImageView(JfxUi.duplicateIcon, 32));

		MigPane content = new MigPane();

		TextField name = new TextField("");
		content.add(new Label(StringUtils.getUiString(labelPrefix+".name")));
		content.add(name, "wrap");
		warningLabel = new Label(StringUtils.getUiString(labelPrefix+".new.dialog.not.empty"));

		content.add(warningLabel, "span");

		this.getDialogPane().setContent(content);
		this.getDialogPane().lookupButton(okButtonType).setDisable(true);

		name.requestFocus();

		// -----

		final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
		btOk.addEventFilter(ActionEvent.ACTION, event -> output = createItem(current, name));

		name.textProperty().addListener((observable, oldValue, newValue) ->
		{
			boolean empty = newValue == null || newValue.length() == 0;
			boolean exists = !empty && getMap().containsKey(newValue);

			if (empty)
			{
				warningLabel.setText(StringUtils.getUiString(labelPrefix+".new.dialog.not.empty"));
			}
			else if (exists)
			{
				warningLabel.setText(StringUtils.getUiString(labelPrefix+".new.dialog.already.exists"));
			}
			else
			{
				warningLabel.setText("");
			}

			this.getDialogPane().lookupButton(okButtonType).setDisable(empty || exists);


		});
	}

	public abstract Map<String, T> getMap();

	public abstract T createItem(T current, TextField name);

	public T getOutput()
	{
		return output;
	}
}
