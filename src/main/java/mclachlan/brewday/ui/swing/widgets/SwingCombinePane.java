package mclachlan.brewday.ui.swing.widgets;

import javax.swing.JCheckBox;
import mclachlan.brewday.process.Combine;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingCombinePane extends SwingProcessStepPane<Combine>
{
	private JCheckBox pitchCombine;

	public SwingCombinePane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("volumes.in", Combine::getInputVolume, Combine::setInputVolume,
			Volume.Type.BEER, Volume.Type.WORT, Volume.Type.MASH);
		addInputVolumeComboBox("combine.input.2", Combine::getInputVolume2, Combine::setInputVolume2,
			Volume.Type.BEER, Volume.Type.WORT, Volume.Type.MASH);

		pitchCombine = new JCheckBox(getUiString("combine.pitch.combine"));
		pitchCombine.setToolTipText(getUiString("combine.pitch.combine.tooltip"));
		addSpanningCheckboxRow(pitchCombine);
		pitchCombine.addActionListener(e ->
		{
			Combine s = getStepForTest();
			if (!isStepPaneRefreshing() && s != null)
			{
				s.setPitchCombine(pitchCombine.isSelected());
				dirtyState.markDirty(s);
			}
		});

		addAddIngredientButton(IngredientAddition.Type.HOPS);
		addAddIngredientButton(IngredientAddition.Type.WATER);
		addAddIngredientButton(IngredientAddition.Type.FERMENTABLES);
		addAddIngredientButton(IngredientAddition.Type.YEAST);
		addAddIngredientButton(IngredientAddition.Type.MISC);
		addComputedVolumePane("volumes.out", Combine::getOutputVolume);
	}

	@Override
	protected void refreshInternal(Combine step, mclachlan.brewday.recipe.Recipe recipe)
	{
		if (step != null && pitchCombine != null)
		{
			pitchCombine.setSelected(step.isPitchCombine());
		}
	}
}
