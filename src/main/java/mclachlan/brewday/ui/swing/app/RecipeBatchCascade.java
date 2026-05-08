package mclachlan.brewday.ui.swing.app;

import java.util.ArrayList;
import java.util.function.Supplier;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ui.swing.screens.BatchesScreen;
import mclachlan.brewday.ui.swing.screens.RecipesScreen;

/**
 * Mirrors JFX {@code RecipePane} cascade into {@link Batch} recipe references on recipe rename/delete.
 */
public class RecipeBatchCascade implements RecipesScreen.RenameHook, RecipesScreen.DeleteHook
{
	private final DirtyStateService dirtyState;
	private final Supplier<BatchesScreen> batchesScreenSupplier;

	public RecipeBatchCascade(DirtyStateService dirtyState, Supplier<BatchesScreen> batchesScreenSupplier)
	{
		this.dirtyState = dirtyState;
		this.batchesScreenSupplier = batchesScreenSupplier;
	}

	@Override
	public void onRecipeRenamed(String oldName, String newName)
	{
		Database db = Database.getInstance();
		for (Batch batch : db.getBatches().values())
		{
			String r = batch.getRecipe();
			if (r != null && r.equalsIgnoreCase(oldName))
			{
				batch.setRecipe(newName);
				dirtyState.markDirty(batch, "batches", "brewing");
			}
		}
		refreshBatchesScreen();
	}

	@Override
	public void onRecipeDeleted(String deletedName)
	{
		Database db = Database.getInstance();
		for (Batch batch : new ArrayList<>(db.getBatches().values()))
		{
			String r = batch.getRecipe();
			if (r != null && r.equalsIgnoreCase(deletedName))
			{
				db.getBatches().remove(batch.getName());
				dirtyState.markDirty("batches", "brewing");
			}
		}
		refreshBatchesScreen();
	}

	private void refreshBatchesScreen()
	{
		BatchesScreen bs = batchesScreenSupplier.get();
		if (bs != null)
		{
			bs.refresh();
		}
	}
}
