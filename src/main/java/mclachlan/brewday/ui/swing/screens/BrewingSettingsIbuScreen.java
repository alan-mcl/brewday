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
import mclachlan.brewday.Settings.HopBitternessFormula;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.widgets.SwingCardStack;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.math.Quantity.Unit.PERCENTAGE;
import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing port of JFX {@code BrewingSettingsIbuPane}: reported hop bitterness formulas,
 * description, and model-specific advanced settings; persists immediately via
 * {@link Database#saveSettings()}.
 */
public class BrewingSettingsIbuScreen extends JPanel implements SwingScreen
{
	private static final int CHECKBOX_HIT_WIDTH = 24;

	private boolean refreshing;

	private final Set<HopBitternessFormula> reported = EnumSet.noneOf(HopBitternessFormula.class);

	private final DefaultListModel<HopBitternessFormula> formulaListModel = new DefaultListModel<>();

	private final JList<HopBitternessFormula> formulaList = new JList<>(formulaListModel);

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
		setBorder(new EmptyBorder(10, 10, 10, 10));

		for (HopBitternessFormula formula : HopBitternessFormula.values())
		{
			formulaListModel.addElement(formula);
		}

		formulaList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		formulaList.setCellRenderer(new FormulaListCellRenderer());
		formulaList.setToolTipText(getUiString("settings.ibu.formula.checkbox.tooltip"));

		formulaList.addListSelectionListener(e ->
		{
			if (!e.getValueIsAdjusting() && !refreshing)
			{
				HopBitternessFormula sel = formulaList.getSelectedValue();
				if (sel != null)
				{
					showFormulaCard(sel);
				}
			}
		});

		formulaList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (refreshing)
				{
					return;
				}
				int index = formulaList.locationToIndex(e.getPoint());
				if (index < 0)
				{
					return;
				}
				Rectangle bounds = formulaList.getCellBounds(index, index);
				if (bounds != null && e.getX() < bounds.x + CHECKBOX_HIT_WIDTH)
				{
					HopBitternessFormula formula = formulaListModel.getElementAt(index);
					toggleReported(formula);
				}
			}
		});

		formulaList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleReported");
		formulaList.getActionMap().put("toggleReported", new javax.swing.AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				if (refreshing)
				{
					return;
				}
				HopBitternessFormula sel = formulaList.getSelectedValue();
				if (sel != null)
				{
					toggleReported(sel);
				}
			}
		});

		JScrollPane westScroll = new JScrollPane(formulaList);
		westScroll.setBorder(BorderFactory.createTitledBorder(
			getUiString("settings.hop.bitterness.models.caption")));

		settingsCards.addCard(HopBitternessFormula.RAGER.name(), new JPanel());
		settingsCards.addCard(HopBitternessFormula.TINSETH_BEERSMITH.name(),
			buildSingleUtilPanel(tinsethBSMaxUtilFactor));
		settingsCards.addCard(HopBitternessFormula.TINSETH.name(),
			buildSingleUtilPanel(tinsethMaxUtilFactor));
		settingsCards.addCard(HopBitternessFormula.DANIELS.name(), new JPanel());
		settingsCards.addCard(HopBitternessFormula.MIBU.name(), new JPanel());
		settingsCards.addCard(HopBitternessFormula.SMPH.name(), new JPanel());
		settingsCards.addCard(HopBitternessFormula.BREWDAY.name(), new JPanel());
		settingsCards.addCard(HopBitternessFormula.GARETZ.name(), buildGaretzPanel());

		hopModelDesc.setEditable(false);
		hopModelDesc.setOpaque(false);
		hopModelDesc.setLineWrap(true);
		hopModelDesc.setWrapStyleWord(true);
		hopModelDesc.setColumns(52);
		hopModelDesc.setBorder(BorderFactory.createEmptyBorder());
		hopModelDesc.setToolTipText(getUiString("ui.readonly.copy.tooltip"));

		JPanel east = new JPanel(new BorderLayout(4, 4));
		east.add(hopModelDesc, BorderLayout.NORTH);
		east.add(settingsCards, BorderLayout.CENTER);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, westScroll, east);
		split.setResizeWeight(0.28);
		split.setDividerLocation(304);

		JLabel orderHint = new JLabel(getUiString("settings.hop.bitterness.formulas.order.hint"));
		orderHint.setToolTipText(getUiString("settings.hop.bitterness.formulas.order.tooltip"));

		add(orderHint, BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);

		tinsethMaxUtilFactor.setToolTipText(getUiString("settings.tinseth.max.utilisation.tooltip"));
		tinsethBSMaxUtilFactor.setToolTipText(getUiString("settings.tinseth.max.utilisation.tooltip"));
		garetzYeastFactor.setToolTipText(getUiString("settings.garetz.yeast.factor.tooltip"));
		garetzPelletFactor.setToolTipText(getUiString("settings.garetz.pellet.factor.tooltip"));
		garetzBagFactor.setToolTipText(getUiString("settings.garetz.bag.factor.tooltip"));
		garetzFilterFactor.setToolTipText(getUiString("settings.garetz.filter.factor.tooltip"));

		refresh();
		wirePersistence();
	}

	/*-------------------------------------------------------------------------*/
	private final class FormulaListCellRenderer implements ListCellRenderer<HopBitternessFormula>
	{
		private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		private final JCheckBox check = new JCheckBox();
		private final JLabel label = new JLabel();

		FormulaListCellRenderer()
		{
			check.setEnabled(false);
			check.setFocusable(false);
			panel.setOpaque(true);
			panel.add(check);
			panel.add(label);
		}

		@Override
		public Component getListCellRendererComponent(
			JList<? extends HopBitternessFormula> list,
			HopBitternessFormula value,
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
	private void toggleReported(HopBitternessFormula formula)
	{
		if (reported.contains(formula))
		{
			reported.remove(formula);
		}
		else
		{
			reported.add(formula);
		}
		formulaList.repaint();
		persistReportedFormulas();
	}

	/*-------------------------------------------------------------------------*/
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

	/*-------------------------------------------------------------------------*/
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

	/*-------------------------------------------------------------------------*/
	private void persistReportedFormulas()
	{
		List<HopBitternessFormula> selected = getSelectedFormulasInOrder();
		if (selected.isEmpty())
		{
			reported.add(HopBitternessFormula.TINSETH);
			selected = List.of(HopBitternessFormula.TINSETH);
			formulaList.repaint();
		}
		Settings settings = Database.getInstance().getSettings();
		settings.set(
			Settings.HOP_BITTERNESS_FORMULAS,
			Settings.formatReportedFormulas(selected));
		Database.getInstance().saveSettings();
	}

	/*-------------------------------------------------------------------------*/
	private List<HopBitternessFormula> getSelectedFormulasInOrder()
	{
		List<HopBitternessFormula> selected = new ArrayList<>();
		for (HopBitternessFormula formula : HopBitternessFormula.values())
		{
			if (reported.contains(formula))
			{
				selected.add(formula);
			}
		}
		return selected;
	}

	/*-------------------------------------------------------------------------*/
	private void showFormulaCard(HopBitternessFormula formula)
	{
		String name = formula.name();
		hopModelDesc.setText(getUiString("bitterness.model.desc." + name));
		settingsCards.setVisibleCard(name);
	}

	/*-------------------------------------------------------------------------*/
	private void persistPercentSetting(PercentageUnit q, String settingsKey)
	{
		if (refreshing || q == null)
		{
			return;
		}
		Database.getInstance().getSettings().set(settingsKey, String.valueOf(q.get(PERCENTAGE)));
		Database.getInstance().saveSettings();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void refresh()
	{
		this.refreshing = true;
		try
		{
			Settings settings = Database.getInstance().getSettings();
			Settings.migrateLegacyHopBitternessSettings(settings.getSettings());

			reported.clear();
			reported.addAll(Settings.parseReportedFormulas(settings));

			formulaList.repaint();

			List<HopBitternessFormula> reportedList = Settings.parseReportedFormulas(settings);
			HopBitternessFormula cardFormula = reportedList.get(0);
			int index = formulaListModel.indexOf(cardFormula);
			if (index >= 0)
			{
				formulaList.setSelectedIndex(index);
			}
			showFormulaCard(cardFormula);

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
		}
		finally
		{
			this.refreshing = false;
		}
	}
}
