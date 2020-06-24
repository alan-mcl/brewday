package mclachlan.brewday.ui.jfx;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.util.converter.DoubleStringConverter;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.Quantity;

/**
 *
 */
public class QuantityEditWidget<T extends Quantity> extends HBox
{
	private final TextField textfield;
	private final Quantity.Unit unit;

	/*-------------------------------------------------------------------------*/
	public QuantityEditWidget(Quantity.Unit unit)
	{
		this.unit = unit;
		this.setAlignment(Pos.CENTER_LEFT);

		textfield = new TextField();
		textfield.setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0d));

		Label unitLabel = new Label(" "+StringUtils.getUiString("unit." + unit.name()));

		textfield.setAlignment(Pos.CENTER);
		unitLabel.setAlignment(Pos.CENTER_LEFT);

		textfield.setPrefWidth(75);

		this.getChildren().add(textfield);
		this.getChildren().add(unitLabel);
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Quantity quantity)
	{
		this.textfield.setText(""+quantity.get(unit));
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
