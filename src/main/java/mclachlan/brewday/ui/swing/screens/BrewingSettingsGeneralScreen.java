package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.math.Quantity.Unit.PERCENTAGE;
import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing port of JFX {@code BrewingSettingsGeneralPane}: brewing defaults and hop utilisation
 * settings persist immediately via {@link Database#saveSettings()}.
 */
public class BrewingSettingsGeneralScreen extends JPanel implements SwingScreen
{
	private boolean refreshing;

	private final JComboBox<String> defaultEquipmentProfile = new JComboBox<>();

	private final SwingQuantityEditWidget<PercentageUnit> mashHopUtilisation =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);

	private final SwingQuantityEditWidget<PercentageUnit> firstWortHopUtilisation =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);

	private final SwingQuantityEditWidget<PercentageUnit> leafHopAdjustment =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);

	private final SwingQuantityEditWidget<PercentageUnit> plugHopAdjustment =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);

	private final SwingQuantityEditWidget<PercentageUnit> pelletHopAdjustment =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);

	public BrewingSettingsGeneralScreen()
	{
		super(new BorderLayout());
		JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(new EmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.0;

		form.add(new JLabel(getUiString("settings.default.equipment.profile")), gbc);
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		defaultEquipmentProfile.setToolTipText(getUiString("settings.default.equipment.profile.tooltip"));
		form.add(defaultEquipmentProfile, gbc);

		addPercentRow(form, gbc, getUiString("settings.mash.hop.utilisation"), mashHopUtilisation,
			"settings.mash.hop.utilisation.tooltip");
		addPercentRow(form, gbc, getUiString("settings.first.wort.hop.utilisation"), firstWortHopUtilisation,
			"settings.first.wort.hop.utilisation.tooltip");
		addPercentRow(form, gbc, getUiString("settings.leaf.hop.adjustment"), leafHopAdjustment,
			"settings.leaf.hop.adjustment.tooltip");
		addPercentRow(form, gbc, getUiString("settings.plug.hop.adjustment"), plugHopAdjustment,
			"settings.plug.hop.adjustment.tooltip");
		addPercentRow(form, gbc, getUiString("settings.pellet.hop.adjustment"), pelletHopAdjustment,
			"settings.pellet.hop.adjustment.tooltip");

		add(form, BorderLayout.NORTH);
		wirePersistence();
		refresh();
	}

	private static void addPercentRow(JPanel form, GridBagConstraints gbc, String labelText,
		SwingQuantityEditWidget<PercentageUnit> widget, String tooltipKey)
	{
		gbc.gridy++;
		gbc.gridx = 0;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		form.add(new JLabel(labelText), gbc);
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		widget.setToolTipText(getUiString(tooltipKey));
		form.add(widget, gbc);
	}

	private void wirePersistence()
	{
		defaultEquipmentProfile.addActionListener(e ->
		{
			if (refreshing)
			{
				return;
			}
			Object selected = defaultEquipmentProfile.getSelectedItem();
			if (selected instanceof String s)
			{
				Database.getInstance().getSettings().set(Settings.DEFAULT_EQUIPMENT_PROFILE, s);
				Database.getInstance().saveSettings();
			}
		});

		mashHopUtilisation.addQuantityChangeListener(q ->
			persistPercentSetting(q, Settings.MASH_HOP_UTILISATION));
		firstWortHopUtilisation.addQuantityChangeListener(q ->
			persistPercentSetting(q, Settings.FIRST_WORT_HOP_UTILISATION));
		leafHopAdjustment.addQuantityChangeListener(q ->
			persistPercentSetting(q, Settings.LEAF_HOP_ADJUSTMENT));
		plugHopAdjustment.addQuantityChangeListener(q ->
			persistPercentSetting(q, Settings.PLUG_HOP_ADJUSTMENT));
		pelletHopAdjustment.addQuantityChangeListener(q ->
			persistPercentSetting(q, Settings.PELLET_HOP_ADJUSTMENT));
	}

	private void persistPercentSetting(PercentageUnit quantity, String settingsKey)
	{
		if (refreshing || quantity == null)
		{
			return;
		}
		Database.getInstance().getSettings().set(settingsKey,
			"" + quantity.get(PERCENTAGE));
		Database.getInstance().saveSettings();
	}

	@Override
	public void refresh()
	{
		this.refreshing = true;
		try
		{
			Settings settings = Database.getInstance().getSettings();

			ArrayList<String> equipmentProfiles = new ArrayList<>(
				Database.getInstance().getEquipmentProfiles().keySet());
			equipmentProfiles.sort(Comparator.comparing(String::toString));
			defaultEquipmentProfile.setModel(new DefaultComboBoxModel<>(
				equipmentProfiles.toArray(String[]::new)));
			Object profileName = settings.get(Settings.DEFAULT_EQUIPMENT_PROFILE);
			if (profileName != null)
			{
				defaultEquipmentProfile.setSelectedItem(profileName);
			}

			mashHopUtilisation.setQuantity(new PercentageUnit(
				Double.parseDouble(settings.get(Settings.MASH_HOP_UTILISATION))));
			firstWortHopUtilisation.setQuantity(new PercentageUnit(
				Double.parseDouble(settings.get(Settings.FIRST_WORT_HOP_UTILISATION))));
			leafHopAdjustment.setQuantity(new PercentageUnit(
				Double.parseDouble(settings.get(Settings.LEAF_HOP_ADJUSTMENT))));
			plugHopAdjustment.setQuantity(new PercentageUnit(
				Double.parseDouble(settings.get(Settings.PLUG_HOP_ADJUSTMENT))));
			pelletHopAdjustment.setQuantity(new PercentageUnit(
				Double.parseDouble(settings.get(Settings.PELLET_HOP_ADJUSTMENT))));
		}
		finally
		{
			this.refreshing = false;
		}
	}
}
