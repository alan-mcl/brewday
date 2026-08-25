package mclachlan.brewday.ui.swing.widgets;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwingTagBarWidgetTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void addFiresOnceForNetNewAndIgnoresDuplicate() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		AtomicInteger adds = new AtomicInteger();
		final SwingTagBarWidget[] holder = new SwingTagBarWidget[1];
		SwingUtilities.invokeAndWait(() ->
		{
			holder[0] = new SwingTagBarWidget();
			holder[0].setTags(Collections.emptyList(), Collections.emptyList());
			holder[0].setOnAdd(s -> adds.incrementAndGet());
		});
		SwingTagBarWidget bar = holder[0];

		SwingUtilities.invokeAndWait(() ->
		{
			bar.getInputFieldForTest().setText("ipa");
			bar.triggerAddForTest();
			bar.getInputFieldForTest().setText("ipa");
			bar.triggerAddForTest();
		});

		assertEquals(1, adds.get());
	}

	@Test
	public void removeFiresOnce() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		AtomicInteger removes = new AtomicInteger();
		final SwingTagBarWidget[] holder = new SwingTagBarWidget[1];
		SwingUtilities.invokeAndWait(() ->
		{
			holder[0] = new SwingTagBarWidget();
			holder[0].setTags(Collections.singletonList("stout"), Collections.emptyList());
			holder[0].setOnRemove(s -> removes.incrementAndGet());
		});
		SwingTagBarWidget bar = holder[0];

		SwingUtilities.invokeAndWait(() -> bar.clickFirstRemoveForTest());

		assertEquals(1, removes.get());
	}

	@Test
	public void pickerListsKnownSuggestionsMinusAssigned() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		final SwingTagBarWidget[] holder = new SwingTagBarWidget[1];
		SwingUtilities.invokeAndWait(() ->
		{
			holder[0] = new SwingTagBarWidget();
			holder[0].setTags(Collections.singletonList("ipa"), Arrays.asList("ipa", "lager", "stout"));
		});
		SwingTagBarWidget bar = holder[0];

		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(2, bar.pickerItemCountForTest());
			assertFalse(bar.pickerContainsForTest("ipa"));
			assertTrue(bar.pickerContainsForTest("lager"));
			assertTrue(bar.pickerContainsForTest("stout"));
		});
	}

	@Test
	public void selectingKnownSuggestionAddsOnce() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		AtomicInteger adds = new AtomicInteger();
		final SwingTagBarWidget[] holder = new SwingTagBarWidget[1];
		SwingUtilities.invokeAndWait(() ->
		{
			holder[0] = new SwingTagBarWidget();
			holder[0].setTags(Collections.emptyList(), Arrays.asList("ipa", "stout"));
			holder[0].setOnAdd(s -> adds.incrementAndGet());
		});
		SwingTagBarWidget bar = holder[0];

		SwingUtilities.invokeAndWait(() ->
		{
			bar.selectPickerItemForTest("stout");
			bar.selectPickerItemForTest("stout");
		});

		assertEquals(1, adds.get());
	}
}
