package mclachlan.brewday.ui.jfx;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 *
 */
class DirtyTableViewRowFactory<T> implements Callback<TableView<T>, TableRow<T>>
{
	private final TableView<T> tableView;
	private final ObservableList<T> dirtyList = FXCollections.observableArrayList();

	public DirtyTableViewRowFactory(TableView<T> tableView)
	{
		this.tableView = tableView;

		dirtyList.addListener((ListChangeListener<T>)c -> refreshTableView());
	}

	public void setDirty(T t)
	{
		dirtyList.add(t);
	}

	private void refreshTableView()
	{
		// tableView.refresh();
		// this dodgy hack required because the above does not do the trick
		((TableColumn<T, ?>)(tableView.getColumns().get(0))).setVisible(false);
		((TableColumn<T, ?>)(tableView.getColumns().get(0))).setVisible(true);
	}

	public void clearAllDirty()
	{
		dirtyList.clear();
	}

	@Override
	public TableRow<T> call(TableView param)
	{
		return new TableRow<>()
		{
			private final SimpleBooleanProperty dirty = new SimpleBooleanProperty();

			/* Current Item which is bound to this table row */
			private T currentItem = null;

			{
				dirty.addListener((observable, oldValue, newValue) ->
				{
					if (currentItem != null && currentItem == getItem())
					{
						updateItem(getItem(), isEmpty());
					}
				});

				itemProperty().addListener((observable, oldValue, newValue) ->
				{
					dirty.unbind();

					if (newValue != null)
					{
						currentItem = newValue;
					}
				});
			}

			@Override
			protected void updateItem(T item, boolean empty)
			{
				super.updateItem(item, empty);

				if (dirtyList.contains(item))
				{
					this.setStyle("-fx-font-weight: bold");
				}
				else
				{
					this.setStyle("");
				}
			}
		};
	}
}
