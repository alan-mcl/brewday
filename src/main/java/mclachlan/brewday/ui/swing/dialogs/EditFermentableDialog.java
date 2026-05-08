package mclachlan.brewday.ui.swing.dialogs;

import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.ArbitraryPhysicalQuantity;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.DiastaticPowerUnit;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class EditFermentableDialog extends JDialog
{
	private final boolean createMode;
	private final JTextField nameField;
	private final JComboBox<Fermentable.Type> typeField;
	private final JTextField originField;
	private final JTextField supplierField;
	private final JTextField yieldField;
	private final JTextField colourField;
	private final JTextField coarseFineDiffField;
	private final JTextField moistureField;
	private final JTextField diastaticPowerField;
	private final JTextField maxInBatchField;
	private final JTextField distilledWaterPhField;
	private final JTextField bufferingCapacityField;
	private final JTextField lacticAcidContentField;
	private final JCheckBox addAfterBoilField;
	private final JCheckBox recommendMashField;
	private final JTextArea descriptionArea;
	private Fermentable result;

	public EditFermentableDialog(JFrame parent, Fermentable fermentable, boolean createMode)
	{
		super(parent, getUiString(createMode ? "common.add" : "common.edit"), true);
		this.createMode = createMode;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints mainGbc = new GridBagConstraints();
		mainGbc.insets = new Insets(4, 4, 4, 4);
		mainGbc.anchor = GridBagConstraints.NORTHWEST;

		nameField = field(fermentable.getName());
		nameField.setEditable(createMode);
		typeField = new JComboBox<>(Fermentable.Type.values());
		typeField.setSelectedItem(fermentable.getType() == null ? Fermentable.Type.GRAIN : fermentable.getType());
		originField = field(fermentable.getOrigin());
		supplierField = field(fermentable.getSupplier());
		yieldField = field(percent(fermentable.getYield()));
		colourField = field(lovibond(fermentable.getColour()));
		coarseFineDiffField = field(percent(fermentable.getCoarseFineDiff()));
		moistureField = field(percent(fermentable.getMoisture()));
		diastaticPowerField = field(lintner(fermentable.getDiastaticPower()));
		maxInBatchField = field(percent(fermentable.getMaxInBatch()));
		distilledWaterPhField = field(ph(fermentable.getDistilledWaterPh()));
		bufferingCapacityField = field(meqKg(fermentable.getBufferingCapacity()));
		lacticAcidContentField = field(percent(fermentable.getLacticAcidContent()));
		addAfterBoilField = new JCheckBox(getUiString("fermentable.add.after.boil"), fermentable.isAddAfterBoil());
		recommendMashField = new JCheckBox(getUiString("fermentable.recommend.mash"), fermentable.isRecommendMash());
		descriptionArea = new JTextArea(fermentable.getDescription() == null ? "" : fermentable.getDescription(), 14, 36);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);

		JPanel detailsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints detailsGbc = new GridBagConstraints();
		detailsGbc.insets = new Insets(4, 4, 4, 4);
		detailsGbc.anchor = GridBagConstraints.NORTHWEST;
		SwingDialogFormBuilder form = new SwingDialogFormBuilder(detailsPanel, detailsGbc, 1);
		form.addFieldRow(getUiString("fermentable.name"), nameField);
		form.addSectionGap();
		form.addFieldRow(getUiString("fermentable.type"), typeField);
		form.addFieldRow(getUiString("fermentable.origin"), originField);
		form.addFieldRow(getUiString("fermentable.supplier"), supplierField);
		form.addSectionGap();
		form.addFieldRow(getUiString("fermentable.yield"), yieldField);
		form.addFieldRow(getUiString("fermentable.colour"), colourField);
		form.addFieldRow(getUiString("fermentable.coarse.fine.diff"), coarseFineDiffField);
		form.addFieldRow(getUiString("fermentable.moisture"), moistureField);
		form.addFieldRow(getUiString("fermentable.diastatic.power"), diastaticPowerField);
		form.addFieldRow(getUiString("fermentable.max.in.batch"), maxInBatchField);
		form.addFieldRow(getUiString("fermentable.distilled.water.ph"), distilledWaterPhField);
		form.addFieldRow(getUiString("fermentable.buffering.capacity"), bufferingCapacityField);
		form.addFieldRow(getUiString("fermentable.lactic.acid.content"), lacticAcidContentField);
		form.addSectionGap();
		form.addFieldRow("", addAfterBoilField);
		form.addFieldRow("", recommendMashField);
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
		descriptionPanel.add(new javax.swing.JLabel(getUiString("fermentable.desc") + ":"), descriptionGbc);
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

	private String percent(PercentageUnit value)
	{
		return value == null ? "" : String.valueOf(value.get());
	}

	private String lovibond(ColourUnit value)
	{
		return value == null ? "" : String.valueOf(value.get());
	}

	private String lintner(DiastaticPowerUnit value)
	{
		return value == null ? "" : String.valueOf(value.get());
	}

	private String ph(PhUnit value)
	{
		return value == null ? "" : String.valueOf(value.get());
	}

	private String meqKg(ArbitraryPhysicalQuantity value)
	{
		return value == null ? "" : String.valueOf(value.get());
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

		Fermentable fermentable = new Fermentable(name);
		fermentable.setType((Fermentable.Type)typeField.getSelectedItem());
		fermentable.setOrigin(originField.getText().trim());
		fermentable.setSupplier(supplierField.getText().trim());
		fermentable.setYield(parsePercentOrShowError(yieldField));
		if (invalid(yieldField, fermentable.getYield())) return;
		fermentable.setColour(parseLovibondOrShowError(colourField));
		if (invalid(colourField, fermentable.getColour())) return;
		fermentable.setCoarseFineDiff(parsePercentOrShowError(coarseFineDiffField));
		if (invalid(coarseFineDiffField, fermentable.getCoarseFineDiff())) return;
		fermentable.setMoisture(parsePercentOrShowError(moistureField));
		if (invalid(moistureField, fermentable.getMoisture())) return;
		fermentable.setDiastaticPower(parseLintnerOrShowError(diastaticPowerField));
		if (invalid(diastaticPowerField, fermentable.getDiastaticPower())) return;
		fermentable.setMaxInBatch(parsePercentOrShowError(maxInBatchField));
		if (invalid(maxInBatchField, fermentable.getMaxInBatch())) return;
		fermentable.setDistilledWaterPh(parsePhOrShowError(distilledWaterPhField));
		if (invalid(distilledWaterPhField, fermentable.getDistilledWaterPh())) return;
		fermentable.setBufferingCapacity(parseMeqKgOrShowError(bufferingCapacityField));
		if (invalid(bufferingCapacityField, fermentable.getBufferingCapacity())) return;
		fermentable.setLacticAcidContent(parsePercentOrShowError(lacticAcidContentField));
		if (invalid(lacticAcidContentField, fermentable.getLacticAcidContent())) return;
		fermentable.setAddAfterBoil(addAfterBoilField.isSelected());
		fermentable.setRecommendMash(recommendMashField.isSelected());
		fermentable.setDescription(descriptionArea.getText());
		result = fermentable;
		dispose();
	}

	private boolean invalid(JTextField field, Object value)
	{
		return value == null && !field.getText().trim().isEmpty();
	}

	private PercentageUnit parsePercentOrShowError(JTextField field)
	{
		try
		{
			return parsePercent(field);
		}
		catch (NumberFormatException e)
		{
			showValidationError(e.getMessage());
			focusForValidation(field);
			return null;
		}
	}

	private ColourUnit parseLovibondOrShowError(JTextField field)
	{
		try
		{
			return parseLovibond(field);
		}
		catch (NumberFormatException e)
		{
			showValidationError(e.getMessage());
			focusForValidation(field);
			return null;
		}
	}

	private DiastaticPowerUnit parseLintnerOrShowError(JTextField field)
	{
		try
		{
			return parseLintner(field);
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

	private ArbitraryPhysicalQuantity parseMeqKgOrShowError(JTextField field)
	{
		try
		{
			return parseMeqKg(field);
		}
		catch (NumberFormatException e)
		{
			showValidationError(e.getMessage());
			focusForValidation(field);
			return null;
		}
	}

	private PercentageUnit parsePercent(JTextField field)
	{
		String text = field.getText().trim();
		if (text.isEmpty())
		{
			return null;
		}
		return (PercentageUnit)Quantity.parseQuantity(text, Quantity.Unit.PERCENTAGE_DISPLAY);
	}

	private ColourUnit parseLovibond(JTextField field)
	{
		String text = field.getText().trim();
		if (text.isEmpty())
		{
			return null;
		}
		return (ColourUnit)Quantity.parseQuantity(text, Quantity.Unit.LOVIBOND);
	}

	private DiastaticPowerUnit parseLintner(JTextField field)
	{
		String text = field.getText().trim();
		if (text.isEmpty())
		{
			return null;
		}
		return (DiastaticPowerUnit)Quantity.parseQuantity(text, Quantity.Unit.LINTNER);
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

	private ArbitraryPhysicalQuantity parseMeqKg(JTextField field)
	{
		String text = field.getText().trim();
		if (text.isEmpty())
		{
			return null;
		}
		return (ArbitraryPhysicalQuantity)Quantity.parseQuantity(text, Quantity.Unit.MEQ_PER_KILOGRAM);
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

	public Fermentable getResult()
	{
		return result;
	}

	public boolean isCreateMode()
	{
		return createMode;
	}
}
