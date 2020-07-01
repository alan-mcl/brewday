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

import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import mclachlan.brewday.db.v2.V2DataObject;

/**
 *
 */
class V2DataObjectTableModel<T extends V2DataObject>
{
	private final ObservableList<T> list = FXCollections.observableArrayList();
	private final TableView<T> tableView;
	private Map<String, T> map;

	public V2DataObjectTableModel(TableView<T> tableView)
	{
		this.tableView = tableView;
		this.tableView.setItems(this.list);
	}

	public void refresh(Map<String, T> map)
	{
		this.list.clear();
		this.map = map;
		list.addAll(map.values());
		tableView.sort();
	}

	public void add(T t)
	{
		this.map.put(t.getName(), t);
		this.list.add(t);
		tableView.sort();
	}

	public void remove(T t)
	{
		this.map.remove(t.getName());
		this.list.remove(t);
	}
}
