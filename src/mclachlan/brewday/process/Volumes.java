package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.BrewdayException;

/**
 *
 */
public class Volumes
{
	/** A fancy name for ingredients. Any other volume is a computed one. */
	private Set<String> inputVolumes = new HashSet<String>();
	private Map<String, Volume> volumes = new HashMap<String, Volume>();

	/**
	 * Adds an input volume.
	 */
	public  void addInputVolume(String key, Volume v)
	{
		if (volumes.containsKey(key))
		{
			throw new BrewdayException("volume already exists ["+key+"]");
		}

		volumes.put(key, v);
		v.setName(key);
		inputVolumes.add(key);
	}

	/**
	 * Adds a computed volume.
	 */
	public void addVolume(String key, Volume v)
	{
		if (volumes.containsKey(key))
		{
			throw new BrewdayException("volume already exists ["+key+"]");
		}

		volumes.put(key, v);
		v.setName(key);
	}

	public Volume getVolume(String key)
	{
		if (!volumes.containsKey(key))
		{
			throw new BrewdayException("volume does not exist ["+key+"]");
		}

		return volumes.get(key);
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("{");

		for (Volume v : volumes.values())
		{
			sb.append(v.toString()+"\n");
		}

		sb.append("}");
		return sb.toString();
	}

	public Map<String, Volume> getVolumes()
	{
		return volumes;
	}

	public Set<String> getInputVolumes()
	{
		return inputVolumes;
	}
}
