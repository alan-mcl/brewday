package mclachlan.brewday.ui.swing.widgets;

import javax.swing.JButton;
import mclachlan.brewday.process.BatchSparge;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code BatchSpargePane}.
 */
public class SwingBatchSpargePane extends SwingProcessStepPane<BatchSparge>
{
	public SwingBatchSpargePane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("batch.sparge.mash",
			BatchSparge::getMashVolume,
			BatchSparge::setMashVolume,
			Volume.Type.MASH);

		addInputVolumeComboBox("batch.sparge.existing.wort",
			BatchSparge::getWortVolume,
			BatchSparge::setWortVolume,
			Volume.Type.WORT);

		JButton waterBuilder = new JButton(SwingIcons.toolbarIcon(SwingIcons.IconKey.WATER_BUILDER));
		waterBuilder.setToolTipText(getUiString("tools.water.builder.tooltip"));
		waterBuilder.addActionListener(e -> runWaterBuilderUtility(getStepForTest()));
		getStepToolbar().add(waterBuilder);

		addIngredientButtonsForPrototype(new BatchSparge());

		addComputedVolumePane("batch.sparge.sparge.runnings", BatchSparge::getOutputSpargeRunnings);
		addComputedVolumePane("batch.sparge.sparge.runnings.existing.wort", BatchSparge::getOutputCombinedWortVolume);
		addComputedVolumePane("batch.sparge.lautered.mash", BatchSparge::getOutputMashVolume);
	}

}
