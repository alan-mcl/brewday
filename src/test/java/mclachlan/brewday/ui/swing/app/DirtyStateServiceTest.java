package mclachlan.brewday.ui.swing.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
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

		service.clear();
		assertFalse(service.hasDirty());
	}
}
