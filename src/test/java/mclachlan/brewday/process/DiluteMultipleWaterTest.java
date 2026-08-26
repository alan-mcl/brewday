package mclachlan.brewday.process;

import java.util.List;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.recipe.WaterAddition;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.GU;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.MINUTES;
import static mclachlan.brewday.math.Quantity.Unit.SRM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DiluteMultipleWaterTest
{
	private static final String WORT_IN = "wort_in";
	private static final String WORT_OUT = "wort_out";

	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void multipleWaterAdditionsBlendIntoOutputVolume()
	{
		Water water = new Water("test water");

		Volume wortIn = new Volume(WORT_IN, Volume.Type.WORT);
		wortIn.setVolume(new VolumeUnit(20, LITRES));
		wortIn.setGravity(new DensityUnit(12, GU, false));
		wortIn.setTemperature(new TemperatureUnit(80, CELSIUS));
		wortIn.setColour(new ColourUnit(10, SRM, false));

		WaterAddition water1 = new WaterAddition(
			water,
			new VolumeUnit(2, LITRES),
			LITRES,
			new TemperatureUnit(20, CELSIUS, false),
			new TimeUnit(0, MINUTES, false));
		WaterAddition water2 = new WaterAddition(
			water,
			new VolumeUnit(3, LITRES),
			LITRES,
			new TemperatureUnit(10, CELSIUS, false),
			new TimeUnit(0, MINUTES, false));

		Dilute dilute = new Dilute(
			"dilute",
			"",
			WORT_IN,
			WORT_OUT,
			List.of(water1, water2));

		Volumes volumes = new Volumes();
		volumes.addVolume(WORT_IN, wortIn);

		ProcessLog log = new ProcessLog();
		dilute.apply(volumes, new EquipmentProfile("test"), log);

		assertEquals(0, log.getErrors().size());

		Volume wortOut = volumes.getVolume(WORT_OUT);
		assertEquals(25, wortOut.getVolume().get(LITRES), 0.01);
		assertEquals(9.6, wortOut.getGravity().get(GU), 0.01);
		assertEquals(8, wortOut.getColour().get(SRM), 0.01);

		TemperatureUnit afterFirst = Equations.calcCombinedTemperature(
			new VolumeUnit(20, LITRES),
			new TemperatureUnit(80, CELSIUS),
			new VolumeUnit(2, LITRES),
			new TemperatureUnit(20, CELSIUS));
		TemperatureUnit expectedTemp = Equations.calcCombinedTemperature(
			new VolumeUnit(22, LITRES),
			afterFirst,
			new VolumeUnit(3, LITRES),
			new TemperatureUnit(10, CELSIUS));
		assertEquals(expectedTemp.get(CELSIUS), wortOut.getTemperature().get(CELSIUS), 0.01);
	}

	@Test
	public void noWaterAdditionLogsError()
	{
		Volume wortIn = new Volume(WORT_IN, Volume.Type.WORT);
		wortIn.setVolume(new VolumeUnit(20, LITRES));
		wortIn.setGravity(new DensityUnit(12, GU, false));
		wortIn.setTemperature(new TemperatureUnit(80, CELSIUS));

		Dilute dilute = new Dilute(
			"dilute",
			"",
			WORT_IN,
			WORT_OUT,
			List.of());

		Volumes volumes = new Volumes();
		volumes.addVolume(WORT_IN, wortIn);

		ProcessLog log = new ProcessLog();
		dilute.apply(volumes, new EquipmentProfile("test"), log);

		assertFalse(log.getErrors().isEmpty());
	}
}
