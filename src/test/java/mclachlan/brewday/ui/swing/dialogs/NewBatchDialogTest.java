package mclachlan.brewday.ui.swing.dialogs;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import java.lang.reflect.Field;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import org.jdatepicker.JDatePicker;
import org.jdatepicker.LocalDateModel;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NewBatchDialogTest
{
	@Test
	public void emptyRecipeListDisablesOk() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		Database.getInstance().loadAll();
		NewBatchDialog d = showDialog();
		try
		{
			@SuppressWarnings("unchecked")
			JComboBox<String> combo = recipeCombo(d);
			JButton ok = okButton(d);
			invokeEdt(() ->
			{
				combo.removeAllItems();
				combo.revalidate();
			});
			invokeEdt(() -> assertFalse(ok.isEnabled()));
		}
		finally
		{
			closeDialog(d);
		}
	}

	@Test
	public void defaultDateIsToday() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		Database.getInstance().loadAll();
		NewBatchDialog d = showDialog();
		try
		{
			JDatePicker picker = datePicker(d);
			LocalDate today = LocalDate.now();
			final LocalDate[] fromModel = new LocalDate[1];
			invokeEdt(() ->
			{
				assertTrue(picker.getModel() instanceof LocalDateModel);
				fromModel[0] = ((LocalDateModel)picker.getModel()).getValue();
			});
			assertEquals(today, fromModel[0]);
		}
		finally
		{
			closeDialog(d);
		}
	}

	@Test
	public void validRecipeAndDateReturnsBatch() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		Database.getInstance().loadAll();
		Assume.assumeFalse(Database.getInstance().getRecipes().isEmpty());
		String recipeName = Database.getInstance().getRecipes().keySet().iterator().next();

		NewBatchDialog d = showDialog();
		try
		{
			@SuppressWarnings("unchecked")
			JComboBox<String> combo = recipeCombo(d);
			JButton ok = okButton(d);
			JDatePicker picker = datePicker(d);
			LocalDate brewDate = LocalDate.of(2018, 3, 15);
			invokeEdt(() ->
			{
				combo.setSelectedItem(recipeName);
				((LocalDateModel)picker.getModel()).setValue(brewDate);
			});
			invokeEdt(() -> assertTrue(ok.isEnabled()));
			invokeEdt(ok::doClick);
			invokeEdt(() -> assertFalse(d.isVisible()));
			Batch out = d.getResult();
			assertNotNull(out);
			assertEquals(recipeName, out.getRecipe());
			assertEquals(brewDate, out.getDate());
		}
		finally
		{
			Batch created = d.getResult();
			if (created != null)
			{
				Database.getInstance().getBatches().remove(created.getName());
			}
			closeDialog(d);
		}
	}

	private static NewBatchDialog showDialog() throws Exception
	{
		final NewBatchDialog[] holder = new NewBatchDialog[1];
		invokeEdt(() ->
		{
			holder[0] = new NewBatchDialog(new JFrame());
			holder[0].setModal(false);
			holder[0].setVisible(true);
		});
		return holder[0];
	}

	private static void closeDialog(NewBatchDialog d) throws Exception
	{
		invokeEdt(() ->
		{
			d.setVisible(false);
			d.dispose();
		});
	}

	@SuppressWarnings("unchecked")
	private static JComboBox<String> recipeCombo(NewBatchDialog d) throws Exception
	{
		Field f = NewBatchDialog.class.getDeclaredField("recipeCombo");
		f.setAccessible(true);
		return (JComboBox<String>)f.get(d);
	}

	private static JButton okButton(NewBatchDialog d) throws Exception
	{
		Field f = NewBatchDialog.class.getDeclaredField("okButton");
		f.setAccessible(true);
		return (JButton)f.get(d);
	}

	private static JDatePicker datePicker(NewBatchDialog d) throws Exception
	{
		Field f = NewBatchDialog.class.getDeclaredField("datePicker");
		f.setAccessible(true);
		return (JDatePicker)f.get(d);
	}

	private static void invokeEdt(Runnable r) throws Exception
	{
		SwingUtilities.invokeAndWait(r);
	}
}
