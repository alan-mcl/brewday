package mclachlan.brewday.ui.swing.screens;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InventoryScreenTest
{
	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void actionsEnableOnSelection() throws Exception
	{
		resetInventory();
		DirtyStateService dirty = new DirtyStateService();
		FakeDialogPort fakePort = new FakeDialogPort();
		InventoryScreen screen = createScreen(dirty, fakePort);

		assertFalse(screen.getEditAction().isEnabled());
		assertFalse(screen.getDeleteAction().isEnabled());

		InventoryLineItem item = new InventoryLineItem("Test Item", IngredientAddition.Type.MISC, new WeightUnit(100, Quantity.Unit.GRAMS), Quantity.Unit.GRAMS);
		Database.getInstance().getInventory().put(item.getName(), item);
		invokeEdt(screen::refresh);
		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));

		assertTrue(screen.getEditAction().isEnabled());
		assertTrue(screen.getDeleteAction().isEnabled());
	}

	@Test
	public void editDeleteAndExportFlowsUseDialogPort() throws Exception
	{
		resetInventory();
		DirtyStateService dirty = new DirtyStateService();
		FakeDialogPort fakePort = new FakeDialogPort();
		InventoryScreen screen = createScreen(dirty, fakePort);

		InventoryLineItem item = new InventoryLineItem("Editable Item", IngredientAddition.Type.MISC, new WeightUnit(200, Quantity.Unit.GRAMS), Quantity.Unit.GRAMS);
		Database.getInstance().getInventory().put(item.getName(), item);
		invokeEdt(screen::refresh);
		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));

		fakePort.nextEditQuantity = 2.5;
		invokeEdt(() -> screen.getEditAction().actionPerformed(null));
		assertEquals(2.5, item.getQuantity().get(item.getUnit()), 0.0001);
		assertTrue(dirty.hasDirty());

		File csv = File.createTempFile("inventory-screen-test", ".csv");
		csv.deleteOnExit();
		fakePort.exportTarget = csv;
		invokeEdt(() -> screen.getExportAction().actionPerformed(null));
		List<String> lines = Files.readAllLines(csv.toPath(), StandardCharsets.UTF_8);
		assertFalse(lines.isEmpty());
		assertTrue(lines.get(0).startsWith("Ingredient,Type,Quantity"));

		fakePort.confirmDelete = true;
		invokeEdt(() -> screen.getTable().setRowSelectionInterval(0, 0));
		invokeEdt(() -> screen.getDeleteAction().actionPerformed(null));
		assertEquals(0, Database.getInstance().getInventory().size());
	}

	@Test
	public void exportErrorIsSurfaced() throws Exception
	{
		resetInventory();
		DirtyStateService dirty = new DirtyStateService();
		FakeDialogPort fakePort = new FakeDialogPort();
		fakePort.failOnWrite = true;
		InventoryScreen screen = createScreen(dirty, fakePort);

		InventoryLineItem item = new InventoryLineItem("Export Item", IngredientAddition.Type.MISC, new WeightUnit(50, Quantity.Unit.GRAMS), Quantity.Unit.GRAMS);
		Database.getInstance().getInventory().put(item.getName(), item);
		invokeEdt(screen::refresh);

		File csv = File.createTempFile("inventory-screen-test-error", ".csv");
		csv.deleteOnExit();
		fakePort.exportTarget = csv;
		invokeEdt(() -> screen.getExportAction().actionPerformed(null));

		assertNotNull(fakePort.lastErrorMessage);
	}

	private static InventoryScreen createScreen(DirtyStateService dirty, FakeDialogPort port) throws Exception
	{
		final InventoryScreen[] holder = new InventoryScreen[1];
		invokeEdt(() -> holder[0] = new InventoryScreen(null, dirty, port));
		return holder[0];
	}

	private static void resetInventory()
	{
		Database.getInstance().getInventory().clear();
	}

	private static void invokeEdt(Runnable run) throws Exception
	{
		SwingUtilities.invokeAndWait(run);
	}

	private static class FakeDialogPort implements InventoryScreen.DialogPort
	{
		private Double nextEditQuantity;
		private boolean confirmDelete;
		private File exportTarget;
		private boolean failOnWrite;
		private String lastErrorMessage;

		@Override
		public InventoryLineItem showAddItemDialog(javax.swing.JFrame parent, mclachlan.brewday.ui.swing.dialogs.AddInventoryItemDialog dialog)
		{
			return null;
		}

		@Override
		public Double promptEditQuantity(javax.swing.JFrame parent, double currentValue)
		{
			return nextEditQuantity;
		}

		@Override
		public boolean confirmDelete(javax.swing.JFrame parent, String message, String title)
		{
			return confirmDelete;
		}

		@Override
		public File chooseExportFile(javax.swing.JFrame parent, File defaultFile)
		{
			return exportTarget;
		}

		@Override
		public void writeCsv(File file, Iterable<InventoryLineItem> items) throws IOException
		{
			if (failOnWrite)
			{
				throw new IOException("synthetic export failure");
			}
			List<String> lines = new ArrayList<>();
			lines.add("Ingredient,Type,Quantity");
			for (InventoryLineItem item : items)
			{
				lines.add(item.getIngredient() + "," + item.getType() + "," + item.getQuantity().get(item.getUnit()) + " " + item.getUnit());
			}
			Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
		}

		@Override
		public void showError(javax.swing.JFrame parent, String message, String title)
		{
			lastErrorMessage = message;
		}
	}
}
