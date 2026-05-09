package mclachlan.brewday.ui.swing.widgets;

import javax.swing.JCheckBox;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.Boil;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code BoilPane}.
 */
public class SwingBoilPane extends SwingProcessStepPane<Boil>
{
	private JCheckBox removeTrubAndChillerLoss;
	private SwingQuantityEditWidget<TimeUnit> timeToBoil;

	public SwingBoilPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addIngredientButtonsForPrototype(new Boil());

		addInputVolumeComboBox("boil.wort.in",
			Boil::getInputWortVolume,
			Boil::setInputWortVolume,
			Volume.Type.WORT, Volume.Type.MASH);

		Quantity.Unit boilTimeUnit = Database.getInstance().getSettings().getUnitForStepAndIngredient(
			Quantity.Type.TIME, ProcessStep.Type.BOIL, IngredientAddition.Type.WATER);
		timeToBoil = new SwingQuantityEditWidget<>(boilTimeUnit);
		timeToBoil.setEditable(false);
		getUnitControlUtils().registerQuantityEdit(timeToBoil, Boil::getTimeToBoil, null);
		addReadOnlyQuantityWidgetRow("boil.time.to.boil", timeToBoil);

		addTimeUnitControl("boil.duration",
			Boil::getDuration, Boil::setDuration, Quantity.Unit.MINUTES);

		removeTrubAndChillerLoss = new JCheckBox(getUiString("boil.remove.trub.and.chiller.loss"));
		addSpanningCheckboxRow(removeTrubAndChillerLoss);

		removeTrubAndChillerLoss.addActionListener(e ->
		{
			Boil s = getStepForTest();
			if (!isStepPaneRefreshing() && s != null)
			{
				s.setRemoveTrubAndChillerLoss(removeTrubAndChillerLoss.isSelected());
				dirtyState.markDirty(s);
			}
		});

		addComputedVolumePane("boil.wort.out", Boil::getOutputWortVolume);
		addComputedVolumePane("boil.trub", Boil::getOutputTrubVolume);
	}

	@Override
	protected void refreshInternal(Boil step, Recipe recipe)
	{
		if (step != null && removeTrubAndChillerLoss != null)
		{
			removeTrubAndChillerLoss.setSelected(step.isRemoveTrubAndChillerLoss());
		}
	}
}
