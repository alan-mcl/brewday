package mclachlan.brewday.ui.swing.screens;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProcessTemplatesScreenTest
{
	@Test
	public void refreshPopulatesTableFromDbPort() throws Exception
	{
		FakeDbPort db = new FakeDbPort();
		Recipe r = new Recipe("T1");
		r.getSteps().add(RecipeEditorSteps.createStep(r, ProcessStep.Type.BOIL));
		db.templates.put("T1", r);
		DirtyStateService dirty = new DirtyStateService();
		final ProcessTemplatesScreen[] holder = new ProcessTemplatesScreen[1];
		SwingUtilities.invokeAndWait(() ->
			holder[0] = new ProcessTemplatesScreen(new JFrame(), dirty, new FakeDialogPort(), db, n -> {}));
		ProcessTemplatesScreen screen = holder[0];
		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(1, screen.getTable().getRowCount());
			assertEquals("T1", screen.getTable().getValueAt(0, 0).toString());
			assertEquals(1, ((Number)screen.getTable().getValueAt(0, 1)).intValue());
		});
	}

	private static final class FakeDbPort implements ProcessTemplatesScreen.DbPort
	{
		private final Map<String, Recipe> templates = new LinkedHashMap<>();

		@Override
		public Map<String, Recipe> processTemplates()
		{
			return templates;
		}

		@Override
		public void saveAll()
		{
		}

		@Override
		public void loadAll()
		{
		}
	}

	private static final class FakeDialogPort implements ProcessTemplatesScreen.DialogPort
	{
		@Override
		public String promptName(JFrame parent, String message, String title, String currentName)
		{
			return null;
		}

		@Override
		public boolean confirm(JFrame parent, String message, String title)
		{
			return false;
		}

		@Override
		public void showError(JFrame parent, String message, String title)
		{
		}
	}
}
