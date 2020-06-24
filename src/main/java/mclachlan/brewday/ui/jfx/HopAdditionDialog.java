package mclachlan.brewday.ui.jfx;

import java.util.Map;
import javafx.scene.control.TableColumn;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.HopAddition;

/**
 *
 */
class HopAdditionDialog extends IngredientAdditionDialog<HopAddition, Hop>
{
	public HopAdditionDialog(ProcessStep step)
	{
		super(JfxUi.hopsIcon, "common.add.hop", step);
	}

	/*-------------------------------------------------------------------------*/
	protected HopAddition createIngredientAddition(
		Hop selectedItem,
		WeightUnit additionAmount,
		TimeUnit additionTime)
	{
		return new HopAddition(selectedItem, additionAmount, additionTime);
	}

	/*-------------------------------------------------------------------------*/
	protected Map<String, Hop> getReferenceIngredients()
	{
		return Database.getInstance().getHops();
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<Hop, String>[] getColumns()
	{
		return new TableColumn[]
			{
				getPropertyValueTableColumn("hop.name", "name"),
				getPropertyValueTableColumn("hop.type", "type"),
				getPropertyValueTableColumn("hop.origin", "origin"),
				getPropertyValueTableColumn("hop.aa", "alphaAcid")
			};
	}
}