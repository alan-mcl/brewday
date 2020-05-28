package mclachlan.brewday;

import java.util.*;

/**
 *
 */
public class Settings
{
	private Map<String, String> settings;

	public Settings(Map<String, String> settings)
	{
		this.settings = settings;
	}

	public String get(String name)
	{
		return settings.get(name);
	}

	public void set(String name, String value)
	{
		settings.put(name, value);
	}

	public Map<String, String> getSettings()
	{
		return settings;
	}

	/*-------------------------------------------------------------------------*/
	public static final String DEFAULT_EQUIPMENT_PROFILE = "default.equipment.profile";
	public static final String GOOGLE_DRIVE_DIRECTORY_NAME = "backend.google.drive.directory.name";
	public static final String GOOGLE_DRIVE_DIRECTORY_ID = "backend.google.drive.directory.id";
	public static final String GOOGLE_DRIVE_AUTO_SYNC = "backend.google.drive.auto.sync";
}
