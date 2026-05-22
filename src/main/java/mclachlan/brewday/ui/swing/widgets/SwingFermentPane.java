package mclachlan.brewday.ui.swing.widgets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Ferment;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code FermentPane}.
 */
public class SwingFermentPane extends SwingProcessStepPane<Ferment>
{
	private JCheckBox removeTrubAndChillerLoss;
	private JComboBox<Ferment.FermentType> fermentType;
	private SwingQuantityEditWidget<DensityUnit> estFG;

	public SwingFermentPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addIngredientButtonsForPrototype(new Ferment());

		addInputVolumeComboBox("volumes.in",
			Ferment::getInputVolume,
			Ferment::setInputVolume,
			Volume.Type.WORT, Volume.Type.BEER);

		fermentType = new JComboBox<>(Ferment.FermentType.values());
		addLabeledWidgetToForm("ferment.type", fermentType);
		fermentType.addActionListener(e ->
		{
			Ferment s = getStepForTest();
			if (!isStepPaneRefreshing() && s != null)
			{
				Object sel = fermentType.getSelectedItem();
				if (sel instanceof Ferment.FermentType t)
				{
					s.setFermentType(t);
					dirtyState.markDirty(s);
				}
			}
		});

		addTemperatureUnitControl("ferment.start.temp",
			Ferment::getStartTemp, Ferment::setStartTemp, Quantity.Unit.CELSIUS);
		addTemperatureUnitControl("ferment.end.temp",
			Ferment::getEndTemp, Ferment::setEndTemp, Quantity.Unit.CELSIUS);
		addTimeUnitControl("ferment.duration",
			Ferment::getDuration, Ferment::setDuration, Quantity.Unit.DAYS);

		removeTrubAndChillerLoss = new JCheckBox(getUiString("ferment.remove.trub.and.chiller.loss"));
		addSpanningCheckboxRow(removeTrubAndChillerLoss);

		removeTrubAndChillerLoss.addActionListener(e ->
		{
			Ferment s = getStepForTest();
			if (!isStepPaneRefreshing() && s != null)
			{
				s.setRemoveTrubAndChillerLoss(removeTrubAndChillerLoss.isSelected());
				dirtyState.markDirty(s);
			}
		});

		Quantity.Unit densityUnit = Database.getInstance().getSettings().getUnitForStepAndIngredient(
			Quantity.Type.FLUID_DENSITY, ProcessStep.Type.MASH, IngredientAddition.Type.WATER);
		estFG = new SwingQuantityEditWidget<>(densityUnit);
		estFG.setEditable(false);
		getUnitControlUtils().registerQuantityEdit(estFG, Ferment::getEstimatedFinalGravity, null);
		addReadOnlyQuantityWidgetRow("ferment.fg", estFG);

		addComputedVolumePane("volumes.out", Ferment::getOutputVolume);
	}

	@Override
	protected void refreshInternal(Ferment step, Recipe recipe)
	{
		if (step != null && removeTrubAndChillerLoss != null)
		{
			removeTrubAndChillerLoss.setSelected(step.isRemoveTrubAndChillerLoss());
		}
		if (step != null && fermentType != null)
		{
			fermentType.setSelectedItem(step.getFermentType());
		}
	}
}
