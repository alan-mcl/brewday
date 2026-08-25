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
import mclachlan.brewday.math.BitternessUnit;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.Volumes;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.YeastAddition;
import mclachlan.brewday.style.Style;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.math.Quantity.Unit.IBU;
import static mclachlan.brewday.math.Quantity.Unit.SECONDS;
import static mclachlan.brewday.math.Quantity.Unit.SRM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BrewRecommendationEngineTest
{
	private static final String IPA_STYLE = "21A/American IPA/BJCP 2021";
	private static final String STOUT_STYLE = "20A/American Porter/BJCP 2021";

	@Test
	public void omitsHistoryGroupsWhenNoBatches()
	{
		Map<String, Recipe> recipes = new HashMap<>();
		recipes.put("Pale Ale", buildRecipe("Pale Ale", IPA_STYLE, "Pale Malt", "Cascade", "US-05"));
		RecommendationContext context = RecommendationContext.forTest(
			recipes, Map.of(), fullInventory("Pale Malt", "Cascade", "US-05"),
			styles(), LocalDate.of(2026, 8, 1), Set.of());

		RecommendationResult result = new BrewRecommendationEngine().recommend(context);

		assertNull(findGroup(result, RecommendationGroupKind.DUE_FOR_REPEAT));
		assertNull(findGroup(result, RecommendationGroupKind.FORGOTTEN_RECIPES));
		assertNull(findGroup(result, RecommendationGroupKind.SOMETHING_DIFFERENT));
		assertNotNull(findGroup(result, RecommendationGroupKind.BEST_INVENTORY_MATCHES));
	}

	@Test
	public void dueForRepeatRequiresMultipleBrewsAndGap()
	{
		Map<String, Recipe> recipes = new HashMap<>();
		recipes.put("House Pale", buildRecipe("House Pale", IPA_STYLE, "Pale Malt", "Cascade", "US-05"));
		recipes.put("House Porter", buildRecipe("House Porter", STOUT_STYLE, "Pale Malt", "Cascade", "US-05"));
		recipes.put("One Off", buildRecipe("One Off", STOUT_STYLE, "Pale Malt", "Cascade", "US-05"));
		recipes.put("Recent Other", buildRecipe("Recent Other", IPA_STYLE, "Pale Malt", "Cascade", "US-05"));

		Map<String, Batch> batches = new HashMap<>();
		batches.put("b1", new Batch("b1", "", "House Pale", LocalDate.of(2024, 1, 1), new Volumes(), false));
		batches.put("b2", new Batch("b2", "", "House Pale", LocalDate.of(2024, 6, 1), new Volumes(), false));
		batches.put("b3", new Batch("b3", "", "House Porter", LocalDate.of(2024, 3, 1), new Volumes(), false));
		batches.put("b4", new Batch("b4", "", "House Porter", LocalDate.of(2024, 8, 1), new Volumes(), false));
		batches.put("b5", new Batch("b5", "", "One Off", LocalDate.of(2024, 2, 1), new Volumes(), false));
		batches.put("b6", new Batch("b6", "", "Recent Other", LocalDate.of(2025, 6, 1), new Volumes(), false));
		batches.put("b7", new Batch("b7", "", "Recent Other", LocalDate.of(2025, 7, 1), new Volumes(), false));

		RecommendationContext context = RecommendationContext.forTest(
			recipes, batches, fullInventory("Pale Malt", "Cascade", "US-05"),
			styles(), LocalDate.of(2026, 8, 1), Set.of());

		RecommendationGroup group = findGroup(new BrewRecommendationEngine().recommend(context),
			RecommendationGroupKind.DUE_FOR_REPEAT);
		assertNotNull(group);
		assertTrue(group.getRecommendations().stream()
			.anyMatch(r -> "House Pale".equals(r.getRecipeName())));
		assertFalse(group.getRecommendations().stream()
			.anyMatch(r -> "One Off".equals(r.getRecipeName())));
	}

	@Test
	public void oneSmallPurchaseNamesMissingIngredient()
	{
		Map<String, Recipe> recipes = new HashMap<>();
		recipes.put("Almost There", buildRecipe("Almost There", IPA_STYLE, "Pale Malt", "Cascade", "US-05"));

		Map<String, InventoryLineItem> inventory = fullInventory("Pale Malt", "US-05");
		inventory.put(InventoryLineItem.getUniqueId("Cascade", IngredientAddition.Type.HOPS),
			new InventoryLineItem("Cascade", IngredientAddition.Type.HOPS,
				Quantity.parseQuantity("40", GRAMS), GRAMS));
		RecommendationContext context = RecommendationContext.forTest(
			recipes, Map.of(), inventory, styles(), LocalDate.of(2026, 8, 1), Set.of());

		RecommendationGroup group = findGroup(new BrewRecommendationEngine().recommend(context),
			RecommendationGroupKind.ONE_SMALL_PURCHASE);
		assertNotNull(group);
		Recommendation rec = group.getRecommendations().get(0);
		assertTrue(rec.getExplanation().contains("Cascade")
			|| rec.getDetailLines().stream().anyMatch(l -> l.contains("Cascade")));
	}

	@Test
	public void useItUpDoesNotRecommendVeryLowMatch()
	{
		Fermentable pale = new Fermentable("Pale Malt");
		Fermentable special = new Fermentable("Special X");
		Yeast yeast = new Yeast("US-05");
		Recipe recipe = new Recipe("Dump Special");
		addBill(recipe, STOUT_STYLE,
			new FermentableAddition(pale, Quantity.parseQuantity("100", GRAMS), GRAMS, time()),
			new FermentableAddition(special, Quantity.parseQuantity("4000", GRAMS), GRAMS, time()),
			new YeastAddition(yeast, Quantity.parseQuantity("11", GRAMS), GRAMS, time()));

		Map<String, InventoryLineItem> inventory = new HashMap<>();
		inventory.put(InventoryLineItem.getUniqueId("Special X", IngredientAddition.Type.FERMENTABLES),
			new InventoryLineItem("Special X", IngredientAddition.Type.FERMENTABLES,
				Quantity.parseQuantity("8000", GRAMS), GRAMS));

		RecommendationContext context = RecommendationContext.forTest(
			Map.of("Dump Special", recipe),
			Map.of(),
			inventory,
			styles(),
			LocalDate.of(2026, 8, 1),
			Set.of());

		RecommendationGroup group = findGroup(new BrewRecommendationEngine().recommend(context),
			RecommendationGroupKind.USE_IT_UP);
		assertNull(group);
	}

	@Test
	public void groupsPreferStyleDiversity()
	{
		Map<String, Recipe> recipes = new HashMap<>();
		for (int i = 1; i <= 4; i++)
		{
			recipes.put("IPA " + i, buildRecipe("IPA " + i, IPA_STYLE, "Pale Malt", "Cascade", "US-05"));
		}
		recipes.put("Porter", buildRecipe("Porter", STOUT_STYLE, "Pale Malt", "Cascade", "US-05"));

		RecommendationContext context = RecommendationContext.forTest(
			recipes, Map.of(), fullInventory("Pale Malt", "Cascade", "US-05"),
			styles(), LocalDate.of(2026, 8, 1), Set.of());

		RecommendationGroup group = findGroup(new BrewRecommendationEngine().recommend(context),
			RecommendationGroupKind.BEST_INVENTORY_MATCHES);
		assertNotNull(group);
		long distinctStyles = group.getRecommendations().stream()
			.map(Recommendation::getStyleDisplay)
			.distinct()
			.count();
		assertTrue(distinctStyles >= 2);
	}

	private static RecommendationGroup findGroup(RecommendationResult result, RecommendationGroupKind kind)
	{
		for (RecommendationGroup group : result.getGroups())
		{
			if (group.getKind() == kind)
			{
				return group;
			}
		}
		return null;
	}

	private static Recipe buildRecipe(
		String name,
		String styleId,
		String malt,
		String hop,
		String yeast)
	{
		Recipe recipe = new Recipe(name);
		addBill(recipe, styleId,
			new FermentableAddition(new Fermentable(malt), Quantity.parseQuantity("4000", GRAMS), GRAMS, time()),
			new HopAddition(new Hop(hop), Quantity.parseQuantity("50", GRAMS), GRAMS, time()),
			new YeastAddition(new Yeast(yeast), Quantity.parseQuantity("11", GRAMS), GRAMS, time()));
		return recipe;
	}

	private static void addBill(Recipe recipe, String styleId, IngredientAddition... additions)
	{
		PackageStep step = new PackageStep();
		step.setStyleId(styleId);
		for (IngredientAddition ia : additions)
		{
			step.getIngredientAdditions().add(ia);
		}
		recipe.getSteps().add(step);
	}

	private static Map<String, InventoryLineItem> fullInventory(String... names)
	{
		Map<String, InventoryLineItem> inventory = new HashMap<>();
		for (String malt : names)
		{
			IngredientAddition.Type type = IngredientAddition.Type.FERMENTABLES;
			if ("Cascade".equals(malt))
			{
				type = IngredientAddition.Type.HOPS;
			}
			else if (malt.startsWith("US-"))
			{
				type = IngredientAddition.Type.YEAST;
			}
			double qty = type == IngredientAddition.Type.YEAST ? 11D : (type == IngredientAddition.Type.HOPS ? 50D : 4000D);
			inventory.put(InventoryLineItem.getUniqueId(malt, type),
				new InventoryLineItem(malt, type, Quantity.parseQuantity("" + qty, GRAMS), GRAMS));
		}
		return inventory;
	}

	private static Map<String, Style> styles()
	{
		Map<String, Style> styles = new HashMap<>();
		styles.put(IPA_STYLE, style(IPA_STYLE, "21", "American IPA", 40D, 6D, 0.055D));
		styles.put(STOUT_STYLE, style(STOUT_STYLE, "20", "American Porter", 35D, 30D, 0.055D));
		return styles;
	}

	private static Style style(
		String name,
		String categoryNumber,
		String guideName,
		double ibu,
		double srm,
		double abv)
	{
		Style style = new Style(name);
		style.setDisplayName(guideName);
		style.setStyleGuideName(guideName);
		style.setCategoryNumber(categoryNumber);
		style.setStyleLetter("A");
		style.setIbuMin(new BitternessUnit(ibu - 5, IBU));
		style.setIbuMax(new BitternessUnit(ibu + 5, IBU));
		style.setColourMin(new ColourUnit(srm - 2, SRM));
		style.setColourMax(new ColourUnit(srm + 2, SRM));
		style.setAbvMin(new PercentageUnit(abv - 0.01));
		style.setAbvMax(new PercentageUnit(abv + 0.01));
		style.setType(Style.Type.ALE);
		return style;
	}

	private static TimeUnit time()
	{
		return new TimeUnit(0, SECONDS, false);
	}
}
