package mclachlan.brewday.ui.swing.dialogs;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.WaterParameters;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class EditWaterParametersDialog extends JDialog
{
	private final boolean createMode;
	private final JTextField nameField;
	private final JTextField minCalciumField;
	private final JTextField maxCalciumField;
	private final JTextField minBicarbonateField;
	private final JTextField maxBicarbonateField;
	private final JTextField minSulfateField;
	private final JTextField maxSulfateField;
	private final JTextField minChlorideField;
	private final JTextField maxChlorideField;
	private final JTextField minSodiumField;
	private final JTextField maxSodiumField;
	private final JTextField minMagnesiumField;
	private final JTextField maxMagnesiumField;
	private final JTextField minAlkalinityField;
	private final JTextField maxAlkalinityField;
	private final JTextField minResidualAlkalinityField;
	private final JTextField maxResidualAlkalinityField;
	private final JTextArea descriptionArea;
	private WaterParameters result;

	public EditWaterParametersDialog(JFrame parent, WaterParameters waterParameters, boolean createMode)
	{
		super(parent, getUiString(createMode ? "common.add" : "common.edit"), true);
		this.createMode = createMode;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;

		nameField = field(waterParameters.getName());
		nameField.setEditable(createMode);
		minCalciumField = field(ppm(waterParameters.getMinCalcium()));
		maxCalciumField = field(ppm(waterParameters.getMaxCalcium()));
		minBicarbonateField = field(ppm(waterParameters.getMinBicarbonate()));
		maxBicarbonateField = field(ppm(waterParameters.getMaxBicarbonate()));
		minSulfateField = field(ppm(waterParameters.getMinSulfate()));
		maxSulfateField = field(ppm(waterParameters.getMaxSulfate()));
		minChlorideField = field(ppm(waterParameters.getMinChloride()));
		maxChlorideField = field(ppm(waterParameters.getMaxChloride()));
		minSodiumField = field(ppm(waterParameters.getMinSodium()));
		maxSodiumField = field(ppm(waterParameters.getMaxSodium()));
		minMagnesiumField = field(ppm(waterParameters.getMinMagnesium()));
		maxMagnesiumField = field(ppm(waterParameters.getMaxMagnesium()));
		minAlkalinityField = field(ppm(waterParameters.getMinAlkalinity()));
		maxAlkalinityField = field(ppm(waterParameters.getMaxAlkalinity()));
		minResidualAlkalinityField = field(ppm(waterParameters.getMinResidualAlkalinity()));
		maxResidualAlkalinityField = field(ppm(waterParameters.getMaxResidualAlkalinity()));
		descriptionArea = new JTextArea(waterParameters.getDescription() == null ? "" : waterParameters.getDescription(), 6, 30);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		wireTooltips();

		addRow(panel, gbc, 0, getUiString("water.parameters.name"), nameField);
		addRangeHeader(panel, gbc, 1);
		addRangeRow(panel, gbc, 2, "Ca", minCalciumField, maxCalciumField);
		addRangeRow(panel, gbc, 3, "HCO3", minBicarbonateField, maxBicarbonateField);
		addRangeRow(panel, gbc, 4, "SO4", minSulfateField, maxSulfateField);
		addRangeRow(panel, gbc, 5, "Cl", minChlorideField, maxChlorideField);
		addRangeRow(panel, gbc, 6, "Na", minSodiumField, maxSodiumField);
		addRangeRow(panel, gbc, 7, "Mg", minMagnesiumField, maxMagnesiumField);
		addRangeRow(panel, gbc, 8, getUiString("water.parameters.min.alkalinity"), minAlkalinityField, maxAlkalinityField);
		addRangeRow(panel, gbc, 9, getUiString("water.parameters.min.residual.alkalinity"), minResidualAlkalinityField, maxResidualAlkalinityField);

		gbc.gridx = 0;
		gbc.gridy = 10;
		gbc.weightx = 0;
		panel.add(new JLabel(getUiString("water.desc") + ":"), gbc);
		gbc.gridx = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		panel.add(new JScrollPane(descriptionArea), gbc);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton ok = new JButton(getUiString("ui.ok"));
		ok.addActionListener(e -> onOk());
		JButton cancel = new JButton(getUiString("ui.cancel"));
		cancel.addActionListener(e -> dispose());
		buttons.add(ok);
		buttons.add(cancel);

		gbc.gridx = 0;
		gbc.gridy = 11;
		gbc.gridwidth = 3;
		panel.add(buttons, gbc);

		setContentPane(panel);
		getRootPane().setDefaultButton(ok);
		ActionHotkeySupport.bind(this.getRootPane(),
			javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			"dialog.cancel",
			new javax.swing.AbstractAction()
			{
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					dispose();
				}
			});
		ActionHotkeySupport.bindFocused(descriptionArea,
			javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
			"dialog.commit.from.description",
			new javax.swing.AbstractAction()
			{
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					onOk();
				}
			});
		pack();
		setLocationRelativeTo(parent);
	}

	private JTextField field(String value)
	{
		return new JTextField(value == null ? "" : value);
	}

	private String ppm(PpmUnit value)
	{
		return value == null ? "" : String.valueOf(value.get());
	}

	private void addRow(JPanel panel, GridBagConstraints gbc, int y, String label, JTextField field)
	{
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.weightx = 0;
		gbc.gridwidth = 1;
		panel.add(new JLabel(label + ":"), gbc);
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(field, gbc);
	}

	private void addRangeHeader(JPanel panel, GridBagConstraints gbc, int y)
	{
		gbc.gridx = 1;
		gbc.gridy = y;
		gbc.weightx = 1.0;
		panel.add(new JLabel("Min"), gbc);
		gbc.gridx = 2;
		panel.add(new JLabel("Max"), gbc);
	}

	private void addRangeRow(JPanel panel, GridBagConstraints gbc, int y, String label, JTextField minField, JTextField maxField)
	{
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.weightx = 0;
		panel.add(new JLabel(label + ":"), gbc);
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(minField, gbc);
		gbc.gridx = 2;
		panel.add(maxField, gbc);
	}

	private void wireTooltips()
	{
		nameField.setToolTipText(getUiString("water.parameters.tooltip.name"));
		minCalciumField.setToolTipText(getUiString("water.parameters.tooltip.min.calcium"));
		maxCalciumField.setToolTipText(getUiString("water.parameters.tooltip.max.calcium"));
		minBicarbonateField.setToolTipText(getUiString("water.parameters.tooltip.min.bicarbonate"));
		maxBicarbonateField.setToolTipText(getUiString("water.parameters.tooltip.max.bicarbonate"));
		minSulfateField.setToolTipText(getUiString("water.parameters.tooltip.min.sulfate"));
		maxSulfateField.setToolTipText(getUiString("water.parameters.tooltip.max.sulfate"));
		minChlorideField.setToolTipText(getUiString("water.parameters.tooltip.min.chloride"));
		maxChlorideField.setToolTipText(getUiString("water.parameters.tooltip.max.chloride"));
		minSodiumField.setToolTipText(getUiString("water.parameters.tooltip.min.sodium"));
		maxSodiumField.setToolTipText(getUiString("water.parameters.tooltip.max.sodium"));
		minMagnesiumField.setToolTipText(getUiString("water.parameters.tooltip.min.magnesium"));
		maxMagnesiumField.setToolTipText(getUiString("water.parameters.tooltip.max.magnesium"));
		minAlkalinityField.setToolTipText(getUiString("water.parameters.tooltip.min.alkalinity"));
		maxAlkalinityField.setToolTipText(getUiString("water.parameters.tooltip.max.alkalinity"));
		minResidualAlkalinityField.setToolTipText(getUiString("water.parameters.tooltip.min.residual.alkalinity"));
		maxResidualAlkalinityField.setToolTipText(getUiString("water.parameters.tooltip.max.residual.alkalinity"));
		descriptionArea.setToolTipText(getUiString("water.parameters.tooltip.desc"));
	}

	private void onOk()
	{
		String name = nameField.getText().trim();
		if (name.isEmpty())
		{
			showValidationError(getUiString("ui.name"));
			focusForValidation(nameField);
			return;
		}

		WaterParameters waterParameters = new WaterParameters(name);
		PpmUnit minCalcium = parsePpmOrShowError(minCalciumField);
		if (minCalcium == null && !minCalciumField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit maxCalcium = parsePpmOrShowError(maxCalciumField);
		if (maxCalcium == null && !maxCalciumField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit minBicarbonate = parsePpmOrShowError(minBicarbonateField);
		if (minBicarbonate == null && !minBicarbonateField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit maxBicarbonate = parsePpmOrShowError(maxBicarbonateField);
		if (maxBicarbonate == null && !maxBicarbonateField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit minSulfate = parsePpmOrShowError(minSulfateField);
		if (minSulfate == null && !minSulfateField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit maxSulfate = parsePpmOrShowError(maxSulfateField);
		if (maxSulfate == null && !maxSulfateField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit minChloride = parsePpmOrShowError(minChlorideField);
		if (minChloride == null && !minChlorideField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit maxChloride = parsePpmOrShowError(maxChlorideField);
		if (maxChloride == null && !maxChlorideField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit minSodium = parsePpmOrShowError(minSodiumField);
		if (minSodium == null && !minSodiumField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit maxSodium = parsePpmOrShowError(maxSodiumField);
		if (maxSodium == null && !maxSodiumField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit minMagnesium = parsePpmOrShowError(minMagnesiumField);
		if (minMagnesium == null && !minMagnesiumField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit maxMagnesium = parsePpmOrShowError(maxMagnesiumField);
		if (maxMagnesium == null && !maxMagnesiumField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit minAlkalinity = parsePpmOrShowError(minAlkalinityField);
		if (minAlkalinity == null && !minAlkalinityField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit maxAlkalinity = parsePpmOrShowError(maxAlkalinityField);
		if (maxAlkalinity == null && !maxAlkalinityField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit minResidualAlkalinity = parsePpmOrShowError(minResidualAlkalinityField);
		if (minResidualAlkalinity == null && !minResidualAlkalinityField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit maxResidualAlkalinity = parsePpmOrShowError(maxResidualAlkalinityField);
		if (maxResidualAlkalinity == null && !maxResidualAlkalinityField.getText().trim().isEmpty())
		{
			return;
		}

		waterParameters.setMinCalcium(minCalcium);
		waterParameters.setMaxCalcium(maxCalcium);
		waterParameters.setMinBicarbonate(minBicarbonate);
		waterParameters.setMaxBicarbonate(maxBicarbonate);
		waterParameters.setMinSulfate(minSulfate);
		waterParameters.setMaxSulfate(maxSulfate);
		waterParameters.setMinChloride(minChloride);
		waterParameters.setMaxChloride(maxChloride);
		waterParameters.setMinSodium(minSodium);
		waterParameters.setMaxSodium(maxSodium);
		waterParameters.setMinMagnesium(minMagnesium);
		waterParameters.setMaxMagnesium(maxMagnesium);
		waterParameters.setMinAlkalinity(minAlkalinity);
		waterParameters.setMaxAlkalinity(maxAlkalinity);
		waterParameters.setMinResidualAlkalinity(minResidualAlkalinity);
		waterParameters.setMaxResidualAlkalinity(maxResidualAlkalinity);
		waterParameters.setDescription(descriptionArea.getText());
		result = waterParameters;
		dispose();
	}

	private PpmUnit parsePpmOrShowError(JTextField field)
	{
		try
		{
			return parsePpm(field);
		}
		catch (NumberFormatException e)
		{
			showValidationError(e.getMessage());
			focusForValidation(field);
			return null;
		}
	}

	private PpmUnit parsePpm(JTextField field)
	{
		String text = field.getText().trim();
		if (text.isEmpty())
		{
			return null;
		}
		return (PpmUnit)Quantity.parseQuantity(text, Quantity.Unit.PPM);
	}

	protected void focusForValidation(JTextField field)
	{
		field.requestFocusInWindow();
		field.selectAll();
	}

	protected void showValidationError(String message)
	{
		JOptionPane.showMessageDialog(this, message, getUiString("ui.error"), JOptionPane.ERROR_MESSAGE);
	}

	public WaterParameters getResult()
	{
		return result;
	}

	public boolean isCreateMode()
	{
		return createMode;
	}
}
