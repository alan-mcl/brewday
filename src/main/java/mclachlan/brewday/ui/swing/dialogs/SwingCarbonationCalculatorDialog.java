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

package mclachlan.brewday.ui.swing.dialogs;

import mclachlan.brewday.ui.swing.UiUnitDisplaySupport;
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.CarbonationUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.PressureUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.CarbonationCalculator;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.math.Quantity.Unit.*;
import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Package-step carbonation calculator: target CO₂, method-specific quantities,
 * style range, safety warnings, and force-carb equilibrium pressure.
 */
public class SwingCarbonationCalculatorDialog extends JDialog
{
	private enum PrimaryField
	{
		CARBONATION,
		PRESSURE
	}

	private final PackageStep step;
	private final Recipe recipe;

	private final JLabel styleRangeLabel;
	private final JLabel styleRangeWarning;
	private final SwingQuantityEditWidget<CarbonationUnit> targetCarb;
	private final JLabel safetyWarnings;
	private final JLabel methodLabel;
	private final JLabel resultLabel;
	private final JPanel forceCarbPanel;
	private final SwingQuantityEditWidget<TemperatureUnit> servingTemp;
	private final SwingQuantityEditWidget<PressureUnit> equilibriumPressure;
	private final JComboBox<String> primingFermentable;
	private final JLabel statusLabel;

	private PrimaryField primaryField = PrimaryField.CARBONATION;
	private CarbonationCalculator.Result lastResult;
	private boolean output;
	private CarbonationUnit appliedTarget;

	public SwingCarbonationCalculatorDialog(Window parent, PackageStep step, Recipe recipe)
	{
		super(parent, getUiString("package.calc.title"), Dialog.ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		this.step = step;
		this.recipe = recipe;

		styleRangeLabel = new JLabel(" ");
		styleRangeWarning = new JLabel(" ");
		styleRangeWarning.setVisible(false);

		targetCarb = new SwingQuantityEditWidget<>(UiUnitDisplaySupport.carbonation());
		targetCarb.setQuantity(CarbonationCalculator.defaultTarget(step, recipe));

		safetyWarnings = new JLabel(" ");
		safetyWarnings.setVisible(false);

		methodLabel = new JLabel(" ");
		resultLabel = new JLabel(" ");

		servingTemp = new SwingQuantityEditWidget<>(UiUnitDisplaySupport.temperature());
		servingTemp.setQuantity(new TemperatureUnit(4D, UiUnitDisplaySupport.temperature()));
		equilibriumPressure = new SwingQuantityEditWidget<>(UiUnitDisplaySupport.pressure());
		forceCarbPanel = buildForceCarbPanel();

		primingFermentable = new JComboBox<>();
		primingFermentable.setVisible(false);
		populatePrimingFermentables();

		statusLabel = new JLabel(" ");

		JPanel quickSet = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		JButton useMin = new JButton(getUiString("package.calc.use.min"));
		JButton useMid = new JButton(getUiString("package.calc.use.mid"));
		JButton useMax = new JButton(getUiString("package.calc.use.max"));
		useMin.addActionListener(e -> applyStyleTarget(true, false, false));
		useMid.addActionListener(e -> applyStyleTarget(false, true, false));
		useMax.addActionListener(e -> applyStyleTarget(false, false, true));
		quickSet.add(useMin);
		quickSet.add(useMid);
		quickSet.add(useMax);

		JPanel form = new JPanel(new GridBagLayout());
		int row = 0;
		addFullWidthRow(form, row++, styleRangeLabel);
		addFullWidthRow(form, row++, quickSet);
		addFullWidthRow(form, row++, styleRangeWarning);
		addRow(form, row++, getUiString("package.calc.target"), targetCarb);
		addFullWidthRow(form, row++, safetyWarnings);
		addRow(form, row++, getUiString("package.carbonation.method"), methodLabel);
		addRow(form, row++, getUiString("package.calc.priming.fermentable"), primingFermentable);
		addFullWidthRow(form, row++, resultLabel);
		addFullWidthRow(form, row++, forceCarbPanel);
		addFullWidthRow(form, row++, statusLabel);

		targetCarb.addQuantityChangeListener(v ->
		{
			primaryField = PrimaryField.CARBONATION;
			recalc();
		});
		servingTemp.addQuantityChangeListener(v -> recalc());
		equilibriumPressure.addQuantityChangeListener(v ->
		{
			primaryField = PrimaryField.PRESSURE;
			recalcFromPressure();
		});
		primingFermentable.addActionListener(e -> recalc());

		JPanel south = new JPanel();
		JButton ok = new JButton(getUiString("ui.ok"));
		JButton cancel = new JButton(getUiString("ui.cancel"));
		DialogButtonTooltips.wireOkCancel(ok, cancel);
		ok.addActionListener(e ->
		{
			output = true;
			appliedTarget = targetCarb.getQuantity();
			dispose();
		});
		cancel.addActionListener(e -> dispose());
		south.add(ok);
		south.add(cancel);
		getRootPane().setDefaultButton(ok);

		setLayout(new BorderLayout());
		add(form, BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);

		updateStyleRangeDisplay();
		recalc();
		pack();
		setLocationRelativeTo(parent);
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildForceCarbPanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder(
			getUiString("package.calc.force.carb.panel")));

		JLabel tempLabel = new JLabel(getUiString("package.calc.serving.temp") + ":");
		JLabel pressureLabel = new JLabel(getUiString("package.calc.equilibrium.pressure") + ":");

		GridBagConstraints gl = new GridBagConstraints();
		gl.gridx = 0;
		gl.gridy = 0;
		gl.anchor = GridBagConstraints.WEST;
		gl.insets = new Insets(4, 6, 4, 4);
		panel.add(tempLabel, gl);

		GridBagConstraints gt = new GridBagConstraints();
		gt.gridx = 1;
		gt.gridy = 0;
		gt.weightx = 1.0;
		gt.fill = GridBagConstraints.HORIZONTAL;
		gt.insets = new Insets(4, 4, 4, 6);
		panel.add(servingTemp, gt);

		gl.gridy = 1;
		panel.add(pressureLabel, gl);
		gt.gridy = 1;
		panel.add(equilibriumPressure, gt);

		servingTemp.setToolTipText(getUiString("package.calc.serving.temp.tooltip"));
		equilibriumPressure.setToolTipText(getUiString("package.calc.equilibrium.pressure.tooltip"));

		return panel;
	}

	/*-------------------------------------------------------------------------*/
	private void populatePrimingFermentables()
	{
		List<String> names = new ArrayList<>();
		for (Fermentable f : Database.getInstance().getFermentables().values())
		{
			FermentableAddition probe = new FermentableAddition(
				f, new WeightUnit(1D, GRAMS), GRAMS, null);
			if (Equations.isPrimingFermentable(probe))
			{
				names.add(f.getName());
			}
		}
		Collections.sort(names);
		primingFermentable.setModel(new DefaultComboBoxModel<>(names.toArray(String[]::new)));
		if (!names.isEmpty())
		{
			primingFermentable.setSelectedIndex(0);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void applyStyleTarget(boolean min, boolean mid, boolean max)
	{
		Style style = resolveStyle();
		if (style == null || style.getCarbMin() == null || style.getCarbMax() == null)
		{
			return;
		}
		CarbonationUnit value;
		if (min)
		{
			value = style.getCarbMin();
		}
		else if (max)
		{
			value = style.getCarbMax();
		}
		else
		{
			double v = (style.getCarbMin().get(VOLUMES) + style.getCarbMax().get(VOLUMES)) / 2D;
			value = new CarbonationUnit(v, VOLUMES, false);
		}
		primaryField = PrimaryField.CARBONATION;
		targetCarb.setQuantity(value);
		recalc();
	}

	/*-------------------------------------------------------------------------*/
	private Style resolveStyle()
	{
		if (step.getStyleId() == null)
		{
			return null;
		}
		return Database.getInstance().getStyles().get(step.getStyleId());
	}

	/*-------------------------------------------------------------------------*/
	private void updateStyleRangeDisplay()
	{
		Style style = resolveStyle();
		if (style == null || style.getCarbMin() == null || style.getCarbMax() == null)
		{
			styleRangeLabel.setText(getUiString("package.calc.style.unavailable"));
		}
		else
		{
			styleRangeLabel.setText(String.format(
				getUiString("package.calc.style.range"),
				style.getCarbMin().get(VOLUMES),
				style.getCarbMax().get(VOLUMES)));
		}
	}

	/*-------------------------------------------------------------------------*/
	private void recalcFromPressure()
	{
		if (servingTemp.getQuantity() == null || equilibriumPressure.getQuantity() == null)
		{
			return;
		}
		CarbonationUnit carb = Equations.calcEquilibriumCo2(
			servingTemp.getQuantity(),
			equilibriumPressure.getQuantity());
		targetCarb.setQuantity(carb);
		recalc();
	}

	/*-------------------------------------------------------------------------*/
	private void recalc()
	{
		PackageStep.CarbonationMethod method = step.getCarbonationMethod();
		if (method == null)
		{
			method = PackageStep.CarbonationMethod.PRIMING_SUGAR;
		}
		methodLabel.setText(method.toString());

		boolean showPriming = method == PackageStep.CarbonationMethod.PRIMING_SUGAR;
		primingFermentable.setVisible(showPriming);

		boolean showForceCarb = method == PackageStep.CarbonationMethod.FORCE_CARB
			&& step.getPackagingType() == PackageStep.PackagingType.KEG;
		forceCarbPanel.setVisible(showForceCarb);

		Fermentable priming = null;
		if (showPriming)
		{
			String name = (String)primingFermentable.getSelectedItem();
			if (name != null)
			{
				priming = Database.getInstance().getFermentables().get(name);
			}
		}

		CarbonationUnit target = targetCarb.getQuantity();
		if (target == null)
		{
			statusLabel.setText(getUiString("package.calc.status.no.target"));
			return;
		}

		lastResult = CarbonationCalculator.calculate(
			step, recipe, target, priming, servingTemp.getQuantity());

		if (showForceCarb && primaryField == PrimaryField.CARBONATION
			&& lastResult.equilibriumPressure() != null)
		{
			equilibriumPressure.setQuantity(lastResult.equilibriumPressure());
		}

		updateStyleRangeWarning(target);
		updateSafetyWarnings(lastResult);
		updateResultLabel(method, lastResult);
		updateStatus(lastResult);
	}

	/*-------------------------------------------------------------------------*/
	private void updateStyleRangeWarning(CarbonationUnit target)
	{
		Style style = resolveStyle();
		if (style == null || style.getCarbMin() == null || style.getCarbMax() == null)
		{
			styleRangeWarning.setVisible(false);
			return;
		}
		double t = target.get(VOLUMES);
		double min = style.getCarbMin().get(VOLUMES);
		double max = style.getCarbMax().get(VOLUMES);
		if (t < min - 1e-6 || t > max + 1e-6)
		{
			styleRangeWarning.setText(getUiString("package.calc.style.out.of.range"));
			styleRangeWarning.setVisible(true);
		}
		else
		{
			styleRangeWarning.setVisible(false);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void updateSafetyWarnings(CarbonationCalculator.Result result)
	{
		StringBuilder sb = new StringBuilder();
		for (CarbonationCalculator.SafetyWarning w : result.warnings())
		{
			if (w.severity() == CarbonationCalculator.WarningSeverity.ERROR)
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append("<br>");
			}
			sb.append(formatWarning(w));
		}
		if (sb.length() == 0)
		{
			safetyWarnings.setText(" ");
			safetyWarnings.setVisible(false);
		}
		else
		{
			safetyWarnings.setText("<html>" + sb + "</html>");
			safetyWarnings.setVisible(true);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static String formatWarning(CarbonationCalculator.SafetyWarning w)
	{
		Object[] args = w.args();
		if (args == null || args.length == 0)
		{
			return getUiString(w.messageKey());
		}
		return String.format(getUiString(w.messageKey()), args);
	}

	/*-------------------------------------------------------------------------*/
	private void updateResultLabel(
		PackageStep.CarbonationMethod method,
		CarbonationCalculator.Result result)
	{
		switch (method)
		{
			case PRIMING_SUGAR ->
			{
				if (result.primingAddition() != null)
				{
					resultLabel.setText(String.format(
						getUiString("package.calc.result.priming"),
						result.primingAddition().getQuantity().get(GRAMS)));
				}
				else
				{
					resultLabel.setText(" ");
				}
			}
			case SPEISE ->
			{
				if (result.requiredSpeiseVolume() != null)
				{
					resultLabel.setText(String.format(
						getUiString("package.calc.result.speise"),
						result.requiredSpeiseVolume().get(LITRES)));
				}
				else
				{
					resultLabel.setText(" ");
				}
			}
			case SPUNDING ->
			{
				if (result.maxAchievableCarb() != null)
				{
					resultLabel.setText(String.format(
						getUiString("package.calc.result.spunding"),
						result.maxAchievableCarb().get(VOLUMES),
						result.targetCarb().get(VOLUMES)));
				}
				else
				{
					resultLabel.setText(" ");
				}
			}
			case KRAUSENING ->
			{
				if (result.requiredKrausenVolume() != null)
				{
					resultLabel.setText(String.format(
						getUiString("package.calc.result.krausen"),
						result.requiredKrausenVolume().get(LITRES)));
				}
				else
				{
					resultLabel.setText(" ");
				}
			}
			case FORCE_CARB ->
			{
				resultLabel.setText(String.format(
					getUiString("package.calc.result.force.carb"),
					result.targetCarb().get(VOLUMES)));
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private void updateStatus(CarbonationCalculator.Result result)
	{
		getRootPane().getDefaultButton().setEnabled(true);
		for (CarbonationCalculator.SafetyWarning w : result.warnings())
		{
			if (w.severity() == CarbonationCalculator.WarningSeverity.ERROR)
			{
				statusLabel.setText(formatWarning(w));
				getRootPane().getDefaultButton().setEnabled(false);
				return;
			}
		}
		getRootPane().getDefaultButton().setEnabled(
			result.status() != CarbonationCalculator.Status.UNSUPPORTED);
		if (result.status() == CarbonationCalculator.Status.OK)
		{
			statusLabel.setText(" ");
		}
		else if (result.status() == CarbonationCalculator.Status.NOT_ACHIEVABLE)
		{
			statusLabel.setText(getUiString("package.calc.status.not.achievable"));
		}
		else if (result.status() == CarbonationCalculator.Status.MISSING_DATA)
		{
			statusLabel.setText(getUiString("package.calc.status.missing.data"));
		}
		else
		{
			statusLabel.setText(getUiString("package.calc.status.unsupported"));
		}
	}

	/*-------------------------------------------------------------------------*/
	public boolean getOutput()
	{
		return output;
	}

	/*-------------------------------------------------------------------------*/
	public CarbonationCalculator.Result getResult()
	{
		return lastResult;
	}

	/*-------------------------------------------------------------------------*/
	public CarbonationUnit getAppliedTarget()
	{
		return appliedTarget;
	}

	/*-------------------------------------------------------------------------*/
	public static void applyResult(PackageStep step, CarbonationCalculator.Result result)
	{
		if (step == null || result == null)
		{
			return;
		}
		PackageStep.CarbonationMethod method = step.getCarbonationMethod();
		if (method == PackageStep.CarbonationMethod.PRIMING_SUGAR && result.primingAddition() != null)
		{
			List<IngredientAddition> kept = new ArrayList<>();
			for (IngredientAddition ia : step.getIngredientAdditions())
			{
				if (ia instanceof FermentableAddition fa
					&& Equations.isPrimingFermentable(fa))
				{
					continue;
				}
				kept.add(ia);
			}
			kept.add(result.primingAddition());
			step.setIngredients(kept);
		}
		else if (method == PackageStep.CarbonationMethod.FORCE_CARB
			&& result.targetCarb() != null)
		{
			step.setForcedCarbonation(result.targetCarb());
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void addRow(JPanel form, int row, String label, javax.swing.JComponent field)
	{
		GridBagConstraints gl = new GridBagConstraints();
		gl.gridx = 0;
		gl.gridy = row;
		gl.anchor = GridBagConstraints.WEST;
		gl.insets = new Insets(4, 8, 4, 8);
		form.add(new JLabel(label + ":"), gl);
		GridBagConstraints gf = new GridBagConstraints();
		gf.gridx = 1;
		gf.gridy = row;
		gf.fill = GridBagConstraints.HORIZONTAL;
		gf.weightx = 1.0;
		gf.insets = new Insets(4, 8, 4, 8);
		form.add(field, gf);
	}

	/*-------------------------------------------------------------------------*/
	private static void addFullWidthRow(JPanel form, int row, javax.swing.JComponent field)
	{
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = row;
		g.gridwidth = 2;
		g.weightx = 1.0;
		g.fill = GridBagConstraints.HORIZONTAL;
		g.insets = new Insets(4, 8, 4, 8);
		form.add(field, g);
	}
}
