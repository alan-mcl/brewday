package mclachlan.brewday.ui.swing.dialogs;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import java.lang.reflect.Field;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NewRecipeDialogTest
{
	@Test
	public void emptyNameDisablesOk() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		Database.getInstance().loadAll();
		NewRecipeDialog d = showDialog();
		try
		{
			JTextField name = nameField(d);
			JButton ok = okButton(d);
			invokeEdt(() -> name.setText(""));
			invokeEdt(() -> assertFalse(ok.isEnabled()));
		}
		finally
		{
			closeDialog(d);
		}
	}

	@Test
	public void duplicateNameDisablesOkAndShowsWarning() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		Database.getInstance().loadAll();
		Assume.assumeFalse(Database.getInstance().getRecipes().isEmpty());
		String existing = Database.getInstance().getRecipes().keySet().iterator().next();
		NewRecipeDialog d = showDialog();
		try
		{
			JTextField name = nameField(d);
			JButton ok = okButton(d);
			invokeEdt(() -> name.setText(existing));
			final int[] nameLen = new int[1];
			invokeEdt(() ->
			{
				assertFalse(ok.isEnabled());
				nameLen[0] = name.getText().length();
			});
			assertTrue(nameLen[0] > 0);
		}
		finally
		{
			closeDialog(d);
		}
	}

	@Test
	public void validNameReturnsRecipe() throws Exception
	{
		SwingTestSupport.assumeDisplay();
		Database.getInstance().loadAll();
		String unique = "ZZ_NewRecipeDlg_" + System.nanoTime();
		NewRecipeDialog d = showDialog();
		try
		{
			JTextField name = nameField(d);
			JButton ok = okButton(d);
			invokeEdt(() -> name.setText(unique));
			invokeEdt(() -> assertTrue(ok.isEnabled()));
			invokeEdt(ok::doClick);
			invokeEdt(() -> assertFalse(d.isVisible()));
			Recipe out = d.getResult();
			assertNotNull(out);
			assertEquals(unique, out.getName());
		}
		finally
		{
			Database.getInstance().getRecipes().remove(unique);
			closeDialog(d);
		}
	}

	private static NewRecipeDialog showDialog() throws Exception
	{
		final NewRecipeDialog[] holder = new NewRecipeDialog[1];
		invokeEdt(() ->
		{
			holder[0] = new NewRecipeDialog(new JFrame());
			holder[0].setModal(false);
			holder[0].setVisible(true);
		});
		return holder[0];
	}

	private static void closeDialog(NewRecipeDialog d) throws Exception
	{
		invokeEdt(() ->
		{
			d.setVisible(false);
			d.dispose();
		});
	}

	private static JTextField nameField(NewRecipeDialog d) throws Exception
	{
		Field f = NewRecipeDialog.class.getDeclaredField("recipeName");
		f.setAccessible(true);
		return (JTextField)f.get(d);
	}

	private static JButton okButton(NewRecipeDialog d) throws Exception
	{
		Field f = NewRecipeDialog.class.getDeclaredField("okButton");
		f.setAccessible(true);
		return (JButton)f.get(d);
	}

	private static void invokeEdt(Runnable r) throws Exception
	{
		SwingUtilities.invokeAndWait(r);
	}
}
