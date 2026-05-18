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
 * Swing port of JFX {@code BrewingSettingsMashPane}: mash pH model selection, description,
 * and MPH-specific advanced correction factor; persists immediately via
 * {@link Database#saveSettings()}.
 */
public class BrewingSettingsMashScreen extends JPanel implements SwingScreen
{
	private boolean refreshing;

	private final JComboBox<Settings.MashPhModel> mashPhModel = new JComboBox<>();

	private final JTextArea mashPhModelDesc = new JTextArea();

	private final SwingCardStack settingsCards = new SwingCardStack();

	private final SwingQuantityEditWidget<PercentageUnit> mphMaltCorrectionFactor =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);

	public BrewingSettingsMashScreen()
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
		form.add(new JLabel(getUiString("settings.mash.ph.model")), gbc);

		mashPhModel.setModel(new DefaultComboBoxModel<>(Settings.MashPhModel.values()));

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		mashPhModel.setToolTipText(getUiString("settings.mash.ph.model.tooltip"));
		form.add(mashPhModel, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		mashPhModelDesc.setEditable(false);
		mashPhModelDesc.setOpaque(false);
		mashPhModelDesc.setLineWrap(true);
		mashPhModelDesc.setWrapStyleWord(true);
		mashPhModelDesc.setColumns(52);
		mashPhModelDesc.setBorder(BorderFactory.createEmptyBorder());
		form.add(mashPhModelDesc, gbc);

		settingsCards.addCard(Settings.MashPhModel.MPH.name(), buildMphSettingsPanel());
		settingsCards.addCard(Settings.MashPhModel.EZ_WATER.name(), new JPanel());

		gbc.gridy++;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		form.add(settingsCards, gbc);

		mphMaltCorrectionFactor.setToolTipText(getUiString("settings.mph.malt.correction.tooltip"));
		mashPhModelDesc.setToolTipText(getUiString("ui.readonly.copy.tooltip"));

		add(form, BorderLayout.CENTER);
		refresh();
		wirePersistence();
	}

	private JPanel buildMphSettingsPanel()
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
		panel.add(new JLabel(getUiString("mash.ph.model.mph.malt.correction.factor")), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		panel.add(mphMaltCorrectionFactor, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.weighty = 1.0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		panel.add(Box.createVerticalGlue(), gbc);

		return panel;
	}

	private void wirePersistence()
	{
		mashPhModel.addActionListener(e ->
		{
			if (refreshing)
			{
				return;
			}
			Object sel = mashPhModel.getSelectedItem();
			if (!(sel instanceof Settings.MashPhModel model))
			{
				return;
			}
			String name = model.name();
			Database.getInstance().getSettings().set(Settings.MASH_PH_MODEL, name);
			Database.getInstance().saveSettings();

			mashPhModelDesc.setText(getUiString("mash.ph.model.desc." + name));
			settingsCards.setVisibleCard(name);
		});

		mphMaltCorrectionFactor.addQuantityChangeListener(q ->
		{
			if (refreshing || q == null)
			{
				return;
			}
			double v = q.get(PERCENTAGE);
			Database.getInstance().getSettings().set(Settings.MPH_MALT_BUFFERING_CORRECTION_FACTOR,
				String.valueOf(v));
			Database.getInstance().saveSettings();
		});
	}

	@Override
	public void refresh()
	{
		this.refreshing = true;
		try
		{
			Settings settings = Database.getInstance().getSettings();

			Settings.MashPhModel model = Settings.MashPhModel.valueOf(settings.get(Settings.MASH_PH_MODEL));
			mashPhModel.setSelectedItem(model);
			String name = model.name();
			mashPhModelDesc.setText(getUiString("mash.ph.model.desc." + name));
			settingsCards.setVisibleCard(name);

			mphMaltCorrectionFactor.setQuantity(new PercentageUnit(
				Double.parseDouble(settings.get(Settings.MPH_MALT_BUFFERING_CORRECTION_FACTOR))));
		}
		finally
		{
			this.refreshing = false;
		}
	}
}
