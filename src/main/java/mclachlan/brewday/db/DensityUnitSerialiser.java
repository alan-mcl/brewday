package mclachlan.brewday.db;

import mclachlan.brewday.db.v2.V2SerialiserObject;
import mclachlan.brewday.math.DensityUnit;

/**
 *
 */
public class DensityUnitSerialiser implements V2SerialiserObject<DensityUnit>
{
	@Override
	public Object toObj(DensityUnit densityUnit)
	{
		return densityUnit.get();
	}

	@Override
	public DensityUnit fromObj(Object obj)
	{
		return new DensityUnit((double)obj);
	}
}
