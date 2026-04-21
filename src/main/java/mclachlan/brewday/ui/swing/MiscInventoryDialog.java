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
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.IngredientAddition;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Dialog for adding miscellaneous items to the inventory.
 */
public class MiscInventoryDialog extends JDialog
{
	private JComboBox<String> miscComboBox;
	private JSpinner quantitySpinner;
	private JComboBox<Quantity.Unit> unitComboBox;
	private InventoryLineItem result = null;
	
	/**
	 * Constructor.
	 */
	public MiscInventoryDialog(JFrame parent)
	{
		super(parent, getUiString("inventory.add.misc"), true);
		
		// Create the panel
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		// Misc selection
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(new JLabel(getUiString("misc.name") + ":"), gbc);
		
		gbc.gridx = 1;
		gbc.gridwidth = 2;
		
		// Get miscs from database
		Map<String, Misc> miscs = Database.getInstance().getMiscs();
		String[] miscNames = miscs.keySet().toArray(new String[0]);
		miscComboBox = new JComboBox<>(miscNames);
		miscComboBox.setSelectedIndex(miscNames.length > 0 ? 0 : -1);
		panel.add(miscComboBox, gbc);
		
		// Quantity
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		panel.add(new JLabel(getUiString("inventory.quantity") + ":"), gbc);
		
		gbc.gridx = 1;
		quantitySpinner = new JSpinner(
			new SpinnerNumberModel(10.0, 0.0, 1000.0, 1.0));
		panel.add(quantitySpinner, gbc);
		
		// Unit
		gbc.gridx = 2;
		Quantity.Unit[] weightUnits =
		{
			Quantity.Unit.GRAMS,
			Quantity.Unit.KILOGRAMS,
			Quantity.Unit.OUNCES,
			Quantity.Unit.POUNDS
		};
		unitComboBox = new JComboBox<>(weightUnits);
		unitComboBox.setSelectedItem(Quantity.Unit.GRAMS);
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
				String miscName = (String)miscComboBox.getSelectedItem();
				if (miscName != null && !miscName.isEmpty())
				{
					double quantity = ((Number)quantitySpinner.getValue()).doubleValue();
					Quantity.Unit unit = (Quantity.Unit)unitComboBox.getSelectedItem();
					
					result = new InventoryLineItem(
						miscName,
						IngredientAddition.Type.MISC,
						new WeightUnit(quantity, unit),
						unit);
					
					dispose();
				}
else
{
					JOptionPane.showMessageDialog(
						MiscInventoryDialog.this,
						"Please select a miscellaneous ingredient",
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
		MiscInventoryDialog dialog = new MiscInventoryDialog(parent);
		dialog.setVisible(true);
		return dialog.getResult();
	}
}
