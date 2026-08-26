package mclachlan.brewday.process;

import java.util.ArrayList;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.MINUTES;
import static mclachlan.brewday.math.Quantity.Unit.SRM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PackageStepHopInventoryTest
{
	private static final String BEER_IN = "beer_in";
	private static final String BEER_OUT = "beer_out";

	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void pelletHopAdditionFoldsAlphaAcidsIntoPackagedVolume()
	{
		Hop hop = new Hop("test hop");
		hop.setAlphaAcid(new PercentageUnit(0.10));
		hop.setForm(Hop.Form.PELLET_T90);

		HopAddition hopAddition = new HopAddition(
			hop,
			new WeightUnit(50, GRAMS),
			GRAMS,
			new TimeUnit(3, MINUTES, false));

		Volume beerIn = newVolume(BEER_IN);
		WeightUnit expectedAlpha = Equations.calcHopAlphaAcidsMg(hopAddition);

		PackageStep packageStep = packageStepWithHop(hopAddition);
		packageStep.setCarbonationMethod(PackageStep.CarbonationMethod.FORCE_CARB);

		Volumes volumes = new Volumes();
		volumes.addVolume(BEER_IN, beerIn);

		ProcessLog log = new ProcessLog();
		packageStep.apply(volumes, new EquipmentProfile("test"), log);

		assertEquals(0, log.getErrors().size());

		Volume packaged = volumes.getVolume(BEER_OUT);
		WeightUnit alphaOut = HopAcidVolumes.get(packaged, Volume.Metric.ALPHA_ACIDS_MG);
		assertEquals(expectedAlpha.get(Quantity.Unit.MILLIGRAMS), alphaOut.get(Quantity.Unit.MILLIGRAMS), 0.01);
		assertNull(HopAcidVolumes.get(beerIn, Volume.Metric.ISO_ALPHA_ACIDS_MG));

		Volume inputStill = volumes.getVolume(BEER_IN);
		assertHopAcidZeroOrAbsent(inputStill, Volume.Metric.ALPHA_ACIDS_MG);
	}

	@Test
	public void isomerizedExtractHopFoldsIsoAlphaIntoPackagedVolume()
	{
		Hop hop = new Hop("iso extract");
		hop.setAlphaAcid(new PercentageUnit(0.60));
		hop.setForm(Hop.Form.ISOMERIZED_EXTRACT);

		HopAddition hopAddition = new HopAddition(
			hop,
			new WeightUnit(10, GRAMS),
			GRAMS,
			new TimeUnit(0, MINUTES, false));
		hopAddition.setForm(Hop.Form.ISOMERIZED_EXTRACT);

		Volume beerIn = newVolume(BEER_IN);
		WeightUnit expectedIso = Equations.calcHopAlphaAcidsMg(hopAddition);

		PackageStep packageStep = packageStepWithHop(hopAddition);
		packageStep.setCarbonationMethod(PackageStep.CarbonationMethod.FORCE_CARB);

		Volumes volumes = new Volumes();
		volumes.addVolume(BEER_IN, beerIn);

		ProcessLog log = new ProcessLog();
		packageStep.apply(volumes, new EquipmentProfile("test"), log);

		assertEquals(0, log.getErrors().size());

		Volume packaged = volumes.getVolume(BEER_OUT);
		WeightUnit isoOut = HopAcidVolumes.get(packaged, Volume.Metric.ISO_ALPHA_ACIDS_MG);
		assertEquals(expectedIso.get(Quantity.Unit.MILLIGRAMS), isoOut.get(Quantity.Unit.MILLIGRAMS), 0.01);
		assertHopAcidZeroOrAbsent(packaged, Volume.Metric.ALPHA_ACIDS_MG);
	}

	/*-------------------------------------------------------------------------*/

	private static Volume newVolume(String name)
	{
		Volume beer = new Volume(name, Volume.Type.BEER);
		beer.setVolume(new VolumeUnit(20, LITRES));
		beer.setGravity(new DensityUnit(12, Quantity.Unit.GU, false));
		beer.setFermentability(new PercentageUnit(0.75));
		beer.setTemperature(new TemperatureUnit(18, CELSIUS));
		beer.setColour(new ColourUnit(8, SRM, false));
		beer.setAbv(new PercentageUnit(5));
		return beer;
	}

	private static PackageStep packageStepWithHop(HopAddition hopAddition)
	{
		ArrayList<IngredientAddition> ingredients = new ArrayList<>();
		ingredients.add(hopAddition);

		return new PackageStep(
			"package",
			"",
			ingredients,
			BEER_IN,
			BEER_OUT,
			new VolumeUnit(0, LITRES),
			null,
			PackageStep.PackagingType.KEG,
			PackageStep.CarbonationMethod.FORCE_CARB,
			null);
	}

	private static void assertHopAcidZeroOrAbsent(Volume volume, Volume.Metric metric)
	{
		WeightUnit mass = metric == Volume.Metric.ALPHA_ACIDS_MG
			? volume.getAlphaAcidsMg()
			: volume.getIsoAlphaAcidsMg();
		if (mass == null)
		{
			return;
		}
		assertEquals(0D, mass.get(Quantity.Unit.MILLIGRAMS), 0.01);
	}
}
