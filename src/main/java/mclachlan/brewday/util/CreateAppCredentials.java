package mclachlan.brewday.util;

import mclachlan.brewday.db.v2.sensitive.SensitiveStore;

/**
 *
 */
public class CreateAppCredentials
{
	public static void main(String[] args) throws Exception
	{
		String key = args[0];
		String credentials = args[1];
		String appKey = args[2];

		SensitiveStore ss = new SensitiveStore("db/sensitive", "brewday");

		ss.init(appKey);

		ss.set(key, credentials);
	}
}
