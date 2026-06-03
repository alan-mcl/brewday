package mclachlan.brewday.ui.swing.widgets;

import alphanum.AlphanumComparator;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.CarbonationUnit;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.FermentationCalculator;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.ProcessLog;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.process.Volumes;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.dialogs.SwingCarbonationCalculatorDialog;
import mclachlan.brewday.ui.UiUtils;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code PackagePane}.
 * <p>
 * Carbonation-specific fields live in a {@link SwingCardStack} (one card per
 * {@link PackageStep.CarbonationMethod}). The packaged beer's output-volume name
 * is edited via the in-tile Rename action on the computed-volume pane.
 */
public class SwingPackagePane extends SwingProcessStepPane<PackageStep>
{
	private JComboBox<String> style;
	private JComboBox<PackageStep.PackagingType> packagingType;
	private JComboBox<PackageStep.CarbonationMethod> carbonationMethod;
	private JLabel combinationWarning;
	private SwingCardStack carbonationCards;
	private SwingQuantityEditWidget<CarbonationUnit> forcedCarbonation;
	private JComboBox<String> speiseVolumeCombo;
	private JComboBox<String> krausenRecipeCombo;
	private JComboBox<String> krausenVolumeCombo;
	private SwingQuantityEditWidget<DensityUnit> predictedFinalGravity;

	/** recipe from last {@link #refreshInternal} — used by listeners */
	private Recipe paneRecipe;

	public SwingPackagePane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addIngredientButtonsForPrototype(new PackageStep());

		JButton carbonationCalculator = new JButton(SwingIcons.toolbarIcon(SwingIcons.IconKey.PACKAGE));
		carbonationCalculator.setToolTipText(getUiString("package.calc.button.tooltip"));
		carbonationCalculator.addActionListener(e -> runCarbonationCalculator());
		getStepToolbar().add(carbonationCalculator);

		addInputVolumeComboBox("volumes.in",
			PackageStep::getInputVolume,
			PackageStep::setInputVolume,
			Volume.Type.BEER);

		style = new JComboBox<>();
		addLabeledWidgetToForm("recipe.style", style);

		packagingType = new JComboBox<>(PackageStep.PackagingType.values());
		addLabeledWidgetToForm("package.type", packagingType);

		carbonationMethod = new JComboBox<>();
		addLabeledWidgetToForm("package.carbonation.method", carbonationMethod);

		combinationWarning = new JLabel(" ");
		combinationWarning.setVisible(false);
		addFullWidthComponentRow(combinationWarning);

		carbonationCards = new SwingCardStack();
		buildCarbonationCards();
		addFullWidthComponentRow(carbonationCards);

		addVolumeUnitControl("package.loss",
			PackageStep::getPackagingLoss, PackageStep::setPackagingLoss, Quantity.Unit.LITRES);

		packagingType.addActionListener(e -> onPackagingTypeChanged());

		carbonationMethod.addActionListener(e -> onCarbonationMethodChanged());

		style.addActionListener(e ->
		{
			PackageStep s = getStepForTest();
			if (s == null || isStepPaneRefreshing())
			{
				return;
			}
			String newValue = (String)style.getSelectedItem();
			if (newValue != null)
			{
				s.setStyleId(newValue);
				dirtyState.markDirty(s);
			}
		});

		addComputedVolumePane("volumes.out", PackageStep::getOutputVolume);
	}

	/*-------------------------------------------------------------------------*/
	private void buildCarbonationCards()
	{
		forcedCarbonation = new SwingQuantityEditWidget<>(Quantity.Unit.VOLUMES);
		getUnitControlUtils().registerQuantityEdit(forcedCarbonation, PackageStep::getForcedCarbonation,
			PackageStep::setForcedCarbonation);
		carbonationCards.addCard(
			PackageStep.CarbonationMethod.FORCE_CARB.name(),
			buildForceCarbCard());

		JPanel primingCard = new JPanel(new GridBagLayout());
		primingCard.setBorder(BorderFactory.createTitledBorder(
			getUiString("package.card.priming")));
		JLabel primingHint = new JLabel(getUiString("package.card.priming.hint"));
		GridBagConstraints hintGbc = new GridBagConstraints();
		hintGbc.gridx = 0;
		hintGbc.gridy = 0;
		hintGbc.weightx = 1.0;
		hintGbc.anchor = GridBagConstraints.WEST;
		hintGbc.insets = new Insets(4, 6, 4, 6);
		hintGbc.fill = GridBagConstraints.HORIZONTAL;
		primingCard.add(primingHint, hintGbc);
		carbonationCards.addCard(PackageStep.CarbonationMethod.PRIMING_SUGAR.name(), primingCard);

		speiseVolumeCombo = new JComboBox<>();
		speiseVolumeCombo.addActionListener(e -> onSpeiseVolumeChanged());
		carbonationCards.addCard(
			PackageStep.CarbonationMethod.SPEISE.name(),
			buildSpeiseCard());

		Quantity.Unit densityUnit = Database.getInstance().getSettings().getUnitForStepAndIngredient(
			Quantity.Type.FLUID_DENSITY, ProcessStep.Type.PACKAGE, IngredientAddition.Type.FERMENTABLES);
		predictedFinalGravity = new SwingQuantityEditWidget<>(densityUnit);
		predictedFinalGravity.setEditable(false);
		carbonationCards.addCard(
			PackageStep.CarbonationMethod.SPUNDING.name(),
			buildSpundingCard());

		krausenRecipeCombo = new JComboBox<>();
		krausenRecipeCombo.addActionListener(e -> onKrausenRecipeChanged());
		krausenVolumeCombo = new JComboBox<>();
		krausenVolumeCombo.addActionListener(e -> onKrausenVolumeChanged());
		carbonationCards.addCard(
			PackageStep.CarbonationMethod.KRAUSENING.name(),
			buildKrausenCard());
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildForceCarbCard()
	{
		JPanel card = new JPanel(new GridBagLayout());
		card.setBorder(BorderFactory.createTitledBorder(
			getUiString("package.card.force.carb")));

		JLabel label = new JLabel(getUiString("package.forced.carbonation") + ":");
		applyLabelTooltip("package.forced.carbonation", label);
		applyLabelTooltip("package.forced.carbonation", forcedCarbonation);

		GridBagConstraints labelGbc = new GridBagConstraints();
		labelGbc.gridx = 0;
		labelGbc.gridy = 0;
		labelGbc.anchor = GridBagConstraints.WEST;
		labelGbc.insets = new Insets(4, 6, 4, 4);
		card.add(label, labelGbc);

		GridBagConstraints widgetGbc = new GridBagConstraints();
		widgetGbc.gridx = 1;
		widgetGbc.gridy = 0;
		widgetGbc.weightx = 1.0;
		widgetGbc.fill = GridBagConstraints.HORIZONTAL;
		widgetGbc.insets = new Insets(4, 4, 4, 6);
		card.add(forcedCarbonation, widgetGbc);

		return card;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildSpeiseCard()
	{
		JPanel card = new JPanel(new GridBagLayout());
		card.setBorder(BorderFactory.createTitledBorder(
			getUiString("package.card.speise")));

		JLabel label = new JLabel(getUiString("package.speise.volume") + ":");
		applyLabelTooltip("package.speise.volume", label);
		applyLabelTooltip("package.speise.volume", speiseVolumeCombo);

		GridBagConstraints labelGbc = new GridBagConstraints();
		labelGbc.gridx = 0;
		labelGbc.gridy = 0;
		labelGbc.anchor = GridBagConstraints.WEST;
		labelGbc.insets = new Insets(4, 6, 4, 4);
		card.add(label, labelGbc);

		GridBagConstraints widgetGbc = new GridBagConstraints();
		widgetGbc.gridx = 1;
		widgetGbc.gridy = 0;
		widgetGbc.weightx = 1.0;
		widgetGbc.fill = GridBagConstraints.HORIZONTAL;
		widgetGbc.insets = new Insets(4, 4, 4, 6);
		card.add(speiseVolumeCombo, widgetGbc);

		return card;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildSpundingCard()
	{
		JPanel card = new JPanel(new GridBagLayout());
		card.setBorder(BorderFactory.createTitledBorder(
			getUiString("package.card.spunding")));

		JLabel label = new JLabel(getUiString("package.spunding.predicted.fg") + ":");
		applyLabelTooltip("package.spunding.predicted.fg", label);
		applyLabelTooltip("package.spunding.predicted.fg", predictedFinalGravity);

		GridBagConstraints labelGbc = new GridBagConstraints();
		labelGbc.gridx = 0;
		labelGbc.gridy = 0;
		labelGbc.anchor = GridBagConstraints.WEST;
		labelGbc.insets = new Insets(4, 6, 4, 4);
		card.add(label, labelGbc);

		GridBagConstraints widgetGbc = new GridBagConstraints();
		widgetGbc.gridx = 1;
		widgetGbc.gridy = 0;
		widgetGbc.weightx = 1.0;
		widgetGbc.fill = GridBagConstraints.HORIZONTAL;
		widgetGbc.insets = new Insets(4, 4, 4, 6);
		card.add(predictedFinalGravity, widgetGbc);

		return card;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel buildKrausenCard()
	{
		JPanel card = new JPanel(new GridBagLayout());
		card.setBorder(BorderFactory.createTitledBorder(
			getUiString("package.card.krausening")));

		JLabel recipeLabel = new JLabel(getUiString("package.krausen.recipe") + ":");
		applyLabelTooltip("package.krausen.recipe", recipeLabel);
		applyLabelTooltip("package.krausen.recipe", krausenRecipeCombo);

		GridBagConstraints recipeLabelGbc = new GridBagConstraints();
		recipeLabelGbc.gridx = 0;
		recipeLabelGbc.gridy = 0;
		recipeLabelGbc.anchor = GridBagConstraints.WEST;
		recipeLabelGbc.insets = new Insets(4, 6, 4, 4);
		card.add(recipeLabel, recipeLabelGbc);

		GridBagConstraints recipeWidgetGbc = new GridBagConstraints();
		recipeWidgetGbc.gridx = 1;
		recipeWidgetGbc.gridy = 0;
		recipeWidgetGbc.weightx = 1.0;
		recipeWidgetGbc.fill = GridBagConstraints.HORIZONTAL;
		recipeWidgetGbc.insets = new Insets(4, 4, 4, 6);
		card.add(krausenRecipeCombo, recipeWidgetGbc);

		JLabel volumeLabel = new JLabel(getUiString("package.krausen.volume") + ":");
		applyLabelTooltip("package.krausen.volume", volumeLabel);
		applyLabelTooltip("package.krausen.volume", krausenVolumeCombo);

		GridBagConstraints volLabelGbc = new GridBagConstraints();
		volLabelGbc.gridx = 0;
		volLabelGbc.gridy = 1;
		volLabelGbc.anchor = GridBagConstraints.WEST;
		volLabelGbc.insets = new Insets(4, 6, 4, 4);
		card.add(volumeLabel, volLabelGbc);

		GridBagConstraints volWidgetGbc = new GridBagConstraints();
		volWidgetGbc.gridx = 1;
		volWidgetGbc.gridy = 1;
		volWidgetGbc.weightx = 1.0;
		volWidgetGbc.fill = GridBagConstraints.HORIZONTAL;
		volWidgetGbc.insets = new Insets(4, 4, 4, 6);
		card.add(krausenVolumeCombo, volWidgetGbc);

		return card;
	}

	/*-------------------------------------------------------------------------*/
	private void onPackagingTypeChanged()
	{
		PackageStep s = getStepForTest();
		if (s == null || isStepPaneRefreshing())
		{
			return;
		}
		Object sel = packagingType.getSelectedItem();
		if (!(sel instanceof PackageStep.PackagingType t))
		{
			return;
		}
		s.setPackagingType(t);
		if (t == PackageStep.PackagingType.BOTTLE)
		{
			clearCarbonationMethodProperties(s);
			s.setCarbonationMethod(PackageStep.CarbonationMethod.PRIMING_SUGAR);
			carbonationMethod.setSelectedItem(PackageStep.CarbonationMethod.PRIMING_SUGAR);
		}
		else if (t == PackageStep.PackagingType.CASK
			&& s.getCarbonationMethod() == PackageStep.CarbonationMethod.FORCE_CARB)
		{
			clearCarbonationMethodProperties(s);
			s.setCarbonationMethod(PackageStep.CarbonationMethod.PRIMING_SUGAR);
			carbonationMethod.setSelectedItem(PackageStep.CarbonationMethod.PRIMING_SUGAR);
		}
		refreshCarbonationMethodCombo(t);
		syncCarbonationUi(s, paneRecipe);
		dirtyState.markDirty(s);
	}

	/*-------------------------------------------------------------------------*/
	private void onCarbonationMethodChanged()
	{
		PackageStep s = getStepForTest();
		if (s == null || isStepPaneRefreshing())
		{
			return;
		}
		Object sel = carbonationMethod.getSelectedItem();
		if (!(sel instanceof PackageStep.CarbonationMethod m))
		{
			return;
		}
		clearCarbonationMethodProperties(s);
		s.setCarbonationMethod(m);
		syncCarbonationUi(s, paneRecipe);
		dirtyState.markDirty(s);
	}

	/*-------------------------------------------------------------------------*/
	private void onSpeiseVolumeChanged()
	{
		PackageStep s = getStepForTest();
		if (s == null || isStepPaneRefreshing())
		{
			return;
		}
		String selected = (String)speiseVolumeCombo.getSelectedItem();
		if (UiUtils.NONE.equals(selected))
		{
			s.setSpeiseVolume(null);
		}
		else
		{
			s.setSpeiseVolume(selected);
		}
		dirtyState.markDirty(s);
	}

	/*-------------------------------------------------------------------------*/
	private void onKrausenRecipeChanged()
	{
		PackageStep s = getStepForTest();
		if (s == null || isStepPaneRefreshing())
		{
			return;
		}
		String selected = (String)krausenRecipeCombo.getSelectedItem();
		if (UiUtils.NONE.equals(selected))
		{
			s.setKrausenRecipeName(null);
			s.setKrausenVolumeName(null);
		}
		else
		{
			s.setKrausenRecipeName(selected);
			s.setKrausenVolumeName(null);
		}
		refreshKrausenVolumeCombo(s);
		dirtyState.markDirty(s);
	}

	/*-------------------------------------------------------------------------*/
	private void onKrausenVolumeChanged()
	{
		PackageStep s = getStepForTest();
		if (s == null || isStepPaneRefreshing())
		{
			return;
		}
		String selected = (String)krausenVolumeCombo.getSelectedItem();
		if (UiUtils.NONE.equals(selected))
		{
			s.setKrausenVolumeName(null);
		}
		else
		{
			s.setKrausenVolumeName(selected);
		}
		dirtyState.markDirty(s);
	}

	/*-------------------------------------------------------------------------*/
	private void runCarbonationCalculator()
	{
		PackageStep s = getStepForTest();
		if (s == null || paneRecipe == null)
		{
			return;
		}
		java.awt.Window parent = SwingUtilities.getWindowAncestor(this);
		SwingCarbonationCalculatorDialog d = new SwingCarbonationCalculatorDialog(
			parent, s, paneRecipe);
		d.setVisible(true);
		if (d.getOutput())
		{
			SwingCarbonationCalculatorDialog.applyResult(s, d.getResult());
			dirtyState.markDirty(s);
			syncCarbonationUi(s, paneRecipe);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void refreshCarbonationMethodCombo(PackageStep.PackagingType packaging)
	{
		List<PackageStep.CarbonationMethod> methods = new ArrayList<>();
		for (PackageStep.CarbonationMethod m : PackageStep.CarbonationMethod.values())
		{
			if (packaging == PackageStep.PackagingType.CASK
				&& m == PackageStep.CarbonationMethod.FORCE_CARB)
			{
				continue;
			}
			methods.add(m);
		}
		PackageStep s = getStepForTest();
		PackageStep.CarbonationMethod current = s == null
			? null
			: s.getCarbonationMethod();
		carbonationMethod.setModel(new DefaultComboBoxModel<>(
			methods.toArray(PackageStep.CarbonationMethod[]::new)));
		if (current != null && methods.contains(current))
		{
			carbonationMethod.setSelectedItem(current);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void clearCarbonationMethodProperties(PackageStep step)
	{
		step.setForcedCarbonation(null);
		step.setSpeiseVolume(null);
		step.setKrausenRecipeName(null);
		step.setKrausenVolumeName(null);
	}

	/*-------------------------------------------------------------------------*/
	private void syncCarbonationUi(PackageStep step, Recipe recipe)
	{
		if (step == null)
		{
			return;
		}

		PackageStep.CarbonationMethod method = step.getCarbonationMethod();
		if (method == null)
		{
			method = PackageStep.CarbonationMethod.PRIMING_SUGAR;
		}

		updateCombinationWarning(step.getPackagingType(), method);
		carbonationCards.setVisibleCard(method.name());

		if (method == PackageStep.CarbonationMethod.FORCE_CARB)
		{
			forcedCarbonation.setQuantity(step.getForcedCarbonation());
		}

		refreshSpeiseCombo(step, recipe);

		if (method == PackageStep.CarbonationMethod.SPUNDING)
		{
			refreshPredictedFinalGravity(step, recipe);
		}

		if (method == PackageStep.CarbonationMethod.KRAUSENING)
		{
			refreshKrausenCombos(step);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void refreshKrausenCombos(PackageStep step)
	{
		if (krausenRecipeCombo == null)
		{
			return;
		}

		DefaultComboBoxModel<String> recipeModel = buildKrausenRecipeModel();
		krausenRecipeCombo.setModel(recipeModel);
		String curRecipe = step == null ? null : step.getKrausenRecipeName();
		if (curRecipe == null || !modelContains(recipeModel, curRecipe))
		{
			krausenRecipeCombo.setSelectedItem(UiUtils.NONE);
		}
		else
		{
			krausenRecipeCombo.setSelectedItem(curRecipe);
		}

		refreshKrausenVolumeCombo(step);
	}

	/*-------------------------------------------------------------------------*/
	private void refreshKrausenVolumeCombo(PackageStep step)
	{
		if (krausenVolumeCombo == null)
		{
			return;
		}

		String recipeName = (String)krausenRecipeCombo.getSelectedItem();
		DefaultComboBoxModel<String> volumeModel = buildKrausenVolumeModel(recipeName);
		krausenVolumeCombo.setModel(volumeModel);
		String curVol = step == null ? null : step.getKrausenVolumeName();
		if (curVol == null || !modelContains(volumeModel, curVol))
		{
			krausenVolumeCombo.setSelectedItem(UiUtils.NONE);
		}
		else
		{
			krausenVolumeCombo.setSelectedItem(curVol);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void refreshSpeiseCombo(PackageStep step, Recipe recipe)
	{
		if (speiseVolumeCombo == null)
		{
			return;
		}
		if (recipe == null)
		{
			speiseVolumeCombo.setModel(new DefaultComboBoxModel<>(new String[] {UiUtils.NONE}));
			return;
		}

		DefaultComboBoxModel<String> model = buildWortVolumeModel(recipe);
		speiseVolumeCombo.setModel(model);
		String cur = step == null ? null : step.getSpeiseVolume();
		if (cur == null || !modelContains(model, cur))
		{
			speiseVolumeCombo.setSelectedItem(UiUtils.NONE);
		}
		else
		{
			speiseVolumeCombo.setSelectedItem(cur);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void refreshPredictedFinalGravity(PackageStep step, Recipe recipe)
	{
		if (predictedFinalGravity == null || step == null
			|| recipe == null || step.getInputVolume() == null)
		{
			return;
		}
		Volume beer = recipe.getVolumes().getVolume(step.getInputVolume());
		if (beer == null)
		{
			return;
		}
		DensityUnit predicted = FermentationCalculator.calcPredictedTerminalFg(
			beer,
			step.getYeastAdditions(),
			new ProcessLog());
		predictedFinalGravity.setQuantity(predicted);
	}

	/*-------------------------------------------------------------------------*/
	private void updateCombinationWarning(
		PackageStep.PackagingType packaging,
		PackageStep.CarbonationMethod method)
	{
		String key = combinationWarningKey(packaging, method);
		if (key == null)
		{
			combinationWarning.setText(" ");
			combinationWarning.setVisible(false);
		}
		else
		{
			combinationWarning.setText(getUiString(key));
			combinationWarning.setVisible(true);
		}
	}

	/*-------------------------------------------------------------------------*/
	static String combinationWarningKey(
		PackageStep.PackagingType packaging,
		PackageStep.CarbonationMethod method)
	{
		if (packaging != PackageStep.PackagingType.BOTTLE || method == null)
		{
			return null;
		}
		return switch (method)
		{
			case FORCE_CARB -> "package.ui.warn.bottle.force.carb";
			case SPEISE -> "package.ui.warn.bottle.speise";
			case SPUNDING -> "package.ui.warn.bottle.spunding";
			case KRAUSENING -> "package.ui.warn.bottle.krausening";
			case PRIMING_SUGAR -> null;
		};
	}

	/*-------------------------------------------------------------------------*/
	private static DefaultComboBoxModel<String> buildKrausenRecipeModel()
	{
		List<String> names = new ArrayList<>(Database.getInstance().getRecipes().keySet());
		Collections.sort(names);
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		model.addElement(UiUtils.NONE);
		for (String n : names)
		{
			model.addElement(n);
		}
		return model;
	}

	/*-------------------------------------------------------------------------*/
	private static DefaultComboBoxModel<String> buildKrausenVolumeModel(String recipeName)
	{
		DefaultComboBoxModel<String> empty = new DefaultComboBoxModel<>();
		empty.addElement(UiUtils.NONE);
		if (recipeName == null || UiUtils.NONE.equals(recipeName))
		{
			return empty;
		}

		Recipe source = Database.getInstance().getRecipes().get(recipeName);
		if (source == null)
		{
			return empty;
		}

		EquipmentProfile equipment = Database.getInstance().getEquipmentProfiles()
			.get(source.getEquipmentProfile());
		if (equipment == null)
		{
			return empty;
		}

		Volumes tmpVolumes = new Volumes();
		ProcessLog tmpLog = new ProcessLog();
		source.run(tmpVolumes, equipment, tmpLog);

		List<String> names = new ArrayList<>();
		for (Volume vol : tmpVolumes.getVolumes().values())
		{
			if (vol.getType() == Volume.Type.WORT || vol.getType() == Volume.Type.BEER)
			{
				names.add(vol.getName());
			}
		}
		Collections.sort(names);
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		model.addElement(UiUtils.NONE);
		for (String n : names)
		{
			model.addElement(n);
		}
		return model;
	}

	/*-------------------------------------------------------------------------*/
	private static DefaultComboBoxModel<String> buildWortVolumeModel(Recipe recipe)
	{
		List<String> names = new ArrayList<>();
		for (Volume vol : recipe.getVolumes().getVolumes().values())
		{
			if (vol.getType() == Volume.Type.WORT)
			{
				names.add(vol.getName());
			}
		}
		Collections.sort(names);
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		model.addElement(UiUtils.NONE);
		for (String n : names)
		{
			model.addElement(n);
		}
		return model;
	}

	/*-------------------------------------------------------------------------*/
	private static boolean modelContains(DefaultComboBoxModel<String> model, String value)
	{
		for (int i = 0; i < model.getSize(); i++)
		{
			if (value.equals(model.getElementAt(i)))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	protected void refreshInternal(PackageStep step, Recipe recipe)
	{
		paneRecipe = recipe;

		List<String> styles = new ArrayList<>(Database.getInstance().getStyles().keySet());
		styles.sort(new AlphanumComparator());
		style.setModel(new DefaultComboBoxModel<>(styles.toArray(String[]::new)));

		if (step != null)
		{
			style.setSelectedItem(step.getStyleId());
			packagingType.setSelectedItem(step.getPackagingType());
			PackageStep.PackagingType pt = step.getPackagingType();
			if (pt == null)
			{
				pt = PackageStep.PackagingType.BOTTLE;
			}
			refreshCarbonationMethodCombo(pt);
			carbonationMethod.setSelectedItem(step.getCarbonationMethod());
			syncCarbonationUi(step, recipe);
		}
	}

	/*-------------------------------------------------------------------------*/
	JComboBox<String> getSpeiseVolumeComboForTest()
	{
		return speiseVolumeCombo;
	}
}
