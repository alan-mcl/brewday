package mclachlan.brewday.ui.jfx;

import java.util.Map;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.YeastAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class YeastAdditionDialog extends IngredientAdditionDialog<YeastAddition, Yeast>
{
	private final YeastAddition addition;
	private QuantityEditWidget<WeightUnit> weight;
	private QuantityEditWidget<TimeUnit> time;

	public YeastAdditionDialog(ProcessStep step, YeastAddition addition)
	{
		super(JfxUi.yeastIcon, "common.add.yeast", step);
		this.addition = addition;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected IngredientAddition.Type getIngredientType()
	{
		return IngredientAddition.Type.YEAST;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void addUiStuffs(MigPane pane)
	{
		Settings settings = Database.getInstance().getSettings();

		IngredientAddition.Type ingType = IngredientAddition.Type.YEAST;
		Quantity.Unit weightUnit = settings.getUnitForStepAndIngredient(Quantity.Type.WEIGHT, getStep().getType(), ingType);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep().getType(), ingType);

		weight = new QuantityEditWidget<>(weightUnit);
		pane.add(new Label(StringUtils.getUiString("recipe.amount")));
		pane.add(weight, "wrap");

		time = new QuantityEditWidget<>(timeUnit);
		pane.add(new Label(StringUtils.getUiString("recipe.time")));
		pane.add(time, "wrap");

		if (addition != null)
		{
			weight.refresh(addition.getQuantity());
			time.refresh(addition.getTime());
		}
	}

	/*-------------------------------------------------------------------------*/
	protected YeastAddition createIngredientAddition(
		Yeast selectedItem)
	{
		return new YeastAddition(selectedItem, weight.getQuantity(), time.getQuantity());
	}

	/*-------------------------------------------------------------------------*/
	protected Map<String, Yeast> getReferenceIngredients()
	{
		return Database.getInstance().getYeasts();
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<Yeast, String>[] getColumns()
	{
		return new TableColumn[]
			{
				getPropertyValueTableColumn("yeast.name", "name"),
				getPropertyValueTableColumn("yeast.type", "type"),
				getPropertyValueTableColumn("yeast.attenuation", "attenuation"),
				getPropertyValueTableColumn("yeast.laboratory", "laboratory")
			};
	}
}