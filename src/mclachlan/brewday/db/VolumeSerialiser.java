package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.BeerVolume;
import mclachlan.brewday.process.MashVolume;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.process.WortVolume;

/**
 *
 */
public class VolumeSerialiser implements V2SerialiserMap<Volume>
{
	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(Volume volume)
	{
		Map result = new HashMap();

		result.put("name", volume.getName());
		result.put("type", volume.getType().name());

		if (volume instanceof MashVolume)
		{
			if (((MashVolume)volume).getVolume() != null)
			{
				result.put("volume", ((MashVolume)volume).getVolume().get());
			}
			if (((MashVolume)volume).getTemperature() != null)
			{
				result.put("temperature", ((MashVolume)volume).getTemperature().get());
			}
			if (((MashVolume)volume).getColour() != null)
			{
				result.put("colour", ((MashVolume)volume).getColour().get());
			}
			if (((MashVolume)volume).getGravity() != null)
			{
				result.put("gravity", ((MashVolume)volume).getGravity().get());
			}
		}
		else if (volume instanceof WortVolume)
		{
			if (((WortVolume)volume).getFermentability() != null)
			{
				result.put("fermentability", ((WortVolume)volume).getFermentability().name());
			}
			if (((WortVolume)volume).getBitterness() != null)
			{
				result.put("bitterness", ((WortVolume)volume).getBitterness().get());
			}
			if (((WortVolume)volume).getVolume() != null)
			{
				result.put("volume", ((WortVolume)volume).getVolume().get());
			}

			result.put("abv", ((WortVolume)volume).getAbv());

			if (((WortVolume)volume).getColour() != null)
			{
				result.put("colour", ((WortVolume)volume).getColour().get());
			}
			if (((WortVolume)volume).getTemperature() != null)
			{
				result.put("temperature", ((WortVolume)volume).getTemperature().get());
			}
			if (((WortVolume)volume).getGravity() != null)
			{
				result.put("gravity", ((WortVolume)volume).getGravity().get());
			}
		}
		else if (volume instanceof BeerVolume)
		{
			if (((BeerVolume)volume).getBitterness() != null)
			{
				result.put("bitterness", ((BeerVolume)volume).getBitterness().get());
			}
			if (((BeerVolume)volume).getVolume() != null)
			{
				result.put("volume", ((BeerVolume)volume).getVolume().get());
			}

			result.put("abv", ((BeerVolume)volume).getAbv());

			if (((BeerVolume)volume).getColour() != null)
			{
				result.put("colour", ((BeerVolume)volume).getColour().get());
			}
			if (((BeerVolume)volume).getTemperature() != null)
			{
				result.put("temperature", ((BeerVolume)volume).getTemperature().get());
			}
			if (((BeerVolume)volume).getGravity() != null)
			{
				result.put("gravity", ((BeerVolume)volume).getGravity().get());
			}
			if (((BeerVolume)volume).getOriginalGravity() != null)
			{
				result.put("originalGravity", ((BeerVolume)volume).getOriginalGravity().get());
			}
		}
		else
		{
			throw new BrewdayException("invalid "+volume);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Volume fromMap(Map<String, ?> map)
	{
		Volume result;

		String name = (String)map.get("name");
		Volume.Type type = Volume.Type.valueOf((String)map.get("type"));

		switch (type)
		{
			case MASH:
				result = new MashVolume(
					map.containsKey("volume") ? new VolumeUnit((Double)map.get("volume")) : null,
					null,
					null,
					map.containsKey("temperature") ? new TemperatureUnit((Double)map.get("temperature")) : null,
					map.containsKey("gravity") ? new DensityUnit((Double)map.get("gravity")) : null,
					map.containsKey("colour") ? new ColourUnit((Double)map.get("colour")) : null,
					null);
				break;

			case WORT:
				result = new WortVolume(
					map.containsKey("volume") ? new VolumeUnit((Double)map.get("volume")) : null,
					map.containsKey("temperature") ? new TemperatureUnit((Double)map.get("temperature")) : null,
					map.containsKey("fermentability") ? WortVolume.Fermentability.valueOf((String)map.get("fermentability")) : null,
					map.containsKey("gravity") ? new DensityUnit((Double)map.get("gravity")) : null,
					(Double)map.get("abv"),
					map.containsKey("colour") ? new ColourUnit((Double)map.get("colour")) : null,
					map.containsKey("bitterness") ? new BitternessUnit((Double)map.get("bitterness")) : null);
				break;

			case BEER:
				result = new BeerVolume(
					map.containsKey("volume") ? new VolumeUnit((Double)map.get("volume")) : null,
					map.containsKey("temperature") ? new TemperatureUnit((Double)map.get("temperature")) : null,
					map.containsKey("originalGravity") ? new DensityUnit((Double)map.get("originalGravity")) : null,
					map.containsKey("gravity") ? new DensityUnit((Double)map.get("gravity")) : null,
					(Double)map.get("abv"),
					map.containsKey("colour") ? new ColourUnit((Double)map.get("colour")) : null,
					map.containsKey("bitterness") ? new BitternessUnit((Double)map.get("bitterness")) : null);
				break;

			default:
				throw new BrewdayException("Invalid "+type);
		}

		result.setName(name);

		return result;
	}
}
