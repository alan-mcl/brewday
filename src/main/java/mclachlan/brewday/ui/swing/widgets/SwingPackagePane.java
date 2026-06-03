package mclachlan.brewday.ui.swing.widgets;

import alphanum.AlphanumComparator;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.CarbonationUnit;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.FermentationCalculator;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.ProcessLog;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

/**
 * Swing analogue of JFX {@code PackagePane}.
 * <p>
 * The packaged beer's output-volume name is edited via the in-tile Rename
 * action on the computed-volume pane (shared with every other step), not via
 * a dedicated text field on this form.
 */
public class SwingPackagePane extends SwingProcessStepPane<PackageStep>
{
	private JComboBox<String> style;
	private JComboBox<PackageStep.PackagingType> packagingType;
	private JComboBox<PackageStep.CarbonationMethod> carbonationMethod;
	private SwingQuantityEditWidget<CarbonationUnit> forcedCarbonation;
	private JComboBox<String> speiseVolumeCombo;
	private SwingQuantityEditWidget<DensityUnit> predictedFinalGravity;

	/** recipe from last {@link #refreshInternal} — used by carbonation UI listeners */
	private Recipe paneRecipe;

	public SwingPackagePane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addIngredientButtonsForPrototype(new PackageStep());

		addInputVolumeComboBox("volumes.in",
			PackageStep::getInputVolume,
			PackageStep::setInputVolume,
			Volume.Type.BEER);

		style = new JComboBox<>();
		addLabeledWidgetToForm("recipe.style", style);

		packagingType = new JComboBox<>(PackageStep.PackagingType.values());
		addLabeledWidgetToForm("package.type", packagingType);

		carbonationMethod = new JComboBox<>(PackageStep.CarbonationMethod.values());
		addLabeledWidgetToForm("package.carbonation.method", carbonationMethod);

		addInputVolumeComboBox("package.speise.volume",
			PackageStep::getSpeiseVolume,
			PackageStep::setSpeiseVolume,
			Volume.Type.WORT);
		speiseVolumeCombo = getInputVolumeComboForTest(1);

		forcedCarbonation = new SwingQuantityEditWidget<>(Quantity.Unit.VOLUMES);
		addLabeledWidgetToForm("package.forced.carbonation", forcedCarbonation);
		getUnitControlUtils().registerQuantityEdit(forcedCarbonation, PackageStep::getForcedCarbonation,
			PackageStep::setForcedCarbonation);

		Quantity.Unit densityUnit = Database.getInstance().getSettings().getUnitForStepAndIngredient(
			Quantity.Type.FLUID_DENSITY, ProcessStep.Type.PACKAGE, IngredientAddition.Type.FERMENTABLES);
		predictedFinalGravity = new SwingQuantityEditWidget<>(densityUnit);
		predictedFinalGravity.setEditable(false);
		addReadOnlyQuantityWidgetRow("package.spunding.predicted.fg", predictedFinalGravity);

		addVolumeUnitControl("package.loss",
			PackageStep::getPackagingLoss, PackageStep::setPackagingLoss, Quantity.Unit.LITRES);

		packagingType.addActionListener(e ->
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
				s.setCarbonationMethod(PackageStep.CarbonationMethod.PRIMING_SUGAR);
				carbonationMethod.setSelectedItem(PackageStep.CarbonationMethod.PRIMING_SUGAR);
			}
			updateCarbonationMethodUi(s, paneRecipe);
			dirtyState.markDirty(s);
		});

		carbonationMethod.addActionListener(e ->
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
			s.setCarbonationMethod(m);
			updateCarbonationMethodUi(s, paneRecipe);
			dirtyState.markDirty(s);
		});

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
	private void updateCarbonationMethodUi(PackageStep step, Recipe recipe)
	{
		if (step == null)
		{
			return;
		}

		PackageStep.CarbonationMethod method = step.getCarbonationMethod();

		if (forcedCarbonation != null)
		{
			if (method == PackageStep.CarbonationMethod.FORCE_CARB)
			{
				forcedCarbonation.setEditable(true);
				forcedCarbonation.setVisible(true);
				forcedCarbonation.setQuantity(step.getForcedCarbonation());
			}
			else
			{
				forcedCarbonation.setEditable(false);
				forcedCarbonation.setVisible(false);
				forcedCarbonation.setQuantity(new CarbonationUnit(0));
			}
		}

		if (speiseVolumeCombo != null)
		{
			boolean speise = method == PackageStep.CarbonationMethod.SPEISE;
			speiseVolumeCombo.setEnabled(speise);
			speiseVolumeCombo.setVisible(speise);
		}

		if (predictedFinalGravity != null)
		{
			boolean spunding = method == PackageStep.CarbonationMethod.SPUNDING;
			predictedFinalGravity.setVisible(spunding);
			if (spunding && recipe != null && step.getInputVolume() != null)
			{
				Volume beer = recipe.getVolumes().getVolume(step.getInputVolume());
				if (beer != null)
				{
					DensityUnit predicted = FermentationCalculator.calcPredictedTerminalFg(
						beer,
						step.getYeastAdditions(),
						new ProcessLog());
					predictedFinalGravity.setQuantity(predicted);
				}
			}
		}
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
			carbonationMethod.setSelectedItem(step.getCarbonationMethod());
			updateCarbonationMethodUi(step, recipe);
		}
	}
}
