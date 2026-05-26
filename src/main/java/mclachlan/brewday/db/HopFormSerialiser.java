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

package mclachlan.brewday.db;

import mclachlan.brewday.db.v2.V2SerialiserObject;
import mclachlan.brewday.ingredients.Hop;

/**
 * Custom serialiser for {@link Hop.Form} that handles the legacy
 * {@code "PELLET"} alias transparently on deserialisation.
 */
public class HopFormSerialiser implements V2SerialiserObject<Hop.Form>
{
	@Override
	public Object toObj(Hop.Form form)
	{
		return form.name();
	}

	@Override
	public Hop.Form fromObj(Object obj)
	{
		if (obj == null)
		{
			return null;
		}
		return Hop.Form.fromString((String)obj);
	}
}
