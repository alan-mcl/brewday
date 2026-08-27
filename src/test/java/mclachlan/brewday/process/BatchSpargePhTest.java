package mclachlan.brewday.process;

import java.util.ArrayList;
import java.util.List;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.WaterAddition;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.MINUTES;
import static mclachlan.brewday.math.Quantity.Unit.PH;
import static mclachlan.brewday.math.Quantity.Unit.SPECIFIC_GRAVITY;
import static mclachlan.brewday.math.Quantity.Unit.SRM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BatchSpargePhTest
{
	private static final String MASH_IN = "mash_in";
	private static final String COMBINED_WORT = "combined_wort";
	private static final String SPARGE_RUNNINGS = "sparge_runnings";
	private static final String SPARGE_MASH = "sparge_mash";

	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void lauteredMashPhBlendsWithSpargeWater()
	{
		PhUnit mashPh = new PhUnit(5.4);
		PhUnit spargePh = new PhUnit(5.8);
		VolumeUnit mashVol = new VolumeUnit(10, LITRES);
		VolumeUnit spargeVol = new VolumeUnit(10, LITRES);

		Volumes volumes = applySparge(mashPh, spargePh, mashVol, spargeVol);

		PhUnit expected = Equations.calcCombinedPh(mashVol, mashPh, spargeVol, spargePh);
		PhUnit lauteredPh = PhVolumes.getPrimary(volumes.getVolume(SPARGE_MASH));
		PhUnit runningsPh = PhVolumes.getPrimary(volumes.getVolume(SPARGE_RUNNINGS));

		assertEquals(expected.get(PH), lauteredPh.get(PH), 0.001);
		assertEquals(expected.get(PH), runningsPh.get(PH), 0.001);
	}

	@Test
	public void lauteredMashPhCopiedWhenSpargeWaterHasNoPh()
	{
		PhUnit mashPh = new PhUnit(5.4);

		Volumes volumes = applySparge(
			mashPh,
			null,
			new VolumeUnit(10, LITRES),
			new VolumeUnit(10, LITRES));

		PhUnit lauteredPh = PhVolumes.getPrimary(volumes.getVolume(SPARGE_MASH));
		assertEquals(mashPh.get(PH), lauteredPh.get(PH), 0.001);
	}

	/*-------------------------------------------------------------------------*/
	private static Volumes applySparge(
		PhUnit mashPh,
		PhUnit spargeWaterPh,
		VolumeUnit mashVol,
		VolumeUnit spargeVol)
	{
		Water mashWater = new Water("mash water");
		mashWater.setPh(new PhUnit(7.0, false));
		WaterAddition mashWaterAddition = new WaterAddition(
			mashWater,
			mashVol,
			LITRES,
			new TemperatureUnit(65, CELSIUS, false),
			new TimeUnit(60, MINUTES, false));

		Volume mash = new Volume(
			MASH_IN,
			Volume.Type.MASH,
			mashVol,
			new ArrayList<>(),
			mashWaterAddition,
			new TemperatureUnit(65, CELSIUS, false),
			new DensityUnit(1.080, SPECIFIC_GRAVITY),
			new ColourUnit(8, SRM),
			mashPh);

		Water spargeWater = new Water("sparge water");
		if (spargeWaterPh != null)
		{
			spargeWater.setPh(spargeWaterPh);
		}
		WaterAddition spargeAddition = new WaterAddition(
			spargeWater,
			spargeVol,
			LITRES,
			new TemperatureUnit(76, CELSIUS, false),
			new TimeUnit(0, MINUTES, false));

		List<IngredientAddition> ingredients = new ArrayList<>();
		ingredients.add(spargeAddition);

		BatchSparge step = new BatchSparge(
			"batch sparge",
			"",
			MASH_IN,
			null,
			COMBINED_WORT,
			SPARGE_RUNNINGS,
			SPARGE_MASH,
			ingredients);

		Volumes volumes = new Volumes();
		volumes.addVolume(MASH_IN, mash);

		ProcessLog log = new ProcessLog();
		step.apply(volumes, new EquipmentProfile(), log);
		assertTrue(log.getErrors().isEmpty());

		return volumes;
	}
}
