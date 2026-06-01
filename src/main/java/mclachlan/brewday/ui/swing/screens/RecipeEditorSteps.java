package mclachlan.brewday.ui.swing.screens;

import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.process.BatchSparge;
import mclachlan.brewday.process.Boil;
import mclachlan.brewday.process.Combine;
import mclachlan.brewday.process.Cool;
import mclachlan.brewday.process.Dilute;
import mclachlan.brewday.process.Ferment;
import mclachlan.brewday.process.FlySparge;
import mclachlan.brewday.process.FreezeConcentrate;
import mclachlan.brewday.process.Heat;
import mclachlan.brewday.process.Lauter;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.MashInfusion;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Split;
import mclachlan.brewday.process.Stand;
import mclachlan.brewday.recipe.Recipe;

/**
 * Mirrors JFX {@code RecipeInfoPane} new-step switch.
 */
public final class RecipeEditorSteps
{
	private RecipeEditorSteps()
	{
	}

	public static ProcessStep createStep(Recipe recipe, ProcessStep.Type result)
	{
		return switch (result)
		{
			case BATCH_SPARGE -> new BatchSparge(recipe);
			case FLY_SPARGE -> new FlySparge(recipe);
			case BOIL -> new Boil(recipe);
			case COOL -> new Cool(recipe);
			case HEAT -> new Heat(recipe);
			case DILUTE -> new Dilute(recipe);
			case FERMENT -> new Ferment(recipe);
			case MASH -> new Mash(recipe);
			case STAND -> new Stand(recipe);
			case PACKAGE -> new PackageStep(recipe);
			case LAUTER -> new Lauter(recipe);
			case MASH_INFUSION -> new MashInfusion(recipe);
			case SPLIT -> new Split(recipe);
			case COMBINE -> new Combine(recipe);
			case FREEZE_CONCENTRATE -> new FreezeConcentrate(recipe);
			default -> throw new BrewdayException("invalid " + result);
		};
	}
}
