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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class EditWaterDialog extends JDialog
{
	private final boolean createMode;
	private final JTextField nameField;
	private final JTextField calciumField;
	private final JTextField bicarbonateField;
	private final JTextField sulfateField;
	private final JTextField chlorideField;
	private final JTextField sodiumField;
	private final JTextField magnesiumField;
	private final JTextField phField;
	private final JTextArea descriptionArea;
	private Water result;

	public EditWaterDialog(JFrame parent, Water water, boolean createMode)
	{
		super(parent, getUiString(createMode ? "common.add" : "common.edit"), true);
		this.createMode = createMode;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.NORTHWEST;

		nameField = field(water.getName());
		nameField.setEditable(createMode);
		calciumField = field(ppm(water.getCalcium()));
		bicarbonateField = field(ppm(water.getBicarbonate()));
		sulfateField = field(ppm(water.getSulfate()));
		chlorideField = field(ppm(water.getChloride()));
		sodiumField = field(ppm(water.getSodium()));
		magnesiumField = field(ppm(water.getMagnesium()));
		phField = field(ph(water.getPh()));
		descriptionArea = new JTextArea(water.getDescription() == null ? "" : water.getDescription(), 4, 24);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		wireTooltips();

		SwingDialogFormBuilder form = new SwingDialogFormBuilder(panel, gbc, 1);
		form.addFieldRow(getUiString("water.name"), nameField);
		form.addSectionGap();
		form.addFieldRow(getUiString("water.calcium"), calciumField);
		form.addFieldRow(getUiString("water.bicarbonate"), bicarbonateField);
		form.addFieldRow(getUiString("water.sulfate"), sulfateField);
		form.addFieldRow(getUiString("water.chloride"), chlorideField);
		form.addFieldRow(getUiString("water.sodium"), sodiumField);
		form.addFieldRow(getUiString("water.magnesium"), magnesiumField);
		form.addFieldRow(getUiString("water.ph"), phField);
		form.addSectionGap();
		form.addFieldRow(getUiString("water.desc"), new JScrollPane(descriptionArea));

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton ok = new JButton(getUiString("ui.ok"));
		ok.addActionListener(e -> onOk());
		JButton cancel = new JButton(getUiString("ui.cancel"));
		cancel.addActionListener(e -> dispose());
		buttons.add(ok);
		buttons.add(cancel);

		form.addSectionGap();
		form.addComponent(0, 2, buttons);
		form.nextRow();

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

	private String ph(PhUnit value)
	{
		return value == null ? "" : String.valueOf(value.get());
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
		if (calcium == null && !calciumField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit bicarbonate = parsePpmOrShowError(bicarbonateField);
		if (bicarbonate == null && !bicarbonateField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit sulfate = parsePpmOrShowError(sulfateField);
		if (sulfate == null && !sulfateField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit chloride = parsePpmOrShowError(chlorideField);
		if (chloride == null && !chlorideField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit sodium = parsePpmOrShowError(sodiumField);
		if (sodium == null && !sodiumField.getText().trim().isEmpty())
		{
			return;
		}
		PpmUnit magnesium = parsePpmOrShowError(magnesiumField);
		if (magnesium == null && !magnesiumField.getText().trim().isEmpty())
		{
			return;
		}
		PhUnit ph = parsePhOrShowError(phField);
		if (ph == null && !phField.getText().trim().isEmpty())
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

	private PhUnit parsePhOrShowError(JTextField field)
	{
		try
		{
			return parsePh(field);
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

	private PhUnit parsePh(JTextField field)
	{
		String text = field.getText().trim();
		if (text.isEmpty())
		{
			return null;
		}
		return (PhUnit)Quantity.parseQuantity(text, Quantity.Unit.PH);
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

	public Water getResult()
	{
		return result;
	}

	public boolean isCreateMode()
	{
		return createMode;
	}
}
