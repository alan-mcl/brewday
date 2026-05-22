/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.math;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.process.BitternessVolumes;
import mclachlan.brewday.process.HopAcidVolumes;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.*;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 *
 */
public class Equations
{
	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the new temperature of the body of fluid after an addition of
	 * some amount at a different temperature.
	 *
	 * @return New temp of the combined fluid volume.
	 */
	public static TemperatureUnit calcCombinedTemperature(
		VolumeUnit currentVolume,
		TemperatureUnit currentTemperature,
		VolumeUnit volumeAddition,
		TemperatureUnit tempAddition)
	{
		boolean estimated =
			currentVolume.isEstimated() || currentTemperature.isEstimated() ||
				volumeAddition.isEstimated() || tempAddition.isEstimated();

		return new TemperatureUnit(
			(
				(currentVolume.get(MILLILITRES) *
					currentTemperature.get(CELSIUS) *
					Const.SPECIFIC_HEAT_OF_WATER)
					+
					volumeAddition.get(MILLILITRES) *
						tempAddition.get(CELSIUS) *
						Const.SPECIFIC_HEAT_OF_WATER
			)
				/
				(
					currentVolume.get(MILLILITRES) *
						Const.SPECIFIC_HEAT_OF_WATER
						+
						volumeAddition.get(MILLILITRES) *
							Const.SPECIFIC_HEAT_OF_WATER
				),
			CELSIUS,
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return A water profile that results from blending the two given volumes.
	 */
	public static Water calcCombinedWaterProfile(
		Water w1, VolumeUnit v1, Water w2, VolumeUnit v2)
	{
		Water result = new Water(w1.getName() + "/" + w2.getName());

		result.setBicarbonate((PpmUnit)calcCombinedLinearInterpolation(v1, w1.getBicarbonate(), v2, w2.getBicarbonate()));
		result.setSulfate((PpmUnit)calcCombinedLinearInterpolation(v1, w1.getSulfate(), v2, w2.getSulfate()));
		result.setChloride((PpmUnit)calcCombinedLinearInterpolation(v1, w1.getChloride(), v2, w2.getChloride()));
		result.setMagnesium((PpmUnit)calcCombinedLinearInterpolation(v1, w1.getMagnesium(), v2, w2.getMagnesium()));
		result.setCalcium((PpmUnit)calcCombinedLinearInterpolation(v1, w1.getCalcium(), v2, w2.getCalcium()));
		result.setSodium((PpmUnit)calcCombinedLinearInterpolation(v1, w1.getSodium(), v2, w2.getSodium()));

		// Linear interpolation of pH is not correct.
		// See for eg http://www.frenchcreeksoftware.com/Predicting%20Properties%20of%20Blended%20Waters%20AWT2008.pdf
		// and http://downloads.hindawi.com/journals/jchem/2011/391396.pdf
		// But the water pH doesn't actually matter for the mash pH calculations that
		// we desire so let's just let this slide.
		result.setPh((PhUnit)calcCombinedLinearInterpolation(v1, w1.getPh(), v2, w2.getPh()));

		return result;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Sources:
	 * <ul>
	 *    <li>Kaiser Water spreadsheet
	 *    <li>http://howtobrew.com/book/section-3/understanding-the-mash-ph/using-salts-for-brewing-water-adjustment
	 *    <li>https://github.com/jcipar/brewing-salts/blob/master/brewing-salts-numeric.js
	 * </ul>
	 *
	 * @return the water profile of the given addition after adding the given n
	 * water agent
	 */
	public static Water calcBrewingSaltAddition(WaterAddition wa,
		MiscAddition ma)
	{
		if (!(ma.getQuantity() instanceof WeightUnit))
		{
			// we only support brewing salts being weight additions
			return wa.getWater();
		}

		Water result = new Water(wa.getWater());

		double volGal = wa.getQuantity().get(US_GALLON);
		double volL = wa.getQuantity().get(LITRES);
		double grams = ma.getQuantity().get(GRAMS);
		double gPerGal = grams / volGal;
		double mgPerL = grams * 1000 / volL;

		double ca = result.getCalcium().get();
		double so4 = result.getSulfate().get();
		double cl = result.getChloride().get();
		double mg = result.getMagnesium().get();
		double na = result.getSodium().get();
		double hco3 = result.getBicarbonate().get();

		Misc.WaterAdditionFormula chemical_formula = ma.getMisc().getWaterAdditionFormula();

		switch (chemical_formula)
		{
			// from Kaiser Water:
			case CALCIUM_CARBONATE_UNDISSOLVED:
				result.setCalcium(new PpmUnit(ca + mgPerL * (40.08 / 100.09)));
				result.setBicarbonate(new PpmUnit(hco3 + mgPerL * (61 / 100.09) * 2));
				break;

			case CALCIUM_CARBONATE_DISSOLVED:
				result.setCalcium(new PpmUnit(ca + mgPerL * (40.08 / 100.09) / 2));
				result.setBicarbonate(new PpmUnit(hco3 + mgPerL * (61 / 100.09)));
				break;

			case CALCIUM_SULPHATE_DIHYDRATE:
				result.setCalcium(new PpmUnit(ca + mgPerL * (40.08 / 172.19)));
				result.setSulfate(new PpmUnit(so4 + mgPerL * (96.07 / 172.19)));
				break;

			case CALCIUM_CHLORIDE_DIHYDRATE:
				result.setCalcium(new PpmUnit(ca + mgPerL * (40.08 / 147.02)));
				result.setChloride(new PpmUnit(cl + mgPerL * (70.9 / 147.02)));
				break;

			case MAGNESIUM_SULFATE_HEPTAHYDRATE:
				result.setMagnesium(new PpmUnit(mg + mgPerL * (24.31 / 246.51)));
				result.setSulfate(new PpmUnit(so4 + mgPerL * (96.07 / 246.51)));
				break;

			case SODIUM_BICARBONATE:
				result.setSodium(new PpmUnit(na + mgPerL * (23D / 84D)));
				result.setBicarbonate(new PpmUnit(hco3 + mgPerL * (61D / 84D)));
				break;

			case SODIUM_CHLORIDE:
				result.setSodium(new PpmUnit(na + mgPerL * (23D / 58.44)));
				result.setChloride(new PpmUnit(cl + mgPerL * (35.45 / 58.44)));
				break;

			// these formulas from Brewing Salts
			case CALCIUM_BICARBONATE:
//				result.setCalcium(new PpmUnit(ca + 142.8*gPerGal));
//				result.setBicarbonate(new PpmUnit(hco3 + 434.8*gPerGal));
				result.setCalcium(new PpmUnit(ca + mgPerL * (40.08 / 162.11)));
				result.setBicarbonate(new PpmUnit(hco3 + mgPerL * (61D / 162.11)));
				break;

			case MAGNESIUM_CHLORIDE_HEXAHYDRATE:
//				result.setMagnesium(new PpmUnit(mg + 31.6*gPerGal));
//				result.setChloride(new PpmUnit(cl + 92.2*gPerGal));
				result.setMagnesium(new PpmUnit(mg + mgPerL * (24.31 / 95.21)));
				result.setChloride(new PpmUnit(cl + mgPerL * (35.45 / 95.21)));
				break;

			case LACTIC_ACID:
			case PHOSPHORIC_ACID:
				// no op on these, they need to be handled separately in the
				// pH calculation functions
				break;

			default:
				throw new BrewdayException("invalid " + chemical_formula);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: http://homebrewingphysics.blogspot.com/ (version 4.2)
	 */
	public static PhUnit calcMashPhMpH(
		WaterAddition mashWater,
		List<FermentableAddition> allAdditions,
		List<MiscAddition> miscAdditions)
	{
		List<FermentableAddition> grainBill = new ArrayList<>();

		// filter out stuff that won't impact the pH
		for (FermentableAddition fa : allAdditions)
		{
			Fermentable f = fa.getFermentable();
			if (f.getBufferingCapacity() != null && f.getBufferingCapacity().get() > 0 &&
				f.getDistilledWaterPh() != null && f.getDistilledWaterPh().get() > 0)
			{
				grainBill.add(fa);
			}
		}

		// sum up the grist distilled pH and buffering capacity
		WeightUnit weightUnit = calcTotalGrainWeight(grainBill);
		double totalGrainWeight = weightUnit.get(KILOGRAMS);
		double total_phi_bi = 0D;
		double total_bi = 0D;

		double acidMaltMeqL = 0D;
		double lacticAcidMeqL = 0D;
		double phosphoricAcidMeqL = 0D;

		for (FermentableAddition fa : grainBill)
		{
			Fermentable fermentable = fa.getFermentable();

			if (fermentable.getType().getQuantityType() != Quantity.Type.VOLUME)
			{
				double ph_i = fermentable.getDistilledWaterPh() == null ? 5.6 : fermentable.getDistilledWaterPh().get(PH);
				double b_i = fermentable.getBufferingCapacity() == null ? 51.5 : fermentable.getBufferingCapacity().get(MEQ_PER_KILOGRAM);
				double f_i = fa.getQuantity().get(KILOGRAMS) / totalGrainWeight;

				double phi_bi_fi = ph_i * b_i * f_i;
				double bi_fi = b_i * f_i;

				total_phi_bi += phi_bi_fi;
				total_bi += bi_fi;

				if (fermentable.getLacticAcidContent() != null && fermentable.getLacticAcidContent().get() > 0)
				{
					double perc = fermentable.getLacticAcidContent().get(PERCENTAGE);
					acidMaltMeqL += (-perc * fa.getQuantity().get(OUNCES) * 28.35 / 90.09 / mashWater.getVolume().get(LITRES) * 1000);
				}
			}
		}
		for (MiscAddition ma : miscAdditions)
		{
			Misc m = ma.getMisc();
			if (m.getAcidContent() != null && m.getAcidContent().get(PERCENTAGE) > 0)
			{
				double perc = m.getAcidContent().get(PERCENTAGE);
				double ml = ma.getQuantity().get(MILLILITRES);

				if (m.getWaterAdditionFormula() == Misc.WaterAdditionFormula.LACTIC_ACID)
				{
					double density = 1 + 0.237 * perc;
					lacticAcidMeqL += (-perc * density / 90.09 * 1000 * ml / mashWater.getVolume().get(LITRES));
				}
				else if (m.getWaterAdditionFormula() == Misc.WaterAdditionFormula.PHOSPHORIC_ACID)
				{
					double density = 1 + 0.49 * perc + 0.375 * Math.pow(perc, 2);
					phosphoricAcidMeqL += (-perc * density / 98 * 1000 * ml / mashWater.getVolume().get(LITRES));
				}
			}
		}

		double waterPh = mashWater.getWater().getPh().get(PH);
		double distilledPh = total_phi_bi / total_bi;
		double mashThickness = mashWater.getVolume().get(LITRES) / totalGrainWeight;

		// this is the bit that MD Riffe worked out from forum user data
		double maltBufferingCorrectionFactor = Double.valueOf(
			Database.getInstance().getSettings().get(
				Settings.MPH_MALT_BUFFERING_CORRECTION_FACTOR));

		double ph_ra_slope = mashThickness / total_bi / maltBufferingCorrectionFactor;

		// divide by 50 to convert from "ppm as CaCO3" to mEq/L
		double c_alkalinity = calcAlkalinitySimple(mashWater.getWater()).get(PPM) / 50;
		double c_total = calcAlkalinity(mashWater.getWater()).get(PPM) / 50;

		double caMeqL = 2 * mashWater.getWater().getCalcium().get(PPM) / 40.078;
		double mgMeqL = 2 * mashWater.getWater().getMagnesium().get(PPM) / 24.305;

		// work out the mash pH with these malts
		double ph = distilledPh;
		double fph;
		double z_alk;
		double zra;

		// fph_i =(1 + 4.435*10^(-7)*10^WaterPH +4.435*10^(-7)* 4.667*10^(-11)*10^(2*WaterPH) ) / (4.435*10^(-7)*10^WaterPH)
		double fph_i = (1 + 0.0000004435 * Math.pow(10, waterPh) + 0.0000004435 * 0.00000000004667 * Math.pow(10, 2 * waterPh))
			/ (0.0000004435 * Math.pow(10, waterPh));

		// the spreadsheet uses 26 iterations, i think this is way too many but hey
		for (int i = 0; i < 26; i++)
		{
			// f(pH)=(1 + 4.435*10^(-7)*10^ph + 4.435*10^(-7)* 4.667*10^(-11)*10^(2*ph) ) / (4.435*10^(-7)*10^ph)
			fph = (1 + 0.0000004435 * Math.pow(10, ph) + 0.0000004435 * 0.00000000004667 * Math.pow(10, 2 * ph))
				/ (0.0000004435 * Math.pow(10, ph));

			// z_alk =(1 + 2*4.667*10^(-11)*10^WaterPh -fph_i/fph)
			z_alk = (1 + 2 * 0.00000000004667 * Math.pow(10, waterPh) - fph_i / fph);

			// zra =c_alk - c_totall/fph - (Ca meq/L)/2.8 - (Mg meq/L))/5.6 + (phos_mEqL) + (lact_mEqL) + (acidmalt_mEqL) + (OH-_mEqL)
			// todo: we do not support calcium hydroxide additions yet, when we do this should be updated to include the OH- impact here
			zra = c_alkalinity - c_total / fph - (caMeqL / 2.8) - (mgMeqL / 5.6) + phosphoricAcidMeqL + lacticAcidMeqL + acidMaltMeqL;

			ph = distilledPh + ph_ra_slope * zra;
		}

		return new PhUnit(ph);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Assumes that there are no acid additions in the mash. Source:
	 * http://homebrewingphysics.blogspot.com/ (version 4.2)
	 *
	 * @return the acid volume needed to reach the target ph
	 */
	public static VolumeUnit calcMashAcidAdditionMpH(
		Misc acid,
		PhUnit targetPh,
		WaterAddition mashWater,
		List<FermentableAddition> grainBill,
		List<MiscAddition> origMiscAdditions)
	{
		if (acid.getAcidContent() != null && acid.getAcidContent().get(PERCENTAGE) > 0)
		{
			if (!acid.isAcidAddition())
			{
				return null;
			}
		}

		// work this out iteratively, kind of a binary search
		double target = targetPh.get(PH);
		double diff = Double.MAX_VALUE;
		double additionMl = 0.01;
		double ph;

		MiscAddition acidAddition = new MiscAddition(acid, new VolumeUnit(additionMl, MILLILITRES), MILLILITRES, new TimeUnit(0));
		ArrayList<MiscAddition> miscAdditions = new ArrayList<>(origMiscAdditions);

		while (Math.abs(diff) > 0.005)
		{
			acidAddition.setQuantity(new VolumeUnit(additionMl, MILLILITRES));
			miscAdditions.add(acidAddition);
			ph = calcMashPhMpH(mashWater, grainBill, miscAdditions).get(PH);
			miscAdditions.remove(acidAddition);

			diff = target - ph;

			if (ph > target)
			{
				additionMl = additionMl + 0.005;
			}
			else
			{
				break;
			}
		}

		return new VolumeUnit(additionMl, MILLILITRES);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: https://ezwatercalculator.com/ (version 3.0.2)
	 */
	public static PhUnit calcMashPhEzWater(
		WaterAddition mashWater,
		List<FermentableAddition> allAdditions,
		List<MiscAddition> miscAdditions)
	{
		List<FermentableAddition> grainBill = new ArrayList<>();

		// filter out stuff that won't impact the pH
		for (FermentableAddition fa : allAdditions)
		{
			Fermentable f = fa.getFermentable();
			if (f.getBufferingCapacity() != null && f.getBufferingCapacity().get() > 0 &&
				f.getDistilledWaterPh() != null && f.getDistilledWaterPh().get() > 0)
			{
				grainBill.add(fa);
			}
		}

		// sum up the grist impact on distilled water ph
		// also detect any acid malt
		WeightUnit weightUnit = calcTotalGrainWeight(grainBill);
		double totalGrainWeight = weightUnit.get(KILOGRAMS);
		double distilledPh = 0;
		double acidMaltContrib = 0;
		double lacticAcidAdditions = 0;
		for (FermentableAddition fa : grainBill)
		{
			Fermentable fermentable = fa.getFermentable();
			if (fermentable.getType().getQuantityType() != Quantity.Type.VOLUME)
			{
				double phi = fermentable.getDistilledWaterPh() == null ? 5.6 : fermentable.getDistilledWaterPh().get(PH);
				double grainWeight = fa.getQuantity().get(KILOGRAMS);
				distilledPh += (phi * grainWeight);

				if (fermentable.getLacticAcidContent() != null && fermentable.getLacticAcidContent().get() > 0)
				{
					acidMaltContrib += (fermentable.getLacticAcidContent().get(PERCENTAGE) * fa.getQuantity().get(OUNCES));
				}
			}
		}
		for (MiscAddition ma : miscAdditions)
		{
			Misc m = ma.getMisc();
			if (m.getWaterAdditionFormula() == Misc.WaterAdditionFormula.LACTIC_ACID &&
				m.getAcidContent() != null && m.getAcidContent().get(PERCENTAGE) > 0)
			{
				double perc = m.getAcidContent().get(PERCENTAGE);
				double ml = ma.getQuantity().get(MILLILITRES);
				lacticAcidAdditions += (perc * ml);
			}
		}

		distilledPh /= totalGrainWeight;

		double totalGrainWeightLbs = weightUnit.get(POUNDS);

		// =HCo3(ppm) * 50/61 + (-176.1*[lactic acid %]*[lactic acid ml]*2 -4160.4*[acid malt %]*[acid malt oz]*2.5)/[water vol gal]
		// we are folding the water additions into the water profile so ignoreing those,
		// but still need to include acid malt and acid misc additions

		// calculate residual alkalinity
		double hco3 = mashWater.getWater().getBicarbonate().get(PPM);
		double waterGal = mashWater.getQuantity().get(US_GALLON);
		double h = hco3 * (50D / 61D);

		double la = (-176.1 * lacticAcidAdditions * 2) / waterGal;
		double mc = (4160.4 * acidMaltContrib * 2.5) / waterGal;

		double ca = mashWater.getWater().getCalcium().get(PPM) / 1.4;
		double mg = mashWater.getWater().getMagnesium().get(PPM) / 1.7;
		double m = 0.1085 * waterGal / totalGrainWeightLbs + 0.013;

		double effectiveAlk = h + la - mc;

		double residualAlk = effectiveAlk - ca - mg;

		// estimate the room temp ph: adjust the distilled water ph with the residual alk
		double estPh = distilledPh + m * residualAlk / 50;

		return new PhUnit(estPh, true);
	}

	/*-------------------------------------------------------------------------*/


	/**
	 * Assumes that there are no acid additions in the mash. Source:
	 * https://ezwatercalculator.com/
	 *
	 * @return the acid volume needed to reach the target ph
	 */
	public static VolumeUnit calcMashAcidAdditionEzWater(
		Misc acid,
		PhUnit targetPh,
		WaterAddition mashWater,
		List<FermentableAddition> grainBill,
		List<MiscAddition> origAdditions)
	{
		if (acid.getAcidContent() != null && acid.getAcidContent().get(PERCENTAGE) > 0)
		{
			if (!acid.isAcidAddition())
			{
				return null;
			}
		}

		// work this out iteratively, kind of a binary search
		double target = targetPh.get(PH);
		double diff = Double.MAX_VALUE;
		double additionMl = 0.01;
		double ph;

		MiscAddition acidAddition = new MiscAddition(acid, new VolumeUnit(additionMl, MILLILITRES), MILLILITRES, new TimeUnit(0));
		ArrayList<MiscAddition> miscAdditions = new ArrayList<>(origAdditions);


		while (Math.abs(diff) > 0.005)
		{
			acidAddition.setQuantity(new VolumeUnit(additionMl, MILLILITRES));
			miscAdditions.add(acidAddition);
			ph = calcMashPhEzWater(mashWater, grainBill, miscAdditions).get(PH);
			miscAdditions.remove(acidAddition);

			diff = target - ph;

			if (ph > target)
			{
				additionMl = additionMl + 0.005;
			}
			else
			{
				break;
			}
		}

		return new VolumeUnit(additionMl, MILLILITRES);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Kai Troester specialty malt titration endpoint (mEq/kg basis).
	 */
	private static final double KAISER_SPECIALTY_TITRATION_PH = 5.7D;

	private static final double KAISER_SPECIALTY_ACIDITY_COEFF = 0.14D;

	private static final double KAISER_ROASTED_ACIDITY_MEQ_KG = 40D;

	private static final double KAISER_CRYSTAL_ACIDITY_INTERCEPT = 14D;

	private static final double KAISER_CRYSTAL_ACIDITY_SLOPE = 0.13D;

	private static final double KAISER_DEFAULT_BASE_PH = 5.72D;

	private static final double KAISER_PH_SLOPE_INTERCEPT = 0.013D;

	private static final double KAISER_PH_SLOPE_THICKNESS = 0.013D;

	private enum KaiserMaltRole
	{
		BASE,
		SPECIALTY_CRYSTAL,
		SPECIALTY_ROASTED
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * <ul>
	 *    <li>http://braukaiser.com/documents/effect_of_water_and_grist_on_mash_pH.pdf</li>
	 *    <li>http://braukaiser.com/documents/Kaiser_water_calculator_US_units.xls</li>
	 * </ul>
	 */
	public static PhUnit calcMashPhKaiserWater(
		WaterAddition mashWater,
		List<FermentableAddition> allAdditions,
		List<MiscAddition> miscAdditions)
	{
		List<FermentableAddition> grainBill = filterKaiserGrainBill(allAdditions);
		WeightUnit weightUnit = calcTotalGrainWeight(grainBill);
		double totalGrainWeightKg = weightUnit.get(KILOGRAMS);
		if (totalGrainWeightKg <= 0)
		{
			return new PhUnit(KAISER_DEFAULT_BASE_PH, true);
		}

		double mashThickness = mashWater.getVolume().get(LITRES) / totalGrainWeightKg;
		double gristPh = calcKaiserGristDistilledPh(grainBill, totalGrainWeightKg, mashThickness);
		double residualAlkMeqL = calcKaiserResidualAlkMeqL(mashWater, grainBill, miscAdditions);
		double phSlope = calcKaiserPhAlkalinitySlope(mashThickness);
		double mashPh = gristPh + phSlope * residualAlkMeqL;

		return new PhUnit(mashPh, true);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * http://braukaiser.com/documents/Kaiser_water_calculator_US_units.xls
	 *
	 * @return the acid volume needed to reach the target ph
	 */
	public static VolumeUnit calcMashAcidAdditionKaiserWater(
		Misc acid,
		PhUnit targetPh,
		WaterAddition mashWater,
		List<FermentableAddition> grainBill,
		List<MiscAddition> origMiscAdditions)
	{
		if (acid.getAcidContent() != null && acid.getAcidContent().get(PERCENTAGE) > 0)
		{
			if (!acid.isAcidAddition())
			{
				return null;
			}
		}

		double target = targetPh.get(PH);
		double diff = Double.MAX_VALUE;
		double additionMl = 0.01;
		double ph;

		MiscAddition acidAddition = new MiscAddition(acid, new VolumeUnit(additionMl, MILLILITRES), MILLILITRES, new TimeUnit(0));
		ArrayList<MiscAddition> miscAdditions = new ArrayList<>(origMiscAdditions);

		while (Math.abs(diff) > 0.005)
		{
			acidAddition.setQuantity(new VolumeUnit(additionMl, MILLILITRES));
			miscAdditions.add(acidAddition);
			ph = calcMashPhKaiserWater(mashWater, grainBill, miscAdditions).get(PH);
			miscAdditions.remove(acidAddition);

			diff = target - ph;

			if (ph > target)
			{
				additionMl = additionMl + 0.005;
			}
			else
			{
				break;
			}
		}

		return new VolumeUnit(additionMl, MILLILITRES);
	}

	/*-------------------------------------------------------------------------*/

	protected static List<FermentableAddition> filterKaiserGrainBill(
		List<FermentableAddition> allAdditions)
	{
		List<FermentableAddition> grainBill = new ArrayList<>();

		for (FermentableAddition fa : allAdditions)
		{
			Fermentable f = fa.getFermentable();
			if (f.getType() != null && f.getType().getQuantityType() == Quantity.Type.VOLUME)
			{
				continue;
			}
			grainBill.add(fa);
		}

		return grainBill;
	}

	/*-------------------------------------------------------------------------*/

	protected static double calcKaiserGristDistilledPh(
		List<FermentableAddition> grainBill,
		double totalGrainWeightKg,
		double mashThickness)
	{
		double basePhWeight = 0D;
		double specialtyFraction = 0D;
		double specialtyAcidityWeight = 0D;

		for (FermentableAddition fa : grainBill)
		{
			Fermentable fermentable = fa.getFermentable();
			double grainWeightKg = fa.getQuantity().get(KILOGRAMS);
			double fraction = grainWeightKg / totalGrainWeightKg;
			KaiserMaltRole role = classifyKaiserMaltRole(fermentable);

			if (role == KaiserMaltRole.BASE)
			{
				double ph = fermentable.getDistilledWaterPh() == null
					? KAISER_DEFAULT_BASE_PH
					: fermentable.getDistilledWaterPh().get(PH);
				basePhWeight += ph * fraction;
			}
			else
			{
				specialtyFraction += fraction;
				specialtyAcidityWeight += calcKaiserSpecificAcidityMeqKg(fermentable) * fraction;
			}
		}

		return basePhWeight + KAISER_SPECIALTY_TITRATION_PH * specialtyFraction
			- KAISER_SPECIALTY_ACIDITY_COEFF * specialtyAcidityWeight / mashThickness;
	}

	/*-------------------------------------------------------------------------*/

	protected static double calcKaiserSpecificAcidityMeqKg(
		Fermentable fermentable)
	{
		KaiserMaltRole role = classifyKaiserMaltRole(fermentable);

		if (role == KaiserMaltRole.SPECIALTY_ROASTED)
		{
			return KAISER_ROASTED_ACIDITY_MEQ_KG;
		}

		double colourEbc = 0D;
		if (fermentable.getColour() != null)
		{
			colourEbc = fermentable.getColour().get(EBC);
		}

		return KAISER_CRYSTAL_ACIDITY_INTERCEPT + KAISER_CRYSTAL_ACIDITY_SLOPE * colourEbc;
	}

	/*-------------------------------------------------------------------------*/

	protected static KaiserMaltRole classifyKaiserMaltRole(
		Fermentable fermentable)
	{
		String name = fermentable.getName() == null ? "" : fermentable.getName().toLowerCase();

		if (nameContainsOr(name, "cara", "caramel", "crystal", "dextrin"))
		{
			return KaiserMaltRole.SPECIALTY_CRYSTAL;
		}

		if (nameContainsOr(name, "roast", "roasted", "chocolate", "choc", "black", "carafa", "choklad"))
		{
			return KaiserMaltRole.SPECIALTY_ROASTED;
		}

		if (nameContainsOr(name, "amber", "biscuit", "victory", "melanoidin", "melanoiden", "honey malt",
			"brown malt", "special roast", "kiln coffee", "cafe malt"))
		{
			return KaiserMaltRole.SPECIALTY_CRYSTAL;
		}

		if (fermentable.getColour() != null && fermentable.getColour().get(EBC) > 40D)
		{
			return KaiserMaltRole.SPECIALTY_ROASTED;
		}

		if (fermentable.getColour() != null && fermentable.getColour().get(EBC) > 15D)
		{
			return KaiserMaltRole.SPECIALTY_CRYSTAL;
		}

		return KaiserMaltRole.BASE;
	}

	/*-------------------------------------------------------------------------*/

	protected static boolean nameContainsOr(String name, String... tokens)
	{
		for (String token : tokens)
		{
			if (name.contains(token))
			{
				return true;
			}
		}
		return false;
	}

	/*-------------------------------------------------------------------------*/

	protected static double calcKaiserResidualAlkMeqL(
		WaterAddition mashWater,
		List<FermentableAddition> grainBill,
		List<MiscAddition> miscAdditions)
	{
		Water water = mashWater.getWater();
		double alkalinityMeqL = 0D;
		if (water.getBicarbonate() != null)
		{
			alkalinityMeqL = water.getBicarbonate().get(PPM) / 61.02;
		}

		double caMeqL = 0D;
		double mgMeqL = 0D;
		if (water.getCalcium() != null)
		{
			caMeqL = 2 * water.getCalcium().get(PPM) / 40.078;
		}
		if (water.getMagnesium() != null)
		{
			mgMeqL = 2 * water.getMagnesium().get(PPM) / 24.305;
		}

		double mashVolL = mashWater.getVolume().get(LITRES);
		double acidMaltMeqL = 0D;
		double lacticAcidMeqL = 0D;
		double phosphoricAcidMeqL = 0D;

		for (FermentableAddition fa : grainBill)
		{
			Fermentable fermentable = fa.getFermentable();
			if (fermentable.getLacticAcidContent() != null && fermentable.getLacticAcidContent().get() > 0)
			{
				double perc = fermentable.getLacticAcidContent().get(PERCENTAGE);
				acidMaltMeqL += (-perc * fa.getQuantity().get(OUNCES) * 28.35 / 90.09 / mashVolL * 1000);
			}
		}
		for (MiscAddition ma : miscAdditions)
		{
			Misc m = ma.getMisc();
			if (m.getAcidContent() != null && m.getAcidContent().get(PERCENTAGE) > 0)
			{
				double perc = m.getAcidContent().get(PERCENTAGE);
				double ml = ma.getQuantity().get(MILLILITRES);

				if (m.getWaterAdditionFormula() == Misc.WaterAdditionFormula.LACTIC_ACID)
				{
					double density = 1 + 0.237 * perc;
					lacticAcidMeqL += (-perc * density / 90.09 * 1000 * ml / mashVolL);
				}
				else if (m.getWaterAdditionFormula() == Misc.WaterAdditionFormula.PHOSPHORIC_ACID)
				{
					double density = 1 + 0.49 * perc + 0.375 * Math.pow(perc, 2);
					phosphoricAcidMeqL += (-perc * density / 98 * 1000 * ml / mashVolL);
				}
			}
		}

		return alkalinityMeqL - caMeqL / 2.8 - mgMeqL / 5.6
			+ phosphoricAcidMeqL + lacticAcidMeqL + acidMaltMeqL;
	}

	/*-------------------------------------------------------------------------*/

	protected static double calcKaiserPhAlkalinitySlope(
		double mashThicknessLPerKg)
	{
		return KAISER_PH_SLOPE_THICKNESS * mashThicknessLPerKg + KAISER_PH_SLOPE_INTERCEPT;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Palmer/Kaminski Z pH model
	 * ({@code Water: A Comprehensive Guide for Brewers}).
	 */
	private static final double Z_PH_DEFAULT_DI_PH = 5.72D;

	/*-------------------------------------------------------------------------*/

	/**
	 * Palmer/Kaminski "Z pH" mash pH prediction model from:
	 * <p>
	 * Water: A Comprehensive Guide for Brewers
	 * <p>
	 * This implementation intentionally follows the empirical Water-book
	 * methodology rather than modern proton-deficit or equilibrium chemistry
	 * approaches.
	 * <p>
	 * Key characteristics:
	 * <p>
	 * - water contribution via Z residual alkalinity - malt contributions
	 * relative to target pH - empirical/damped buffering contribution model -
	 * iterative zero-sum solving
	 * <p>
	 * Sign convention:
	 * <p>
	 * positive residual: equilibrium mash pH is ABOVE trial pH
	 * <p>
	 * negative residual: equilibrium mash pH is BELOW trial pH
	 */
	public static PhUnit calcMashPhZPh(
		WaterAddition mashWater,
		List<FermentableAddition> allAdditions,
		List<MiscAddition> miscAdditions)
	{
		List<FermentableAddition> grainBill =
			filterKaiserGrainBill(allAdditions);

		if (grainBill.isEmpty())
		{
			return new PhUnit(Z_PH_DEFAULT_DI_PH, true);
		}

		double mashPh = solveZPhBisection(
			mashWater,
			grainBill,
			miscAdditions);

		return new PhUnit(mashPh, true);
	}

	/*-------------------------------------------------------------------------*/

	protected static double solveZPhBisection(
		WaterAddition mashWater,
		List<FermentableAddition> grainBill,
		List<MiscAddition> miscAdditions)
	{
		double low = 4.5D;
		double high = 6.5D;

		while ((high - low) > 0.001D)
		{
			double mid = (low + high) / 2D;

			double residual = calcZPhResidual(
				mid,
				mashWater,
				grainBill,
				miscAdditions);

			/*
			 * positive residual:
			 *     equilibrium lies ABOVE current trial pH
			 */
			if (residual > 0)
			{
				low = mid;
			}
			else
			{
				high = mid;
			}
		}

		return (low + high) / 2D;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Net mash residual in mEq.
	 */
	protected static double calcZPhResidual(
		double targetPh,
		WaterAddition mashWater,
		List<FermentableAddition> grainBill,
		List<MiscAddition> miscAdditions)
	{
		double maltContribution =
			calcZPhMaltContribution(
				targetPh,
				grainBill);

		double waterContribution =
			calcZPhWaterContribution(
				targetPh,
				mashWater);

		double acidContribution =
			calcZPhAcidContribution(
				miscAdditions);

		return
			maltContribution
				+ waterContribution
				- acidContribution;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Water contribution using Palmer/Kaminski Z residual alkalinity method.
	 * <p>
	 * All values are in mEq.
	 */
	protected static double calcZPhWaterContribution(
		double targetPh,
		WaterAddition mashWater)
	{
		Water water = mashWater.getWater();

		double mashVolumeL =
			mashWater.getVolume().get(LITRES);

		/*
		 * Total alkalinity expressed as mEq/L.
		 */
		double totalAlkMeqL =
			calcAlkalinitySimple(water).get(PPM) / 50D;

		double waterPh = 7.0D;

		if (water.getPh() != null
			&& water.getPh().get(PH) > 0)
		{
			waterPh = water.getPh().get(PH);
		}

		/*
		 * Determine Ct from:
		 *
		 * Ct = total alkalinity / deltaChargeTo4_3
		 */
		double dc0 =
			carbonateCharge(4.3D)
				- carbonateCharge(waterPh);

		if (Math.abs(dc0) < 0.0001D)
		{
			dc0 = 0.0001D;
		}

		double ct = totalAlkMeqL / dc0;

		/*
		 * Determine Z alkalinity relative to target pH.
		 */
		double dcz =
			carbonateCharge(targetPh)
				- carbonateCharge(waterPh);

		double zAlkMeqL = ct * dcz;

		/*
		 * Convert Ca and Mg to mEq/L.
		 */
		double caMeqL = 0D;
		double mgMeqL = 0D;

		if (water.getCalcium() != null)
		{
			caMeqL =
				water.getCalcium().get(PPM)
					/ 20.04D;
		}

		if (water.getMagnesium() != null)
		{
			mgMeqL =
				water.getMagnesium().get(PPM)
					/ 12.15D;
		}

		/*
		 * Palmer/Kaminski Z residual alkalinity:
		 *
		 * Z RA = Z alkalinity - (Ca/3.5 + Mg/7)
		 */
		double zRaMeqL =
			zAlkMeqL
				- (caMeqL / 3.5D)
				- (mgMeqL / 7D);

		return zRaMeqL * mashVolumeL;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Empirical carbonate charge approximation derived from Figure 22.
	 * <p>
	 * Returns:
	 * <p>
	 * mEq/mmol charge of carbonate species at pH.
	 */
	protected static double carbonateCharge(double pH)
	{
		/*
		 * Logistic approximation fitted to Figure 22:
		 *
		 * pH 4.3  -> ~ -0.01
		 * pH 5.4  -> ~ -0.10
		 * pH 7.5  -> ~ -0.93
		 * pH 8.4+ -> ~ -1.00
		 */
		double exp =
			Math.exp((pH - 6.35D) * 2.05D);

		return -(exp / (1D + exp));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Malt alkalinity/acidity contribution in mEq.
	 * <p>
	 * This implementation intentionally follows the empirical Water-book
	 * contribution style shown in Figure 21 rather than a strict proton-deficit
	 * model.
	 * <p>
	 * Positive values: alkalinity contribution
	 * <p>
	 * Negative values: acidity contribution
	 */
	protected static double calcZPhMaltContribution(
		double targetPh,
		List<FermentableAddition> grainBill)
	{
		double total = 0D;

		for (FermentableAddition fa : grainBill)
		{
			Fermentable fermentable =
				fa.getFermentable();

			if (fermentable.getType() != null
				&& fermentable.getType().getQuantityType()
				== Quantity.Type.VOLUME)
			{
				continue;
			}

			double diPh = Z_PH_DEFAULT_DI_PH;

			if (fermentable.getDistilledWaterPh() != null
				&& fermentable.getDistilledWaterPh().get(PH) > 0)
			{
				diPh =
					fermentable.getDistilledWaterPh().get(PH);
			}

			/*
			 * Figure 21 clearly demonstrates that the effective
			 * contribution curves are significantly damped relative
			 * to raw buffering-capacity calculations.
			 *
			 * Therefore the Water-book implementation is treated
			 * as empirical contribution space, not strict chemistry.
			 */
			double buffering = 45D;

			if (fermentable.getBufferingCapacity() != null
				&& fermentable.getBufferingCapacity()
				.get(MEQ_PER_KILOGRAM) > 0)
			{
				buffering =
					fermentable.getBufferingCapacity()
						.get(MEQ_PER_KILOGRAM);
			}

			double weightKg =
				fa.getQuantity().get(KILOGRAMS);

			/*
			 * Empirical damping factor derived from Figure 21.
			 *
			 * Raw buffering equations substantially over-predict
			 * specialty malt acidity and base malt alkalinity.
			 */
			double effectiveBuffering =
				buffering * 0.22D;

			double contribution =
				(diPh - targetPh)
					* effectiveBuffering
					* weightKg;

			total += contribution;
		}

		return total;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Strong acid additions in mEq.
	 * <p>
	 * Returned value is POSITIVE acidity and must therefore be SUBTRACTED from
	 * mash residual.
	 */
	protected static double calcZPhAcidContribution(
		List<MiscAddition> miscAdditions)
	{
		double acidMeq = 0D;

		for (MiscAddition ma : miscAdditions)
		{
			Misc m = ma.getMisc();

			if (m.getAcidContent() == null
				|| m.getAcidContent().get(PERCENTAGE) <= 0)
			{
				continue;
			}

			double perc =
				m.getAcidContent().get(PERCENTAGE);

			/*
			 * Assumes fractional percentage:
			 *
			 * 0.80 == 80%
			 */
			double ml =
				ma.getQuantity().get(MILLILITRES);

			if (m.getWaterAdditionFormula()
				== Misc.WaterAdditionFormula.LACTIC_ACID)
			{
				/*
				 * empirical density approximation
				 */
				double density =
					1D + 0.237D * perc;

				double solutionMassG =
					density * ml;

				double acidMassG =
					solutionMassG * perc;

				double moles =
					acidMassG / 90.09D;

				acidMeq += moles * 1000D;
			}
			else if (m.getWaterAdditionFormula()
				== Misc.WaterAdditionFormula.PHOSPHORIC_ACID)
			{
				double density =
					1D
						+ 0.49D * perc
						+ 0.375D * perc * perc;

				double solutionMassG =
					density * ml;

				double acidMassG =
					solutionMassG * perc;

				double moles =
					acidMassG / 98D;

				/*
				 * Treat phosphoric acid as effectively monoprotic
				 * in mash pH range.
				 */
				acidMeq += moles * 1000D;
			}
		}

		return acidMeq;
	}

	/**
	 * Calculates the amount of a specific acid addition required to move
	 * the mash to the target pH using the Z-pH model.
	 *
	 * Returns:
	 *     quantity of acid in millilitres
	 *
	 * Supported:
	 *     - lactic acid
	 *     - phosphoric acid
	 */
	public static VolumeUnit calcMashAcidAdditionZPh(
		Misc acid,
		PhUnit targetMashPh,
		WaterAddition mashWater,
		List<FermentableAddition> grainBill,
		List<MiscAddition> miscAdditions)
	{
		if (acid == null
			|| acid.getWaterAdditionFormula() == null)
		{
			return new VolumeUnit(0);
		}

		/*
		 * Determine current residual at target pH WITHOUT the
		 * acid addition being solved for.
		 */
		double residual =
			calcZPhResidual(
				targetMashPh.get(PH),
				mashWater,
				grainBill,
				miscAdditions);

		/*
		 * Positive residual means mash equilibrium lies ABOVE target pH,
		 * therefore additional acid is required.
		 */
		if (residual <= 0D)
		{
			return new VolumeUnit(0);
		}

		double acidStrengthMeqPerMl =
			calcAcidStrengthMeqPerMl(acid);

		if (acidStrengthMeqPerMl <= 0D)
		{
			return new VolumeUnit(0);
		}

		double requiredMl =
			residual / acidStrengthMeqPerMl;

		return new VolumeUnit(requiredMl, MILLILITRES);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Acid strength in mEq/mL.
	 *
	 * Assumes:
	 *
	 * - acid percentage is fractional:
	 *       0.80 == 80%
	 *
	 * - phosphoric acid behaves effectively monoprotically
	 *   in mash pH range.
	 */
	protected static double calcAcidStrengthMeqPerMl(Misc acid)
	{
		if (acid.getAcidContent() == null)
		{
			return 0D;
		}

		double perc =
			acid.getAcidContent().get(PERCENTAGE);

		if (perc <= 0D)
		{
			return 0D;
		}

		if (acid.getWaterAdditionFormula()
			== Misc.WaterAdditionFormula.LACTIC_ACID)
		{
			/*
			 * empirical density approximation
			 */
			double density =
				1D + 0.237D * perc;

			/*
			 * grams solution per mL
			 */
			double solutionMassG = density;

			/*
			 * grams lactic acid per mL
			 */
			double acidMassG =
				solutionMassG * perc;

			/*
			 * MW lactic acid = 90.09 g/mol
			 */
			double moles =
				acidMassG / 90.09D;

			/*
			 * monoprotic
			 */
			return moles * 1000D;
		}
		else if (acid.getWaterAdditionFormula()
			== Misc.WaterAdditionFormula.PHOSPHORIC_ACID)
		{
			double density =
				1D
					+ 0.49D * perc
					+ 0.375D * perc * perc;

			double solutionMassG =
				density;

			double acidMassG =
				solutionMassG * perc;

			/*
			 * MW phosphoric acid = 98 g/mol
			 */
			double moles =
				acidMassG / 98D;

			/*
			 * effectively monoprotic in mash pH range
			 */
			return moles * 1000D;
		}

		return 0D;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the gravity change when a volume change occurs.
	 *
	 * @return New gravity of the output volume.
	 */
	public static DensityUnit calcGravityWithVolumeChange(
		VolumeUnit volumeIn,
		DensityUnit gravityIn,
		VolumeUnit volumeOut)
	{
		boolean estimated = volumeIn.isEstimated() || gravityIn.isEstimated() || volumeOut.isEstimated();

		return new DensityUnit(
			gravityIn.get() *
				volumeIn.get(MILLILITRES) /
				volumeOut.get(MILLILITRES),
			gravityIn.getUnit(),
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the gravity of the combined fluids. source:
	 * <p>
	 * Source:
	 * http://braukaiser.com/wiki/index.php/Batch_Sparge_and_Party_Gyle_Simulator
	 * <p>
	 * see also:
	 * https://www.quora.com/How-do-I-find-the-specific-gravity-when-two-liquids-are-mixed
	 *
	 * @return New gravity of the output volume.
	 */
	public static DensityUnit calcCombinedGravity(
		VolumeUnit v1,
		DensityUnit d1,
		VolumeUnit v2,
		DensityUnit d2)
	{
		boolean estimated = v1.isEstimated() || d1.isEstimated() || v2.isEstimated() || d2.isEstimated();

		double e1 = getExtractContent(v1, d1).get(KILOGRAMS);
		double e2 = getExtractContent(v2, d2).get(KILOGRAMS);

		double w1 = v1.get(LITRES) * d1.get(SPECIFIC_GRAVITY);
		double w2 = v2.get(LITRES) * d2.get(SPECIFIC_GRAVITY);

		double plato = 100 * (e1 + e2) / (w1 + w2);

		return new DensityUnit(plato, PLATO, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return the extract content of the given volume at the given gravity
	 */
	public static WeightUnit getExtractContent(VolumeUnit vol,
		DensityUnit gravity)
	{
		boolean estimated = vol.isEstimated() || gravity.isEstimated();

		double volL = vol.get(LITRES);

		double sg = gravity.get(SPECIFIC_GRAVITY);

		double plato = gravity.get(PLATO);

		double extract = volL * (sg) * (plato / 100D);

		return new WeightUnit(extract, KILOGRAMS, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the colour of the combined fluids. Source: I made this up
	 *
	 * @return New colour of the output volume.
	 */
	public static ColourUnit calcCombinedColour(
		VolumeUnit v1,
		ColourUnit c1,
		VolumeUnit v2,
		ColourUnit c2)
	{
		return (ColourUnit)calcCombinedLinearInterpolation(v1, c1, v2, c2);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Uses linear interpolation to calculate a general combined quantity. Tries
	 * its best to return the right quantity class. Source: I made this up
	 */
	public static Quantity calcCombinedLinearInterpolation(
		VolumeUnit v1,
		Quantity q1,
		VolumeUnit v2,
		Quantity q2)
	{
		if (v1 == null || q1 == null || v2 == null || q2 == null)
		{
			return null;
		}

		boolean estimated = v1.isEstimated() || q1.isEstimated() || v2.isEstimated() || q2.isEstimated();

		double vc = v1.get() + v2.get();
		double qc = (v1.get() / vc * q1.get()) + (v2.get() / vc * q2.get());

		Quantity result = Quantity.parseQuantity("" + qc, q1.getUnit());
		result.setEstimated(estimated);

		return result;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the bitterness of the combined fluids. Source: I made this up
	 *
	 * @return New bitterness of the combined volume.
	 */
	public static BitternessUnit calcCombinedBitterness(
		VolumeUnit v1,
		BitternessUnit b1,
		VolumeUnit v2,
		BitternessUnit b2)
	{
		return (BitternessUnit)calcCombinedLinearInterpolation(v1, b1, v2, b2);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the volume decrease due to cooling, due to evapouration.
	 *
	 * @return The new volume after shrinkage
	 */
	public static VolumeUnit calcCoolingShrinkage(
		VolumeUnit volumeIn,
		TemperatureUnit tempDecrease)
	{
		boolean estimated = volumeIn.isEstimated() || tempDecrease.isEstimated();

		return new VolumeUnit(
			volumeIn.get(MILLILITRES) *
				(1 - (Const.COOLING_SHRINKAGE * tempDecrease.get(CELSIUS))),
			MILLILITRES,
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the ABV change when a volume change occurs
	 *
	 * @return the new ABV
	 */
	public static PercentageUnit calcAbvWithVolumeChange(
		VolumeUnit volumeIn,
		PercentageUnit abvIn,
		VolumeUnit volumeOut)
	{
		if (volumeIn == null || abvIn == null || volumeOut == null)
		{
			return null;
		}
		boolean estimated = volumeIn.isEstimated() || abvIn.isEstimated() || volumeOut.isEstimated();

		double abvInD = abvIn == null ? 0 : abvIn.get();
		double volInD = volumeIn.get();
		double volOutD = volumeOut.get();
		return new PercentageUnit(abvInD * volInD / volOutD, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the ABV change when a gravity change occurs. Source:
	 * http://www.brewunited.com/abv_calculator.php
	 *
	 * @return the new ABV, expressed within 0..1
	 */
	public static PercentageUnit calcAbvWithGravityChange(
		DensityUnit gravityIn,
		DensityUnit gravityOut)
	{
		double abv = (gravityIn.get(SPECIFIC_GRAVITY) - gravityOut.get(SPECIFIC_GRAVITY)) * Const.ABV_CONST;
		boolean estimated = gravityIn.isEstimated() || gravityOut.isEstimated();
		return new PercentageUnit(abv / 100D, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the volume of the a new mash. Source:
	 * http://braukaiser.com/wiki/index.php/Batch_Sparge_and_Party_Gyle_Simulator
	 */
	public static VolumeUnit calcMashVolume(
		List<FermentableAddition> grainBill,
		VolumeUnit waterVolume,
		double conversionEfficiency)
	{
		boolean estimated = waterVolume.isEstimated();

		WeightUnit grainWeight = calcTotalGrainWeight(grainBill);

		// L/kg
		double apparentAbsorbtion = Const.GRAIN_WATER_ABSORPTION;

		// %
		double weightedYield = calcGrainBillWeightedYield(grainBill, grainWeight);

		// L/kg
		double trueAbsorptionRate = apparentAbsorbtion +
			(0.63D * conversionEfficiency * weightedYield);

		// kg
		double totalMashExtract = grainWeight.get(KILOGRAMS) * weightedYield;

		// L
		double volumeFromDisolvedExtract = 0.63D * totalMashExtract;

		// L
		double trueAbsorption = apparentAbsorbtion + volumeFromDisolvedExtract;

		// L
		double totalMashVol = waterVolume.get(LITRES) + volumeFromDisolvedExtract;

		return new VolumeUnit(totalMashVol, LITRES, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the max volume of wort that can be drained from a given mash.
	 * Note that this excludes the lauter loss. Source:
	 * http://braukaiser.com/wiki/index.php/Batch_Sparge_and_Party_Gyle_Simulator
	 */
	public static VolumeUnit calcWortVolume(
		List<FermentableAddition> grainBill,
		VolumeUnit waterVolume,
		double conversionEfficiency)
	{
		boolean estimated = waterVolume.isEstimated();

		WeightUnit grainWeight = calcTotalGrainWeight(grainBill);

		// L/kg
		double apparentAbsorbtion = Const.GRAIN_WATER_ABSORPTION;

		// %
		double weightedYield = calcGrainBillWeightedYield(grainBill, grainWeight);

		// L/kg
		double trueAbsorptionRate = apparentAbsorbtion +
			(0.63D * conversionEfficiency * weightedYield);

		// kg
		double totalMashExtract = grainWeight.get(KILOGRAMS) * weightedYield;

		// L
		double volumeFromDisolvedExtract = 0.63D * totalMashExtract;

		// L
		double trueAbsorption = trueAbsorptionRate * grainWeight.get(KILOGRAMS);

		// L
		double totalMashVol = waterVolume.get(LITRES) + volumeFromDisolvedExtract;

		double totalRunoffVol = totalMashVol - trueAbsorption;

		return new VolumeUnit(totalRunoffVol, LITRES, estimated);
	}

	/*-------------------------------------------------------------------------*/
	public static VolumeUnit calcWaterVolumeToAchieveMashVolume(
		List<FermentableAddition> grainBill,
		double conversionEfficiency,
		VolumeUnit targetMashVolume)
	{
		WeightUnit grainWeight = calcTotalGrainWeight(grainBill);
		VolumeUnit absorbedWater = calcAbsorbedWater(grainBill, conversionEfficiency);

		double waterDisplacement = grainWeight.get(GRAMS) * Const.GRAIN_WATER_DISPLACEMENT;
		boolean estimated = grainWeight.isEstimated();

		double waterVol =
			targetMashVolume.get(MILLILITRES) +
				absorbedWater.get(MILLILITRES) -
				waterDisplacement -
				grainWeight.get(GRAMS);

		return new VolumeUnit(
			waterVol,
			MILLILITRES,
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * http://braukaiser.com/wiki/index.php/Batch_Sparge_and_Party_Gyle_Simulator
	 *
	 * @return apparent volume of water absorbed in the grain
	 */
	public static VolumeUnit calcAbsorbedWater(
		List<FermentableAddition> grainBill,
		double conversionEfficiency)
	{
		WeightUnit grainWeight = calcTotalGrainWeight(grainBill);

		// L/kg
		double apparentAbsorbtion = Const.GRAIN_WATER_ABSORPTION;

		// %
		double weightedYield = calcGrainBillWeightedYield(grainBill, grainWeight);

		// L/kg
		double trueAbsorptionRate = apparentAbsorbtion +
			(0.63D * conversionEfficiency * weightedYield);

		// L
		double trueAbsorption = trueAbsorptionRate * grainWeight.get(KILOGRAMS);

		return new VolumeUnit(trueAbsorption, LITRES, true);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the SRM of the output wort using the Morey formula. Source:
	 * http://brewwiki.com/index.php/Estimating_Color
	 *
	 * @param waterVolume in ml
	 * @return wort colour in SRM
	 */
	public static ColourUnit calcColourSrmMoreyFormula(
		List<FermentableAddition> grainBill,
		VolumeUnit waterVolume)
	{
		if (grainBill.isEmpty())
		{
			return new ColourUnit(0D, SRM, false);
		}

		// calc malt colour units
		double mcu = 0D;
		for (FermentableAddition fa : grainBill)
		{
			Fermentable f = fa.getFermentable();

			double colour = f.getColour().get(SRM); // I think this was imported as Lovibond?
			double weight = switch (f.getType().getQuantityType())
			{
				case WEIGHT -> fa.getQuantity().get(POUNDS);
				case VOLUME ->
					new WeightUnit(fa.getQuantity().get(MILLILITRES), GRAMS).get(POUNDS); // bit of a hack this
				default ->
					throw new BrewdayException("invalid unit type " + f.getType().getQuantityType());
			};

			mcu += (colour * weight);
		}

		mcu /= waterVolume.get(US_GALLON);

		// apply Dan Morey's formula
		return new ColourUnit(1.499D * (Math.pow(mcu, 0.6859D)), SRM, true);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the colour impact of a boil.
	 *
	 * @param colourIn
	 * @return
	 */
	public static ColourUnit calcColourAfterBoil(ColourUnit colourIn)
	{
		//
		// Brewday has an issue with colour calculations: existing formulae (eg
		// Morey) require the use of MCUs based on post-boil gravity.
		// (source: http://www.beersmith.com/forum/index.php?topic=5797.0)
		// But Brewday can't easily do that because the process steps are
		// decoupled and there isn't necessarily a 1:1 mapping from mash to boil.
		//
		// One option would be passing MCUs around as a metric in the volumes,
		// waiting to arrive at a post-boil volume. I doubt this would work
		// properly and haven't tried it yet.
		//
		// Instead I'm doing this: the typical homebrew process produces a post-boil
		// volume about 60% of the input water. Working out a table of SRM values
		// shows me that the SRM output is 42% higher when the MCU's are worked
		// out with 60% of the water volume.
		// So to model this in Brewday at boil time we increase the SRM by 42%.
		//
		// This is kinda wacky I admit. But to quote Palmer, there are "inherent
		// limits of any model for beer colour" so I guess it's best to be a bit
		// relaxed about this stuff.
		//

		double srmIn = colourIn.get(SRM);
		return new ColourUnit(srmIn * 1.42, SRM);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param volumeIn  in ml, assumed SRM of 0
	 * @param colourIn  in SRM
	 * @param volumeOut in ml
	 * @return colour in SRM
	 */
	public static ColourUnit calcColourWithVolumeChange(
		VolumeUnit volumeIn,
		ColourUnit colourIn,
		VolumeUnit volumeOut)
	{
		boolean estimated = volumeIn.isEstimated() || colourIn.isEstimated() || volumeOut.isEstimated();

		return new ColourUnit(colourIn.get(SRM) *
			volumeIn.get(MILLILITRES) /
			volumeOut.get(MILLILITRES),
			SRM,
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param volumeIn assumed IBU of 0
	 */
	public static BitternessUnit calcBitternessWithVolumeChange(
		VolumeUnit volumeIn,
		BitternessUnit bitternessIn,
		VolumeUnit volumeOut)
	{
		if (bitternessIn == null)
		{
			return new BitternessUnit(0);
		}

		boolean estimated = volumeIn.isEstimated() || bitternessIn.isEstimated() || volumeOut.isEstimated();

		return new BitternessUnit(
			bitternessIn.get(IBU) *
				volumeIn.get(MILLILITRES) /
				volumeOut.get(MILLILITRES),
			IBU,
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param colour in SRM
	 * @return colour after fermentation, in SRM
	 */
	public static ColourUnit calcColourAfterFermentation(ColourUnit colour)
	{
		return new ColourUnit(
			colour.get(SRM) * (1 - Const.COLOUR_LOSS_DURING_FERMENTATION),
			SRM,
			colour.isEstimated());
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Estimates iso-alpha acid mass remaining after fermentation.
	 *
	 * <p>Fermentation reduces iso-alpha acid concentration through several
	 * mechanisms including:</p>
	 *
	 * <ul>
	 *     <li>adsorption onto yeast cell walls</li>
	 *     <li>association with hot/cold break material</li>
	 *     <li>precipitation during clarification and maturation</li>
	 *     <li>transfer losses with sedimented material</li>
	 * </ul>
	 *
	 * <p>This implementation applies a simple empirical retention factor to
	 * represent aggregate post-kettle losses during fermentation.</p>
	 *
	 * <p>Typical literature-reported retention ranges are approximately
	 * 60–90% depending on yeast strain, flocculation, tank geometry,
	 * hopping rate, and clarification regime.</p>
	 *
	 * <p>References:</p>
	 *
	 * <ul>
	 *     <li>Kunze, W. - Technology Brewing and Malting</li>
	 *     <li>Maye et al. - MBAA Technical Quarterly hop bitterness studies</li>
	 *     <li>Shellhammer lab bitterness stability work</li>
	 * </ul>
	 *
	 * @param isoAlpha iso-alpha acid mass entering fermentation, in mg
	 * @return estimated iso-alpha acid mass remaining after fermentation, in mg
	 */
	public static WeightUnit calcIsoAlphaAfterFermentation(
		WeightUnit isoAlpha)
	{
		double retainedMg =
			isoAlpha.get(Quantity.Unit.MILLIGRAMS) *
				Const.ISO_ALPHA_RETENTION_DURING_FERMENTATION;

		return new WeightUnit(
			retainedMg,
			Quantity.Unit.MILLIGRAMS,
			isoAlpha.isEstimated());
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: http://www.realbeer.com/hops/research.html
	 */
	public static BitternessUnit calcIbuTinseth(
		HopAddition hopAddition,
		TimeUnit steepDuration,
		DensityUnit wortGravity,
		VolumeUnit wortVolume,
		double equipmentUtilisation)
	{
		boolean estimated = wortGravity.isEstimated() || wortVolume.isEstimated();

		double decimalAAUtilisation = calcTinsethDecimalUtilisation(steepDuration, wortGravity);

		Hop h = hopAddition.getHop();
		double alpha = h.getAlphaAcid().get(PERCENTAGE);
		double weight = hopAddition.getQuantity().get(GRAMS);

		double mgPerL = (alpha * weight * 1000) / (wortVolume.get(LITRES));

		BitternessUnit tinsethResult = new BitternessUnit(
			(mgPerL * decimalAAUtilisation) * equipmentUtilisation,
			IBU,
			estimated);

		// Tinseth's experiments were done with leaf hops
		double multiplier = getHopFormMultiplier(Hop.Form.LEAF, hopAddition.getHop().getForm());

		return new BitternessUnit(tinsethResult.get(IBU) * multiplier, IBU);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return Total alpha acid mass from a hop addition, in milligrams.
	 */
	public static WeightUnit calcHopAlphaAcidsMg(HopAddition hopAddition)
	{
		boolean estimated = hopAddition.getQuantity().isEstimated();
		if (hopAddition.getHop().getAlphaAcid() != null)
		{
			estimated = estimated || hopAddition.getHop().getAlphaAcid().isEstimated();
		}

		double alpha = hopAddition.getHop().getAlphaAcid().get(PERCENTAGE);
		double weightG = hopAddition.getQuantity().get(GRAMS);
		double mg = alpha * weightG * 1000;

		return new WeightUnit(mg, MILLIGRAMS, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return Isomerized alpha acid mass from Tinseth utilisation, in
	 * milligrams.
	 */
	public static WeightUnit calcHopIsoAlphaAcidsMgTinseth(
		HopAddition hopAddition,
		TimeUnit steepDuration,
		DensityUnit wortGravity,
		VolumeUnit wortVolume,
		double equipmentUtilisation)
	{
		BitternessUnit ibu = calcIbuTinseth(
			hopAddition,
			steepDuration,
			wortGravity,
			wortVolume,
			equipmentUtilisation);

		double isoMg = ibu.get(IBU) * wortVolume.get(LITRES);

		return new WeightUnit(isoMg, MILLIGRAMS, ibu.isEstimated());
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return Isomerized alpha acid mass implied by an IBU contribution, in
	 * milligrams.
	 */
	public static WeightUnit calcIsoAlphaAcidsMgFromIbu(
		BitternessUnit ibu,
		VolumeUnit wortVolume)
	{
		if (ibu == null || wortVolume == null)
		{
			return new WeightUnit(0, MILLIGRAMS);
		}

		boolean estimated = ibu.isEstimated() || wortVolume.isEstimated();
		double isoMg = ibu.get(IBU) * wortVolume.get(LITRES);

		return new WeightUnit(isoMg, MILLIGRAMS, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Brewday bitterness model: IBU is iso-alpha mass per litre of wort.
	 */
	public static BitternessUnit calcBrewdayIbu(
		WeightUnit isoAlphaAcidsMg,
		VolumeUnit volume)
	{
		if (isoAlphaAcidsMg == null || volume == null)
		{
			return new BitternessUnit(0);
		}

		double litres = volume.get(LITRES);
		if (litres <= 0)
		{
			return new BitternessUnit(0);
		}

		boolean estimated = isoAlphaAcidsMg.isEstimated() || volume.isEstimated();

		return new BitternessUnit(
			isoAlphaAcidsMg.get(MILLIGRAMS) / litres,
			IBU,
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	private static double calcTinsethDecimalUtilisation(
		TimeUnit steepDuration,
		DensityUnit wortGravity)
	{
		double aveGrav = wortGravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY);

		double maxUtilFactor = Double.valueOf(Database.getInstance().getSettings().get(
			Settings.TINSETH_MAX_UTILISATION));
		double bignessFactor = 1.65D * Math.pow(0.000125, aveGrav - 1);
		double boilTimeFactor = (1D - Math.exp(-0.04 * steepDuration.get(MINUTES))) / maxUtilFactor;

		return bignessFactor * boilTimeFactor;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param baseForm The base hop form for which the IBU formula does not
	 *                 adjust IBUs. This is typically LEAF (e.g. Tinseth) or
	 *                 PELLET (e.g. Rager)
	 * @param form     The hop form in use
	 * @return
	 */
	public static double getHopFormMultiplier(Hop.Form baseForm, Hop.Form form)
	{
		double multiplier = 1D;
		double base = 0D;

		Settings settings = Database.getInstance().getSettings();

		switch (baseForm)
		{
			case PELLET:
				base += Double.valueOf(settings.get(Settings.PELLET_HOP_ADJUSTMENT));
				break;
			case PLUG:
				base += Double.valueOf(settings.get(Settings.PLUG_HOP_ADJUSTMENT));
				break;
			case LEAF:
				base += Double.valueOf(settings.get(Settings.LEAF_HOP_ADJUSTMENT));
				break;
		}

		switch (form)
		{
			case PELLET:
				multiplier += Double.valueOf(settings.get(Settings.PELLET_HOP_ADJUSTMENT));
				break;
			case PLUG:
				multiplier += Double.valueOf(settings.get(Settings.PLUG_HOP_ADJUSTMENT));
				break;
			case LEAF:
				multiplier += Double.valueOf(settings.get(Settings.LEAF_HOP_ADJUSTMENT));
				break;
		}

		return multiplier - base;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: https://www.realbeer.com/hops/FAQ.html#units
	 */
	public static BitternessUnit calcIbuRager(
		HopAddition hopAddition,
		TimeUnit steepDuration,
		DensityUnit wortGravity,
		VolumeUnit wortVolume,
		double equipmentUtilisation)
	{
		boolean estimated = wortGravity.isEstimated() || wortVolume.isEstimated();

		double weightG = hopAddition.getQuantity().get(GRAMS);
		double minutes = steepDuration.get(MINUTES);
		double alpha = hopAddition.getHop().getAlphaAcid().get(PERCENTAGE);
		double volumeL = wortVolume.get(LITRES);

		double ga = Math.max(0, wortGravity.get(SPECIFIC_GRAVITY) - 1.050) * 0.2D;

		double utilisation = (18.11 + 13.86 * Math.tanh((minutes - 31.32) / 18.27)) / 100;

		double ibu = (weightG * utilisation * alpha * 1000) / (volumeL * (1 + ga));

		// Rager's numbers are believed to be for pellet hops.
		double multiplier = getHopFormMultiplier(
			Hop.Form.PELLET, hopAddition.getHop().getForm());

		return new BitternessUnit(ibu * equipmentUtilisation * multiplier,
			IBU, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: https://www.realbeer.com/hops/FAQ.html#units
	 */
	public static BitternessUnit calcIbuGaretz(
		HopAddition hopAddition,
		TimeUnit steepDuration,
		DensityUnit wortGravity,
		VolumeUnit finalVol,
		VolumeUnit boilVol,
		double equipmentUtilisation,
		double equipmentElevationInFeet)
	{
		// WTF Garetz, I need to estimate the IBUs?
		// Luckily there are some other handy ways of doing that...
		BitternessUnit est = calcIbuTinseth(hopAddition, steepDuration, wortGravity, boilVol, equipmentUtilisation);

		// iterate to refine the estimate
		for (int i = 0; i < 5; i++)
		{
			est = calcIbuGaretzInternal(
				hopAddition,
				steepDuration,
				wortGravity,
				finalVol,
				boilVol,
				est,
				equipmentUtilisation,
				equipmentElevationInFeet);
		}

		return est;
	}

	/*-------------------------------------------------------------------------*/
	private static BitternessUnit calcIbuGaretzInternal(
		HopAddition hopAddition,
		TimeUnit steepDuration,
		DensityUnit wortGravity,
		VolumeUnit finalVol,
		VolumeUnit boilVol,
		BitternessUnit desiredIbu,
		double equipmentUtilisation,
		double equipmentElevationInFeet)
	{
		boolean estimated = wortGravity.isEstimated() || boilVol.isEstimated();

		double startingGrav = wortGravity.get(SPECIFIC_GRAVITY);
		double mins = steepDuration.get(MINUTES);
		// Garetz needs whole-number percentages, wtf?
		double alpha = hopAddition.getHop().getAlphaAcid().get(PERCENTAGE_DISPLAY);
		double grams = hopAddition.getQuantity().get(GRAMS);
		double litres = boilVol.get(LITRES);

		// from here: https://straighttothepint.com/ibu-calculator/
		double utilisation = 7.2994 + 15.0746 * Math.tanh((mins - 21.86) / 24.71);

		// concentration factor
		double cf = finalVol.get() / boilVol.get();

		// boil gravity
		double bg = 1 + (cf * (startingGrav - 1));

		// gravity factor
		double gf = (bg - 1.050) / 0.2 + 1;

		// hopping rate factor
		double hf = 1 + ((cf * desiredIbu.get(IBU)) / 260);

		// temp factor
		double tf = 1 + equipmentElevationInFeet / 550 * 0.02;

		// yeast factor, pellet factor, bag factor, filter factor
		Settings settings = Database.getInstance().getSettings();
		double yf = Double.valueOf(settings.get(Settings.GARETZ_YEAST_FACTOR));
		double pf = Double.valueOf(settings.get(Settings.GARETZ_PELLET_FACTOR));
		double bf = Double.valueOf(settings.get(Settings.GARETZ_BAG_FACTOR));
		double ff = Double.valueOf(settings.get(Settings.GARETZ_FILTER_FACTOR));

		// combined adjustments
		double ca = gf * hf * tf * yf * pf * bf * ff;

		double ibu = (utilisation * alpha * grams * 0.1) / (litres * ca);

		// Garetz does not modify upwards for pellets so we assume that as the base
		double mult = getHopFormMultiplier(Hop.Form.PELLET, hopAddition.getHop().getForm());

		return new BitternessUnit(ibu * equipmentUtilisation * mult,
			IBU, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * https://straighttothepint.com/ibu-calculator/
	 */
	public static BitternessUnit calcIbuDaniels(
		HopAddition hopAddition,
		TimeUnit steepDuration,
		DensityUnit wortGravity,
		VolumeUnit wortVolume,
		double equipmentUtilisation)
	{
		boolean estimated = wortGravity.isEstimated() || wortVolume.isEstimated();

		// per the source, we are using the Tinseth utilisation formula here.
		// Daniels uses a table in his book but the source of the data is unclear.
		double aveGrav = wortGravity.get(DensityUnit.Unit.SPECIFIC_GRAVITY);

		// the TINSETH utilisation formula
		double bignessFactor = 1.65D * Math.pow(0.000125, aveGrav - 1);
		double boilTimeFactor = (1D - Math.exp(-0.04 * steepDuration.get(MINUTES))) / 4.15D;
		double utilisation = bignessFactor * boilTimeFactor;

		// daniels formula:

		double alpha = hopAddition.getHop().getAlphaAcid().get(PERCENTAGE);
		double weightOz = hopAddition.getQuantity().get(OUNCES);
		double volGal = wortVolume.get(US_GALLON);

		double ibu = utilisation * alpha * weightOz * 7489 / volGal;

		// Daniels adjusts upwards for pellets so we assume LEAF as the base
		double mult = getHopFormMultiplier(Hop.Form.LEAF, hopAddition.getHop().getForm());

		return new BitternessUnit(ibu * equipmentUtilisation * mult, IBU, estimated);
	}

	/*-------------------------------------------------------------------------*/

	public static TemperatureUnit calcStandEndingTemperature(
		TemperatureUnit inputTemp,
		TimeUnit standDuration)
	{
		double inC = inputTemp.get(CELSIUS);
		double lossC = Const.HEAT_LOSS * standDuration.get(HOURS);
		return new TemperatureUnit(inC - lossC, CELSIUS);
	}


	/*-------------------------------------------------------------------------*/

	/**
	 * mIBU kettle shape: height = this factor × diameter (cylindrical
	 * estimate).
	 */
	private static final double MIBU_KETTLE_HEIGHT_TO_DIAMETER = 1.2D;

	private static final double MIBU_INTEGRATION_STEP_MINUTES = 0.001D;

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * https://alchemyoverlord.wordpress.com/2015/05/12/a-modified-ibu-measurement-especially-for-late-hopping/
	 */
	public static BitternessUnit calcIbuMibu(
		HopAddition hopAddition,
		TimeUnit boilTime,
		TimeUnit coolTime,
		DensityUnit wortGravity,
		VolumeUnit wortVolume,
		double kettleDiameterCm,
		double openingDiameterCm,
		double equipmentUtilisation)
	{
		double boilUtil = computeMibuBoilUtilization(
			wortGravity.get(SPECIFIC_GRAVITY),
			boilTime.get(MINUTES));
		double postBoilUtil = computeMibuPostBoilUtilization(
			wortGravity.get(SPECIFIC_GRAVITY),
			boilTime.get(MINUTES),
			coolTime.get(MINUTES),
			wortVolume.get(LITRES),
			kettleDiameterCm,
			openingDiameterCm);

		return calcIbuFromMibuUtilization(
			hopAddition,
			boilUtil + postBoilUtil,
			wortGravity,
			wortVolume,
			equipmentUtilisation);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Post-flameout portion of mIBU only (for hop-stand / whirlpool steps).
	 */
	public static BitternessUnit calcIbuMibuPostBoil(
		HopAddition hopAddition,
		TimeUnit boilTime,
		TimeUnit coolTime,
		DensityUnit wortGravity,
		VolumeUnit wortVolume,
		double kettleDiameterCm,
		double openingDiameterCm,
		double equipmentUtilisation)
	{
		double postBoilUtil = computeMibuPostBoilUtilization(
			wortGravity.get(SPECIFIC_GRAVITY),
			boilTime.get(MINUTES),
			coolTime.get(MINUTES),
			wortVolume.get(LITRES),
			kettleDiameterCm,
			openingDiameterCm);

		return calcIbuFromMibuUtilization(
			hopAddition,
			postBoilUtil,
			wortGravity,
			wortVolume,
			equipmentUtilisation);
	}

	/*-------------------------------------------------------------------------*/

	private static BitternessUnit calcIbuFromMibuUtilization(
		HopAddition hopAddition,
		double decimalAAUtilisation,
		DensityUnit wortGravity,
		VolumeUnit wortVolume,
		double equipmentUtilisation)
	{
		boolean estimated = wortGravity.isEstimated() || wortVolume.isEstimated();

		double alpha = hopAddition.getHop().getAlphaAcid().get(PERCENTAGE);
		double weight = hopAddition.getQuantity().get(GRAMS);
		double mgPerL = (alpha * weight * 1000) / wortVolume.get(LITRES);

		double multiplier = getHopFormMultiplier(Hop.Form.LEAF, hopAddition.getHop().getForm());

		return new BitternessUnit(
			mgPerL * decimalAAUtilisation * equipmentUtilisation * multiplier,
			IBU,
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	private static double getTinsethMaxUtilFactor()
	{
		return Double.valueOf(Database.getInstance().getSettings().get(
			Settings.TINSETH_MAX_UTILISATION));
	}

	/*-------------------------------------------------------------------------*/

	private static double computeMibuBoilUtilization(double boilGravity,
		double boilTimeMin)
	{
		double maxUtilFactor = getTinsethMaxUtilFactor();
		double bignessFactor = 1.65D * Math.pow(0.000125, boilGravity - 1);
		double boilTimeFactor = (1D - Math.exp(-0.04 * boilTimeMin)) / maxUtilFactor;
		return bignessFactor * boilTimeFactor;
	}

	/*-------------------------------------------------------------------------*/

	private static double computeMibuInstantaneousUtilization(double boilGravity,
		double t)
	{
		double maxUtilFactor = getTinsethMaxUtilFactor();
		return 1.65D * Math.pow(0.000125, boilGravity - 1) * 0.04 * Math.exp(-0.04 * t) / maxUtilFactor;
	}

	/*-------------------------------------------------------------------------*/

	private static double computeMibuPostBoilUtilization(
		double boilGravity,
		double boilTimeMin,
		double coolTimeMin,
		double volumeLiters,
		double kettleDiameterCm,
		double openingDiameterCm)
	{
		if (coolTimeMin <= 0)
		{
			return 0D;
		}

		double decimalAArating = 0D;
		double endT = boilTimeMin + coolTimeMin;

		for (double t = boilTimeMin; t < endT; t += MIBU_INTEGRATION_STEP_MINUTES)
		{
			double dU = computeMibuInstantaneousUtilization(boilGravity, t);
			double t2 = t - boilTimeMin;
			double tempK = calcWortTempKelvinAfterFlameout(
				t2, volumeLiters, kettleDiameterCm, openingDiameterCm);
			double degreeOfUtilization = calcRelativeUtilizationAtTempKelvin(tempK);

			if (t < 5.0)
			{
				degreeOfUtilization = 1.0;
			}

			decimalAArating += dU * degreeOfUtilization * MIBU_INTEGRATION_STEP_MINUTES;
		}

		return decimalAArating;
	}

	/*-------------------------------------------------------------------------*/

	private static double calcWortTempKelvinAfterFlameout(
		double timeAfterFlameoutMin,
		double volumeLiters,
		double kettleDiameterCm,
		double openingDiameterCm)
	{
		double radiusCm = kettleDiameterCm / 2.0;
		double surfaceAreaCm2 = Math.PI * radiusCm * radiusCm;
		double openingRadiusCm = openingDiameterCm / 2.0;
		double openingAreaCm2 = Math.PI * openingRadiusCm * openingRadiusCm;
		double effectiveAreaCm2 = Math.sqrt(surfaceAreaCm2 * openingAreaCm2);
		double b = (0.0002925 * effectiveAreaCm2 / volumeLiters) + 0.00538;
		return 53.70 * Math.exp(-b * timeAfterFlameoutMin) + 319.55;
	}

	/*-------------------------------------------------------------------------*/

	private static double calcRelativeUtilizationAtTempKelvin(double tempK)
	{
		return 2.39E11 * Math.exp(-9773.0 / tempK);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Estimate internal kettle diameter (cm) from volume when not configured.
	 * Assumes a cylinder with height = {@link #MIBU_KETTLE_HEIGHT_TO_DIAMETER} ×
	 * diameter.
	 */
	public static double estimateBoilKettleDiameterCm(
		VolumeUnit boilKettleVolume)
	{
		// V_litres = (pi/4) * (d_cm/100)^2 * (height/d) * d with height = 1.2*d
		// => V_litres = 300 * pi * (d_cm/100)^3
		double volumeLiters = boilKettleVolume.get(LITRES);
		double dMeters = Math.cbrt(volumeLiters / (MIBU_KETTLE_HEIGHT_TO_DIAMETER * 1000.0 * Math.PI));
		return dMeters * 100.0;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * https://alchemyoverlord.wordpress.com/2015/05/12/a-modified-ibu-measurement-especially-for-late-hopping/
	 * <p>
	 * Legacy hop-stand IBU estimate (non-mIBU formulas). Uses a simplified fixed
	 * end-temperature model rather than time-varying kettle cooling.
	 *
	 * @return The IBU added by a given post-boil hop stand.
	 */
	public static BitternessUnit calcHopStandIbu(
		List<HopAddition> hopAdditions,
		DensityUnit wortDensity,
		VolumeUnit wortVolume,
		TimeUnit boilTime,
		TimeUnit coolTime)
	{
		double hopStandUtilization;

		double integrationTime = MIBU_INTEGRATION_STEP_MINUTES;
		double decimalAArating = 0.0;

		double boilMin = boilTime.get(MINUTES);
		double coolMin = coolTime.get(MINUTES);
		double boilGravity = wortDensity.get(SPECIFIC_GRAVITY);

		for (double t = boilMin; t < boilMin + coolMin; t = t + integrationTime)
		{
			double dU = computeMibuInstantaneousUtilization(boilGravity, t);

			TemperatureUnit endTemp = calcStandEndingTemperature(new TemperatureUnit(100, CELSIUS), coolTime);
			double tempK = endTemp.get(KELVIN);

			double degreeOfUtilization = calcRelativeUtilizationAtTempKelvin(tempK);

			if (t < 5.0)
			{
				degreeOfUtilization = 1.0;
			}

			double combinedValue = dU * degreeOfUtilization;
			decimalAArating = decimalAArating + (combinedValue * integrationTime);
		}

		hopStandUtilization = decimalAArating;

		BitternessUnit bitternessOut = new BitternessUnit(0);
		for (IngredientAddition hopCharge : hopAdditions)
		{
			bitternessOut.add(
				Equations.calcIbuTinseth(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					wortDensity,
					wortVolume,
					hopStandUtilization));
		}

		return new BitternessUnit(bitternessOut.get());
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * https://sciencing.com/calculate-tons-cooling-cooling-tower-10058467.html
	 */
	public static TimeUnit calcHeatingTime(
		VolumeUnit volume,
		TemperatureUnit startTemp,
		TemperatureUnit endTemp,
		PowerUnit heatingPower)
	{
		double td = endTemp.get(CELSIUS) - startTemp.get(CELSIUS);

		double kWh = (4.2 * volume.get(LITRES) * td) / 3600;

		return new TimeUnit(
			kWh / heatingPower.get(KILOWATT),
			HOURS,
			true);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Given grain and water, returns the resultant mash temp. Source:
	 * http://howtobrew.com/book/section-3/the-methods-of-mashing/calculations-for-boiling-water-additions
	 * (rearranged the terms)
	 *
	 * @return mash temp
	 */
	public static TemperatureUnit calcMashTemp(
		WeightUnit totalGrainWeight,
		WaterAddition strikeWater,
		TemperatureUnit grainTemp)
	{
		// ratio water to grain
		double r = strikeWater.getQuantity().get(MILLILITRES) /
			totalGrainWeight.get(GRAMS);

		TemperatureUnit tw = strikeWater.getTemperature();

		double c = Const.MASH_TEMP_THERMO_CONST;

		boolean estimated = totalGrainWeight.isEstimated() || grainTemp.isEstimated();

		return new TemperatureUnit(
			(c * grainTemp.get(CELSIUS)
				+ r * tw.get(CELSIUS))
				/ (c + r),
			CELSIUS,
			estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 *
	 */
	public static TemperatureUnit calcWaterTemp(
		WeightUnit totalGrainWeight,
		WaterAddition strikeWater,
		TemperatureUnit grainTemp,
		TemperatureUnit targetMashTemp)
	{
		boolean estimated = totalGrainWeight.isEstimated() || grainTemp.isEstimated();

		// ratio water to grain
		double r = strikeWater.getQuantity().get(MILLILITRES) /
			totalGrainWeight.get(GRAMS);

		double tg = grainTemp.get(CELSIUS);

		double c = Const.MASH_TEMP_THERMO_CONST;


		double tt = targetMashTemp.get(CELSIUS);

		double tw = (tt * (c + r) - c * tg) / r;

		return new TemperatureUnit(tw, CELSIUS, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * http://braukaiser.com/wiki/index.php/Effects_of_mash_parameters_on_fermentability_and_efficiency_in_single_infusion_mashing
	 *
	 * @param mashTemp The average mash temperature
	 * @return The estimated attenuation limit of the wort produced
	 */
	public static PercentageUnit getWortAttenuationLimit(
		TemperatureUnit mashTemp)
	{
		// per Braukaiser:
		// for mash temp >= 67.5C we model a line A = 0.9 - 0.04*(T - 67.5)
		// for mash temp < 67.5 we model a line A = 0.9 - 0.015*(67.5-T)

		double result;
		double tempC = mashTemp.get(CELSIUS);

		if (tempC >= 67.5)
		{
			result = 0.9 - 0.04 * (tempC - 67.5);
		}
		else
		{
			result = 0.9 - 0.015 * (67.5 - tempC);
		}

		return new PercentageUnit(result, true);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return Estimated apparent attenuation, in %
	 * @deprecated Use {@link mclachlan.brewday.process.FermentationCalculator}
	 * for multi-culture fermentation; retained for legacy comparisons.
	 */
	@Deprecated
	public static double calcEstimatedAttenuation(Volume inputWort,
		YeastAddition yeastAddition)
	{
		if (yeastAddition == null)
		{
			return 0D;
		}

		PercentageUnit wortAttenuationLimit = inputWort.getFermentability();
		if (wortAttenuationLimit == null)
		{
			wortAttenuationLimit = new PercentageUnit(0.9D);
		}

		Yeast yeast = yeastAddition.getYeast();
		double yeastAttenuation = yeast.getAttenuation() == null
			? 0.75D
			: yeast.getAttenuation().get(PERCENTAGE);
		double wortAttenuation = wortAttenuationLimit.get(PERCENTAGE);

		if (wortAttenuation < yeastAttenuation)
		{
			return wortAttenuation + (yeastAttenuation - wortAttenuation) / 2;
		}
		return yeastAttenuation + (wortAttenuation - yeastAttenuation) / 2;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates mash gravity using the extract points / ppg method to derive
	 * SG.
	 * <p>
	 * Source:
	 * https://byo.com/article/hitting-target-original-gravity-and-volume-advanced-homebrewing/
	 * See also:
	 * http://beersmith.com/blog/2015/01/30/calculating-original-gravity-for-beer-recipe-design/
	 */
	public static DensityUnit calcMashExtractContentFromPppg(
		List<FermentableAddition> grainBill,
		double mashEfficiency,
		VolumeUnit volumeOut)
	{
		double extractPoints = 0D;
		for (FermentableAddition fa : grainBill)
		{
			if (fa.getFermentable().getType().getQuantityType() != Quantity.Type.VOLUME)
			{
				PercentageUnit yield = fa.getFermentable().getYield();
				double pppg = calcExtractPotentialFromYield(yield);
				extractPoints += fa.getQuantity().get(POUNDS) * pppg;
			}
		}

		double actualExtract = extractPoints * mashEfficiency;

		double gal = volumeOut.get(US_GALLON);
		return new DensityUnit(actualExtract / gal);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * http://braukaiser.com/wiki/index.php/Batch_Sparge_and_Party_Gyle_Simulator
	 */
	public static DensityUnit getSpargeRunningGravity(
		WaterAddition spargeWater,
		DensityUnit mashGravity,
		VolumeUnit mashVolume)
	{
		// L
		double mashVol = mashVolume.get(LITRES);
		double mashPlato = mashGravity.get(PLATO);
		double mashSG = mashGravity.get(SPECIFIC_GRAVITY);

		// plato = g/100g
		// in kg:
		double extractRemainingInMash = mashVol * (mashSG) * (mashPlato / 100D);

		// Braukaiser's spreadsheet does this differently, but this is easier with the
		// info available here: work out the water remaining by subtracting the
		// extract volume increase
		// L
		double waterRemainingInMash = mashVol - (extractRemainingInMash * 0.63D);

		// L
		double totalWaterInMash = spargeWater.getQuantity().get(LITRES) + waterRemainingInMash;

		// P
		double newWortGravity = 100 * extractRemainingInMash / (extractRemainingInMash + totalWaterInMash);

		return new DensityUnit(newWortGravity, PLATO);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates mash gravity using the grain yield to derive degrees Plato
	 * <p>
	 * Source: http://braukaiser.com/wiki/index.php/Understanding_Efficiency
	 */
	public static DensityUnit calcMashExtractContentFromYield(
		List<FermentableAddition> grainBill,
		double conversionEfficiency,
		WaterAddition mashWater)
	{
		WeightUnit totalGrainWeight = calcTotalGrainWeight(grainBill);

		// mash water-to-grain ratio in l/kg
//		double r = (mashWater.getVolume().get(LITRES)) /
//			totalGrainWeight.get(KILOGRAMS);

		double weightedE = 0D;
		double mGrain = totalGrainWeight.get(KILOGRAMS);
		double vWater = mashWater.getQuantity().get(LITRES);

		double result = 0D;

		weightedE = calcGrainBillWeightedYield(grainBill, totalGrainWeight);

		result = conversionEfficiency * 100 * (mGrain * weightedE) / (vWater + mGrain * weightedE);

		return new DensityUnit(result, DensityUnit.Unit.PLATO, true);
	}

	/*-------------------------------------------------------------------------*/
	protected static double calcGrainBillWeightedYield(
		List<FermentableAddition> grainBill, WeightUnit totalGrainWeight)
	{
		double weightedE = 0;

		for (FermentableAddition fa : grainBill)
		{
			Fermentable fermentable = fa.getFermentable();

			if (fermentable.getType().getQuantityType() != Quantity.Type.VOLUME)
			{
				double yield = fermentable.getYield().get(PERCENTAGE);
				double moisture = fermentable.getMoisture().get(PERCENTAGE);

				double actualYield = yield * (1 - moisture);

				double proportion = fa.getQuantity().get(GRAMS) /
					totalGrainWeight.get(GRAMS);

				weightedE += (actualYield * proportion);
			}
		}
		return weightedE;
	}

	/*-------------------------------------------------------------------------*/
	public static WeightUnit calcTotalGrainWeight(
		List<FermentableAddition> grainBill)
	{
		double result = 0D;
		for (FermentableAddition item : grainBill)
		{
			// ignore liquid mash additions for this calculation
			if (item.getFermentable().getType().getQuantityType() != Quantity.Type.VOLUME)
			{
				result += item.getQuantity().get(GRAMS);
			}
		}
		return new WeightUnit(result, GRAMS, false);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the gravity returned by steeping the given grains. Source:
	 * Beersmith
	 */
	public static DensityUnit calcSteepedGrainsGravity(
		List<FermentableAddition> grainBill,
		VolumeUnit volumeOut)
	{
		// treat a steep like a mash with 15% efficiency
		return calcMashExtractContentFromPppg(grainBill, 0.15D, volumeOut);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the gravity provided by just dissolving the given fermentable
	 * in the given volume of fluid.
	 * <p>
	 * Source:
	 * http://braukaiser.com/wiki/index.php/Troubleshooting_Brewhouse_Efficiency
	 *
	 * @return The additional gravity
	 */
	public static DensityUnit calcSteepedFermentableAdditionGravity(
		FermentableAddition fermentableAddition,
		VolumeUnit volume)
	{
		Fermentable fermentable = fermentableAddition.getFermentable();
		Fermentable.Type type = fermentable.getType();

		double pppg = calcExtractPotentialFromYield(fermentable.getYield());

		if (type == Fermentable.Type.GRAIN || type == Fermentable.Type.ADJUNCT)
		{
			// these are not soluble
			// however if these are grains with a diastatic power of 0 then we expect some
			// gravit from steeping them

			if (type == Fermentable.Type.GRAIN && fermentable.getDiastaticPower().get() <= 0)
			{
				// drawing a curve from the data here: http://howtobrew.com/book/section-2/steeping-specialty-grains/mechanics-of-steeping
				// we estimate the ppg from the yield

				pppg = -27.087 * fermentable.getYield().get() + 33.188;
			}
			else
			{
				return new DensityUnit(0);
			}
		}

		double weightLb = switch (fermentable.getType().getQuantityType())
		{
			case WEIGHT -> fermentableAddition.getQuantity().get(POUNDS);
			case VOLUME ->
				new WeightUnit(fermentableAddition.getQuantity().get(MILLILITRES), GRAMS).get(POUNDS);
			default ->
				throw new BrewdayException("invalid " + fermentable.getType().getQuantityType());
		};
		double volumeGal = volume.get(US_GALLON);

		double points = weightLb * pppg / volumeGal;

		return new DensityUnit(points, GU, true);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the colour provided by just dissolving the given fermentable in
	 * the given volume of fluid.
	 *
	 * @return The additional colour
	 */
	public static ColourUnit calcSolubleFermentableAdditionColourContribution(
		FermentableAddition fermentableAddition,
		VolumeUnit volume)
	{
		Fermentable.Type type = fermentableAddition.getFermentable().getType();
		if (type == Fermentable.Type.GRAIN || type == Fermentable.Type.ADJUNCT)
		{
			// these are not soluble
			return new ColourUnit(0);
		}

		VolumeUnit fermVol;
		if (fermentableAddition.getQuantity() instanceof VolumeUnit)
		{
			fermVol = (VolumeUnit)fermentableAddition.getQuantity();
		}
		else if (fermentableAddition.getQuantity() instanceof WeightUnit)
		{
			// assume a 1kg to 1l conversion
			fermVol = new VolumeUnit(fermentableAddition.getQuantity().get(KILOGRAMS), LITRES);
		}
		else
		{
			throw new BrewdayException("Invalid: " + fermentableAddition.getQuantity());
		}

		return calcColourWithVolumeChange(
			fermVol,
			fermentableAddition.getFermentable().getColour(),
			volume);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the bitternmess provided by just dissolving the given
	 * fermentable in the given volume of fluid. This only works for Fermentables
	 * of type EXTRACT and a non zero ibuGalPerLb property.
	 *
	 * @return The additional bitterness.
	 */
	public static BitternessUnit calcSolubleFermentableAdditionBitternessContribution(
		FermentableAddition fermentableAddition,
		VolumeUnit volume)
	{
		Fermentable.Type type = fermentableAddition.getFermentable().getType();
		if (type != Fermentable.Type.LIQUID_EXTRACT && type != Fermentable.Type.DRY_EXTRACT)
		{
			// no IBU provided
			return new BitternessUnit(0);
		}

		double ibuGalPerLb = fermentableAddition.getFermentable().getIbuGalPerLb();
		if (ibuGalPerLb <= 0)
		{
			// no IBU provided
			return new BitternessUnit(0);
		}

		double amountInLbs = fermentableAddition.getQuantity().get(POUNDS);

		// todo: this is based on a 60-minute boil; should be adjusting for boil time
		// source: BeerXML spec
		return new BitternessUnit(amountInLbs * ibuGalPerLb / volume.get(US_GALLON));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * http://www.howtobrew.com/book/section-2/what-is-malted-grain/extraction-and-maximum-yield
	 *
	 * @param yield the grain yield in %
	 * @return the extract potential in ppg
	 */
	public static double calcExtractPotentialFromYield(PercentageUnit yield)
	{
		// Extract potential in USA units:
		// GU that can be achieved with 1.00 pound (455 g) of malt mashed in 1.00 gallon (3.78 L) of water.
		// source: https://byo.com/article/understanding-malt-spec-sheets-advanced-brewing/

		return 46.21 * yield.get(PERCENTAGE);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * http://beersmith.com/blog/2010/09/07/apparent-and-real-attenuation-for-beer-brewers-part-1/
	 *
	 * @param start The starting gravity
	 * @param end   The final gravity
	 * @return The % attenuation
	 */
	public static double calcAttenuation(DensityUnit start, DensityUnit end)
	{
		double sgStart = start.get(SPECIFIC_GRAVITY);
		double sgEnd = end.get(SPECIFIC_GRAVITY);

		return (sgStart - sgEnd) / (sgStart - 1D);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * http://braukaiser.com/wiki/index.php/Accurately_Calculating_Sugar_Additions_for_Carbonation
	 * See also: https://byo.com/article/master-the-action-carbonation/
	 *
	 * @param inputVolume The volume to be carbonated
	 * @param priming     The nature and quantity of the substance used for
	 *                    priming
	 * @return The carbonation of the beer volume, in volumes CO2
	 */
	public static CarbonationUnit calcCarbonation(
		VolumeUnit inputVolume,
		FermentableAddition priming)
	{
		Fermentable fermentable = priming.getFermentable();

		if (fermentable.getType() == Fermentable.Type.GRAIN ||
			fermentable.getType() == Fermentable.Type.ADJUNCT)
		{
			// these are not fermentable without modification; zero carbonation
			return new CarbonationUnit(0);
		}

		WeightUnit weight = (WeightUnit)priming.getQuantity();
		double yield = fermentable.getYield().get(PERCENTAGE);

		// Each gram of fermentable extract is fermented into equal parts (by weight)
		// of alcohol and CO2 (this is not exactly true, but close enough for this calculation).

		double gramsPerLitre = 0.5D * yield * weight.get(GRAMS)
			/ inputVolume.get(LITRES);

		boolean estimated = inputVolume.isEstimated();

		return new CarbonationUnit(gramsPerLitre, GRAMS_PER_LITRE, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source:
	 * http://braukaiser.com/wiki/index.php/Accurately_Calculating_Sugar_Additions_for_Carbonation
	 * See also: https://byo.com/article/master-the-action-carbonation/
	 */
	public static FermentableAddition calcPrimingSugarAmount(
		VolumeUnit inputVolume,
		Fermentable primingSubstance,
		CarbonationUnit targetCarb)
	{
		double v = inputVolume.get(LITRES);
		double c = targetCarb.get(GRAMS_PER_LITRE);
		double y = primingSubstance.getYield().get(PERCENTAGE);

		// Each gram of fermentable extract is fermented into equal parts (by weight)
		// of alcohol and CO2 (this is not exactly true, but close enough for this calculation).

		double g = (v * c) / (0.5 * y);

		return new FermentableAddition(primingSubstance,
			new WeightUnit(g, GRAMS),
			GRAMS,
			new TimeUnit(0));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: http://braukaiser.com/documents/CO2_content_metric.pdf
	 *
	 * @param temp     the temp of the solution
	 * @param pressure the pressure under which the solution is, in kPa
	 */
	public static CarbonationUnit calcEquilibriumCo2(
		TemperatureUnit temp,
		PressureUnit pressure)
	{
		double tBeer = temp.get(KELVIN);
		double gramsPerLitre = (pressure.get(BAR))
			* Math.pow(2.71828182845904, -10.73797 + (2617.25 / tBeer))
			* 10;

		boolean estimated = temp.isEstimated() || pressure.isEstimated();

		return new CarbonationUnit(gramsPerLitre, GRAMS_PER_LITRE, estimated);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Dilutes the given Volume with the given water addition and returns a new
	 * Volume representing the mixture.
	 */
	public static Volume dilute(Volume input, WaterAddition waterAddition,
		String outputVolumeName)
	{
		VolumeUnit volumeOut = new VolumeUnit(input.getVolume());
		volumeOut = volumeOut.add(waterAddition.getVolume());

		TemperatureUnit tempOut = calcCombinedTemperature(
			input.getVolume(),
			input.getTemperature(),
			waterAddition.getVolume(),
			waterAddition.getTemperature());

		DensityUnit gravityOut = calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		PercentageUnit abvOut = calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		// assuming the water is at zero SRM and zero IBU

		ColourUnit colourOut = calcColourWithVolumeChange(
			input.getVolume(),
			input.getColour(),
			volumeOut);

		Volume result = new Volume(
			outputVolumeName,
			input.getType(),
			volumeOut,
			tempOut,
			input.getFermentability(),
			gravityOut,
			abvOut,
			colourOut,
			BitternessVolumes.zero());

		BitternessVolumes.applyVolumeChange(
			input,
			result,
			volumeOut,
			Settings.parseReportedFormulas(
				mclachlan.brewday.db.Database.getInstance().getSettings()));

		HopAcidVolumes.applyVolumeUnchanged(input, result);

		BitternessVolumes.syncReportedDerived(
			result,
			Settings.parseReportedFormulas(
				mclachlan.brewday.db.Database.getInstance().getSettings()));

		return result;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Source: http://braukaiser.com/wiki/index.php/Decoction_Mashing
	 *
	 * @return The volume that needs to be decocted to hit a certain mash temp
	 */
	public static VolumeUnit calcDecoctionVolume(
		VolumeUnit mashVolume,
		TemperatureUnit startTemp,
		TemperatureUnit targetTemp)
	{
		// decoction volume = total mash volume * (target temp - start temp) / (boil temp - start temp)

		double mashVolLitres = mashVolume.get(LITRES);
		double ratio =
			(targetTemp.get(CELSIUS) - startTemp.get(CELSIUS)) /
				(100 - startTemp.get(CELSIUS));

		return new VolumeUnit(mashVolLitres * ratio, LITRES);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return The Kolbach Residual Alkalinity, in ppm as CaCO3
	 */
	public static PpmUnit calcResidualAlkalinitySimple(Water water)
	{
		if (water.getCalcium() != null && water.getMagnesium() != null)
		{
			double alkalinity = calcAlkalinitySimple(water).get(PPM);
			double caFactor = water.getCalcium().get(PPM) / 1.4;
			double mgFactor = water.getMagnesium().get(PPM) / 1.7;

			return new PpmUnit(alkalinity - (caFactor + mgFactor));
		}
		else
		{
			return new PpmUnit(0);
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * This simple  formula just uses the water bincarbonate content to estimate
	 * alkalinity as ppm CaCO3. Source: EZ Water
	 *
	 * @return The Alkalinity, in ppm as CaCO3
	 */
	public static PpmUnit calcAlkalinitySimple(Water water)
	{
		if (water.getBicarbonate() != null)
		{
			double biCarbonateMEqL = water.getBicarbonate().get(PPM) / 61.02;

			// ppm = mEq/L * equiv weight
			// equivalent mass of CaCO3 = 50g
			return new PpmUnit(biCarbonateMEqL * 50, false);
		}
		else
		{
			return new PpmUnit(0);
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * This more complex alkalinity calculation takes the water pH into account.
	 * Source: The Water Book
	 *
	 * @return The Alkalinity, in ppm as CaCO3
	 */
	public static PpmUnit calcAlkalinity(Water water)
	{
		double ph = water.getPh().get(PH);

		double carbonatePerc, bicarbonatePerc, carbonicAcidPerc;

		if (ph <= 4)
		{
			carbonatePerc = alkalinityTable[0][1];
			bicarbonatePerc = alkalinityTable[0][2];
			carbonicAcidPerc = alkalinityTable[0][3];
		}
		else if (ph < 9)
		{
			int phRow = (int)((ph - 4) / 0.2);
			double phFloor = alkalinityTable[phRow][0];

			double inter = (ph - phFloor) / 0.2;

			// linear interpolate the values
			carbonatePerc = alkalinityTable[phRow][1] +
				(alkalinityTable[phRow + 1][1] - alkalinityTable[phRow][1]) * inter;

			bicarbonatePerc = alkalinityTable[phRow][2] +
				(alkalinityTable[phRow + 1][2] - alkalinityTable[phRow][2]) * inter;

			carbonicAcidPerc = alkalinityTable[phRow][3] +
				(alkalinityTable[phRow + 1][3] - alkalinityTable[phRow][3]) * inter;
		}
		else
		{
			carbonatePerc = alkalinityTable[alkalinityTable.length - 1][1];
			bicarbonatePerc = alkalinityTable[alkalinityTable.length - 1][2];
			carbonicAcidPerc = alkalinityTable[alkalinityTable.length - 1][3];
		}

		carbonatePerc = carbonatePerc / 100;
		bicarbonatePerc = bicarbonatePerc / 100;
		carbonicAcidPerc = carbonicAcidPerc / 100;

		double biCarbonateMEqL = water.getBicarbonate().get(PPM) / 61.02;

		double totalAlkalinityMEqL = biCarbonateMEqL / bicarbonatePerc;

		// ppm = mEq/L * equiv weight
		// equivalent mass of CaCO3 = 50g
		return new PpmUnit(totalAlkalinityMEqL * 50, false);
	}

	// columns: pH, Carbonate (CO3-2), BiCarbonate (HCO3-), Carbonic Acid (H2CO3)
	// Source: The Water Book, table 28
	private static final double[][] alkalinityTable =
		{
			{4, 0, 0.42, 99.58},
			{4.2, 0, 0.66, 99.34},
			{4.4, 0, 1.04, 98.96},
			{4.6, 0, 1.63, 98.37},
			{4.8, 0, 2.56, 97.44},
			{5, 0, 4, 96},
			{5.2, 0, 6.2, 93.8},
			{5.4, 0, 9.48, 90.52},
			{5.6, 0, 14.23, 85.77},
			{5.8, 0, 20.83, 79.17},
			{6, 0, 29.42, 70.58},
			{6.2, 0, 39.78, 60.21},
			{6.4, 0, 51.15, 48.85},
			{6.6, 0, 62.39, 37.6},
			{6.8, 0, 72.44, 27.54},
			{7, 0, 80.63, 19.34},
			{7.2, 0, 86.8, 13.14},
			{7.4, 0.1, 91.2, 8.71},
			{7.6, 0.16, 94.17, 5.67},
			{7.8, 0.25, 96.09, 3.65},
			{8, 0.41, 97.26, 2.33},
			{8.2, 0.65, 97.87, 1.48},
			{8.4, 1.03, 98.04, 0.94},
			{8.6, 1.62, 97.79, 0.59},
			{8.8, 2.55, 97.08, 0.37},
			{9, 3.99, 95.78, 0.23},
		};
}
