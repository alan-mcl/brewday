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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.YeastCalculator;
import mclachlan.brewday.recipe.YeastSourceType;
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

	private static final String[] ASSUMPTION_KEYS = new String[]
	{
		"tools.yeast.calculator.assumptions.1",
		"tools.yeast.calculator.assumptions.2",
		"tools.yeast.calculator.assumptions.3",
		"tools.yeast.calculator.assumptions.4",
		"tools.yeast.calculator.assumptions.5",
		"tools.yeast.calculator.assumptions.6"
	};

	private final JComboBox<Yeast> yeastCombo;
	private final JComboBox<YeastSourceType> sourceType;
	private final SwingQuantitySelectAndEditWidget pitchQuantity;

	private final JRadioButton cellEstimate;
	private final JRadioButton cellManual;
	private final JRadioButton cellSlurry;
	private final JTextField manualCellsBillions;
	private final JTextField slurryCellsPerMlBillions;

	private final JRadioButton viabManual;
	private final JRadioButton viabDefault;
	private final JRadioButton viabAge;
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

	public SwingYeastCalculatorPanel()
	{
		super(new GridBagLayout());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		yeastCombo = new JComboBox<>();
		yeastCombo.setRenderer((list, value, index, isSelected, hasFocus) ->
			new JLabel(value == null ? "" : value.getName()));
		loadYeasts();
		yeastCombo.addActionListener(e -> onYeastChanged());

		sourceType = new JComboBox<>(YeastSourceType.values());
		sourceType.setRenderer((list, value, index, isSelected, hasFocus) ->
		{
			JLabel label = new JLabel(value == null ? "" : sourceTypeLabel(value));
			if (isSelected && hasFocus)
			{
				label.setOpaque(true);
			}
			return label;
		});

		pitchQuantity = new SwingQuantitySelectAndEditWidget(GRAMS, Quantity.Type.WEIGHT, Quantity.Type.VOLUME);

		cellEstimate = new JRadioButton(getUiString("tools.yeast.calculator.cells.estimate"), true);
		cellManual = new JRadioButton(getUiString("tools.yeast.calculator.cells.manual"));
		cellSlurry = new JRadioButton(getUiString("tools.yeast.calculator.cells.slurry"));
		ButtonGroup cellGroup = new ButtonGroup();
		cellGroup.add(cellEstimate);
		cellGroup.add(cellManual);
		cellGroup.add(cellSlurry);

		manualCellsBillions = new JTextField("11", 8);
		slurryCellsPerMlBillions = new JTextField("1.0", 8);
		manualCellsBillions.setEnabled(false);
		slurryCellsPerMlBillions.setEnabled(false);

		viabManual = new JRadioButton(getUiString("tools.yeast.calculator.viability.manual"));
		viabDefault = new JRadioButton(getUiString("tools.yeast.calculator.viability.default"), true);
		viabAge = new JRadioButton(getUiString("tools.yeast.calculator.viability.age"));
		ButtonGroup viabGroup = new ButtonGroup();
		viabGroup.add(viabManual);
		viabGroup.add(viabDefault);
		viabGroup.add(viabAge);

		manualViability = new SwingQuantityEditWidget<>(PERCENTAGE_DISPLAY);
		manualViability.setQuantity(new PercentageUnit(0.96D));
		manualViability.setEditable(false);

		productionDateModel = new LocalDateModel(LocalDate.now().minusMonths(1));
		productionDate = new JDatePicker(productionDateModel);
		productionDate.setTextfieldColumns(12);
		productionDate.setEnabled(false);

		pitchDateModel = new LocalDateModel(LocalDate.now());
		pitchDate = new JDatePicker(pitchDateModel);
		pitchDate.setTextfieldColumns(12);
		pitchDate.setEnabled(false);

		storageTemp = new JComboBox<>(YeastCalculator.StorageTemperature.values());
		storageTemp.setRenderer((list, value, index, isSelected, hasFocus) ->
			new JLabel(value == null ? "" : storageTempLabel(value)));
		storageTemp.setSelectedItem(YeastCalculator.StorageTemperature.FRIDGE_10C);
		storageTemp.setEnabled(false);

		wortVolume = new SwingQuantityEditWidget<>(LITRES);
		wortVolume.setQuantity(new VolumeUnit(20D, LITRES));

		originalGravity = new SwingQuantityEditWidget<>(PLATO);
		originalGravity.setQuantity(new DensityUnit(12.5D, PLATO));

		fermentationTemp = new SwingQuantityEditWidget<>(CELSIUS);
		fermentationTemp.setQuantity(new TemperatureUnit(20D));

		totalCellsLabel = new JLabel(" ");
		effectiveCellsLabel = new JLabel(" ");
		requiredCellsLabel = new JLabel(" ");
		pitchRatioLabel = new JLabel(" ");
		pitchRateLabel = new JLabel(" ");
		recommendedPitchLabel = new JLabel(" ");
		warningsLabel = new JLabel("<html></html>");
		errorLabel = new JLabel(" ");
		errorLabel.setForeground(Color.RED);

		int valueColumnWidth = 220;
		constrainQuantityFieldWidth(pitchQuantity, valueColumnWidth);
		constrainQuantityFieldWidth(manualViability, valueColumnWidth);
		constrainQuantityFieldWidth(wortVolume, valueColumnWidth);
		constrainQuantityFieldWidth(originalGravity, valueColumnWidth);
		constrainQuantityFieldWidth(fermentationTemp, valueColumnWidth);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(0, 8, 12, 8);
		add(buildAttributionLabel(), gbc);

		int row = 1;
		row = addSectionTitle(row, getUiString("tools.yeast.calculator.section.pitch"));
		row = addInputRow(row, getUiString("tools.yeast.calculator.yeast"), yeastCombo, valueColumnWidth);
		row = addInputRow(row, getUiString("tools.yeast.calculator.source"), sourceType, valueColumnWidth);
		row = addInputRow(row, getUiString("tools.yeast.calculator.pitch.amount"), pitchQuantity, valueColumnWidth);

		row = addSectionTitle(row, getUiString("tools.yeast.calculator.section.cells"));
		row = addRadioRow(row, cellEstimate);
		row = addRadioRow(row, cellManual);
		row = addInputRow(row, getUiString("tools.yeast.calculator.cells.manual.billions"),
			manualCellsBillions, valueColumnWidth);
		row = addRadioRow(row, cellSlurry);
		row = addInputRow(row, getUiString("tools.yeast.calculator.cells.slurry.density"),
			slurryCellsPerMlBillions, valueColumnWidth);

		row = addSectionTitle(row, getUiString("tools.yeast.calculator.section.viability"));
		row = addRadioRow(row, viabDefault);
		row = addRadioRow(row, viabManual);
		row = addInputRow(row, getUiString("tools.yeast.calculator.viability.percent"),
			manualViability, valueColumnWidth);
		row = addRadioRow(row, viabAge);
		row = addInputRow(row, getUiString("tools.yeast.calculator.production.date"),
			productionDate, valueColumnWidth);
		row = addInputRow(row, getUiString("tools.yeast.calculator.pitch.date"),
			pitchDate, valueColumnWidth);
		row = addInputRow(row, getUiString("tools.yeast.calculator.storage.temp"),
			storageTemp, valueColumnWidth);

		row = addSectionTitle(row, getUiString("tools.yeast.calculator.section.wort"));
		row = addInputRow(row, getUiString("tools.yeast.calculator.wort.volume"), wortVolume, valueColumnWidth);
		row = addInputRow(row, getUiString("tools.yeast.calculator.original.gravity"),
			originalGravity, valueColumnWidth);
		row = addInputRow(row, getUiString("tools.yeast.calculator.fermentation.temp"),
			fermentationTemp, valueColumnWidth);

		row = addSectionTitle(row, getUiString("tools.yeast.calculator.section.results"));
		row = addResultRow(row, getUiString("tools.yeast.calculator.result.total.cells"), totalCellsLabel);
		row = addResultRow(row, getUiString("tools.yeast.calculator.result.effective.cells"), effectiveCellsLabel);
		row = addResultRow(row, getUiString("tools.yeast.calculator.result.required.cells"), requiredCellsLabel);
		row = addResultRow(row, getUiString("tools.yeast.calculator.result.pitch.ratio"), pitchRatioLabel);
		row = addResultRow(row, getUiString("tools.yeast.calculator.result.pitch.rate"), pitchRateLabel);
		row = addResultRow(row, getUiString("tools.yeast.calculator.result.recommended"), recommendedPitchLabel);
		addResultRow(row + 1, getUiString("tools.yeast.calculator.result.warnings"), warningsLabel);

		JPanel assumptions = buildAssumptionsPanel();
		assumptions.setBorder(BorderFactory.createEmptyBorder(12, 0, 8, 0));

		gbc.gridy = row + 2;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 8, 0, 8);
		add(errorLabel, gbc);

		gbc.gridy = row + 3;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		add(assumptions, gbc);

		gbc.gridy = row + 4;
		gbc.weighty = 1.0;
		add(Box.createVerticalGlue(), gbc);

		setAlignmentX(Component.LEFT_ALIGNMENT);
		setAlignmentY(Component.TOP_ALIGNMENT);

		registerRecalcListeners();
		onYeastChanged();
		recalculate();
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

		yeastCombo.addActionListener(e -> recalculate());
		sourceType.addActionListener(e -> recalculate());
		pitchQuantity.getTextField().getDocument().addDocumentListener(docListener);
		pitchQuantity.getUnitCombo().addActionListener(e -> recalculate());

		cellEstimate.addActionListener(e -> updateCellMode());
		cellManual.addActionListener(e -> updateCellMode());
		cellSlurry.addActionListener(e -> updateCellMode());

		viabManual.addActionListener(e -> updateViabilityMode());
		viabDefault.addActionListener(e -> updateViabilityMode());
		viabAge.addActionListener(e -> updateViabilityMode());

		manualCellsBillions.getDocument().addDocumentListener(docListener);
		slurryCellsPerMlBillions.getDocument().addDocumentListener(docListener);

		manualViability.addQuantityChangeListener(q -> recalculate());
		productionDateModel.addChangeListener(e -> recalculate());
		pitchDateModel.addChangeListener(e -> recalculate());
		storageTemp.addActionListener(e -> recalculate());

		wortVolume.addQuantityChangeListener(q -> recalculate());
		originalGravity.addQuantityChangeListener(q -> recalculate());
		fermentationTemp.addQuantityChangeListener(q -> recalculate());

		updateCellMode();
		updateViabilityMode();
	}

	/*-------------------------------------------------------------------------*/
	private void updateCellMode()
	{
		manualCellsBillions.setEnabled(cellManual.isSelected());
		slurryCellsPerMlBillions.setEnabled(cellSlurry.isSelected());
		recalculate();
	}

	/*-------------------------------------------------------------------------*/
	private void updateViabilityMode()
	{
		boolean manual = viabManual.isSelected();
		boolean age = viabAge.isSelected();
		manualViability.setEditable(manual);
		productionDate.setEnabled(age);
		pitchDate.setEnabled(age);
		storageTemp.setEnabled(age);
		recalculate();
	}

	/*-------------------------------------------------------------------------*/
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
		YeastCalculator.CellCountMode cellMode = cellManual.isSelected()
			? YeastCalculator.CellCountMode.MANUAL_TOTAL
			: cellSlurry.isSelected()
				? YeastCalculator.CellCountMode.SLURRY_DENSITY
				: YeastCalculator.CellCountMode.ESTIMATE_FROM_QUANTITY;

		Long manualCells = null;
		if (cellManual.isSelected())
		{
			manualCells = parseBillionsToCells(manualCellsBillions.getText());
		}

		Double slurryDensity = null;
		if (cellSlurry.isSelected())
		{
			slurryDensity = parseBillionsPerMl(slurryCellsPerMlBillions.getText());
		}

		YeastCalculator.ViabilityMode viabMode = viabManual.isSelected()
			? YeastCalculator.ViabilityMode.MANUAL
			: viabAge.isSelected()
				? YeastCalculator.ViabilityMode.FROM_PACKAGE_AGE
				: YeastCalculator.ViabilityMode.DEFAULT_BY_SOURCE;

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
		pitchRatioLabel.setForeground(pitchRatioColor(result.pitchRatio()));
		pitchRateLabel.setText(getUiString(
			"tools.yeast.calculator.result.pitch.rate.value",
			result.weightedPitchRatePerMlPlato() / 1e6D));

		String rec = formatRecommended(result, yeast);
		recommendedPitchLabel.setText(rec);

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
		StringBuilder sb = new StringBuilder("<html><ul style='margin:0;padding-left:16px'>");
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
	private static String storageTempLabel(YeastCalculator.StorageTemperature temp)
	{
		return getUiString("tools.yeast.calculator.storage." + temp.name());
	}

	/*-------------------------------------------------------------------------*/
	private static JLabel buildAttributionLabel()
	{
		String text = getUiString("tools.yeast.calculator.attribution");
		String html = "<html><div style='text-align:left'>"
			+ text.replace("\n", "<br>")
			+ "</div></html>";
		JLabel label = new JLabel(html);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildAssumptionsPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel title = new JLabel(getUiString("tools.yeast.calculator.assumptions.title") + ":");
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(title);
		panel.add(Box.createVerticalStrut(4));

		for (String key : ASSUMPTION_KEYS)
		{
			JLabel bullet = new JLabel("\u2022 " + getUiString(key));
			bullet.setAlignmentX(Component.LEFT_ALIGNMENT);
			bullet.setBorder(BorderFactory.createEmptyBorder(0, 12, 2, 0));
			panel.add(bullet);
		}
		return panel;
	}

	/*-------------------------------------------------------------------------*/
	private int addSectionTitle(int row, String title)
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(12, 8, 4, 8);
		JLabel label = new JLabel(title);
		label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
		add(label, gbc);
		return row + 1;
	}

	/*-------------------------------------------------------------------------*/
	private int addInputRow(int row, String label, Component field, int valueColumnWidth)
	{
		GridBagConstraints gl = new GridBagConstraints();
		gl.gridx = 0;
		gl.gridy = row;
		gl.anchor = GridBagConstraints.NORTHWEST;
		gl.insets = new Insets(4, 8, 4, 8);
		add(new JLabel(label + ":"), gl);

		GridBagConstraints gf = new GridBagConstraints();
		gf.gridx = 1;
		gf.gridy = row;
		gf.anchor = GridBagConstraints.NORTHWEST;
		gf.insets = new Insets(4, 8, 4, 8);
		add(wrapValueField(field, valueColumnWidth), gf);
		return row + 1;
	}

	/*-------------------------------------------------------------------------*/
	private int addRadioRow(int row, JRadioButton radio)
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(2, 16, 2, 8);
		add(radio, gbc);
		return row + 1;
	}

	/*-------------------------------------------------------------------------*/
	private int addResultRow(int row, String label, JLabel value)
	{
		GridBagConstraints gl = new GridBagConstraints();
		gl.gridx = 0;
		gl.gridy = row;
		gl.anchor = GridBagConstraints.NORTHWEST;
		gl.insets = new Insets(4, 8, 4, 8);
		add(new JLabel(label + ":"), gl);

		GridBagConstraints gv = new GridBagConstraints();
		gv.gridx = 1;
		gv.gridy = row;
		gv.anchor = GridBagConstraints.NORTHWEST;
		gv.insets = new Insets(4, 8, 4, 8);
		add(value, gv);
		return row + 1;
	}

	/*-------------------------------------------------------------------------*/
	private static JPanel wrapValueField(Component field, int width)
	{
		JPanel wrapper = new JPanel();
		wrapper.setOpaque(false);
		wrapper.add(field);
		int height = wrapper.getPreferredSize().height;
		Dimension size = new Dimension(width, height);
		wrapper.setPreferredSize(size);
		wrapper.setMinimumSize(size);
		wrapper.setMaximumSize(new Dimension(width, height));
		return wrapper;
	}

	/*-------------------------------------------------------------------------*/
	private static void constrainQuantityFieldWidth(SwingQuantityEditWidget<?> field, int width)
	{
		int height = field.getPreferredSize().height;
		Dimension size = new Dimension(width, height);
		field.setPreferredSize(size);
		field.setMinimumSize(size);
		field.setMaximumSize(new Dimension(width, height));
	}

	/*-------------------------------------------------------------------------*/
	private static void constrainQuantityFieldWidth(
		SwingQuantitySelectAndEditWidget field,
		int width)
	{
		int height = field.getPreferredSize().height;
		Dimension size = new Dimension(width, height);
		field.setPreferredSize(size);
		field.setMinimumSize(size);
		field.setMaximumSize(new Dimension(width, height));
	}
}
