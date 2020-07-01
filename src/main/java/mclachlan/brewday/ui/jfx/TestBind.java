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

package mclachlan.brewday.ui.jfx;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 */
public class TestBind
{
	public static void main(String[] args) throws Exception
	{
		StringProperty p1 = new SimpleStringProperty();
		StringProperty p2 = new SimpleStringProperty();

		System.out.println("p2.get() = [" + p2.get() + "]");

		p1.bindBidirectional(p2);

		p1.set("2");


		System.out.println("p2.get() = [" + p2.get() + "]");
	}
}
