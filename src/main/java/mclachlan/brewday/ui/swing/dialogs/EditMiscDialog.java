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
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class EditMiscDialog extends JDialog
{
	private final boolean createMode;
	private final JTextField nameField;
	private final JComboBox<Misc.Type> typeField;
	private final JComboBox<Misc.Use> useField;
	private final JComboBox<Quantity.Type> measurementTypeField;
	private final JComboBox<Misc.WaterAdditionFormula> waterAdditionFormulaField;
	private final SwingQuantityEditWidget<PercentageUnit> acidContentField;
	private final JTextField usageRecommendationField;
	private final JTextArea descriptionArea;
	private Misc result;

	public EditMiscDialog(JFrame parent, Misc misc, boolean createMode)
	{
		super(parent, getUiString(createMode ? "common.add" : "common.edit"), true);
		this.createMode = createMode;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints mainGbc = new GridBagConstraints();
		mainGbc.insets = new Insets(4, 4, 4, 4);
		mainGbc.anchor = GridBagConstraints.NORTHWEST;

		nameField = field(misc.getName());
		nameField.setEditable(createMode);
		typeField = new JComboBox<>(Misc.Type.values());
		typeField.setSelectedItem(misc.getType() == null ? Misc.Type.OTHER : misc.getType());
		useField = new JComboBox<>(Misc.Use.values());
		useField.setSelectedItem(misc.getUse() == null ? Misc.Use.BOIL : misc.getUse());
		measurementTypeField = new JComboBox<>(Quantity.Type.values());
		measurementTypeField.setSelectedItem(misc.getMeasurementType() == null ? Quantity.Type.WEIGHT : misc.getMeasurementType());
		DefaultComboBoxModel<Misc.WaterAdditionFormula> formulaModel = new DefaultComboBoxModel<>();
		formulaModel.addElement(null);
		for (Misc.WaterAdditionFormula formula : Misc.WaterAdditionFormula.values())
		{
			formulaModel.addElement(formula);
		}
		waterAdditionFormulaField = new JComboBox<>(formulaModel);
		waterAdditionFormulaField.setRenderer(waterAdditionFormulaRenderer());
		waterAdditionFormulaField.setSelectedItem(misc.getWaterAdditionFormula());
		acidContentField = percentWidget(misc.getAcidContent());
		usageRecommendationField = field(misc.getUsageRecommendation());
		descriptionArea = new JTextArea(misc.getDescription() == null ? "" : misc.getDescription(), 14, 36);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		wireTooltips();

		JPanel detailsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints detailsGbc = new GridBagConstraints();
		detailsGbc.insets = new Insets(4, 4, 4, 4);
		detailsGbc.anchor = GridBagConstraints.NORTHWEST;
		SwingDialogFormBuilder form = new SwingDialogFormBuilder(detailsPanel, detailsGbc, 1);
		form.addFieldRow(getUiString("misc.name"), nameField);
		form.addSectionGap();
		form.addFieldRow(getUiString("misc.type"), typeField);
		form.addFieldRow(getUiString("misc.use"), useField);
		form.addFieldRow(getUiString("misc.measurementType"), measurementTypeField);
		form.addFieldRow(getUiString("misc.water.addition.formula"), waterAdditionFormulaField);
		form.addFieldRow(getUiString("misc.acid.content"), acidContentField);
		form.addSectionGap();
		form.addFieldRow(getUiString("misc.usage.recommendation"), usageRecommendationField);
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
		descriptionPanel.add(new javax.swing.JLabel(getUiString("misc.desc") + ":"), descriptionGbc);
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
		DialogButtonTooltips.wireOkCancel(ok, cancel);
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

	private void wireTooltips()
	{
		nameField.setToolTipText(getUiString("misc.tooltip.name"));
		acidContentField.setToolTipText(getUiString("misc.tooltip.acid.content"));
		usageRecommendationField.setToolTipText(getUiString("misc.tooltip.usage.recommendation"));
		descriptionArea.setToolTipText(getUiString("misc.tooltip.desc"));
	}

	private JTextField field(String value)
	{
		return new JTextField(value == null ? "" : value);
	}

	private DefaultListCellRenderer waterAdditionFormulaRenderer()
	{
		return new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(
				JList<?> list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus)
			{
				Object display = value;
				if (value == null)
				{
					display = getUiString("misc.water.addition.formula.none");
				}
				else if (value instanceof Misc.WaterAdditionFormula formula)
				{
					display = formula.toString();
				}
				return super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
			}
		};
	}

	private SwingQuantityEditWidget<PercentageUnit> percentWidget(PercentageUnit value)
	{
		SwingQuantityEditWidget<PercentageUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		w.setQuantity(value);
		return w;
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

		Misc misc = new Misc(name);
		misc.setType((Misc.Type)typeField.getSelectedItem());
		misc.setUse((Misc.Use)useField.getSelectedItem());
		misc.setMeasurementType((Quantity.Type)measurementTypeField.getSelectedItem());
		misc.setWaterAdditionFormula((Misc.WaterAdditionFormula)waterAdditionFormulaField.getSelectedItem());
		misc.setAcidContent(parsePercentOrShowError(acidContentField));
		if (invalid(acidContentField, misc.getAcidContent())) return;
		misc.setUsageRecommendation(usageRecommendationField.getText().trim());
		misc.setDescription(descriptionArea.getText());
		result = misc;
		dispose();
	}

	private boolean invalid(SwingQuantityEditWidget<?> field, Object value)
	{
		return value == null && !field.isBlank();
	}

	private PercentageUnit parsePercentOrShowError(SwingQuantityEditWidget<PercentageUnit> field)
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

	public Misc getResult()
	{
		return result;
	}

	public boolean isCreateMode()
	{
		return createMode;
	}
}
