package mclachlan.brewday.ui.swing.dialogs;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.HopStand;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwingHopAdditionDialogTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void hopAdditionDialogAcceptsJDialogOwnerWindow() throws Exception
	{
		SwingTestSupport.assumeDisplay();

		Recipe r = new Recipe("HopDlgOwner");
		HopStand hopStand = (HopStand)RecipeEditorSteps.createStep(r, ProcessStep.Type.HOP_STAND);
		r.getSteps().add(hopStand);

		SwingUtilities.invokeAndWait(() ->
		{
			JDialog owner = new JDialog();
			SwingHopAdditionDialog d = new SwingHopAdditionDialog(owner, hopStand, null, true);
			d.dispose();
			owner.dispose();
		});
	}

	@Test
	public void hopAdditionDialogSearchFiltersAndOkProducesHopAddition() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		Assume.assumeFalse(Database.getInstance().getHops().isEmpty());

		Recipe r = new Recipe("HopDlgT");
		HopStand hopStand = (HopStand)RecipeEditorSteps.createStep(r, ProcessStep.Type.HOP_STAND);
		r.getSteps().add(hopStand);

		SwingHopAdditionDialog[] holder = new SwingHopAdditionDialog[1];
		SwingUtilities.invokeAndWait(() ->
		{
			holder[0] = new SwingHopAdditionDialog(null, hopStand, null, true);
			holder[0].setModal(false);
			holder[0].setVisible(true);
		});

		SwingHopAdditionDialog d = holder[0];
		int fullRows = d.getTableForTest().getRowCount();
		assertTrue(fullRows > 0);

		SwingUtilities.invokeAndWait(() ->
		{
			d.getSearchFieldForTest().setText("zzzzzznonexistenthopname");
		});
		SwingUtilities.invokeAndWait(() ->
		{
			assertTrue(d.getVisibleRowCountForTest() < fullRows);
			d.getSearchFieldForTest().setText("");
		});

		String key = Settings.INGREDIENT_ADDITIONS_FROM_INVENTORY_ONLY;
		boolean before = Boolean.parseBoolean(Database.getInstance().getSettings().get(key));
		SwingUtilities.invokeAndWait(() -> d.getOnlyInventoryCheckboxForTest().doClick());
		boolean after = Boolean.parseBoolean(Database.getInstance().getSettings().get(key));
		assertTrue(before != after);

		SwingUtilities.invokeAndWait(() ->
		{
			d.getTableForTest().setRowSelectionInterval(0, 0);
			d.getQuantityWidgetForTest().getTextField().setText("15");
			if (d.getTimeWidgetForTest() != null)
			{
				d.getTimeWidgetForTest().getTextField().setText("30");
			}
			d.getOkButtonForTest().doClick();
		});

		HopAddition out = d.getOutput();
		assertNotNull(out);
		assertNotNull(out.getHop());
		assertTrue(out.getQuantity().get(out.getUnit()) > 0);
	}
}
