package mclachlan.brewday.ui.jfx;

import java.util.Map;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.v2.V2DataObject;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
abstract class NewItemDialog<T extends V2DataObject> extends Dialog<T>
{
	private final Label warningLabel;
	private T output;

	public NewItemDialog(final String labelPrefix, Image icon)
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

		this.setTitle(StringUtils.getUiString(labelPrefix + ".add"));
		this.setGraphic(JfxUi.getImageView(icon, 32));

		MigPane content = new MigPane();

		TextField name = new TextField("");
		name.setPrefWidth(200);
		warningLabel = new Label(StringUtils.getUiString(labelPrefix+".new.dialog.not.empty"));
		warningLabel.setWrapText(true);

		content.add(new Label(StringUtils.getUiString(labelPrefix + ".name")));
		content.add(name, "wrap");

		content.add(warningLabel, "span");

		this.getDialogPane().setContent(content);
		this.getDialogPane().lookupButton(okButtonType).setDisable(true);

		name.requestFocus();

		// -----

		final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
		btOk.addEventFilter(ActionEvent.ACTION, event ->
		{
			output = createNewItem(name.getText());
		});

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

	/*-------------------------------------------------------------------------*/
	public abstract Map<String, T> getMap();

	public abstract T createNewItem(String name);

	public T getOutput()
	{
		return output;
	}
}
