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

package mclachlan.brewday.process;

/**
 *
 */
class WortVolume //extends FluidVolume
{
//	private Fermentability fermentability;
//
//	/*-------------------------------------------------------------------------*/
//	public WortVolume()
//	{
//		super(Type.WORT);
//	}
//
//	/*-------------------------------------------------------------------------*/
//	public WortVolume(
//		VolumeUnit volume,
//		TemperatureUnit temperature,
//		Fermentability fermentability,
//		DensityUnit gravity,
//		double abv,
//		ColourUnit colour,
//		BitternessUnit bitterness)
//	{
//		super(Type.WORT, temperature, colour, bitterness, gravity, volume, abv);
//		this.fermentability = fermentability;
//	}
//
//	/*-------------------------------------------------------------------------*/
//	public String describe()
//	{
//		double t = getTemperature()==null ? Double.NaN : getTemperature().get(Quantity.Unit.CELSIUS);
//		double v = getVolume()==null ? Double.NaN : getVolume().get(Quantity.Unit.LITRES);
//		double g = getGravity()==null ? Double.NaN : getGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY);
//		double c = getColour()==null ? Double.NaN : getColour().get(Quantity.Unit.SRM);
//
//		return
//			StringUtils.getProcessString("volumes.wort.format",
//				getType().toString(),
//				getName(),
//				v,
//				t,
//				g,
//				c);
//	}
//
//	/*-------------------------------------------------------------------------*/
//
//	public PercentageUnit getFermentability()
//	{
//		return fermentability;
//	}
//
//	public void setFermentability(Fermentability fermentability)
//	{
//		this.fermentability = fermentability;
//	}
//
//
//
//	/*-------------------------------------------------------------------------*/
//
//	@Override
//	public Volume clone()
//	{
//		return new WortVolume(
//			getVolume(),
//			getTemperature(),
//			fermentability,
//			getGravity(),
//			getAbv(),
//			getColour(),
//			getBitterness());
//	}

}
