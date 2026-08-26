package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.util.StringUtils.getUiString;
import static org.junit.Assert.assertTrue;

public class SwingComputedVolumePaneTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void refreshShowsVolumeDescriptionWhenPresent() throws Exception
	{
		SwingTestSupport.assumeDisplay();

		Recipe recipe = new Recipe("CvTest");
		Volume v = new Volume("out", Volume.Type.WORT);
		recipe.getVolumes().addVolume("out", v);

		SwingComputedVolumePane pane = new SwingComputedVolumePane("Title");
		SwingUtilities.invokeAndWait(() -> pane.refresh("out", recipe));

		SwingUtilities.invokeAndWait(() ->
		{
			assertTrue(pane.getNameLabelForTest().getText().equals("out"));
			assertTrue(pane.getBodyLabelForTest().getText().length() > 0);
		});
	}

	@Test
	public void refreshShowsErrorWhenVolumeMissing() throws Exception
	{
		SwingTestSupport.assumeDisplay();

		Recipe recipe = new Recipe("CvMiss");
		SwingComputedVolumePane pane = new SwingComputedVolumePane("Title");
		SwingUtilities.invokeAndWait(() -> pane.refresh("missing", recipe));

		SwingUtilities.invokeAndWait(() ->
			assertTrue(pane.getNameLabelForTest().getText().equals(getUiString("volumes.error"))));
	}
}
