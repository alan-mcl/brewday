package mclachlan.brewday;

import mclachlan.brewday.db.Database;

/**
 *
 */
public class StringUtils
{
	public static String getUiString(String key)
	{
		String result = Database.getInstance().getStrings("ui").getProperty(key);

		if (result == null)
		{
			throw new BrewdayException("UI label not found: ["+key+"]");
		}

		return result;
	}

	public static String getUiString(String key, Object... args)
	{
		String result = getUiString(key);

		return String.format(result, args);
	}

	public static String getProcessString(String key)
	{
		String result = Database.getInstance().getStrings("process").getProperty(key);

		if (result == null)
		{
			throw new BrewdayException("UI label not found: ["+key+"]");
		}

		return result;
	}

	public static String getProcessString(String key, Object... args)
	{
		String result = getProcessString(key);

		return String.format(result, args);
	}

	public static String getDocString(String key)
	{
		String result = Database.getInstance().getStrings("document").getProperty(key);

		if (result == null)
		{
			throw new BrewdayException("Document label not found: ["+key+"]");
		}

		return result;
	}

	public static String getDocString(String key, Object... args)
	{
		String result = getDocString(key);

		return String.format(result, args);
	}

}
