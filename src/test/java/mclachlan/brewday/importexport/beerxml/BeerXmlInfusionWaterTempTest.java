package mclachlan.brewday.importexport.beerxml;

import java.io.File;
import java.util.List;
import java.util.Map;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.MashInfusion;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class BeerXmlInfusionWaterTempTest
{
	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void firstMashWaterTempHitsStepTarget() throws Exception
	{
		File fixture = new File("test_data/beerxml/test_allgrain.xml");
		Assume.assumeTrue("fixture missing: " + fixture.getPath(), fixture.isFile());

		Map<Class<?>, Map<String, V2DataObject>> result =
			new BeerXmlParser().parse(List.of(fixture), false, false, false);

		Recipe recipe = (Recipe)result.get(Recipe.class).values().iterator().next();
		Mash mash = recipe.getSteps().stream()
			.filter(Mash.class::isInstance)
			.map(Mash.class::cast)
			.findFirst()
			.orElseThrow();

		WaterAddition strikeWater = mash.getWaterAdditions().get(0);
		assertNotNull(strikeWater);

		TemperatureUnit expected = Equations.calcWaterTemp(
			Equations.calcTotalGrainWeight(mash.getFermentableAdditions()),
			strikeWater,
			mash.getGrainTemp(),
			new TemperatureUnit(66.6666667, CELSIUS));

		assertEquals(expected.get(CELSIUS), strikeWater.getTemperature().get(CELSIUS), 0.05);
		assertNotEquals(
			66.6666667,
			strikeWater.getTemperature().get(CELSIUS),
			0.05);
	}

	@Test
	public void laterInfusionWaterTempHitsStepTarget() throws Exception
	{
		File fixture = new File("test_data/beerxml/test_two_step_infusion.xml");
		Assume.assumeTrue("fixture missing: " + fixture.getPath(), fixture.isFile());

		Map<Class<?>, Map<String, V2DataObject>> result =
			new BeerXmlParser().parse(List.of(fixture), false, false, false);

		Recipe recipe = (Recipe)result.get(Recipe.class).values().iterator().next();
		Mash mash = recipe.getSteps().stream()
			.filter(Mash.class::isInstance)
			.map(Mash.class::cast)
			.findFirst()
			.orElseThrow();

		MashInfusion mashInfusion = recipe.getSteps().stream()
			.filter(MashInfusion.class::isInstance)
			.map(MashInfusion.class::cast)
			.findFirst()
			.orElseThrow();

		WaterAddition strikeWater = mash.getWaterAdditions().get(0);
		List<FermentableAddition> grainBill = mash.getFermentableAdditions();
		VolumeUnit mashVolume = Equations.calcMashVolume(
			grainBill,
			strikeWater.getVolume(),
			1D);

		WaterAddition infusionWater = mashInfusion.getWaterAdditions().get(0);
		TemperatureUnit expected = Equations.calcAdditionTemperature(
			mashVolume,
			new TemperatureUnit(50, CELSIUS),
			new VolumeUnit(5, LITRES),
			new TemperatureUnit(65, CELSIUS));

		assertEquals(expected.get(CELSIUS), infusionWater.getTemperature().get(CELSIUS), 0.0001);
		assertNotEquals(65D, infusionWater.getTemperature().get(CELSIUS), 0.05);
	}
}
