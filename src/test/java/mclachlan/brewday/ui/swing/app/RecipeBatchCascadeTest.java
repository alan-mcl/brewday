package mclachlan.brewday.ui.swing.app;

import java.time.LocalDate;
import java.util.Iterator;
import javax.swing.SwingUtilities;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ui.swing.screens.BatchesScreen;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RecipeBatchCascadeTest
{
	@Test
	public void recipeRenameUpdatesReferencingBatches() throws Exception
	{
		Database.getInstance().loadAll();
		Assume.assumeTrue(Database.getInstance().getRecipes().size() >= 2);
		Iterator<String> it = Database.getInstance().getRecipes().keySet().iterator();
		String recipeA = it.next();
		String recipeB = it.next();

		Batch batch = Brewday.getInstance().createNewBatch(recipeA, LocalDate.of(2017, 5, 5));
		String batchId = batch.getName();
		Database.getInstance().getBatches().put(batchId, batch);

		DirtyStateService dirty = new DirtyStateService();
		RecipeBatchCascade cascade = new RecipeBatchCascade(dirty, () -> null);
		SwingUtilities.invokeAndWait(() -> cascade.onRecipeRenamed(recipeA, recipeB));

		assertEquals(recipeB, batch.getRecipe());
		assertTrue(dirty.isDirty(batch));

		Database.getInstance().getBatches().remove(batchId);
	}

	@Test
	public void recipeDeleteRemovesReferencingBatches() throws Exception
	{
		Database.getInstance().loadAll();
		Assume.assumeFalse(Database.getInstance().getRecipes().isEmpty());
		String recipeName = Database.getInstance().getRecipes().keySet().iterator().next();

		Batch batch = Brewday.getInstance().createNewBatch(recipeName, LocalDate.of(2016, 4, 4));
		String batchId = batch.getName();
		Database.getInstance().getBatches().put(batchId, batch);
		assertNotNull(Database.getInstance().getBatches().get(batchId));

		DirtyStateService dirty = new DirtyStateService();
		final int[] refreshes = new int[1];
		final BatchesScreen[] counter = new BatchesScreen[1];
		SwingUtilities.invokeAndWait(() ->
			counter[0] = new BatchesScreen(null, dirty)
			{
				@Override
				public void refresh()
				{
					refreshes[0]++;
					super.refresh();
				}
			});
		int refreshCountAfterConstruct = refreshes[0];
		RecipeBatchCascade cascade = new RecipeBatchCascade(dirty, () -> counter[0]);
		SwingUtilities.invokeAndWait(() -> cascade.onRecipeDeleted(recipeName));

		assertFalse(Database.getInstance().getBatches().containsKey(batchId));
		assertTrue(dirty.isDirty("batches"));
		assertEquals(refreshCountAfterConstruct + 1, refreshes[0]);
	}
}
