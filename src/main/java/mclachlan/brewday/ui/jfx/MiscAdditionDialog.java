package mclachlan.brewday.ui.jfx;

import java.util.Map;
import javafx.scene.control.TableColumn;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.MiscAddition;

/**
 *
 */
class MiscAdditionDialog extends IngredientAdditionDialog<MiscAddition, Misc>
{
	public MiscAdditionDialog(ProcessStep step)
	{
		super(JfxUi.miscIcon, "common.add.misc", step);
	}

	/*-------------------------------------------------------------------------*/
	protected MiscAddition createIngredientAddition(
		Misc selectedItem,
		WeightUnit additionAmount,
		TimeUnit additionTime)
	{
		return new MiscAddition(selectedItem, additionAmount, additionTime);
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