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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import mclachlan.brewday.db.v2.V2DataObject;

/**
 *
 */
class DirtyList<T extends V2DataObject> extends ListView<Label>
{
	private final Map<T, Label> nodes = new HashMap<>();
	private final Map<Label, T> values = new HashMap<>();

	public void add(T t, Image graphic)
	{
		Label label = new DirtyLabel<>(t, t.getName(), JfxUi.getImageView(graphic, 24));
		nodes.put(t, label);
		values.put(label, t);
		super.getItems().add(label);
	}

	public void addAll(List<T> ts, Image graphic)
	{
		for (T t : ts)
		{
			add(t, graphic);
		}
	}

	public T getValue(Label label)
	{
		return values.get(label);
	}

	public void removeAll()
	{
		this.getChildren().clear();
	}

	public void select(T t)
	{
		getSelectionModel().select(nodes.get(t));
	}

	public void setDirty(T t)
	{
		Label label = nodes.get(t);
		label.setStyle("-fx-font-weight: bold;");
	}

	public void clearAllDirty()
	{
		for (Label l : nodes.values())
		{
			l.setStyle("");
		}
	}

	private static class DirtyLabel<T> extends Label
	{
		private T t;

		public DirtyLabel(T t, String text, Node graphic)
		{
			super(text, graphic);
			this.t = t;
		}
	}

}
