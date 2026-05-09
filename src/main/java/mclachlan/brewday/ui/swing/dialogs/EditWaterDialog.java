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
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class EditWaterDialog extends JDialog
{
	private final boolean createMode;
	private final JTextField nameField;
	private final SwingQuantityEditWidget<PpmUnit> calciumField;
	private final SwingQuantityEditWidget<PpmUnit> bicarbonateField;
	private final SwingQuantityEditWidget<PpmUnit> sulfateField;
	private final SwingQuantityEditWidget<PpmUnit> chlorideField;
	private final SwingQuantityEditWidget<PpmUnit> sodiumField;
	private final SwingQuantityEditWidget<PpmUnit> magnesiumField;
	private final SwingQuantityEditWidget<PhUnit> phField;
	private final JTextArea descriptionArea;
	private Water result;

	public EditWaterDialog(JFrame parent, Water water, boolean createMode)
	{
		super(parent, getUiString(createMode ? "common.add" : "common.edit"), true);
		this.createMode = createMode;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints mainGbc = new GridBagConstraints();
		mainGbc.insets = new Insets(4, 4, 4, 4);
		mainGbc.anchor = GridBagConstraints.NORTHWEST;

		nameField = field(water.getName());
		nameField.setEditable(createMode);
		calciumField = ppmWidget(water.getCalcium());
		bicarbonateField = ppmWidget(water.getBicarbonate());
		sulfateField = ppmWidget(water.getSulfate());
		chlorideField = ppmWidget(water.getChloride());
		sodiumField = ppmWidget(water.getSodium());
		magnesiumField = ppmWidget(water.getMagnesium());
		phField = phWidget(water.getPh());
		descriptionArea = new JTextArea(water.getDescription() == null ? "" : water.getDescription(), 14, 36);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		wireTooltips();

		JPanel detailsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints detailsGbc = new GridBagConstraints();
		detailsGbc.insets = new Insets(4, 4, 4, 4);
		detailsGbc.anchor = GridBagConstraints.NORTHWEST;
		SwingDialogFormBuilder form = new SwingDialogFormBuilder(detailsPanel, detailsGbc, 1);
		form.addFieldRow(getUiString("water.name"), nameField);
		form.addSectionGap();
		form.addFieldRow(getUiString("water.calcium"), calciumField);
		form.addFieldRow(getUiString("water.bicarbonate"), bicarbonateField);
		form.addFieldRow(getUiString("water.sulfate"), sulfateField);
		form.addFieldRow(getUiString("water.chloride"), chlorideField);
		form.addFieldRow(getUiString("water.sodium"), sodiumField);
		form.addFieldRow(getUiString("water.magnesium"), magnesiumField);
		form.addFieldRow(getUiString("water.ph"), phField);
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
		descriptionPanel.add(new javax.swing.JLabel(getUiString("water.desc") + ":"), descriptionGbc);
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
		SwingQuantityEditWidget<PpmUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PPM);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<PhUnit> phWidget(PhUnit value)
	{
		SwingQuantityEditWidget<PhUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PH);
		w.setQuantity(value);
		return w;
	}

	private void wireTooltips()
	{
		nameField.setToolTipText(getUiString("water.tooltip.name"));
		calciumField.setToolTipText(getUiString("water.tooltip.calcium"));
		bicarbonateField.setToolTipText(getUiString("water.tooltip.bicarbonate"));
		sulfateField.setToolTipText(getUiString("water.tooltip.sulfate"));
		chlorideField.setToolTipText(getUiString("water.tooltip.chloride"));
		sodiumField.setToolTipText(getUiString("water.tooltip.sodium"));
		magnesiumField.setToolTipText(getUiString("water.tooltip.magnesium"));
		phField.setToolTipText(getUiString("water.tooltip.ph"));
		descriptionArea.setToolTipText(getUiString("water.tooltip.desc"));
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

		Water water = new Water(name);
		PpmUnit calcium = parsePpmOrShowError(calciumField);
		if (calcium == null && !calciumField.isBlank())
		{
			return;
		}
		PpmUnit bicarbonate = parsePpmOrShowError(bicarbonateField);
		if (bicarbonate == null && !bicarbonateField.isBlank())
		{
			return;
		}
		PpmUnit sulfate = parsePpmOrShowError(sulfateField);
		if (sulfate == null && !sulfateField.isBlank())
		{
			return;
		}
		PpmUnit chloride = parsePpmOrShowError(chlorideField);
		if (chloride == null && !chlorideField.isBlank())
		{
			return;
		}
		PpmUnit sodium = parsePpmOrShowError(sodiumField);
		if (sodium == null && !sodiumField.isBlank())
		{
			return;
		}
		PpmUnit magnesium = parsePpmOrShowError(magnesiumField);
		if (magnesium == null && !magnesiumField.isBlank())
		{
			return;
		}
		PhUnit ph = parsePhOrShowError(phField);
		if (ph == null && !phField.isBlank())
		{
			return;
		}

		water.setCalcium(calcium);
		water.setBicarbonate(bicarbonate);
		water.setSulfate(sulfate);
		water.setChloride(chloride);
		water.setSodium(sodium);
		water.setMagnesium(magnesium);
		water.setPh(ph);
		water.setDescription(descriptionArea.getText());
		result = water;
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
			showValidationError(e.getMessage());
			focusForValidation(field);
			return null;
		}
	}

	private PhUnit parsePhOrShowError(SwingQuantityEditWidget<PhUnit> field)
	{
		try
		{
			return field.parseOrNull();
		}
		catch (NumberFormatException e)
		{
			showValidationError(e.getMessage());
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

	public Water getResult()
	{
		return result;
	}

	public boolean isCreateMode()
	{
		return createMode;
	}
}
