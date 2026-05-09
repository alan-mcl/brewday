package mclachlan.brewday.ui.swing.app;

/**
 * Opens the Swing batch editor for a batch id (name) in {@link mclachlan.brewday.db.Database#getBatches}.
 */
public interface BatchEditorNavPort
{
	void openBatchEditor(String batchId);
}
