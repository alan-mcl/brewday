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

package mclachlan.brewday;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.batch.BatchVolumeEstimate;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class Brewday
{
	private static final Brewday instance = new Brewday();
	private final BatchAnalyser batchAnalyser = new BatchAnalyser();
	private final Properties appConfig;

	public static final String BREWDAY_VERSION = "mclachlan.brewday.version";
	public static final String BREWDAY_DB = "mclachlan.brewday.db";

	/*-------------------------------------------------------------------------*/
	public static Brewday getInstance()
	{
		return instance;
	}

	/*-------------------------------------------------------------------------*/
	private Brewday()
	{
		// read app config
		appConfig = new Properties();
		try
		{
			FileInputStream inStream = new FileInputStream("brewday.cfg");
			appConfig.load(inStream);
			inStream.close();
		}
		catch (IOException e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	public Properties getAppConfig()
	{
		return appConfig;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param name
	 * 	The unique name of the new recipe
	 * @param processTemplateName
	 * 	The process template to use for this recipe
	 * @return
	 * 	A new recipe with the given name and configured defaults.
	 */
	public Recipe createNewRecipe(String name, String processTemplateName)
	{
		ArrayList<ProcessStep> steps = new ArrayList<>();

		String equipmentProfile =
			Database.getInstance().getSettings().get(Settings.DEFAULT_EQUIPMENT_PROFILE);

		Recipe template = Database.getInstance().getProcessTemplates().get(processTemplateName);

		Recipe recipe = new Recipe(
			name,
			StringUtils.getUiString("recipe.created.from.process.template", processTemplateName),
			equipmentProfile,
			new ArrayList<>(),
			steps);
		if (template != null)
		{
			recipe.applyProcessTemplate(template);
		}

		return recipe;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Parses the given string and returns a quantity. This method tries to parse
	 * user entered strings and convert them to a sensible unit, using a hint if
	 * available.
	 *
	 * NOTE THIS IS A WORK IN PROGRESS AND VERY BASIC RIGHT NOW
	 *
	 * @param quantityString Whatever junk the user typed in.
	 * @param unitHint A hint as to what the unit type should be; in many cases
	 * 	this is used as a default if the user does not enter a unit type
	 * @return a quantity of the best possible type, or null if this string
	 * 	can't be parsed or does not match the hint.
	 */
	public Quantity parseQuantity(String quantityString, Quantity.Unit unitHint)
	{
		// todo: make this better

		double quantity;
		try
		{
			quantity = Double.parseDouble(quantityString);
		}
		catch (NumberFormatException n)
		{
			try
			{
				quantity = Double.parseDouble(quantityString.replaceAll(",", "."));
			}
			catch (NumberFormatException e)
			{
				return null;
			}
		}

		if (unitHint == Quantity.Unit.GRAMS || unitHint == Quantity.Unit.KILOGRAMS)
		{
			return new WeightUnit(quantity);
		}
		else if (unitHint == Quantity.Unit.CELSIUS)
		{
			return new TemperatureUnit(quantity);
		}
		else if (unitHint == Quantity.Unit.MILLILITRES ||
			unitHint == Quantity.Unit.LITRES)
		{
			return new VolumeUnit(quantity, unitHint);
		}
		else if (unitHint == Quantity.Unit.SPECIFIC_GRAVITY)
		{
			if (quantity < 2D)
			{
				// assume that the user entered a decimal-point string eg "1.024"
				return new DensityUnit(quantity, Quantity.Unit.SPECIFIC_GRAVITY);
			}
			else
			{
				// assume that the user entered a non-decimal point string, eg "1014"
				return new DensityUnit(quantity/1000D, Quantity.Unit.SPECIFIC_GRAVITY);
			}

		}
		else if (unitHint == Quantity.Unit.SRM)
		{
			return new ColourUnit(quantity);
		}
		else if (unitHint == Quantity.Unit.IBU)
		{
			return new BitternessUnit(quantity);
		}
		else
		{
			return null;
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param recipeName
	 * 	The name of the recipe to use
	 * @param date
	 * 	The date of the brew session
	 * @return
	 * 	A new batch of the given recipe, uniquely named, on the given date.
	 */
	public Batch createNewBatch(String recipeName, LocalDate date)
	{
		Recipe recipe = Database.getInstance().getRecipes().get(recipeName);
		return createNewBatch(recipe, date);
	}

	public Batch createNewBatch(Recipe recipe, LocalDate date)
	{
		recipe.run();

		// copy the estimated volumes
		Volumes vols = new Volumes(recipe.getVolumes());

		// null out the fields that need to be measured
		for (Volume v : vols.getVolumes().values())
		{
			v.setMetrics(new HashMap<>());
		}

		String id = recipe.getName()+" (1)";

		// detect duplicates
		if (Database.getInstance().getBatches().get(id) != null)
		{
			id = recipe.getName()+" (%d)";
			int count = 1;
			while (Database.getInstance().getBatches().get(
				String.format(id, count)) != null)
			{
				count++;
			}
			id = String.format(id, count);
		}

		return new Batch(id, StringUtils.getProcessString("batch.new.desc", recipe.getName()), recipe.getName(), date, vols);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Given a batch, return the list of estimates to be considered for analysis.
	 * The list will be sorted in order of recipe process output.
	 */
	public List<BatchVolumeEstimate> getBatchVolumeEstimates(Batch batch)
	{
		List<BatchVolumeEstimate> result = new ArrayList<>();

		Recipe recipe = Database.getInstance().getRecipes().get(batch.getRecipe());

		if (recipe == null)
		{
			// recipe not there, we can't make any estimates
			return result;
		}

		// run the recipe first in case it has no estimates yet
		recipe.run();

		EquipmentProfile equipmentProfile = Database.getInstance().getEquipmentProfiles().get(recipe.getEquipmentProfile());

		// copy over ingredient additions from the recipe to the batch
		Volumes recipeVols = recipe.getVolumes();
		for (Volume v : batch.getActualVolumes().getVolumes().values())
		{
			if (recipeVols.contains(v.getName()))
			{
				v.setIngredientAdditions(recipeVols.getVolume(v.getName()).getIngredientAdditions());
			}
		}

		ProcessLog log = new ProcessLog();
		recipe.sortSteps(log);

		// re-run with the actual volumes
		recipe.run(batch.getActualVolumes(), equipmentProfile, log);

		Set<String> keyVolumes = new HashSet<>();

		//
		// find all the volumes for key measurements as follows
		// - pre-boil volume and gravity
		// - OG at fermentation time
		// - FG and volume at packaging time
		//
		for (ProcessStep step : recipe.getSteps())
		{
			if (step instanceof Boil)
			{
				String preBoil = ((Boil)step).getInputWortVolume();

				// check for WORT type to avoid decoction boils
				if (recipeVols.contains(preBoil) &&
					recipeVols.getVolume(preBoil).getType() == Volume.Type.WORT)
				{
					keyVolumes.add(preBoil);
				}
			}
			else if (step instanceof Ferment)
			{
				// in the case of multiple fermentation stages we only want the first one

				String fermentInput = ((Ferment)step).getInputVolume();

				ProcessStep prevStep = recipe.getStepProducingVolume(fermentInput);
				if (!(prevStep instanceof Ferment))
				{
					keyVolumes.add(fermentInput);
					// need this to work out attenuation
					keyVolumes.add(((Ferment)step).getOutputVolume());
				}
			}
			else if (step instanceof PackageStep)
			{
				keyVolumes.addAll(step.getOutputVolumes());
			}
		}

		//
		// create  all the step volume measurements
		//
		for (ProcessStep step : recipe.getSteps())
		{
			for (String volName : step.getOutputVolumes())
			{
				Volume estVol = recipe.getVolumes().getVolume(volName);
				Volume measuredVol = batch.getActualVolumes().getVolumes().get(volName);

				if (estVol.getType() == Volume.Type.MASH)
				{
					if (measuredVol == null)
					{
						measuredVol = new Volume(volName, Volume.Type.MASH);
						batch.getActualVolumes().addVolume(estVol.getName(), measuredVol);
					}

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_TEMPERATURE,
							estVol.getTemperature(),
							measuredVol.getTemperature(),
							false));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_VOLUME,
							estVol.getVolume(),
							measuredVol.getVolume(),
							false));
				}
				else if (estVol.getType() == Volume.Type.WORT)
				{
					if (measuredVol == null)
					{
						measuredVol = new Volume(volName, Volume.Type.WORT);
						batch.getActualVolumes().addVolume(estVol.getName(), measuredVol);
					}

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_TEMPERATURE,
							estVol.getTemperature(),
							measuredVol.getTemperature(),
							false));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_VOLUME,
							estVol.getVolume(),
							measuredVol.getVolume(),
							keyVolumes.contains(volName)));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_DENSITY,
							estVol.getGravity(),
							measuredVol.getGravity(),
							keyVolumes.contains(volName)));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_COLOUR,
							estVol.getColour(),
							measuredVol.getColour(),
							false));
				}
				else if (estVol.getType() == Volume.Type.BEER)
				{
					if (measuredVol == null)
					{
						measuredVol = new Volume(volName, Volume.Type.BEER);
						batch.getActualVolumes().addVolume(estVol.getName(), measuredVol);
					}

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_VOLUME,
							estVol.getVolume(),
							measuredVol.getVolume(),
							keyVolumes.contains(volName)));

					// not a key metric because it's not needed to work out the attenuation
					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_DENSITY,
							estVol.getGravity(),
							measuredVol.getGravity(),
							keyVolumes.contains(volName)));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_COLOUR,
							estVol.getColour(),
							measuredVol.getColour(),
							false));
				}
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Return a list of strings representing the analysis of estimates vs
	 * measurements for the given batch.
	 *
	 * @param batch
	 * 	The batch to analyse
	 * @return
	 * 	A list of strings. These have already been pulled out of the resource
	 * 	bundle and are ready for rendering on the UI.
	 */
	public List<String> getBatchAnalysis(Batch batch)
	{
		return batchAnalyser.getBatchAnalysis(batch);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	All available recipe tags
	 */
	public List<String> getRecipeTags()
	{
		Set<String> tags = new HashSet<>();

		for (Recipe r : Database.getInstance().getRecipes().values())
		{
			tags.addAll(r.getTags());
		}

		ArrayList<String> result = new ArrayList<>(tags);
		result.sort(String::compareTo);
		return result;
	}
}
