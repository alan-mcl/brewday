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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class EditYeastDialog extends JDialog
{
	private final boolean createMode;
	private final JTextField nameField;
	private final JComboBox<Yeast.Type> typeField;
	private final JComboBox<Yeast.Form> formField;
	private final JTextField laboratoryField;
	private final JTextField productIdField;
	private final SwingQuantityEditWidget<PercentageUnit> attenuationField;
	private final JComboBox<Yeast.Flocculation> flocculationField;
	private final SwingQuantityEditWidget<TemperatureUnit> minTempField;
	private final SwingQuantityEditWidget<TemperatureUnit> maxTempField;
	private final JTextField recommendedStylesField;
	private final JTextArea descriptionArea;
	private Yeast result;

	public EditYeastDialog(JFrame parent, Yeast yeast, boolean createMode)
	{
		super(parent, getUiString(createMode ? "common.add" : "common.edit"), true);
		this.createMode = createMode;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints mainGbc = new GridBagConstraints();
		mainGbc.insets = new Insets(4, 4, 4, 4);
		mainGbc.anchor = GridBagConstraints.NORTHWEST;

		nameField = field(yeast.getName());
		nameField.setEditable(createMode);
		typeField = new JComboBox<>(Yeast.Type.values());
		typeField.setSelectedItem(yeast.getType() == null ? Yeast.Type.ALE : yeast.getType());
		formField = new JComboBox<>(Yeast.Form.values());
		formField.setSelectedItem(yeast.getForm() == null ? Yeast.Form.DRY : yeast.getForm());
		laboratoryField = field(yeast.getLaboratory());
		productIdField = field(yeast.getProductId());
		attenuationField = percentWidget(yeast.getAttenuation());
		flocculationField = new JComboBox<>(Yeast.Flocculation.values());
		flocculationField.setSelectedItem(yeast.getFlocculation() == null ? Yeast.Flocculation.MEDIUM : yeast.getFlocculation());
		minTempField = tempWidget(yeast.getMinTemp());
		maxTempField = tempWidget(yeast.getMaxTemp());
		recommendedStylesField = field(yeast.getRecommendedStyles());
		descriptionArea = new JTextArea(yeast.getDescription() == null ? "" : yeast.getDescription(), 14, 36);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);

		JPanel detailsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints detailsGbc = new GridBagConstraints();
		detailsGbc.insets = new Insets(4, 4, 4, 4);
		detailsGbc.anchor = GridBagConstraints.NORTHWEST;
		SwingDialogFormBuilder form = new SwingDialogFormBuilder(detailsPanel, detailsGbc, 1);
		form.addFieldRow(getUiString("yeast.name"), nameField);
		form.addSectionGap();
		form.addFieldRow(getUiString("yeast.type"), typeField);
		form.addFieldRow(getUiString("yeast.form"), formField);
		form.addFieldRow(getUiString("yeast.laboratory"), laboratoryField);
		form.addFieldRow(getUiString("yeast.product.id"), productIdField);
		form.addSectionGap();
		form.addFieldRow(getUiString("yeast.attenuation"), attenuationField);
		form.addFieldRow(getUiString("yeast.flocculation"), flocculationField);
		form.addFieldRow(getUiString("yeast.min.temp"), minTempField);
		form.addFieldRow(getUiString("yeast.max.temp"), maxTempField);
		form.addFieldRow(getUiString("yeast.styles"), recommendedStylesField);
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
		descriptionPanel.add(new javax.swing.JLabel(getUiString("yeast.desc") + ":"), descriptionGbc);
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

	private JTextField field(String value)
	{
		return new JTextField(value == null ? "" : value);
	}

	private SwingQuantityEditWidget<PercentageUnit> percentWidget(PercentageUnit value)
	{
		SwingQuantityEditWidget<PercentageUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<TemperatureUnit> tempWidget(TemperatureUnit value)
	{
		SwingQuantityEditWidget<TemperatureUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.CELSIUS);
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

		Yeast yeast = new Yeast(name);
		yeast.setType((Yeast.Type)typeField.getSelectedItem());
		yeast.setForm((Yeast.Form)formField.getSelectedItem());
		yeast.setLaboratory(laboratoryField.getText().trim());
		yeast.setProductId(productIdField.getText().trim());
		yeast.setAttenuation(parsePercentOrShowError(attenuationField));
		if (invalid(attenuationField, yeast.getAttenuation())) return;
		yeast.setFlocculation((Yeast.Flocculation)flocculationField.getSelectedItem());
		yeast.setMinTemp(parseCelsiusOrShowError(minTempField));
		if (invalid(minTempField, yeast.getMinTemp())) return;
		yeast.setMaxTemp(parseCelsiusOrShowError(maxTempField));
		if (invalid(maxTempField, yeast.getMaxTemp())) return;
		yeast.setRecommendedStyles(recommendedStylesField.getText().trim());
		yeast.setDescription(descriptionArea.getText());
		result = yeast;
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

	private TemperatureUnit parseCelsiusOrShowError(SwingQuantityEditWidget<TemperatureUnit> field)
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

	public Yeast getResult()
	{
		return result;
	}

	public boolean isCreateMode()
	{
		return createMode;
	}
}
