package mclachlan.brewday.ui;

import mclachlan.brewday.Brewday;

/**
 *
 */
public class UiUtils
{


	/*-------------------------------------------------------------------------*/

	/*-------------------------------------------------------------------------*/
	public static String getVersion()
	{
		return Brewday.getInstance().getAppConfig().getProperty("mclachlan.brewday.version");
	}
}
