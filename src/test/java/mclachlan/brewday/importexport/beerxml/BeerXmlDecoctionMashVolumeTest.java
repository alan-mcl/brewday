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
import mclachlan.brewday.process.Split;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BeerXmlDecoctionMashVolumeTest
{
	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void decoctionVolumeUsesMashVolumeNotTunCapacity() throws Exception
	{
		File fixture = new File("test_data/beerxml/test_keg.xml");
		Assume.assumeTrue("fixture missing: " + fixture.getPath(), fixture.isFile());

		Map<Class<?>, Map<String, V2DataObject>> result =
			new BeerXmlParser().parse(List.of(fixture), false, false, false);

		Recipe recipe = (Recipe)result.get(Recipe.class).values().iterator().next();

		Split decoctionSplit = recipe.getSteps().stream()
			.filter(Split.class::isInstance)
			.map(Split.class::cast)
			.findFirst()
			.orElseThrow();

		Mash mash = recipe.getSteps().stream()
			.filter(Mash.class::isInstance)
			.map(Mash.class::cast)
			.findFirst()
			.orElseThrow();

		List<FermentableAddition> grainBill = mash.getFermentableAdditions();

		WaterAddition strikeWater = mash.getWaterAdditions().get(0);
		assertNotNull(strikeWater);

		VolumeUnit mashVol = Equations.calcMashVolume(grainBill, strikeWater.getVolume(), 1D);
		TemperatureUnit startTemp = new TemperatureUnit(63.8888889, CELSIUS);
		TemperatureUnit targetTemp = new TemperatureUnit(68.8888889, CELSIUS);

		VolumeUnit expected = Equations.calcDecoctionVolume(mashVol, startTemp, targetTemp);
		VolumeUnit tunCapacityResult = Equations.calcDecoctionVolume(
			new VolumeUnit(25, LITRES),
			startTemp,
			targetTemp);

		assertEquals(expected.get(LITRES), decoctionSplit.getSplitVolume().get(LITRES), 0.0001);
		assertTrue(
			"decoction volume should be smaller than tun-capacity estimate",
			tunCapacityResult.get(LITRES) > expected.get(LITRES));
	}
}
