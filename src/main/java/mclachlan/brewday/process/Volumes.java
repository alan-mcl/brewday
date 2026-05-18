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

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class Volumes
{
	/** Contains all the volumes of this recipe or batch, indexed by name */
	private Map<String, Volume> volumes = new HashMap<>();

	/** Special output volume(s) that represent the end result, typically beer. */
	private Set<String> outputVolumes = new HashSet<>();

	/*-------------------------------------------------------------------------*/
	public Volumes()
	{
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Deep clone of the other set of volumes.
	 */
	public Volumes(Volumes other)
	{
		for (Map.Entry<String, Volume> e : other.volumes.entrySet())
		{
			this.volumes.put(e.getKey(), e.getValue().clone());
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Adds an output volume.
	 */
	public void addOutputVolume(String key, Volume v)
	{
		if (volumes.containsKey(key))
		{
			throw new BrewdayException("volume already exists ["+key+"]");
		}

		volumes.put(key, v);
		v.setName(key);
		outputVolumes.add(key);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Given the key and volume, add as an output volume it if it's not present. If it is present,
	 * update all of it's estimate metrics. Measured metrics from a volume already
	 * present are not updated.
	 */
	public void addOrUpdateOutputVolume(String key, Volume v)
	{
		addOrUpdateVolume(key, v);
		v.setName(key);
		outputVolumes.add(key);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Adds a computed volume.
	 */
	public void addVolume(String key, Volume v)
	{
		if (v == null)
		{
			throw new NullPointerException();
		}
		if (key == null)
		{
			throw new NullPointerException();
		}
		if (volumes.containsKey(key))
		{
			throw new BrewdayException("volume already exists ["+key+"]");
		}

		volumes.put(key, v);
		v.setName(key);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Given the key and volume, add it if it's not present. If it is present,
	 * update all of it's estimate metrics. Measured metrics from a volume already
	 * present are not updated.
	 */
	public void addOrUpdateVolume(String key, Volume v)
	{
		if (v == null)
		{
			throw new NullPointerException();
		}
		if (key == null)
		{
			throw new NullPointerException();
		}
		if (!volumes.containsKey(key))
		{
			this.addVolume(key, v);
		}
		else
		{
			Volume current = getVolume(key);

			for (Volume.Metric m : v.getMetrics().keySet())
			{
				Quantity currentQuantity = current.getMetric(m);
				Quantity otherQuantity = v.getMetric(m);

				if (currentQuantity != null && !currentQuantity.isEstimated() && otherQuantity.isEstimated())
				{
					// do not override measured metrics with estimated ones
				}
				else
				{
					current.setMetric(m, otherQuantity, otherQuantity.isEstimated());
				}
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public Volume getVolume(String key)
	{
		if (!volumes.containsKey(key))
		{
			throw new BrewdayException("volume does not exist ["+key+"]");
		}

		return volumes.get(key);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("{");

		for (Volume v : volumes.values())
		{
			sb.append(v.toString()).append("\n");
		}

		sb.append("}");
		return sb.toString();
	}

	/*-------------------------------------------------------------------------*/
	public Map<String, Volume> getVolumes()
	{
		return volumes;
	}

	/*-------------------------------------------------------------------------*/
	public Set<String> getOutputVolumes()
	{
		return outputVolumes;
	}

	/*-------------------------------------------------------------------------*/
	public void setOutputVolumes(Set<String> outputVolumes)
	{
		this.outputVolumes = outputVolumes;
	}

	/*-------------------------------------------------------------------------*/
	public void setVolumes(Map<String, Volume> volumes)
	{
		this.volumes = volumes;
	}

	/*-------------------------------------------------------------------------*/
	public boolean contains(String volName)
	{
		return volumes.containsKey(volName);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Renames a volume in this registry: changes its key in the volume map and,
	 * if the volume is registered as an output, updates the output set as well.
	 * The {@link Volume#getName()} of the renamed entry is also updated.
	 * <p>
	 * Silently no-ops if {@code oldKey} is not present (the Volumes registry is a
	 * runtime cache rebuilt by {@link Recipe#run} / {@code dryRun}; callers may
	 * rename volume names on steps before the recipe has been run).
	 *
	 * @throws BrewdayException if either argument is null/blank, if the names are
	 * equal, or if {@code newKey} already exists.
	 */
	public void renameVolume(String oldKey, String newKey)
	{
		if (oldKey == null || oldKey.isBlank())
		{
			throw new BrewdayException("old volume name is null or blank");
		}
		if (newKey == null || newKey.isBlank())
		{
			throw new BrewdayException("new volume name is null or blank");
		}
		if (oldKey.equals(newKey))
		{
			throw new BrewdayException("old and new volume names are equal ["+oldKey+"]");
		}
		if (!volumes.containsKey(oldKey))
		{
			// runtime cache not yet populated; nothing to rename here
			return;
		}
		if (volumes.containsKey(newKey))
		{
			throw new BrewdayException("volume already exists ["+newKey+"]");
		}

		Volume v = volumes.remove(oldKey);
		v.setName(newKey);
		volumes.put(newKey, v);

		if (outputVolumes.remove(oldKey))
		{
			outputVolumes.add(newKey);
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	A random volume of the given type
	 */
	public String getRandomVolumeOfType(Volume.Type type)
	{
		for (Map.Entry<String, Volume> v : volumes.entrySet())
		{
			if (v.getValue().getType() == type)
			{
				return v.getKey();
			}
		}

		return null;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	The  unconsumed volume of the given type that is nearest to the head 
	 * 	of the DAG
	 */
	public String getVolumeByType(Volume.Type type, Recipe recipe)
	{
		String result = null;
		recipe.sortSteps(new ProcessLog()); // todo pass in the process log

		for (ProcessStep ps : recipe.getSteps())
		{
			for (String vol : ps.getOutputVolumes())
			{
				if (contains(vol) && getVolume(vol).getType() == type)
				{
					boolean consumed = false;
					for (ProcessStep ps2 : recipe.getSteps())
					{
						if (ps2.getInputVolumes().contains(vol))
						{
							consumed = true;
						}
					}
					if (!consumed)
					{
						return vol;
					}
				}
			}
		}



//		for (Map.Entry<String, Volume> v : volumes.entrySet())
//		{
//			if (v.getValue().getType() == type)
//			{
//				return v.getKey();
//			}
//		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public Collection<String> getVolumes(Volume.Type... t)
	{
		List<Volume.Type> types = Arrays.asList(t);
		Collection<String> result = new HashSet<>();

		for (Map.Entry<String, Volume> v : volumes.entrySet())
		{
			if (types.contains(v.getValue().getType()))
			{
				result.add(v.getKey());
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public void clear()
	{
		this.volumes.clear();
		this.outputVolumes.clear();
	}
}
