package mclachlan.brewday.ui.swing.screens;

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
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
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.math.WaterParameters;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WaterParametersScreenTest
{
	@Test
	public void tablePopulatesFromDbPort() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waterParameters.put("Pilsner", profile("Pilsner", 10, 30));
		WaterParametersScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService());

		assertEquals(1, screen.getModel().getRowCount());
		assertEquals("Pilsner", screen.getModel().getValueAt(0, 0));
	}

	@Test
	public void addEditDeleteSaveUndoAndExport() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();
		WaterParametersScreen screen = createScreen(dbPort, dialog, dirty);

		dialog.nextEdited = profile("Sydney", 20, 40);
		invokeEdt(() -> screen.getAddAction().actionPerformed(null));
		assertEquals(1, dbPort.waterParameters.size());
		assertTrue(dirty.hasDirty());

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.nextEdited = profile("Sydney", 21, 41);
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(21D, dbPort.waterParameters.get("Sydney").getMinCalcium().get(), 0.0001);

		File csv = File.createTempFile("water-parameters-screen-test", ".csv");
		csv.deleteOnExit();
		dialog.exportFile = csv;
		invokeEdt(() -> screen.getExportAction().actionPerformed(null));
		assertFalse(Files.readAllLines(csv.toPath(), StandardCharsets.UTF_8).isEmpty());
		assertEquals(1, dialog.exportedNames.size());
		assertEquals("Sydney", dialog.exportedNames.get(0));

		dialog.confirm = true;
		invokeEdt(() -> screen.getSaveAction().actionPerformed(null));
		assertEquals(1, dbPort.saveCount);
		assertFalse(dirty.hasDirty());

		dirty.markDirty("water.parameters");
		invokeEdt(() -> screen.getUndoAction().actionPerformed(null));
		assertEquals(1, dbPort.loadCount);
		assertFalse(dirty.hasDirty());

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getDeleteAction().actionPerformed(null));
		assertEquals(0, dbPort.waterParameters.size());
	}

	@Test
	public void hotkeysAndTooltipsAreWired() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waterParameters.put("Hotkey", profile("Hotkey", 10, 20));
		WaterParametersScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService());
		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));

		assertEquals("Add New", screen.getAddAction().getValue(Action.NAME));
		assertEquals("Delete", screen.getDeleteAction().getValue(Action.NAME));
		assertEquals("Filter", screen.getFilterAction().getValue(Action.NAME));
		assertEquals("Rename", screen.getRenameAction().getValue(Action.NAME));
		assertTrue(((String)screen.getExportAction().getValue(Action.SHORT_DESCRIPTION)).contains("Ctrl/Cmd+X"));

		KeyStroke exportKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X);
		Object exportMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(exportKs);
		assertEquals("water.parameters.hotkey.export", exportMap);
	}

	@Test
	public void filterBehaviorAndExportUsesVisibleRows() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waterParameters.put("Melbourne", profile("Melbourne", 20, 40));
		dbPort.waterParameters.put("Sydney", profile("Sydney", 30, 50));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.exportFile = File.createTempFile("water-parameters-filter-export", ".csv");
		dialog.exportFile.deleteOnExit();
		WaterParametersScreen screen = createScreen(dbPort, dialog, new DirtyStateService());

		assertFalse(screen.isFilterPanelVisible());
		invokeEdt(() -> screen.getFilterAction().actionPerformed(null));
		assertTrue(screen.isFilterPanelVisible());

		invokeEdt(() -> screen.getFilterField().setText("syd"));
		assertEquals(1, screen.getTable().getRowCount());
		invokeEdt(() -> screen.getExportAction().actionPerformed(null));
		assertEquals(1, dialog.exportedNames.size());
		assertEquals("Sydney", dialog.exportedNames.get(0));

		invokeEdt(() ->
		{
			Object actionKey = screen.getFilterField().getInputMap(JComponent.WHEN_FOCUSED)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
			Action action = screen.getFilterField().getActionMap().get(actionKey);
			action.actionPerformed(null);
		});
		assertFalse(screen.isFilterPanelVisible());
		assertEquals("", screen.getFilterField().getText());
	}

	@Test
	public void renameRekeysMapAndInvokesHook() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waterParameters.put("Old", profile("Old", 10, 20));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.renameResult = "New";
		FakeRenameHook hook = new FakeRenameHook();
		WaterParametersScreen screen = createScreen(dbPort, dialog, new DirtyStateService(), hook);

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getRenameAction().actionPerformed(null));

		assertFalse(dbPort.waterParameters.containsKey("Old"));
		assertTrue(dbPort.waterParameters.containsKey("New"));
		assertEquals("Old", hook.oldName);
		assertEquals("New", hook.newName);
	}

	@Test
	public void editFlowPassesCurrentNameToDialog() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waterParameters.put("CurrentName", profile("CurrentName", 10, 20));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.nextEdited = profile("CurrentName", 11, 21);
		WaterParametersScreen screen = createScreen(dbPort, dialog, new DirtyStateService());

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));

		assertEquals("CurrentName", dialog.lastDialogInputName);
	}

	@Test
	public void dirtyRowsAreBoldAndClearOnSaveUndo() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.waterParameters.put("Dirty", profile("Dirty", 10, 20));
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();
		WaterParametersScreen screen = createScreen(dbPort, dialog, dirty);

		assertEquals(Font.PLAIN, rowFontStyle(screen, 0));
		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.nextEdited = profile("Dirty", 11, 20);
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(Font.BOLD, rowFontStyle(screen, 0));

		dialog.confirm = true;
		invokeEdt(() -> screen.getSaveAction().actionPerformed(null));
		assertEquals(Font.PLAIN, rowFontStyle(screen, 0));
	}

	private WaterParametersScreen createScreen(FakeDbPort dbPort, FakeDialogPort dialog, DirtyStateService dirty) throws Exception
	{
		final WaterParametersScreen[] holder = new WaterParametersScreen[1];
		invokeEdt(() -> holder[0] = new WaterParametersScreen(null, dirty, dialog, dbPort));
		return holder[0];
	}

	private WaterParametersScreen createScreen(FakeDbPort dbPort, FakeDialogPort dialog, DirtyStateService dirty, WaterParametersScreen.RenameHook renameHook) throws Exception
	{
		final WaterParametersScreen[] holder = new WaterParametersScreen[1];
		invokeEdt(() -> holder[0] = new WaterParametersScreen(null, dirty, dialog, dbPort, renameHook));
		return holder[0];
	}

	private int rowFontStyle(WaterParametersScreen screen, int row) throws Exception
	{
		final int[] style = new int[1];
		invokeEdt(() -> style[0] = screen.rowFontStyle(row));
		return style[0];
	}

	private static void invokeEdt(Runnable runnable) throws Exception
	{
		SwingUtilities.invokeAndWait(runnable);
	}

	private WaterParameters profile(String name, double minCalcium, double maxCalcium)
	{
		WaterParameters item = new WaterParameters(name);
		item.setMinCalcium(new PpmUnit(minCalcium));
		item.setMaxCalcium(new PpmUnit(maxCalcium));
		item.setMinBicarbonate(new PpmUnit(50D));
		item.setMaxBicarbonate(new PpmUnit(80D));
		item.setMinSulfate(new PpmUnit(60D));
		item.setMaxSulfate(new PpmUnit(120D));
		item.setMinAlkalinity(new PpmUnit(20D));
		item.setMaxAlkalinity(new PpmUnit(70D));
		item.setMinResidualAlkalinity(new PpmUnit(5D));
		item.setMaxResidualAlkalinity(new PpmUnit(40D));
		item.setDescription("desc");
		return item;
	}

	private static class FakeDbPort implements WaterParametersScreen.DbPort
	{
		private final Map<String, WaterParameters> waterParameters = new LinkedHashMap<>();
		private int saveCount;
		private int loadCount;

		@Override
		public Map<String, WaterParameters> waterParameters()
		{
			return waterParameters;
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

	private static class FakeDialogPort implements WaterParametersScreen.DialogPort
	{
		private WaterParameters nextEdited;
		private String lastDialogInputName;
		private String renameResult;
		private boolean confirm = true;
		private File exportFile;
		private String lastErrorMessage;
		private final List<String> exportedNames = new ArrayList<>();

		@Override
		public WaterParameters showEditWaterParametersDialog(javax.swing.JFrame parent, WaterParameters current, boolean createMode)
		{
			lastDialogInputName = current == null ? null : current.getName();
			return nextEdited;
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
		public void writeCsv(File target, java.util.Collection<WaterParameters> waterParameters) throws IOException
		{
			exportedNames.clear();
			for (WaterParameters waterParameter : waterParameters)
			{
				exportedNames.add(waterParameter.getName());
			}
			Files.write(target.toPath(), java.util.List.of("Name,MinCalcium", "X,1"), StandardCharsets.UTF_8);
		}

		@Override
		public void showError(javax.swing.JFrame parent, String message, String title)
		{
			lastErrorMessage = message;
		}
	}

	private static class FakeRenameHook implements WaterParametersScreen.RenameHook
	{
		private String oldName;
		private String newName;

		@Override
		public void onWaterParametersRenamed(String oldName, String newName)
		{
			this.oldName = oldName;
			this.newName = newName;
		}
	}
}
