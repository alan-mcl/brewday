package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.UiUnitPreferences;
import mclachlan.brewday.ui.swing.app.SwingScreen;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Display-unit preferences for the Swing UI. Cosmetic only; does not change persisted quantity storage.
 */
public class UiUnitSettingsScreen extends JPanel implements SwingScreen
{
	private boolean refreshing;

	private final EnumMap<UiUnitPreferences.Slot, JComboBox<Quantity.Unit>> slotCombos =
		new EnumMap<>(UiUnitPreferences.Slot.class);

	public UiUnitSettingsScreen()
	{
		super(new BorderLayout());

		JPanel column = new JPanel(new BorderLayout());
		JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(new EmptyBorder(10, 10, 10, 10));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.0;
		form.add(new JLabel(getUiString("settings.ui.units.title")), gbc);

		gbc.gridy++;
		JTextArea intro = new JTextArea(getUiString("settings.ui.units.intro"));
		intro.setEditable(false);
		intro.setOpaque(false);
		intro.setLineWrap(true);
		intro.setWrapStyleWord(true);
		intro.setColumns(48);
		intro.setBorder(new EmptyBorder(0, 0, 8, 0));
		form.add(intro, gbc);

		for (UiUnitPreferences.Slot slot : UiUnitPreferences.orderedSlots())
		{
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.weightx = 0.0;
			gbc.fill = GridBagConstraints.NONE;
			JLabel label = new JLabel(getUiString(slotLabelKey(slot)) + ":");
			label.setToolTipText(getUiString(slotTooltipKey(slot)));
			form.add(label, gbc);

			gbc.gridx = 1;
			gbc.weightx = 1.0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			JComboBox<Quantity.Unit> combo = unitCombo(slot);
			slotCombos.put(slot, combo);
			form.add(combo, gbc);
		}

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(12, 4, 4, 4);
		JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
		JButton metric = new JButton(getUiString("settings.ui.units.set.metric"));
		metric.setToolTipText(getUiString("settings.ui.units.set.metric.tooltip"));
		metric.addActionListener(e -> applyPreset(UiUnitPreferences.metric()));
		JButton imperial = new JButton(getUiString("settings.ui.units.set.imperial"));
		imperial.setToolTipText(getUiString("settings.ui.units.set.imperial.tooltip"));
		imperial.addActionListener(e -> applyPreset(UiUnitPreferences.imperial()));
		buttons.add(metric);
		buttons.add(imperial);
		form.add(buttons, gbc);

		column.add(form, BorderLayout.NORTH);
		add(new JScrollPane(column), BorderLayout.CENTER);

		for (JComboBox<Quantity.Unit> combo : slotCombos.values())
		{
			combo.addActionListener(e -> persistIfInteractive());
		}

		refresh();
	}

	private static JComboBox<Quantity.Unit> unitCombo(UiUnitPreferences.Slot slot)
	{
		List<Quantity.Unit> options = UiUnitPreferences.slotOptions(slot);
		JComboBox<Quantity.Unit> combo = new JComboBox<>(
			new DefaultComboBoxModel<>(options.toArray(new Quantity.Unit[0])));
		combo.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (c instanceof JLabel label && value instanceof Quantity.Unit u)
				{
					label.setText(u.toString());
				}
				return c;
			}
		});
		return combo;
	}

	private static String slotLabelKey(UiUnitPreferences.Slot slot)
	{
		return switch (slot)
		{
			case FERMENTABLE_WEIGHT -> "settings.ui.units.fermentable.weight";
			case HOP_MISC_WEIGHT -> "settings.ui.units.hop.misc.weight";
			case YEAST_WEIGHT -> "settings.ui.units.yeast.weight";
			case BATCH_VOLUME -> "settings.ui.units.batch.volume";
			case SMALL_VOLUME -> "settings.ui.units.small.volume";
			case TEMPERATURE -> "settings.ui.units.temperature";
			case DENSITY -> "settings.ui.units.density";
			case COLOUR -> "settings.ui.units.colour";
			case PRESSURE -> "settings.ui.units.pressure";
			case CARBONATION -> "settings.ui.units.carbonation";
			case LENGTH -> "settings.ui.units.length";
		};
	}

	private static String slotTooltipKey(UiUnitPreferences.Slot slot)
	{
		return slotLabelKey(slot) + ".tooltip";
	}

	private void applyPreset(UiUnitPreferences preset)
	{
		if (refreshing)
		{
			return;
		}
		preset.persist(Database.getInstance().getSettings());
		Database.getInstance().saveSettings();
		syncCombosFrom(UiUnitPreferences.from(Database.getInstance().getSettings()));
	}

	private void persistIfInteractive()
	{
		if (refreshing)
		{
			return;
		}
		UiUnitPreferences prefs = readCombos();
		prefs.persist(Database.getInstance().getSettings());
		Database.getInstance().saveSettings();
	}

	private UiUnitPreferences readCombos()
	{
		EnumMap<UiUnitPreferences.Slot, Quantity.Unit> map = new EnumMap<>(UiUnitPreferences.Slot.class);
		for (Map.Entry<UiUnitPreferences.Slot, JComboBox<Quantity.Unit>> e : slotCombos.entrySet())
		{
			Quantity.Unit selected = (Quantity.Unit)e.getValue().getSelectedItem();
			map.put(e.getKey(), selected);
		}
		return new UiUnitPreferences(map);
	}

	private void syncCombosFrom(UiUnitPreferences prefs)
	{
		for (UiUnitPreferences.Slot slot : UiUnitPreferences.orderedSlots())
		{
			JComboBox<Quantity.Unit> combo = slotCombos.get(slot);
			if (combo == null)
			{
				continue;
			}
			Quantity.Unit unit = prefs.get(slot);
			combo.setSelectedItem(unit);
			if (combo.getSelectedItem() == null && combo.getItemCount() > 0)
			{
				combo.setSelectedIndex(0);
			}
		}
	}

	@Override
	public void refresh()
	{
		refreshing = true;
		try
		{
			syncCombosFrom(UiUnitPreferences.from(Database.getInstance().getSettings()));
		}
		finally
		{
			refreshing = false;
		}
	}
}
