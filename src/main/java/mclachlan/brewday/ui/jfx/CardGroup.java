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

import java.util.*;
import javafx.scene.Group;
import javafx.scene.Node;
import mclachlan.brewday.BrewdayException;

/**
 *
 */
public class CardGroup extends Group
{
	private Map<String, Node> childMap = new HashMap<>();

	public void add(String key, Node child)
	{
		childMap.put(key, child);
		this.getChildren().add(child);
		child.setVisible(false);
	}

	public void setVisible(String key)
	{
		childMap.values().forEach(node -> node.setVisible(false));
		Node node = childMap.get(key);
		if (node != null)
		{
			node.setVisible(true);
		}
		else
		{
			throw new BrewdayException("Invalid: "+key);
		}
	}

	public Node getVisible()
	{
		for (Node node : childMap.values())
		{
			if (node.isVisible())
			{
				return node;
			}
		}

		throw new BrewdayException("No card is visible?");
	}

	public Node getChild(String key)
	{
		return childMap.get(key);
	}
}
