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
public abstract class Volume
{
	private Type type;

	/*-------------------------------------------------------------------------*/
	protected Volume()
	{
	}

	protected Volume(Type type)
	{
		this.type = type;
	}

	/*-------------------------------------------------------------------------*/

	public Type getType()
	{
		return type;
	}

	/**
	 * @return the unique name of this volume;
	 */
	public abstract String getName();

	/**
	 * @param name unique name for this volume
	 */
	public abstract void setName(String name);

	/**
	 * @return a brief text description of this volume
	 */
	public abstract String describe();

	/**
	 * @return a deep clone of this volume
	 */
	public abstract Volume clone();

	/*-------------------------------------------------------------------------*/
	public static enum Type
	{
		MASH("Mash", 1),
		WORT("Wort", 2),
		BEER("Beer", 3);

		private String name;
		private int sortOrder;

		Type(String name, int sortOrder)
		{
			this.name = name;
			this.sortOrder = sortOrder;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public int getSortOrder()
		{
			return sortOrder;
		}
	}
}
