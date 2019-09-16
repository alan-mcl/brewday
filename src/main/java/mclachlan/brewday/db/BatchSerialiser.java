package mclachlan.brewday.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.db.v2.V2Utils;
import mclachlan.brewday.process.Volumes;

/**
 *
 */
public class BatchSerialiser implements V2SerialiserMap<Batch>
{
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy");

	private VolumeSerialiser volumeSerialiser = new VolumeSerialiser();

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(Batch batch)
	{
		Map result = new HashMap();

		result.put("name", batch.getName());
		result.put("description", batch.getDescription());
		result.put("recipe", batch.getRecipe());
		result.put("date", DATE_FORMAT.format(batch.getDate()));
		result.put("measurements",
			V2Utils.serialiseMap(
				batch.getActualVolumes().getVolumes(),
				volumeSerialiser));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Batch fromMap(Map<String, ?> map)
	{
		String name = (String)map.get("name");
		String description = (String)map.get("description");
		String recipe = (String)map.get("recipe");
		String date = (String)map.get("date");
		Map<String, ?> measurements = (Map<String, ?>)map.get("measurements");

		Volumes actualVolumes = new Volumes();

		actualVolumes.setVolumes(V2Utils.deserialiseMap(
			measurements,
			volumeSerialiser));

		try
		{
			return new Batch(
				name,
				description,
				recipe,
				DATE_FORMAT.parse(date),
				actualVolumes);
		}
		catch (ParseException e)
		{
			throw new BrewdayException(e);
		}
	}
}