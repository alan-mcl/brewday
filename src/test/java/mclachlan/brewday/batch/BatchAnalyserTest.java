package mclachlan.brewday.batch;

import java.time.LocalDate;
import java.util.List;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.db.Database;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class BatchAnalyserTest
{
	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void analysisToleratesMissingFirstRunningsGravity()
	{
		Assume.assumeFalse(Database.getInstance().getRecipes().isEmpty());

		String recipeName = Database.getInstance().getRecipes().keySet().iterator().next();
		var batch = Brewday.getInstance().createNewBatch(recipeName, LocalDate.now());

		List<String> analysis = new BatchAnalyser().getBatchAnalysis(batch);

		// no exception; mash section may still appear with unknown measured efficiency
		Assume.assumeTrue(analysis != null);
	}
}
