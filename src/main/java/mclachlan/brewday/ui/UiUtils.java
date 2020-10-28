/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui;

import javafx.scene.image.Image;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.jfx.Icons;

/**
 *
 */
public class UiUtils
{
	public static final String NONE = " - ";


	/*-------------------------------------------------------------------------*/
	public static String getVersion()
	{
		return Brewday.getInstance().getAppConfig().getProperty(Brewday.BREWDAY_VERSION);
	}

	/*-------------------------------------------------------------------------*/
	public static Image getMiscIcon(Misc misc)
	{
		if (misc.getType() != null)
		{
			switch (misc.getType())
			{
				case SPICE:
					return Icons.miscIconSpice;
				case FINING:
					return Icons.miscIconFining;
				case WATER_AGENT:
					PercentageUnit acidContent = misc.getAcidContent();
					if (acidContent != null && acidContent.get(Quantity.Unit.PERCENTAGE) > 0)
					{
						return Icons.miscIconAcid;
					}
					else
					{
						return Icons.miscIconWaterAgent;
					}
				case HERB:
					return Icons.miscIconHerb;
				case FLAVOUR:
					return Icons.miscIconFlavour;
				case OTHER:
				default:
					return Icons.miscIconGeneric;
			}
		}
		else
		{
			return Icons.miscIconGeneric;
		}
	}

	/*-------------------------------------------------------------------------*/
	public static Image getFermentableIcon(Fermentable f)
	{
		if (f.getType() != null)
		{
			switch (f.getType())
			{
				case GRAIN:
					return Icons.fermentableIconGrain;
				case SUGAR:
					return Icons.fermentableIconSugar;
				case LIQUID_EXTRACT:
					return Icons.fermentableIconLiquidExtract;
				case DRY_EXTRACT:
					return Icons.fermentableIconDryExtract;
				case ADJUNCT:
					return Icons.fermentableIconAdjunct;
				case JUICE:
					return Icons.fermentableIconJuice;
				default:
					return Icons.fermentableIconGeneric;
			}
		}
		else
		{
			return Icons.fermentableIconGeneric;
		}
	}
}
