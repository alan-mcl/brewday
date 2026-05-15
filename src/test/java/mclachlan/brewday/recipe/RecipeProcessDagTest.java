package mclachlan.brewday.recipe;

import java.util.List;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.Heat;
import mclachlan.brewday.process.ProcessLog;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.db.Database;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecipeProcessDagTest
{
	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void buildProcessStepDagLinearChain()
	{
		TemperatureUnit t = new TemperatureUnit(65, Quantity.Unit.CELSIUS);
		TimeUnit r = new TimeUnit(1, Quantity.Unit.MINUTES);
		TimeUnit st = new TimeUnit(1, Quantity.Unit.MINUTES);
		Heat a = new Heat("A", "", null, "v1", t, r, st);
		Heat b = new Heat("B", "", "v1", "v2", t, r, st);
		Heat c = new Heat("C", "", "v2", "v3", t, r, st);
		Recipe rec = new Recipe("R");
		rec.getSteps().addAll(List.of(a, b, c));
		DirectedAcyclicGraph<ProcessStep, String> g = new DirectedAcyclicGraph<>(String.class);
		ProcessLog log = new ProcessLog();
		assertTrue(rec.buildProcessStepDag(g, log));
		assertEquals(2, g.edgeSet().size());
		assertTrue(log.getErrors().isEmpty());
	}

	@Test
	public void buildProcessStepDagDetectsCycle()
	{
		TemperatureUnit t = new TemperatureUnit(65, Quantity.Unit.CELSIUS);
		TimeUnit r = new TimeUnit(1, Quantity.Unit.MINUTES);
		TimeUnit st = new TimeUnit(1, Quantity.Unit.MINUTES);
		Heat s1 = new Heat("S1", "", "vin", "v1", t, r, st);
		Heat s2 = new Heat("S2", "", "v1", "v2", t, r, st);
		Heat s3 = new Heat("S3", "", "v2", "vin", t, r, st);
		Recipe rec = new Recipe("R");
		rec.getSteps().addAll(List.of(s1, s2, s3));
		DirectedAcyclicGraph<ProcessStep, String> g = new DirectedAcyclicGraph<>(String.class);
		ProcessLog log = new ProcessLog();
		assertFalse(rec.buildProcessStepDag(g, log));
		assertFalse(log.getErrors().isEmpty());
	}
}
