package mclachlan.brewday.ingredients;

import java.util.*;

/**
 *
 */
public class GrainBill
{
	private List<Grain> grains;

	public GrainBill(List<Grain> grains)
	{
		this.grains = grains;
	}

	public double getGrainWeight()
	{
		double result = 0D;

		for (Grain g : grains)
		{
			result += g.getWeight();
		}
		return result;
	}

	public List<Grain> getGrains()
	{
		return grains;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("GrainBill{");
		sb.append("grains=").append(getGrainWeight());
		sb.append('}');
		return sb.toString();
	}
}
