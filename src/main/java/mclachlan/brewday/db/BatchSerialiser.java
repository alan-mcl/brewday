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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.db;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

	private VolumeSerialiser volumeSerialiser = new VolumeSerialiser();

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(Batch batch, Database db)
	{
		Map result = new HashMap();

		result.put("name", batch.getName());
		result.put("description", batch.getDescription());
		result.put("recipe", batch.getRecipe());
		result.put("date", DATE_FORMAT.format(batch.getDate()));
		result.put("inventoryConsumed", batch.isInventoryConsumed());
		result.put("measurements",
			V2Utils.serialiseMap(
				batch.getActualVolumes().getVolumes(),
				volumeSerialiser, db));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Batch fromMap(Map<String, ?> map, Database db)
	{
		String name = (String)map.get("name");
		String description = (String)map.get("description");
		String recipe = (String)map.get("recipe");
		String date = (String)map.get("date");
		Map<String, ?> measurements = (Map<String, ?>)map.get("measurements");
		Boolean invConsumed = (Boolean)map.get("inventoryConsumed");

		Volumes actualVolumes = new Volumes();

		actualVolumes.setVolumes(V2Utils.deserialiseMap(
			measurements,
			volumeSerialiser, db));

		try
		{
			return new Batch(
				name,
				description,
				recipe,
				LocalDate.parse(date, DATE_FORMAT),
				actualVolumes,
				invConsumed);
		}
		catch (DateTimeParseException e)
		{
			throw new BrewdayException(e);
		}
	}
}
