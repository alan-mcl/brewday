package mclachlan.brewday.ui.swing.widgets;

import alphanum.AlphanumComparator;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.CarbonationUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.Volume;
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

		forcedCarbonation = new SwingQuantityEditWidget<>(Quantity.Unit.VOLUMES);
		addLabeledWidgetToForm("package.forced.carbonation", forcedCarbonation);
		getUnitControlUtils().registerQuantityEdit(forcedCarbonation, PackageStep::getForcedCarbonation,
			PackageStep::setForcedCarbonation);

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
			updateForcedCarbonationUi(s);
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
			updateForcedCarbonationUi(s);
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
	private void updateForcedCarbonationUi(PackageStep step)
	{
		if (step == null || forcedCarbonation == null)
		{
			return;
		}
		if (step.getCarbonationMethod() == PackageStep.CarbonationMethod.FORCE_CARB)
		{
			forcedCarbonation.setEditable(true);
			forcedCarbonation.setQuantity(step.getForcedCarbonation());
		}
		else
		{
			forcedCarbonation.setEditable(false);
			forcedCarbonation.setQuantity(new CarbonationUnit(0));
		}
	}

	@Override
	protected void refreshInternal(PackageStep step, Recipe recipe)
	{
		List<String> styles = new ArrayList<>(Database.getInstance().getStyles().keySet());
		styles.sort(new AlphanumComparator());
		style.setModel(new DefaultComboBoxModel<>(styles.toArray(String[]::new)));

		if (step != null)
		{
			style.setSelectedItem(step.getStyleId());
			packagingType.setSelectedItem(step.getPackagingType());
			carbonationMethod.setSelectedItem(step.getCarbonationMethod());
			updateForcedCarbonationUi(step);
		}
	}
}
