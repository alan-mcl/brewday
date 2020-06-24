package mclachlan.brewday.ui.jfx;

import java.util.Map;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class MiscAdditionDialog extends IngredientAdditionDialog<MiscAddition, Misc>
{
	private QuantityEditWidget<WeightUnit> weight;
	private QuantityEditWidget<TimeUnit> time;

	/*-------------------------------------------------------------------------*/
	public MiscAdditionDialog(ProcessStep step)
	{
		super(JfxUi.miscIcon, "common.add.misc", step);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected IngredientAddition.Type getIngredientType()
	{
		return IngredientAddition.Type.MISC;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void addUiStuffs(MigPane pane)
	{
		Settings settings = Database.getInstance().getSettings();

		IngredientAddition.Type ingType = IngredientAddition.Type.MISC;
		Quantity.Unit weightUnit = settings.getUnitForStepAndIngredient(Quantity.Type.WEIGHT, getStep().getType(), ingType);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep().getType(), ingType);

		weight = new QuantityEditWidget<>(weightUnit);
		pane.add(new Label(StringUtils.getUiString("recipe.amount")));
		pane.add(weight, "wrap");

		time = new QuantityEditWidget<>(timeUnit);
		pane.add(new Label(StringUtils.getUiString("recipe.amount")));
		pane.add(time, "wrap");
	}

	/*-------------------------------------------------------------------------*/
	protected MiscAddition createIngredientAddition(
		Misc selectedItem)
	{
		return new MiscAddition(selectedItem, weight.getQuantity(), time.getQuantity());
	}

	/*-------------------------------------------------------------------------*/
	protected Map<String, Misc> getReferenceIngredients()
	{
		return Database.getInstance().getMiscs();
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<Misc, String>[] getColumns()
	{
		return new TableColumn[]
			{
				getPropertyValueTableColumn("misc.name", "name"),
				getPropertyValueTableColumn("misc.type", "type"),
				getPropertyValueTableColumn("misc.use", "use"),
				getPropertyValueTableColumn("misc.usage.recommendation", "usageRecommendation")
			};
	}
}