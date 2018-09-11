package mclachlan.brewday.ingredients;

import java.util.*;
import mclachlan.brewday.process.Volume;

/**
 *
 */
public class HopAddition implements Volume
{
	private String name;

	private List<Hop> hops;

	public HopAddition(List<Hop> hops)
	{
		this.hops = hops;
	}

	public List<Hop> getHops()
	{
		return hops;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setName(String name)
	{
		this.name = name;
	}

	public void setHops(List<Hop> hops)
	{
		this.hops = hops;
	}

	@Override
	public String describe()
	{
		return String.format("Hops: %s", name);
	}
}
