package mclachlan.brewday.ui.swing.widgets;

import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.math.WeightUnit;
import java.util.List;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.dialogs.SwingAcidifierDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingGrainProportionAdjusterDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingTargetMashTempDialog;

import static mclachlan.brewday.math.Quantity.Unit.GRAMS;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code MashPane}.
 */
public class SwingMashPane extends SwingProcessStepPane<Mash>
{
	public SwingMashPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addIngredientButtonsForPrototype(new Mash());

		addInputVolumeComboBox("boil.wort.in",
			Mash::getInputMashVolume,
			Mash::setInputMashVolume,
			Volume.Type.MASH);

		addTemperatureUnitControl("mash.grain.temp",
			Mash::getGrainTemp, Mash::setGrainTemp, Quantity.Unit.CELSIUS);
		addTimeUnitControl("mash.duration",
			Mash::getDuration, Mash::setDuration, Quantity.Unit.MINUTES);

		Quantity.Unit mashTempUnit = Database.getInstance().getSettings().getUnitForStepAndIngredient(
			Quantity.Type.TEMPERATURE, ProcessStep.Type.MASH, IngredientAddition.Type.WATER);
		SwingQuantityEditWidget<TemperatureUnit> mashTempDisplay = new SwingQuantityEditWidget<>(mashTempUnit);
		mashTempDisplay.setEditable(false);
		getUnitControlUtils().registerQuantityEdit(mashTempDisplay, Mash::getMashTemp, null);
		addReadOnlyQuantityWidgetRow("mash.temp", mashTempDisplay);

		SwingQuantityEditWidget<PhUnit> mashPhDisplay = new SwingQuantityEditWidget<>(Quantity.Unit.PH);
		mashPhDisplay.setEditable(false);
		getUnitControlUtils().registerQuantityEdit(mashPhDisplay, Mash::getMashPh, null);
		addReadOnlyQuantityWidgetRow("mash.ph", mashPhDisplay);

		JButton waterBuilder = new JButton(SwingIcons.toolbarIcon(SwingIcons.IconKey.WATER_BUILDER));
		waterBuilder.setToolTipText(getUiString("tools.water.builder.tooltip"));
		waterBuilder.addActionListener(e -> runWaterBuilderUtility(getStepForTest()));
		getStepToolbar().add(waterBuilder);

		JButton acidifier = new JButton(SwingIcons.toolbarIcon(SwingIcons.IconKey.MISC));
		acidifier.setToolTipText(getUiString("tools.acidifier.tooltip"));
		acidifier.addActionListener(e -> runAcidifier());
		getStepToolbar().add(acidifier);

		JButton mashTempTarget = new JButton(SwingIcons.toolbarIcon(SwingIcons.IconKey.HEAT));
		mashTempTarget.setToolTipText(getUiString("tools.mash.temp.tooltip"));
		mashTempTarget.addActionListener(e -> runTargetMashTemp());
		getStepToolbar().add(mashTempTarget);

		JButton grainProportion = new JButton(SwingIcons.toolbarIcon(SwingIcons.IconKey.FERMENTABLE));
		grainProportion.setToolTipText(getUiString("tools.grain.proportion.adjuster.tooltip"));
		grainProportion.addActionListener(e -> runGrainProportionAdjuster());
		getStepToolbar().add(grainProportion);

		addComputedVolumePane("mash.volume.created", Mash::getOutputMashVolume);
	}

	private void runAcidifier()
	{
		Mash mash = getStepForTest();
		if (mash == null)
		{
			return;
		}
		Window parent = SwingUtilities.getWindowAncestor(this);
		SwingAcidifierDialog d = new SwingAcidifierDialog(parent,
			mash.getMashPh(),
			mash.getCombinedWaterProfile(mash.getDuration()),
			mash.getFermentableAdditions(),
			mash.getMiscAdditions());
		d.setVisible(true);
		if (d.getOutput())
		{
			for (MiscAddition ma : d.getAcidAdditions())
			{
				ma.setTime(mash.getDuration());
				mash.addIngredientAddition(ma);
				recipeTree.addAddition(mash, ma);
				dirtyState.markDirty(ma);
			}
			dirtyState.markDirty(mash);
		}
	}

	private void runTargetMashTemp()
	{
		Mash mash = getStepForTest();
		if (mash == null)
		{
			return;
		}
		Window parent = SwingUtilities.getWindowAncestor(this);
		SwingTargetMashTempDialog d = new SwingTargetMashTempDialog(parent,
			mash.getCombinedWaterProfile(mash.getDuration()),
			mash.getFermentableAdditions(),
			mash.getGrainTemp());
		d.setVisible(true);
		if (d.getOutput())
		{
			var temp = d.getTemp();
			if (temp != null)
			{
				for (var wa : mash.getWaterAdditions())
				{
					wa.setTemperature(temp);
					dirtyState.markDirty(wa);
				}
				dirtyState.markDirty(mash);
			}
		}
	}

	private void runGrainProportionAdjuster()
	{
		Mash mash = getStepForTest();
		if (mash == null)
		{
			return;
		}
		if (!SwingGrainProportionAdjusterDialog.hasEnoughAdjustableRows(mash.getFermentableAdditions()))
		{
			JOptionPane.showMessageDialog(
				SwingUtilities.getWindowAncestor(this),
				getUiString("tools.grain.proportion.adjuster.need.two"),
				getUiString("tools.grain.proportion.adjuster"),
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		Window parent = SwingUtilities.getWindowAncestor(this);
		SwingGrainProportionAdjusterDialog d = new SwingGrainProportionAdjusterDialog(parent,
			mash.getFermentableAdditions());
		d.setVisible(true);
		if (!d.getOutput())
		{
			return;
		}
		double[] grams = d.getGramWeights();
		List<FermentableAddition> list = d.getAdjustableAdditions();
		for (int i = 0; i < list.size(); i++)
		{
			FermentableAddition fa = list.get(i);
			fa.setQuantity(new WeightUnit(grams[i], GRAMS, false));
			dirtyState.markDirty(fa);
		}
		dirtyState.markDirty(mash);
	}
}
