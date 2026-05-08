package mclachlan.brewday.ui.swing.dialogs;

import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import mclachlan.brewday.process.ProcessStep;
import org.junit.Assume;
import org.junit.Test;

import static mclachlan.brewday.util.StringUtils.getUiString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SwingNewStepDialogTest
{
	@Test
	public void defaultSelectionIsFirstSortOrderAndOkEnabled() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		final SwingNewStepDialog[] holder = new SwingNewStepDialog[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new SwingNewStepDialog(new JFrame()));
		SwingNewStepDialog d = holder[0];

		assertEquals(ProcessStep.Type.MASH, d.peekSelectedStepType());
		assertTrue(d.isOkEnabled());
		assertNotNull(d.peekDescriptionText());
		assertTrue(d.peekDescriptionText().length() > 0);
	}

	@Test
	public void changingComboUpdatesDescription() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		final SwingNewStepDialog[] holder = new SwingNewStepDialog[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new SwingNewStepDialog(new JFrame()));
		SwingNewStepDialog d = holder[0];

		SwingUtilities.invokeAndWait(() ->
			d.getStepTypeComboForTest().setSelectedItem(ProcessStep.Type.BOIL));

		String boilDesc = getUiString(ProcessStep.Type.BOIL.getDescKey());
		SwingUtilities.invokeAndWait(() ->
			assertEquals(boilDesc, d.peekDescriptionText()));
	}

	@Test
	public void confirmReturnsSelectedTypeCancelReturnsNull() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		final SwingNewStepDialog[] holder = new SwingNewStepDialog[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new SwingNewStepDialog(new JFrame()));
		SwingNewStepDialog d = holder[0];

		SwingUtilities.invokeAndWait(() ->
		{
			d.getStepTypeComboForTest().setSelectedItem(ProcessStep.Type.LAUTER);
			d.confirmForTest();
		});
		assertEquals(ProcessStep.Type.LAUTER, d.getResult());

		final SwingNewStepDialog[] holder2 = new SwingNewStepDialog[1];
		SwingUtilities.invokeAndWait(() -> holder2[0] = new SwingNewStepDialog(new JFrame()));
		SwingUtilities.invokeAndWait(() -> holder2[0].cancelForTest());
		assertNull(holder2[0].getResult());
	}
}
