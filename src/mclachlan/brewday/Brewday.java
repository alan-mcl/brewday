package mclachlan.brewday;

import java.util.*;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.ProcessStep;
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
	public Recipe createNewRecipe(String name)
	{
		ArrayList<ProcessStep> steps = new ArrayList<>();

		String equipmentProfile =
			Database.getInstance().getSettings().get(Settings.DEFAULT_EQUIPMENT_PROFILE);

		return new Recipe(name, equipmentProfile, null, steps);
	}
}
