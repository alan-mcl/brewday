package mclachlan.brewday.ui.jfx;

import java.util.Map;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.WaterAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class WaterAdditionDialog extends IngredientAdditionDialog<WaterAddition, Water>
{
	private QuantityEditWidget<TemperatureUnit> temperature;
	private QuantityEditWidget<VolumeUnit> volume;
	private QuantityEditWidget<TimeUnit> time;

	public WaterAdditionDialog(ProcessStep step)
	{
		super(JfxUi.waterIcon, "common.add.water", step);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected IngredientAddition.Type getIngredientType()
	{
		return IngredientAddition.Type.WATER;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void addUiStuffs(MigPane pane)
	{
		Settings settings = Database.getInstance().getSettings();

		Quantity.Unit tempUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TEMPERATURE, getStep().getType(), IngredientAddition.Type.WATER);
		Quantity.Unit volUnit = settings.getUnitForStepAndIngredient(Quantity.Type.VOLUME, getStep().getType(), IngredientAddition.Type.WATER);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep().getType(), IngredientAddition.Type.WATER);

		volume = new QuantityEditWidget<>(volUnit);
		pane.add(new Label(StringUtils.getUiString("recipe.amount")));
		pane.add(volume, "wrap");

		time = new QuantityEditWidget<>(timeUnit);
		pane.add(new Label(StringUtils.getUiString("recipe.amount")));
		pane.add(time, "wrap");

		temperature = new QuantityEditWidget<>(tempUnit);
		pane.add(new Label(StringUtils.getUiString("water.addition.temperature")));
		pane.add(temperature, "wrap");
	}

	/*-------------------------------------------------------------------------*/
	protected WaterAddition createIngredientAddition(
		Water selectedItem)
	{
		return new WaterAddition(
			selectedItem,
			volume.getQuantity(),
			temperature.getQuantity(),
			time.getQuantity());
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