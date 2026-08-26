package mclachlan.brewday.ui.swing.screens;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.SwingTestSupport;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProcessTemplatesScreenTest
{
	@BeforeClass
	public static void loadDb()
	{
		Database.getInstance().loadAll();
	}

	@BeforeClass
	public static void requireDisplay()
	{
		SwingTestSupport.assumeDisplay();
	}

	@Test
	public void refreshPopulatesTableFromDbPort() throws Exception
	{
		FakeDbPort db = new FakeDbPort();
		Recipe r = new Recipe("T1");
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
			assertEquals(0, ((Number)screen.getTable().getValueAt(0, 1)).intValue());
		});
	}

	@Test
	public void filterToolbarAndRowFilter() throws Exception
	{
		FakeDbPort db = new FakeDbPort();
		Recipe a = new Recipe("Alpha");
		Recipe b = new Recipe("Beta");
		db.templates.put("Alpha", a);
		db.templates.put("Beta", b);
		DirtyStateService dirty = new DirtyStateService();
		final ProcessTemplatesScreen[] holder = new ProcessTemplatesScreen[1];
		SwingUtilities.invokeAndWait(() ->
			holder[0] = new ProcessTemplatesScreen(new JFrame(), dirty, new FakeDialogPort(), db, n -> {}));
		ProcessTemplatesScreen screen = holder[0];
		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals("Filter", screen.getFilterAction().getValue(Action.NAME));
			assertFalse(screen.isFilterPanelVisible());
			screen.getFilterAction().actionPerformed(null);
			assertTrue(screen.isFilterPanelVisible());
			screen.getFilterField().setText("Beta");
		});
		SwingUtilities.invokeAndWait(() ->
			assertEquals(1, screen.getTable().getRowCount()));
	}

	@Test
	public void exportCsvWritesVisibleTemplates() throws Exception
	{
		FakeDbPort db = new FakeDbPort();
		Recipe a = new Recipe("Alpha");
		Recipe b = new Recipe("Beta");
		db.templates.put("Alpha", a);
		db.templates.put("Beta", b);
		File tmp = Files.createTempFile("process-templates-export", ".csv").toFile();
		tmp.deleteOnExit();
		ExportingFakeDialogPort dialog = new ExportingFakeDialogPort(tmp);
		DirtyStateService dirty = new DirtyStateService();
		final ProcessTemplatesScreen[] holder = new ProcessTemplatesScreen[1];
		SwingUtilities.invokeAndWait(() ->
			holder[0] = new ProcessTemplatesScreen(new JFrame(), dirty, dialog, db, n -> {}));
		ProcessTemplatesScreen screen = holder[0];
		SwingUtilities.invokeAndWait(() -> screen.getExportAction().actionPerformed(null));
		String content = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);
		assertTrue(content.contains("Name,Steps"));
		assertTrue(content.contains("Alpha"));
		assertTrue(content.contains("Beta"));
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

	private static class FakeDialogPort implements ProcessTemplatesScreen.DialogPort
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
		public File chooseExportFile(JFrame parent, File defaultFile)
		{
			return null;
		}

		@Override
		public void writeCsv(File target, Collection<Recipe> templates) throws IOException
		{
		}

		@Override
		public void showError(JFrame parent, String message, String title)
		{
		}

		@Override
		public void showError(JFrame parent, Throwable throwable, String title)
		{
		}
	}

	private static final class ExportingFakeDialogPort extends FakeDialogPort
	{
		private final File target;

		ExportingFakeDialogPort(File target)
		{
			this.target = target;
		}

		@Override
		public File chooseExportFile(JFrame parent, File defaultFile)
		{
			return target;
		}

		@Override
		public void writeCsv(File file, Collection<Recipe> templates) throws IOException
		{
			try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)))
			{
				w.println("Name,Steps");
				for (Recipe r : templates)
				{
					w.printf("%s,%d%n", r.getName(), r.getSteps() == null ? 0 : r.getSteps().size());
				}
			}
		}
	}
}
