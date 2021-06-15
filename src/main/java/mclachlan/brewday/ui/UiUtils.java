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

import java.util.*;
import javafx.scene.image.Image;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;
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

	public static Comparator<IngredientAddition> getIngredientAdditionComparator()
	{
		return (ia1, ia2) ->
		{
			double sortOrder1;
			double sortOrder2;
			if (ia1.getType() != ia2.getType())
			{
				sortOrder1 = ia1.getType().getSortOrder();
				sortOrder2 = ia2.getType().getSortOrder();
			}
			else
			{
				if (ia1 instanceof FermentableAddition)
				{
					// order fermentables by type asc, then quantity desc

					Fermentable.Type type1 = ((FermentableAddition)ia1).getFermentable().getType();
					Fermentable.Type type2 = ((FermentableAddition)ia2).getFermentable().getType();

					if (type1 == type2)
					{
						// quantity desc
						sortOrder1 = ia2.getQuantity().get();
						sortOrder2 = ia1.getQuantity().get();
					}
					else
					{
						// type asc
						sortOrder1 = type1.getSortOrder();
						sortOrder2 = type2.getSortOrder();
					}
				}
				else if (ia1 instanceof MiscAddition)
				{
					// order miscs by type (water agents by acid content desc), then quantity.

					Misc.Type type1 = ((MiscAddition)ia1).getMisc().getType();
					Misc.Type type2 = ((MiscAddition)ia2).getMisc().getType();

					if (type1 == type2)
					{
						PercentageUnit ac1 = ((MiscAddition)ia1).getMisc().getAcidContent();
						PercentageUnit ac2 = ((MiscAddition)ia2).getMisc().getAcidContent();
						if (ac1 != null && ac2 != null && ac1.get() > 0 && ac2.get() > 0)
						{
							// acid content descending
							sortOrder1 = ac2.get();
							sortOrder2 = ac1.get();
						}
						else
						{
							sortOrder1 = ia2.getQuantity().get();
							sortOrder2 = ia1.getQuantity().get();
						}
					}
					else
					{
						sortOrder1 = type1.getSortOrder();
						sortOrder2 = type2.getSortOrder();
					}
				}
				else
				{
					// sort Hops, Yeast and Water by quantity desc
					sortOrder1 = ia2.getQuantity().get();
					sortOrder2 = ia1.getQuantity().get();
				}
			}

			return (int)(sortOrder1 - sortOrder2);
		};
	}

	public static Comparator<InventoryLineItem> getInventoryLineItemComparator()
	{
		return (ia1, ia2) ->
		{
			double sortOrder1;
			double sortOrder2;

			if (ia1.getType() != ia2.getType())
			{
				sortOrder1 = ia1.getType().getSortOrder();
				sortOrder2 = ia2.getType().getSortOrder();
			}
			else
			{
				if (ia1.getType() == IngredientAddition.Type.FERMENTABLES)
				{
					// order fermentables by type asc, then quantity desc

					Fermentable f1 = Database.getInstance().getFermentables().get(ia1.getIngredient());
					Fermentable f2 = Database.getInstance().getFermentables().get(ia2.getIngredient());

					Fermentable.Type type1 = f1.getType();
					Fermentable.Type type2 = f2.getType();

					if (type1 == type2)
					{
						// quantity desc
						sortOrder1 = ia2.getQuantity().get();
						sortOrder2 = ia1.getQuantity().get();
					}
					else
					{
						// type asc
						sortOrder1 = type1.getSortOrder();
						sortOrder2 = type2.getSortOrder();
					}
				}
				else if (ia1.getType() == IngredientAddition.Type.MISC)
				{
					// order miscs by type (water agents by acid content desc), then quantity.

					Misc m1 = Database.getInstance().getMiscs().get(ia1.getIngredient());
					Misc m2 = Database.getInstance().getMiscs().get(ia2.getIngredient());

					Misc.Type type1 = m1.getType();
					Misc.Type type2 = m2.getType();

					if (type1 == type2)
					{
						PercentageUnit ac1 = m1.getAcidContent();
						PercentageUnit ac2 = m2.getAcidContent();
						if (ac1 != null && ac2 != null && ac1.get() > 0 && ac2.get() > 0)
						{
							// acid content descending
							sortOrder1 = ac2.get();
							sortOrder2 = ac1.get();
						}
						else
						{
							sortOrder1 = ia2.getQuantity().get();
							sortOrder2 = ia1.getQuantity().get();
						}
					}
					else
					{
						sortOrder1 = type1.getSortOrder();
						sortOrder2 = type2.getSortOrder();
					}
				}
				else
				{
					// sort Hops, Yeast and Water by quantity desc
					sortOrder1 = ia2.getQuantity().get();
					sortOrder2 = ia1.getQuantity().get();
				}
			}

			return (int)(sortOrder1 - sortOrder2);
		};
	}
}
