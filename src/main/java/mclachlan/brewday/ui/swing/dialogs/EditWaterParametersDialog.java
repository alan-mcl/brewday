package mclachlan.brewday.ui.swing.dialogs;

import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.WaterParameters;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class EditWaterParametersDialog extends JDialog
{
	private final boolean createMode;
	private final JTextField nameField;
	private final SwingQuantityEditWidget<PpmUnit> minCalciumField;
	private final SwingQuantityEditWidget<PpmUnit> maxCalciumField;
	private final SwingQuantityEditWidget<PpmUnit> minBicarbonateField;
	private final SwingQuantityEditWidget<PpmUnit> maxBicarbonateField;
	private final SwingQuantityEditWidget<PpmUnit> minSulfateField;
	private final SwingQuantityEditWidget<PpmUnit> maxSulfateField;
	private final SwingQuantityEditWidget<PpmUnit> minChlorideField;
	private final SwingQuantityEditWidget<PpmUnit> maxChlorideField;
	private final SwingQuantityEditWidget<PpmUnit> minSodiumField;
	private final SwingQuantityEditWidget<PpmUnit> maxSodiumField;
	private final SwingQuantityEditWidget<PpmUnit> minMagnesiumField;
	private final SwingQuantityEditWidget<PpmUnit> maxMagnesiumField;
	private final SwingQuantityEditWidget<PpmUnit> minAlkalinityField;
	private final SwingQuantityEditWidget<PpmUnit> maxAlkalinityField;
	private final SwingQuantityEditWidget<PpmUnit> minResidualAlkalinityField;
	private final SwingQuantityEditWidget<PpmUnit> maxResidualAlkalinityField;
	private final JTextArea descriptionArea;
	private WaterParameters result;

	public EditWaterParametersDialog(JFrame parent, WaterParameters waterParameters, boolean createMode)
	{
		super(parent, getUiString(createMode ? "common.add" : "common.edit"), true);
		this.createMode = createMode;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints mainGbc = new GridBagConstraints();
		mainGbc.insets = new Insets(4, 4, 4, 4);
		mainGbc.anchor = GridBagConstraints.NORTHWEST;

		nameField = field(waterParameters.getName());
		nameField.setEditable(createMode);
		minCalciumField = ppmWidget(waterParameters.getMinCalcium());
		maxCalciumField = ppmWidget(waterParameters.getMaxCalcium());
		minBicarbonateField = ppmWidget(waterParameters.getMinBicarbonate());
		maxBicarbonateField = ppmWidget(waterParameters.getMaxBicarbonate());
		minSulfateField = ppmWidget(waterParameters.getMinSulfate());
		maxSulfateField = ppmWidget(waterParameters.getMaxSulfate());
		minChlorideField = ppmWidget(waterParameters.getMinChloride());
		maxChlorideField = ppmWidget(waterParameters.getMaxChloride());
		minSodiumField = ppmWidget(waterParameters.getMinSodium());
		maxSodiumField = ppmWidget(waterParameters.getMaxSodium());
		minMagnesiumField = ppmWidget(waterParameters.getMinMagnesium());
		maxMagnesiumField = ppmWidget(waterParameters.getMaxMagnesium());
		minAlkalinityField = ppmWidget(waterParameters.getMinAlkalinity());
		maxAlkalinityField = ppmWidget(waterParameters.getMaxAlkalinity());
		minResidualAlkalinityField = ppmWidget(waterParameters.getMinResidualAlkalinity());
		maxResidualAlkalinityField = ppmWidget(waterParameters.getMaxResidualAlkalinity());
		descriptionArea = new JTextArea(waterParameters.getDescription() == null ? "" : waterParameters.getDescription(), 14, 36);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		wireTooltips();

		JPanel detailsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints detailsGbc = new GridBagConstraints();
		detailsGbc.insets = new Insets(4, 4, 4, 4);
		detailsGbc.anchor = GridBagConstraints.NORTHWEST;
		SwingDialogFormBuilder form = new SwingDialogFormBuilder(detailsPanel, detailsGbc, 2);
		form.addFieldRow(getUiString("water.parameters.name"), nameField);
		form.addSectionGap();
		form.addLabel(1, "Min");
		form.addLabel(2, "Max");
		form.nextRow();
		addRangeRow(form, "Ca", minCalciumField, maxCalciumField);
		addRangeRow(form, "HCO3", minBicarbonateField, maxBicarbonateField);
		addRangeRow(form, "SO4", minSulfateField, maxSulfateField);
		addRangeRow(form, "Cl", minChlorideField, maxChlorideField);
		addRangeRow(form, "Na", minSodiumField, maxSodiumField);
		addRangeRow(form, "Mg", minMagnesiumField, maxMagnesiumField);
		addRangeRow(form, getUiString("water.parameters.alkalinity") + " (ppm)", minAlkalinityField, maxAlkalinityField);
		addRangeRow(form, getUiString("water.parameters.residual.alkalinity") + " (ppm)", minResidualAlkalinityField, maxResidualAlkalinityField);
		form.addVerticalGlue();

		JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
		descriptionScroll.setPreferredSize(new Dimension(360, 280));
		descriptionScroll.setMinimumSize(new Dimension(300, 220));
		JPanel descriptionPanel = new JPanel(new GridBagLayout());
		GridBagConstraints descriptionGbc = new GridBagConstraints();
		descriptionGbc.insets = new Insets(4, 4, 4, 4);
		descriptionGbc.gridx = 0;
		descriptionGbc.gridy = 0;
		descriptionGbc.anchor = GridBagConstraints.NORTHWEST;
		descriptionPanel.add(new javax.swing.JLabel(getUiString("water.parameters.desc") + ":"), descriptionGbc);
		descriptionGbc.gridy = 1;
		descriptionGbc.weightx = 1.0;
		descriptionGbc.weighty = 1.0;
		descriptionGbc.fill = GridBagConstraints.BOTH;
		descriptionPanel.add(descriptionScroll, descriptionGbc);

		mainGbc.gridx = 0;
		mainGbc.gridy = 0;
		mainGbc.weightx = 1.0;
		mainGbc.weighty = 1.0;
		mainGbc.fill = GridBagConstraints.BOTH;
		panel.add(detailsPanel, mainGbc);
		mainGbc.gridx = 1;
		mainGbc.weightx = 1.0;
		panel.add(descriptionPanel, mainGbc);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton ok = new JButton(getUiString("ui.ok"));
		ok.addActionListener(e -> onOk());
		JButton cancel = new JButton(getUiString("ui.cancel"));
		cancel.addActionListener(e -> dispose());
		buttons.add(ok);
		buttons.add(cancel);
		mainGbc.gridx = 0;
		mainGbc.gridy = 1;
		mainGbc.gridwidth = 2;
		mainGbc.weightx = 1.0;
		mainGbc.weighty = 0;
		mainGbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(buttons, mainGbc);

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
		setResizable(false);
		setLocationRelativeTo(parent);
	}

	private JTextField field(String value)
	{
		return new JTextField(value == null ? "" : value);
	}

	private SwingQuantityEditWidget<PpmUnit> ppmWidget(PpmUnit value)
	{
		SwingQuantityEditWidget<PpmUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PPM, false);
		w.setQuantity(value);
		return w;
	}

	private void addRangeRow(SwingDialogFormBuilder form, String label,
		SwingQuantityEditWidget<PpmUnit> minField, SwingQuantityEditWidget<PpmUnit> maxField)
	{
		String suffix = label.contains("(ppm)") ? "" : " (ppm)";
		form.addLabel(0, label + suffix + ":");
		form.addComponent(1, 1, minField);
		form.addComponent(2, 1, maxField);
		form.nextRow();
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
		if (minCalcium == null && !minCalciumField.isBlank())
		{
			return;
		}
		PpmUnit maxCalcium = parsePpmOrShowError(maxCalciumField);
		if (maxCalcium == null && !maxCalciumField.isBlank())
		{
			return;
		}
		PpmUnit minBicarbonate = parsePpmOrShowError(minBicarbonateField);
		if (minBicarbonate == null && !minBicarbonateField.isBlank())
		{
			return;
		}
		PpmUnit maxBicarbonate = parsePpmOrShowError(maxBicarbonateField);
		if (maxBicarbonate == null && !maxBicarbonateField.isBlank())
		{
			return;
		}
		PpmUnit minSulfate = parsePpmOrShowError(minSulfateField);
		if (minSulfate == null && !minSulfateField.isBlank())
		{
			return;
		}
		PpmUnit maxSulfate = parsePpmOrShowError(maxSulfateField);
		if (maxSulfate == null && !maxSulfateField.isBlank())
		{
			return;
		}
		PpmUnit minChloride = parsePpmOrShowError(minChlorideField);
		if (minChloride == null && !minChlorideField.isBlank())
		{
			return;
		}
		PpmUnit maxChloride = parsePpmOrShowError(maxChlorideField);
		if (maxChloride == null && !maxChlorideField.isBlank())
		{
			return;
		}
		PpmUnit minSodium = parsePpmOrShowError(minSodiumField);
		if (minSodium == null && !minSodiumField.isBlank())
		{
			return;
		}
		PpmUnit maxSodium = parsePpmOrShowError(maxSodiumField);
		if (maxSodium == null && !maxSodiumField.isBlank())
		{
			return;
		}
		PpmUnit minMagnesium = parsePpmOrShowError(minMagnesiumField);
		if (minMagnesium == null && !minMagnesiumField.isBlank())
		{
			return;
		}
		PpmUnit maxMagnesium = parsePpmOrShowError(maxMagnesiumField);
		if (maxMagnesium == null && !maxMagnesiumField.isBlank())
		{
			return;
		}
		PpmUnit minAlkalinity = parsePpmOrShowError(minAlkalinityField);
		if (minAlkalinity == null && !minAlkalinityField.isBlank())
		{
			return;
		}
		PpmUnit maxAlkalinity = parsePpmOrShowError(maxAlkalinityField);
		if (maxAlkalinity == null && !maxAlkalinityField.isBlank())
		{
			return;
		}
		PpmUnit minResidualAlkalinity = parsePpmOrShowError(minResidualAlkalinityField);
		if (minResidualAlkalinity == null && !minResidualAlkalinityField.isBlank())
		{
			return;
		}
		PpmUnit maxResidualAlkalinity = parsePpmOrShowError(maxResidualAlkalinityField);
		if (maxResidualAlkalinity == null && !maxResidualAlkalinityField.isBlank())
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

	private PpmUnit parsePpmOrShowError(SwingQuantityEditWidget<PpmUnit> field)
	{
		try
		{
			return field.parseOrNull();
		}
		catch (NumberFormatException e)
		{
			showValidationError(e);
			focusForValidation(field);
			return null;
		}
	}

	protected void focusForValidation(Component field)
	{
		field.requestFocusInWindow();
		if (field instanceof JTextField jtf)
		{
			jtf.selectAll();
		}
		else if (field instanceof SwingQuantityEditWidget<?> w)
		{
			w.selectAll();
		}
	}

	protected void showValidationError(String message)
	{
		SwingUiErrors.showError(this, message, getUiString("ui.error"));
	}

	protected void showValidationError(Throwable t)
	{
		SwingUiErrors.showError(this, t, getUiString("ui.error"));
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
