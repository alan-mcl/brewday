package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code AcidifierDialog}.
 */
public class SwingAcidifierDialog extends JDialog
{
	private final SwingQuantityEditWidget<PhUnit> currentMashPh;
	private final SwingQuantityEditWidget<PhUnit> targetMashPh;
	private final JComboBox<String> acid;
	private final SwingQuantityEditWidget<PercentageUnit> acidConcentration;
	private final SwingQuantityEditWidget<VolumeUnit> acidVolume;

	private boolean output;

	public SwingAcidifierDialog(Window parent,
		PhUnit currentPh,
		WaterAddition mashWater,
		List<FermentableAddition> grainBill,
		List<MiscAddition> miscAdditions)
	{
		super(parent, getUiString("tools.acidifier"), Dialog.ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		currentMashPh = new SwingQuantityEditWidget<>(Quantity.Unit.PH);
		currentMashPh.setEditable(false);
		currentMashPh.setQuantity(currentPh);

		targetMashPh = new SwingQuantityEditWidget<>(Quantity.Unit.PH);

		acid = new JComboBox<>();
		List<String> acids = new ArrayList<>();
		for (Misc m : Database.getInstance().getMiscs().values())
		{
			if (m.isAcidAddition() && m.getAcidContent() != null
				&& m.getAcidContent().get(Quantity.Unit.PERCENTAGE) > 0)
			{
				acids.add(m.getName());
			}
		}
		acids.sort(String::compareTo);
		acid.setModel(new DefaultComboBoxModel<>(acids.toArray(String[]::new)));

		acidConcentration = new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		acidConcentration.setEditable(false);

		acidVolume = new SwingQuantityEditWidget<>(Quantity.Unit.MILLILITRES);
		acidVolume.setEditable(false);

		JPanel form = new JPanel(new GridBagLayout());
		int row = 0;
		addRow(form, row++, getUiString("tools.acidifier.current.ph"), currentMashPh);
		addRow(form, row++, getUiString("tools.acidifier.target.ph"), targetMashPh);
		addRow(form, row++, getUiString("tools.acidifier.acid"), acid);
		addRow(form, row++, getUiString("tools.acidifier.acid.concentration"), acidConcentration);
		addRow(form, row++, getUiString("tools.acidifier.acid.volume"), acidVolume);

		targetMashPh.addQuantityChangeListener(v -> recalc(mashWater, grainBill, miscAdditions));
		acid.addActionListener(e ->
		{
			String newV = (String)acid.getSelectedItem();
			if (newV != null)
			{
				Misc misc = Database.getInstance().getMiscs().get(newV);
				if (misc != null)
				{
					acidConcentration.setQuantity(misc.getAcidContent());
				}
				recalc(mashWater, grainBill, miscAdditions);
			}
		});

		if (!acids.isEmpty())
		{
			acid.setSelectedIndex(0);
		}

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

	private void recalc(WaterAddition mashWater, List<FermentableAddition> grainBill, List<MiscAddition> miscAdditions)
	{
		Misc misc = Database.getInstance().getMiscs().get((String)acid.getSelectedItem());
		if (targetMashPh.getQuantity() != null && misc != null)
		{
			Settings.MashPhModel model = Settings.MashPhModel.valueOf(
				Database.getInstance().getSettings().get(Settings.MASH_PH_MODEL));
			VolumeUnit vol = switch (model)
			{
				case EZ_WATER -> Equations.calcMashAcidAdditionEzWater(
					misc, targetMashPh.getQuantity(), mashWater, grainBill, miscAdditions);
				case MPH -> Equations.calcMashAcidAdditionMpH(
					misc, targetMashPh.getQuantity(), mashWater, grainBill, miscAdditions);
			};
			acidVolume.setQuantity(vol);
		}
	}

	public List<MiscAddition> getAcidAdditions()
	{
		Misc misc = Database.getInstance().getMiscs().get((String)acid.getSelectedItem());
		VolumeUnit vol = acidVolume.getQuantity();
		if (misc != null && vol != null && vol.get() > 0)
		{
			return Collections.singletonList(new MiscAddition(misc, vol, acidVolume.getUnit(), new TimeUnit(0)));
		}
		return new ArrayList<>();
	}

	public boolean getOutput()
	{
		return output;
	}
}
