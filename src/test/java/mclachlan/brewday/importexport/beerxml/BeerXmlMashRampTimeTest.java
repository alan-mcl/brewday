package mclachlan.brewday.importexport.beerxml;

import java.io.File;
import java.util.List;
import java.util.Map;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.recipe.Recipe;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BeerXmlMashRampTimeTest
{
	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void firstMashStepImportsRampTimeSeparatelyFromDuration() throws Exception
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

		assertEquals(60D, mash.getDuration().get(MINUTES), 0.0001);
		assertNotNull(mash.getRampTime());
		assertEquals(2D, mash.getRampTime().get(MINUTES), 0.0001);
	}
}
