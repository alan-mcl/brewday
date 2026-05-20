package mclachlan.brewday.ui.swing.widgets;

import javax.swing.JCheckBox;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Cool;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingCoolPane extends SwingProcessStepPane<Cool>
{
	private JCheckBox removeTrubAndChillerLoss;

	public SwingCoolPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("volumes.in", Cool::getInputVolume, Cool::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);

		addTemperatureUnitControl("cool.target.temp", Cool::getTargetTemp, Cool::setTargetTemp, Quantity.Unit.CELSIUS);

		removeTrubAndChillerLoss = new JCheckBox(getUiString("cool.remove.trub.and.chiller.loss"));
		addSpanningCheckboxRow(removeTrubAndChillerLoss);

		removeTrubAndChillerLoss.addActionListener(e ->
		{
			Cool s = getStepForTest();
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

		addComputedVolumePane("cool.wort.out", Cool::getOutputVolume);
	}

	@Override
	protected void refreshInternal(Cool step, Recipe recipe)
	{
		if (step != null && removeTrubAndChillerLoss != null)
		{
			removeTrubAndChillerLoss.setSelected(step.isRemoveTrubAndChillerLoss());
		}
	}
}
