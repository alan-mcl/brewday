package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Rebalance mash grain-bill percentages while holding total weight constant.
 */
public class SwingGrainProportionAdjusterDialog extends JDialog
{
	private final List<FermentableAddition> adjustable;
	private final double totalGrams;
	private final double[] gramWeights;
	private final double[] percents;

	private final List<SwingQuantityEditWidget<?>> quantityFields = new ArrayList<>();
	private final List<SwingQuantityEditWidget<PercentageUnit>> percentFields = new ArrayList<>();
	private final JLabel totalLabel;

	private boolean updating;
	private boolean output;

	public SwingGrainProportionAdjusterDialog(Window parent, List<FermentableAddition> fermentableAdditions)
	{
		super(parent, getUiString("tools.grain.proportion.adjuster"), Dialog.ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		adjustable = GrainBillProportionAdjuster.filterAdjustable(fermentableAdditions);
		totalGrams = Equations.calcTotalGrainWeight(adjustable).get(GRAMS);
		int n = adjustable.size();
		gramWeights = new double[n];
		percents = GrainBillProportionAdjuster.initPercents(adjustable, totalGrams);
		for (int i = 0; i < n; i++)
		{
			gramWeights[i] = GrainBillProportionAdjuster.grams(adjustable.get(i));
		}

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints hc = headerConstraints(0);
		form.add(new JLabel(getUiString("tools.grain.proportion.adjuster.column.grain")), hc);
		hc.gridx = 1;
		form.add(new JLabel(getUiString("tools.grain.proportion.adjuster.column.quantity")), hc);
		hc.gridx = 2;
		form.add(new JLabel(getUiString("tools.grain.proportion.adjuster.column.percent")), hc);

		for (int i = 0; i < n; i++)
		{
			FermentableAddition fa = adjustable.get(i);
			int row = i + 1;

			GridBagConstraints lc = labelConstraints(0, row);
			form.add(new JLabel(fa.getName()), lc);

			Quantity.Unit qtyUnit = fa.getUnit() != null ? fa.getUnit() : Quantity.Unit.KILOGRAMS;
			SwingQuantityEditWidget<WeightUnit> qtyField = new SwingQuantityEditWidget<>(qtyUnit);
			qtyField.setEditable(false);
			quantityFields.add(qtyField);
			GridBagConstraints qc = fieldConstraints(1, row);
			form.add(qtyField, qc);

			SwingQuantityEditWidget<PercentageUnit> pctField =
				new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
			final int rowIndex = i;
			pctField.addQuantityChangeListener(v -> onPercentEdited(rowIndex, v));
			percentFields.add(pctField);
			GridBagConstraints pc = fieldConstraints(2, row);
			form.add(pctField, pc);
		}

		totalLabel = new JLabel();
		GridBagConstraints tc = labelConstraints(0, n + 1);
		tc.gridwidth = 3;
		form.add(totalLabel, tc);

		refreshAllFields();

		JPanel south = new JPanel();
		JButton ok = new JButton(getUiString("ui.ok"));
		JButton cancel = new JButton(getUiString("ui.cancel"));
		south.add(ok);
		south.add(cancel);
		ok.addActionListener(e ->
		{
			output = true;
			dispose();
		});
		cancel.addActionListener(e -> dispose());
		getRootPane().setDefaultButton(ok);

		setLayout(new BorderLayout(8, 8));
		add(new JScrollPane(form), BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(parent);
	}

	private void onPercentEdited(int editedIndex, PercentageUnit newPct)
	{
		if (updating || newPct == null)
		{
			return;
		}
		double newPercent = newPct.get(Quantity.Unit.PERCENTAGE_DISPLAY);
		int counterIndex = GrainBillProportionAdjuster.chooseCounterIndex(editedIndex, adjustable, gramWeights);
		if (counterIndex < 0)
		{
			return;
		}
		GrainBillProportionAdjuster.applyPercentChange(
			editedIndex, counterIndex, newPercent, percents, gramWeights, totalGrams);
		refreshAllFields();
	}

	private void refreshAllFields()
	{
		updating = true;
		try
		{
			int n = adjustable.size();
			for (int i = 0; i < n; i++)
			{
				FermentableAddition fa = adjustable.get(i);
				@SuppressWarnings("unchecked")
				SwingQuantityEditWidget<WeightUnit> qtyField =
					(SwingQuantityEditWidget<WeightUnit>)quantityFields.get(i);
				WeightUnit w = new WeightUnit(gramWeights[i], GRAMS, false);
				qtyField.setQuantity(w);

				percentFields.get(i).setQuantity(new PercentageUnit(percents[i] / 100D));
			}
			double sumPct = 0D;
			for (double p : percents)
			{
				sumPct += p;
			}
			totalLabel.setText(getUiString("tools.grain.proportion.adjuster.total", sumPct));
		}
		finally
		{
			updating = false;
		}
	}

	public List<FermentableAddition> getAdjustableAdditions()
	{
		return adjustable;
	}

	public double[] getGramWeights()
	{
		return gramWeights;
	}

	public boolean getOutput()
	{
		return output;
	}

	public static boolean hasEnoughAdjustableRows(List<FermentableAddition> fermentableAdditions)
	{
		return GrainBillProportionAdjuster.filterAdjustable(fermentableAdditions).size() >= 2;
	}

	private static GridBagConstraints headerConstraints(int col)
	{
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = col;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(4, 8, 8, 8);
		return c;
	}

	private static GridBagConstraints labelConstraints(int col, int row)
	{
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = col;
		c.gridy = row;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(4, 8, 4, 8);
		return c;
	}

	private static GridBagConstraints fieldConstraints(int col, int row)
	{
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = col;
		c.gridy = row;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.insets = new Insets(4, 8, 4, 8);
		return c;
	}
}
