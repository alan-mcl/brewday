/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.math;

import java.util.*;
import mclachlan.brewday.ingredients.Yeast;

/**
 *
 */
public class Const
{
	// conversion constants
	public static double GRAMS_PER_POUND = 455D;
	public static double GRAMS_PER_OUNCE = 28.3495D;
	public static double L_PER_US_GALLON = 3.78D;
	public static double ML_PER_US_GALLON = 3785.41D;
	public static double ML_PER_US_FL_OZ = 29.5735D;

	/** used in ABV equation */
	public static double ABV_CONST = 131.25D;

	/** volume shrinkage in % per deg C */
	public static double COOLING_SHRINKAGE = 0.04D/80;

	/** heat loss assuming an ambient temperature of 30C, in deg C per h */
	public static double HEAT_LOSS = 30D;

	/** specific heat of water, in Kj/(kg*K) */
	public static double SPECIFIC_HEAT_OF_WATER = 4.2D;

	/** dry grain absorption of water, in L per kg.
	 * http://braukaiser.com/wiki/index.php/Batch_Sparge_and_Party_Gyle_Simulator */
	public static double GRAIN_WATER_ABSORPTION = 1D;

	/** grain displacement of water, in L per kg
	 * Source: https://byo.com/article/calculating-water-usage-advanced-brewing/ */
	public static double GRAIN_WATER_DISPLACEMENT = 0.67D;

	/** % colour lost during fermentation, heuristic */
	public static double COLOUR_LOSS_DURING_FERMENTATION = 0.02D;

	/** fraction of iso-alpha mass retained in beer after fermentation (1.0 = no loss) */
	public static double ISO_ALPHA_RETENTION_DURING_FERMENTATION = 0.85D;

	/** Recommended mash pH range (Palmer, Troester). Outside this range raises a warning. */
	public static double MASH_PH_LOW = 5.2D;
	public static double MASH_PH_HIGH = 5.6D;

	/** Sparge water pH above this risks tannin/silicate extraction (Palmer, Briggs, Kunze). */
	public static double SPARGE_WATER_PH_MAX = 6.0D;

	/** Estimated runoff/collected wort pH above this promotes tannin extraction. */
	public static double RUNOFF_PH_MAX = 6.0D;

	/** Expected finished beer pH range. Outside this range raises a warning. */
	public static double BEER_PH_LOW = 3.8D;
	public static double BEER_PH_HIGH = 4.8D;

	/**
	 * Empirical pH drop applied to wort pH at the wort-to-beer transition, keyed by yeast
	 * type. Intended for prediction and reporting only; not a chemistry simulation.
	 */
	public static Map<Yeast.Type, Double> FERMENTATION_PH_DROP =
		Map.of(
			Yeast.Type.ALE, 1.0D,
			Yeast.Type.LAGER, 0.8D,
			Yeast.Type.WHEAT, 1.0D,
			Yeast.Type.CHAMPAGNE, 1.2D,
			Yeast.Type.WINE, 1.2D);

	/** Default pH drop when the yeast type has no configured value. */
	public static double FERMENTATION_PH_DROP_DEFAULT = 1.0D;

	/**
	 * Optional, off-by-default empirical kettle-pH hop-utilisation correction. Literature is
	 * inconsistent and existing IBU models ignore pH, so this stays disabled unless enabled here.
	 */
	public static boolean BOIL_PH_UTILISATION_ENABLED = false;

	/** Thermodynamic constant for working out mash temp:
	 * Source: http://howtobrew.com/book/section-3/the-methods-of-mashing/calculations-for-boiling-water-additions
	 */
	public static double MASH_TEMP_THERMO_CONST = 0.41D;

	/**
	 * 1 atmosphere pressure in kPa
	 */
	public static PressureUnit ONE_ATMOSPHERE_IN_KPA =
		new PressureUnit(101.325D, Quantity.Unit.KPA, true);
}
