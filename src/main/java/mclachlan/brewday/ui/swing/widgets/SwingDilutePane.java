package mclachlan.brewday.ui.swing.widgets;

import javax.swing.JCheckBox;
import mclachlan.brewday.process.Dilute;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingDilutePane extends SwingProcessStepPane<Dilute>
{
	private JCheckBox removeTrubAndChillerLoss;

	public SwingDilutePane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("volumes.in", Dilute::getInputVolume, Dilute::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);
		addAddIngredientButton(IngredientAddition.Type.WATER);

		removeTrubAndChillerLoss = new JCheckBox(getUiString("dilute.remove.trub.and.chiller.loss"));
		addSpanningCheckboxRow(removeTrubAndChillerLoss);

		removeTrubAndChillerLoss.addActionListener(e ->
		{
			Dilute s = getStepForTest();
			if (!isStepPaneRefreshing() && s != null)
			{
				s.setRemoveTrubAndChillerLoss(removeTrubAndChillerLoss.isSelected());
				dirtyState.markDirty(s);
			}
		});

		addComputedVolumePane("volumes.out", Dilute::getOutputVolume);
	}

	@Override
	protected void refreshInternal(Dilute step, Recipe recipe)
	{
		if (step != null && removeTrubAndChillerLoss != null)
		{
			removeTrubAndChillerLoss.setSelected(step.isRemoveTrubAndChillerLoss());
		}
	}
}
