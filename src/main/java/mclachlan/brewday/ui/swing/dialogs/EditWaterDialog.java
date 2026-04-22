package mclachlan.brewday.ui.swing.dialogs;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.PpmUnit;

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
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;

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

		addRow(panel, gbc, 0, getUiString("water.name"), nameField);
		addRow(panel, gbc, 1, getUiString("water.calcium"), calciumField);
		addRow(panel, gbc, 2, getUiString("water.bicarbonate"), bicarbonateField);
		addRow(panel, gbc, 3, getUiString("water.sulfate"), sulfateField);
		addRow(panel, gbc, 4, getUiString("water.chloride"), chlorideField);
		addRow(panel, gbc, 5, getUiString("water.sodium"), sodiumField);
		addRow(panel, gbc, 6, getUiString("water.magnesium"), magnesiumField);
		addRow(panel, gbc, 7, getUiString("water.ph"), phField);

		gbc.gridx = 0;
		gbc.gridy = 8;
		gbc.weightx = 0;
		panel.add(new JLabel(getUiString("water.desc") + ":"), gbc);
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(descriptionArea, gbc);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton ok = new JButton(getUiString("ui.ok"));
		ok.addActionListener(e -> onOk());
		JButton cancel = new JButton(getUiString("ui.cancel"));
		cancel.addActionListener(e -> dispose());
		buttons.add(ok);
		buttons.add(cancel);

		gbc.gridx = 0;
		gbc.gridy = 9;
		gbc.gridwidth = 2;
		panel.add(buttons, gbc);

		setContentPane(panel);
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

	private void onOk()
	{
		String name = nameField.getText().trim();
		if (name.isEmpty())
		{
			JOptionPane.showMessageDialog(this, getUiString("ui.name"), getUiString("ui.error"), JOptionPane.ERROR_MESSAGE);
			return;
		}

		try
		{
			Water water = new Water(name);
			water.setCalcium(new PpmUnit(parseNumber(calciumField)));
			water.setBicarbonate(new PpmUnit(parseNumber(bicarbonateField)));
			water.setSulfate(new PpmUnit(parseNumber(sulfateField)));
			water.setChloride(new PpmUnit(parseNumber(chlorideField)));
			water.setSodium(new PpmUnit(parseNumber(sodiumField)));
			water.setMagnesium(new PpmUnit(parseNumber(magnesiumField)));
			water.setPh(new PhUnit(parseNumber(phField)));
			water.setDescription(descriptionArea.getText());
			result = water;
			dispose();
		}
		catch (NumberFormatException e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), getUiString("ui.error"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private double parseNumber(JTextField field)
	{
		String text = field.getText().trim();
		if (text.isEmpty())
		{
			return 0D;
		}
		return Double.parseDouble(text);
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
