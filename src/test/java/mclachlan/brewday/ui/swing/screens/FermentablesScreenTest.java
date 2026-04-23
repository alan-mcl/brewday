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
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FermentablesScreenTest
{
	@Test
	public void addEditDeleteSaveUndoAndExport() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();
		FermentablesScreen screen = createScreen(dbPort, dialog, dirty);

		dialog.nextEdited = fermentable("Pale", Fermentable.Type.GRAIN);
		invokeEdt(() -> screen.getAddAction().actionPerformed(null));
		assertEquals(1, dbPort.fermentables.size());
		assertTrue(dirty.hasDirty());

		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.nextEdited = fermentable("Pale", Fermentable.Type.SUGAR);
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(Fermentable.Type.SUGAR, dbPort.fermentables.get("Pale").getType());

		File csv = File.createTempFile("fermentables-screen-test", ".csv");
		csv.deleteOnExit();
		dialog.exportFile = csv;
		invokeEdt(() -> screen.getExportAction().actionPerformed(null));
		assertFalse(Files.readAllLines(csv.toPath(), StandardCharsets.UTF_8).isEmpty());

		dialog.confirm = true;
		invokeEdt(() -> screen.getSaveAction().actionPerformed(null));
		assertEquals(1, dbPort.saveCount);
		assertFalse(dirty.hasDirty());

		dirty.markDirty("fermentables");
		invokeEdt(() -> screen.getUndoAction().actionPerformed(null));
		assertEquals(1, dbPort.loadCount);
		assertFalse(dirty.hasDirty());
	}

	@Test
	public void hotkeysAndFilterAreWired() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.fermentables.put("Pale", fermentable("Pale", Fermentable.Type.GRAIN));
		dbPort.fermentables.put("Sugar", fermentable("Sugar", Fermentable.Type.SUGAR));
		FermentablesScreen screen = createScreen(dbPort, new FakeDialogPort(), new DirtyStateService());

		KeyStroke filterKs = ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F);
		Object filterMap = screen.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(filterKs);
		assertEquals("fermentable.hotkey.filterCtrl", filterMap);

		invokeEdt(() -> screen.getFilterAction().actionPerformed(null));
		invokeEdt(() -> screen.getFilterField().setText("sug"));
		assertEquals(1, screen.getTable().getRowCount());
		assertEquals("Sugar", screen.getTable().getValueAt(0, 0));
	}

	@Test
	public void dirtyRowsAreBold() throws Exception
	{
		FakeDbPort dbPort = new FakeDbPort();
		dbPort.fermentables.put("Pale", fermentable("Pale", Fermentable.Type.GRAIN));
		FakeDialogPort dialog = new FakeDialogPort();
		DirtyStateService dirty = new DirtyStateService();
		FermentablesScreen screen = createScreen(dbPort, dialog, dirty);

		assertEquals(Font.PLAIN, rowFontStyle(screen, 0));
		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		dialog.nextEdited = fermentable("Pale", Fermentable.Type.SUGAR);
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(Font.BOLD, rowFontStyle(screen, 0));
	}

	private FermentablesScreen createScreen(FakeDbPort dbPort, FakeDialogPort dialog, DirtyStateService dirty) throws Exception
	{
		final FermentablesScreen[] holder = new FermentablesScreen[1];
		invokeEdt(() -> holder[0] = new FermentablesScreen(null, dirty, dialog, dbPort));
		return holder[0];
	}

	private int rowFontStyle(FermentablesScreen screen, int row) throws Exception
	{
		final int[] style = new int[1];
		invokeEdt(() -> style[0] = screen.rowFontStyle(row));
		return style[0];
	}

	private static void invokeEdt(Runnable runnable) throws Exception
	{
		SwingUtilities.invokeAndWait(runnable);
	}

	private Fermentable fermentable(String name, Fermentable.Type type)
	{
		Fermentable f = new Fermentable(name);
		f.setType(type);
		f.setOrigin("AU");
		f.setSupplier("MaltCo");
		f.setYield((PercentageUnit)Quantity.parseQuantity("80", Quantity.Unit.PERCENTAGE_DISPLAY));
		f.setColour(new ColourUnit(4, Quantity.Unit.LOVIBOND));
		f.setDistilledWaterPh(new PhUnit(5.6));
		return f;
	}

	private static class FakeDbPort implements FermentablesScreen.DbPort
	{
		private final Map<String, Fermentable> fermentables = new LinkedHashMap<>();
		private int saveCount;
		private int loadCount;

		@Override
		public Map<String, Fermentable> fermentables()
		{
			return fermentables;
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

	private static class FakeDialogPort implements FermentablesScreen.DialogPort
	{
		private Fermentable nextEdited;
		private String renameResult;
		private boolean confirm = true;
		private File exportFile;
		private final List<String> exportedNames = new ArrayList<>();

		@Override
		public Fermentable showEditFermentableDialog(javax.swing.JFrame parent, Fermentable current, boolean createMode)
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
		public void writeCsv(File target, java.util.Collection<Fermentable> fermentables) throws IOException
		{
			exportedNames.clear();
			for (Fermentable fermentable : fermentables)
			{
				exportedNames.add(fermentable.getName());
			}
			Files.write(target.toPath(), java.util.List.of("Name,Type", "X,GRAIN"), StandardCharsets.UTF_8);
		}

		@Override
		public void showError(javax.swing.JFrame parent, String message, String title)
		{
		}
	}
}
