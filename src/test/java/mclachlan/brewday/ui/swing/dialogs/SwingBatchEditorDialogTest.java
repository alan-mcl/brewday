package mclachlan.brewday.ui.swing.dialogs;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.batch.BatchVolumeEstimate;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwingBatchEditorDialogTest
{
	@Test
	public void editedMeasurementRowIsBoldUntilCleared() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Database.getInstance().loadAll();
		Assume.assumeFalse(Database.getInstance().getRecipes().isEmpty());

		String recipeName = Database.getInstance().getRecipes().keySet().iterator().next();
		Batch live = Brewday.getInstance().createNewBatch(recipeName, LocalDate.now());
		Database.getInstance().getBatches().put(live.getName(), live);

		DirtyStateService dirty = new DirtyStateService();
		final SwingBatchEditorDialog[] holder = new SwingBatchEditorDialog[1];
		SwingUtilities.invokeAndWait(() ->
			holder[0] = new SwingBatchEditorDialog(new JFrame(), dirty, live));
		SwingBatchEditorDialog editor = holder[0];
		SwingUtilities.invokeAndWait(() -> {});

		int modelRow = firstKeyVolumeMeasurementModelRow(editor);
		Assume.assumeTrue(modelRow >= 0);

		JTable table = editor.getMeasurementsTableForTest();
		AbstractTableModel model = (AbstractTableModel)table.getModel();
		int viewRow = table.convertRowIndexToView(modelRow);
		String volName = volumeNameForModelRow(editor, modelRow);

		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(Font.PLAIN, editor.rowFontStyle(viewRow));
			model.setValueAt("20", modelRow, 4);
		});

		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(Font.BOLD, editor.rowFontStyle(viewRow));
			Volume liveVol = live.getActualVolumes().getVolumes().get(volName);
			Quantity liveMeas = liveVol == null ? null : liveVol.getVolume();
			assertTrue(liveMeas == null || liveMeas.isEstimated());
		});

		SwingUtilities.invokeAndWait(() -> model.setValueAt("", modelRow, 4));

		SwingUtilities.invokeAndWait(() -> assertEquals(Font.PLAIN, editor.rowFontStyle(viewRow)));

		SwingUtilities.invokeAndWait(editor::dispose);
	}

	@Test
	public void cancelDiscardsDraftMeasurementChanges() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Database.getInstance().loadAll();
		Assume.assumeFalse(Database.getInstance().getRecipes().isEmpty());

		String recipeName = Database.getInstance().getRecipes().keySet().iterator().next();
		Batch live = Brewday.getInstance().createNewBatch(recipeName, LocalDate.now());
		Database.getInstance().getBatches().put(live.getName(), live);

		DirtyStateService dirty = new DirtyStateService();
		final SwingBatchEditorDialog[] holder = new SwingBatchEditorDialog[1];
		SwingUtilities.invokeAndWait(() ->
			holder[0] = new SwingBatchEditorDialog(new JFrame(), dirty, live));
		SwingBatchEditorDialog editor = holder[0];
		SwingUtilities.invokeAndWait(() -> {});

		int modelRow = firstKeyVolumeMeasurementModelRow(editor);
		Assume.assumeTrue(modelRow >= 0);

		AbstractTableModel model = (AbstractTableModel)editor.getMeasurementsTableForTest().getModel();
		String volName = volumeNameForModelRow(editor, modelRow);

		SwingUtilities.invokeAndWait(() -> model.setValueAt("20", modelRow, 4));
		SwingUtilities.invokeAndWait(editor::cancelForTest);

		Volume liveVol = live.getActualVolumes().getVolumes().get(volName);
		Quantity liveMeas = liveVol == null ? null : liveVol.getVolume();
		assertTrue(liveMeas == null || liveMeas.isEstimated());
		assertFalse(dirty.isDirty(live));
		assertFalse(dirty.isDirty("batches"));
	}

	@Test
	public void okAppliesDraftMeasurementMarksLiveDirty() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Database.getInstance().loadAll();
		Assume.assumeFalse(Database.getInstance().getRecipes().isEmpty());

		String recipeName = Database.getInstance().getRecipes().keySet().iterator().next();
		Batch live = Brewday.getInstance().createNewBatch(recipeName, LocalDate.now());
		Database.getInstance().getBatches().put(live.getName(), live);

		DirtyStateService dirty = new DirtyStateService();
		final SwingBatchEditorDialog[] holder = new SwingBatchEditorDialog[1];
		SwingUtilities.invokeAndWait(() ->
			holder[0] = new SwingBatchEditorDialog(new JFrame(), dirty, live));
		SwingBatchEditorDialog editor = holder[0];
		SwingUtilities.invokeAndWait(() -> {});

		int modelRow = firstKeyVolumeMeasurementModelRow(editor);
		Assume.assumeTrue(modelRow >= 0);

		AbstractTableModel model = (AbstractTableModel)editor.getMeasurementsTableForTest().getModel();
		String volName = volumeNameForModelRow(editor, modelRow);

		SwingUtilities.invokeAndWait(() -> model.setValueAt("20", modelRow, 4));
		SwingUtilities.invokeAndWait(editor::applyForTest);

		Volume liveVol = live.getActualVolumes().getVolumes().get(volName);
		assertNotNull(liveVol);
		VolumeUnit liveVolQty = liveVol.getVolume();
		assertNotNull(liveVolQty);
		assertFalse(liveVolQty.isEstimated());
		assertEquals(20D, liveVolQty.get(Quantity.Unit.LITRES), 0.01);
		assertTrue(dirty.isDirty(live));
		assertTrue(dirty.isDirty("batches"));

		SwingUtilities.invokeAndWait(editor::dispose);
	}

	private static int firstKeyVolumeMeasurementModelRow(SwingBatchEditorDialog editor)
	{
		List<BatchVolumeEstimate> rows =
			Brewday.getInstance().getBatchVolumeEstimates(editor.getDraftForTest());
		int visibleIdx = 0;
		for (BatchVolumeEstimate bve : rows)
		{
			if (!bve.isKey())
			{
				continue;
			}
			if (BatchVolumeEstimate.MEASUREMENTS_VOLUME.equals(bve.getMetricKey()))
			{
				return visibleIdx;
			}
			visibleIdx++;
		}
		return -1;
	}

	private static String volumeNameForModelRow(SwingBatchEditorDialog editor, int modelRow)
	{
		List<BatchVolumeEstimate> rows =
			Brewday.getInstance().getBatchVolumeEstimates(editor.getDraftForTest());
		int visibleIdx = 0;
		for (BatchVolumeEstimate bve : rows)
		{
			if (!bve.isKey())
			{
				continue;
			}
			if (visibleIdx == modelRow)
			{
				return bve.getVolumeName();
			}
			visibleIdx++;
		}
		throw new IllegalStateException("model row not found: " + modelRow);
	}
}
