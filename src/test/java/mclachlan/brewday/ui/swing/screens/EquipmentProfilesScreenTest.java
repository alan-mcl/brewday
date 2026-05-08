package mclachlan.brewday.ui.swing.screens;

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.ArbitraryPhysicalQuantity;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.PowerUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EquipmentProfilesScreenTest
{
	@Test
	public void addEditDeleteSaveUndoAndExport() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();
		EquipmentProfilesScreen screen = createScreen(dbPort, dialog, dirty,
			new EquipmentProfilesScreen.NoOpRenameHook(), new EquipmentProfilesScreen.NoOpDeleteHook());

		dialog.nextEdited = profile("MyEquip");
		invokeEdt(() -> screen.getAddAction().actionPerformed(null));
		assertEquals(1, dbPort.equipment.size());
		assertTrue(dirty.hasDirty());

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.nextEdited = profile("MyEquip");
		dialog.nextEdited.setBoilElementPower(new PowerUnit(4.5));
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(4.5, dbPort.equipment.get("MyEquip").getBoilElementPower().get(), 0.001);

		File csv = File.createTempFile("equipment-screen-test", ".csv");
		csv.deleteOnExit();
		dialog.exportFile = csv;
		invokeEdt(() -> screen.getExportAction().actionPerformed(null));
		assertFalse(Files.readAllLines(csv.toPath(), StandardCharsets.UTF_8).isEmpty());

		dialog.confirm = true;
		invokeEdt(() -> screen.getSaveAction().actionPerformed(null));
		assertEquals(1, dbPort.saveCount);
		assertFalse(dirty.hasDirty());

		dirty.markDirty("equipment.profiles");
		invokeEdt(() -> screen.getUndoAction().actionPerformed(null));
		assertEquals(1, dbPort.loadCount);
		assertFalse(dirty.hasDirty());
	}

	@Test
	public void hotkeysAndFilterAreWired() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.equipment.put("A", profile("A"));
		dbPort.equipment.put("B", profile("B"));
		EquipmentProfilesScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService(),
			new EquipmentProfilesScreen.NoOpRenameHook(), new EquipmentProfilesScreen.NoOpDeleteHook());

		KeyStroke filterKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F);
		Object filterMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(filterKs);
		assertEquals("equipment.hotkey.filterCtrl", filterMap);

		invokeEdt(() -> screen.getFilterAction().actionPerformed(null));
		invokeEdt(() -> screen.getFilterField().setText("B"));
		assertEquals(1, screen.getTable().getRowCount());
		assertEquals("B", screen.getTable().getValueAt(0, 0));
	}

	@Test
	public void dirtyRowsAreBold() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.equipment.put("A", profile("A"));
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();
		EquipmentProfilesScreen screen = createScreen(dbPort, dialog, dirty,
			new EquipmentProfilesScreen.NoOpRenameHook(), new EquipmentProfilesScreen.NoOpDeleteHook());

		assertEquals(Font.PLAIN, rowFontStyle(screen, 0));
		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.nextEdited = profile("A");
		dialog.nextEdited.setBoilElementPower(new PowerUnit(9));
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(Font.BOLD, rowFontStyle(screen, 0));
	}

	@Test
	public void renameAndDeleteInvokeHooks() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.equipment.put("Old", profile("Old"));
		FakeDialogPort dialog = new FakeDialogPort();
		dialog.renameResult = "New";
		RecordingRenameHook renameHook = new RecordingRenameHook();
		RecordingDeleteHook deleteHook = new RecordingDeleteHook();
		EquipmentProfilesScreen screen = createScreen(dbPort, dialog, new DirtyStateService(), renameHook, deleteHook);

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getRenameAction().actionPerformed(null));
		assertEquals("Old", renameHook.oldName);
		assertEquals("New", renameHook.newName);
		assertTrue(dbPort.equipment.containsKey("New"));

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.confirm = true;
		invokeEdt(() -> screen.getDeleteAction().actionPerformed(null));
		assertEquals("New", deleteHook.deletedName);
		assertFalse(dbPort.equipment.containsKey("New"));
	}

	private EquipmentProfilesScreen createScreen(FakeDbPort dbPort, FakeDialogPort dialog, DirtyStateService dirty,
		EquipmentProfilesScreen.RenameHook renameHook, EquipmentProfilesScreen.DeleteHook deleteHook) throws Exception
	{
		final EquipmentProfilesScreen[] holder = new EquipmentProfilesScreen[1];
		invokeEdt(() -> holder[0] = new EquipmentProfilesScreen(null, dirty, dialog, dbPort, renameHook, deleteHook));
		return holder[0];
	}

	private int rowFontStyle(EquipmentProfilesScreen screen, int row) throws Exception
	{
		final int[] style = new int[1];
		invokeEdt(() -> style[0] = screen.rowFontStyle(row));
		return style[0];
	}

	private static void invokeEdt(Runnable runnable) throws Exception
	{
		SwingUtilities.invokeAndWait(runnable);
	}

	private static EquipmentProfile profile(String name)
	{
		EquipmentProfile p = new EquipmentProfile(name);
		p.setConversionEfficiency(new PercentageUnit(0.75));
		p.setMashTunVolume(new VolumeUnit(40));
		p.setBoilKettleVolume(new VolumeUnit(50));
		p.setFermenterVolume(new VolumeUnit(60));
		return p;
	}

	private static class RecordingRenameHook implements EquipmentProfilesScreen.RenameHook
	{
		String oldName;
		String newName;

		@Override
		public void onEquipmentProfileRenamed(String oldName, String newName)
		{
			this.oldName = oldName;
			this.newName = newName;
		}
	}

	private static class RecordingDeleteHook implements EquipmentProfilesScreen.DeleteHook
	{
		String deletedName;

		@Override
		public void onEquipmentProfileDeleted(String name)
		{
			this.deletedName = name;
		}
	}

	private static class FakeDbPort implements EquipmentProfilesScreen.DbPort
	{
		private final Map<String, EquipmentProfile> equipment = new LinkedHashMap<>();
		private int saveCount;
		private int loadCount;

		@Override
		public Map<String, EquipmentProfile> equipmentProfiles()
		{
			return equipment;
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

	private static class FakeDialogPort implements EquipmentProfilesScreen.DialogPort
	{
		private EquipmentProfile nextEdited;
		private String renameResult;
		private boolean confirm = true;
		private File exportFile;
		private final List<String> exportedNames = new ArrayList<>();

		@Override
		public EquipmentProfile showEditEquipmentProfileDialog(javax.swing.JFrame parent, EquipmentProfile current, boolean createMode)
		{
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
		public void writeCsv(File target, java.util.Collection<EquipmentProfile> profiles) throws IOException
		{
			exportedNames.clear();
			for (EquipmentProfile p : profiles)
			{
				exportedNames.add(p.getName());
			}
			Files.write(target.toPath(), java.util.List.of("Name", "X"), StandardCharsets.UTF_8);
		}

		@Override
		public void showError(javax.swing.JFrame parent, String message, String title)
		{
		}
	}
}
