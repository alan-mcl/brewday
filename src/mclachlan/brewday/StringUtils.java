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
}
