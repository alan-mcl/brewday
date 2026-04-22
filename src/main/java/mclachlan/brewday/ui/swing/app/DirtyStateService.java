package mclachlan.brewday.ui.swing.app;

import java.util.HashSet;
import java.util.Set;

public class DirtyStateService
{
	private final Set<Object> dirty = new HashSet<>();

	public void markDirty(Object... objs)
	{
		for (Object obj : objs)
		{
			if (obj != null)
			{
				dirty.add(obj);
			}
		}
	}

	public boolean hasDirty()
	{
		return !dirty.isEmpty();
	}

	public void clear()
	{
		dirty.clear();
	}
}
