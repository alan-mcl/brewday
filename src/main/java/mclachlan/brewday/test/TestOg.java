
/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.test;

import java.util.*;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.WaterAddition;

/**
 *
 */
public class TestOg
{
	public static void main(String[] args)
	{
		Fermentable fermentable = new Fermentable("Pale Malt (Weyerman)");
		fermentable.setYield(new PercentageUnit(.80));
		fermentable.setColour(new ColourUnit(5, Quantity.Unit.SRM));
		fermentable.setMoisture(new PercentageUnit(.04));
		FermentableAddition fa = new FermentableAddition(fermentable, new WeightUnit(5, Quantity.Unit.KILOGRAMS), Quantity.Unit.KILOGRAMS, new TimeUnit(60, Quantity.Unit.MINUTES));

		WaterAddition waterAddition = new WaterAddition(
			new Water("test"),
			new VolumeUnit(15, Quantity.Unit.LITRES),
			Quantity.Unit.LITRES,
			new TemperatureUnit(70, Quantity.Unit.CELSIUS),
			new TimeUnit(60, Quantity.Unit.MINUTES));

		ArrayList<IngredientAddition> mashAdditions = new ArrayList<>();
		mashAdditions.add(fa);
		mashAdditions.add(waterAddition);

		String mashVolOutName = "mashVolOut";

		Mash mash = new Mash("mash", "",
			mashAdditions, null, mashVolOutName,
			new TimeUnit(60, Quantity.Unit.MINUTES),
			new TemperatureUnit(20, Quantity.Unit.CELSIUS));

		ProcessLog log = new ProcessLog();
		EquipmentProfile equipmentProfile = new EquipmentProfile();
		Volumes volumes = new Volumes();

		equipmentProfile.setConversionEfficiency(new PercentageUnit(1));
		equipmentProfile.setMashTunVolume(new VolumeUnit(99, Quantity.Unit.LITRES));
		equipmentProfile.setLauterLoss(new VolumeUnit(0, Quantity.Unit.LITRES));

		System.out.println("MASH");
		mash.apply(volumes, equipmentProfile, log);

		Volume mashVolOut = volumes.getVolume(mashVolOutName);

		System.out.println("mashVolOut = " + mashVolOut.getVolume().get(Quantity.Unit.LITRES)+"L @ "+
			mashVolOut.getGravity().get(Quantity.Unit.SPECIFIC_GRAVITY));

		//--------------
		String lauterMashOut = "lauterMashOut";
		String firstRunnings = "firstRunnings";
		Lauter lauter = new Lauter("lauter", "", mashVolOutName, lauterMashOut, firstRunnings);

		System.out.println("LAUTER");
		lauter.apply(volumes, equipmentProfile, log);

		mashVolOut = volumes.getVolume(lauterMashOut);
		Volume firstRunoff = volumes.getVolume(firstRunnings);

		System.out.println("mashVolOut = " + mashVolOut.getVolume().get(Quantity.Unit.LITRES)+"L @ "+
			mashVolOut.getGravity().get(Quantity.Unit.SPECIFIC_GRAVITY));
		System.out.println("firstRunoff = " + firstRunoff.getVolume().get(Quantity.Unit.LITRES)+"L @ "+
			firstRunoff.getGravity().get(Quantity.Unit.SPECIFIC_GRAVITY));

		//--------------

		WaterAddition bsWaterAddition = new WaterAddition(
			new Water("bsWater"),
			new VolumeUnit(10, Quantity.Unit.LITRES),
			Quantity.Unit.LITRES,
			new TemperatureUnit(70, Quantity.Unit.CELSIUS),
			new TimeUnit(0, Quantity.Unit.MINUTES));

		ArrayList<IngredientAddition> bsAdditions = new ArrayList<>();
		bsAdditions.add(bsWaterAddition);

		String bsCombinedWortOut = "bsCombinedWortOut";
		String bsSpargeRunningsOut = "bsSpargeRunningsOut";
		String bsMashVolOut = "bsMashVolOut";
		BatchSparge bs = new BatchSparge("bs", "",
			lauterMashOut, firstRunnings, bsCombinedWortOut, bsSpargeRunningsOut, bsMashVolOut,
			bsAdditions);

		System.out.println("BATCH SPARGE");
		bs.apply(volumes, equipmentProfile, log);

		mashVolOut = volumes.getVolume(bsMashVolOut);
		Volume spargeRunnings = volumes.getVolume(bsSpargeRunningsOut);
		Volume combinedWort = volumes.getVolume(bsCombinedWortOut);

		System.out.println("mashVolOut = " + mashVolOut.getVolume().get(Quantity.Unit.LITRES)+"L @ "+
			mashVolOut.getGravity().get(Quantity.Unit.SPECIFIC_GRAVITY));
		System.out.println("spargeRunnings = " + spargeRunnings.getVolume().get(Quantity.Unit.LITRES)+"L @ "+
			spargeRunnings.getGravity().get(Quantity.Unit.SPECIFIC_GRAVITY));
		System.out.println("combinedWort = " + combinedWort.getVolume().get(Quantity.Unit.LITRES)+"L @ "+
			combinedWort.getGravity().get(Quantity.Unit.SPECIFIC_GRAVITY));
	}
}
