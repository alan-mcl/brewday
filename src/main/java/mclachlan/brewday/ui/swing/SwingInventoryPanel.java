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

import java.awt.Component;
import java.awt.Dialog;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.TableColumn;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Panel for displaying and managing inventory items in the Swing UI.
 * This is the Swing equivalent of the JavaFX InventoryPane.
 */
public class SwingInventoryPanel extends V2DataObjectPane<InventoryLineItem>
{
	/**
	 * Constructor.
	 */
	public SwingInventoryPanel(TrackDirty parent)
	{
		super(SwingUi.INVENTORY, parent, "inventory", SwingIcons.inventoryIcon, SwingIcons.inventoryIcon);
	}

	/**
	 * Build the toolbar with CRUD operations.
	 */
	@Override
	protected JToolBar buildToolBar(String dirtyFlag, TrackDirty parent, String labelPrefix, Icon addIcon)
	{
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		
		// Add buttons for different ingredient types
		JButton addWaterButton = new JButton(getUiString(labelPrefix + ".add.water"), SwingIcons.waterIcon);
		addWaterButton.setToolTipText(getUiString(labelPrefix + ".add.water"));
		addWaterButton.addActionListener(e -> {
			// This would open a water selection dialog
			JOptionPane.showMessageDialog(this, 
				"Water addition dialog would open here", 
				getUiString(labelPrefix + ".add.water"), 
				JOptionPane.INFORMATION_MESSAGE);
		});
		toolBar.add(addWaterButton);
		
		JButton addFermentableButton = new JButton(getUiString(labelPrefix + ".add.fermentable"), SwingIcons.fermentableIconGeneric);
		addFermentableButton.setToolTipText(getUiString(labelPrefix + ".add.fermentable"));
		addFermentableButton.addActionListener(e -> {
			// This would open a fermentable selection dialog
			JOptionPane.showMessageDialog(this, 
				"Fermentable addition dialog would open here", 
				getUiString(labelPrefix + ".add.fermentable"), 
				JOptionPane.INFORMATION_MESSAGE);
		});
		toolBar.add(addFermentableButton);
		
		JButton addHopButton = new JButton(getUiString(labelPrefix + ".add.hop"), SwingIcons.hopsIcon);
		addHopButton.setToolTipText(getUiString(labelPrefix + ".add.hop"));
		addHopButton.addActionListener(e -> {
			// This would open a hop selection dialog
			JOptionPane.showMessageDialog(this, 
				"Hop addition dialog would open here", 
				getUiString(labelPrefix + ".add.hop"), 
				JOptionPane.INFORMATION_MESSAGE);
		});
		toolBar.add(addHopButton);
		
		JButton addYeastButton = new JButton(getUiString(labelPrefix + ".add.yeast"), SwingIcons.yeastIcon);
		addYeastButton.setToolTipText(getUiString(labelPrefix + ".add.yeast"));
		addYeastButton.addActionListener(e -> {
			// This would open a yeast selection dialog
			JOptionPane.showMessageDialog(this, 
				"Yeast addition dialog would open here", 
				getUiString(labelPrefix + ".add.yeast"), 
				JOptionPane.INFORMATION_MESSAGE);
		});
		toolBar.add(addYeastButton);
		
		JButton addMiscButton = new JButton(getUiString(labelPrefix + ".add.misc"), SwingIcons.miscIconGeneric);
		addMiscButton.setToolTipText(getUiString(labelPrefix + ".add.misc"));
		addMiscButton.addActionListener(e -> {
			// This would open a misc selection dialog
			JOptionPane.showMessageDialog(this, 
				"Misc addition dialog would open here", 
				getUiString(labelPrefix + ".add.misc"), 
				JOptionPane.INFORMATION_MESSAGE);
		});
		toolBar.add(addMiscButton);
		
		// Add separator
		toolBar.add(new JToolBar.Separator());
		
		// Edit button
		JButton editButton = new JButton(getUiString("ui.edit"));
		editButton.setToolTipText(getUiString("ui.edit"));
		editButton.addActionListener(e -> {
			int selectedRow = getTable().getSelectedRow();
			if (selectedRow >= 0)
			{
				int modelRow = getTable().convertRowIndexToModel(selectedRow);
				InventoryLineItem selectedItem = getTableModel().getItemAt(modelRow);
				
				Component editor = editItemDialog(selectedItem, parent);
				if (editor != null)
				{
					// Show the editor in a dialog
					JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), 
						getUiString("ui.edit") + " " + selectedItem.getName(), 
						Dialog.ModalityType.APPLICATION_MODAL);
					dialog.setContentPane((JPanel)editor);
					dialog.pack();
					dialog.setLocationRelativeTo(this);
					dialog.setVisible(true);
					
					refresh(Database.getInstance());
				}
			}
else
{
				JOptionPane.showMessageDialog(this,
					getUiString("ui.select.item.first"),
					getUiString("ui.error"),
					JOptionPane.ERROR_MESSAGE);
			}
		});
		toolBar.add(editButton);
		
		// Delete button
		JButton deleteButton = new JButton(getUiString("ui.delete"), SwingIcons.deleteIcon);
		deleteButton.setToolTipText(getUiString("ui.delete"));
		deleteButton.addActionListener(e -> {
			int selectedRow = getTable().getSelectedRow();
			if (selectedRow >= 0)
			{
				int modelRow = getTable().convertRowIndexToModel(selectedRow);
				InventoryLineItem selectedItem = getTableModel().getItemAt(modelRow);
				
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
else
{
				JOptionPane.showMessageDialog(this,
					getUiString("ui.select.item.first"),
					getUiString("ui.error"),
					JOptionPane.ERROR_MESSAGE);
			}
		});
		toolBar.add(deleteButton);
		
		// Add separator
		toolBar.add(new JToolBar.Separator());
		
		// Export CSV button
		JButton exportButton = new JButton(getUiString("ui.export.csv"));
		exportButton.setToolTipText(getUiString("ui.export.csv"));
		exportButton.addActionListener(e -> {
			java.util.List<InventoryLineItem> selectedItems = new java.util.ArrayList<>();
			int[] selectedRows = getTable().getSelectedRows();
			
			if (selectedRows.length == 0)
			{
				// If no rows selected, export all
				for (int i = 0; i < getTableModel().getRowCount(); i++)
				{
					selectedItems.add(getTableModel().getItemAt(i));
				}
			}
else
{
				// Export selected rows
				for (int selectedRow : selectedRows)
				{
					int modelRow = getTable().convertRowIndexToModel(selectedRow);
					selectedItems.add(getTableModel().getItemAt(modelRow));
				}
			}
			
			exportCsv(selectedItems, "inventory");
		});
		toolBar.add(exportButton);
		
		return toolBar;
	}

	/**
	 * Create a dialog for editing an inventory item.
	 */
	@Override
	protected Component editItemDialog(InventoryLineItem selectedItem, TrackDirty parent)
	{
		// Create a simple panel for editing the quantity
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		// Add item name
		JLabel nameLabel = new JLabel(selectedItem.getIngredient());
		nameLabel.setFont(nameLabel.getFont().deriveFont(java.awt.Font.BOLD));
		nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(nameLabel);
		
		// Add some spacing
		panel.add(Box.createRigidArea(new java.awt.Dimension(0, 10)));
		
		// Add quantity field
		JPanel quantityPanel = new JPanel();
		quantityPanel.add(new JLabel(getUiString("inventory.quantity") + ":"));
		
		JTextField quantityField = new JTextField(10);
		quantityField.setText(String.valueOf(selectedItem.getQuantity().get(selectedItem.getUnit())));
		quantityPanel.add(quantityField);
		
		// Add unit label
		JLabel unitLabel = new JLabel(selectedItem.getUnit().toString());
		quantityPanel.add(unitLabel);
		
		panel.add(quantityPanel);
		
		// Add some spacing
		panel.add(Box.createRigidArea(new java.awt.Dimension(0, 10)));
		
		// Add buttons
		JPanel buttonPanel = new JPanel();
		
		JButton saveButton = new JButton(getUiString("ui.save"));
		saveButton.addActionListener(e -> {
			try
			{
				double value = Double.parseDouble(quantityField.getText());
				
				// Create a new Quantity object with the updated value
				Quantity newQuantity = Quantity.parseQuantity(
					String.valueOf(value), 
					selectedItem.getUnit());
				
				// Set the new quantity on the item
				selectedItem.setQuantity(newQuantity);
				parent.setDirty(SwingUi.INVENTORY);
				
				// Close the dialog
				JDialog dialog = (JDialog)SwingUtilities.getWindowAncestor(panel);
				dialog.dispose();
			}
catch (NumberFormatException ex)
{
				JOptionPane.showMessageDialog(panel,
					getUiString("ui.invalid.number"),
					getUiString("ui.error"),
					JOptionPane.ERROR_MESSAGE);
			}
		});
		buttonPanel.add(saveButton);
		
		JButton cancelButton = new JButton(getUiString("ui.cancel"));
		cancelButton.addActionListener(e -> {
			// Close the dialog without saving
			JDialog dialog = (JDialog)SwingUtilities.getWindowAncestor(panel);
			dialog.dispose();
		});
		buttonPanel.add(cancelButton);
		
		panel.add(buttonPanel);
		
		return panel;
	}

	/**
	 * Create a duplicate of an inventory item.
	 * Not supported for inventory items.
	 */
	@Override
	protected InventoryLineItem createDuplicateItem(InventoryLineItem current, String newName)
	{
		throw new BrewdayException("Duplicating inventory items is not supported");
	}

	/**
	 * Create a new inventory item.
	 * Not used directly - items are created through the add buttons.
	 */
	@Override
	protected InventoryLineItem createNewItem(String name)
	{
		// Not used - items are created through the add buttons
		return null;
	}

	/**
	 * Get the map of inventory items from the database.
	 */
	@Override
	protected Map<String, InventoryLineItem> getMap(Database database)
	{
		return database.getInventory();
	}

	/**
	 * Get the table columns for the inventory table.
	 */
	@Override
	protected TableColumn[] getTableColumns(String labelPrefix)
	{
		TableColumn typeCol = getTableBuilder().getStringPropertyValueCol(
			labelPrefix + ".item.type", 
			item -> item.getType().toString());
		
		TableColumn quantityCol = getTableBuilder().getStringPropertyValueCol(
			labelPrefix + ".quantity", 
			item -> String.format("%.2f %s", 
				item.getQuantity().get(item.getUnit()), 
				item.getUnit().toString()));
		
		return new TableColumn[] { typeCol, quantityCol };
	}

	/**
	 * Get the icon for an inventory item based on its type.
	 */
	@Override
	protected Icon getIcon(InventoryLineItem item)
	{
		switch (item.getType())
		{
			case FERMENTABLES:
				return SwingIcons.fermentableIconGeneric;
			case HOPS:
				return SwingIcons.hopsIcon;
			case WATER:
				return SwingIcons.waterIcon;
			case YEAST:
				return SwingIcons.yeastIcon;
			case MISC:
				return SwingIcons.miscIconGeneric;
			default:
				return SwingIcons.inventoryIcon;
		}
	}
}
