package mclachlan.brewday.ui;

import java.util.HashMap;
import mclachlan.brewday.Settings;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.OUNCES;
import static mclachlan.brewday.math.Quantity.Unit.POUNDS;
import static org.junit.Assert.assertEquals;

public class UiQuantityDisplayTest
{
	@Test
	public void getUnitForInventoryMatchesIngredientType()
	{
		Settings settings = new Settings(new HashMap<>());
		UiUnitPreferences.imperial().persist(settings);

		assertEquals(POUNDS, settings.getUnitForInventory(
			Quantity.Type.WEIGHT,
			IngredientAddition.Type.FERMENTABLES));
		assertEquals(OUNCES, settings.getUnitForInventory(
			Quantity.Type.WEIGHT,
			IngredientAddition.Type.HOPS));
	}
}
