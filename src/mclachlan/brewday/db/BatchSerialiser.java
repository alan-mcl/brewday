package mclachlan.brewday.db;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.process.Volumes;

/**
 *
 */
public class BatchSerialiser implements V2SerialiserMap<Batch>
{
	private StepSerialiser stepSerialiser = new StepSerialiser();

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(Batch batch)
	{
		Map result = new HashMap();

		result.put("name", batch.getName());
		result.put("description", batch.getDescription());
		result.put("recipe", batch.getRecipe());
		result.put("date", DateFormat.getDateInstance(DateFormat.MEDIUM).format(batch.getDate()));
		//result.put("volumes", todo );

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

		// todo volumes

		try
		{
			return new Batch(
				name,
				description,
				recipe,
				DateFormat.getDateInstance(DateFormat.MEDIUM).parse(date),
				new Volumes());
		}
		catch (ParseException e)
		{
			throw new BrewdayException(e);
		}
	}
}
