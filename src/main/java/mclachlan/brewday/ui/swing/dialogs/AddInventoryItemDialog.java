package mclachlan.brewday.ui.swing.dialogs;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.IngredientAddition;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class AddInventoryItemDialog extends JDialog
{
	private InventoryLineItem result;
	private final JComboBox<String> nameCombo;
	private final JSpinner quantitySpinner;
	private final JComboBox<Quantity.Unit> unitCombo;
	private final IngredientAddition.Type type;

	public AddInventoryItemDialog(
		JFrame parent,
		IngredientAddition.Type type,
		String titleKey,
		String nameKey,
		Quantity.Unit[] units)
	{
		super(parent, getUiString(titleKey), true);
		this.type = type;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(new JLabel(getUiString(nameKey) + ":"), gbc);

		gbc.gridx = 1;
		gbc.gridwidth = 2;
		Map<String, ?> src = getMap(type);
		nameCombo = new JComboBox<>(src.keySet().toArray(new String[0]));
		nameCombo.setSelectedIndex(nameCombo.getItemCount() > 0 ? 0 : -1);
		panel.add(nameCombo, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		panel.add(new JLabel(getUiString("inventory.quantity") + ":"), gbc);

		gbc.gridx = 1;
		quantitySpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 10000.0, 0.1));
		panel.add(quantitySpinner, gbc);

		gbc.gridx = 2;
		unitCombo = new JComboBox<>(units);
		panel.add(unitCombo, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton addButton = new JButton(getUiString("common.add"));
		addButton.addActionListener(e -> onAdd());
		buttons.add(addButton);
		JButton cancelButton = new JButton(getUiString("ui.cancel"));
		cancelButton.addActionListener(e -> dispose());
		buttons.add(cancelButton);
		panel.add(buttons, gbc);

		setContentPane(panel);
		pack();
		setLocationRelativeTo(parent);
		setResizable(false);
	}

	private void onAdd()
	{
		String name = (String)nameCombo.getSelectedItem();
		if (name == null || name.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "Please select an item", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		double quantity = ((Number)quantitySpinner.getValue()).doubleValue();
		Quantity.Unit unit = (Quantity.Unit)unitCombo.getSelectedItem();
		if (type == IngredientAddition.Type.WATER)
		{
			result = new InventoryLineItem(name, type, new VolumeUnit(quantity, unit), unit);
		}
		else
		{
			result = new InventoryLineItem(name, type, new WeightUnit(quantity, unit), unit);
		}
		dispose();
	}

	private Map<String, ?> getMap(IngredientAddition.Type type)
	{
		Database db = Database.getInstance();
		return switch (type)
		{
			case FERMENTABLES -> db.getFermentables();
			case HOPS -> db.getHops();
			case YEAST -> db.getYeasts();
			case MISC -> db.getMiscs();
			case WATER -> db.getWaters();
		};
	}

	public InventoryLineItem getResult()
	{
		return result;
	}
}
