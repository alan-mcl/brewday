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

package mclachlan.brewday.db;

import java.lang.reflect.Constructor;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.v2.V2SerialiserObject;
import mclachlan.brewday.math.Quantity;

/**
 * Just saves the value out, in the default unit.
 */
public class QuantityValueSerialiser<T extends Quantity> implements V2SerialiserObject<T>
{
	private Class clazz;

	public QuantityValueSerialiser(Class clazz)
	{
		this.clazz = clazz;
	}

	@Override
	public Object toObj(T t)
	{
		return String.valueOf(t.get());
	}

	@Override
	public T fromObj(Object obj)
	{
		try
		{

			if (obj == null || "".equals(obj))
			{
				Constructor declaredConstructor = clazz.getDeclaredConstructor();
				return (T)declaredConstructor.newInstance();
			}
			else
			{
				double value;
				if (obj instanceof Double)
				{
					value = (Double)obj;
				}
				else
				{
					value = Double.valueOf((String)obj);
				}
				Constructor declaredConstructor = clazz.getDeclaredConstructor(double.class);
				return (T)declaredConstructor.newInstance((double)value);
			}
		}
		catch (Exception  e)
		{
			throw new BrewdayException(e);
		}
	}
}
