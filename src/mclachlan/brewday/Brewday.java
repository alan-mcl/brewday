package mclachlan.brewday;

import java.text.DateFormat;
import java.util.*;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
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
}
