package mclachlan.brewday.ui.swing.widgets;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.Box;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WaterBuilder;
import mclachlan.brewday.math.WaterParameters;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.ui.swing.app.SwingIcons;

import static mclachlan.brewday.math.Quantity.Unit.PPM;
import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing port of JFX {@code WaterBuilderPane}.
 */
public class SwingWaterBuilderPanel extends JPanel
{
	private static final Misc.WaterAdditionFormula[] FORMULAS = new Misc.WaterAdditionFormula[]
	{
		Misc.WaterAdditionFormula.CALCIUM_CARBONATE_UNDISSOLVED,
		Misc.WaterAdditionFormula.CALCIUM_CARBONATE_DISSOLVED,
		Misc.WaterAdditionFormula.CALCIUM_SULPHATE_DIHYDRATE,
		Misc.WaterAdditionFormula.CALCIUM_CHLORIDE_DIHYDRATE,
		Misc.WaterAdditionFormula.MAGNESIUM_SULFATE_HEPTAHYDRATE,
		Misc.WaterAdditionFormula.SODIUM_BICARBONATE,
		Misc.WaterAdditionFormula.SODIUM_CHLORIDE,
		Misc.WaterAdditionFormula.CALCIUM_BICARBONATE,
		Misc.WaterAdditionFormula.MAGNESIUM_CHLORIDE_HEXAHYDRATE
	};

	private final ProcessStep step;
	private final String unspecifiedWater;

	private final JComboBox<String> sourceWaterName = new JComboBox<>();
	private final JComboBox<String> dilutionWaterName = new JComboBox<>();
	private final JComboBox<String> targetWaterName = new JComboBox<>();

	private final SwingQuantityEditWidget<VolumeUnit> sourceVol = new SwingQuantityEditWidget<>(Quantity.Unit.LITRES);
	private final SwingQuantityEditWidget<VolumeUnit> dilutionVol = new SwingQuantityEditWidget<>(Quantity.Unit.LITRES);
	private final SwingQuantityEditWidget<VolumeUnit> targetVol = new SwingQuantityEditWidget<>(Quantity.Unit.LITRES);

	private final SwingQuantityEditWidget<PhUnit> sourcePh = new SwingQuantityEditWidget<>(Quantity.Unit.PH);
	private final SwingQuantityEditWidget<PpmUnit> sourceCa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> sourceMg = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> sourceNa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> sourceSO4 = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> sourceCl = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> sourceHCO3 = new SwingQuantityEditWidget<>(PPM, false);

	private final SwingQuantityEditWidget<PhUnit> dilutionPh = new SwingQuantityEditWidget<>(Quantity.Unit.PH);
	private final SwingQuantityEditWidget<PpmUnit> dilutionCa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> dilutionMg = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> dilutionNa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> dilutionSO4 = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> dilutionCl = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> dilutionHCO3 = new SwingQuantityEditWidget<>(PPM, false);

	private final SwingQuantityEditWidget<PpmUnit> targetMinCa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMinMg = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMinNa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMinSO4 = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMinCl = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMinHCO3 = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMinAlk = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMinRA = new SwingQuantityEditWidget<>(PPM, false);

	private final SwingQuantityEditWidget<PpmUnit> targetMaxCa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMaxMg = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMaxNa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMaxSO4 = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMaxCl = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMaxHCO3 = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMaxAlk = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> targetMaxRA = new SwingQuantityEditWidget<>(PPM, false);

	private final SwingQuantityEditWidget<PpmUnit> resultCa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> resultMg = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> resultNa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> resultSO4 = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> resultCl = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> resultHCO3 = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> resultAlk = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> resultRA = new SwingQuantityEditWidget<>(PPM, false);

	private final SwingQuantityEditWidget<PpmUnit> deltaCa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> deltaMg = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> deltaNa = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> deltaSO4 = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> deltaCl = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> deltaHCO3 = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> deltaAlk = new SwingQuantityEditWidget<>(PPM, false);
	private final SwingQuantityEditWidget<PpmUnit> deltaRA = new SwingQuantityEditWidget<>(PPM, false);
	private final JTextField mse = new JTextField();

	private final Map<Misc.WaterAdditionFormula, JCheckBox> allowedByFormula = new LinkedHashMap<>();
	private final Map<Misc.WaterAdditionFormula, SwingQuantityEditWidget<mclachlan.brewday.math.WeightUnit>> amountByFormula = new LinkedHashMap<>();
	private final Map<Misc.WaterAdditionFormula, JComboBox<String>> miscByFormula = new LinkedHashMap<>();

	private final JComboBox<WaterBuilder.AdditionGoal> goal = new JComboBox<>(WaterBuilder.AdditionGoal.values());
	private final JLabel solverMessage = new JLabel();

	public SwingWaterBuilderPanel(ProcessStep step)
	{
		super(new GridBagLayout());
		this.step = step;
		this.unspecifiedWater = getUiString("tools.water.builder.water.name.none");

		initState();
		buildUi();
		refreshFromDatabase();
		if (step != null)
		{
			init(step.getWaterAdditions());
		}
		bindListeners();
	}

	public void refreshFromDatabase()
	{
		ArrayList<String> waters = new ArrayList<>(Database.getInstance().getWaters().keySet());
		waters.sort(String::compareTo);
		waters.add(0, unspecifiedWater);
		sourceWaterName.setModel(new javax.swing.DefaultComboBoxModel<>(waters.toArray(String[]::new)));
		dilutionWaterName.setModel(new javax.swing.DefaultComboBoxModel<>(waters.toArray(String[]::new)));

		ArrayList<String> waterParams = new ArrayList<>(Database.getInstance().getWaterParameters().keySet());
		waterParams.sort(String::compareTo);
		waterParams.add(0, unspecifiedWater);
		targetWaterName.setModel(new javax.swing.DefaultComboBoxModel<>(waterParams.toArray(String[]::new)));
	}

	public List<MiscAddition> getAdditions()
	{
		List<MiscAddition> result = new ArrayList<>();
		for (Misc.WaterAdditionFormula formula : FORMULAS)
		{
			SwingQuantityEditWidget<mclachlan.brewday.math.WeightUnit> amount = amountByFormula.get(formula);
			JComboBox<String> miscCombo = miscByFormula.get(formula);
			mclachlan.brewday.math.WeightUnit q = amount.getQuantity();
			if (q == null || q.get() <= 0)
			{
				continue;
			}
			String miscName = (String)miscCombo.getSelectedItem();
			if (miscName == null)
			{
				continue;
			}
			Misc misc = Database.getInstance().getMiscs().get(miscName);
			if (misc != null)
			{
				result.add(new MiscAddition(misc, q, amount.getUnit(), new TimeUnit(0)));
			}
		}
		return result;
	}

	public void init(List<WaterAddition> waterAdditions)
	{
		if (waterAdditions == null || waterAdditions.isEmpty())
		{
			return;
		}

		Water startingWater;
		Water dilutionWater = null;
		VolumeUnit startingVolume;
		VolumeUnit dilutionVolume = null;

		if (waterAdditions.size() == 1)
		{
			startingWater = waterAdditions.get(0).getWater();
			startingVolume = waterAdditions.get(0).getVolume();
			sourceWaterName.setSelectedItem(startingWater.getName());
			dilutionWaterName.setSelectedItem(unspecifiedWater);
		}
		else if (waterAdditions.size() == 2)
		{
			startingWater = waterAdditions.get(0).getWater();
			startingVolume = waterAdditions.get(0).getVolume();
			dilutionWater = waterAdditions.get(1).getWater();
			dilutionVolume = waterAdditions.get(1).getVolume();
			sourceWaterName.setSelectedItem(startingWater.getName());
			dilutionWaterName.setSelectedItem(dilutionWater.getName());
		}
		else
		{
			startingWater = new Water();
			startingWater.setCalcium(new PpmUnit(0));
			startingWater.setMagnesium(new PpmUnit(0));
			startingWater.setSodium(new PpmUnit(0));
			startingWater.setSulfate(new PpmUnit(0));
			startingWater.setChloride(new PpmUnit(0));
			startingWater.setBicarbonate(new PpmUnit(0));
			startingWater.setPh(new PhUnit(7));
			startingVolume = new VolumeUnit(0);
			sourceWaterName.setSelectedItem(unspecifiedWater);
			dilutionWaterName.setSelectedItem(unspecifiedWater);
			for (WaterAddition wa : waterAdditions)
			{
				startingWater = Equations.calcCombinedWaterProfile(startingWater, startingVolume, wa.getWater(), wa.getVolume());
				startingVolume = startingVolume.add(wa.getVolume());
			}
		}

		sourceVol.setQuantity(startingVolume);
		sourcePh.setQuantity(startingWater.getPh());
		sourceCa.setQuantity(startingWater.getCalcium());
		sourceMg.setQuantity(startingWater.getMagnesium());
		sourceNa.setQuantity(startingWater.getSodium());
		sourceSO4.setQuantity(startingWater.getSulfate());
		sourceCl.setQuantity(startingWater.getChloride());
		sourceHCO3.setQuantity(startingWater.getBicarbonate());

		if (dilutionWater != null && dilutionVolume != null)
		{
			dilutionVol.setQuantity(dilutionVolume);
			dilutionPh.setQuantity(dilutionWater.getPh());
			dilutionCa.setQuantity(dilutionWater.getCalcium());
			dilutionMg.setQuantity(dilutionWater.getMagnesium());
			dilutionNa.setQuantity(dilutionWater.getSodium());
			dilutionSO4.setQuantity(dilutionWater.getSulfate());
			dilutionCl.setQuantity(dilutionWater.getChloride());
			dilutionHCO3.setQuantity(dilutionWater.getBicarbonate());
		}
	}

	private void initState()
	{
		sourceVol.setQuantity(new VolumeUnit(0));
		dilutionVol.setQuantity(new VolumeUnit(0));
		targetVol.setQuantity(new VolumeUnit(0));
		targetVol.setEditable(false);

		for (SwingQuantityEditWidget<?> widget : List.of(
			resultCa, resultMg, resultNa, resultSO4, resultCl, resultHCO3, resultAlk, resultRA,
			deltaCa, deltaMg, deltaNa, deltaSO4, deltaCl, deltaHCO3, deltaAlk, deltaRA))
		{
			widget.setEditable(false);
		}
		mse.setEditable(false);
		goal.setSelectedItem(WaterBuilder.AdditionGoal.MAXIMISE_ADDITIONS);

		for (Misc.WaterAdditionFormula formula : FORMULAS)
		{
			JCheckBox allowed = new JCheckBox();
			allowed.setSelected(true);
			SwingQuantityEditWidget<mclachlan.brewday.math.WeightUnit> amount =
				new SwingQuantityEditWidget<>(Quantity.Unit.GRAMS);
			amount.setQuantity(new mclachlan.brewday.math.WeightUnit(0, Quantity.Unit.GRAMS));
			JComboBox<String> miscOptions = new JComboBox<>();
			List<String> options = getIngredientOptions(formula);
			miscOptions.setModel(new javax.swing.DefaultComboBoxModel<>(options.toArray(String[]::new)));
			if (miscOptions.getItemCount() > 0)
			{
				miscOptions.setSelectedIndex(0);
			}
			else
			{
				allowed.setSelected(false);
				allowed.setEnabled(false);
				amount.setEnabled(false);
				miscOptions.setEnabled(false);
			}
			allowedByFormula.put(formula, allowed);
			amountByFormula.put(formula, amount);
			miscByFormula.put(formula, miscOptions);
		}
	}

	private void bindListeners()
	{
		sourceVol.addQuantityChangeListener(v -> refreshResultFromWidgets());
		dilutionVol.addQuantityChangeListener(v -> refreshResultFromWidgets());
		for (SwingQuantityEditWidget<?> widget : List.of(sourcePh, sourceCa, sourceMg, sourceNa, sourceSO4, sourceCl, sourceHCO3,
			dilutionPh, dilutionCa, dilutionMg, dilutionNa, dilutionSO4, dilutionCl, dilutionHCO3,
			targetMinCa, targetMinMg, targetMinNa, targetMinSO4, targetMinCl, targetMinHCO3, targetMinAlk, targetMinRA,
			targetMaxCa, targetMaxMg, targetMaxNa, targetMaxSO4, targetMaxCl, targetMaxHCO3, targetMaxAlk, targetMaxRA))
		{
			widget.addQuantityChangeListener(v -> refreshResultFromWidgets());
		}
		for (Misc.WaterAdditionFormula formula : FORMULAS)
		{
			JCheckBox allowed = allowedByFormula.get(formula);
			SwingQuantityEditWidget<mclachlan.brewday.math.WeightUnit> amount = amountByFormula.get(formula);
			allowed.addActionListener(e ->
			{
				amount.setEnabled(allowed.isSelected());
				if (!allowed.isSelected())
				{
					amount.setQuantity(new mclachlan.brewday.math.WeightUnit(0, Quantity.Unit.GRAMS));
				}
				refreshResultFromWidgets();
			});
			amount.addQuantityChangeListener(v -> refreshResultFromWidgets());
		}

		sourceWaterName.addActionListener(e -> onWaterSelection(sourceWaterName, sourcePh, sourceCa, sourceMg, sourceNa, sourceSO4, sourceCl,
			sourceHCO3));
		dilutionWaterName.addActionListener(e -> onWaterSelection(dilutionWaterName, dilutionPh, dilutionCa, dilutionMg, dilutionNa, dilutionSO4,
			dilutionCl, dilutionHCO3));
		targetWaterName.addActionListener(e -> onTargetSelection());
	}

	private void buildUi()
	{
		GridBagConstraints g = new GridBagConstraints();
		g.insets = new Insets(4, 4, 4, 4);
		g.anchor = GridBagConstraints.NORTHWEST;
		g.fill = GridBagConstraints.HORIZONTAL;
		g.weightx = 1.0;
		g.gridx = 0;
		g.gridy = 0;

		JPanel waterSelections = new JPanel(new GridBagLayout());
		waterSelections.setAlignmentX(Component.LEFT_ALIGNMENT);
		waterSelections.setAlignmentY(Component.TOP_ALIGNMENT);
		addSelectionRow(waterSelections, 0, getUiString("tools.water.builder.starting.water"), sourceWaterName);
		addSelectionRow(waterSelections, 1, getUiString("tools.water.builder.dilution.water"), dilutionWaterName);
		addSelectionRow(waterSelections, 2, getUiString("tools.water.builder.target.water"), targetWaterName);
		add(waterSelections, g);

		g.gridy++;
		JPanel waters = buildWatersPanel();
		add(waters, g);

		g.gridy++;
		JPanel additions = buildAdditionsPanel();
		add(additions, g);

		g.gridy++;
		JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
		goal.setToolTipText(getUiString("tools.water.builder.goal.tooltip"));
		JButton solve = new JButton(getUiString("tools.water.builder.solve"),
			SwingIcons.toolbarIcon(SwingIcons.IconKey.GRAPH));
		solve.setToolTipText(getUiString("tools.water.builder.solve.tooltip"));
		solve.addActionListener(e -> solve());
		buttons.add(goal);
		buttons.add(solve);
		buttons.add(solverMessage);
		add(buttons, g);

		g.gridy++;
		g.weighty = 1.0;
		g.fill = GridBagConstraints.NONE;
		add(Box.createVerticalGlue(), g);

		setAlignmentX(Component.LEFT_ALIGNMENT);
		setAlignmentY(Component.TOP_ALIGNMENT);
		wireTooltips();
	}

	private void wireTooltips()
	{
		sourceVol.setToolTipText(getUiString("tools.water.builder.tooltip.source.volume"));
		dilutionVol.setToolTipText(getUiString("tools.water.builder.tooltip.dilution.volume"));
		targetVol.setToolTipText(getUiString("tools.water.builder.tooltip.result.volume"));
		mse.setToolTipText(getUiString("tools.water.builder.mse.tooltip"));

		sourcePh.setToolTipText(getUiString("water.tooltip.ph"));
		dilutionPh.setToolTipText(getUiString("water.tooltip.ph"));
		sourceCa.setToolTipText(getUiString("water.tooltip.calcium"));
		sourceMg.setToolTipText(getUiString("water.tooltip.magnesium"));
		sourceNa.setToolTipText(getUiString("water.tooltip.sodium"));
		sourceSO4.setToolTipText(getUiString("water.tooltip.sulfate"));
		sourceCl.setToolTipText(getUiString("water.tooltip.chloride"));
		sourceHCO3.setToolTipText(getUiString("water.tooltip.bicarbonate"));
		dilutionCa.setToolTipText(getUiString("water.tooltip.calcium"));
		dilutionMg.setToolTipText(getUiString("water.tooltip.magnesium"));
		dilutionNa.setToolTipText(getUiString("water.tooltip.sodium"));
		dilutionSO4.setToolTipText(getUiString("water.tooltip.sulfate"));
		dilutionCl.setToolTipText(getUiString("water.tooltip.chloride"));
		dilutionHCO3.setToolTipText(getUiString("water.tooltip.bicarbonate"));

		targetMinCa.setToolTipText(getUiString("water.parameters.tooltip.min.calcium"));
		targetMaxCa.setToolTipText(getUiString("water.parameters.tooltip.max.calcium"));
		targetMinMg.setToolTipText(getUiString("water.parameters.tooltip.min.magnesium"));
		targetMaxMg.setToolTipText(getUiString("water.parameters.tooltip.max.magnesium"));
		targetMinNa.setToolTipText(getUiString("water.parameters.tooltip.min.sodium"));
		targetMaxNa.setToolTipText(getUiString("water.parameters.tooltip.max.sodium"));
		targetMinSO4.setToolTipText(getUiString("water.parameters.tooltip.min.sulfate"));
		targetMaxSO4.setToolTipText(getUiString("water.parameters.tooltip.max.sulfate"));
		targetMinCl.setToolTipText(getUiString("water.parameters.tooltip.min.chloride"));
		targetMaxCl.setToolTipText(getUiString("water.parameters.tooltip.max.chloride"));
		targetMinHCO3.setToolTipText(getUiString("water.parameters.tooltip.min.bicarbonate"));
		targetMaxHCO3.setToolTipText(getUiString("water.parameters.tooltip.max.bicarbonate"));
		targetMinAlk.setToolTipText(getUiString("water.parameters.tooltip.min.alkalinity"));
		targetMaxAlk.setToolTipText(getUiString("water.parameters.tooltip.max.alkalinity"));
		targetMinRA.setToolTipText(getUiString("water.parameters.tooltip.min.residual.alkalinity"));
		targetMaxRA.setToolTipText(getUiString("water.parameters.tooltip.max.residual.alkalinity"));

		resultCa.setToolTipText(getUiString("water.tooltip.calcium"));
		resultMg.setToolTipText(getUiString("water.tooltip.magnesium"));
		resultNa.setToolTipText(getUiString("water.tooltip.sodium"));
		resultSO4.setToolTipText(getUiString("water.tooltip.sulfate"));
		resultCl.setToolTipText(getUiString("water.tooltip.chloride"));
		resultHCO3.setToolTipText(getUiString("water.tooltip.bicarbonate"));
		resultAlk.setToolTipText(getUiString("water.tooltip.alkalinity"));
		resultRA.setToolTipText(getUiString("water.tooltip.ra"));

		deltaCa.setToolTipText(getUiString("water.tooltip.calcium"));
		deltaMg.setToolTipText(getUiString("water.tooltip.magnesium"));
		deltaNa.setToolTipText(getUiString("water.tooltip.sodium"));
		deltaSO4.setToolTipText(getUiString("water.tooltip.sulfate"));
		deltaCl.setToolTipText(getUiString("water.tooltip.chloride"));
		deltaHCO3.setToolTipText(getUiString("water.tooltip.bicarbonate"));
		deltaAlk.setToolTipText(getUiString("water.tooltip.alkalinity"));
		deltaRA.setToolTipText(getUiString("water.tooltip.ra"));

		for (SwingQuantityEditWidget<mclachlan.brewday.math.WeightUnit> amount : amountByFormula.values())
		{
			amount.setToolTipText(getUiString("tools.water.builder.tooltip.addition.amount"));
		}
	}

	private JPanel buildWatersPanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setAlignmentY(Component.TOP_ALIGNMENT);
		String[] cols = new String[]
		{
			"",
			getUiString("tools.water.builder.water.volume"),
			getUiString("water.ph"),
			getUiString("water.calcium.ppm"),
			getUiString("water.magnesium.ppm"),
			getUiString("water.sodium.ppm"),
			getUiString("water.sulfate.ppm"),
			getUiString("water.chloride.ppm"),
			getUiString("water.bicarbonate.ppm"),
			getUiString("water.alkalinity.abbr"),
			getUiString("water.ra.abbr")
		};
		for (int c = 0; c < cols.length; c++)
		{
			addCell(panel, 0, c, new JLabel(cols[c]));
		}

		addWaterRow(panel, 1, getUiString("tools.water.builder.starting.water"), sourceVol, sourcePh, sourceCa, sourceMg, sourceNa, sourceSO4,
			sourceCl, sourceHCO3, null, null);
		addWaterRow(panel, 2, getUiString("tools.water.builder.dilution.water"), dilutionVol, dilutionPh, dilutionCa, dilutionMg, dilutionNa,
			dilutionSO4, dilutionCl, dilutionHCO3, null, null);

		addCell(panel, 3, 0, new JLabel(getUiString("tools.water.builder.target.water")));
		addCell(panel, 3, 2, new JLabel(getUiString("tools.water.builder.min")));
		addCell(panel, 3, 3, targetMinCa);
		addCell(panel, 3, 4, targetMinMg);
		addCell(panel, 3, 5, targetMinNa);
		addCell(panel, 3, 6, targetMinSO4);
		addCell(panel, 3, 7, targetMinCl);
		addCell(panel, 3, 8, targetMinHCO3);
		addCell(panel, 3, 9, targetMinAlk);
		addCell(panel, 3, 10, targetMinRA);

		addCell(panel, 4, 2, new JLabel(getUiString("tools.water.builder.max")));
		addCell(panel, 4, 3, targetMaxCa);
		addCell(panel, 4, 4, targetMaxMg);
		addCell(panel, 4, 5, targetMaxNa);
		addCell(panel, 4, 6, targetMaxSO4);
		addCell(panel, 4, 7, targetMaxCl);
		addCell(panel, 4, 8, targetMaxHCO3);
		addCell(panel, 4, 9, targetMaxAlk);
		addCell(panel, 4, 10, targetMaxRA);

		addCell(panel, 5, 0, new JLabel(getUiString("tools.water.builder.resulting.water")));
		addCell(panel, 5, 1, targetVol);
		addCell(panel, 5, 3, resultCa);
		addCell(panel, 5, 4, resultMg);
		addCell(panel, 5, 5, resultNa);
		addCell(panel, 5, 6, resultSO4);
		addCell(panel, 5, 7, resultCl);
		addCell(panel, 5, 8, resultHCO3);
		addCell(panel, 5, 9, resultAlk);
		addCell(panel, 5, 10, resultRA);

		addCell(panel, 6, 0, new JLabel(getUiString("tools.water.builder.deltas")));
		addCell(panel, 6, 1, mse);
		addCell(panel, 6, 3, deltaCa);
		addCell(panel, 6, 4, deltaMg);
		addCell(panel, 6, 5, deltaNa);
		addCell(panel, 6, 6, deltaSO4);
		addCell(panel, 6, 7, deltaCl);
		addCell(panel, 6, 8, deltaHCO3);
		addCell(panel, 6, 9, deltaAlk);
		addCell(panel, 6, 10, deltaRA);
		return panel;
	}

	private JPanel buildAdditionsPanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setAlignmentY(Component.TOP_ALIGNMENT);
		int row = 0;
		row = addAdditionRow(panel, row, getUiString("tools.water.builder.dec.ph"), Misc.WaterAdditionFormula.CALCIUM_CHLORIDE_DIHYDRATE);
		row = addAdditionRow(panel, row, getUiString("tools.water.builder.dec.ph"), Misc.WaterAdditionFormula.CALCIUM_SULPHATE_DIHYDRATE);
		row = addAdditionRow(panel, row, getUiString("tools.water.builder.dec.ph"), Misc.WaterAdditionFormula.MAGNESIUM_CHLORIDE_HEXAHYDRATE);
		row = addAdditionRow(panel, row, getUiString("tools.water.builder.dec.ph"), Misc.WaterAdditionFormula.MAGNESIUM_SULFATE_HEPTAHYDRATE);
		row = addAdditionRow(panel, row, getUiString("tools.water.builder.ph.neutral"), Misc.WaterAdditionFormula.SODIUM_CHLORIDE);
		row = addAdditionRow(panel, row, getUiString("tools.water.builder.inc.ph"), Misc.WaterAdditionFormula.SODIUM_BICARBONATE);
		row = addAdditionRow(panel, row, getUiString("tools.water.builder.inc.ph"), Misc.WaterAdditionFormula.CALCIUM_CARBONATE_UNDISSOLVED);
		row = addAdditionRow(panel, row, getUiString("tools.water.builder.inc.ph"), Misc.WaterAdditionFormula.CALCIUM_CARBONATE_DISSOLVED);
		addAdditionRow(panel, row, getUiString("tools.water.builder.inc.ph"), Misc.WaterAdditionFormula.CALCIUM_BICARBONATE);
		return panel;
	}

	private int addAdditionRow(JPanel panel, int row, String moodLabel, Misc.WaterAdditionFormula formula)
	{
		addCell(panel, row, 0, new JLabel(moodLabel));
		addCell(panel, row, 1, allowedByFormula.get(formula));
		addCell(panel, row, 2, new JLabel(getUiString("misc.water.addition.formula." + formula.name())));
		addCell(panel, row, 3, amountByFormula.get(formula));
		addCell(panel, row, 4, miscByFormula.get(formula));
		return row + 1;
	}

	private void addSelectionRow(JPanel panel, int row, String label, JComboBox<String> combo)
	{
		addCell(panel, row, 0, new JLabel(label));
		addCell(panel, row, 1, combo);
	}

	private void addWaterRow(JPanel panel, int row, String title,
		SwingQuantityEditWidget<VolumeUnit> vol,
		SwingQuantityEditWidget<PhUnit> ph,
		SwingQuantityEditWidget<PpmUnit> ca,
		SwingQuantityEditWidget<PpmUnit> mg,
		SwingQuantityEditWidget<PpmUnit> na,
		SwingQuantityEditWidget<PpmUnit> so4,
		SwingQuantityEditWidget<PpmUnit> cl,
		SwingQuantityEditWidget<PpmUnit> hco3,
		SwingQuantityEditWidget<PpmUnit> alk,
		SwingQuantityEditWidget<PpmUnit> ra)
	{
		addCell(panel, row, 0, new JLabel(title));
		addCell(panel, row, 1, vol);
		addCell(panel, row, 2, ph);
		addCell(panel, row, 3, ca);
		addCell(panel, row, 4, mg);
		addCell(panel, row, 5, na);
		addCell(panel, row, 6, so4);
		addCell(panel, row, 7, cl);
		addCell(panel, row, 8, hco3);
		if (alk != null)
		{
			addCell(panel, row, 9, alk);
		}
		if (ra != null)
		{
			addCell(panel, row, 10, ra);
		}
	}

	private void addCell(JPanel panel, int row, int col, java.awt.Component component)
	{
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = col;
		c.gridy = row;
		c.insets = new Insets(2, 3, 2, 3);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = col >= 2 ? 0.8 : 0.2;
		panel.add(component, c);
	}

	private void onWaterSelection(
		JComboBox<String> combo,
		SwingQuantityEditWidget<PhUnit> ph,
		SwingQuantityEditWidget<PpmUnit> ca,
		SwingQuantityEditWidget<PpmUnit> mg,
		SwingQuantityEditWidget<PpmUnit> na,
		SwingQuantityEditWidget<PpmUnit> so4,
		SwingQuantityEditWidget<PpmUnit> cl,
		SwingQuantityEditWidget<PpmUnit> hco3)
	{
		String selected = (String)combo.getSelectedItem();
		if (selected == null || unspecifiedWater.equals(selected))
		{
			return;
		}
		Water water = Database.getInstance().getWaters().get(selected);
		if (water == null)
		{
			return;
		}
		ph.setQuantity(water.getPh());
		ca.setQuantity(water.getCalcium());
		mg.setQuantity(water.getMagnesium());
		na.setQuantity(water.getSodium());
		so4.setQuantity(water.getSulfate());
		cl.setQuantity(water.getChloride());
		hco3.setQuantity(water.getBicarbonate());
		refreshResultFromWidgets();
	}

	private void onTargetSelection()
	{
		String selected = (String)targetWaterName.getSelectedItem();
		if (selected == null || unspecifiedWater.equals(selected))
		{
			return;
		}
		WaterParameters wp = Database.getInstance().getWaterParameters().get(selected);
		if (wp == null)
		{
			return;
		}
		targetMinCa.setQuantity(wp.getMinCalcium());
		targetMinMg.setQuantity(wp.getMinMagnesium());
		targetMinNa.setQuantity(wp.getMinSodium());
		targetMinSO4.setQuantity(wp.getMinSulfate());
		targetMinCl.setQuantity(wp.getMinChloride());
		targetMinHCO3.setQuantity(wp.getMinBicarbonate());
		targetMinAlk.setQuantity(wp.getMinAlkalinity());
		targetMinRA.setQuantity(wp.getMinResidualAlkalinity());
		targetMaxCa.setQuantity(wp.getMaxCalcium());
		targetMaxMg.setQuantity(wp.getMaxMagnesium());
		targetMaxNa.setQuantity(wp.getMaxSodium());
		targetMaxSO4.setQuantity(wp.getMaxSulfate());
		targetMaxCl.setQuantity(wp.getMaxChloride());
		targetMaxHCO3.setQuantity(wp.getMaxBicarbonate());
		targetMaxAlk.setQuantity(wp.getMaxAlkalinity());
		targetMaxRA.setQuantity(wp.getMaxResidualAlkalinity());
		refreshResultFromWidgets();
	}

	private void solve()
	{
		WaterBuilder wb = new WaterBuilder();
		Water startingWater = getStartingWater();
		WaterParameters targetWater = getTargetWater();
		Map<Misc.WaterAdditionFormula, Boolean> allowed = getAllowedAdditions();
		Map<Misc.WaterAdditionFormula, Double> result = wb.calcAdditions(
			startingWater,
			targetWater,
			allowed,
			(WaterBuilder.AdditionGoal)goal.getSelectedItem());

		if (result == null)
		{
			solverMessage.setText(getUiString("tools.water.builder.no.solution"));
			return;
		}

		solverMessage.setText(getUiString("tools.water.builder.found.a.solution"));
		double vol = qValue(targetVol, Quantity.Unit.LITRES);
		for (Misc.WaterAdditionFormula formula : FORMULAS)
		{
			double mgPerL = result.getOrDefault(formula, 0D);
			double grams = mgPerL * vol / 1000D;
			amountByFormula.get(formula).setQuantity(new mclachlan.brewday.math.WeightUnit(grams, Quantity.Unit.GRAMS));
		}
		refreshResultFromWidgets();
	}

	private Map<Misc.WaterAdditionFormula, Boolean> getAllowedAdditions()
	{
		Map<Misc.WaterAdditionFormula, Boolean> result = new HashMap<>();
		for (Misc.WaterAdditionFormula formula : FORMULAS)
		{
			result.put(formula, allowedByFormula.get(formula).isSelected());
		}
		return result;
	}

	private void refreshResultFromWidgets()
	{
		WaterBuilder wb = new WaterBuilder();
		Water start = getStartingWater();
		WaterParameters target = getTargetWater();
		double vol = qValue(targetVol, Quantity.Unit.LITRES);

		double caCo3Un = qValue(amountByFormula.get(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_UNDISSOLVED), Quantity.Unit.GRAMS);
		double caCo3Dis = qValue(amountByFormula.get(Misc.WaterAdditionFormula.CALCIUM_CARBONATE_DISSOLVED), Quantity.Unit.GRAMS);
		double caSo4 = qValue(amountByFormula.get(Misc.WaterAdditionFormula.CALCIUM_SULPHATE_DIHYDRATE), Quantity.Unit.GRAMS);
		double caCl = qValue(amountByFormula.get(Misc.WaterAdditionFormula.CALCIUM_CHLORIDE_DIHYDRATE), Quantity.Unit.GRAMS);
		double mgSo4 = qValue(amountByFormula.get(Misc.WaterAdditionFormula.MAGNESIUM_SULFATE_HEPTAHYDRATE), Quantity.Unit.GRAMS);
		double naHco3 = qValue(amountByFormula.get(Misc.WaterAdditionFormula.SODIUM_BICARBONATE), Quantity.Unit.GRAMS);
		double naCl = qValue(amountByFormula.get(Misc.WaterAdditionFormula.SODIUM_CHLORIDE), Quantity.Unit.GRAMS);
		double caHco3 = qValue(amountByFormula.get(Misc.WaterAdditionFormula.CALCIUM_BICARBONATE), Quantity.Unit.GRAMS);
		double mgCl = qValue(amountByFormula.get(Misc.WaterAdditionFormula.MAGNESIUM_CHLORIDE_HEXAHYDRATE), Quantity.Unit.GRAMS);

		Water resultWater = wb.buildWater(start, new VolumeUnit(vol, Quantity.Unit.LITRES, false),
			caCo3Un, caCo3Dis, caSo4, caCl, mgSo4, naHco3, naCl, caHco3, mgCl);

		resultCa.setQuantity(resultWater.getCalcium());
		resultMg.setQuantity(resultWater.getMagnesium());
		resultNa.setQuantity(resultWater.getSodium());
		resultSO4.setQuantity(resultWater.getSulfate());
		resultCl.setQuantity(resultWater.getChloride());
		resultHCO3.setQuantity(resultWater.getBicarbonate());
		resultAlk.setQuantity(resultWater.getAlkalinity());
		resultRA.setQuantity(resultWater.getResidualAlkalinity());

		double dca = getDelta(resultWater.getCalcium().get(PPM), qValue(targetMinCa, PPM), qValue(targetMaxCa, PPM));
		double dmg = getDelta(resultWater.getMagnesium().get(PPM), qValue(targetMinMg, PPM), qValue(targetMaxMg, PPM));
		double dna = getDelta(resultWater.getSodium().get(PPM), qValue(targetMinNa, PPM), qValue(targetMaxNa, PPM));
		double dso4 = getDelta(resultWater.getSulfate().get(PPM), qValue(targetMinSO4, PPM), qValue(targetMaxSO4, PPM));
		double dcl = getDelta(resultWater.getChloride().get(PPM), qValue(targetMinCl, PPM), qValue(targetMaxCl, PPM));
		double dhco3 = getDelta(resultWater.getBicarbonate().get(PPM), qValue(targetMinHCO3, PPM), qValue(targetMaxHCO3, PPM));
		double dalk = getDelta(resultWater.getAlkalinity().get(PPM), qValue(targetMinAlk, PPM), qValue(targetMaxAlk, PPM));
		double dra = getDelta(resultWater.getResidualAlkalinity().get(PPM), qValue(targetMinRA, PPM), qValue(targetMaxRA, PPM));

		deltaCa.setQuantity(new PpmUnit(dca, false));
		deltaMg.setQuantity(new PpmUnit(dmg, false));
		deltaNa.setQuantity(new PpmUnit(dna, false));
		deltaSO4.setQuantity(new PpmUnit(dso4, false));
		deltaCl.setQuantity(new PpmUnit(dcl, false));
		deltaHCO3.setQuantity(new PpmUnit(dhco3, false));
		deltaAlk.setQuantity(new PpmUnit(dalk, false));
		deltaRA.setQuantity(new PpmUnit(dra, false));

		double mseValue = (Math.pow(dca, 2) + Math.pow(dmg, 2) + Math.pow(dna, 2) + Math.pow(dso4, 2)
			+ Math.pow(dcl, 2) + Math.pow(dhco3, 2) + Math.pow(dalk, 2) + Math.pow(dra, 2)) / 8D;
		mse.setText(String.format(getUiString("tools.water.builder.mse"), mseValue));
	}

	private Water getStartingWater()
	{
		Water source = new Water();
		source.setPh(quantityOrZero(sourcePh, Quantity.Unit.PH, PhUnit.class));
		source.setCalcium(quantityOrZero(sourceCa, PPM, PpmUnit.class));
		source.setMagnesium(quantityOrZero(sourceMg, PPM, PpmUnit.class));
		source.setSodium(quantityOrZero(sourceNa, PPM, PpmUnit.class));
		source.setSulfate(quantityOrZero(sourceSO4, PPM, PpmUnit.class));
		source.setChloride(quantityOrZero(sourceCl, PPM, PpmUnit.class));
		source.setBicarbonate(quantityOrZero(sourceHCO3, PPM, PpmUnit.class));

		Water dilution = new Water();
		dilution.setPh(quantityOrZero(dilutionPh, Quantity.Unit.PH, PhUnit.class));
		dilution.setCalcium(quantityOrZero(dilutionCa, PPM, PpmUnit.class));
		dilution.setMagnesium(quantityOrZero(dilutionMg, PPM, PpmUnit.class));
		dilution.setSodium(quantityOrZero(dilutionNa, PPM, PpmUnit.class));
		dilution.setSulfate(quantityOrZero(dilutionSO4, PPM, PpmUnit.class));
		dilution.setChloride(quantityOrZero(dilutionCl, PPM, PpmUnit.class));
		dilution.setBicarbonate(quantityOrZero(dilutionHCO3, PPM, PpmUnit.class));

		VolumeUnit sourceVolume = quantityOrZero(sourceVol, Quantity.Unit.LITRES, VolumeUnit.class);
		VolumeUnit dilutionVolume = quantityOrZero(dilutionVol, Quantity.Unit.LITRES, VolumeUnit.class);
		VolumeUnit combined = sourceVolume.add(dilutionVolume);
		targetVol.setQuantity(combined);

		if (dilutionVolume.get(Quantity.Unit.LITRES) > 0)
		{
			return Equations.calcCombinedWaterProfile(source, sourceVolume, dilution, dilutionVolume);
		}
		return source;
	}

	private WaterParameters getTargetWater()
	{
		WaterParameters target = new WaterParameters();
		target.setMinCalcium(quantityOrZero(targetMinCa, PPM, PpmUnit.class));
		target.setMinMagnesium(quantityOrZero(targetMinMg, PPM, PpmUnit.class));
		target.setMinSodium(quantityOrZero(targetMinNa, PPM, PpmUnit.class));
		target.setMinSulfate(quantityOrZero(targetMinSO4, PPM, PpmUnit.class));
		target.setMinChloride(quantityOrZero(targetMinCl, PPM, PpmUnit.class));
		target.setMinBicarbonate(quantityOrZero(targetMinHCO3, PPM, PpmUnit.class));
		target.setMinAlkalinity(quantityOrZero(targetMinAlk, PPM, PpmUnit.class));
		target.setMinResidualAlkalinity(quantityOrZero(targetMinRA, PPM, PpmUnit.class));
		target.setMaxCalcium(quantityOrZero(targetMaxCa, PPM, PpmUnit.class));
		target.setMaxMagnesium(quantityOrZero(targetMaxMg, PPM, PpmUnit.class));
		target.setMaxSodium(quantityOrZero(targetMaxNa, PPM, PpmUnit.class));
		target.setMaxSulfate(quantityOrZero(targetMaxSO4, PPM, PpmUnit.class));
		target.setMaxChloride(quantityOrZero(targetMaxCl, PPM, PpmUnit.class));
		target.setMaxBicarbonate(quantityOrZero(targetMaxHCO3, PPM, PpmUnit.class));
		target.setMaxAlkalinity(quantityOrZero(targetMaxAlk, PPM, PpmUnit.class));
		target.setMaxResidualAlkalinity(quantityOrZero(targetMaxRA, PPM, PpmUnit.class));
		return target;
	}

	private List<String> getIngredientOptions(Misc.WaterAdditionFormula formula)
	{
		List<String> result = new ArrayList<>();
		for (Misc misc : Database.getInstance().getMiscs().values())
		{
			if (formula == misc.getWaterAdditionFormula())
			{
				result.add(misc.getName());
			}
		}
		result.sort(Comparator.comparing(String::toString));
		return result;
	}

	private double getDelta(double current, double min, double max)
	{
		if (current < min)
		{
			return current - min;
		}
		else if (current > max)
		{
			return max - current;
		}
		return 0;
	}

	private double qValue(SwingQuantityEditWidget<? extends Quantity> widget, Quantity.Unit unit)
	{
		Quantity q = widget.getQuantity();
		return q == null ? 0D : q.get(unit);
	}

	@SuppressWarnings("unchecked")
	private <T extends Quantity> T quantityOrZero(
		SwingQuantityEditWidget<? extends Quantity> widget,
		Quantity.Unit unit,
		Class<T> cls)
	{
		Quantity q = widget.getQuantity();
		if (q != null)
		{
			return (T)q;
		}
		return (T)Quantity.parseQuantity("0", unit);
	}
}
