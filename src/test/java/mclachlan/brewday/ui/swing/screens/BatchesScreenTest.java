package mclachlan.brewday.ui.swing.screens;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.process.Volumes;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.BatchEditorNavPort;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatchesScreenTest
{
	@Before
	public void assumeDisplay()
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
	}

	@Test
	public void addDuplicateRenameDeleteSaveUndoAndExport() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		FakeDialogPort dialog = new FakeDialogPort();
		FakeBatchEditorNav nav = new FakeBatchEditorNav();
		DirtyStateService dirty = new DirtyStateService();
		BatchesScreen screen = createScreen(dbPort, dialog, dirty,
			new BatchesScreen.NoOpRenameHook(), new BatchesScreen.NoOpDeleteHook(), nav);

		dialog.nextBatch = batch("B1", "R1", LocalDate.of(2024, 1, 1));
		invokeEdt(() -> screen.getAddAction().actionPerformed(null));
		assertEquals(1, dbPort.batches.size());
		assertTrue(dirty.hasDirty());

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.renameResult = "B1Copy";
		invokeEdt(() -> screen.getDuplicateAction().actionPerformed(null));
		assertEquals(2, dbPort.batches.size());

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(1, nav.openCount);
		assertEquals("B1", nav.lastBatchId);

		File csv = File.createTempFile("batches-screen-test", ".csv");
		csv.deleteOnExit();
		dialog.exportFile = csv;
		invokeEdt(() -> screen.getExportAction().actionPerformed(null));
		assertFalse(Files.readAllLines(csv.toPath(), StandardCharsets.UTF_8).isEmpty());

		dialog.confirm = true;
		invokeEdt(() -> screen.getSaveAction().actionPerformed(null));
		assertEquals(1, dbPort.saveCount);
		assertFalse(dirty.hasDirty());

		dirty.markDirty("batches");
		invokeEdt(() -> screen.getUndoAction().actionPerformed(null));
		assertEquals(1, dbPort.loadCount);
		assertFalse(dirty.hasDirty());
	}

	@Test
	public void hotkeysAndFilter() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.batches.put("A", batch("A", "ipa", LocalDate.of(2020, 1, 1)));
		dbPort.batches.put("B", batch("B", "lager", LocalDate.of(2021, 6, 1)));
		BatchesScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService(),
			new BatchesScreen.NoOpRenameHook(), new BatchesScreen.NoOpDeleteHook());

		KeyStroke filterKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F);
		Object filterMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(filterKs);
		assertEquals("batch.hotkey.filterCtrl", filterMap);

		invokeEdt(() -> screen.getFilterAction().actionPerformed(null));
		invokeEdt(() -> screen.getFilterField().setText("lager"));
		assertEquals(1, screen.getTable().getRowCount());

		invokeEdt(() -> screen.getFilterField().setText("A"));
		assertEquals(1, screen.getTable().getRowCount());
		assertEquals("A", screen.getTable().getValueAt(0, 0));
	}

	@Test
	public void dirtyRowsAreBold() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		Batch b = batch("X", "R", LocalDate.now());
		dbPort.batches.put("X", b);
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();
		BatchesScreen screen = createScreen(dbPort, dialog, dirty,
			new BatchesScreen.NoOpRenameHook(), new BatchesScreen.NoOpDeleteHook());

		assertEquals(Font.PLAIN, rowFontStyle(screen, 0));
		dirty.markDirty(b);
		invokeEdt(screen::refresh);
		assertEquals(Font.BOLD, rowFontStyle(screen, 0));
	}

	@Test
	public void renameAndDeleteInvokeHooks() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.batches.put("Old", batch("Old", "R", LocalDate.now()));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.renameResult = "New";
		RecordingRenameHook renameHook = new RecordingRenameHook();
		RecordingDeleteHook deleteHook = new RecordingDeleteHook();
		BatchesScreen screen = createScreen(dbPort, dialog, new DirtyStateService(), renameHook, deleteHook);

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getRenameAction().actionPerformed(null));
		assertEquals("Old", renameHook.oldId);
		assertEquals("New", renameHook.newId);
		assertTrue(dbPort.batches.containsKey("New"));

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.confirm = true;
		invokeEdt(() -> screen.getDeleteAction().actionPerformed(null));
		assertEquals("New", deleteHook.deletedId);
		assertFalse(dbPort.batches.containsKey("New"));
	}

	@Test
	public void defaultSortIsDateDescending() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.batches.put("older", batch("older", "R", LocalDate.of(2019, 1, 1)));
		dbPort.batches.put("newer", batch("newer", "R", LocalDate.of(2022, 1, 1)));
		BatchesScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService(),
			new BatchesScreen.NoOpRenameHook(), new BatchesScreen.NoOpDeleteHook());

		List<? extends RowSorter.SortKey> keys = screen.getRowSorter().getSortKeys();
		assertEquals(1, keys.size());
		assertEquals(4, keys.get(0).getColumn());
		assertEquals(SortOrder.DESCENDING, keys.get(0).getSortOrder());

		final String[] topId = new String[1];
		invokeEdt(() ->
		{
			int modelRow = screen.getTable().convertRowIndexToModel(0);
			topId[0] = (String)((DefaultTableModel)screen.getTable().getModel()).getValueAt(modelRow, 0);
		});
		assertEquals("newer", topId[0]);
	}

	@Test
	public void doubleClickOpensEditStub() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.batches.put("B", batch("B", "R", LocalDate.now()));
		FakeDialogPort dialog = new FakeDialogPort();
		FakeBatchEditorNav nav = new FakeBatchEditorNav();
		BatchesScreen screen = createScreen(dbPort, dialog, new DirtyStateService(),
			new BatchesScreen.NoOpRenameHook(), new BatchesScreen.NoOpDeleteHook(), nav);

		invokeEdt(() ->
		{
			screen.getTable().setRowSelectionInterval(0, 0);
			for (var ml : screen.getTable().getMouseListeners())
			{
				ml.mouseClicked(new MouseEvent(screen.getTable(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
					0, 5, 5, 2, false, MouseEvent.BUTTON1));
			}
		});
		assertEquals(1, nav.openCount);
		assertEquals("B", nav.lastBatchId);
	}

	private BatchesScreen createScreen(FakeDbPort dbPort, FakeDialogPort dialog, DirtyStateService dirty,
		BatchesScreen.RenameHook renameHook, BatchesScreen.DeleteHook deleteHook) throws Exception
	{
		return createScreen(dbPort, dialog, dirty, renameHook, deleteHook, null);
	}

	private BatchesScreen createScreen(FakeDbPort dbPort, FakeDialogPort dialog, DirtyStateService dirty,
		BatchesScreen.RenameHook renameHook, BatchesScreen.DeleteHook deleteHook, BatchEditorNavPort batchEditorNav) throws Exception
	{
		final BatchesScreen[] holder = new BatchesScreen[1];
		invokeEdt(() -> holder[0] = new BatchesScreen(null, dirty, dialog, dbPort, renameHook, deleteHook, batchEditorNav));
		return holder[0];
	}

	private int rowFontStyle(BatchesScreen screen, int row) throws Exception
	{
		final int[] style = new int[1];
		invokeEdt(() -> style[0] = screen.rowFontStyle(row));
		return style[0];
	}

	private static void invokeEdt(Runnable runnable) throws Exception
	{
		SwingUtilities.invokeAndWait(runnable);
	}

	private static Batch batch(String id, String recipe, LocalDate date)
	{
		return new Batch(id, "d", recipe, date, new Volumes(), false);
	}

	private static class RecordingRenameHook implements BatchesScreen.RenameHook
	{
		String oldId;
		String newId;

		@Override
		public void onBatchRenamed(String oldId, String newId)
		{
			this.oldId = oldId;
			this.newId = newId;
		}
	}

	private static class RecordingDeleteHook implements BatchesScreen.DeleteHook
	{
		String deletedId;

		@Override
		public void onBatchDeleted(String batchId)
		{
			this.deletedId = batchId;
		}
	}

	private static class FakeDbPort implements BatchesScreen.DbPort
	{
		private final Map<String, Batch> batches = new LinkedHashMap<>();
		private int saveCount;
		private int loadCount;

		@Override
		public Map<String, Batch> batches()
		{
			return batches;
		}

		@Override
		public void saveAll()
		{
			saveCount++;
		}

		@Override
		public void loadAll()
		{
			loadCount++;
		}
	}

	private static class FakeBatchEditorNav implements BatchEditorNavPort
	{
		private int openCount;
		private String lastBatchId;

		@Override
		public void openBatchEditor(String batchId)
		{
			openCount++;
			lastBatchId = batchId;
		}
	}

	private static class FakeDialogPort implements BatchesScreen.DialogPort
	{
		private Batch nextBatch;
		private String renameResult;
		private boolean confirm = true;
		private File exportFile;

		@Override
		public Batch showNewBatchDialog(javax.swing.JFrame parent)
		{
			return nextBatch;
		}

		@Override
		public String promptName(javax.swing.JFrame parent, String message, String title, String currentName)
		{
			return renameResult;
		}

		@Override
		public boolean confirm(javax.swing.JFrame parent, String message, String title)
		{
			return confirm;
		}

		@Override
		public File chooseExportFile(javax.swing.JFrame parent, File defaultFile)
		{
			return exportFile;
		}

		@Override
		public void writeBatchCsv(File target, java.util.Collection<Batch> batches) throws IOException
		{
			List<String> lines = new ArrayList<>();
			lines.add("Name,Recipe,Date,Description");
			for (Batch b : batches)
			{
				lines.add(b.getName() + ",,,");
			}
			Files.write(target.toPath(), lines, StandardCharsets.UTF_8);
		}

		@Override
		public void showError(javax.swing.JFrame parent, String message, String title)
		{
		}

		@Override
		public void showError(javax.swing.JFrame parent, Throwable throwable, String title)
		{
		}
	}
}
