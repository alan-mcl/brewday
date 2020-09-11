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
import java.util.function.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableView;
import mclachlan.brewday.db.v2.V2DataObject;

/**
 *
 */
class V2DataObjectTableModel<T extends V2DataObject>
{
	private final TableView<T> tableView;
	private Map<String, T> map;
	private final ObservableList<T> unfilteredList;
	private final FilteredList<T> filteredList;

	public V2DataObjectTableModel(TableView<T> tableView)
	{
		this.tableView = tableView;
		unfilteredList = FXCollections.observableArrayList();
		filteredList = new FilteredList<>(unfilteredList);
		SortedList<T> sortedList = new SortedList<>(filteredList);
		this.tableView.setItems(sortedList);
		sortedList.comparatorProperty().bind(tableView.comparatorProperty());
	}

	public void refresh(Map<String, T> map)
	{
		filter(t -> true);
		unfilteredList.clear();

		this.map = map;

		unfilteredList.addAll(map.values());
		tableView.sort();
	}

	public void add(T t)
	{
		this.map.put(t.getName(), t);
		unfilteredList.add(t);
		tableView.sort();
	}

	public void remove(T t)
	{
		this.map.remove(t.getName());
		unfilteredList.remove(t);
	}

	public void filter(Predicate<T> predicate)
	{
		filteredList.setPredicate(predicate);
	}
}
