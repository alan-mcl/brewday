package mclachlan.brewday.process;

import java.util.*;

/**
 *
 */
public class Batch
{
	private List<ProcessStep> steps;
	private Volumes volumes;

	public Batch(List<ProcessStep> steps, Volumes brew)
	{

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
	 */
	public void run()
	{
		for (ProcessStep s : getSteps())
		{
			s.apply(getVolumes());
		}

	}
}
