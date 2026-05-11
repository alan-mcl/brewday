package mclachlan.brewday.ui.swing.widgets;

import javax.swing.JButton;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.process.MashInfusion;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code MashInfusionPane}.
 */
public class SwingMashInfusionPane extends SwingProcessStepPane<MashInfusion>
{
	public SwingMashInfusionPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox(
			"mash.infusion.volume.in",
			MashInfusion::getInputMashVolume,
			MashInfusion::setInputMashVolume,
			Volume.Type.MASH);

		Quantity.Unit tempUnit = Database.getInstance().getSettings().getUnitForStepAndIngredient(
			Quantity.Type.TEMPERATURE, ProcessStep.Type.MASH, IngredientAddition.Type.WATER);

		SwingQuantityEditWidget<TemperatureUnit> mashTempDisplay = new SwingQuantityEditWidget<>(tempUnit);
		mashTempDisplay.setEditable(false);
		getUnitControlUtils().registerQuantityEdit(mashTempDisplay, MashInfusion::getMashTemp, null);
		addReadOnlyQuantityWidgetRow("mash.temp", mashTempDisplay);

		addTimeUnitControl("mash.infusion.ramp.time",
			MashInfusion::getRampTime, MashInfusion::setRampTime, Quantity.Unit.MINUTES);
		addTimeUnitControl("mash.infusion.duration",
			MashInfusion::getStandTime, MashInfusion::setStandTime, Quantity.Unit.MINUTES);

		JButton waterBuilder = new JButton(SwingIcons.toolbarIcon(SwingIcons.IconKey.WATER_BUILDER));
		waterBuilder.setToolTipText(getUiString("tools.water.builder"));
		waterBuilder.addActionListener(e -> runWaterBuilderUtility(getStepForTest()));
		getStepToolbar().add(waterBuilder);

		addIngredientButtonsForPrototype(new MashInfusion());

		addComputedVolumePane("mash.infusion.mash.volume.out", MashInfusion::getOutputMashVolume);
	}
}
