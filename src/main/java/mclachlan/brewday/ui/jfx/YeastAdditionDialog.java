package mclachlan.brewday.ui.jfx;

import java.util.Map;
import javafx.scene.control.TableColumn;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.YeastAddition;

/**
 *
 */
class YeastAdditionDialog extends IngredientAdditionDialog<YeastAddition, Yeast>
{
	public YeastAdditionDialog(ProcessStep step)
	{
		super(JfxUi.yeastIcon, "common.add.yeast", step);
	}

	/*-------------------------------------------------------------------------*/
	protected YeastAddition createIngredientAddition(
		Yeast selectedItem,
		WeightUnit additionAmount,
		TimeUnit additionTime)
	{
		return new YeastAddition(selectedItem, additionAmount, additionTime);
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