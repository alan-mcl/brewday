/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import javafx.scene.Node;
import javafx.scene.control.*;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class RecipeEditor extends MigPane implements TrackDirty
{
	private final CardGroup stepCards;

	private final TextArea stepsEndResult;
	private final TextArea log;

	private final RecipeTreeView recipeTreeView;
	private final RecipeInfoPane recipeInfoPane;

	private Recipe recipe;
	private final TrackDirty parent;
	private final boolean processTemplateMode;

	/*-------------------------------------------------------------------------*/
	public RecipeEditor(
		final Recipe recipe,
		final TrackDirty parent,
		boolean processTemplateMode)
	{
		super("gap 3");

		this.setPrefSize(1200, 750);

		this.recipe = recipe;
		this.parent = parent;
		this.processTemplateMode = processTemplateMode;
		this.stepCards = new CardGroup();

		recipeTreeView = new RecipeTreeView(() -> this.recipe, stepCards);

		ProcessStepPane<BatchSparge> batchSpargePane = new BatchSpargePane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<Boil> boilPane = new BoilPane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<Cool> coolPane = new CoolPane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<Heat> heatPane = new HeatPane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<Dilute> dilutePane = new DilutePane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<Ferment> fermentPane = new FermentPane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<Mash> mashPane = new MashPane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<Lauter> lauterPane = new LauterPane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<Stand> standPane = new StandPane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<PackageStep> packagePane = new PackagePane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<Split> splitByPercentPane = new SplitPane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<Combine> combinePane = new CombinePane(this, recipeTreeView, processTemplateMode);
		ProcessStepPane<MashInfusion> mashInfusionPane = new MashInfusionPane(this, recipeTreeView, processTemplateMode);

		stepCards.add(ProcessStep.Type.BATCH_SPARGE.toString(), batchSpargePane);
		stepCards.add(ProcessStep.Type.BOIL.toString(), boilPane);
		stepCards.add(ProcessStep.Type.COOL.toString(), coolPane);
		stepCards.add(ProcessStep.Type.HEAT.toString(), heatPane);
		stepCards.add(ProcessStep.Type.DILUTE.toString(), dilutePane);
		stepCards.add(ProcessStep.Type.FERMENT.toString(), fermentPane);
		stepCards.add(ProcessStep.Type.MASH.toString(), mashPane);
		stepCards.add(ProcessStep.Type.LAUTER.toString(), lauterPane);
		stepCards.add(ProcessStep.Type.STAND.toString(), standPane);
		stepCards.add(ProcessStep.Type.PACKAGE.toString(), packagePane);
		stepCards.add(ProcessStep.Type.SPLIT.toString(), splitByPercentPane);
		stepCards.add(ProcessStep.Type.COMBINE.toString(), combinePane);
		stepCards.add(ProcessStep.Type.MASH_INFUSION.toString(), mashInfusionPane);

		if (!processTemplateMode)
		{
			FermentableAdditionPane fermentableAdditionPane = new FermentableAdditionPane(this, recipeTreeView);
			HopAdditionPane hopAdditionPane = new HopAdditionPane(this, recipeTreeView);
			WaterAdditionPane waterAdditionPane = new WaterAdditionPane(this, recipeTreeView);
			YeastAdditionPane yeastAdditionPane = new YeastAdditionPane(this, recipeTreeView);
			MiscAdditionPane miscAdditionPane = new MiscAdditionPane(this, recipeTreeView);

			stepCards.add(IngredientAddition.Type.HOPS.toString(), hopAdditionPane);
			stepCards.add(IngredientAddition.Type.FERMENTABLES.toString(), fermentableAdditionPane);
			stepCards.add(IngredientAddition.Type.WATER.toString(), waterAdditionPane);
			stepCards.add(IngredientAddition.Type.YEAST.toString(), yeastAdditionPane);
			stepCards.add(IngredientAddition.Type.MISC.toString(), miscAdditionPane);
		}

		recipeInfoPane = new RecipeInfoPane(this, recipeTreeView);
		stepCards.add(UiUtils.NONE, recipeInfoPane);

		stepsEndResult = new TextArea();
		stepsEndResult.setEditable(false);
		stepsEndResult.setMaxWidth(300);
		stepsEndResult.setPrefHeight(650);
		stepsEndResult.setWrapText(true);

		MigPane stepCardsPane = new MigPane();
		stepCardsPane.add(stepCards);

		TabPane tabs = new TabPane();
		tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		tabs.getStyleClass().add("floating");

		MigPane processTab = new MigPane();
		processTab.add(recipeTreeView, "aligny top");
		processTab.add(stepCardsPane, "aligny top");

		log = new TextArea();
		log.setEditable(false);

		Tab tab1 = new Tab(StringUtils.getUiString("recipe.process"), processTab);
		Tab tab2 = new Tab(StringUtils.getUiString("recipe.log"), log);

		tabs.getTabs().addAll(tab1, tab2);

		tabs.setPrefSize(800, 650);

//		this.add(recipeEditBar, "dock north");
		this.add(tabs, "dock center");
		this.add(stepsEndResult, "dock east");

		// ---- values
		refresh(recipe);

	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Recipe recipe)
	{
		this.recipe = recipe;

		rerunRecipe(this.recipe);

		recipeTreeView.refresh(recipe);
		stepCards.setVisible(UiUtils.NONE);
		recipeInfoPane.refresh(recipe);
		refreshEndResult(recipe);
		refreshLog(recipe);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void setDirty(Object... objs)
	{
		for (Object dirty : objs)
		{
			if (dirty != null && !JfxUi.getInstance().isDirty(dirty))
			{
				if (dirty instanceof ProcessStep)
				{
					// need to run this first to set up the recipe internal state before refreshing
					rerunRecipe(recipe);

					ProcessStep step = (ProcessStep)dirty;

					recipeTreeView.setDirty(step);
					parent.setDirty(recipe, dirty);

					rerunRecipe(recipe);

					Node visible = stepCards.getVisible();
					if (visible instanceof ProcessStepPane && ((ProcessStepPane<?>)visible).getStep()==step)
					{
						((ProcessStepPane<ProcessStep>)visible).refresh(step, recipe);
					}
				}
				else if (dirty instanceof IngredientAddition)
				{
					rerunRecipe(recipe);

					IngredientAddition addition = (IngredientAddition)dirty;

					recipeTreeView.setDirty(addition);
					parent.setDirty(recipe, dirty);

					rerunRecipe(recipe);

					Node visible = stepCards.getVisible();
					if (visible instanceof IngredientAdditionPane)
					{
						IngredientAdditionPane<IngredientAddition, V2DataObject> iap =
							(IngredientAdditionPane<IngredientAddition, V2DataObject>)visible;

						// This check skips refreshes when the edit widget has fired
						// dues to the user changing to a new pane
						if (addition.getType() == iap.getAddition().getType())
						{
							iap.refresh(addition, recipe);
						}
					}
				}
				else if (dirty instanceof Recipe)
				{
					recipeTreeView.setDirty(recipe);
					parent.setDirty(recipe);

					rerunRecipe(recipe);

					recipeInfoPane.refresh(recipe);
				}
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	protected void rerunRecipe(Recipe recipe)
	{
		if (processTemplateMode)
		{
			recipe.dryRun();
		}
		else
		{
			recipe.run();
		}

		refreshLog(recipe);
		refreshEndResult(recipe);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void clearDirty()
	{
		throw new RuntimeException("Unimplemented auto generated method!");
	}

	/*-------------------------------------------------------------------------*/
	private void refreshLog(Recipe recipe)
	{
		log.setText("");

		StringBuilder sb = new StringBuilder();

		for (String s : recipe.getLog().getMsgs())
		{
			s = s.replaceAll("\n", "; ");
			sb.append(s).append("\n");
		}

		log.setText(sb.toString());
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshEndResult(Recipe recipe)
	{
		stepsEndResult.setText("");

		StringBuilder sb = new StringBuilder(StringUtils.getUiString("recipe.end.result") + "\n");

		if (recipe.getErrors().size() > 0)
		{
			sb.append("\n").append(StringUtils.getUiString("recipe.errors")).append("\n");
			for (String s : recipe.getErrors())
			{
				sb.append(s);
				sb.append("\n");
			}
		}

		if (recipe.getWarnings().size() > 0)
		{
			sb.append("\n").append(StringUtils.getUiString("recipe.warnings")).append("\n");
			for (String s : recipe.getWarnings())
			{
				sb.append(s);
				sb.append("\n");
			}
		}

		if (recipe.getVolumes().getOutputVolumes().size() > 0)
		{
			for (String s : recipe.getVolumes().getOutputVolumes())
			{
				Volume v = (Volume)recipe.getVolumes().getVolume(s);

				if (processTemplateMode)
				{
					sb.append(String.format("\n'%s'\n", v.getName()));
				}
				else
				{
					sb.append(String.format("\n'%s' (%.1fl)\n", v.getName(), v.getVolume().get(Quantity.Unit.LITRES)));
					if (v.getType() == Volume.Type.BEER)
					{
						sb.append(String.format("OG %.3f\n", v.getOriginalGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
						sb.append(String.format("FG %.3f\n", v.getGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
					}
					sb.append(String.format("%.1f%% ABV\n", v.getAbv().get() * 100));
					sb.append(String.format("%.0f IBU\n", v.getBitterness().get(Quantity.Unit.IBU)));
					sb.append(String.format("%.1f SRM\n", v.getColour().get(Quantity.Unit.SRM)));
				}
			}
		}
		else
		{
			sb.append("\n").
				append(StringUtils.getUiString("recipe.no.output.volumes")).
				append("\n");
		}

		stepsEndResult.setText(sb.toString());
	}

}
