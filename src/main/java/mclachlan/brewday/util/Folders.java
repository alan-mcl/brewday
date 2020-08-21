
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

import java.util.*;

/**
 *
 */
public class Folders<T extends HasPath>
{
	private TreeNode<T> root;

	/*-------------------------------------------------------------------------*/

	public Folders(String rootName)
	{
		root = new TreeNode<>(rootName);
	}

	/*-------------------------------------------------------------------------*/
	public void init(Map<String, T> items)
	{
		for (T t : items.values())
		{
			addAtPath(t.getPath(), t);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void addAtPath(String path, T t)
	{
		String[] pathElems = t.getPath().split("/");
		if (pathElems.length > 0)
		{
			for (String pathElem : pathElems)
			{
//				root
//				todo
			}
		}
		else
		{
			root.addContent(t);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static class TreeNode<T>
	{
		private String name;
		private List<T> contents = new ArrayList<>();
		private List<TreeNode> children = new ArrayList<>();

		public TreeNode(String name)
		{
			this.name = name;
		}

		public void addChild(TreeNode t)
		{
			this.children.add(t);
		}

		public void addContent(T t)
		{
			this.contents.add(t);
		}

		public List<T> getContents()
		{
			return contents;
		}

		public List<TreeNode> getChildren()
		{
			return children;
		}
	}
}
