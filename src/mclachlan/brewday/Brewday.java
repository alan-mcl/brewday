package mclachlan.brewday;

import java.util.*;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.batch.BatchVolumeEstimate;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.StringUtils.getUiString;

/**
 *
 */
public class Brewday
{
	private static Brewday instance = new Brewday();

	public static Brewday getInstance()
	{
		return instance;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param name
	 * 	The unique name of the new recipe
	 * @return
	 * 	A new recipe with the given name and configured defaults.
	 */
	public Recipe createNewRecipe(String name)
	{
		ArrayList<ProcessStep> steps = new ArrayList<>();

		String equipmentProfile =
			Database.getInstance().getSettings().get(Settings.DEFAULT_EQUIPMENT_PROFILE);

		return new Recipe(name, equipmentProfile, steps);
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
	public Batch createNewBatch(String recipeName, Date date)
	{
		Recipe recipe = Database.getInstance().getRecipes().get(recipeName);
		recipe.run();

		// copy the estimated volumes
		Volumes vols = new Volumes(recipe.getVolumes());

		// null out the fields that need to be measured
		for (Volume v : vols.getVolumes().values())
		{
			v.setTemperature(null);
			v.setColour(null);
			v.setGravity(null);
			v.setIngredientAdditions(new ArrayList<>());
			v.setVolume(null);
			v.setAbv(null);
			v.setOriginalGravity(null);
			v.setFermentability(null);
			v.setBitterness(null);
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

		return new Batch(id, StringUtils.getProcessString("batch.new.desc", recipeName), recipeName, date, vols);
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
	public Quantity getQuantity(String quantityString, Quantity.Unit unitHint)
	{
		// todo: make this better

		double quantity = Double.parseDouble(quantityString);

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
	 * Given a batch, return the list of estimates to be considered for analysis.
	 * The list will be sorted in order of recipe process output.
	 */
	public List<BatchVolumeEstimate> getBatchVolumeEstimates(Batch batch)
	{
		Recipe recipe = Database.getInstance().getRecipes().get(batch.getRecipe());

		List<BatchVolumeEstimate> result = new ArrayList<>();

		ProcessLog log = new ProcessLog();
		recipe.sortSteps(log);

		for (ProcessStep step : recipe.getSteps())
		{
			for (String outputVolume : step.getOutputVolumes())
			{
				Volume estVol = recipe.getVolumes().getVolume(outputVolume);
				Volume measuredVol = batch.getActualVolumes().getVolumes().get(outputVolume);

				if (estVol.getType() == Volume.Type.MASH)
				{
					if (measuredVol == null)
					{
						measuredVol = new Volume(outputVolume, Volume.Type.MASH);
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
						measuredVol = new Volume(outputVolume, Volume.Type.WORT);
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
							true));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_DENSITY,
							estVol.getGravity(),
							measuredVol.getGravity(),
							true));

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
						measuredVol = new Volume(outputVolume, Volume.Type.BEER);
						batch.getActualVolumes().addVolume(estVol.getName(), measuredVol);
					}

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_VOLUME,
							estVol.getVolume(),
							measuredVol.getVolume(),
							true));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_DENSITY,
							estVol.getGravity(),
							measuredVol.getGravity(),
							true));

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
		List<String> result = new ArrayList<>();
		Recipe recipe = Database.getInstance().getRecipes().get(batch.getRecipe());
		Set<String> outputVolumes = recipe.getVolumes().getOutputVolumes();

		for (String outputVolume : outputVolumes)
		{
			Volume estV = recipe.getVolumes().getVolume(outputVolume);
			Volume measV = batch.getActualVolumes().getVolume(outputVolume);

			PercentageUnit measuredAbv = null;

			if (measV.getGravity() != null)
			{
				measuredAbv = Equations.calcAvbWithGravityChange(
					measV.getOriginalGravity(),
					measV.getGravity());
			}

			result.add(getUiString("batch.analysis.packaged", estV.getName()));
			result.add(getUiString("batch.analysis.abv",
				estV.getAbv().get()*100,
				measuredAbv==null ?
					getUiString("quantity.unknown")
					: getUiString("quantity.percent", measuredAbv.get()*100)));
		}

		return result;
	}
}
