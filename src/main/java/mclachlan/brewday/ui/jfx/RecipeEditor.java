package mclachlan.brewday.ui.jfx;

import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.*;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
class RecipeEditor extends MigPane implements TrackDirty
{
	private final ComboBox<String> equipmentProfile;

	private final CardGroup stepCards;

	private final DirtyRecipeTreeView stepsTree;
	private final TextArea stepsEndResult;
	private final TextArea log;

	// recipe edit buttons
	private Button applyProcessTemplate, addStep, remove, duplicate, substitute,
		addFermentable, addHop, addMisc, addYeast, addWater, deleteButton;

	private Recipe recipe;
	private TrackDirty parent;

	/*-------------------------------------------------------------------------*/
	public RecipeEditor(
		Recipe recipe,
		TrackDirty parent)
	{
		super("gap 3");
		this.recipe = recipe;
		this.parent = parent;

		stepsTree = new DirtyRecipeTreeView();
		stepsTree.setPrefSize(300, 650);

		stepCards = new CardGroup();

		ProcessStepPane<BatchSparge> batchSpargePane = new BatchSpargePane(this);
		ProcessStepPane<Boil> boilPane = new BoilPane(this);
		ProcessStepPane<Cool> coolPane = new CoolPane(this);
		// dilutePanel = new DilutePanel(dirty);
		ProcessStepPane<Ferment> fermentPane = new FermentPane(this);
		ProcessStepPane<Mash> mashPane = new MashPane(this);
		//	standPanel = new StandPanel(dirty);
		ProcessStepPane<PackageStep> packagePane = new PackagePane(this);
		ProcessStepPane<SplitByPercent> splitByPercentPane = new SplitByPercentPane(this);
		//	mashInfusionPanel = new MashInfusionPanel(dirty);

		stepCards.add(EditorPanel.NONE, new Pane());

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

		FermentableAdditionPane fermentableAdditionPane = new FermentableAdditionPane(this);
		HopAdditionPane hopAdditionPane = new HopAdditionPane(this);
		WaterAdditionPane waterAdditionPane = new WaterAdditionPane(this);
		YeastAdditionPane yeastAdditionPane = new YeastAdditionPane(this);
		MiscAdditionPane miscAdditionPane = new MiscAdditionPane(this);

		stepCards.add(IngredientAddition.Type.HOPS.toString(), hopAdditionPane);
		stepCards.add(IngredientAddition.Type.FERMENTABLES.toString(), fermentableAdditionPane);
		stepCards.add(IngredientAddition.Type.WATER.toString(), waterAdditionPane);
		stepCards.add(IngredientAddition.Type.YEAST.toString(), yeastAdditionPane);
		stepCards.add(IngredientAddition.Type.MISC.toString(), miscAdditionPane);

		HBox recipeEditBar = new HBox(3);
		recipeEditBar.setPadding(new Insets(0, 3, 0, 3));
		recipeEditBar.setAlignment(Pos.CENTER_LEFT);

		addStep = new Button(null, JfxUi.getImageView(JfxUi.addStep, RecipesPane3.SIZE));
		addFermentable = new Button(null, JfxUi.getImageView(JfxUi.addFermentable, RecipesPane3.SIZE));
		addHop = new Button(null, JfxUi.getImageView(JfxUi.addHops, RecipesPane3.SIZE));
		addMisc = new Button(null, JfxUi.getImageView(JfxUi.addMisc, RecipesPane3.SIZE));
		addYeast = new Button(null, JfxUi.getImageView(JfxUi.addYeast, RecipesPane3.SIZE));
		addWater = new Button(null, JfxUi.getImageView(JfxUi.addWater, RecipesPane3.SIZE));
		deleteButton = new Button(null, JfxUi.getImageView(JfxUi.deleteIcon, RecipesPane3.SIZE));
		substitute = new Button(null, JfxUi.getImageView(JfxUi.substituteIcon, RecipesPane3.SIZE));
		applyProcessTemplate = new Button(null, JfxUi.getImageView(JfxUi.processTemplateIcon, RecipesPane3.SIZE));

		ArrayList<String> equipmentProfiles = new ArrayList<>(Database.getInstance().getEquipmentProfiles().keySet());
		equipmentProfiles.sort(String::compareTo);
		equipmentProfile = new ComboBox<>(FXCollections.observableList(equipmentProfiles));

		recipeEditBar.getChildren().add(addStep);
		recipeEditBar.getChildren().add(addFermentable);
		recipeEditBar.getChildren().add(addHop);
		recipeEditBar.getChildren().add(addMisc);
		recipeEditBar.getChildren().add(addYeast);
		recipeEditBar.getChildren().add(addWater);
		recipeEditBar.getChildren().add(substitute);
		recipeEditBar.getChildren().add(applyProcessTemplate);

		recipeEditBar.getChildren().add(new Label(StringUtils.getUiString("recipe.equipment.profile")));
		recipeEditBar.getChildren().add(equipmentProfile);

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
		processTab.add(stepsTree, "aligny top");
		processTab.add(stepCardsPane, "aligny top");

		log = new TextArea();
		log.setEditable(false);

		Tab tab1 = new Tab(StringUtils.getUiString("recipe.process"), processTab);
		Tab tab2 = new Tab(StringUtils.getUiString("recipe.log"), log);

		tabs.getTabs().addAll(tab1, tab2);

		tabs.setPrefSize(800, 650);

		this.add(recipeEditBar, "dock north");
		this.add(tabs, "dock center");
		this.add(stepsEndResult, "dock east");

		// ---- values
		refresh(recipe);

		// ---- listeners

		stepsTree.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				if (newValue != null && newValue != oldValue)
				{
					Object value = stepsTree.getValue(newValue.getValue());

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
				}
			});
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Recipe recipe)
	{
		this.recipe = recipe;
		stepsTree.refresh(recipe);
		stepCards.setVisible(EditorPanel.NONE);
		equipmentProfile.getSelectionModel().select(recipe.getEquipmentProfile());
		refreshEndResult(recipe);
		refreshLog(recipe);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void setDirty(Object dirty)
	{
		if (dirty != null)
		{
			if (dirty instanceof ProcessStep)
			{
				ProcessStep step = (ProcessStep)dirty;
				Recipe recipe = step.getRecipe();

				stepsTree.setDirty(step);
				parent.setDirty(recipe);

				rerunRecipe(recipe);
			}
			else if (dirty instanceof IngredientAddition)
			{
				IngredientAddition addition = (IngredientAddition)dirty;

				stepsTree.setDirty(addition);
//				stepsTree.setDirty(step); // todo need to figure out the step
				parent.setDirty(recipe);

				rerunRecipe(recipe);
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
