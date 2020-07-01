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

package mclachlan.brewday.db.v2;

/**
 * Can serialise type E to and from a Object representation
 */
public interface V2SerialiserObject<E>
{
	/**
	 * Serialise this type to a Object.
	 */
	Object toObj(E e);

	/**
	 * Deserialise this type from an Object.
	 */
	E fromObj(Object obj);
}
