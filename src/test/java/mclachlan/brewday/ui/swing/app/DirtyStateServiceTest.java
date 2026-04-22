package mclachlan.brewday.ui.swing.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DirtyStateServiceTest
{
	@Test
	public void markDirtyAndClear()
	{
		DirtyStateService service = new DirtyStateService();

		assertFalse(service.hasDirty());
		service.markDirty("one", null, "two");
		assertTrue(service.hasDirty());
		assertTrue(service.isDirty("one"));
		assertFalse(service.isDirty("missing"));

		service.clear();
		assertFalse(service.hasDirty());
		assertFalse(service.isDirty("one"));
	}

	@Test
	public void listenersAreNotifiedOnChange()
	{
		DirtyStateService service = new DirtyStateService();
		final int[] notifications = new int[1];
		Runnable listener = () -> notifications[0]++;
		service.addListener(listener);

		service.markDirty("a");
		service.markDirty("a"); // no new dirty token, no new notification
		service.clear();
		service.clear(); // already clear, no notification

		assertEquals(2, notifications[0]);
		service.removeListener(listener);
	}
}
