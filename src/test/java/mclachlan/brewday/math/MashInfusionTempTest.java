package mclachlan.brewday.math;

import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.KILOGRAMS;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MashInfusionTempTest
{
	@Test
	public void infusionWaterTempAndResultingMashTempAreConsistent()
	{
		WeightUnit grainWeight = new WeightUnit(5, KILOGRAMS);
		VolumeUnit mashLiquor = new VolumeUnit(15, LITRES);
		TemperatureUnit currentMashTemp = new TemperatureUnit(50, CELSIUS);
		VolumeUnit infusionVolume = new VolumeUnit(5, LITRES);
		TemperatureUnit targetMashTemp = new TemperatureUnit(65, CELSIUS);

		TemperatureUnit infusionWaterTemp = Equations.calcMashInfusionWaterTemp(
			grainWeight,
			mashLiquor,
			currentMashTemp,
			infusionVolume,
			targetMashTemp);

		TemperatureUnit resultingMashTemp = Equations.calcMashInfusionTemp(
			grainWeight,
			mashLiquor,
			currentMashTemp,
			infusionVolume,
			infusionWaterTemp);

		assertEquals(targetMashTemp.get(CELSIUS), resultingMashTemp.get(CELSIUS), 0.0001);
	}

	@Test
	public void palmerModelDiffersFromTwoFluidMixOnThickMash()
	{
		WeightUnit grainWeight = new WeightUnit(5, KILOGRAMS);
		VolumeUnit mashLiquor = new VolumeUnit(15, LITRES);
		TemperatureUnit currentMashTemp = new TemperatureUnit(50, CELSIUS);
		VolumeUnit infusionVolume = new VolumeUnit(5, LITRES);
		TemperatureUnit targetMashTemp = new TemperatureUnit(65, CELSIUS);

		// Total mash volume includes dissolved extract displacement beyond liquor alone.
		VolumeUnit mashVolume = new VolumeUnit(19, LITRES);

		TemperatureUnit palmerInfusionTemp = Equations.calcMashInfusionWaterTemp(
			grainWeight,
			mashLiquor,
			currentMashTemp,
			infusionVolume,
			targetMashTemp);

		TemperatureUnit twoFluidInfusionTemp = Equations.calcAdditionTemperature(
			mashVolume,
			currentMashTemp,
			infusionVolume,
			targetMashTemp);

		assertNotEquals(
			palmerInfusionTemp.get(CELSIUS),
			twoFluidInfusionTemp.get(CELSIUS),
			0.5);
	}
}
