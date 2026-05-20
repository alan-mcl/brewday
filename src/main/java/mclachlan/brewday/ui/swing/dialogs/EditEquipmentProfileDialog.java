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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.ArbitraryPhysicalQuantity;
import mclachlan.brewday.math.LengthUnit;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.PowerUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class EditEquipmentProfileDialog extends JDialog
{
	private final boolean createMode;
	private final JTextField nameField;
	private final SwingQuantityEditWidget<LengthUnit> elevationField;
	private final SwingQuantityEditWidget<PercentageUnit> conversionEfficiencyField;
	private final SwingQuantityEditWidget<VolumeUnit> mashTunVolumeField;
	private final SwingQuantityEditWidget<WeightUnit> mashTunWeightField;
	private final SwingQuantityEditWidget<ArbitraryPhysicalQuantity> mashTunSpecificHeatField;
	private final SwingQuantityEditWidget<VolumeUnit> lauterLossField;
	private final SwingQuantityEditWidget<VolumeUnit> boilKettleVolumeField;
	private final SwingQuantityEditWidget<LengthUnit> boilKettleDiameterField;
	private final SwingQuantityEditWidget<LengthUnit> boilKettleOpeningDiameterField;
	private final SwingQuantityEditWidget<PercentageUnit> boilEvaporationField;
	private final SwingQuantityEditWidget<PowerUnit> boilElementPowerField;
	private final SwingQuantityEditWidget<PercentageUnit> hopUtilisationField;
	private final SwingQuantityEditWidget<VolumeUnit> trubChillerLossField;
	private final SwingQuantityEditWidget<VolumeUnit> fermenterVolumeField;
	private final JTextArea descriptionArea;
	private EquipmentProfile result;

	public EditEquipmentProfileDialog(JFrame parent, EquipmentProfile profile, boolean createMode)
	{
		super(parent, getUiString(createMode ? "common.add" : "common.edit"), true);
		this.createMode = createMode;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints mainGbc = new GridBagConstraints();
		mainGbc.insets = new Insets(4, 4, 4, 4);
		mainGbc.anchor = GridBagConstraints.NORTHWEST;

		nameField = field(profile.getName());
		nameField.setEditable(createMode);
		elevationField = lengthWidget(profile.getElevation());
		conversionEfficiencyField = percentWidget(profile.getConversionEfficiency());
		mashTunVolumeField = volumeWidget(profile.getMashTunVolume());
		mashTunWeightField = weightWidget(profile.getMashTunWeight());
		mashTunSpecificHeatField = specificHeatWidget(profile.getMashTunSpecificHeat());
		lauterLossField = volumeWidget(profile.getLauterLoss());
		boilKettleVolumeField = volumeWidget(profile.getBoilKettleVolume());
		boilKettleDiameterField = lengthWidgetCentimetre(profile.getBoilKettleDiameter());
		boilKettleOpeningDiameterField = lengthWidgetCentimetre(profile.getBoilKettleOpeningDiameter());
		boilEvaporationField = percentWidget(profile.getBoilEvapourationRate());
		boilElementPowerField = powerWidget(profile.getBoilElementPower());
		hopUtilisationField = percentWidget(profile.getHopUtilisation());
		trubChillerLossField = volumeWidget(profile.getTrubAndChillerLoss());
		fermenterVolumeField = volumeWidget(profile.getFermenterVolume());
		descriptionArea = new JTextArea(profile.getDescription() == null ? "" : profile.getDescription(), 14, 36);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		wireTooltips();

		JPanel detailsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints detailsGbc = new GridBagConstraints();
		detailsGbc.insets = new Insets(4, 4, 4, 4);
		detailsGbc.anchor = GridBagConstraints.NORTHWEST;
		SwingDialogFormBuilder form = new SwingDialogFormBuilder(detailsPanel, detailsGbc, 1);
		form.addFieldRow(getUiString("equipment.name"), nameField);
		form.addSectionGap();
		form.addFieldRow(getUiString("equipment.elevation"), elevationField);
		form.addFieldRow(getUiString("equipment.conversion.efficiency"), conversionEfficiencyField);
		form.addSectionGap();
		form.addFieldRow(getUiString("equipment.mash.tun.volume"), mashTunVolumeField);
		form.addFieldRow(getUiString("equipment.mash.tun.weight"), mashTunWeightField);
		form.addFieldRow(getUiString("equipment.mash.tun.specific.heat"), mashTunSpecificHeatField);
		form.addFieldRow(getUiString("equipment.lauter.loss"), lauterLossField);
		form.addSectionGap();
		form.addFieldRow(getUiString("equipment.boil.kettle.volume"), boilKettleVolumeField);
		form.addFieldRow(getUiString("equipment.boil.kettle.diameter"), boilKettleDiameterField);
		form.addFieldRow(getUiString("equipment.boil.kettle.opening.diameter"), boilKettleOpeningDiameterField);
		form.addFieldRow(getUiString("equipment.evapouration"), boilEvaporationField);
		form.addFieldRow(getUiString("equipment.boil.element.power"), boilElementPowerField);
		form.addFieldRow(getUiString("equipment.hop.utilisation"), hopUtilisationField);
		form.addFieldRow(getUiString("equipment.trub.chiller.loss"), trubChillerLossField);
		form.addSectionGap();
		form.addFieldRow(getUiString("equipment.fermenter.volume"), fermenterVolumeField);
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
		descriptionPanel.add(new JLabel(getUiString("equipment.desc") + ":"), descriptionGbc);
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
		nameField.setToolTipText(getUiString("equipment.tooltip.name"));
		elevationField.setToolTipText(getUiString("equipment.tooltip.elevation"));
		conversionEfficiencyField.setToolTipText(getUiString("equipment.tooltip.conversion.efficiency"));
		mashTunVolumeField.setToolTipText(getUiString("equipment.tooltip.mash.tun.volume"));
		mashTunWeightField.setToolTipText(getUiString("equipment.tooltip.mash.tun.weight"));
		mashTunSpecificHeatField.setToolTipText(getUiString("equipment.tooltip.mash.tun.specific.heat"));
		lauterLossField.setToolTipText(getUiString("equipment.tooltip.lauter.loss"));
		boilKettleVolumeField.setToolTipText(getUiString("equipment.tooltip.boil.kettle.volume"));
		boilKettleDiameterField.setToolTipText(getUiString("equipment.tooltip.boil.kettle.diameter"));
		boilKettleOpeningDiameterField.setToolTipText(getUiString("equipment.tooltip.boil.kettle.opening.diameter"));
		boilEvaporationField.setToolTipText(getUiString("equipment.tooltip.evapouration"));
		boilElementPowerField.setToolTipText(getUiString("equipment.tooltip.boil.element.power"));
		hopUtilisationField.setToolTipText(getUiString("equipment.tooltip.hop.utilisation"));
		trubChillerLossField.setToolTipText(getUiString("equipment.tooltip.trub.chiller.loss"));
		fermenterVolumeField.setToolTipText(getUiString("equipment.tooltip.fermenter.volume"));
		descriptionArea.setToolTipText(getUiString("equipment.tooltip.desc"));
	}

	private JTextField field(String value)
	{
		return new JTextField(value == null ? "" : value);
	}

	private SwingQuantityEditWidget<LengthUnit> lengthWidget(LengthUnit value)
	{
		SwingQuantityEditWidget<LengthUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.METRE);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<LengthUnit> lengthWidgetCentimetre(LengthUnit value)
	{
		SwingQuantityEditWidget<LengthUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.CENTIMETRE);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<PercentageUnit> percentWidget(PercentageUnit value)
	{
		SwingQuantityEditWidget<PercentageUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<VolumeUnit> volumeWidget(VolumeUnit value)
	{
		SwingQuantityEditWidget<VolumeUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.LITRES);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<WeightUnit> weightWidget(WeightUnit value)
	{
		SwingQuantityEditWidget<WeightUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.KILOGRAMS);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<PowerUnit> powerWidget(PowerUnit value)
	{
		SwingQuantityEditWidget<PowerUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.KILOWATT);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<ArbitraryPhysicalQuantity> specificHeatWidget(ArbitraryPhysicalQuantity value)
	{
		SwingQuantityEditWidget<ArbitraryPhysicalQuantity> w = new SwingQuantityEditWidget<>(Quantity.Unit.JOULE_PER_KG_CELSIUS);
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

		EquipmentProfile out = new EquipmentProfile(name);
		out.setDescription(descriptionArea.getText());
		out.setElevation(parseLengthOrShowError(elevationField));
		if (invalid(elevationField, out.getElevation())) return;
		out.setConversionEfficiency(parsePercentOrShowError(conversionEfficiencyField));
		if (invalid(conversionEfficiencyField, out.getConversionEfficiency())) return;
		out.setMashTunVolume(parseVolumeOrShowError(mashTunVolumeField));
		if (invalid(mashTunVolumeField, out.getMashTunVolume())) return;
		out.setMashTunWeight(parseWeightOrShowError(mashTunWeightField));
		if (invalid(mashTunWeightField, out.getMashTunWeight())) return;
		out.setMashTunSpecificHeat(parseSpecificHeatOrShowError(mashTunSpecificHeatField));
		if (invalid(mashTunSpecificHeatField, out.getMashTunSpecificHeat())) return;
		out.setLauterLoss(parseVolumeOrShowError(lauterLossField));
		if (invalid(lauterLossField, out.getLauterLoss())) return;
		out.setBoilKettleVolume(parseVolumeOrShowError(boilKettleVolumeField));
		if (invalid(boilKettleVolumeField, out.getBoilKettleVolume())) return;
		out.setBoilKettleDiameter(parseLengthOrShowError(boilKettleDiameterField));
		if (invalid(boilKettleDiameterField, out.getBoilKettleDiameter())) return;
		out.setBoilKettleOpeningDiameter(parseLengthOrShowError(boilKettleOpeningDiameterField));
		if (invalid(boilKettleOpeningDiameterField, out.getBoilKettleOpeningDiameter())) return;
		out.setBoilEvapourationRate(parsePercentOrShowError(boilEvaporationField));
		if (invalid(boilEvaporationField, out.getBoilEvapourationRate())) return;
		out.setBoilElementPower(parsePowerOrShowError(boilElementPowerField));
		if (invalid(boilElementPowerField, out.getBoilElementPower())) return;
		out.setHopUtilisation(parsePercentOrShowError(hopUtilisationField));
		if (invalid(hopUtilisationField, out.getHopUtilisation())) return;
		out.setTrubAndChillerLoss(parseVolumeOrShowError(trubChillerLossField));
		if (invalid(trubChillerLossField, out.getTrubAndChillerLoss())) return;
		out.setFermenterVolume(parseVolumeOrShowError(fermenterVolumeField));
		if (invalid(fermenterVolumeField, out.getFermenterVolume())) return;

		result = out;
		dispose();
	}

	private boolean invalid(SwingQuantityEditWidget<?> field, Object value)
	{
		return value == null && !field.isBlank();
	}

	private LengthUnit parseLengthOrShowError(SwingQuantityEditWidget<LengthUnit> field)
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

	private VolumeUnit parseVolumeOrShowError(SwingQuantityEditWidget<VolumeUnit> field)
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

	private WeightUnit parseWeightOrShowError(SwingQuantityEditWidget<WeightUnit> field)
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

	private PowerUnit parsePowerOrShowError(SwingQuantityEditWidget<PowerUnit> field)
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

	private ArbitraryPhysicalQuantity parseSpecificHeatOrShowError(SwingQuantityEditWidget<ArbitraryPhysicalQuantity> field)
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

	private void focusForValidation(Component field)
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

	private void showValidationError(String message)
	{
		SwingUiErrors.showError(this, message, getUiString("ui.error"));
	}

	private void showValidationError(Throwable t)
	{
		SwingUiErrors.showError(this, t, getUiString("ui.error"));
	}

	public EquipmentProfile getResult()
	{
		return result;
	}

	public boolean isCreateMode()
	{
		return createMode;
	}
}
