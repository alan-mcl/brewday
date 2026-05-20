package mclachlan.brewday.ui.swing.widgets;

import javax.swing.JCheckBox;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Stand;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingStandPane extends SwingProcessStepPane<Stand>
{
	private JCheckBox removeTrubAndChillerLoss;

	public SwingStandPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("volumes.in", Stand::getInputVolume, Stand::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);
		addTimeUnitControl("stand.duration", Stand::getDuration, Stand::setDuration, Quantity.Unit.MINUTES);

		removeTrubAndChillerLoss = new JCheckBox(getUiString("stand.remove.trub.and.chiller.loss"));
		addSpanningCheckboxRow(removeTrubAndChillerLoss);

		removeTrubAndChillerLoss.addActionListener(e ->
		{
			Stand s = getStepForTest();
			if (!isStepPaneRefreshing() && s != null)
			{
				s.setRemoveTrubAndChillerLoss(removeTrubAndChillerLoss.isSelected());
				dirtyState.markDirty(s);
			}
		});

		addAddIngredientButton(IngredientAddition.Type.HOPS);
		addAddIngredientButton(IngredientAddition.Type.WATER);
		addAddIngredientButton(IngredientAddition.Type.FERMENTABLES);
		addAddIngredientButton(IngredientAddition.Type.YEAST);
		addAddIngredientButton(IngredientAddition.Type.MISC);
		addComputedVolumePane("volumes.out", Stand::getOutputVolume);
	}

	@Override
	protected void refreshInternal(Stand step, Recipe recipe)
	{
		if (step != null && removeTrubAndChillerLoss != null)
		{
			removeTrubAndChillerLoss.setSelected(step.isRemoveTrubAndChillerLoss());
		}
	}
}
