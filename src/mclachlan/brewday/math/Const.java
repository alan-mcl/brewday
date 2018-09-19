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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.math;

/**
 *
 */
public class Const
{
	// todo: stuff to be parameterised
	public static double MASH_EFFICIENCY = 0.7D;

	// conversion constants
	public static double GRAMS_PER_POUND = 455D;
	public static double LITERS_PER_GALLON = 3.78D;

	/** boil off rate in l per h */
	public static double BOIL_OFF_PER_HOUR = 3D;

	/** used in ABV equation */
	public static double ABV_CONST = 1.05D / 0.79D;

	/** volume shrinkage in % per deg C */
	public static double COOLING_SHRINKAGE = 0.04D/80;

	/** heat loss assuming an ambient temperature of 30C, in deg C per h */
	public static double HEAT_LOSS = 30D;

	/** specific heat of water, in Kj/(kg*K) */
	public static double SPECIFIC_HEAT_OF_WATER = 4.2D;

	/** dry grain absorption of water, in l per kg.
	 * Source: https://byo.com/article/calculating-water-usage-advanced-brewing/ */
	public static double GRAIN_WATER_ABSORPTION = 1.04D;

	/** grain displacement of water, in l per kg
	 * Source: https://byo.com/article/calculating-water-usage-advanced-brewing/ */
	public static double GRAIN_WATER_DISPLACEMENT = 0.67D;

	/** % colour lost during fermentation
	 * Source: http://www.learntobrew.com/beer-calculations/*/
	public static double COLOUR_LOSS_DURING_FERMENTATION = 0.3D;

	/** Thermodynamic constant for working out mash temp:
	 * Source: http://howtobrew.com/book/section-3/the-methods-of-mashing/calculations-for-boiling-water-additions
	 */
	public static double MASH_TEMP_THERMO_CONST = 0.41D;
}
