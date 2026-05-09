package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code TargetMashTempDialog}.
 */
public class SwingTargetMashTempDialog extends JDialog
{
	private final SwingQuantityEditWidget<TemperatureUnit> targetTemp;
	private final SwingQuantityEditWidget<TemperatureUnit> waterTemp;
	private boolean output;

	public SwingTargetMashTempDialog(Window parent,
		WaterAddition mashWater,
		List<FermentableAddition> grainBill,
		TemperatureUnit grainTemp)
	{
		super(parent, getUiString("tools.mash.temp"), Dialog.ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		targetTemp = new SwingQuantityEditWidget<>(Quantity.Unit.CELSIUS);
		waterTemp = new SwingQuantityEditWidget<>(Quantity.Unit.CELSIUS);
		waterTemp.setEditable(false);

		JPanel form = new JPanel(new GridBagLayout());
		addRow(form, 0, getUiString("tools.mash.temp.target"), targetTemp);
		addRow(form, 1, getUiString("tools.mash.temp.water.temp"), waterTemp);

		targetTemp.addQuantityChangeListener(v -> recalc(mashWater, grainBill, grainTemp));

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

		setLayout(new BorderLayout());
		add(form, BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(parent);
	}

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

	private void recalc(WaterAddition mashWater, List<FermentableAddition> grainBill, TemperatureUnit grainTemp)
	{
		if (targetTemp.getQuantity() != null)
		{
			TemperatureUnit t = Equations.calcWaterTemp(
				Equations.calcTotalGrainWeight(grainBill),
				mashWater,
				grainTemp,
				targetTemp.getQuantity());
			waterTemp.setQuantity(t);
		}
	}

	public TemperatureUnit getTemp()
	{
		return waterTemp.getQuantity();
	}

	public boolean getOutput()
	{
		return output;
	}
}
