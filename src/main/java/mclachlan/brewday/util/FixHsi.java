
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
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.PercentageUnit;

public class FixHsi
{
	public static void main(String[] args)
	{
		Database.getInstance().loadAll();

		for (Hop f : Database.getInstance().getHops().values())
		{
			f.setHopStorageIndex(new PercentageUnit(f.getHopStorageIndex().get()/100D));
			System.out.println(f.getName());
		}

		Database.getInstance().saveAll();
	}
}
