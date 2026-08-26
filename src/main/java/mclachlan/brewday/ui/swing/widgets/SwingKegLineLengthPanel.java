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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.KegLineLengthCalculator;
import mclachlan.brewday.math.LengthUnit;
import mclachlan.brewday.math.PressureUnit;
import mclachlan.brewday.math.TimeUnit;

import mclachlan.brewday.ui.swing.UiUnitDisplaySupport;

import static mclachlan.brewday.math.Quantity.Unit.MILLIMETRE;
import static mclachlan.brewday.math.Quantity.Unit.SECONDS;
import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Tools &gt; Keg Line Length calculator panel.
 */
public class SwingKegLineLengthPanel extends JPanel
{
	private static final double HOSE_PRESET_MM_3_16 = 4.7625D;
	private static final double HOSE_PRESET_MM_1_4 = 6.35D;

	private static final String[] ASSUMPTION_KEYS = new String[]
	{
		"tools.keg.line.assumptions.1",
		"tools.keg.line.assumptions.2",
		"tools.keg.line.assumptions.3",
		"tools.keg.line.assumptions.4",
		"tools.keg.line.assumptions.5",
		"tools.keg.line.assumptions.6"
	};

	private final SwingQuantityEditWidget<DensityUnit> specificGravity;
	private final SwingQuantityEditWidget<PressureUnit> co2Pressure;
	private final SwingQuantityEditWidget<LengthUnit> hoseDiameter;
	private final JComboBox<String> hosePresets;
	private final SwingQuantityEditWidget<LengthUnit> tapHeight;
	private final SwingQuantityEditWidget<TimeUnit> pourTime;
	private final SwingQuantityEditWidget<LengthUnit> elevation;
	private final SwingQuantityEditWidget<LengthUnit> hoseLengthResult;
	private final JLabel detailsLabel;
	private final JLabel errorLabel;

	public SwingKegLineLengthPanel()
	{
		super(new GridBagLayout());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		specificGravity = new SwingQuantityEditWidget<>(UiUnitDisplaySupport.density());
		specificGravity.setQuantity(new DensityUnit(1.05D, UiUnitDisplaySupport.density()));

		co2Pressure = new SwingQuantityEditWidget<>(UiUnitDisplaySupport.pressure());
		co2Pressure.setQuantity(new PressureUnit(96.5D, UiUnitDisplaySupport.pressure(), false));

		hoseDiameter = new SwingQuantityEditWidget<>(
			UiUnitDisplaySupport.current().getSmallLengthUnit(false));
		hoseDiameter.setQuantity(new LengthUnit(HOSE_PRESET_MM_3_16, MILLIMETRE));

		hosePresets = new JComboBox<>(new String[]
		{
			getUiString("tools.keg.line.hose.preset.3_16"),
			getUiString("tools.keg.line.hose.preset.1_4")
		});
		hosePresets.setSelectedIndex(0);
		hosePresets.addActionListener(e -> applyHosePreset());

		tapHeight = new SwingQuantityEditWidget<>(UiUnitDisplaySupport.length());
		tapHeight.setQuantity(new LengthUnit(0.46D, UiUnitDisplaySupport.length()));

		pourTime = new SwingQuantityEditWidget<>(SECONDS);
		pourTime.setQuantity(new TimeUnit(10D));

		elevation = new SwingQuantityEditWidget<>(UiUnitDisplaySupport.length());
		elevation.setQuantity(new LengthUnit(0D, UiUnitDisplaySupport.length()));

		hoseLengthResult = new SwingQuantityEditWidget<>(UiUnitDisplaySupport.length());
		hoseLengthResult.setEditable(false);

		specificGravity.setToolTipText(getUiString("tools.keg.line.specific.gravity.tooltip"));
		co2Pressure.setToolTipText(getUiString("tools.keg.line.co2.pressure.tooltip"));
		hoseDiameter.setToolTipText(getUiString("tools.keg.line.hose.diameter.tooltip"));
		tapHeight.setToolTipText(getUiString("tools.keg.line.tap.height.tooltip"));
		pourTime.setToolTipText(getUiString("tools.keg.line.pour.time.tooltip"));
		elevation.setToolTipText(getUiString("tools.keg.line.elevation.tooltip"));
		hoseLengthResult.setToolTipText(getUiString("tools.keg.line.result.length.tooltip"));

		detailsLabel = new JLabel(" ");
		errorLabel = new JLabel(" ");
		errorLabel.setForeground(java.awt.Color.RED);

		JPanel hoseValue = buildHoseValuePanel();
		int valueColumnWidth = Math.max(hoseValue.getPreferredSize().width, 200);
		constrainValueColumnWidth(hoseValue, valueColumnWidth);
		constrainQuantityFieldWidth(specificGravity, valueColumnWidth);
		constrainQuantityFieldWidth(co2Pressure, valueColumnWidth);
		constrainQuantityFieldWidth(tapHeight, valueColumnWidth);
		constrainQuantityFieldWidth(pourTime, valueColumnWidth);
		constrainQuantityFieldWidth(elevation, valueColumnWidth);
		constrainQuantityFieldWidth(hoseLengthResult, valueColumnWidth);

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
		row = addInputRow(row, getUiString("tools.keg.line.specific.gravity"), specificGravity, valueColumnWidth);
		row = addInputRow(row, getUiString("tools.keg.line.co2.pressure"), co2Pressure, valueColumnWidth);
		row = addHoseRow(row, hoseValue, valueColumnWidth);
		row = addInputRow(row, getUiString("tools.keg.line.tap.height"), tapHeight, valueColumnWidth);
		row = addInputRow(row, getUiString("tools.keg.line.pour.time"), pourTime, valueColumnWidth);
		row = addInputRow(row, getUiString("tools.keg.line.elevation"), elevation, valueColumnWidth);
		row = addInputRow(row, getUiString("tools.keg.line.result.length"), hoseLengthResult, valueColumnWidth);
		addDetailsRow(row, detailsLabel);

		JPanel assumptions = buildAssumptionsPanel();
		assumptions.setBorder(BorderFactory.createEmptyBorder(12, 0, 8, 0));

		gbc.gridy = row + 1;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(0, 8, 0, 8);
		add(errorLabel, gbc);

		gbc.gridy = row + 2;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		add(assumptions, gbc);

		gbc.gridy = row + 3;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.NONE;
		add(Box.createVerticalGlue(), gbc);

		setAlignmentX(Component.LEFT_ALIGNMENT);
		setAlignmentY(Component.TOP_ALIGNMENT);

		specificGravity.addQuantityChangeListener(q -> recalculate());
		co2Pressure.addQuantityChangeListener(q -> recalculate());
		hoseDiameter.addQuantityChangeListener(q -> recalculate());
		tapHeight.addQuantityChangeListener(q -> recalculate());
		pourTime.addQuantityChangeListener(q -> recalculate());
		elevation.addQuantityChangeListener(q -> recalculate());

		recalculate();
	}

	/*-------------------------------------------------------------------------*/
	public void refreshDisplayUnits()
	{
		specificGravity.setUnit(UiUnitDisplaySupport.density());
		co2Pressure.setUnit(UiUnitDisplaySupport.pressure());
		hoseDiameter.setUnit(UiUnitDisplaySupport.current().getSmallLengthUnit(false));
		tapHeight.setUnit(UiUnitDisplaySupport.length());
		elevation.setUnit(UiUnitDisplaySupport.length());
		hoseLengthResult.setUnit(UiUnitDisplaySupport.length());
		recalculate();
	}

	/*-------------------------------------------------------------------------*/

	private JPanel buildHoseValuePanel()
	{
		JPanel hoseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		hoseRow.add(hoseDiameter);
		hoseRow.add(hosePresets);
		return hoseRow;
	}

	private static void constrainValueColumnWidth(JPanel panel, int width)
	{
		int height = panel.getPreferredSize().height;
		Dimension size = new Dimension(width, height);
		panel.setPreferredSize(size);
		panel.setMinimumSize(size);
		panel.setMaximumSize(new Dimension(width, height));
	}

	private static void constrainQuantityFieldWidth(SwingQuantityEditWidget<?> field, int width)
	{
		int height = field.getPreferredSize().height;
		Dimension size = new Dimension(width, height);
		field.setPreferredSize(size);
		field.setMinimumSize(size);
		field.setMaximumSize(new Dimension(width, height));
	}

	private static JLabel buildAttributionLabel()
	{
		String text = getUiString("tools.keg.line.attribution");
		String html = "<html><div style='text-align:left'>"
			+ text.replace("\n", "<br>")
			+ "</div></html>";
		JLabel label = new JLabel(html);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private JPanel buildAssumptionsPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel title = new JLabel(getUiString("tools.keg.line.assumptions.title") + ":");
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

	private int addInputRow(int row, String label, SwingQuantityEditWidget<?> field, int valueColumnWidth)
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
		gf.fill = GridBagConstraints.NONE;
		gf.weightx = 0;
		gf.insets = new Insets(4, 8, 4, 8);
		add(wrapValueField(field, valueColumnWidth), gf);
		return row + 1;
	}

	private int addHoseRow(int row, JPanel hoseValue, int valueColumnWidth)
	{
		GridBagConstraints gl = new GridBagConstraints();
		gl.gridx = 0;
		gl.gridy = row;
		gl.anchor = GridBagConstraints.NORTHWEST;
		gl.insets = new Insets(4, 8, 4, 8);
		add(new JLabel(getUiString("tools.keg.line.hose.diameter") + ":"), gl);

		GridBagConstraints gf = new GridBagConstraints();
		gf.gridx = 1;
		gf.gridy = row;
		gf.anchor = GridBagConstraints.NORTHWEST;
		gf.fill = GridBagConstraints.NONE;
		gf.weightx = 0;
		gf.insets = new Insets(4, 8, 4, 8);
		add(wrapValueField(hoseValue, valueColumnWidth), gf);
		return row + 1;
	}

	private static JPanel wrapValueField(Component field, int width)
	{
		JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		wrapper.setOpaque(false);
		wrapper.add(field);
		int height = wrapper.getPreferredSize().height;
		Dimension size = new Dimension(width, height);
		wrapper.setPreferredSize(size);
		wrapper.setMinimumSize(size);
		wrapper.setMaximumSize(new Dimension(width, height));
		return wrapper;
	}

	private void addDetailsRow(int row, JLabel label)
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(2, 8, 4, 8);
		add(label, gbc);
	}

	private void applyHosePreset()
	{
		double mm = hosePresets.getSelectedIndex() == 1 ? HOSE_PRESET_MM_1_4 : HOSE_PRESET_MM_3_16;
		hoseDiameter.setQuantity(new LengthUnit(mm, MILLIMETRE));
	}

	private void recalculate()
	{
		try
		{
			DensityUnit sg = specificGravity.getQuantity();
			PressureUnit pressure = co2Pressure.getQuantity();
			LengthUnit hoseId = hoseDiameter.getQuantity();
			LengthUnit height = tapHeight.getQuantity();
			TimeUnit time = pourTime.getQuantity();

			if (sg == null || pressure == null || hoseId == null || height == null || time == null)
			{
				hoseLengthResult.setQuantity(null);
				detailsLabel.setText(" ");
				errorLabel.setText(" ");
				return;
			}

			LengthUnit elev = elevation.getQuantity();
			KegLineLengthCalculator.Result result = KegLineLengthCalculator.calculate(
				sg, pressure, hoseId, height, time, elev);

			hoseLengthResult.setQuantity(result.hoseLength());
			detailsLabel.setText(getUiString("tools.keg.line.details",
				result.reynoldsNumber(), result.frictionFactor()));
			errorLabel.setText(" ");
		}
		catch (BrewdayException ex)
		{
			hoseLengthResult.setQuantity(null);
			detailsLabel.setText(" ");
			errorLabel.setText(ex.getMessage());
		}
	}
}
