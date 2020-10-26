
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
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.WaterAddition;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

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
			return StringUtils.getUiString("constraint."+name());
		}
	}

	/*-------------------------------------------------------------------------*/
	public enum AdditionGoal
	{
		MINIMISE, MAXIMISE;


		@Override
		public String toString()
		{
			return StringUtils.getUiString("addition.goal."+name());
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param startingWater
	 * 	The water profile to start with. You do the dilutions
	 * @param targetWater
	 * 	The desired ending water profile
	 * @param allowedAdditions
	 * 	Map of additions that can be used in the solution
	 * @param targetConstraints
	 * 	Map of constraints on the different water components
	 * @return
	 * 	A map of water addition formulae to their needed mg/L quantities.
	 * 	Returns null if solving fails or no solution is possible.
	 */
	public Map<Misc.WaterAdditionFormula, Double> calcAdditions(
		Water startingWater,
		Water targetWater,
		Map<Misc.WaterAdditionFormula, Boolean> allowedAdditions,
		Map<Water.Component, Constraint> targetConstraints,
		AdditionGoal additionGoal)
	{
		try
		{
			double targetCaPpm = targetWater.getCalcium().get(Quantity.Unit.PPM);
			double targetHCO3Ppm = targetWater.getBicarbonate().get(Quantity.Unit.PPM);
			double targetSO4Ppm = targetWater.getSulfate().get(Quantity.Unit.PPM);
			double targetClPpm = targetWater.getChloride().get(Quantity.Unit.PPM);
			double targetMgPpm = targetWater.getMagnesium().get(Quantity.Unit.PPM);
			double targetNaPpm = targetWater.getSodium().get(Quantity.Unit.PPM);

			double sourceCaPpm = startingWater.getCalcium().get(Quantity.Unit.PPM);
			double sourceHCO3Ppm = startingWater.getBicarbonate().get(Quantity.Unit.PPM);
			double sourceSO4Ppm = startingWater.getSulfate().get(Quantity.Unit.PPM);
			double sourceClPpm = startingWater.getChloride().get(Quantity.Unit.PPM);
			double sourceMgPpm = startingWater.getMagnesium().get(Quantity.Unit.PPM);
			double sourceNaPpm = startingWater.getSodium().get(Quantity.Unit.PPM);

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

			LinearObjectiveFunction obj = new LinearObjectiveFunction(
				new double[]{1,1,1,1,1,1,1,1,1}, 0);

			Collection<LinearConstraint> constraints = new ArrayList<>();

			// Ca
			if (targetConstraints.get(Water.Component.CALCIUM) != Constraint.DONTCARE)
			{
				constraints.add(new LinearConstraint(
					new double[]{40.08 / 100.09, (40.08 / 100.09) / 2, 40.08 / 172.9, 40.08 / 147.02, 0, 0, 0, 40.08 / 162.11, 0},
					getRelationship(targetConstraints.get(Water.Component.CALCIUM)), targetCaPpm - sourceCaPpm));
			}

			// HCO3-
			if (targetConstraints.get(Water.Component.BICARBONATE) != Constraint.DONTCARE)
			{
				constraints.add(new LinearConstraint(
					new double[]{(61/100.09)*2, (61/100.09), 0, 0, 0, 61D/84D, 0, 61D/162.11, 35.45/95.21},
					getRelationship(targetConstraints.get(Water.Component.BICARBONATE)), targetHCO3Ppm - sourceHCO3Ppm));
			}

			// SO4
			if (targetConstraints.get(Water.Component.SULFATE) != Constraint.DONTCARE)
			{
				constraints.add(new LinearConstraint(
					new double[]{0, 0, (96.07/172.19), 0, (96.07/246.51), 0, 0, 0, 0},
					getRelationship(targetConstraints.get(Water.Component.SULFATE)), targetSO4Ppm - sourceSO4Ppm));
			}

			// Cl-
			if (targetConstraints.get(Water.Component.CHLORIDE) != Constraint.DONTCARE)
			{
				constraints.add(new LinearConstraint(
					new double[]{0, 0, 0, (70.9/147.02), 0, 0, (35.45/58.44), 0, (35.45/95.21)},
					getRelationship(targetConstraints.get(Water.Component.CHLORIDE)), targetClPpm - sourceClPpm));
			}

			// Mg
			if (targetConstraints.get(Water.Component.MAGNESIUM) != Constraint.DONTCARE)
			{
				constraints.add(new LinearConstraint(
					new double[]{0, 0, 0, 0, (24.31/246.51), 0, 0, 0, (24.31/95.21)},
					getRelationship(targetConstraints.get(Water.Component.MAGNESIUM)), targetMgPpm - sourceMgPpm));
			}

			// Na
			if (targetConstraints.get(Water.Component.SODIUM) != Constraint.DONTCARE)
			{
				constraints.add(new LinearConstraint(
					new double[]{0, 0, 0, 0, 0, (23D/84D), (23D/58.44), 0, 0},
					getRelationship(targetConstraints.get(Water.Component.SODIUM)), targetNaPpm - sourceNaPpm));
			}

			// all vars >=0
			NonNegativeConstraint nonNegativeConstraint = new NonNegativeConstraint(true);

			// check which additions are allowed
			for (int i=0; i<allowed.length; i++)
			{
				if (!allowed[i])
				{
					double[] coefficients = {0, 0, 0, 0, 0, 0, 0, 0, 0};
					coefficients[i] = 1;
					constraints.add(new LinearConstraint(coefficients, Relationship.EQ, 0));
				}
			}

			GoalType goal;
			switch (additionGoal)
			{
				case MINIMISE:
					goal = GoalType.MINIMIZE;
					break;
				case MAXIMISE:
					goal = GoalType.MAXIMIZE;
					break;
				default:
					throw new IllegalStateException("Unexpected value: " + additionGoal);
			}

			PointValuePair solution = lp.optimize(
				obj,
				new LinearConstraintSet(constraints),
				nonNegativeConstraint,
				goal);

			Map<Misc.WaterAdditionFormula, Double> result = new HashMap<>();
			for (int i = 0; i < 9; i++)
			{
				double n = solution.getPoint()[i];
				result.put(keys[i], n);
			}

			return result;
		}
		catch (Exception e)
		{
			// ignore failed fits
//			e.printStackTrace();
			return null;
		}
	}

	/*-------------------------------------------------------------------------*/
	public BestFitResults bestFit(
		Water startingWater,
		Water targetWater,
		Map<Misc.WaterAdditionFormula, Boolean> allowedAdditions)
	{
		double bestMse = Double.MAX_VALUE;

		Map<Water.Component, Constraint> constraints = new HashMap<>();
		AdditionGoal goal;
		BestFitResults result = null;

		Constraint[] constraintOptions = {Constraint.DONTCARE, Constraint.LEQ, Constraint.GEQ};

		for (int i=0; i<AdditionGoal.values().length; i++)
		{
			goal = AdditionGoal.values()[i];

			for (int ca=0; ca<constraintOptions.length; ca++)
			{
				for (int hco3=0; hco3<constraintOptions.length; hco3++)
				{
					for (int so4=0; so4<constraintOptions.length; so4++)
					{
						for (int cl=0; cl<constraintOptions.length; cl++)
						{
							for (int na=0; na<constraintOptions.length; na++)
							{
								for (int mg=0; mg<constraintOptions.length; mg++)
								{
									constraints.put(Water.Component.CALCIUM, constraintOptions[ca]);
									constraints.put(Water.Component.BICARBONATE, constraintOptions[hco3]);
									constraints.put(Water.Component.SULFATE, constraintOptions[so4]);
									constraints.put(Water.Component.CHLORIDE, constraintOptions[cl]);
									constraints.put(Water.Component.SODIUM, constraintOptions[na]);
									constraints.put(Water.Component.MAGNESIUM, constraintOptions[mg]);

									Map<Misc.WaterAdditionFormula, Double> solution = this.calcAdditions(
										startingWater,
										targetWater,
										allowedAdditions,
										constraints,
										goal);

									if (solution != null)
									{
										double mse = 0;

										Water w = buildWaterFromResult(startingWater, solution, new VolumeUnit(1, Quantity.Unit.LITRES));

										mse += Math.pow(w.getCalcium().get(Quantity.Unit.PPM) - targetWater.getCalcium().get(Quantity.Unit.PPM), 2);
										mse += Math.pow(w.getMagnesium().get(Quantity.Unit.PPM) - targetWater.getMagnesium().get(Quantity.Unit.PPM), 2);
										mse += Math.pow(w.getSodium().get(Quantity.Unit.PPM) - targetWater.getSodium().get(Quantity.Unit.PPM), 2);
										mse += Math.pow(w.getSulfate().get(Quantity.Unit.PPM) - targetWater.getSulfate().get(Quantity.Unit.PPM), 2);
										mse += Math.pow(w.getChloride().get(Quantity.Unit.PPM) - targetWater.getChloride().get(Quantity.Unit.PPM), 2);
										mse += Math.pow(w.getBicarbonate().get(Quantity.Unit.PPM) - targetWater.getBicarbonate().get(Quantity.Unit.PPM), 2);

										mse /= 6;

										if (mse < bestMse)
										{
											result = new BestFitResults(
												new HashMap<>(solution),
												new HashMap<>(constraints),
												goal);

											System.out.println("mse = " + mse);

											bestMse = mse;
										}

									}
								}
							}
						}
					}
				}
			}
		}

		return result;
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
	private Water getWater(double n, Water w, Misc.WaterAdditionFormula additionType, VolumeUnit volume)
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

	/*-------------------------------------------------------------------------*/
	public static class BestFitResults
	{
		private final Map<Misc.WaterAdditionFormula, Double> additions;
		private final Map<Water.Component, Constraint> targetConstraints;
		private final AdditionGoal additionGoal;

		public BestFitResults(
			Map<Misc.WaterAdditionFormula, Double> additions,
			Map<Water.Component, Constraint> targetConstraints,
			AdditionGoal additionGoal)
		{
			this.additions = additions;
			this.targetConstraints = targetConstraints;
			this.additionGoal = additionGoal;
		}

		public Map<Misc.WaterAdditionFormula, Double> getAdditions()
		{
			return additions;
		}

		public Map<Water.Component, Constraint> getTargetConstraints()
		{
			return targetConstraints;
		}

		public AdditionGoal getAdditionGoal()
		{
			return additionGoal;
		}
	}

	/*-------------------------------------------------------------------------*/
	private Relationship getRelationship(Constraint c)
	{
		switch (c)
		{
			case EQUAL:
				return Relationship.EQ;
			case GEQ:
				return Relationship.GEQ;
			case LEQ:
				return Relationship.LEQ;
			default:
				throw new IllegalStateException("Unexpected value: " + c);
		}
	}
}
