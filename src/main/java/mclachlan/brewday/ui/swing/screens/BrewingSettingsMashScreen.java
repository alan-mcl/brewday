/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import mclachlan.brewday.Settings;
import mclachlan.brewday.Settings.MashPhModel;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.widgets.SwingCardStack;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.math.Quantity.Unit.PERCENTAGE;
import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing port of JFX {@code BrewingSettingsMashPane}: reported mash pH models,
 * description, and MPH-specific advanced correction factor; persists immediately via
 * {@link Database#saveSettings()}.
 */
public class BrewingSettingsMashScreen extends JPanel implements SwingScreen
{
	private static final int CHECKBOX_HIT_WIDTH = 24;

	private boolean refreshing;

	private final Set<MashPhModel> reported = EnumSet.noneOf(MashPhModel.class);

	private final DefaultListModel<MashPhModel> modelListModel = new DefaultListModel<>();

	private final JList<MashPhModel> modelList = new JList<>(modelListModel);

	private final JTextArea mashPhModelDesc = new JTextArea();

	private final SwingCardStack settingsCards = new SwingCardStack();

	private final SwingQuantityEditWidget<PercentageUnit> mphMaltCorrectionFactor =
		new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE, false);

	public BrewingSettingsMashScreen()
	{
		super(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		for (MashPhModel model : MashPhModel.values())
		{
			modelListModel.addElement(model);
		}

		modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		modelList.setCellRenderer(new MashPhModelListCellRenderer());
		modelList.setToolTipText(getUiString("settings.mash.ph.model.checkbox.tooltip"));

		modelList.addListSelectionListener(e ->
		{
			if (!e.getValueIsAdjusting() && !refreshing)
			{
				MashPhModel sel = modelList.getSelectedValue();
				if (sel != null)
				{
					showModelCard(sel);
				}
			}
		});

		modelList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (refreshing)
				{
					return;
				}
				int index = modelList.locationToIndex(e.getPoint());
				if (index < 0)
				{
					return;
				}
				Rectangle bounds = modelList.getCellBounds(index, index);
				if (bounds != null && e.getX() < bounds.x + CHECKBOX_HIT_WIDTH)
				{
					MashPhModel model = modelListModel.getElementAt(index);
					toggleReported(model);
				}
			}
		});

		modelList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleReported");
		modelList.getActionMap().put("toggleReported", new javax.swing.AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				if (refreshing)
				{
					return;
				}
				MashPhModel sel = modelList.getSelectedValue();
				if (sel != null)
				{
					toggleReported(sel);
				}
			}
		});

		JScrollPane westScroll = new JScrollPane(modelList);
		westScroll.setBorder(BorderFactory.createTitledBorder(
			getUiString("settings.mash.ph.models.caption")));

		settingsCards.addCard(MashPhModel.MPH.name(), buildMphSettingsPanel());
		settingsCards.addCard(MashPhModel.EZ_WATER.name(), new JPanel());
		settingsCards.addCard(MashPhModel.KAISER_WATER.name(), new JPanel());

		mashPhModelDesc.setEditable(false);
		mashPhModelDesc.setOpaque(false);
		mashPhModelDesc.setLineWrap(true);
		mashPhModelDesc.setWrapStyleWord(true);
		mashPhModelDesc.setColumns(52);
		mashPhModelDesc.setBorder(BorderFactory.createEmptyBorder());
		mashPhModelDesc.setToolTipText(getUiString("ui.readonly.copy.tooltip"));

		JPanel east = new JPanel(new BorderLayout(4, 4));
		east.add(mashPhModelDesc, BorderLayout.NORTH);
		east.add(settingsCards, BorderLayout.CENTER);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, westScroll, east);
		split.setResizeWeight(0.28);
		split.setDividerLocation(304);

		JLabel orderHint = new JLabel(getUiString("settings.mash.ph.models.order.hint"));
		orderHint.setToolTipText(getUiString("settings.mash.ph.models.order.tooltip"));

		add(orderHint, BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);

		mphMaltCorrectionFactor.setToolTipText(getUiString("settings.mph.malt.correction.tooltip"));

		refresh();
		wirePersistence();
	}

	/*-------------------------------------------------------------------------*/
	private final class MashPhModelListCellRenderer implements ListCellRenderer<MashPhModel>
	{
		private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		private final JCheckBox check = new JCheckBox();
		private final JLabel label = new JLabel();

		MashPhModelListCellRenderer()
		{
			check.setEnabled(false);
			check.setFocusable(false);
			panel.setOpaque(true);
			panel.add(check);
			panel.add(label);
		}

		@Override
		public Component getListCellRendererComponent(
			JList<? extends MashPhModel> list,
			MashPhModel value,
			int index,
			boolean isSelected,
			boolean cellHasFocus)
		{
			if (value == null)
			{
				return new DefaultListCellRenderer();
			}
			check.setSelected(reported.contains(value));
			label.setText(value.toString());

			panel.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
			panel.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
			check.setBackground(panel.getBackground());
			check.setForeground(panel.getForeground());
			label.setBackground(panel.getBackground());
			label.setForeground(panel.getForeground());

			return panel;
		}
	}

	/*-------------------------------------------------------------------------*/
	private void toggleReported(MashPhModel model)
	{
		if (reported.contains(model))
		{
			reported.remove(model);
		}
		else
		{
			reported.add(model);
		}
		modelList.repaint();
		persistReportedModels();
	}

	/*-------------------------------------------------------------------------*/
	private void showModelCard(MashPhModel model)
	{
		String name = model.name();
		mashPhModelDesc.setText(getUiString("mash.ph.model.desc." + name));
		settingsCards.setVisibleCard(name);
	}

	/*-------------------------------------------------------------------------*/
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

		addTopAlignedGlueRow(panel, gbc);
		return panel;
	}

	/*-------------------------------------------------------------------------*/
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

	/*-------------------------------------------------------------------------*/
	private void wirePersistence()
	{
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

	/*-------------------------------------------------------------------------*/
	private void persistReportedModels()
	{
		List<MashPhModel> selected = getSelectedModelsInOrder();
		if (selected.isEmpty())
		{
			reported.add(MashPhModel.MPH);
			selected = List.of(MashPhModel.MPH);
			modelList.repaint();
		}
		Settings settings = Database.getInstance().getSettings();
		settings.set(
			Settings.MASH_PH_MODELS,
			Settings.formatReportedModels(selected));
		Database.getInstance().saveSettings();
	}

	/*-------------------------------------------------------------------------*/
	private List<MashPhModel> getSelectedModelsInOrder()
	{
		List<MashPhModel> selected = new ArrayList<>();
		for (MashPhModel model : MashPhModel.values())
		{
			if (reported.contains(model))
			{
				selected.add(model);
			}
		}
		return selected;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void refresh()
	{
		this.refreshing = true;
		try
		{
			Settings settings = Database.getInstance().getSettings();
			Settings.migrateLegacyMashPhSettings(settings.getSettings());

			reported.clear();
			reported.addAll(Settings.parseReportedModels(settings));

			modelList.repaint();

			List<MashPhModel> reportedList = Settings.parseReportedModels(settings);
			MashPhModel cardModel = reportedList.get(0);
			int index = modelListModel.indexOf(cardModel);
			if (index >= 0)
			{
				modelList.setSelectedIndex(index);
			}
			showModelCard(cardModel);

			mphMaltCorrectionFactor.setQuantity(new PercentageUnit(
				Double.parseDouble(settings.get(Settings.MPH_MALT_BUFFERING_CORRECTION_FACTOR))));
		}
		finally
		{
			this.refreshing = false;
		}
	}
}
