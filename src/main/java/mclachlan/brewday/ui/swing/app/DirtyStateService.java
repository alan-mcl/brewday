package mclachlan.brewday.ui.swing.app;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirtyStateService
{
	private final Set<Object> dirty = new HashSet<>();
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

	public void markDirty(Object... objs)
	{
		boolean changed = false;
		for (Object obj : objs)
		{
			if (obj != null)
			{
				changed |= dirty.add(obj);
			}
		}
		if (changed)
		{
			notifyListeners();
		}
	}

	public boolean hasDirty()
	{
		return !dirty.isEmpty();
	}

	public boolean isDirty(Object obj)
	{
		return obj != null && dirty.contains(obj);
	}

	public void clear()
	{
		if (dirty.isEmpty())
		{
			return;
		}
		dirty.clear();
		notifyListeners();
	}

	public void addListener(Runnable listener)
	{
		if (listener != null)
		{
			listeners.add(listener);
		}
	}

	public void removeListener(Runnable listener)
	{
		listeners.remove(listener);
	}

	private void notifyListeners()
	{
		for (Runnable listener : listeners)
		{
			listener.run();
		}
	}
}
