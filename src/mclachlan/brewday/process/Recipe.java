/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.BrewdayException;

/**
 *
 */
public class Recipe
{
	private String name;
	private List<ProcessStep> steps;
	private Volumes volumes;
	private List<String> warnings = new ArrayList<String>();
	private List<String> errors = new ArrayList<String>();

	public Recipe(String name, List<ProcessStep> steps, Volumes brew)
	{
		this.name = name;
		this.steps = steps;
		volumes = brew;
	}

	public List<ProcessStep> getSteps()
	{
		return steps;
	}

	public Volumes getVolumes()
	{
		return volumes;
	}

	/**
	 * Runs the batch end to end, populating created volumes and data along the way.
	 * Clears computed volumes before running.
	 */
	public void run()
	{
		errors.clear();
		warnings.clear();
		clearComputedVolumes();
		sortSteps();

		for (ProcessStep s : getSteps())
		{
			try
			{
				s.apply(getVolumes(), this);
			}
			catch (BrewdayException e)
			{
				errors.add(s.getName()+": "+e.getMessage());
				System.out.println("getSteps() = [" + getSteps() + "]");
				e.printStackTrace();
				return;
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public void sortSteps()
	{
		// Steps should be an acyclic directed graph, and we want a topological sort.
		// Instead of proper graph topo sort algo we use this dirty hack instead.
		// Maybe if more graph-like behaviour emerges it'll be worth refactoring
		// the ProcessStep package into a proper graph representation, or wrapping it in one.

		ProcessStep[] wip = steps.toArray(new ProcessStep[steps.size()]);

		boolean swapping = true;

		while (swapping)
		{
			swapping = false;

			for (int i = wip.length-1; i >=0; i--)
			{
				ProcessStep p1 = wip[i];

				for (int j = i; j >=0; j--)
				{
					ProcessStep p2 = wip[j];

					boolean p1SuppliesP2 = !Collections.disjoint(p1.getOutputVolumes(), p2.getInputVolumes());
					boolean p2SuppliesP1 = !Collections.disjoint(p1.getInputVolumes(), p2.getOutputVolumes());

					if (p1SuppliesP2 && p2SuppliesP1)
					{
						// can't have this
						throw new BrewdayException("Pipeline error: steps [" + p1.getName() + "] " +
							"and [" + p2.getName() + "] have a circular volume dependency");
					}

					if (p1SuppliesP2)
					{
						// swap the steps
						wip[i] = p2;
						wip[j] = p1;
						swapping = true;
					}
				}
			}
		}

		this.steps = new ArrayList<ProcessStep>(Arrays.asList(wip));
	}

	/**
	 * Clears computed volumes, leaving input volumes intact
	 */
	public void clearComputedVolumes()
	{
		Volumes newV = new Volumes();
		for (Volume v : volumes.getVolumes().values())
		{
			if (volumes.getInputVolumes().contains(v.getName()))
			{
				newV.addInputVolume(v.getName(), v);
			}
		}

		this.volumes = newV;
	}

	public String getName()
	{
		return name;
	}

	public void addError(String e)
	{
		this.errors.add(e);
	}

	public void addWarning(String w)
	{
		this.warnings.add(w);
	}

	public List<String> getErrors()
	{
		return this.errors;
	}

	public List<String> getWarnings()
	{
		return warnings;
	}

	public String getUniqueStepName(ProcessStep.Type type)
	{
		int count = 0;
		for (ProcessStep step : getSteps())
		{
			if (step.getType() == type)
			{
				count++;
			}
		}

		return type.toString()+" "+(count+1);
	}
}
