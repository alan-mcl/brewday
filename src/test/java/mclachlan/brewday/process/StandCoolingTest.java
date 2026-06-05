package mclachlan.brewday.process;

import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.GU;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.MINUTES;
import static mclachlan.brewday.math.Quantity.Unit.SRM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StandCoolingTest
{
	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void beerInputProducesBeerOutput()
	{
		Volume beerIn = new Volume("beer_in", Volume.Type.BEER);
		beerIn.setVolume(new VolumeUnit(20, LITRES));
		beerIn.setGravity(new DensityUnit(10, GU, false));
		beerIn.setFermentability(new PercentageUnit(0.75));
		beerIn.setTemperature(new TemperatureUnit(18, CELSIUS));
		beerIn.setColour(new ColourUnit(8, SRM, false));
		beerIn.setAbv(new PercentageUnit(5));

		Stand stand = new Stand(
			"cellar",
			"",
			"beer_in",
			"beer_out",
			new TimeUnit(60, MINUTES, false),
			java.util.List.of());
		stand.setCoolingCoefficient(0.035);

		Volumes volumes = new Volumes();
		volumes.addVolume("beer_in", beerIn);

		EquipmentProfile equipment = new EquipmentProfile("test");
		equipment.setAmbientTemperature(new TemperatureUnit(12, CELSIUS));

		ProcessLog log = new ProcessLog();
		stand.apply(volumes, equipment, log);

		assertEquals(0, log.getErrors().size());
		Volume beerOut = volumes.getVolume("beer_out");
		assertEquals(Volume.Type.BEER, beerOut.getType());
		assertTrue(beerOut.getTemperature().get(CELSIUS) < beerIn.getTemperature().get(CELSIUS));
	}
}
