package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.app.SwingThemeSupport;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing-only look-and-feel chooser: {@link Settings#SWING_LOOK_AND_FEEL} ({@code swing.laf}), not
 * JavaFX {@link Settings#UI_THEME}.
 */
public class UiSettingsScreen extends JPanel implements SwingScreen
{
	private static final String[] ORDERED_LAF_TOKENS =
		{
			Settings.SWING_LAF_FLAT_LIGHT,
			Settings.SWING_LAF_FLAT_DARK,
			Settings.SWING_LAF_FLAT_DARCULA,
			Settings.SWING_LAF_FLAT_INTELLIJ,
			Settings.SWING_LAF_NIMBUS,
			Settings.SWING_LAF_METAL,
			Settings.SWING_LAF_SYSTEM,
		};

	private boolean refreshing;

	private final JComboBox<LafChoice> lafCombo = new JComboBox<>();

	public UiSettingsScreen()
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
		form.add(new JLabel(getUiString("settings.swing.appearance")), gbc);

		gbc.gridy++;
		JTextArea intro = new JTextArea(getUiString("settings.swing.appearance.intro"));
		intro.setEditable(false);
		intro.setOpaque(false);
		intro.setLineWrap(true);
		intro.setWrapStyleWord(true);
		intro.setColumns(48);
		intro.setBorder(new EmptyBorder(0, 0, 8, 0));
		form.add(intro, gbc);

		gbc.gridy++;
		form.add(new JLabel(getUiString("settings.swing.appearance.lookAndFeel")), gbc);

		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		for (String token : ORDERED_LAF_TOKENS)
		{
			lafCombo.addItem(new LafChoice(token, getUiString(uiKeyForToken(token))));
		}
		form.add(lafCombo, gbc);

		gbc.gridy++;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		form.add(new JLabel(getUiString("setting.swing.laf.applies.live")), gbc);

		column.add(form, BorderLayout.NORTH);
		add(column, BorderLayout.WEST);

		lafCombo.addActionListener(e -> persistComboIfInteractive());

		refresh();
	}

	private static String uiKeyForToken(String token)
	{
		switch (token)
		{
			case Settings.SWING_LAF_FLAT_LIGHT:
				return "setting.swing.laf.flat.light";
			case Settings.SWING_LAF_FLAT_DARK:
				return "setting.swing.laf.flat.dark";
			case Settings.SWING_LAF_FLAT_DARCULA:
				return "setting.swing.laf.flat.darcula";
			case Settings.SWING_LAF_FLAT_INTELLIJ:
				return "setting.swing.laf.flat.intellij";
			case Settings.SWING_LAF_NIMBUS:
				return "setting.swing.laf.nimbus";
			case Settings.SWING_LAF_METAL:
				return "setting.swing.laf.metal";
			case Settings.SWING_LAF_SYSTEM:
				return "setting.swing.laf.system";
			default:
				return "setting.swing.laf.flat.light";
		}
	}

	private void persistComboIfInteractive()
	{
		if (refreshing)
		{
			return;
		}
		LafChoice ch = (LafChoice)lafCombo.getSelectedItem();
		if (ch == null)
		{
			return;
		}
		Database.getInstance().getSettings().set(Settings.SWING_LOOK_AND_FEEL, ch.token);
		Database.getInstance().saveSettings();
		SwingThemeSupport.applySwingLafLive(ch.token);
	}

	private void syncComboSelection(String normalizedToken)
	{
		for (int i = 0; i < lafCombo.getItemCount(); i++)
		{
			LafChoice c = lafCombo.getItemAt(i);
			if (c != null && normalizedToken.equals(c.token))
			{
				lafCombo.setSelectedIndex(i);
				return;
			}
		}
		lafCombo.setSelectedIndex(0);
	}

	@Override
	public void refresh()
	{
		refreshing = true;
		try
		{
			Settings settings = Database.getInstance().getSettings();
			String raw = settings.get(Settings.SWING_LOOK_AND_FEEL);
			String normalized = SwingThemeSupport.normalizeSwingLafKey(raw);
			if (raw == null || !raw.equals(normalized))
			{
				settings.set(Settings.SWING_LOOK_AND_FEEL, normalized);
				Database.getInstance().saveSettings();
			}
			syncComboSelection(normalized);
		}
		finally
		{
			refreshing = false;
		}
	}

	private static final class LafChoice
	{
		private final String token;
		private final String label;

		private LafChoice(String token, String label)
		{
			this.token = token;
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}
}
