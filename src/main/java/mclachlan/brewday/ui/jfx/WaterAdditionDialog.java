package mclachlan.brewday.ui.jfx;

import java.util.Map;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.WaterAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class WaterAdditionDialog extends IngredientAdditionDialog<WaterAddition, Water>
{
	QuantityEditWidget<TemperatureUnit> temperature;

	public WaterAdditionDialog(ProcessStep step)
	{
		super(JfxUi.waterIcon, "common.add.water", step);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void addUiStuffs(MigPane pane)
	{
		temperature = new QuantityEditWidget<>(Quantity.Unit.CELSIUS);

		pane.add(new Label(StringUtils.getUiString("water.addition.temperature")));
		pane.add(temperature);
	}

	/*-------------------------------------------------------------------------*/
	protected WaterAddition createIngredientAddition(
		Water selectedItem,
		WeightUnit additionAmount,
		TimeUnit additionTime)
	{
		return new WaterAddition(
			selectedItem,
			new VolumeUnit(additionAmount.get(), Quantity.Unit.LITRES, false), // todo
			temperature.getQuantity(),
			additionTime);
	}

	/*-------------------------------------------------------------------------*/
	protected Map<String, Water> getReferenceIngredients()
	{
		return Database.getInstance().getWaters();
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<Water, String>[] getColumns()
	{
		return new TableColumn[]
			{
				getPropertyValueTableColumn("water.name", "name"),
				getPropertyValueTableColumn("water.ph", "ph"),
				getPropertyValueTableColumn("water.bicarbonate", "bicarbonate"),
				getPropertyValueTableColumn("water.sulfate", "sulfate")
			};
	}
}