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

package mclachlan.brewday.ui.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Base class for displaying and managing V2DataObjects in the Swing UI.
 * Equivalent to the JavaFX V2DataObjectPane.
 */
public abstract class V2DataObjectPane<T extends V2DataObject> extends JPanel implements TrackDirty
{
	/** The parent TrackDirty component */
	private final TrackDirty parent;
	
	/** The dirty flag for this component */
	private final String dirtyFlag;
	
	/** The table model for displaying data objects */
	private final V2DataObjectTableModel<T> tableModel = new V2DataObjectTableModel<>();
	
	/** The table for displaying data objects */
	private final JTable table = new JTable(tableModel);
	
	/** The table builder for creating columns */
	private final TableBuilder<T> tableBuilder = new TableBuilder<>();
	
	/** The set of dirty objects */
	private final Set<Object> dirtyObjects = new HashSet<>();
	
	/** The label prefix for UI strings */
	private final String labelPrefix;
	
	/** The icon for this data object type */
	private final Icon icon;
	
	/** The icon for adding new data objects */
	private final Icon addIcon;
	
	/**
	 * Constructor.
	 */
	public V2DataObjectPane(
		String dirtyFlag,
		TrackDirty parent,
		String labelPrefix,
		Icon icon,
		Icon addIcon)
	{
		this.dirtyFlag = dirtyFlag;
		this.parent = parent;
		this.labelPrefix = labelPrefix;
		this.icon = icon;
		this.addIcon = addIcon;
		
		// Set up the layout
		setLayout(new BorderLayout());
		
		// Create the toolbar
		JToolBar toolBar = buildToolBar(dirtyFlag, parent, labelPrefix, addIcon);
		add(toolBar, BorderLayout.NORTH);
		
		// Set up the table
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoCreateRowSorter(true);
		
		// Add the table columns
		TableColumn iconCol = tableBuilder.getIconColumn(this::getIcon);
		table.getColumnModel().addColumn(iconCol);
		
		TableColumn nameCol = tableBuilder.getStringPropertyValueCol(labelPrefix + ".name", "name");
		table.getColumnModel().addColumn(nameCol);
		
		// Add additional columns specific to the data object type
		TableColumn[] additionalColumns = getTableColumns(labelPrefix);
		if (additionalColumns != null)
		{
			for (TableColumn column : additionalColumns)
			{
				table.getColumnModel().addColumn(column);
			}
		}
		
		// Add the table to a scroll pane
		JScrollPane scrollPane = new JScrollPane(table);
		add(scrollPane, BorderLayout.CENTER);
		
		// Initial sort
		tableInitialSort(table);
		
		// Refresh the data
		refresh(Database.getInstance());
	}
	
	/**
	 * Build the toolbar.
	 */
	protected JToolBar buildToolBar(
		String dirtyFlag,
		TrackDirty parent,
		String labelPrefix,
		Icon addIcon)
	{
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		
		// Add button
		JButton addButton = new JButton(getUiString("ui.add"), addIcon);
		addButton.addActionListener(e -> {
			T newItem = newItemDialog(labelPrefix, addIcon);
			if (newItem != null)
			{
				refresh(Database.getInstance());
				parent.setDirty(dirtyFlag);
			}
		});
		toolBar.add(addButton);
		
		// Edit button
		JButton editButton = new JButton(getUiString("ui.edit"));
		editButton.addActionListener(e -> {
			int selectedRow = table.getSelectedRow();
			if (selectedRow >= 0)
			{
				int modelRow = table.convertRowIndexToModel(selectedRow);
				T selectedItem = tableModel.getItemAt(modelRow);
				
				Component editor = editItemDialog(selectedItem, parent);
				if (editor != null)
				{
					// Show the editor in a dialog
					JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), 
						getUiString("ui.edit") + " " + selectedItem.getName(), 
						Dialog.ModalityType.APPLICATION_MODAL);
					dialog.setContentPane((Container)editor);
					dialog.pack();
					dialog.setLocationRelativeTo(this);
					dialog.setVisible(true);
					
					refresh(Database.getInstance());
				}
			}
		});
		toolBar.add(editButton);
		
		// Delete button
		JButton deleteButton = new JButton(getUiString("ui.delete"));
		deleteButton.addActionListener(e -> {
			int selectedRow = table.getSelectedRow();
			if (selectedRow >= 0)
			{
				int modelRow = table.convertRowIndexToModel(selectedRow);
				T selectedItem = tableModel.getItemAt(modelRow);
				
				int result = JOptionPane.showConfirmDialog(this,
					getUiString("ui.delete.confirm") + " " + selectedItem.getName() + "?",
					getUiString("ui.delete"),
					JOptionPane.YES_NO_OPTION);
				
				if (result == JOptionPane.YES_OPTION)
				{
					delete(dirtyFlag, labelPrefix);
					refresh(Database.getInstance());
					parent.setDirty(dirtyFlag);
				}
			}
		});
		toolBar.add(deleteButton);
		
		// Duplicate button
		JButton duplicateButton = new JButton(getUiString("ui.duplicate"));
		duplicateButton.addActionListener(e -> {
			int selectedRow = table.getSelectedRow();
			if (selectedRow >= 0)
			{
				int modelRow = table.convertRowIndexToModel(selectedRow);
				T selectedItem = tableModel.getItemAt(modelRow);
				
				String newName = JOptionPane.showInputDialog(this,
					getUiString("ui.duplicate.prompt"),
					getUiString("ui.duplicate"),
					JOptionPane.QUESTION_MESSAGE);
				
				if (newName != null && !newName.isEmpty())
				{
					Map<String, T> map = getMap(Database.getInstance());
					if (map.containsKey(newName))
					{
						JOptionPane.showMessageDialog(this,
							getUiString("ui.duplicate.exists"),
							getUiString("ui.error"),
							JOptionPane.ERROR_MESSAGE);
					}
else
{
						T newItem = createDuplicateItem(selectedItem, newName);
						map.put(newName, newItem);
						refresh(Database.getInstance());
						parent.setDirty(dirtyFlag);
					}
				}
			}
		});
		toolBar.add(duplicateButton);
		
		// Export CSV button
		JButton exportButton = new JButton(getUiString("ui.export.csv"));
		exportButton.addActionListener(e -> {
			List<T> selectedItems = new ArrayList<>();
			int[] selectedRows = table.getSelectedRows();
			
			if (selectedRows.length == 0)
			{
				// If no rows selected, export all
				for (int i = 0; i < tableModel.getRowCount(); i++)
				{
					selectedItems.add(tableModel.getItemAt(i));
				}
			}
else
{
				// Export selected rows
				for (int selectedRow : selectedRows)
				{
					int modelRow = table.convertRowIndexToModel(selectedRow);
					selectedItems.add(tableModel.getItemAt(modelRow));
				}
			}
			
			exportCsv(selectedItems, labelPrefix);
		});
		toolBar.add(exportButton);
		
		return toolBar;
	}
	
	/**
	 * Delete the selected item.
	 */
	protected void delete(String dirtyFlag, String labelPrefix)
	{
		int selectedRow = table.getSelectedRow();
		if (selectedRow >= 0)
		{
			int modelRow = table.convertRowIndexToModel(selectedRow);
			T selectedItem = tableModel.getItemAt(modelRow);
			String name = selectedItem.getName();
			
			Map<String, T> map = getMap(Database.getInstance());
			map.remove(name);
			
			// Handle cascade delete
			cascadeDelete(name);
		}
	}
	
	/**
	 * Export the selected items to a CSV file.
	 */
	protected void exportCsv(List<T> selectedItems, String defaultFileName)
	{
		if (selectedItems.isEmpty())
		{
			return;
		}
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(getUiString("ui.export.csv"));
		fileChooser.setSelectedFile(new File(defaultFileName + ".csv"));
		
		int result = fileChooser.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION)
		{
			File file = fileChooser.getSelectedFile();
			
			try (PrintWriter writer = new PrintWriter(file))
			{
				// Write headers
				writer.println(convertToCSV(getCsvHeaders()));
				
				// Write data
				for (T item : selectedItems)
				{
					writer.println(convertToCSV(getCsvColumns(item)));
				}
				
				JOptionPane.showMessageDialog(this,
					getUiString("ui.export.csv.success"),
					getUiString("ui.export.csv"),
					JOptionPane.INFORMATION_MESSAGE);
			}
catch (Exception e)
{
				JOptionPane.showMessageDialog(this,
					getUiString("ui.export.csv.error") + ": " + e.getMessage(),
					getUiString("ui.error"),
					JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	/**
	 * Get the CSV headers.
	 */
	protected String[] getCsvHeaders()
	{
		return new String[]
		{
			getUiString(labelPrefix + ".name")
		};
	}
	
	/**
	 * Get the CSV columns for an item.
	 */
	protected String[] getCsvColumns(T t)
	{
		return new String[]
		{
			t.getName()
		};
	}
	
	/**
	 * Convert an array of strings to a CSV line.
	 */
	protected String convertToCSV(String[] data)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length; i++)
		{
			sb.append(escapeSpecialCharacters(data[i]));
			if (i < data.length - 1)
			{
				sb.append(",");
			}
		}
		return sb.toString();
	}
	
	/**
	 * Escape special characters for CSV.
	 */
	protected String escapeSpecialCharacters(String data)
	{
		if (data == null)
		{
			return "";
		}
		
		String escapedData = data.replaceAll("\\R", " ");
		if (data.contains(",") || data.contains("\"") || data.contains("'"))
		{
			data = data.replace("\"", "\"\"");
			escapedData = "\"" + data + "\"";
		}
		return escapedData;
	}
	
	/**
	 * Show a dialog for creating a new item.
	 */
	protected T newItemDialog(String labelPrefix, Icon addIcon)
	{
		String newName = JOptionPane.showInputDialog(this,
			getUiString("ui.new.item.prompt"),
			getUiString("ui.new.item"),
			JOptionPane.QUESTION_MESSAGE);
		
		if (newName != null && !newName.isEmpty())
		{
			Map<String, T> map = getMap(Database.getInstance());
			if (map.containsKey(newName))
			{
				JOptionPane.showMessageDialog(this,
					getUiString("ui.new.item.exists"),
					getUiString("ui.error"),
					JOptionPane.ERROR_MESSAGE);
				return null;
			}
else
{
				T newItem = createNewItem(newName);
				map.put(newName, newItem);
				return newItem;
			}
		}
		
		return null;
	}
	
	/**
	 * Show a dialog for editing an item.
	 */
	protected abstract Component editItemDialog(T selectedItem, TrackDirty parent);
	
	/**
	 * Create a duplicate of an item with a new name.
	 */
	protected abstract T createDuplicateItem(T current, String newName);
	
	/**
	 * Create a new item with the given name.
	 */
	protected abstract T createNewItem(String name);
	
	/**
	 * Get the map of data objects from the database.
	 */
	protected abstract Map<String, T> getMap(Database database);
	
	/**
	 * Get the table columns for this data object type.
	 */
	protected abstract TableColumn[] getTableColumns(String labelPrefix);
	
	/**
	 * Handle cascade rename when an item is renamed.
	 */
	protected void cascadeRename(String oldName, String newName)
	{
		// Default implementation does nothing
	}
	
	/**
	 * Handle cascade delete when an item is deleted.
	 */
	protected void cascadeDelete(String deletedName)
	{
		// Default implementation does nothing
	}
	
	/**
	 * Get the icon for a data object.
	 */
	protected Icon getIcon(T t)
	{
		return icon;
	}
	
	/**
	 * Refresh the data from the database.
	 */
	public void refresh(Database database)
	{
		Map<String, T> map = getMap(database);
		
		tableModel.clear();
		for (T item : map.values())
		{
			tableModel.addItem(item);
		}
		
		tableModel.fireTableDataChanged();
	}
	
	/**
	 * Get the table.
	 */
	public JTable getTable()
	{
		return table;
	}
	
	/**
	 * Get the table model.
	 */
	public V2DataObjectTableModel<T> getTableModel()
	{
		return tableModel;
	}
	
	/**
	 * Get the table builder.
	 */
	public TableBuilder<T> getTableBuilder()
	{
		return tableBuilder;
	}
	
	/**
	 * Set the initial sort for the table.
	 */
	protected void tableInitialSort(JTable table)
	{
		TableRowSorter<DefaultTableModel> sorter = 
			(TableRowSorter<DefaultTableModel>)table.getRowSorter();
		
		// Sort by name column (index 1)
		List<RowSorter.SortKey> sortKeys = new ArrayList<>();
		sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
		sorter.setSortKeys(sortKeys);
		sorter.sort();
	}
	
	/**
	 * Filter the table.
	 */
	public void filterTable()
	{
		filterTable(null);
	}
	
	/**
	 * Filter the table with a predicate.
	 */
	public void filterTable(Predicate<T> predicate)
	{
		tableModel.setFilter(predicate);
		tableModel.fireTableDataChanged();
	}
	
	/**
	 * Mark objects as dirty.
	 */
	@Override
	public void setDirty(Object... objs)
	{
		for (Object obj : objs)
		{
			if (obj != null)
			{
				dirtyObjects.add(obj);
				
				if (obj instanceof String && obj.equals(dirtyFlag))
				{
					// Propagate the dirty flag to the parent
					parent.setDirty(dirtyFlag);
				}
else
{
					// Propagate the object to the parent
					parent.setDirty(obj);
				}
			}
		}
	}
	
	/**
	 * Clear the dirty state.
	 */
	@Override
	public void clearDirty()
	{
		dirtyObjects.clear();
	}
	
	/**
	 * Table model for V2DataObjects.
	 */
	public static class V2DataObjectTableModel<T extends V2DataObject> extends DefaultTableModel
	{
		private final List<T> items = new ArrayList<>();
		private final List<T> filteredItems = new ArrayList<>();
		private Predicate<T> filter;
		
		public V2DataObjectTableModel()
		{
			super(0, 0);
		}
		
		public void addItem(T item)
		{
			items.add(item);
			if (filter == null || filter.test(item))
			{
				filteredItems.add(item);
			}
		}
		
		public void clear()
		{
			items.clear();
			filteredItems.clear();
		}
		
		public void setFilter(Predicate<T> filter)
		{
			this.filter = filter;
			filteredItems.clear();
			
			if (filter == null)
			{
				filteredItems.addAll(items);
			}
else
{
				for (T item : items)
				{
					if (filter.test(item))
					{
						filteredItems.add(item);
					}
				}
			}
		}
		
		@Override
		public int getRowCount()
		{
			return filteredItems.size();
		}
		
		@Override
		public Object getValueAt(int row, int column)
		{
			if (row >= 0 && row < filteredItems.size())
			{
				return filteredItems.get(row);
			}
			return null;
		}
		
		public T getItemAt(int row)
		{
			if (row >= 0 && row < filteredItems.size())
			{
				return filteredItems.get(row);
			}
			return null;
		}
		
		@Override
		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
	}
}
