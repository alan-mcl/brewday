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

import javafx.geometry.Insets;
import javafx.scene.control.*;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.EditorPanel;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class RecipeEditor extends MigPane implements TrackDirty
{
	private final CardGroup stepCards;

	private final TreeView<Label> stepsTree;
	private final RecipeTreeViewModel stepsTreeModel;
	private final TextArea stepsEndResult;
	private final TextArea log;

	private final RecipeInfoPane recipeInfoPane;

	private Recipe recipe;
	private final TrackDirty parent;

	/*-------------------------------------------------------------------------*/
	public RecipeEditor(
		final Recipe recipe,
		final TrackDirty parent)
	{
		super("gap 3");

		this.setPrefSize(1200, 750);

		this.recipe = recipe;
		this.parent = parent;

		stepsTree = new TreeView<>();
		stepsTreeModel = new RecipeTreeViewModel(stepsTree);
		stepsTree.setPrefSize(300, 650);

		stepCards = new CardGroup();

		ProcessStepPane<BatchSparge> batchSpargePane = new BatchSpargePane(this, stepsTreeModel);
		ProcessStepPane<Boil> boilPane = new BoilPane(this, stepsTreeModel);
		ProcessStepPane<Cool> coolPane = new CoolPane(this, stepsTreeModel);
		// dilutePanel = new DilutePanel(dirty);
		ProcessStepPane<Ferment> fermentPane = new FermentPane(this, stepsTreeModel);
		ProcessStepPane<Mash> mashPane = new MashPane(this, stepsTreeModel);
		//	standPanel = new StandPanel(dirty);
		ProcessStepPane<PackageStep> packagePane = new PackagePane(this, stepsTreeModel);
		ProcessStepPane<SplitByPercent> splitByPercentPane = new SplitByPercentPane(this, stepsTreeModel);
		//	mashInfusionPanel = new MashInfusionPanel(dirty);

		stepCards.add(ProcessStep.Type.BATCH_SPARGE.toString(), batchSpargePane);
		stepCards.add(ProcessStep.Type.BOIL.toString(), boilPane);
		stepCards.add(ProcessStep.Type.COOL.toString(), coolPane);
		//		stepCards.add(ProcessStep.Type.DILUTE.toString(), dilutePanel);
		stepCards.add(ProcessStep.Type.FERMENT.toString(), fermentPane);
		stepCards.add(ProcessStep.Type.MASH.toString(), mashPane);
		//		stepCards.add(ProcessStep.Type.STAND.toString(), standPanel);
		stepCards.add(ProcessStep.Type.PACKAGE.toString(), packagePane);
		//		stepCards.add(ProcessStep.Type.MASH_INFUSION.toString(), mashInfusionPanel);
		stepCards.add(ProcessStep.Type.SPLIT_BY_PERCENT.toString(), splitByPercentPane);

		FermentableAdditionPane fermentableAdditionPane = new FermentableAdditionPane(this, stepsTreeModel);
		HopAdditionPane hopAdditionPane = new HopAdditionPane(this, stepsTreeModel);
		WaterAdditionPane waterAdditionPane = new WaterAdditionPane(this, stepsTreeModel);
		YeastAdditionPane yeastAdditionPane = new YeastAdditionPane(this, stepsTreeModel);
		MiscAdditionPane miscAdditionPane = new MiscAdditionPane(this, stepsTreeModel);

		stepCards.add(IngredientAddition.Type.HOPS.toString(), hopAdditionPane);
		stepCards.add(IngredientAddition.Type.FERMENTABLES.toString(), fermentableAdditionPane);
		stepCards.add(IngredientAddition.Type.WATER.toString(), waterAdditionPane);
		stepCards.add(IngredientAddition.Type.YEAST.toString(), yeastAdditionPane);
		stepCards.add(IngredientAddition.Type.MISC.toString(), miscAdditionPane);

		recipeInfoPane = new RecipeInfoPane(this, stepsTreeModel);
		stepCards.add(EditorPanel.NONE, recipeInfoPane);

		ToolBar recipeEditBar = new ToolBar();
		recipeEditBar.setPadding(new Insets(3, 3, 6, 3));
//		recipeEditBar.setPadding(new Insets(0, 3, 0, 3));

		/*StringUtils.getUiString("recipe.add.step")*/
		Button addStep = new Button(null/*StringUtils.getUiString("recipe.add.step")*/, JfxUi.getImageView(JfxUi.addStep, JfxUi.ICON_SIZE));
		Button addFermentable = new Button(null/*StringUtils.getUiString("common.add.fermentable")*/, JfxUi.getImageView(JfxUi.addFermentable, JfxUi.ICON_SIZE));
		Button addHop = new Button(null/*StringUtils.getUiString("common.add.hop")*/, JfxUi.getImageView(JfxUi.addHops, JfxUi.ICON_SIZE));
		Button addMisc = new Button(null/*StringUtils.getUiString("common.add.misc")*/, JfxUi.getImageView(JfxUi.addMisc, JfxUi.ICON_SIZE));
		Button addYeast = new Button(null/*StringUtils.getUiString("common.add.yeast")*/, JfxUi.getImageView(JfxUi.addYeast, JfxUi.ICON_SIZE));
		Button addWater = new Button(null/*StringUtils.getUiString("common.add.water")*/, JfxUi.getImageView(JfxUi.addWater, JfxUi.ICON_SIZE));
		Button deleteButton = new Button(null/*StringUtils.getUiString("editor.delete")*/, JfxUi.getImageView(JfxUi.deleteIcon, JfxUi.ICON_SIZE));
		Button substitute = new Button(null/*StringUtils.getUiString("common.substitute")*/, JfxUi.getImageView(JfxUi.substituteIcon, JfxUi.ICON_SIZE));
		Button duplicate = new Button(null/*StringUtils.getUiString("common.duplicate")*/, JfxUi.getImageView(JfxUi.duplicateIcon, JfxUi.ICON_SIZE));
		// recipe edit buttons
		Button applyProcessTemplate = new Button(null/*StringUtils.getUiString("common.substitute")*/, JfxUi.getImageView(JfxUi.processTemplateIcon, JfxUi.ICON_SIZE));

		recipeEditBar.getItems().add(addStep);
		recipeEditBar.getItems().add(addFermentable);
		recipeEditBar.getItems().add(addHop);
		recipeEditBar.getItems().add(addMisc);
		recipeEditBar.getItems().add(addYeast);
		recipeEditBar.getItems().add(addWater);
		recipeEditBar.getItems().add(new Separator());
		recipeEditBar.getItems().add(deleteButton);
		recipeEditBar.getItems().add(duplicate);
		recipeEditBar.getItems().add(substitute);

		stepsEndResult = new TextArea();
		stepsEndResult.setEditable(false);
		stepsEndResult.setMaxWidth(300);
		stepsEndResult.setPrefHeight(650);
		stepsEndResult.setWrapText(true);

		MigPane stepCardsPane = new MigPane();
//		stepCardsPane.add(recipeEditBar, "dock north");
		stepCardsPane.add(stepCards);

		TabPane tabs = new TabPane();
		tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		tabs.getStyleClass().add("floating");

		MigPane processTab = new MigPane();
		processTab.add(stepsTree, "aligny top");
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

		// ---- listeners

		stepsTree.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				if (newValue != null && newValue != oldValue)
				{
					Object value = stepsTreeModel.getValue(newValue.getValue());

					if (value instanceof ProcessStep)
					{
						String key = ((ProcessStep)value).getType().toString();
						stepCards.setVisible(key);
						ProcessStepPane child = (ProcessStepPane)stepCards.getChild(key);
						child.refresh((ProcessStep)value, recipe);
					}
					else if (value instanceof IngredientAddition)
					{
						String key = ((IngredientAddition)value).getType().toString();
						stepCards.setVisible(key);
						IngredientAdditionPane child = (IngredientAdditionPane)stepCards.getChild(key);
						child.refresh((IngredientAddition)value, recipe);
					}
					else
					{
						stepCards.setVisible(EditorPanel.NONE);
					}
				}
			});
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Recipe recipe)
	{
		this.recipe = recipe;
		stepsTreeModel.refresh(recipe);
		stepCards.setVisible(EditorPanel.NONE);
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
			if (dirty != null)
			{
				if (dirty instanceof ProcessStep)
				{
					// need to run this first to set up the recipe internal state before refreshing
					rerunRecipe(recipe);

					ProcessStep step = (ProcessStep)dirty;
					Recipe recipe = step.getRecipe();

					stepsTreeModel.setDirty(step);
					parent.setDirty(recipe, dirty);

					rerunRecipe(recipe);
				}
				else if (dirty instanceof IngredientAddition)
				{
					rerunRecipe(recipe);

					IngredientAddition addition = (IngredientAddition)dirty;

					stepsTreeModel.setDirty(addition);
					parent.setDirty(recipe, dirty);

					rerunRecipe(recipe);
				}
				else if (dirty instanceof Recipe)
				{
					stepsTreeModel.setDirty(recipe);
					parent.setDirty(recipe);

					rerunRecipe(recipe);
				}
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private void rerunRecipe(Recipe recipe)
	{
		recipe.run();

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
		else
		{
			sb.append("\n").
				append(StringUtils.getUiString("recipe.no.output.volumes")).
				append("\n");
		}

		stepsEndResult.setText(sb.toString());
	}
}
