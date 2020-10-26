
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
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

/**
 *
 */
public class LPTest
{
	public static void main(String[] args)
	{
		double targetCaPpm = 50;
		double targetHCO3Ppm = 50;
		double targetSO4Ppm = 50;
		double targetClPpm = 50;
		double targetMgPpm = 50;
		double targetNaPpm = 50;

		double sourceCaPpm = 0;
		double sourceHCO3Ppm = 0;
		double sourceSO4Ppm = 0;
		double sourceClPpm = 0;
		double sourceMgPpm = 0;
		double sourceNaPpm = 0;

		// the vars are:
		String[] labels= {"CaCO3(Un)", "CaCO3(Dis)", "CaSO4", "CaCl", "MgSO4", "NaHCO3", "NaCl", "Ca(HCO3)2", "MgCl"};
		boolean[] allowed = {false, false, true, true, true, true, true, false, false};

		LinearOptimizer lp = new SimplexSolver();

		LinearObjectiveFunction obj = new LinearObjectiveFunction(
			new double[]{1,1,1,1,1,1,1,1,1}, 0);

		Collection<LinearConstraint> constraints = new ArrayList<>();

		// Ca
		constraints.add(new LinearConstraint(
			new double[]{40.08/100.09, 40.08/100.09, 40.08/172.9, 40.08/147.02, 0, 0, 0, 40.08/162.11, 0},
			Relationship.LEQ, targetCaPpm - sourceCaPpm));

		// HCO3-
		constraints.add(new LinearConstraint(
			new double[]{(61/100.09)*2, (61/100.09), 0, 0, 0, 61D/84D, 0, 61D/162.11, 35.45/95.21},
			Relationship.LEQ, targetHCO3Ppm - sourceHCO3Ppm));

		// SO4
		constraints.add(new LinearConstraint(
			new double[]{0, 0, (96.07/172.19), 0, (96.07/246.51), 0, 0, 0, 0},
			Relationship.LEQ, targetSO4Ppm - sourceSO4Ppm));

		// Cl-
		constraints.add(new LinearConstraint(
			new double[]{0, 0, 0, (70.9/147.02), 0, 0, (35.45/58.44), 0, (35.45/95.21)},
			Relationship.LEQ, targetClPpm - sourceClPpm));

		// Mg
		constraints.add(new LinearConstraint(
			new double[]{0, 0, 0, 0, (24.31/246.51), 0, 0, 0, (24.31/95.21)},
			Relationship.LEQ, targetMgPpm - sourceMgPpm));

		// Na
		constraints.add(new LinearConstraint(
			new double[]{0, 0, 0, 0, 0, (23D/84D), (23D/58.44), 0, 0},
			Relationship.LEQ, targetNaPpm - sourceNaPpm));

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

		PointValuePair solution = lp.optimize(
			obj,
			new LinearConstraintSet(constraints),
			nonNegativeConstraint,
			GoalType.MAXIMIZE);

		if (solution != null)
		{
			//get solution
			double min = solution.getValue();
			System.out.println("Opt: " + min);

			//print decision variables
			for (int i = 0; i < 9; i++)
			{
				double n = solution.getPoint()[i];
				System.out.println(labels[i] +": "+Math.round(n)+"mg/l = "+(n*20/1000)+" g in 20L");
			}
		}
	}
}