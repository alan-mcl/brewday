
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

package mclachlan.brewday.math;

import java.util.*;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.util.Log;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import static mclachlan.brewday.math.Quantity.Unit.PPM;

/**
 * Wraps the LP modules
 */
public class WaterBuilder
{
	/*-------------------------------------------------------------------------*/
	public enum Constraint
	{
		EQUAL, GEQ, LEQ, DONTCARE;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("constraint." + name());
		}
	}

	/*-------------------------------------------------------------------------*/
	public enum AdditionGoal
	{
		MINIMISE_ADDITIONS,
		MAXIMISE_ADDITIONS;


		@Override
		public String toString()
		{
			return StringUtils.getUiString("addition.goal." + name());
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param startingWater    The water profile to start with. You do the
	 *                         dilutions
	 * @param targetWater      The desired ending water profile
	 * @param allowedAdditions Map of additions that can be used in the solution
	 * @return A map of water addition formulae to their needed mg/L quantities.
	 * Returns null if solving fails or no solution is possible.
	 */
	public Map<Misc.WaterAdditionFormula, Double> calcAdditions(
		Water startingWater,
		WaterParameters targetWater,
		Map<Misc.WaterAdditionFormula, Boolean> allowedAdditions,
		AdditionGoal additionGoal)
	{
		if (additionGoal == AdditionGoal.MINIMISE_ADDITIONS || additionGoal == AdditionGoal.MAXIMISE_ADDITIONS)
		{
			return calcAdditionsMinOrMaxIngredients(startingWater, targetWater, allowedAdditions, additionGoal);
		}
		else
		{
			throw new BrewdayException("invalid: " + additionGoal);
		}
	}

	/*-------------------------------------------------------------------------*/
	protected Map<Misc.WaterAdditionFormula, Double> calcAdditionsMinOrMaxIngredients(
		Water startingWater,
		WaterParameters targetWater,
		Map<Misc.WaterAdditionFormula, Boolean> allowedAdditions,
		AdditionGoal additionGoal)
	{
		try
		{
			double sourceCaPpm = startingWater.getCalcium().get(PPM);
			double sourceHCO3Ppm = startingWater.getBicarbonate().get(PPM);
			double sourceSO4Ppm = startingWater.getSulfate().get(PPM);
			double sourceClPpm = startingWater.getChloride().get(PPM);
			double sourceMgPpm = startingWater.getMagnesium().get(PPM);
			double sourceNaPpm = startingWater.getSodium().get(PPM);

			// the vars are:
			Misc.WaterAdditionFormula[] keys =
				{
					Misc.WaterAdditionFormula.CALCIUM_CARBONATE_UNDISSOLVED,
					Misc.WaterAdditionFormula.CALCIUM_CARBONATE_DISSOLVED,
					Misc.WaterAdditionFormula.CALCIUM_SULPHATE_DIHYDRATE,
					Misc.WaterAdditionFormula.CALCIUM_CHLORIDE_DIHYDRATE,
					Misc.WaterAdditionFormula.MAGNESIUM_SULFATE_HEPTAHYDRATE,
					Misc.WaterAdditionFormula.SODIUM_BICARBONATE,
					Misc.WaterAdditionFormula.SODIUM_CHLORIDE,
					Misc.WaterAdditionFormula.CALCIUM_BICARBONATE,
					Misc.WaterAdditionFormula.MAGNESIUM_CHLORIDE_HEXAHYDRATE,
				};

			boolean[] allowed =
				{
					allowedAdditions.get(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_UNDISSOLVED),
					allowedAdditions.get(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_DISSOLVED),
					allowedAdditions.get(Misc.WaterAdditionFormula.CALCIUM_SULPHATE_DIHYDRATE),
					allowedAdditions.get(Misc.WaterAdditionFormula.CALCIUM_CHLORIDE_DIHYDRATE),
					allowedAdditions.get(Misc.WaterAdditionFormula.MAGNESIUM_SULFATE_HEPTAHYDRATE),
					allowedAdditions.get(Misc.WaterAdditionFormula.SODIUM_BICARBONATE),
					allowedAdditions.get(Misc.WaterAdditionFormula.SODIUM_CHLORIDE),
					allowedAdditions.get(Misc.WaterAdditionFormula.CALCIUM_BICARBONATE),
					allowedAdditions.get(Misc.WaterAdditionFormula.MAGNESIUM_CHLORIDE_HEXAHYDRATE),
				};

			LinearOptimizer lp = new SimplexSolver();

			LinearObjectiveFunction obj;

			//
			// LP construction:
			//
			// For MINIMIZE_ADDITIONS and MAXIMISE_ADDITIONS:
			//
			// Input Water ppm |  Target ppm
			// ------------------------------
			// Ca      s1      |     t1
			// HCO-    s2      |     t2
			// SO4     s3      |     t3
			// Cl-     s4      |     t4
			// Mg      s5      |     t5
			// Na      s6      |     t6
			//
			// Additions (mg/l):
			// -------------------
			// CaCO3_undis     x1
			// CaCO3_dis       x2
			// CaSO4           x3
			// CaCl            x4
			// MgSO4           x5
			// NaHCO3          x6
			// NaCl            x7
			// Ca(HCO3)        x8
			// MgCl            x9
			//
			// Let's  the molecular ratio used to get from addition mg/l to
			// solution ppm be "a":
			// a11...a69 (e.g. a11 = 40.08/100.09 = Ca contributed by CaCO3_undis )
			//
			// So then (where <=> is the constraints specified in targetConstraints):
			//
			// ppm constraints:
			// - - - - - - - - - - - - - - - - -
			// a11.x1 + ... + a19.x9 <=> t1 - s1
			// a21.x1 + ... + a29.x9 <=> t2 - s2
			// a31.x1 + ... + a39.x9 <=> t3 - s3
			// a41.x1 + ... + a49.x9 <=> t4 - s4
			// a51.x1 + ... + a59.x9 <=> t5 - s5
			// a61.x1 + ... + a69.x9 <=> t6 - s6
			//
			// alkalinity constraint
			// - - - - - - - - - - - - - - - - -
			// We use the simple Alk formula: ppm HCO3*50/61.02
			// Let 50/61.02 = a7. This gives us:
			// a7.a21.x1 + ... + a7.a29.x9 <=> Alk - a7.s2
			//
			// residual alkalinity constraint
			// - - - - - - - - - - - - - - - - -
			// RA = Alk - (ppm Ca)/1.4 - (ppm Mg)/1.7
			// thus:
			// (a7.a21 - a11/1.4 - a51/1.7)x1 + ... + (a7.a29 - a19/1.4 - a59/1.7)x9
			//      <=> -(a7.s2 - s1/1.4 - s5/1.7
			//
			// non=negative constraints:
			// - - - - - - - - - - - - - - - - -
			// xn >= 0 for n = 1..9
			//
			// allowed ingredients constraints:
			// - - - - - - - - - - - - - - - - -
			// xn = 0 for n in allowedAdditions
			//
			// objective function:
			// - - - - - - - - - - - - - - - - -
			// For MIN or MAX additions we simply sum the addition quantities
			// y = x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9
			//


			// we simply sum up the addition quantities
			obj = new LinearObjectiveFunction(
				new double[]{1, 1, 1, 1, 1, 1, 1, 1, 1}, 0);

			Collection<LinearConstraint> constraints = getLinearConstraints(
				sourceCaPpm, sourceHCO3Ppm, sourceSO4Ppm, sourceClPpm, sourceMgPpm, sourceNaPpm, targetWater, allowed);

			// all vars >=0
			NonNegativeConstraint nonNegativeConstraint = new NonNegativeConstraint(true);

			GoalType goal;
			switch (additionGoal)
			{
				case MINIMISE_ADDITIONS:
					goal = GoalType.MINIMIZE;
					break;
				case MAXIMISE_ADDITIONS:
					goal = GoalType.MAXIMIZE;
					break;
				default:
					throw new IllegalStateException("Unexpected value: " + additionGoal);
			}

			PointValuePair solution;

			solution = lp.optimize(
				obj,
				new LinearConstraintSet(constraints),
				nonNegativeConstraint,
				goal);

			return getWaterAdditions(keys, solution);
		}
		catch (Exception e)
		{
			// ignore failed fits
			Brewday.getInstance().getLog().log(Log.DEBUG, e);
			return null;
		}
	}

	/*-------------------------------------------------------------------------*/
	protected Map<Misc.WaterAdditionFormula, Double> getWaterAdditions(
		Misc.WaterAdditionFormula[] keys, PointValuePair solution)
	{
		Map<Misc.WaterAdditionFormula, Double> result = new HashMap<>();
		for (int i = 0; i < 9; i++)
		{
			double n = solution.getPoint()[i];
			result.put(keys[i], n);
		}
		return result;
	}

	/*-------------------------------------------------------------------------*/
	protected Collection<LinearConstraint> getLinearConstraints(
		double sourceCalciumPpm,
		double sourceBicarbonatePpm, double sourceSulfatePpm,
		double sourceChloridePpm,
		double sourceMagnesiumPpm, double sourceSodiumPpm,
		WaterParameters target,
		boolean[] allowed)
	{
		Collection<LinearConstraint> constraints = new ArrayList<>();

		double[] calciumCoeff = {40.08 / 100.09, (40.08 / 100.09) / 2, 40.08 / 172.9, 40.08 / 147.02, 0, 0, 0, 40.08 / 162.11, 0};
		double[] bicarbonateCoeff = {(61 / 100.09) * 2, (61 / 100.09), 0, 0, 0, 61D / 84D, 0, 61D / 162.11, 35.45 / 95.21};
		double[] sulfateCoeff = {0, 0, (96.07 / 172.9), 0, (96.07 / 246.51), 0, 0, 0, 0};
		double[] chlorideCoeff = {0, 0, 0, (70.9 / 147.02), 0, 0, (35.45 / 58.44), 0, (35.45 / 95.21)};
		double[] magnesiumCoeff = {0, 0, 0, 0, (24.31 / 246.51), 0, 0, 0, (24.31 / 95.21)};
		double[] sodiumCoeff = {0, 0, 0, 0, 0, (23D / 84D), (23D / 58.44), 0, 0};

		// alkalinity constraint
		// - - - - - - - - - - - - - - - - -
		// We use the simple Alk formula: Alk = (sourceHCO3 + incHCO3) * 50/61.02
		// Let 50/61.02 = a7. This gives us:
		// a7.a21.x1 + ... + a7.a29.x9 <=> Alk - a7.s2

		double a7 = 50D / 61.02D;
		double[] alkCoeff =
			{
				a7 * bicarbonateCoeff[0],
				a7 * bicarbonateCoeff[1],
				a7 * bicarbonateCoeff[2],
				a7 * bicarbonateCoeff[3],
				a7 * bicarbonateCoeff[4],
				a7 * bicarbonateCoeff[5],
				a7 * bicarbonateCoeff[6],
				a7 * bicarbonateCoeff[7],
				a7 * bicarbonateCoeff[8],
			};

		if (target.getMinAlkalinity() != null)
		{
			constraints.add(new LinearConstraint(alkCoeff, Relationship.GEQ,
				target.getMinAlkalinity().get(PPM) - a7 * sourceBicarbonatePpm));
		}
		if (target.getMaxAlkalinity() != null)
		{
			constraints.add(new LinearConstraint(alkCoeff, Relationship.LEQ,
				target.getMaxAlkalinity().get(PPM) - a7 * sourceBicarbonatePpm));
		}

		// residual alkalinity constraint
		// - - - - - - - - - - - - - - - - -
		// RA = Alk - (ppm Ca)/1.4 - (ppm Mg)/1.7
		// thus:
		// (a7.a21 - a11/1.4 - a51/1.7)x1 + ... + (a7.a29 - a19/1.4 - a59/1.7)x9
		//      <=> -(a7.s2 - s1/1.4 - s5/1.7)

		double[] raCoeff =
			{
				a7 * bicarbonateCoeff[0] - calciumCoeff[0] / 1.4 - magnesiumCoeff[0] / 1.7,
				a7 * bicarbonateCoeff[1] - calciumCoeff[1] / 1.4 - magnesiumCoeff[1] / 1.7,
				a7 * bicarbonateCoeff[2] - calciumCoeff[2] / 1.4 - magnesiumCoeff[2] / 1.7,
				a7 * bicarbonateCoeff[3] - calciumCoeff[3] / 1.4 - magnesiumCoeff[3] / 1.7,
				a7 * bicarbonateCoeff[4] - calciumCoeff[4] / 1.4 - magnesiumCoeff[4] / 1.7,
				a7 * bicarbonateCoeff[5] - calciumCoeff[5] / 1.4 - magnesiumCoeff[5] / 1.7,
				a7 * bicarbonateCoeff[6] - calciumCoeff[6] / 1.4 - magnesiumCoeff[6] / 1.7,
				a7 * bicarbonateCoeff[7] - calciumCoeff[7] / 1.4 - magnesiumCoeff[7] / 1.7,
				a7 * bicarbonateCoeff[8] - calciumCoeff[8] / 1.4 - magnesiumCoeff[8] / 1.7,
			};

		if (target.getMinResidualAlkalinity() != null)
		{
			double raValue = target.getMinResidualAlkalinity().get(PPM)
				- (a7 * sourceBicarbonatePpm - sourceCalciumPpm / 1.4 - sourceMagnesiumPpm / 1.7);

			constraints.add(new LinearConstraint(raCoeff, Relationship.GEQ, raValue));
		}
		if (target.getMaxResidualAlkalinity() != null)
		{
			double raValue = target.getMaxResidualAlkalinity().get(PPM)
				- (a7 * sourceBicarbonatePpm - sourceCalciumPpm / 1.4 - sourceMagnesiumPpm / 1.7);

			constraints.add(new LinearConstraint(raCoeff, Relationship.LEQ, raValue));
		}

		// Ca
		if (target.getMinCalcium() != null)
		{
			constraints.add(new LinearConstraint(calciumCoeff, Relationship.GEQ,
				target.getMinCalcium().get(PPM) - sourceCalciumPpm));
		}
		if (target.getMaxCalcium() != null)
		{
			constraints.add(new LinearConstraint(calciumCoeff, Relationship.LEQ,
				target.getMaxCalcium().get(PPM) - sourceCalciumPpm));
		}

		// HCO3-
		if (target.getMinBicarbonate() != null)
		{
			constraints.add(new LinearConstraint(bicarbonateCoeff, Relationship.GEQ,
				target.getMinBicarbonate().get(PPM) - sourceBicarbonatePpm));
		}
		if (target.getMaxBicarbonate() != null)
		{
			constraints.add(new LinearConstraint(bicarbonateCoeff, Relationship.LEQ,
				target.getMaxBicarbonate().get(PPM) - sourceBicarbonatePpm));
		}

		// SO4
		if (target.getMinSulfate() != null)
		{
			constraints.add(new LinearConstraint(sulfateCoeff, Relationship.GEQ,
				target.getMinSulfate().get(PPM) - sourceSulfatePpm));
		}
		if (target.getMaxSulfate() != null)
		{
			constraints.add(new LinearConstraint(sulfateCoeff, Relationship.LEQ,
				target.getMaxSulfate().get(PPM) - sourceSulfatePpm));
		}

		// Cl-
		if (target.getMinChloride() != null)
		{
			constraints.add(new LinearConstraint(chlorideCoeff, Relationship.GEQ,
				target.getMinChloride().get(PPM) - sourceChloridePpm));
		}
		if (target.getMaxChloride() != null)
		{
			constraints.add(new LinearConstraint(chlorideCoeff, Relationship.LEQ,
				target.getMaxChloride().get(PPM) - sourceChloridePpm));
		}

		// Mg
		if (target.getMinMagnesium() != null)
		{
			constraints.add(new LinearConstraint(magnesiumCoeff, Relationship.GEQ,
				target.getMinMagnesium().get(PPM) - sourceMagnesiumPpm));
		}
		if (target.getMaxMagnesium() != null)
		{
			constraints.add(new LinearConstraint(magnesiumCoeff, Relationship.LEQ,
				target.getMaxMagnesium().get(PPM) - sourceMagnesiumPpm));
		}

		// Na
		if (target.getMinSodium() != null)
		{
			constraints.add(new LinearConstraint(sodiumCoeff, Relationship.GEQ,
				target.getMinSodium().get(PPM) - sourceSodiumPpm));
		}
		if (target.getMaxSodium() != null)
		{
			constraints.add(new LinearConstraint(sodiumCoeff, Relationship.LEQ,
				target.getMaxSodium().get(PPM) - sourceSodiumPpm));
		}

		// check which additions are allowed
		for (int i = 0; i < allowed.length; i++)
		{
			if (!allowed[i])
			{
				double[] coefficients = {0, 0, 0, 0, 0, 0, 0, 0, 0};
				coefficients[i] = 1;
				constraints.add(new LinearConstraint(coefficients, Relationship.EQ, 0));
			}
		}
		return constraints;
	}

	/*-------------------------------------------------------------------------*/
	public Water buildWaterFromResult(Water startingWater,
		Map<Misc.WaterAdditionFormula, Double> result, VolumeUnit volume)
	{
		double vol = volume.get(Quantity.Unit.LITRES);

		double caCo3UnG = result.get(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_UNDISSOLVED) * vol / 1000D;
		double caCo3DisG = result.get(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_DISSOLVED) * vol / 1000D;
		double caSo4G = result.get(Misc.WaterAdditionFormula.CALCIUM_SULPHATE_DIHYDRATE) * vol / 1000D;
		double caClG = result.get(Misc.WaterAdditionFormula.CALCIUM_CHLORIDE_DIHYDRATE) * vol / 1000D;
		double mgSo4G = result.get(Misc.WaterAdditionFormula.MAGNESIUM_SULFATE_HEPTAHYDRATE) * vol / 1000D;
		double naHco3G = result.get(Misc.WaterAdditionFormula.SODIUM_BICARBONATE) * vol / 1000D;
		double naClG = result.get(Misc.WaterAdditionFormula.SODIUM_CHLORIDE) * vol / 1000D;
		double caHCO3G = result.get(Misc.WaterAdditionFormula.CALCIUM_BICARBONATE) * vol / 1000D;
		double mgClG = result.get(Misc.WaterAdditionFormula.MAGNESIUM_CHLORIDE_HEXAHYDRATE) * vol / 1000D;

		return buildWater(startingWater, volume, caCo3UnG, caCo3DisG, caSo4G, caClG, mgSo4G, naHco3G, naClG, caHCO3G, mgClG);
	}

	/*-------------------------------------------------------------------------*/
	public Water buildWater(Water startingWater, VolumeUnit volume,
		double caCo3UnG, double caCo3DisG, double caSo4G, double caClG,
		double mgSo4G, double naHco3G, double naClG, double caHCO3G, double mgClG)
	{
		// work out the additions
		Water w = getWater(caCo3UnG, startingWater, Misc.WaterAdditionFormula.CALCIUM_CARBONATE_UNDISSOLVED, volume);
		w = getWater(caCo3DisG, w, Misc.WaterAdditionFormula.CALCIUM_CARBONATE_DISSOLVED, volume);
		w = getWater(caSo4G, w, Misc.WaterAdditionFormula.CALCIUM_SULPHATE_DIHYDRATE, volume);
		w = getWater(caClG, w, Misc.WaterAdditionFormula.CALCIUM_CHLORIDE_DIHYDRATE, volume);
		w = getWater(mgSo4G, w, Misc.WaterAdditionFormula.MAGNESIUM_SULFATE_HEPTAHYDRATE, volume);
		w = getWater(naHco3G, w, Misc.WaterAdditionFormula.SODIUM_BICARBONATE, volume);
		w = getWater(naClG, w, Misc.WaterAdditionFormula.SODIUM_CHLORIDE, volume);
		w = getWater(caHCO3G, w, Misc.WaterAdditionFormula.CALCIUM_BICARBONATE, volume);
		w = getWater(mgClG, w, Misc.WaterAdditionFormula.MAGNESIUM_CHLORIDE_HEXAHYDRATE, volume);
		return w;
	}

	/*-------------------------------------------------------------------------*/
	private Water getWater(double n, Water w,
		Misc.WaterAdditionFormula additionType, VolumeUnit volume)
	{
		Misc misc = new Misc();
		misc.setWaterAdditionFormula(additionType);
		MiscAddition ma = new MiscAddition();
		ma.setUnit(Quantity.Unit.GRAMS);
		ma.setMisc(misc);
		ma.setQuantity(new WeightUnit(n, Quantity.Unit.GRAMS));
		w = Equations.calcBrewingSaltAddition(new WaterAddition(
			w, volume, Quantity.Unit.LITRES, new TemperatureUnit(0), new TimeUnit(0)), ma);
		return w;
	}
}
