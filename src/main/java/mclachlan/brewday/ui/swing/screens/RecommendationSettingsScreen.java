package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recommend.RecommendationSettings;
import mclachlan.brewday.ui.swing.app.SwingScreen;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Threshold controls for What Should I Brew? recommendation groups.
 */
public class RecommendationSettingsScreen extends JPanel implements SwingScreen
{
	private boolean refreshing;

	private final JSpinner minGroupSize = intSpinner(1, 3, 1);
	private final JSpinner maxPerGroup = intSpinner(1, 3, 1);
	private final JComboBox<HemisphereChoice> hemisphere = new JComboBox<>();
	private final JSpinner seasonalLeadMonths = intSpinner(0, 6, 1);
	private final JSpinner bestInventoryMinMatch = intSpinner(0, 100, 5);
	private final JSpinner dueRepeatGapMonths = intSpinner(1, 120, 1);
	private final JSpinner styleRevisitGapMonths = intSpinner(1, 120, 1);
	private final JSpinner somethingDifferentMinContrast = doubleSpinner(0D, 10D, 0.1D);
	private final JSpinner neverBrewedMinMatch = intSpinner(0, 100, 5);
	private final JSpinner forgottenGapMonths = intSpinner(1, 120, 1);
	private final JSpinner useItUpMinMatch = intSpinner(0, 100, 5);
	private final JSpinner onePurchaseMinMatch = intSpinner(0, 99, 5);
	private final JSpinner stretchMinContrast = doubleSpinner(0D, 10D, 0.1D);
	private final JSpinner stretchMinMatch = intSpinner(0, 100, 5);

	private final JButton restoreDefaults = new JButton(getUiString("settings.recommend.restore.defaults"));

	public RecommendationSettingsScreen()
	{
		super(new BorderLayout());

		for (RecommendationSettings.Hemisphere h : RecommendationSettings.Hemisphere.values())
		{
			hemisphere.addItem(new HemisphereChoice(h));
		}

		JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(new EmptyBorder(10, 10, 10, 10));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.0;
		form.add(new JLabel(getUiString("settings.recommend.title")), gbc);

		gbc.gridy++;
		JTextArea intro = new JTextArea(getUiString("settings.recommend.intro"));
		intro.setEditable(false);
		intro.setOpaque(false);
		intro.setLineWrap(true);
		intro.setWrapStyleWord(true);
		intro.setColumns(48);
		intro.setBorder(new EmptyBorder(0, 0, 8, 0));
		form.add(intro, gbc);

		addRow(form, gbc, getUiString("settings.recommend.min.group.size"), minGroupSize,
			"settings.recommend.min.group.size.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.max.group.size"), maxPerGroup,
			"settings.recommend.max.group.size.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.hemisphere"), hemisphere,
			"settings.recommend.hemisphere.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.seasonal.lead.months"), seasonalLeadMonths,
			"settings.recommend.seasonal.lead.months.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.best.inventory.min.match"), bestInventoryMinMatch,
			"settings.recommend.best.inventory.min.match.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.due.repeat.gap.months"), dueRepeatGapMonths,
			"settings.recommend.due.repeat.gap.months.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.style.revisit.gap.months"), styleRevisitGapMonths,
			"settings.recommend.style.revisit.gap.months.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.something.different.min.contrast"),
			somethingDifferentMinContrast, "settings.recommend.something.different.min.contrast.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.never.brewed.min.match"), neverBrewedMinMatch,
			"settings.recommend.never.brewed.min.match.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.forgotten.gap.months"), forgottenGapMonths,
			"settings.recommend.forgotten.gap.months.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.use.it.up.min.match"), useItUpMinMatch,
			"settings.recommend.use.it.up.min.match.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.one.purchase.min.match"), onePurchaseMinMatch,
			"settings.recommend.one.purchase.min.match.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.stretch.min.contrast"), stretchMinContrast,
			"settings.recommend.stretch.min.contrast.tooltip");
		addRow(form, gbc, getUiString("settings.recommend.stretch.min.match"), stretchMinMatch,
			"settings.recommend.stretch.min.match.tooltip");

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		gbc.weighty = 1.0;
		restoreDefaults.setToolTipText(getUiString("settings.recommend.restore.defaults.tooltip"));
		form.add(restoreDefaults, gbc);

		JScrollPane scroll = new JScrollPane(form);
		scroll.setBorder(null);
		add(scroll, BorderLayout.NORTH);

		wirePersistence();
		refresh();
	}

	private static JSpinner intSpinner(int min, int max, int step)
	{
		return new JSpinner(new SpinnerNumberModel(min, min, max, step));
	}

	private static JSpinner doubleSpinner(double min, double max, double step)
	{
		return new JSpinner(new SpinnerNumberModel(min, min, max, step));
	}

	private static void addRow(JPanel form, GridBagConstraints gbc, String labelText, JSpinner spinner, String tooltipKey)
	{
		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		form.add(new JLabel(labelText), gbc);
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		spinner.setToolTipText(getUiString(tooltipKey));
		form.add(spinner, gbc);
	}

	private static void addRow(JPanel form, GridBagConstraints gbc, String labelText, JComboBox<?> combo, String tooltipKey)
	{
		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		form.add(new JLabel(labelText), gbc);
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		combo.setToolTipText(getUiString(tooltipKey));
		form.add(combo, gbc);
	}

	private void wirePersistence()
	{
		minGroupSize.addChangeListener(e -> persistIfInteractive());
		maxPerGroup.addChangeListener(e -> persistIfInteractive());
		hemisphere.addActionListener(e -> persistIfInteractive());
		seasonalLeadMonths.addChangeListener(e -> persistIfInteractive());
		bestInventoryMinMatch.addChangeListener(e -> persistIfInteractive());
		dueRepeatGapMonths.addChangeListener(e -> persistIfInteractive());
		styleRevisitGapMonths.addChangeListener(e -> persistIfInteractive());
		somethingDifferentMinContrast.addChangeListener(e -> persistIfInteractive());
		neverBrewedMinMatch.addChangeListener(e -> persistIfInteractive());
		forgottenGapMonths.addChangeListener(e -> persistIfInteractive());
		useItUpMinMatch.addChangeListener(e -> persistIfInteractive());
		onePurchaseMinMatch.addChangeListener(e -> persistIfInteractive());
		stretchMinContrast.addChangeListener(e -> persistIfInteractive());
		stretchMinMatch.addChangeListener(e -> persistIfInteractive());

		restoreDefaults.addActionListener(e ->
		{
			if (refreshing)
			{
				return;
			}
			RecommendationSettings.clearPersisted(Database.getInstance().getSettings());
			Database.getInstance().saveSettings();
			refresh();
		});
	}

	private void persistIfInteractive()
	{
		if (refreshing)
		{
			return;
		}
		HemisphereChoice choice = (HemisphereChoice)hemisphere.getSelectedItem();
		int min = ((Number)minGroupSize.getValue()).intValue();
		int max = ((Number)maxPerGroup.getValue()).intValue();
		if (min > max)
		{
			min = max;
			minGroupSize.setValue(min);
		}
		RecommendationSettings settings = new RecommendationSettings(
			min,
			max,
			choice == null ? RecommendationSettings.Hemisphere.NORTHERN : choice.hemisphere(),
			((Number)seasonalLeadMonths.getValue()).intValue(),
			((Number)bestInventoryMinMatch.getValue()).intValue(),
			((Number)dueRepeatGapMonths.getValue()).longValue(),
			((Number)styleRevisitGapMonths.getValue()).longValue(),
			((Number)somethingDifferentMinContrast.getValue()).doubleValue(),
			((Number)neverBrewedMinMatch.getValue()).intValue(),
			((Number)forgottenGapMonths.getValue()).longValue(),
			((Number)useItUpMinMatch.getValue()).intValue(),
			((Number)onePurchaseMinMatch.getValue()).intValue(),
			((Number)stretchMinContrast.getValue()).doubleValue(),
			((Number)stretchMinMatch.getValue()).intValue());
		settings.persist(Database.getInstance().getSettings());
		Database.getInstance().saveSettings();
	}

	@Override
	public void refresh()
	{
		refreshing = true;
		try
		{
			RecommendationSettings settings = RecommendationSettings.from(Database.getInstance().getSettings());
			minGroupSize.setValue(settings.getMinGroupSize());
			maxPerGroup.setValue(settings.getMaxPerGroup());
			selectHemisphere(settings.getHemisphere());
			seasonalLeadMonths.setValue(settings.getSeasonalLeadMonths());
			bestInventoryMinMatch.setValue(settings.getBestInventoryMinMatch());
			dueRepeatGapMonths.setValue((int)settings.getDueRepeatGapMonths());
			styleRevisitGapMonths.setValue((int)settings.getStyleRevisitGapMonths());
			somethingDifferentMinContrast.setValue(settings.getSomethingDifferentMinContrast());
			neverBrewedMinMatch.setValue(settings.getNeverBrewedMinMatch());
			forgottenGapMonths.setValue((int)settings.getForgottenGapMonths());
			useItUpMinMatch.setValue(settings.getUseItUpMinMatch());
			onePurchaseMinMatch.setValue(settings.getOnePurchaseMinMatch());
			stretchMinContrast.setValue(settings.getStretchMinContrast());
			stretchMinMatch.setValue(settings.getStretchMinMatch());
		}
		finally
		{
			refreshing = false;
		}
	}

	private void selectHemisphere(RecommendationSettings.Hemisphere target)
	{
		for (int i = 0; i < hemisphere.getItemCount(); i++)
		{
			HemisphereChoice c = hemisphere.getItemAt(i);
			if (c != null && c.hemisphere() == target)
			{
				hemisphere.setSelectedIndex(i);
				return;
			}
		}
	}

	private record HemisphereChoice(RecommendationSettings.Hemisphere hemisphere)
	{
		@Override
		public String toString()
		{
			return switch (hemisphere)
			{
				case NORTHERN -> getUiString("settings.recommend.hemisphere.northern");
				case SOUTHERN -> getUiString("settings.recommend.hemisphere.southern");
			};
		}
	}
}
