package mclachlan.brewday.ui.swing.widgets;

import javax.swing.JButton;
import mclachlan.brewday.process.FlySparge;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Editor pane for the continuous (fly) sparge step: one mash input, sparge
 * water additions, and the collected wort + spent grain outputs.
 */
public class SwingFlySpargePane extends SwingProcessStepPane<FlySparge>
{
	public SwingFlySpargePane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("fly.sparge.mash",
			FlySparge::getInputMashVolume,
			FlySparge::setInputMashVolume,
			Volume.Type.MASH);

		JButton waterBuilder = new JButton(SwingIcons.toolbarIcon(SwingIcons.IconKey.WATER_BUILDER));
		waterBuilder.setToolTipText(getUiString("tools.water.builder.tooltip"));
		waterBuilder.addActionListener(e -> runWaterBuilderUtility(getStepForTest()));
		getStepToolbar().add(waterBuilder);

		addIngredientButtonsForPrototype(new FlySparge());

		addComputedVolumePane("fly.sparge.collected.wort", FlySparge::getOutputCollectedWort);
		addComputedVolumePane("fly.sparge.spent.grain", FlySparge::getOutputSpentGrain);
	}
}
