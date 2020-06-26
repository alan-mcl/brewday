package mclachlan.brewday.ui.jfx;

import java.util.Locale;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;

/**
 *
 */
public class QuantityEditWidget<T extends Quantity> extends HBox
{
	private final TextField textfield;
	private Quantity.Unit unit;
	private final Label unitLabel;

	/*-------------------------------------------------------------------------*/
	public QuantityEditWidget(Quantity.Unit unit)
	{
		this.setAlignment(Pos.CENTER_LEFT);

		textfield = new TextField();
//		textfield.setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0d));

		unitLabel = new Label();

		textfield.setAlignment(Pos.CENTER);
		unitLabel.setAlignment(Pos.CENTER_LEFT);

		textfield.setPrefWidth(75);

		this.unit = unit;
		unitLabel.setText(" "+ StringUtils.getUiString("unit." + unit.name()));

		this.getChildren().add(textfield);
		this.getChildren().add(unitLabel);
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Quantity quantity)
	{
		Settings settings = Database.getInstance().getSettings();

		if (quantity != null)
		{
			double v = quantity.get(unit);

			String formatter = settings.getStringFormatter(v);

			// passing Locale.ROOT here to force a '.' decimal separator... to work with JMetro...
			String format = String.format(Locale.ROOT, formatter, v);

			this.textfield.setText(format);
		}
		else
		{
			this.textfield.clear();
		}
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Quantity quantity, Quantity.Unit unit)
	{
		this.unit = unit;
		refresh(quantity);
		unitLabel.setText(" "+ StringUtils.getUiString("unit." + unit.name()));
	}

	/*-------------------------------------------------------------------------*/
	public T getQuantity()
	{
		return (T)Quantity.parseQuantity(textfield.getText(), unit);
	}

	/*-------------------------------------------------------------------------*/
	public void addListener(ChangeListener<String> listener)
	{
		textfield.textProperty().addListener(listener);
	}
}
