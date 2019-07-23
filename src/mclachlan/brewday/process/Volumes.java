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
	public String getVolumeByType(Volume.Type type)
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
