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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.IngredientAddition;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Dialog for adding fermentable items to the inventory.
 */
public class FermentableInventoryDialog extends JDialog
{
	private JComboBox<String> fermentableComboBox;
	private JSpinner quantitySpinner;
	private JComboBox<Quantity.Unit> unitComboBox;
	private InventoryLineItem result = null;
	
	/**
	 * Constructor.
	 */
	public FermentableInventoryDialog(JFrame parent)
	{
		super(parent, getUiString("inventory.add.fermentable"), true);
		
		// Create the panel
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		// Fermentable selection
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(new JLabel(getUiString("fermentable.name") + ":"), gbc);
		
		gbc.gridx = 1;
		gbc.gridwidth = 2;
		
		// Get fermentables from database
		Map<String, Fermentable> fermentables = Database.getInstance().getFermentables();
		String[] fermentableNames = fermentables.keySet().toArray(new String[0]);
		fermentableComboBox = new JComboBox<>(fermentableNames);
		fermentableComboBox.setSelectedIndex(fermentableNames.length > 0 ? 0 : -1);
		panel.add(fermentableComboBox, gbc);
		
		// Quantity
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		panel.add(new JLabel(getUiString("inventory.quantity") + ":"), gbc);
		
		gbc.gridx = 1;
		quantitySpinner = new JSpinner(
			new SpinnerNumberModel(1.0, 0.0, 10000.0, 0.1));
		panel.add(quantitySpinner, gbc);
		
		// Unit
		gbc.gridx = 2;
		Quantity.Unit[] weightUnits =
		{
			Quantity.Unit.KILOGRAMS,
			Quantity.Unit.GRAMS,
			Quantity.Unit.POUNDS,
			Quantity.Unit.OUNCES
		};
		unitComboBox = new JComboBox<>(weightUnits);
		unitComboBox.setSelectedItem(Quantity.Unit.KILOGRAMS);
		panel.add(unitComboBox, gbc);
		
		// Buttons
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.CENTER;
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		
		JButton addButton = new JButton(getUiString("common.add"));
		addButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String fermentableName = (String)fermentableComboBox.getSelectedItem();
				if (fermentableName != null && !fermentableName.isEmpty())
				{
					double quantity = ((Number)quantitySpinner.getValue()).doubleValue();
					Quantity.Unit unit = (Quantity.Unit)unitComboBox.getSelectedItem();
					
					result = new InventoryLineItem(
						fermentableName,
						IngredientAddition.Type.FERMENTABLES,
						new WeightUnit(quantity, unit),
						unit);
					
					dispose();
				}
else
{
					JOptionPane.showMessageDialog(
						FermentableInventoryDialog.this,
						"Please select a fermentable",
						"Error",
						JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		
		JButton cancelButton = new JButton(getUiString("ui.cancel"));
		cancelButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});
		
		buttonPanel.add(addButton);
		buttonPanel.add(cancelButton);
		panel.add(buttonPanel, gbc);
		
		// Set up the dialog
		setContentPane(panel);
		pack();
		setLocationRelativeTo(parent);
		setResizable(false);
	}
	
	/**
	 * Get the result of the dialog.
	 */
	public InventoryLineItem getResult()
	{
		return result;
	}
	
	/**
	 * Show the dialog and return the result.
	 */
	public static InventoryLineItem showDialog(JFrame parent)
	{
		FermentableInventoryDialog dialog = new FermentableInventoryDialog(parent);
		dialog.setVisible(true);
		return dialog.getResult();
	}
}
