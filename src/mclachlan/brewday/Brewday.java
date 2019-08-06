package mclachlan.brewday;

import java.text.DateFormat;
import java.util.*;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volumes;
import mclachlan.brewday.recipe.Recipe;

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

		return new Recipe(name, equipmentProfile, null, steps);
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

		Volumes vols = new Volumes(recipe.getVolumes());

		String dateStr = DateFormat.getDateInstance().format(date);

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

		int quantity = Integer.parseInt(quantityString);

		if (unitHint == Quantity.Unit.GRAMS || unitHint == Quantity.Unit.KILOGRAMS)
		{
			return new WeightUnit(quantity);
		}
		else if (unitHint == Quantity.Unit.CELSIUS)
		{
			return new TemperatureUnit(quantity);
		}
		else if (unitHint == Quantity.Unit.MILLILITRES)
		{
			return new VolumeUnit(quantity);
		}
		else if (unitHint == Quantity.Unit.SPECIFIC_GRAVITY)
		{
			return new DensityUnit(quantity);
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
}
