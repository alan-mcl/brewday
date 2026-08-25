package mclachlan.brewday.recommend;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.YeastAddition;
import mclachlan.brewday.style.Style;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.math.Quantity.Unit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InventoryMatchCalculatorTest
{
	@Test
	public void missingBaseMaltHurtsMoreThanSpecialty()
	{
		Fermentable pale = new Fermentable("Pale Malt");
		Fermentable crystal = new Fermentable("Crystal 60");
		Recipe recipe = recipeWithBill(
			new FermentableAddition(pale, Quantity.parseQuantity("4000", GRAMS), GRAMS, time()),
			new FermentableAddition(crystal, Quantity.parseQuantity("500", GRAMS), GRAMS, time()));

		Map<String, InventoryLineItem> inventory = new HashMap<>();
		inventory.put(InventoryLineItem.getUniqueId("Pale Malt", IngredientAddition.Type.FERMENTABLES),
			new InventoryLineItem("Pale Malt", IngredientAddition.Type.FERMENTABLES,
				Quantity.parseQuantity("4000", GRAMS), GRAMS));
		inventory.put(InventoryLineItem.getUniqueId("Crystal 60", IngredientAddition.Type.FERMENTABLES),
			new InventoryLineItem("Crystal 60", IngredientAddition.Type.FERMENTABLES,
				Quantity.parseQuantity("0", GRAMS), GRAMS));

		InventoryMatch missingCrystal = InventoryMatchCalculator.calculate(recipe, inventory);
		assertTrue(missingCrystal.hasCriticalMiss() == false);

		inventory.put(InventoryLineItem.getUniqueId("Pale Malt", IngredientAddition.Type.FERMENTABLES),
			new InventoryLineItem("Pale Malt", IngredientAddition.Type.FERMENTABLES,
				Quantity.parseQuantity("0", GRAMS), GRAMS));
		inventory.put(InventoryLineItem.getUniqueId("Crystal 60", IngredientAddition.Type.FERMENTABLES),
			new InventoryLineItem("Crystal 60", IngredientAddition.Type.FERMENTABLES,
				Quantity.parseQuantity("500", GRAMS), GRAMS));

		InventoryMatch missingBase = InventoryMatchCalculator.calculate(recipe, inventory);
		assertTrue(missingBase.hasCriticalMiss());
		assertTrue(missingCrystal.getMatchPercent() > missingBase.getMatchPercent());
	}

	@Test
	public void missingYeastIsCriticalAndCapsMatch()
	{
		Fermentable pale = new Fermentable("Pale Malt");
		Yeast yeast = new Yeast("US-05");
		Recipe recipe = recipeWithBill(
			new FermentableAddition(pale, Quantity.parseQuantity("4000", GRAMS), GRAMS, time()),
			new YeastAddition(yeast, Quantity.parseQuantity("11", GRAMS), GRAMS, time()));

		Map<String, InventoryLineItem> inventory = new HashMap<>();
		inventory.put(InventoryLineItem.getUniqueId("Pale Malt", IngredientAddition.Type.FERMENTABLES),
			new InventoryLineItem("Pale Malt", IngredientAddition.Type.FERMENTABLES,
				Quantity.parseQuantity("4000", GRAMS), GRAMS));

		InventoryMatch match = InventoryMatchCalculator.calculate(recipe, inventory);
		assertTrue(match.hasCriticalMiss());
		assertTrue(match.getMatchPercent() <= 75);
		assertFalse(match.isFullyBrewable());
	}

	@Test
	public void fullCoverageIsOneHundred()
	{
		Fermentable pale = new Fermentable("Pale Malt");
		Hop hop = new Hop("Cascade");
		Yeast yeast = new Yeast("US-05");
		Recipe recipe = recipeWithBill(
			new FermentableAddition(pale, Quantity.parseQuantity("4000", GRAMS), GRAMS, time()),
			new HopAddition(hop, Quantity.parseQuantity("50", GRAMS), GRAMS, time()),
			new YeastAddition(yeast, Quantity.parseQuantity("11", GRAMS), GRAMS, time()));

		Map<String, InventoryLineItem> inventory = new HashMap<>();
		inventory.put(InventoryLineItem.getUniqueId("Pale Malt", IngredientAddition.Type.FERMENTABLES),
			new InventoryLineItem("Pale Malt", IngredientAddition.Type.FERMENTABLES,
				Quantity.parseQuantity("4000", GRAMS), GRAMS));
		inventory.put(InventoryLineItem.getUniqueId("Cascade", IngredientAddition.Type.HOPS),
			new InventoryLineItem("Cascade", IngredientAddition.Type.HOPS,
				Quantity.parseQuantity("50", GRAMS), GRAMS));
		inventory.put(InventoryLineItem.getUniqueId("US-05", IngredientAddition.Type.YEAST),
			new InventoryLineItem("US-05", IngredientAddition.Type.YEAST,
				Quantity.parseQuantity("11", GRAMS), GRAMS));

		InventoryMatch match = InventoryMatchCalculator.calculate(recipe, inventory);
		assertTrue(match.isFullyBrewable());
		assertEquals(100, match.getMatchPercent());
	}

	private static Recipe recipeWithBill(IngredientAddition... additions)
	{
		Recipe recipe = new Recipe("Test Recipe");
		PackageStep step = new PackageStep();
		step.setStyleId("21A/American IPA/BJCP 2021");
		for (IngredientAddition ia : additions)
		{
			step.getIngredientAdditions().add(ia);
		}
		recipe.getSteps().add(step);
		return recipe;
	}

	private static TimeUnit time()
	{
		return new TimeUnit(0, SECONDS, false);
	}
}
