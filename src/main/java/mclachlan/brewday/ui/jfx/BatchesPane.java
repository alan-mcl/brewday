/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the batche that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.scene.Parent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;

import static mclachlan.brewday.StringUtils.getUiString;

/**
 *
 */
public class BatchesPane extends V2DataObjectPane<Batch>
{
	/*-------------------------------------------------------------------------*/
	public BatchesPane(String dirtyFlag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "batch", Icons.beerIcon, Icons.newIcon);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Parent editItemDialog(Batch obj, TrackDirty parent)
	{
		return new BatchEditor(obj, this);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Batch createDuplicateItem(Batch current, String newName)
	{
		Batch result = new Batch(current);
		result.setName(newName);
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Batch createNewItem(String name)
	{
		// no op because we override the below method
		return null;
	}

	@Override
	protected Batch newItemDialog(String labelPrefix, Image addIcon)
	{
		NewBatchDialog dialog = new NewBatchDialog();

		dialog.showAndWait();
		return dialog.getOutput();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Map<String, Batch> getMap(Database database)
	{
		return database.getBatches();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void tableInitialSort(TableView<Batch> table)
	{
		// start sorted by name
		TableColumn<Batch, ?> dateCol = table.getColumns().get(3);
		dateCol.setSortType(TableColumn.SortType.DESCENDING);
		table.getSortOrder().setAll(dateCol);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected TableColumn<Batch, String>[] getTableColumns(String labelPrefix)
	{
		TableColumn<Batch, LocalDate> dateCol = new TableColumn<>(getUiString(labelPrefix + ".date"));
		dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
		dateCol.setCellFactory(column -> new TableCell<>()
		{
			final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");

			@Override
			protected void updateItem(LocalDate item, boolean empty)
			{
				super.updateItem(item, empty);
				if (empty)
				{
					setText(null);
				}
				else
				{
					setText(dtf.format(item));
				}
			}
		});
		dateCol.setComparator(LocalDate::compareTo);

		return (TableColumn<Batch, String>[])new TableColumn[]
			{
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".recipe", "recipe"),
				dateCol,
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".desc", "description")
			};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeRename(String oldName, String newName)
	{
		// no op
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeDelete(String deletedName)
	{
		// no op
	}

	@Override
	protected Image getIcon(Batch batch)
	{
		return Icons.beerIcon;
	}
}
