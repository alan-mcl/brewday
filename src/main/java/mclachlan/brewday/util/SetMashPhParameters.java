
/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.util;

import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.Quantity;

/**
 * Updates the distilled water pH based on the MpH 4.2 mappings.
 */
public class SetMashPhParameters
{
	public static void main(String[] args)
	{
		Database.getInstance().loadAll();

		for (Fermentable f : Database.getInstance().getFermentables().values())
		{
			if (f.getType() == Fermentable.Type.GRAIN)
			{
				f.setDistilledWaterPh(new PhUnit(0));
				setMashPhParameters(f);
				System.out.println(f.getName() + ": " + f.getDistilledWaterPh().get() +" ("+f.getColour().get(Quantity.Unit.LOVIBOND)+")");
			}
		}

//		Database.getInstance().saveAll();
	}

	/*-------------------------------------------------------------------------*/
	public static void setMashPhParameters(Fermentable f)
	{
		if (procFlakedGrain(f)) return;
		if (procWheat(f)) return;
		if (procSpecialtyAndDarkRoasted(f)) return;
		if (procCrystal(f)) return;
		if (procBase(f)) return;

		procLastChance(f);
	}

	/*-------------------------------------------------------------------------*/
	private static void procLastChance(Fermentable f)
	{
		// we need to assign something

		if (nameContainsOr(f, "malted", "smoked", "rye malt"))
		{
			// treat like a munich malt
			updateGrain(f, 5.57,	53.7);
		}
		else if (nameContainsOr(f, "acid"))
		{
			// TODO acid malts!
		}
		else
		{
			// no idea what this is, treat like a pale base malt
			updateGrain(f, 5.72,	45.5);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static boolean procBase(Fermentable f)
	{
		if (procAnd(f, 5.78,	51.7,"briess", "2-row")) return true;
		if (procAnd(f, 5.80,	42.4,"rahr", "pils")) return true;
		if (procAnd(f, 5.62,	47.2,"weyermann", "pneumatic", "pils")) return true;
		if (procAnd(f, 5.85,	34.4,"weyermann", "floor", "malted", "pils")) return true;
		if (procAnd(f, 5.67,	49.7,"rahr", "pale", "ale")) return true;
		if (procAnd(f, 5.69,	49.5,"crisp", "maris", "otter")) return true;
		if (procAnd(f, 5.84,	51.4,"muntons", "maris", "otter")) return true;
		if (procAnd(f, 5.65,	51.8,"weyermann", "vienna")) return true;
		if (procAnd(f, 5.65,	57.6,"briess", "goldpils", "vienna")) return true;
		if (procAnd(f, 5.50,	59.2,"briess", "ashburne", "mild")) return true;
		if (procAnd(f, 5.51,	49.0,"weyermann", "munich i")) return true;
		if (procAnd(f, 5.62,	60.7,"franco", "belges", "munich light")) return true;
		if (procAnd(f, 5.54,	56.7,"weyermann", "munich ii")) return true;
		if (procAnd(f, 5.62,	53.5,"briess", "munich 10l")) return true;
		if (procAnd(f, 4.39,	47.5,"briess", "aromatic")) return true;
		if (procAnd(f, 5.38,	64.7,"dingemans", "aromatic")) return true;

		// generic values
		if (procOr(f, 5.39,	55.1,"aromatic")) return true;
		if (procOr(f, 5.57,	53.7,"munich", "redx")) return true;
		if (procOr(f, 5.69,	52.3,"pale ale", "vienna", "mild")) return true;
		if (procOr(f, 5.72,	45.5,"pils", "pilsner", "lager", "2-row", "6-row", "2 row", "6 row", "pale")) return true;

		return false;
	}

	/*-------------------------------------------------------------------------*/
	private static boolean procCrystal(Fermentable f)
	{
		if (nameContainsOr(f, "cara", "caramel", "crystal", "dextrin"))
		{
			if (procAnd(f, 5.54,	33.2,"crisp", "dextrin")) return true;
			if (procAnd(f, 5.88,	36.5,"weyermann", "carafoam")) return true;
			if (procAnd(f, 5.28,	46.8,"briess", "caramel c10")) return true;
			if (procAnd(f, 5.18,	46.7,"simpsons", "caramalt")) return true;
			if (procAnd(f, 5.09,	55.7,"briess", "caramel l20")) return true;
			if (procAnd(f, 5.43,	48.6,"cargill", "caramel 20")) return true;
			if (procAnd(f, 4.87,	65.4,"briess", "caramel 40")) return true;
			if (procAnd(f, 4.92,	53.3,"simpsons", "crystal light")) return true;
			if (procAnd(f, 5.10,	60.5,"weyermann", "caramunich i")) return true;
			if (procAnd(f, 4.71,	80.2,"weyermann", "caramunich ii")) return true;
			if (procAnd(f, 4.76,	71.7,"briess", "caramel 60")) return true;
			if (procAnd(f, 4.97,	68.6,"cargill", "caramel 60")) return true;
			if (procAnd(f, 4.92,	64.8,"weyermann", "caramunich iii")) return true;
			if (procAnd(f, 4.73,	72.6,"briess", "caramel 80")) return true;
			if (procAnd(f, 4.77,	78.4,"briess", "caramel 90")) return true;
			if (procAnd(f, 4.71,	77.6,"briess", "caramel 120")) return true;
			if (procAnd(f, 4.58,	74.2,"simpsons", "drc")) return true;
			if (procAnd(f, 4.48,	79.40,"briess", "caramel 150")) return true;
			if (procAnd(f, 4.48,	98.80,"weyermann", "caraaroma")) return true;


			// generic options
			if (procAnd(f, 5.71,	34.8,"dextrin")) return true;

			double c = f.getColour().get(Quantity.Unit.LOVIBOND);
			if (c <= 15) { updateGrain(f, 5.26,	49.3); return true; }
			if (c <= 30) { updateGrain(f, 5.15,	54.5); return true; }
			if (c <= 50) { updateGrain(f, 4.89,	65.0); return true; }
			if (c <= 70) { updateGrain(f, 4.81,	68.3); return true; }
			if (c <= 100) { updateGrain(f, 4.74,	74.0); return true; }
			if (c <= 130) { updateGrain(f, 4.68,	77.0); return true; }
			if (c > 130) { updateGrain(f, 4.48,	89.10); return true; }
		}

		return false;
	}

	/*-------------------------------------------------------------------------*/
	private static boolean procSpecialtyAndDarkRoasted(Fermentable f)
	{
		if (nameContainsOr(f, "amber", "biscuit", "victory", "abbey", "aromatic", "aromamalt", "melanoidin", "melanoiden", "honey", "imperial", "toasted", "brumalt"))
		{
			if (procAnd(f, 5.19, 36.8, "briess", "victory")) return true;
			if (procAnd(f, 5.1, 35.5, "crisp", "amber")) return true;
			if (procAnd(f, 4.94, 62.6, "weyermann", "melanoidin")) return true;
			if (procAnd(f, 4.82, 95.4, "gambrinus", "honey")) return true;

			// generic biscuit/amber
			updateGrain(f, 5.14, 39.1);
			return true;
		}
		else if (nameContainsOr(f, "brown", "special", "kiln", "roast", "café", "cafe")
			&& f.getColour().get(Quantity.Unit.LOVIBOND) < 200)
		{
			if (procAnd(f, 5.19, 31.4, "briess", "carabrown")) return true;
			if (procAnd(f, 4.91, 99.1, "briess", "special", "roast")) return true;
			if (procAnd(f, 4.55, 58.8, "briess", "special", "extra")) return true;

			// generic brown
			updateGrain(f, 5.1, 39.0);
			return true;
		}
		else if (nameContainsOr(f, "roast", "chocolate", "choc", "black", "carafa", "choklad"))
		{
			if (procAnd(f, 4.69, 66.0, "briess", "roasted", "barley")) return true;
			if (procAnd(f, 4.61, 69.2, "briess", "black", "barley")) return true;
			if (procAnd(f, 4.43, 62.7, "briess", "dark", "chocolate")) return true;
			if (procAnd(f, 4.66, 64.9, "briess", "chocolate")) return true;
			if (procAnd(f, 4.24, 59.8, "briess", "black")) return true;
			if (procAnd(f, 4.7, 78.7, "crisp", "chocolate")) return true;
			if (procAnd(f, 4.55, 78.2, "simpsons", "chocolate")) return true;
			if (procAnd(f, 4.57, 77, "simpsons", "black")) return true;
			if (procAnd(f, 4.73, 77.5, "weyermann", "carafa i", "special")) return true;
			if (procAnd(f, 4.71, 68.7, "weyermann", "carafa i")) return true;
			if (procAnd(f, 4.7, 68.7, "weyermann", "carafa ii", "special")) return true;
			if (procAnd(f, 4.81, 64.4, "weyermann", "carafa iii")) return true;

			// generic dark roasted
			updateGrain(f, 4.64, 68.7);
			return true;
		}

		return false;
	}

	/*-------------------------------------------------------------------------*/
	private static boolean procWheat(Fermentable f)
	{
		if (nameContainsOr(f, "wheat"))
		{
			if (procAnd(f, 6.0, 38.8, "briess", "white")) return true;
			if (procAnd(f, 5.8, 34.1, "briess", "red")) return true;
			if (procAnd(f, 6.07, 34.6, "weyermann")) return true;

			if (!nameContainsOr(f, "chocolate", "choc", "roasted", "caramel", "cara", "crystal"))
			{
				updateGrain(f, 5.97, 34.8);
			}
		}

		return false;
	}

	/*-------------------------------------------------------------------------*/
	private static boolean procFlakedGrain(Fermentable f)
	{

		if (nameContainsOr(f, "flaked", "flakes", "torrified", "torrefied", "raw", "naked"))
		{
			if (procOr(f, 5.55, 47.2, "barley")) return true;
			if (procOr(f, 6.24, 9.6, "corn")) return true;
			if (procOr(f, 6.21, 48.2, "oats")) return true;
			if (procOr(f, 6.65, 29.8, "rye")) return true;
			if (procOr(f, 6.57, 28.2, "wheat")) return true;

			// could br rice, etc. treat with some average values
			updateGrain(f, 6.0, 30);
		}

		return false;
	}

	/*-------------------------------------------------------------------------*/
	private static boolean procOr(Fermentable f, double ph,
		double bc, String... s)
	{
		if (nameContainsOr(f, s))
		{
			updateGrain(f, ph, bc);
			return true;
		}
		return false;
	}

	/*-------------------------------------------------------------------------*/
	private static boolean procAnd(Fermentable f, double ph,
		double bc, String... s)
	{
		if (nameContainsAnd(f, s))
		{
			updateGrain(f, ph, bc);
			return true;
		}
		return false;
	}

	/*-------------------------------------------------------------------------*/
	private static void updateGrain(Fermentable f,
		double distilledWaterPh,
		double bufferingCapacity)
	{
		f.setDistilledWaterPh(new PhUnit(distilledWaterPh));
		// todo: buffering capacity
	}

	/*-------------------------------------------------------------------------*/
	private static boolean nameContainsOr(Fermentable f, String... ss)
	{
		for (String s : ss)
		{
			if (f.getName().toLowerCase().contains(s))
			{
				return true;
			}
		}
		return false;
	}

	/*-------------------------------------------------------------------------*/
	private static boolean nameContainsAnd(Fermentable f, String... ss)
	{
		boolean result = true;

		for (String s : ss)
		{
			result &= (f.getName().toLowerCase().contains(s));
		}

		return result;
	}

}
