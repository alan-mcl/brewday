package mclachlan.brewday.ui.swing.screens;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.Font;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WaterScreenTest
{
	@Test
	public void tablePopulatesFromDbPort() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("Melbourne", water("Melbourne", 20, 12, 10, 16, 7.2));
		WaterScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService());

		assertEquals(1, screen.getModel().getRowCount());
		assertEquals("Melbourne", screen.getModel().getValueAt(0, 0));
	}

	@Test
	public void nullQuantitiesRenderAsBlank() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		Water water = new Water("Blanky");
		dbPort.waters.put("Blanky", water);
		WaterScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService());

		assertEquals("", screen.getModel().getValueAt(0, 1));
		assertEquals("", screen.getModel().getValueAt(0, 2));
		assertEquals("", screen.getModel().getValueAt(0, 5));
	}

	@Test
	public void addEditDeleteSaveUndoAndExport() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();
		WaterScreen screen = createScreen(dbPort, dialog, dirty);

		dialog.nextEditedWater = water("Sydney", 30, 40, 50, 60, 7.4);
		invokeEdt(() -> screen.getAddAction().actionPerformed(null));
		assertEquals(1, dbPort.waters.size());
		assertTrue(dirty.hasDirty());

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.nextEditedWater = water("Sydney", 31, 41, 51, 61, 7.5);
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(31D, dbPort.waters.get("Sydney").getCalcium().get(), 0.0001);

		File csv = File.createTempFile("water-screen-test", ".csv");
		csv.deleteOnExit();
		dialog.exportFile = csv;
		invokeEdt(() -> screen.getExportAction().actionPerformed(null));
		assertFalse(Files.readAllLines(csv.toPath(), StandardCharsets.UTF_8).isEmpty());
		assertEquals(1, dialog.exportedWaterNames.size());
		assertEquals("Sydney", dialog.exportedWaterNames.get(0));

		dialog.confirm = true;
		invokeEdt(() -> screen.getSaveAction().actionPerformed(null));
		assertEquals(1, dbPort.saveCount);
		assertFalse(dirty.hasDirty());

		dirty.markDirty("water");
		invokeEdt(() -> screen.getUndoAction().actionPerformed(null));
		assertEquals(1, dbPort.loadCount);
		assertFalse(dirty.hasDirty());

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getDeleteAction().actionPerformed(null));
		assertEquals(0, dbPort.waters.size());
	}

	@Test
	public void exportErrorShowsDialogError() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("Err", water("Err", 10, 10, 10, 10, 7.0));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.failWrite = true;
		dialog.exportFile = File.createTempFile("water-screen-test-err", ".csv");
		dialog.exportFile.deleteOnExit();
		WaterScreen screen = createScreen(dbPort, dialog, new DirtyStateService());

		invokeEdt(() -> screen.getExportAction().actionPerformed(null));
		assertNotNull(dialog.lastErrorMessage);
	}

	@Test
	public void hotkeysAndTooltipsAreWired() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("Hotkey", water("Hotkey", 20, 20, 20, 20, 7.1));
		WaterScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService());
		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));

		assertEquals("Add New", screen.getAddAction().getValue(Action.NAME));
		assertEquals("Delete", screen.getDeleteAction().getValue(Action.NAME));
		assertEquals("Filter", screen.getFilterAction().getValue(Action.NAME));
		assertEquals("Rename", screen.getRenameAction().getValue(Action.NAME));
		assertTrue(((String)screen.getAddAction().getValue(Action.SHORT_DESCRIPTION)).contains("Ctrl/Cmd+N"));
		assertTrue(((String)screen.getRenameAction().getValue(Action.SHORT_DESCRIPTION)).contains("Ctrl/Cmd+R"));
		assertTrue(((String)screen.getFilterAction().getValue(Action.SHORT_DESCRIPTION)).contains("Ctrl/Cmd+F"));
		assertTrue(((String)screen.getExportAction().getValue(Action.SHORT_DESCRIPTION)).contains("Ctrl/Cmd+X"));

		KeyStroke saveKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_S);
		Object saveMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(saveKs);
		assertEquals("water.hotkey.save", saveMap);

		KeyStroke undoUKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_U);
		Object undoUMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(undoUKs);
		assertEquals("water.hotkey.undoU", undoUMap);

		KeyStroke undoZKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_Z);
		Object undoZMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(undoZKs);
		assertEquals("water.hotkey.undoZ", undoZMap);

		KeyStroke deleteKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_D);
		Object deleteMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(deleteKs);
		assertEquals("water.hotkey.deleteCtrl", deleteMap);

		KeyStroke renameKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_R);
		Object renameMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(renameKs);
		assertEquals("water.hotkey.renameCtrl", renameMap);
		Object renameF2Map = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
		assertEquals("water.hotkey.renameF2", renameF2Map);

		KeyStroke filterKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F);
		Object filterMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(filterKs);
		assertEquals("water.hotkey.filterCtrl", filterMap);

		KeyStroke exportKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X);
		Object exportMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(exportKs);
		assertEquals("water.hotkey.export", exportMap);

		Object exportWindowMap = screen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(exportKs);
		assertEquals("water.hotkey.export.window", exportWindowMap);
	}

	@Test
	public void headerTooltipsIncludeUnits() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("Tooltip", water("Tooltip", 10, 11, 12, 13, 7.1));
		WaterScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService());

		String nameTip = tooltipForColumn(screen, 0);
		String calciumTip = tooltipForColumn(screen, 1);
		String phTip = tooltipForColumn(screen, 5);
		assertTrue(nameTip.toLowerCase().contains("text"));
		assertTrue(calciumTip.toLowerCase().contains("ppm"));
		assertTrue(phTip.toLowerCase().contains("ph"));
	}

	@Test
	public void enterAndDoubleClickTriggerEdit() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("EditMe", water("EditMe", 10, 10, 10, 10, 7.0));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.nextEditedWater = water("EditMe", 99, 10, 10, 10, 7.0);
		WaterScreen screen = createScreen(dbPort, dialog, new DirtyStateService());
		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));

		invokeEdt(() ->
		{
			Object actionKey = screen.getTable().getInputMap(JComponent.WHEN_FOCUSED)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
			Action action = screen.getTable().getActionMap().get(actionKey);
			action.actionPerformed(null);
		});
		assertEquals(99D, dbPort.waters.get("EditMe").getCalcium().get(), 0.0001);

		dialog.nextEditedWater = water("EditMe", 123, 10, 10, 10, 7.0);
		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() ->
		{
			MouseEvent evt = new MouseEvent(screen.getTable(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 5, 5, 2, false);
			screen.getTable().dispatchEvent(evt);
		});
		assertEquals(123D, dbPort.waters.get("EditMe").getCalcium().get(), 0.0001);
	}

	@Test
	public void enterInTableDoesNotEditWithoutSelection() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("EditMe", water("EditMe", 10, 10, 10, 10, 7.0));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.nextEditedWater = water("EditMe", 99, 10, 10, 10, 7.0);
		WaterScreen screen = createScreen(dbPort, dialog, new DirtyStateService());

		invokeEdt(() ->
		{
			Object actionKey = screen.getTable().getInputMap(JComponent.WHEN_FOCUSED)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
			javax.swing.Action action = screen.getTable().getActionMap().get(actionKey);
			action.actionPerformed(null);
		});

		assertEquals(10D, dbPort.waters.get("EditMe").getCalcium().get(), 0.0001);
	}

	@Test
	public void filterIsHiddenByDefaultAndCanBeShown() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("Melbourne", water("Melbourne", 20, 12, 10, 16, 7.2));
		dbPort.waters.put("Sydney", water("Sydney", 30, 40, 50, 60, 7.4));
		WaterScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService());

		assertFalse(screen.isFilterPanelVisible());
		invokeEdt(() -> screen.getFilterAction().actionPerformed(null));
		assertTrue(screen.isFilterPanelVisible());
	}

	@Test
	public void filterNarrowingAndEscapeHidesWork() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("Melbourne", water("Melbourne", 20, 12, 10, 16, 7.2));
		dbPort.waters.put("Sydney", water("Sydney", 30, 40, 50, 60, 7.4));
		WaterScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService());
		invokeEdt(() -> screen.getFilterAction().actionPerformed(null));
		assertEquals(2, screen.getTable().getRowCount());

		invokeEdt(() -> screen.getFilterField().setText("mel"));
		assertEquals(1, screen.getTable().getRowCount());
		assertEquals("Melbourne", screen.getTable().getValueAt(0, 0));

		invokeEdt(() ->
		{
			Object actionKey = screen.getFilterField().getInputMap(JComponent.WHEN_FOCUSED)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
			Action action = screen.getFilterField().getActionMap().get(actionKey);
			action.actionPerformed(null);
		});
		assertEquals("", screen.getFilterField().getText());
		assertEquals(2, screen.getTable().getRowCount());
		assertFalse(screen.isFilterPanelVisible());
	}

	@Test
	public void filterHotkeysArePresent() throws Exception
	{
		WaterScreen screen = createScreen(new FakeDbPort(), new FakeDialogPort(), new DirtyStateService());

		Object ctrlActionKey = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
			.get(ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F));
		assertEquals("water.hotkey.filterCtrl", ctrlActionKey);
		assertNotNull(screen.getActionMap().get(ctrlActionKey));

		Object altActionKey = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
			.get(KeyStroke.getKeyStroke(KeyEvent.VK_F, java.awt.event.InputEvent.ALT_DOWN_MASK));
		assertEquals("water.hotkey.filterAlt", altActionKey);
		assertNotNull(screen.getActionMap().get(altActionKey));
	}

	@Test
	public void exportUsesOnlyFilteredRows() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("Melbourne", water("Melbourne", 20, 12, 10, 16, 7.2));
		dbPort.waters.put("Sydney", water("Sydney", 30, 40, 50, 60, 7.4));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.exportFile = File.createTempFile("water-screen-filter-export", ".csv");
		dialog.exportFile.deleteOnExit();
		WaterScreen screen = createScreen(dbPort, dialog, new DirtyStateService());

		invokeEdt(() -> screen.getFilterAction().actionPerformed(null));
		invokeEdt(() -> screen.getFilterField().setText("syd"));
		invokeEdt(() -> screen.getExportAction().actionPerformed(null));

		assertEquals(1, dialog.exportedWaterNames.size());
		assertEquals("Sydney", dialog.exportedWaterNames.get(0));
	}

	@Test
	public void ctrlXOnFocusedFilterTriggersExport() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("Melbourne", water("Melbourne", 20, 12, 10, 16, 7.2));
		dbPort.waters.put("Sydney", water("Sydney", 30, 40, 50, 60, 7.4));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.exportFile = File.createTempFile("water-screen-filter-focused-export", ".csv");
		dialog.exportFile.deleteOnExit();
		WaterScreen screen = createScreen(dbPort, dialog, new DirtyStateService());

		invokeEdt(() ->
		{
			screen.getFilterAction().actionPerformed(null);
			screen.getFilterField().setText("syd");
		});
		invokeEdt(() ->
		{
			Object actionKey = screen.getFilterField().getInputMap(JComponent.WHEN_FOCUSED)
				.get(ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X));
			Action action = screen.getFilterField().getActionMap().get(actionKey);
			action.actionPerformed(null);
		});

		assertEquals(1, dialog.exportedWaterNames.size());
		assertEquals("Sydney", dialog.exportedWaterNames.get(0));
	}

	@Test
	public void renameRekeysMapAndInvokesHook() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("Old", water("Old", 20, 12, 10, 16, 7.2));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.renameResult = "New";
		FakeRenameHook renameHook = new FakeRenameHook();
		WaterScreen screen = createScreen(dbPort, dialog, new DirtyStateService(), renameHook);

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getRenameAction().actionPerformed(null));

		assertEquals(false, dbPort.waters.containsKey("Old"));
		assertEquals(true, dbPort.waters.containsKey("New"));
		assertEquals("Old", renameHook.oldName);
		assertEquals("New", renameHook.newName);
	}

	@Test
	public void renameRejectsDuplicateName() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("Old", water("Old", 20, 12, 10, 16, 7.2));
		dbPort.waters.put("Taken", water("Taken", 30, 12, 10, 16, 7.2));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.renameResult = "Taken";
		FakeRenameHook renameHook = new FakeRenameHook();
		WaterScreen screen = createScreen(dbPort, dialog, new DirtyStateService(), renameHook);

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getRenameAction().actionPerformed(null));

		assertEquals(true, dbPort.waters.containsKey("Old"));
		assertEquals(true, dbPort.waters.containsKey("Taken"));
		assertNotNull(dialog.lastErrorMessage);
		assertEquals(null, renameHook.oldName);
	}

	@Test
	public void dirtyRowsAreBoldAndClearOnSaveUndo() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waters.put("DirtyMe", water("DirtyMe", 20, 12, 10, 16, 7.2));
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();
		WaterScreen screen = createScreen(dbPort, dialog, dirty);

		assertEquals(Font.PLAIN, rowFontStyle(screen, 0));

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.nextEditedWater = water("DirtyMe", 31, 12, 10, 16, 7.2);
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(Font.BOLD, rowFontStyle(screen, 0));

		dialog.confirm = true;
		invokeEdt(() -> screen.getSaveAction().actionPerformed(null));
		assertEquals(Font.PLAIN, rowFontStyle(screen, 0));

		dialog.nextEditedWater = water("DirtyMe", 32, 12, 10, 16, 7.2);
		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(Font.BOLD, rowFontStyle(screen, 0));

		invokeEdt(() -> screen.getUndoAction().actionPerformed(null));
		assertEquals(Font.PLAIN, rowFontStyle(screen, 0));
	}

	private WaterScreen createScreen(FakeDbPort dbPort, FakeDialogPort dialog, DirtyStateService dirty) throws Exception
	{
		final WaterScreen[] holder = new WaterScreen[1];
		invokeEdt(() -> holder[0] = new WaterScreen(null, dirty, dialog, dbPort));
		return holder[0];
	}

	private WaterScreen createScreen(FakeDbPort dbPort, FakeDialogPort dialog, DirtyStateService dirty, WaterScreen.RenameHook renameHook) throws Exception
	{
		final WaterScreen[] holder = new WaterScreen[1];
		invokeEdt(() -> holder[0] = new WaterScreen(null, dirty, dialog, dbPort, renameHook));
		return holder[0];
	}

	private String tooltipForColumn(WaterScreen screen, int modelColumn) throws Exception
	{
		final String[] tip = new String[1];
		invokeEdt(() ->
		{
			int viewColumn = screen.getTable().convertColumnIndexToView(modelColumn);
			javax.swing.table.JTableHeader header = screen.getTable().getTableHeader();
			java.awt.Rectangle rect = header.getHeaderRect(viewColumn);
			MouseEvent event = new MouseEvent(header, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
				rect.x + Math.max(1, rect.width / 2), Math.max(1, rect.height / 2), 0, false);
			tip[0] = header.getToolTipText(event);
		});
		return tip[0];
	}

	private int rowFontStyle(WaterScreen screen, int row) throws Exception
	{
		final int[] style = new int[1];
		invokeEdt(() -> style[0] = screen.rowFontStyle(row));
		return style[0];
	}

	private static void invokeEdt(Runnable runnable) throws Exception
	{
		SwingUtilities.invokeAndWait(runnable);
	}

	private Water water(String name, double ca, double hco3, double so4, double cl, double ph)
	{
		Water water = new Water(name);
		water.setCalcium(new PpmUnit(ca));
		water.setBicarbonate(new PpmUnit(hco3));
		water.setSulfate(new PpmUnit(so4));
		water.setChloride(new PpmUnit(cl));
		water.setSodium(new PpmUnit(12D));
		water.setMagnesium(new PpmUnit(8D));
		water.setPh(new PhUnit(ph));
		water.setDescription("desc");
		return water;
	}

	private static class FakeDbPort implements WaterScreen.DbPort
	{
		private final Map<String, Water> waters = new LinkedHashMap<>();
		private int saveCount;
		private int loadCount;

		@Override
		public Map<String, Water> waters()
		{
			return waters;
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

	private static class FakeDialogPort implements WaterScreen.DialogPort
	{
		private Water nextEditedWater;
		private String renameResult;
		private boolean confirm = true;
		private File exportFile;
		private boolean failWrite;
		private String lastErrorMessage;
		private final List<String> exportedWaterNames = new ArrayList<>();

		@Override
		public Water showEditWaterDialog(javax.swing.JFrame parent, Water current, boolean createMode)
		{
			return nextEditedWater;
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
		public void writeCsv(File target, java.util.Collection<Water> waters) throws IOException
		{
			exportedWaterNames.clear();
			for (Water water : waters)
			{
				exportedWaterNames.add(water.getName());
			}
			if (failWrite)
			{
				throw new IOException("synthetic write failure");
			}
			Files.write(target.toPath(), java.util.List.of("Name,Calcium", "X,1"), StandardCharsets.UTF_8);
		}

		@Override
		public void showError(javax.swing.JFrame parent, String message, String title)
		{
			lastErrorMessage = message;
		}
	}

	private static class FakeRenameHook implements WaterScreen.RenameHook
	{
		private String oldName;
		private String newName;

		@Override
		public void onWaterRenamed(String oldName, String newName)
		{
			this.oldName = oldName;
			this.newName = newName;
		}
	}
}
