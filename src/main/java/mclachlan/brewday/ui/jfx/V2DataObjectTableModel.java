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
