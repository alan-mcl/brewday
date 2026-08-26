package mclachlan.brewday.process;

import java.util.ArrayList;
import java.util.List;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BoilInputVolumeSelectionTest
{
	private static final String MASH_IN = "mash_in";
	private static final String LAUTERED_MASH = "lautered_mash";
	private static final String FIRST_RUNNINGS = "first_runnings";
	private static final String COMBINED_WORT = "combined_wort";
	private static final String SPARGE_RUNNINGS = "sparge_runnings";
	private static final String SPARGE_MASH = "sparge_mash";

	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void boilUsesCombinedWortWhenFirstRunningsRemainUnconsumed()
	{
		Recipe recipe = recipeWithBatchSparge(null);

		Boil boil = new Boil(recipe);

		assertEquals(COMBINED_WORT, boil.getInputWortVolume());
	}

	@Test
	public void boilUsesCombinedWortWhenBatchSpargeConsumesFirstRunnings()
	{
		Recipe recipe = recipeWithBatchSparge(FIRST_RUNNINGS);

		Boil boil = new Boil(recipe);

		assertEquals(COMBINED_WORT, boil.getInputWortVolume());
	}

	private static Recipe recipeWithBatchSparge(String batchSpargeWortInput)
	{
		Lauter lauter = new Lauter(
			"lauter",
			"",
			MASH_IN,
			LAUTERED_MASH,
			FIRST_RUNNINGS);

		BatchSparge batchSparge = new BatchSparge(
			"batch sparge",
			"",
			LAUTERED_MASH,
			batchSpargeWortInput,
			COMBINED_WORT,
			SPARGE_RUNNINGS,
			SPARGE_MASH,
			new ArrayList<>());

		Recipe recipe = new Recipe();
		recipe.setName("BoilInputVolumeSelectionTest");
		List<ProcessStep> steps = new ArrayList<>();
		steps.add(lauter);
		steps.add(batchSparge);
		recipe.setSteps(steps);

		Volumes volumes = recipe.getVolumes();
		volumes.addVolume(MASH_IN, new Volume(Volume.Type.MASH));
		volumes.addVolume(LAUTERED_MASH, new Volume(Volume.Type.MASH));
		volumes.addVolume(FIRST_RUNNINGS, new Volume(Volume.Type.WORT));
		volumes.addVolume(COMBINED_WORT, new Volume(Volume.Type.WORT));
		volumes.addVolume(SPARGE_RUNNINGS, new Volume(Volume.Type.WORT));
		volumes.addVolume(SPARGE_MASH, new Volume(Volume.Type.MASH));

		return recipe;
	}
}
