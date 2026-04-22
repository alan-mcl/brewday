package mclachlan.brewday.ui.swing.screens;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
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
		assertTrue(((String)screen.getAddAction().getValue(Action.SHORT_DESCRIPTION)).contains("Ctrl/Cmd+N"));
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

		KeyStroke exportKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X);
		Object exportMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(exportKs);
		assertEquals("water.hotkey.export", exportMap);
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

	private WaterScreen createScreen(FakeDbPort dbPort, FakeDialogPort dialog, DirtyStateService dirty) throws Exception
	{
		final WaterScreen[] holder = new WaterScreen[1];
		invokeEdt(() -> holder[0] = new WaterScreen(null, dirty, dialog, dbPort));
		return holder[0];
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
		private boolean confirm = true;
		private File exportFile;
		private boolean failWrite;
		private String lastErrorMessage;

		@Override
		public Water showEditWaterDialog(javax.swing.JFrame parent, Water current, boolean createMode)
		{
			return nextEditedWater;
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
}
