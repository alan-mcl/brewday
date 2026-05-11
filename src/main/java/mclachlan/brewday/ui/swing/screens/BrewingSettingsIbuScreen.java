package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.widgets.SwingCardStack;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.math.Quantity.Unit.PERCENTAGE;
import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing port of JFX {@code BrewingSettingsIbuPane}: hop bitterness formula, description,
 * and model-specific advanced settings; persists immediately via {@link Database#saveSettings()}.
 * Quantity listeners use each control's own value (JFX had copy-paste wiring bugs for
 * Tinseth BeerSmith and Garetz filter factor).
 */
public class BrewingSettingsIbuScreen extends JPanel implements SwingScreen
{
	private boolean refreshing;

	private final JComboBox<Settings.HopBitternessFormula> hopBitternessModel = new JComboBox<>();

	private final JTextArea hopModelDesc = new JTextArea();

	private final SwingCardStack settingsCards = new SwingCardStack();

	private final SwingQuantityEditWidget<PercentageUnit> tinsethMaxUtilFactor =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);

	private final SwingQuantityEditWidget<PercentageUnit> tinsethBSMaxUtilFactor =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);

	private final SwingQuantityEditWidget<PercentageUnit> garetzYeastFactor =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);

	private final SwingQuantityEditWidget<PercentageUnit> garetzPelletFactor =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);

	private final SwingQuantityEditWidget<PercentageUnit> garetzBagFactor =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);

	private final SwingQuantityEditWidget<PercentageUnit> garetzFilterFactor =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);

	public BrewingSettingsIbuScreen()
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
		form.add(new JLabel(getUiString("settings.hop.bitterness.formula")), gbc);

		hopBitternessModel.setModel(new DefaultComboBoxModel<>(Settings.HopBitternessFormula.values()));

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		form.add(hopBitternessModel, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		hopModelDesc.setEditable(false);
		hopModelDesc.setOpaque(false);
		hopModelDesc.setLineWrap(true);
		hopModelDesc.setWrapStyleWord(true);
		hopModelDesc.setColumns(52);
		hopModelDesc.setBorder(BorderFactory.createEmptyBorder());
		form.add(hopModelDesc, gbc);

		settingsCards.addCard(Settings.HopBitternessFormula.RAGER.name(), new JPanel());
		settingsCards.addCard(Settings.HopBitternessFormula.TINSETH_BEERSMITH.name(),
			buildSingleUtilPanel(tinsethBSMaxUtilFactor));
		settingsCards.addCard(Settings.HopBitternessFormula.TINSETH.name(),
			buildSingleUtilPanel(tinsethMaxUtilFactor));
		settingsCards.addCard(Settings.HopBitternessFormula.DANIELS.name(), new JPanel());
		settingsCards.addCard(Settings.HopBitternessFormula.GARETZ.name(), buildGaretzPanel());

		gbc.gridy++;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		form.add(settingsCards, gbc);

		add(form, BorderLayout.CENTER);
		refresh();
		wirePersistence();
	}

	private JPanel buildSingleUtilPanel(SwingQuantityEditWidget<PercentageUnit> utilWidget)
	{
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(4, 0, 4, 8);
		gbc.gridx = 0;
		gbc.gridy = 0;
		JLabel heading = new JLabel(getUiString("settings.advanced"));
		heading.setFont(heading.getFont().deriveFont(Font.BOLD));
		panel.add(heading, gbc);

		gbc.gridy++;
		gbc.gridwidth = 2;
		panel.add(new JLabel(getUiString("settings.dont.muck")), gbc);

		gbc.gridy++;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		panel.add(new JLabel(getUiString("settings.tinseth.max.utilisation")), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		panel.add(utilWidget, gbc);

		addTopAlignedGlueRow(panel, gbc);
		return panel;
	}

	private JPanel buildGaretzPanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(4, 0, 4, 8);
		gbc.gridx = 0;
		gbc.gridy = 0;
		JLabel heading = new JLabel(getUiString("settings.advanced"));
		heading.setFont(heading.getFont().deriveFont(Font.BOLD));
		panel.add(heading, gbc);

		gbc.gridy++;
		gbc.gridwidth = 2;
		panel.add(new JLabel(getUiString("settings.dont.muck")), gbc);

		gbc.gridy++;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		panel.add(new JLabel(getUiString("settings.garetz.yeast.factor")), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		panel.add(garetzYeastFactor, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		panel.add(new JLabel(getUiString("settings.garetz.pellet.factor")), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		panel.add(garetzPelletFactor, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		panel.add(new JLabel(getUiString("settings.garetz.bag.factor")), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		panel.add(garetzBagFactor, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		panel.add(new JLabel(getUiString("settings.garetz.filter.factor")), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		panel.add(garetzFilterFactor, gbc);

		addTopAlignedGlueRow(panel, gbc);
		return panel;
	}

	private static void addTopAlignedGlueRow(JPanel panel, GridBagConstraints gbc)
	{
		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.weighty = 1.0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		panel.add(Box.createVerticalGlue(), gbc);
	}

	private void wirePersistence()
	{
		hopBitternessModel.addActionListener(e ->
		{
			if (refreshing)
			{
				return;
			}
			Object sel = hopBitternessModel.getSelectedItem();
			if (!(sel instanceof Settings.HopBitternessFormula formula))
			{
				return;
			}
			String name = formula.name();
			Database.getInstance().getSettings().set(Settings.HOP_BITTERNESS_FORMULA, name);
			Database.getInstance().saveSettings();

			hopModelDesc.setText(getUiString("bitterness.model.desc." + name));
			settingsCards.setVisibleCard(name);
		});

		tinsethMaxUtilFactor.addQuantityChangeListener(q ->
			persistPercentSetting(q, Settings.TINSETH_MAX_UTILISATION));
		tinsethBSMaxUtilFactor.addQuantityChangeListener(q ->
			persistPercentSetting(q, Settings.TINSETH_MAX_UTILISATION));

		garetzYeastFactor.addQuantityChangeListener(q ->
			persistPercentSetting(q, Settings.GARETZ_YEAST_FACTOR));
		garetzPelletFactor.addQuantityChangeListener(q ->
			persistPercentSetting(q, Settings.GARETZ_PELLET_FACTOR));
		garetzBagFactor.addQuantityChangeListener(q ->
			persistPercentSetting(q, Settings.GARETZ_BAG_FACTOR));
		garetzFilterFactor.addQuantityChangeListener(q ->
			persistPercentSetting(q, Settings.GARETZ_FILTER_FACTOR));
	}

	private void persistPercentSetting(PercentageUnit q, String settingsKey)
	{
		if (refreshing || q == null)
		{
			return;
		}
		Database.getInstance().getSettings().set(settingsKey, String.valueOf(q.get(PERCENTAGE)));
		Database.getInstance().saveSettings();
	}

	@Override
	public void refresh()
	{
		this.refreshing = true;
		try
		{
			Settings settings = Database.getInstance().getSettings();

			Settings.HopBitternessFormula model = Settings.HopBitternessFormula.valueOf(
				settings.get(Settings.HOP_BITTERNESS_FORMULA));
			hopBitternessModel.setSelectedItem(model);

			String name = model.name();
			hopModelDesc.setText(getUiString("bitterness.model.desc." + name));

			double tinsethMaxUtil = Double.parseDouble(settings.get(Settings.TINSETH_MAX_UTILISATION));
			tinsethMaxUtilFactor.setQuantity(new PercentageUnit(tinsethMaxUtil));
			tinsethBSMaxUtilFactor.setQuantity(new PercentageUnit(tinsethMaxUtil));

			garetzYeastFactor.setQuantity(new PercentageUnit(
				Double.parseDouble(settings.get(Settings.GARETZ_YEAST_FACTOR))));
			garetzPelletFactor.setQuantity(new PercentageUnit(
				Double.parseDouble(settings.get(Settings.GARETZ_PELLET_FACTOR))));
			garetzBagFactor.setQuantity(new PercentageUnit(
				Double.parseDouble(settings.get(Settings.GARETZ_BAG_FACTOR))));
			garetzFilterFactor.setQuantity(new PercentageUnit(
				Double.parseDouble(settings.get(Settings.GARETZ_FILTER_FACTOR))));

			settingsCards.setVisibleCard(name);
		}
		finally
		{
			this.refreshing = false;
		}
	}
}
