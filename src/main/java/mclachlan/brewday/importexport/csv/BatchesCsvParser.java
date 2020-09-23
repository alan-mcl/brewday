
/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.importexport.csv;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.Recipe;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 *
 */
public class BatchesCsvParser
{
	public enum CsvFormat
	{
		EXCEL, RFC_4180;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("csv.format." + name());
		}
	}

	/*-------------------------------------------------------------------------*/
	public Map<Class<?>, Map<String, V2DataObject>> parse(
		List<File> files,
		CsvFormat csvFormat,
		Quantity.Unit volumUnit,
		Quantity.Unit densityUnit) throws Exception
	{
		Map<Class<?>, Map<String, V2DataObject>> result = new HashMap<>();

		result.put(Batch.class, new HashMap<>());

		for (File file : files)
		{
			parseFile(file, csvFormat, result, volumUnit, densityUnit);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	private void parseFile(
		File file,
		CsvFormat csvFormat,
		Map<Class<?>, Map<String, V2DataObject>> result,
		Quantity.Unit volumeUnit,
		Quantity.Unit densityUnit) throws IOException
	{
		Reader in = new FileReader(file);

		CSVFormat format;
		switch (csvFormat)
		{
			case EXCEL:
				format = CSVFormat.EXCEL;
				break;
			case RFC_4180:
				format = CSVFormat.RFC4180;
				break;
			default:
				throw new BrewdayException("Unexpected value: " + csvFormat);
		}

		Iterable<CSVRecord> records = format.withFirstRecordAsHeader().parse(in);

		for (CSVRecord record : records)
		{
			// mandatory fields
			String recipeName = record.get("Name");
			String date = record.get("Date");
			double preBoilVol = Double.valueOf(record.get("Meas Pre-Boil Vol"));
			double preBoilGravity = Double.valueOf(record.get("Meas Pre-Boil Gravity"));
			double measOg = Double.valueOf(record.get("Measured OG"));
			double measFg = Double.valueOf(record.get("Measured FG"));
			double measBatchSize = Double.valueOf(record.get("Meas Batch Size"));
			double measBottlingVol = Double.valueOf(record.get("Meas Bottling Vol"));

			Recipe recipe = Database.getInstance().getRecipes().get(recipeName);

			if (recipe != null)
			{
				Batch batch = Brewday.getInstance().createNewBatch(recipe, parseDate(date));

				batch.setDescription(StringUtils.getProcessString("import.csv.batch.desc", LocalDate.now()));

				// measurements
				setBatchMeasurements(
					recipe,
					batch,
					preBoilVol,
					preBoilGravity,
					measOg,
					measFg,
					measBatchSize,
					measBottlingVol,
					volumeUnit,
					densityUnit);

				result.get(Batch.class).put(batch.getName(), batch);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private void setBatchMeasurements(Recipe recipe, Batch batch,
		double preBoilVol,
		double preBoilGravity,
		double measOg,
		double measFg,
		double measBatchSize,
		double measBottlingVol,
		Quantity.Unit volumeUnit,
		Quantity.Unit gravityUnit)
	{
		recipe.run();

		Volumes vols = batch.getActualVolumes();

		for (ProcessStep step : recipe.getSteps())
		{
			if (step instanceof Boil)
			{
				String preBoilVolume = ((Boil)step).getInputWortVolume();
				if (preBoilVolume != null && vols.contains(preBoilVolume))
				{
					Volume vol = vols.getVolume(preBoilVolume);
					if (vol.getType() == Volume.Type.WORT)
					{
						// check for WORT boils to avoid decoction mash boil steps

						vol.setVolume(new VolumeUnit(preBoilVol, volumeUnit, false));
						vol.setGravity(new DensityUnit(preBoilGravity, gravityUnit, false));
					}
				}
			}
			else if (step instanceof Ferment)
			{
				String intoFermenter = ((Ferment)step).getInputVolume();

				ProcessStep prevStep = recipe.getStepProducingVolume(intoFermenter);
				// we only want to do this on the FIRST fermentation step
				if (!(prevStep instanceof Ferment))
				{
					if (vols.contains(intoFermenter))
					{
						Volume vol = vols.getVolume(intoFermenter);
						vol.setVolume(new VolumeUnit(measBatchSize, volumeUnit, false));
						vol.setGravity(new DensityUnit(measOg, gravityUnit, false));

						String postFerm = ((Ferment)step).getOutputVolume();
						if (vols.contains(postFerm))
						{
							vol = vols.getVolume(postFerm);
							vol.setGravity(new DensityUnit(measFg, gravityUnit, false));
						}
					}
				}
			}
			else if (step instanceof PackageStep)
			{
				String beerOutput = ((PackageStep)step).getOutputVolume();
				if (vols.contains(beerOutput))
				{
					Volume vol = vols.getVolume(beerOutput);
					vol.setVolume(new VolumeUnit(measBottlingVol, volumeUnit, false));
					vol.setGravity(new DensityUnit(measFg, gravityUnit, false));
				}
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private LocalDate parseDate(String date)
	{
		DateTimeFormatter[] formatters =
			{
				// sensible ISO stuff
				DateTimeFormatter.ISO_DATE,
				DateTimeFormatter.ISO_LOCAL_DATE,

				// random BeerSmith supported formats
				DateTimeFormatter.ofPattern("dd MMM yyyy"),
				DateTimeFormatter.ofPattern("MM dd yy"),
				DateTimeFormatter.ofPattern("dd MM yy"),
				DateTimeFormatter.ofPattern("yyyy MM dd"),
			};

		String toParse = date.replaceAll("/-\\\\", " ");
		RuntimeException x = null;

		for (int i = 0; i < formatters.length; i++)
		{
			// try a bunch of date formats, return as soon as one works
			try
			{
				return LocalDate.parse(toParse, formatters[i]);
			}
			catch (DateTimeException ignored)
			{
				x = ignored;
			}
		}

		throw x;
	}
}
