package mclachlan.brewday.ui.swing.dialogs;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.widgets.IngredientComboBoxRenderer;

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
		List<String> names = new ArrayList<>(src.keySet());
		Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
		nameCombo = new JComboBox<>(names.toArray(new String[0]));
		nameCombo.setRenderer(new IngredientComboBoxRenderer(type));
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
		mclachlan.brewday.ui.swing.app.DialogButtonTooltips.wireAdd(addButton);
		mclachlan.brewday.ui.swing.app.DialogButtonTooltips.wireOkCancel(addButton, cancelButton);
		cancelButton.addActionListener(e -> dispose());
		buttons.add(cancelButton);
		panel.add(buttons, gbc);

		setContentPane(panel);
		getRootPane().setDefaultButton(addButton);
		ActionHotkeySupport.bind(getRootPane(),
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			"addInventoryItem.cancel",
			new AbstractAction()
			{
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					dispose();
				}
			});
		pack();
		setLocationRelativeTo(parent);
		setResizable(false);
	}

	private void onAdd()
	{
		String name = (String)nameCombo.getSelectedItem();
		if (name == null || name.isEmpty())
		{
			SwingUiErrors.showError(this, "Please select an item", "Error");
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
			case YEAST, YEAST_CULTURE -> db.getYeasts();
			case MISC -> db.getMiscs();
			case WATER -> db.getWaters();
		};
	}

	public InventoryLineItem getResult()
	{
		return result;
	}
}
