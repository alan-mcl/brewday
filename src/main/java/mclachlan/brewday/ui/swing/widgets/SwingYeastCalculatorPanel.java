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

package mclachlan.brewday.ui.swing.widgets;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.YeastCalculator;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.YeastSourceType;
import mclachlan.brewday.ui.swing.UiUnitDisplaySupport;
import mclachlan.brewday.util.StringUtils;
import org.jdatepicker.JDatePicker;
import org.jdatepicker.LocalDateModel;

import static mclachlan.brewday.math.Quantity.Unit.*;
import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Tools &gt; Yeast Calculator panel.
 */
public class SwingYeastCalculatorPanel extends JPanel
{
	private static final double BILLION = 1_000_000_000D;
	private static final int FIELD_WIDTH = 120;

	private static final String[] ASSUMPTION_KEYS = new String[]
	{
		"tools.yeast.calculator.assumptions.1",
		"tools.yeast.calculator.assumptions.2",
		"tools.yeast.calculator.assumptions.3",
		"tools.yeast.calculator.assumptions.4",
		"tools.yeast.calculator.assumptions.5",
		"tools.yeast.calculator.assumptions.6"
	};

	private static final String CELLS_CARD_NONE = "none";
	private static final String CELLS_CARD_MANUAL = "manual";
	private static final String CELLS_CARD_SLURRY = "slurry";

	private static final String VIAB_CARD_NONE = "none";
	private static final String VIAB_CARD_MANUAL = "manual";
	private static final String VIAB_CARD_AGE = "age";

	private final JComboBox<Yeast> yeastCombo;
	private final JComboBox<YeastSourceType> sourceType;
	private final SwingQuantitySelectAndEditWidget pitchQuantity;

	private final JComboBox<YeastCalculator.CellCountMode> cellModeCombo;
	private final CardLayout cellsCardLayout;
	private final JPanel cellsConditionalPanel;
	private final JTextField manualCellsBillions;
	private final JTextField slurryCellsPerMlBillions;

	private final JComboBox<YeastCalculator.ViabilityMode> viabilityModeCombo;
	private final CardLayout viabilityCardLayout;
	private final JPanel viabilityConditionalPanel;
	private final SwingQuantityEditWidget<PercentageUnit> manualViability;
	private final LocalDateModel productionDateModel;
	private final JDatePicker productionDate;
	private final LocalDateModel pitchDateModel;
	private final JDatePicker pitchDate;
	private final JComboBox<YeastCalculator.StorageTemperature> storageTemp;

	private final SwingQuantityEditWidget<VolumeUnit> wortVolume;
	private final SwingQuantityEditWidget<DensityUnit> originalGravity;
	private final SwingQuantityEditWidget<TemperatureUnit> fermentationTemp;

	private final JLabel totalCellsLabel;
	private final JLabel effectiveCellsLabel;
	private final JLabel requiredCellsLabel;
	private final JLabel pitchRatioLabel;
	private final JLabel pitchRateLabel;
	private final JLabel recommendedPitchLabel;
	private final JLabel warningsLabel;
	private final JLabel errorLabel;

	private final Font pitchRatioFont;

	public SwingYeastCalculatorPanel()
	{
		super(new GridBagLayout());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		Settings settings = Database.getInstance().getSettings();
		Quantity.Unit densityUnit = settings.getUnitForStepAndIngredient(
			Quantity.Type.FLUID_DENSITY,
			ProcessStep.Type.FERMENT,
			IngredientAddition.Type.YEAST);
		Quantity.Unit tempUnit = settings.getUnitForStepAndIngredient(
			Quantity.Type.TEMPERATURE,
			ProcessStep.Type.FERMENT,
			IngredientAddition.Type.YEAST);

		yeastCombo = new JComboBox<>();
		configureComboRenderer(yeastCombo, y -> y == null ? "" : y.getName());
		loadYeasts();

		sourceType = new JComboBox<>(YeastSourceType.values());
		configureComboRenderer(sourceType, t -> t == null ? "" : sourceTypeLabel(t));

		pitchQuantity = new SwingQuantitySelectAndEditWidget(
			settings.getUnitForStepAndIngredient(
				Quantity.Type.WEIGHT, ProcessStep.Type.FERMENT, IngredientAddition.Type.YEAST),
			Quantity.Type.WEIGHT, Quantity.Type.VOLUME);

		cellModeCombo = new JComboBox<>(YeastCalculator.CellCountMode.values());
		configureComboRenderer(cellModeCombo, m -> m == null ? "" : cellModeLabel(m));
		cellModeCombo.setSelectedItem(YeastCalculator.CellCountMode.ESTIMATE_FROM_QUANTITY);

		manualCellsBillions = new JTextField("220", 8);
		slurryCellsPerMlBillions = new JTextField("1.0", 8);
		cellsCardLayout = new CardLayout();
		cellsConditionalPanel = new JPanel(cellsCardLayout);
		cellsConditionalPanel.add(new JPanel(), CELLS_CARD_NONE);
		cellsConditionalPanel.add(
			buildLabeledFieldRow(
				getUiString("tools.yeast.calculator.cells.manual.billions"),
				manualCellsBillions),
			CELLS_CARD_MANUAL);
		cellsConditionalPanel.add(
			buildLabeledFieldRow(
				getUiString("tools.yeast.calculator.cells.slurry.density"),
				slurryCellsPerMlBillions),
			CELLS_CARD_SLURRY);
		cellsCardLayout.show(cellsConditionalPanel, CELLS_CARD_NONE);

		viabilityModeCombo = new JComboBox<>(YeastCalculator.ViabilityMode.values());
		configureComboRenderer(viabilityModeCombo, m -> m == null ? "" : viabilityModeLabel(m));
		viabilityModeCombo.setSelectedItem(YeastCalculator.ViabilityMode.DEFAULT_BY_SOURCE);

		manualViability = new SwingQuantityEditWidget<>(PERCENTAGE_DISPLAY);
		manualViability.setQuantity(new PercentageUnit(0.96D));

		productionDateModel = new LocalDateModel(LocalDate.now().minusMonths(1));
		productionDate = new JDatePicker(productionDateModel);
		productionDate.setTextfieldColumns(10);

		pitchDateModel = new LocalDateModel(LocalDate.now());
		pitchDate = new JDatePicker(pitchDateModel);
		pitchDate.setTextfieldColumns(10);

		storageTemp = new JComboBox<>(YeastCalculator.StorageTemperature.values());
		configureComboRenderer(storageTemp, t -> t == null ? "" : storageTempLabel(t));
		storageTemp.setSelectedItem(YeastCalculator.StorageTemperature.FRIDGE_10C);

		viabilityCardLayout = new CardLayout();
		viabilityConditionalPanel = new JPanel(viabilityCardLayout);
		viabilityConditionalPanel.add(new JPanel(), VIAB_CARD_NONE);
		viabilityConditionalPanel.add(buildViabilityManualPanel(), VIAB_CARD_MANUAL);
		viabilityConditionalPanel.add(buildViabilityAgePanel(), VIAB_CARD_AGE);
		viabilityCardLayout.show(viabilityConditionalPanel, VIAB_CARD_NONE);

		wortVolume = new SwingQuantityEditWidget<>(UiUnitDisplaySupport.batchVolume());
		wortVolume.setQuantity(new VolumeUnit(20D, UiUnitDisplaySupport.batchVolume()));

		originalGravity = new SwingQuantityEditWidget<>(densityUnit);
		originalGravity.setQuantity(defaultOriginalGravity(densityUnit));

		fermentationTemp = new SwingQuantityEditWidget<>(tempUnit);
		fermentationTemp.setQuantity(new TemperatureUnit(20D, tempUnit));

		totalCellsLabel = new JLabel(" ");
		effectiveCellsLabel = new JLabel(" ");
		requiredCellsLabel = new JLabel(" ");
		pitchRatioLabel = new JLabel(" ");
		pitchRateLabel = new JLabel(" ");
		recommendedPitchLabel = new JLabel(" ");
		warningsLabel = new JLabel("<html></html>");
		errorLabel = new JLabel(" ");
		errorLabel.setForeground(Color.RED);

		pitchRatioFont = pitchRatioLabel.getFont()
			.deriveFont(Font.BOLD, pitchRatioLabel.getFont().getSize2D() + 2f);

		constrainFieldWidth(pitchQuantity, FIELD_WIDTH);
		constrainFieldWidth(manualViability, FIELD_WIDTH);
		constrainFieldWidth(wortVolume, FIELD_WIDTH);
		constrainFieldWidth(originalGravity, FIELD_WIDTH);
		constrainFieldWidth(fermentationTemp, FIELD_WIDTH);
		constrainFieldWidth(yeastCombo, FIELD_WIDTH + 80);
		constrainFieldWidth(sourceType, FIELD_WIDTH + 40);
		constrainFieldWidth(cellModeCombo, FIELD_WIDTH + 80);
		constrainFieldWidth(viabilityModeCombo, FIELD_WIDTH + 80);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(0, 4, 6, 4);
		add(buildAttributionLabel(), gbc);

		gbc.gridy = 1;
		add(buildWortBar(), gbc);

		gbc.gridy = 2;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.BOTH;
		add(buildBodyPanel(), gbc);

		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.weighty = 0;
		add(errorLabel, gbc);

		gbc.gridy = 4;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(buildAssumptionsPanel(), gbc);

		setAlignmentX(Component.LEFT_ALIGNMENT);
		setAlignmentY(Component.TOP_ALIGNMENT);

		registerRecalcListeners();
		onYeastChanged();
		updateCellMode();
		updateViabilityMode();
		recalculate();
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildWortBar()
	{
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
		bar.setBorder(BorderFactory.createTitledBorder(
			getUiString("tools.yeast.calculator.section.wort")));
		bar.add(labeledQuantity(
			getUiString("tools.yeast.calculator.wort.volume"),
			wortVolume));
		bar.add(labeledQuantity(
			getUiString("tools.yeast.calculator.original.gravity"),
			originalGravity));
		bar.add(labeledQuantity(
			getUiString("tools.yeast.calculator.fermentation.temp"),
			fermentationTemp));
		return bar;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildBodyPanel()
	{
		JPanel body = new JPanel(new GridBagLayout());
		GridBagConstraints left = new GridBagConstraints();
		left.gridx = 0;
		left.gridy = 0;
		left.weightx = 0.5;
		left.weighty = 0;
		left.fill = GridBagConstraints.BOTH;
		left.anchor = GridBagConstraints.NORTHWEST;
		left.insets = new Insets(0, 0, 0, 4);
		body.add(buildInputsColumn(), left);

		GridBagConstraints right = new GridBagConstraints();
		right.gridx = 1;
		right.gridy = 0;
		right.weightx = 0.5;
		right.weighty = 0;
		right.fill = GridBagConstraints.BOTH;
		right.anchor = GridBagConstraints.NORTHWEST;
		right.insets = new Insets(0, 4, 0, 0);
		body.add(buildResultsCard(), right);
		return body;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildInputsColumn()
	{
		JPanel column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
		column.setAlignmentX(Component.LEFT_ALIGNMENT);

		addFullWidthCard(column, buildPitchCard());
		column.add(Box.createVerticalStrut(6));
		addFullWidthCard(column, buildCellsCard());
		column.add(Box.createVerticalStrut(6));
		addFullWidthCard(column, buildViabilityCard());
		return column;
	}

	/*-------------------------------------------------------------------------*/
	private static void addFullWidthCard(JPanel column, JPanel card)
	{
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		int h = card.getPreferredSize().height;
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
		column.add(card);
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildPitchCard()
	{
		JPanel card = new JPanel(new GridBagLayout());
		card.setBorder(BorderFactory.createTitledBorder(
			getUiString("tools.yeast.calculator.section.pitch")));
		int row = 0;
		row = addCardRow(card, row, getUiString("tools.yeast.calculator.yeast"), yeastCombo);
		row = addCardRow(card, row, getUiString("tools.yeast.calculator.source"), sourceType);
		addCardRow(card, row, getUiString("tools.yeast.calculator.pitch.amount"), pitchQuantity);
		return card;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildCellsCard()
	{
		JPanel card = new JPanel(new GridBagLayout());
		card.setBorder(BorderFactory.createTitledBorder(
			getUiString("tools.yeast.calculator.section.cells")));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(2, 6, 4, 6);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		card.add(cellModeCombo, gbc);

		gbc.gridy = 1;
		gbc.insets = new Insets(0, 6, 4, 6);
		card.add(cellsConditionalPanel, gbc);
		return card;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildViabilityCard()
	{
		JPanel card = new JPanel(new GridBagLayout());
		card.setBorder(BorderFactory.createTitledBorder(
			getUiString("tools.yeast.calculator.section.viability")));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(2, 6, 4, 6);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		card.add(viabilityModeCombo, gbc);

		gbc.gridy = 1;
		gbc.insets = new Insets(0, 6, 4, 6);
		card.add(viabilityConditionalPanel, gbc);
		return card;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildViabilityManualPanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		addCardRow(panel, 0,
			getUiString("tools.yeast.calculator.viability.percent"),
			manualViability);
		return panel;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildViabilityAgePanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		row = addCardRow(panel, row,
			getUiString("tools.yeast.calculator.production.date"),
			productionDate);
		row = addCardRow(panel, row,
			getUiString("tools.yeast.calculator.pitch.date"),
			pitchDate);
		addCardRow(panel, row,
			getUiString("tools.yeast.calculator.storage.temp"),
			storageTemp);
		return panel;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildResultsCard()
	{
		JPanel card = new JPanel(new GridBagLayout());
		card.setBorder(BorderFactory.createTitledBorder(
			getUiString("tools.yeast.calculator.section.results")));
		int row = 0;
		row = addCardRow(card, row,
			getUiString("tools.yeast.calculator.result.total.cells"),
			totalCellsLabel);
		row = addCardRow(card, row,
			getUiString("tools.yeast.calculator.result.effective.cells"),
			effectiveCellsLabel);
		row = addCardRow(card, row,
			getUiString("tools.yeast.calculator.result.required.cells"),
			requiredCellsLabel);
		row = addCardRow(card, row,
			getUiString("tools.yeast.calculator.result.pitch.ratio"),
			pitchRatioLabel);
		row = addCardRow(card, row,
			getUiString("tools.yeast.calculator.result.pitch.rate"),
			pitchRateLabel);
		row = addCardRow(card, row,
			getUiString("tools.yeast.calculator.result.recommended"),
			recommendedPitchLabel);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(4, 6, 2, 6);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		card.add(new JLabel(getUiString("tools.yeast.calculator.result.warnings") + ":"), gbc);

		gbc.gridy = row + 1;
		gbc.insets = new Insets(0, 6, 4, 6);
		card.add(warningsLabel, gbc);
		return card;
	}

	/*-------------------------------------------------------------------------*/
	private static int addCardRow(JPanel card, int row, String label, Component field)
	{
		GridBagConstraints gl = new GridBagConstraints();
		gl.gridx = 0;
		gl.gridy = row;
		gl.anchor = GridBagConstraints.NORTHWEST;
		gl.insets = new Insets(2, 6, 2, 4);
		card.add(new JLabel(label + ":"), gl);

		GridBagConstraints gf = new GridBagConstraints();
		gf.gridx = 1;
		gf.gridy = row;
		gf.anchor = GridBagConstraints.NORTHWEST;
		gf.fill = GridBagConstraints.HORIZONTAL;
		gf.weightx = 1.0;
		gf.insets = new Insets(2, 4, 2, 6);
		card.add(field, gf);
		return row + 1;
	}

	/*-------------------------------------------------------------------------*/
	private static JPanel buildLabeledFieldRow(String label, Component field)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		row.add(new JLabel(label + ":"));
		row.add(field);
		return row;
	}

	/*-------------------------------------------------------------------------*/
	private static JPanel labeledQuantity(String label, SwingQuantityEditWidget<?> field)
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		p.setOpaque(false);
		p.add(new JLabel(label + ":"));
		p.add(field);
		return p;
	}

	/*-------------------------------------------------------------------------*/
	private static <T> void configureComboRenderer(
		JComboBox<T> combo,
		Function<T, String> labelFn)
	{
		combo.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(
				JList<?> list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus)
			{
				Component c = super.getListCellRendererComponent(
					list,
					value,
					index,
					isSelected,
					cellHasFocus);
				if (c instanceof JLabel label)
				{
					@SuppressWarnings("unchecked")
					T item = (T)value;
					label.setText(item == null ? "" : labelFn.apply(item));
				}
				return c;
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	private static DensityUnit defaultOriginalGravity(Quantity.Unit unit)
	{
		DensityUnit sg = new DensityUnit(1.050D, SPECIFIC_GRAVITY);
		if (unit == SPECIFIC_GRAVITY)
		{
			return sg;
		}
		return new DensityUnit(sg.get(unit), unit, false);
	}

	/*-------------------------------------------------------------------------*/
	private void loadYeasts()
	{
		List<Yeast> yeasts = new ArrayList<>(Database.getInstance().getYeasts().values());
		yeasts.sort(Comparator.comparing(Yeast::getName, String.CASE_INSENSITIVE_ORDER));
		yeastCombo.removeAllItems();
		for (Yeast y : yeasts)
		{
			yeastCombo.addItem(y);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void onYeastChanged()
	{
		Yeast y = (Yeast)yeastCombo.getSelectedItem();
		if (y == null || y.getForm() == null)
		{
			return;
		}
		pitchQuantity.setUnitOptions(
			y.getForm().getDefaultUnit(),
			Quantity.Type.WEIGHT,
			Quantity.Type.VOLUME);
		if (y.getForm() == Yeast.Form.DRY)
		{
			pitchQuantity.setQuantity(Quantity.parseQuantity("11", GRAMS));
		}
		else
		{
			pitchQuantity.setQuantity(Quantity.parseQuantity("125", MILLILITRES));
		}
	}

	/*-------------------------------------------------------------------------*/
	private void registerRecalcListeners()
	{
		DocumentListener docListener = new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				recalculate();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				recalculate();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				recalculate();
			}
		};

		yeastCombo.addActionListener(e ->
		{
			onYeastChanged();
			recalculate();
		});
		sourceType.addActionListener(e -> recalculate());
		pitchQuantity.getTextField().getDocument().addDocumentListener(docListener);
		pitchQuantity.getUnitCombo().addActionListener(e -> recalculate());

		cellModeCombo.addActionListener(e -> updateCellMode());
		viabilityModeCombo.addActionListener(e -> updateViabilityMode());

		manualCellsBillions.getDocument().addDocumentListener(docListener);
		slurryCellsPerMlBillions.getDocument().addDocumentListener(docListener);

		manualViability.addQuantityChangeListener(q -> recalculate());
		productionDateModel.addChangeListener(e -> recalculate());
		pitchDateModel.addChangeListener(e -> recalculate());
		storageTemp.addActionListener(e -> recalculate());

		wortVolume.addQuantityChangeListener(q -> recalculate());
		originalGravity.addQuantityChangeListener(q -> recalculate());
		fermentationTemp.addQuantityChangeListener(q -> recalculate());
	}

	/*-------------------------------------------------------------------------*/
	private void updateCellMode()
	{
		YeastCalculator.CellCountMode mode =
			(YeastCalculator.CellCountMode)cellModeCombo.getSelectedItem();
		if (mode == null)
		{
			cellsCardLayout.show(cellsConditionalPanel, CELLS_CARD_NONE);
		}
		else if (mode == YeastCalculator.CellCountMode.MANUAL_TOTAL)
		{
			cellsCardLayout.show(cellsConditionalPanel, CELLS_CARD_MANUAL);
		}
		else if (mode == YeastCalculator.CellCountMode.SLURRY_DENSITY)
		{
			cellsCardLayout.show(cellsConditionalPanel, CELLS_CARD_SLURRY);
		}
		else
		{
			cellsCardLayout.show(cellsConditionalPanel, CELLS_CARD_NONE);
		}
		recalculate();
	}

	/*-------------------------------------------------------------------------*/
	private void updateViabilityMode()
	{
		YeastCalculator.ViabilityMode mode =
			(YeastCalculator.ViabilityMode)viabilityModeCombo.getSelectedItem();
		if (mode == YeastCalculator.ViabilityMode.MANUAL)
		{
			viabilityCardLayout.show(viabilityConditionalPanel, VIAB_CARD_MANUAL);
			manualViability.setEditable(true);
		}
		else if (mode == YeastCalculator.ViabilityMode.FROM_PACKAGE_AGE)
		{
			viabilityCardLayout.show(viabilityConditionalPanel, VIAB_CARD_AGE);
			manualViability.setEditable(false);
		}
		else
		{
			viabilityCardLayout.show(viabilityConditionalPanel, VIAB_CARD_NONE);
			manualViability.setEditable(false);
		}
		recalculate();
	}

	/*-------------------------------------------------------------------------*/
	public void refreshDisplayUnits()
	{
		Settings settings = Database.getInstance().getSettings();
		Quantity.Unit densityUnit = settings.getUnitForStepAndIngredient(
			Quantity.Type.FLUID_DENSITY,
			ProcessStep.Type.FERMENT,
			IngredientAddition.Type.YEAST);
		Quantity.Unit tempUnit = settings.getUnitForStepAndIngredient(
			Quantity.Type.TEMPERATURE,
			ProcessStep.Type.FERMENT,
			IngredientAddition.Type.YEAST);
		Quantity.Unit volUnit = UiUnitDisplaySupport.batchVolume();
		Quantity.Unit pitchUnit = settings.getUnitForStepAndIngredient(
			Quantity.Type.WEIGHT, ProcessStep.Type.FERMENT, IngredientAddition.Type.YEAST);

		wortVolume.setUnit(volUnit);
		originalGravity.setUnit(densityUnit);
		fermentationTemp.setUnit(tempUnit);
		pitchQuantity.setUnitOptions(pitchUnit, Quantity.Type.WEIGHT, Quantity.Type.VOLUME);
		recalculate();
	}

	private void recalculate()
	{
		try
		{
			errorLabel.setText(" ");
			Yeast yeast = (Yeast)yeastCombo.getSelectedItem();
			if (yeast == null)
			{
				clearResults();
				return;
			}

			VolumeUnit vol = wortVolume.getQuantity();
			DensityUnit og = originalGravity.getQuantity();
			TemperatureUnit temp = fermentationTemp.getQuantity();
			if (vol == null || og == null || temp == null)
			{
				clearResults();
				return;
			}

			YeastCalculator.PitchInput input = buildInput(yeast, vol, og, temp);
			YeastCalculator.Result result = YeastCalculator.calculate(input);
			displayResult(result, yeast);
		}
		catch (Exception ex)
		{
			clearResults();
			errorLabel.setText(ex.getMessage() == null ? ex.toString() : ex.getMessage());
		}
	}

	/*-------------------------------------------------------------------------*/
	private YeastCalculator.PitchInput buildInput(
		Yeast yeast,
		VolumeUnit vol,
		DensityUnit og,
		TemperatureUnit temp)
	{
		YeastCalculator.CellCountMode cellMode =
			(YeastCalculator.CellCountMode)cellModeCombo.getSelectedItem();
		if (cellMode == null)
		{
			cellMode = YeastCalculator.CellCountMode.ESTIMATE_FROM_QUANTITY;
		}

		Long manualCells = null;
		if (cellMode == YeastCalculator.CellCountMode.MANUAL_TOTAL)
		{
			manualCells = parseBillionsToCells(manualCellsBillions.getText());
		}

		Double slurryDensity = null;
		if (cellMode == YeastCalculator.CellCountMode.SLURRY_DENSITY)
		{
			slurryDensity = parseBillionsPerMl(slurryCellsPerMlBillions.getText());
		}

		YeastCalculator.ViabilityMode viabMode =
			(YeastCalculator.ViabilityMode)viabilityModeCombo.getSelectedItem();
		if (viabMode == null)
		{
			viabMode = YeastCalculator.ViabilityMode.DEFAULT_BY_SOURCE;
		}

		return new YeastCalculator.PitchInput(
			yeast,
			pitchQuantity.getQuantity(),
			pitchQuantity.getUnit(),
			(YeastSourceType)sourceType.getSelectedItem(),
			cellMode,
			manualCells,
			slurryDensity,
			viabMode,
			manualViability.getQuantity(),
			productionDateModel.getValue(),
			pitchDateModel.getValue(),
			(YeastCalculator.StorageTemperature)storageTemp.getSelectedItem(),
			vol.get(LITRES),
			og.get(PLATO),
			temp);
	}

	/*-------------------------------------------------------------------------*/
	private void displayResult(YeastCalculator.Result result, Yeast yeast)
	{
		totalCellsLabel.setText(YeastCalculator.formatCells(result.cellCount()));
		effectiveCellsLabel.setText(YeastCalculator.formatCells((long)result.effectiveCells()));
		requiredCellsLabel.setText(YeastCalculator.formatCells((long)result.requiredCells()));
		pitchRatioLabel.setText(formatPitchRatio(result.pitchRatio()));
		pitchRatioLabel.setFont(pitchRatioFont);
		pitchRatioLabel.setForeground(pitchRatioColor(result.pitchRatio()));
		pitchRateLabel.setText(getUiString(
			"tools.yeast.calculator.result.pitch.rate.value",
			result.weightedPitchRatePerMlPlato() / 1e6D));
		recommendedPitchLabel.setText(formatRecommended(result, yeast));
		warningsLabel.setText(formatWarningsHtml(result.warnings()));
	}

	/*-------------------------------------------------------------------------*/
	private static String formatRecommended(YeastCalculator.Result result, Yeast yeast)
	{
		if (result.pitchRatio() <= 0D || result.pitchRatio() >= 1D)
		{
			return getUiString("tools.yeast.calculator.result.recommended.none");
		}
		Yeast.Form form = yeast.getForm();
		if (form == Yeast.Form.DRY && result.recommendedDryGrams() > 0D)
		{
			return getUiString(
				"tools.yeast.calculator.result.recommended.dry",
				result.recommendedDryGrams());
		}
		if ((form == Yeast.Form.LIQUID || form == Yeast.Form.CULTURE)
			&& result.recommendedLiquidMl() > 0D)
		{
			return getUiString(
				"tools.yeast.calculator.result.recommended.liquid",
				result.recommendedLiquidMl());
		}
		return getUiString("tools.yeast.calculator.result.recommended.none");
	}

	/*-------------------------------------------------------------------------*/
	private static String formatPitchRatio(double ratio)
	{
		if (ratio <= 0D)
		{
			return getUiString("tools.yeast.calculator.result.pitch.ratio.none");
		}
		return String.format("%.2f", ratio);
	}

	/*-------------------------------------------------------------------------*/
	private static Color pitchRatioColor(double ratio)
	{
		if (ratio <= 0D)
		{
			return Color.BLACK;
		}
		if (ratio < 0.75D)
		{
			return new Color(180, 80, 0);
		}
		if (ratio > 1.5D)
		{
			return new Color(0, 90, 160);
		}
		return new Color(0, 120, 0);
	}

	/*-------------------------------------------------------------------------*/
	private static String formatWarningsHtml(List<YeastCalculator.Warning> warnings)
	{
		if (warnings == null || warnings.isEmpty())
		{
			return "<html>" + getUiString("tools.yeast.calculator.result.warnings.none") + "</html>";
		}
		StringBuilder sb = new StringBuilder("<html><ul style='margin:0;padding-left:14px'>");
		for (YeastCalculator.Warning w : warnings)
		{
			String text = formatWarning(w);
			sb.append("<li>").append(escapeHtml(text)).append("</li>");
		}
		sb.append("</ul></html>");
		return sb.toString();
	}

	/*-------------------------------------------------------------------------*/
	private static String formatWarning(YeastCalculator.Warning w)
	{
		if (w.messageKey() == null)
		{
			return "";
		}
		if (w.messageKey().startsWith("tools."))
		{
			return getUiString(w.messageKey());
		}
		return StringUtils.getProcessString(w.messageKey(), w.args());
	}

	/*-------------------------------------------------------------------------*/
	private static String escapeHtml(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;");
	}

	/*-------------------------------------------------------------------------*/
	private void clearResults()
	{
		totalCellsLabel.setText(" ");
		effectiveCellsLabel.setText(" ");
		requiredCellsLabel.setText(" ");
		pitchRatioLabel.setText(" ");
		pitchRatioLabel.setFont(pitchRatioFont);
		pitchRatioLabel.setForeground(Color.BLACK);
		pitchRateLabel.setText(" ");
		recommendedPitchLabel.setText(" ");
		warningsLabel.setText("<html></html>");
	}

	/*-------------------------------------------------------------------------*/
	private static Long parseBillionsToCells(String text)
	{
		if (text == null || text.isBlank())
		{
			return null;
		}
		double billions = Double.parseDouble(text.trim());
		return (long)(billions * BILLION);
	}

	/*-------------------------------------------------------------------------*/
	private static Double parseBillionsPerMl(String text)
	{
		if (text == null || text.isBlank())
		{
			return YeastCalculator.SLURRY_DEFAULT_CELLS_PER_ML;
		}
		double billions = Double.parseDouble(text.trim());
		return billions * BILLION;
	}

	/*-------------------------------------------------------------------------*/
	private static String sourceTypeLabel(YeastSourceType type)
	{
		return getUiString("tools.yeast.calculator.source." + type.name());
	}

	/*-------------------------------------------------------------------------*/
	private static String cellModeLabel(YeastCalculator.CellCountMode mode)
	{
		return getUiString("tools.yeast.calculator.cells.mode." + mode.name());
	}

	/*-------------------------------------------------------------------------*/
	private static String viabilityModeLabel(YeastCalculator.ViabilityMode mode)
	{
		return getUiString("tools.yeast.calculator.viability.mode." + mode.name());
	}

	/*-------------------------------------------------------------------------*/
	private static String storageTempLabel(YeastCalculator.StorageTemperature temp)
	{
		return getUiString("tools.yeast.calculator.storage." + temp.name());
	}

	/*-------------------------------------------------------------------------*/
	private static JLabel buildAttributionLabel()
	{
		String text = getUiString("tools.yeast.calculator.attribution");
		String html = "<html><div style='text-align:left'>"
			+ text.replace("\n", " ")
			+ "</div></html>";
		JLabel label = new JLabel(html);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildAssumptionsPanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

		Font small = panel.getFont().deriveFont(panel.getFont().getSize2D() - 1f);

		GridBagConstraints titleGbc = new GridBagConstraints();
		titleGbc.gridx = 0;
		titleGbc.gridy = 0;
		titleGbc.gridwidth = 2;
		titleGbc.anchor = GridBagConstraints.NORTHWEST;
		titleGbc.insets = new Insets(0, 4, 4, 4);
		JLabel title = new JLabel(getUiString("tools.yeast.calculator.assumptions.title") + ":");
		title.setFont(small);
		panel.add(title, titleGbc);

		for (int i = 0; i < ASSUMPTION_KEYS.length; i++)
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = i % 2;
			gbc.gridy = 1 + i / 2;
			gbc.weightx = 0.5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.insets = new Insets(0, 8, 2, 8);
			JLabel bullet = new JLabel("\u2022 " + getUiString(ASSUMPTION_KEYS[i]));
			bullet.setFont(small);
			panel.add(bullet, gbc);
		}
		return panel;
	}

	/*-------------------------------------------------------------------------*/
	private static void constrainFieldWidth(Component field, int width)
	{
		int height = field.getPreferredSize().height;
		Dimension size = new Dimension(width, height);
		field.setPreferredSize(size);
		field.setMinimumSize(size);
		field.setMaximumSize(new Dimension(width, height));
	}
}
