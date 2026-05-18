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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
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
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class EditFermentableDialog extends JDialog
{
	private final boolean createMode;
	private final JTextField nameField;
	private final JComboBox<Fermentable.Type> typeField;
	private final JTextField originField;
	private final JTextField supplierField;
	private final SwingQuantityEditWidget<PercentageUnit> yieldField;
	private final SwingQuantityEditWidget<ColourUnit> colourField;
	private final SwingQuantityEditWidget<PercentageUnit> coarseFineDiffField;
	private final SwingQuantityEditWidget<PercentageUnit> moistureField;
	private final SwingQuantityEditWidget<DiastaticPowerUnit> diastaticPowerField;
	private final SwingQuantityEditWidget<PercentageUnit> maxInBatchField;
	private final SwingQuantityEditWidget<PhUnit> distilledWaterPhField;
	private final SwingQuantityEditWidget<ArbitraryPhysicalQuantity> bufferingCapacityField;
	private final SwingQuantityEditWidget<PercentageUnit> lacticAcidContentField;
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
		yieldField = percentWidget(fermentable.getYield());
		colourField = colourWidget(fermentable.getColour());
		coarseFineDiffField = percentWidget(fermentable.getCoarseFineDiff());
		moistureField = percentWidget(fermentable.getMoisture());
		diastaticPowerField = lintnerWidget(fermentable.getDiastaticPower());
		maxInBatchField = percentWidget(fermentable.getMaxInBatch());
		distilledWaterPhField = phWidget(fermentable.getDistilledWaterPh());
		bufferingCapacityField = meqWidget(fermentable.getBufferingCapacity());
		lacticAcidContentField = percentWidget(fermentable.getLacticAcidContent());
		addAfterBoilField = new JCheckBox(getUiString("fermentable.add.after.boil"), fermentable.isAddAfterBoil());
		recommendMashField = new JCheckBox(getUiString("fermentable.recommend.mash"), fermentable.isRecommendMash());
		descriptionArea = new JTextArea(fermentable.getDescription() == null ? "" : fermentable.getDescription(), 14, 36);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		wireTooltips();

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
		nameField.setToolTipText(getUiString("fermentable.tooltip.name"));
		originField.setToolTipText(getUiString("fermentable.tooltip.origin"));
		supplierField.setToolTipText(getUiString("fermentable.tooltip.supplier"));
		yieldField.setToolTipText(getUiString("fermentable.tooltip.yield"));
		colourField.setToolTipText(getUiString("fermentable.tooltip.colour"));
		coarseFineDiffField.setToolTipText(getUiString("fermentable.tooltip.coarse.fine"));
		moistureField.setToolTipText(getUiString("fermentable.tooltip.moisture"));
		diastaticPowerField.setToolTipText(getUiString("fermentable.tooltip.diastatic.power"));
		maxInBatchField.setToolTipText(getUiString("fermentable.tooltip.max.in.batch"));
		distilledWaterPhField.setToolTipText(getUiString("fermentable.tooltip.distilled.water.ph"));
		bufferingCapacityField.setToolTipText(getUiString("fermentable.tooltip.buffering.capacity"));
		lacticAcidContentField.setToolTipText(getUiString("fermentable.tooltip.lactic.acid"));
		descriptionArea.setToolTipText(getUiString("fermentable.tooltip.desc"));
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

	private SwingQuantityEditWidget<ColourUnit> colourWidget(ColourUnit value)
	{
		SwingQuantityEditWidget<ColourUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.SRM);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<DiastaticPowerUnit> lintnerWidget(DiastaticPowerUnit value)
	{
		SwingQuantityEditWidget<DiastaticPowerUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.LINTNER);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<PhUnit> phWidget(PhUnit value)
	{
		SwingQuantityEditWidget<PhUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PH);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<ArbitraryPhysicalQuantity> meqWidget(ArbitraryPhysicalQuantity value)
	{
		SwingQuantityEditWidget<ArbitraryPhysicalQuantity> w = new SwingQuantityEditWidget<>(Quantity.Unit.MEQ_PER_KILOGRAM);
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

		Fermentable fermentable = new Fermentable(name);
		fermentable.setType((Fermentable.Type)typeField.getSelectedItem());
		fermentable.setOrigin(originField.getText().trim());
		fermentable.setSupplier(supplierField.getText().trim());
		fermentable.setYield(parsePercentOrShowError(yieldField));
		if (invalid(yieldField, fermentable.getYield())) return;
		fermentable.setColour(parseSrmOrShowError(colourField));
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

	private ColourUnit parseSrmOrShowError(SwingQuantityEditWidget<ColourUnit> field)
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

	private DiastaticPowerUnit parseLintnerOrShowError(SwingQuantityEditWidget<DiastaticPowerUnit> field)
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

	private PhUnit parsePhOrShowError(SwingQuantityEditWidget<PhUnit> field)
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

	private ArbitraryPhysicalQuantity parseMeqKgOrShowError(SwingQuantityEditWidget<ArbitraryPhysicalQuantity> field)
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

	public Fermentable getResult()
	{
		return result;
	}

	public boolean isCreateMode()
	{
		return createMode;
	}
}
