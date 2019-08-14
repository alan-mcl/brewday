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
class BeerVolume// extends FluidVolume
{
//	private DensityUnit originalGravity = new DensityUnit();
//
//	/*-------------------------------------------------------------------------*/
//	public BeerVolume()
//	{
//		super(Type.BEER);
//	}
//
//	/*-------------------------------------------------------------------------*/
//	public BeerVolume(
//		VolumeUnit volume,
//		TemperatureUnit temperature,
//		DensityUnit originalGravity,
//		DensityUnit gravity,
//		double abv,
//		ColourUnit colour,
//		BitternessUnit bitterness)
//	{
//		super(Type.BEER, temperature, colour, bitterness, gravity, volume, abv);
//		this.originalGravity = originalGravity;
//	}
//
//	/*-------------------------------------------------------------------------*/
//	@Override
//	public String describe()
//	{
//		double t = getTemperature()==null ? Double.NaN : getTemperature().get(Quantity.Unit.CELSIUS);
//		double v = getVolume()==null ? Double.NaN : getVolume().get(Quantity.Unit.LITRES);
//		double g = getGravity()==null ? Double.NaN : getGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY);
//		double c = getColour()==null ? Double.NaN : getColour().get(Quantity.Unit.SRM);
//		double b = getBitterness()==null ? Double.NaN : getBitterness().get(Quantity.Unit.IBU);
//
//		return
//			StringUtils.getProcessString("volumes.beer.format",
//				getType().toString(),
//				getName(),
//				v,
//				getOriginalGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY),
//				getGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY),
//				c,
//				b,
//				getAbv()*100);
//	}
//
//	/*-------------------------------------------------------------------------*/
//	@Override
//	public Volume clone()
//	{
//		return new BeerVolume(
//			getVolume(),
//			getTemperature(),
//			originalGravity,
//			getGravity(),
//			getAbv(),
//			getColour(),
//			getBitterness());
//	}
//
//	/*-------------------------------------------------------------------------*/
//
//	public DensityUnit getOriginalGravity()
//	{
//		return originalGravity;
//	}
//
//	public void setOriginalGravity(DensityUnit originalGravity)
//	{
//		this.originalGravity = originalGravity;
//	}
}
