package mclachlan.brewday.ui.jfx;

import java.util.Map;
import javafx.scene.control.TableColumn;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.FermentableAddition;

/**
 *
 */
class FermentableAdditionDialog extends IngredientAdditionDialog<FermentableAddition, Fermentable>
{
	/*-------------------------------------------------------------------------*/
	public FermentableAdditionDialog(ProcessStep step)
	{
		super(JfxUi.grainsIcon, "common.add.fermentable", step);
	}

	/*-------------------------------------------------------------------------*/
	protected FermentableAddition createIngredientAddition(
		Fermentable selectedItem,
		WeightUnit additionAmount,
		TimeUnit additionTime)
	{
		return new FermentableAddition(selectedItem, additionAmount, additionTime);
	}

	/*-------------------------------------------------------------------------*/
	protected Map<String, Fermentable> getReferenceIngredients()
	{
		return Database.getInstance().getFermentables();
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<Fermentable, String>[] getColumns()
	{
		return new TableColumn[]
			{
				getPropertyValueTableColumn("fermentable.name", "name"),
				getPropertyValueTableColumn("fermentable.type", "type"),
				getPropertyValueTableColumn("fermentable.origin", "origin"),
				getPropertyValueTableColumn("fermentable.colour", "colour")
			};
	}
}