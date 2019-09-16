package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.v2.V2SerialiserObject;

/**
 *
 */
public class SettingsSerialiser implements V2SerialiserObject<Settings>
{
	@Override
	public Object toObj(Settings settings)
	{
		return settings.getSettings();
	}

	@Override
	public Settings fromObj(Object obj)
	{
		return new Settings((Map<String, String>)obj);
	}
}
