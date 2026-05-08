package mclachlan.brewday.ui.swing.dialogs;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Stand;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwingWaterAdditionDialogTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void waterAdditionDialogOkProducesWaterAddition() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Assume.assumeFalse(Database.getInstance().getWaters().isEmpty());

		Recipe r = new Recipe("WaterDlgT");
		Stand stand = (Stand)RecipeEditorSteps.createStep(r, ProcessStep.Type.STAND);
		r.getSteps().add(stand);

		SwingWaterAdditionDialog[] holder = new SwingWaterAdditionDialog[1];
		SwingUtilities.invokeAndWait(() ->
		{
			holder[0] = new SwingWaterAdditionDialog(null, stand, null, true);
			holder[0].setModal(false);
			holder[0].setVisible(true);
		});

		SwingWaterAdditionDialog d = holder[0];
		assertTrue(d.getTableForTest().getRowCount() > 0);

		SwingUtilities.invokeAndWait(() ->
		{
			d.getTableForTest().setRowSelectionInterval(0, 0);
			d.getQuantityWidgetForTest().getTextField().setText("3");
			if (d.getTimeWidgetForTest() != null)
			{
				d.getTimeWidgetForTest().getTextField().setText("5");
			}
			if (d.getTemperatureWidgetForTest() != null)
			{
				d.getTemperatureWidgetForTest().getTextField().setText("65");
			}
			d.getOkButtonForTest().doClick();
		});

		WaterAddition out = d.getOutput();
		assertNotNull(out);
		assertNotNull(out.getWater());
		assertTrue(out.getQuantity().get(out.getUnit()) > 0);
	}
}
