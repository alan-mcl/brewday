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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.ui.swing.*;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class RecipesPane2 extends MigPane implements TrackDirty
{

	// ingredients tab
	private RecipeComponent recipeComponent;
	private ChoiceBox<String> equipmentProfile;

	// process tab
	private Button applyProcessTemplate;
	private Button addStep, remove, duplicate, substitute;
	private Button addFermentable, addHop, addMisc, addYeast, addWater;

	private CardGroup stepCards;
	private ProcessStepPane mashInfusionPanel, batchSpargePanel, boilPanel,
		coolPanel, dilutePanel, fermentPanel, mashPane,
		standPanel, packagePanel, splitByPercentPanel;
	private FermentableAdditionPane fermentableAdditionPane;
	private HopAdditionPanel hopAdditionPanel;
	private WaterAdditionPanel waterAdditionPanel;
	private YeastAdditionPanel yeastAdditionPanel;
	private MiscAdditionPanel miscAdditionPanel;

	private DirtyList<Recipe> list;
	private DirtyTreeView stepsTree;
	private TextArea stepsEndResult;
	private TextArea log;

	private Map<String, Recipe> map;
	private ObjectProperty<Recipe> current;
	private String dirtyFlag;
	private boolean detectDirty = true;
	private TrackDirty parent;

	// ribbon buttons
	private Button addRecipeButton, copyButton, renameButton, deleteButton,
		saveAllButton, discardAllButton;

	/*-------------------------------------------------------------------------*/

	public RecipesPane2(String dirtyFlag, TrackDirty parent)
	{
		this.parent = parent;

		this.dirtyFlag = dirtyFlag;
		this.list = new DirtyList<>();

		stepsTree = new DirtyTreeView();

		stepCards = new CardGroup();

//		batchSpargePanel = new BatchSpargePanel(dirtyFlag);
//		boilPanel = new BoilPanel(dirtyFlag);
//		coolPanel = new CoolPanel(dirtyFlag);
//		dilutePanel = new DilutePanel(dirtyFlag);
//		fermentPanel = new FermentPanel(dirtyFlag);
		mashPane = new MashPane(this);
//		standPanel = new StandPanel(dirtyFlag);
//		packagePanel = new PackagePanel(dirtyFlag);
//		splitByPercentPanel = new SplitByPercentPanel(dirtyFlag);
//		mashInfusionPanel = new MashInfusionPanel(dirtyFlag);

		stepCards.add(EditorPanel.NONE, new Pane());

//		stepCards.add(ProcessStep.Type.BATCH_SPARGE.toString(), batchSpargePanel);
//		stepCards.add(ProcessStep.Type.BOIL.toString(), boilPanel);
//		stepCards.add(ProcessStep.Type.COOL.toString(), coolPanel);
//		stepCards.add(ProcessStep.Type.DILUTE.toString(), dilutePanel);
//		stepCards.add(ProcessStep.Type.FERMENT.toString(), fermentPanel);
		stepCards.add(ProcessStep.Type.MASH.toString(), mashPane);
//		stepCards.add(ProcessStep.Type.STAND.toString(), standPanel);
//		stepCards.add(ProcessStep.Type.PACKAGE.toString(), packagePanel);
//		stepCards.add(ProcessStep.Type.MASH_INFUSION.toString(), mashInfusionPanel);
//		stepCards.add(ProcessStep.Type.SPLIT_BY_PERCENT.toString(), splitByPercentPanel);

		fermentableAdditionPane = new FermentableAdditionPane(this);
//		hopAdditionPanel = new HopAdditionPanel();
//		waterAdditionPanel = new WaterAdditionPanel();
//		yeastAdditionPanel = new YeastAdditionPanel();
//		miscAdditionPanel = new MiscAdditionPanel();

//		stepCards.add(IngredientAddition.Type.HOPS.toString(), hopAdditionPanel);
		stepCards.add(IngredientAddition.Type.FERMENTABLES.toString(), fermentableAdditionPane);
//		stepCards.add(IngredientAddition.Type.WATER.toString(), waterAdditionPanel);
//		stepCards.add(IngredientAddition.Type.YEAST.toString(), yeastAdditionPanel);
//		stepCards.add(IngredientAddition.Type.MISC.toString(), miscAdditionPanel);

		// todo remove---
//		stepCards.setVisible(ProcessStep.Type.MASH.toString());
		// --------------

		TilePane ribbonBar = new TilePane(3, 3);
		ribbonBar.setPadding(new Insets(0,0,3,0));

		int size = 32;
		saveAllButton = new Button(null, getImageView(JfxUi.saveIcon, size));
		discardAllButton = new Button(null, getImageView(JfxUi.undoIcon, size));
		addRecipeButton = new Button(null, getImageView(JfxUi.addRecipe, size));
		copyButton = new Button(null, getImageView(JfxUi.duplicateIcon, size));
		renameButton = new Button(null, getImageView(JfxUi.renameIcon, size));
		deleteButton = new Button(null, getImageView(JfxUi.deleteIcon, size));
		addStep = new Button(null, getImageView(JfxUi.addStep, size));
		addFermentable = new Button(null, getImageView(JfxUi.addFermentable, size));
		addHop = new Button(null, getImageView(JfxUi.addHops, size));
		addMisc = new Button(null, getImageView(JfxUi.addMisc, size));
		addYeast = new Button(null, getImageView(JfxUi.addYeast, size));
		addWater = new Button(null, getImageView(JfxUi.addWater, size));
		substitute = new Button(null, getImageView(JfxUi.substituteIcon, size));
		applyProcessTemplate = new Button(null, getImageView(JfxUi.processTemplateIcon, size));


		ribbonBar.getChildren().add(addRecipeButton);
		ribbonBar.getChildren().add(copyButton);
		ribbonBar.getChildren().add(renameButton);
		ribbonBar.getChildren().add(deleteButton);
		ribbonBar.getChildren().add(addStep);
		ribbonBar.getChildren().add(addFermentable);
		ribbonBar.getChildren().add(addHop);
		ribbonBar.getChildren().add(addMisc);
		ribbonBar.getChildren().add(addYeast);
		ribbonBar.getChildren().add(addWater);
		ribbonBar.getChildren().add(substitute);
		ribbonBar.getChildren().add(applyProcessTemplate);

//		for (Node n : ribbonBar.getChildren())
//		{
//			Button b = (Button)n;
//			b.setPrefSize(80, 30);
//		}


		stepsEndResult = new TextArea();
		stepsEndResult.setEditable(false);
		stepsEndResult.setMaxWidth(200);

		MigPane stepsAndButtons = new MigPane();
		stepsAndButtons.add(stepsTree, "dock center");
//		stepsAndButtons.add(buttons, "dock south");

		MigPane stepCardsPane = new MigPane();
		stepCardsPane.add(stepCards);

		MigPane center = new MigPane();

		center.add(stepsAndButtons);
		center.add(stepCardsPane);

		TabPane tabs = new TabPane();
		tabs.getStyleClass().add("floating");

		MigPane processTab = new MigPane();
		processTab.add(stepsAndButtons, "aligny top");
		processTab.add(stepCardsPane, "aligny top");
		processTab.add(stepsEndResult, "aligny top, alignx right");

		Tab tab1 = new Tab("Process", processTab);
		Tab tab2 = new Tab("Log", new Label("log"));

		tabs.getTabs().addAll(tab1, tab2);

		list.setPrefWidth(200);
		list.setPrefHeight(650);

		tabs.setPrefWidth(900);
		tabs.setPrefHeight(650);

		this.add(ribbonBar, "dock north, alignx left");
		this.add(list, "dock west, aligny top");
		this.add(tabs, "dock center, aligny top");

		//-------------

		current = new SimpleObjectProperty<>();

		stepsTree.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				if (newValue != null && newValue != oldValue)
				{
					Object value = stepsTree.getValue(newValue.getValue());

					if (value instanceof ProcessStep)
					{
						stepCards.setVisible(((ProcessStep)value).getType().toString());
						if (value instanceof Mash)
						{
							mashPane.refresh((Mash)value, getCurrent());
						}
					}
					else if (value instanceof IngredientAddition)
					{
						stepCards.setVisible(((IngredientAddition)value).getType().toString());
						if (value instanceof FermentableAddition)
						{
							fermentableAdditionPane.refresh((FermentableAddition)value, getCurrent());
						}
					}
				}
			});

		current.addListener((observable, oldValue, newValue) ->
		{
			refresh(newValue);
		});

		list.getSelectionModel().selectedItemProperty().addListener(
			(obs, oldSelection, newSelection) ->
			{
				Recipe value = list.getValue(newSelection);
				setCurrent(value);
			});
	}

	/*-------------------------------------------------------------------------*/
	public void setCurrent(Recipe recipe)
	{
		this.current.setValue(recipe);

		recipe.run();
	}

	/*-------------------------------------------------------------------------*/
	public Recipe getCurrent()
	{
		return current.get();
	}

	/*-------------------------------------------------------------------------*/
	public Map<String, Recipe> getMap()
	{
		return map;
	}

	/*-------------------------------------------------------------------------*/
	private ImageView getImageView(Image newIcon, int i)
	{
		return JfxUi.getImageView(newIcon, i);
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Database db)
	{
		this.detectDirty = false;

		this.map = db.getRecipes();
		if (map.size() > 0)
		{
			List<Recipe> recipes = new ArrayList<>(this.map.values());
			recipes.sort(Comparator.comparing(Recipe::getName));

			list.removeAll();
			list.addAll(recipes, JfxUi.recipeIcon);
			list.select(recipes.get(0));

			stepCards.setVisible(EditorPanel.NONE);
		}

		this.detectDirty = true;
	}

	/*-------------------------------------------------------------------------*/
	private void refresh(Recipe recipe)
	{
		this.detectDirty = false;

		recipe.run();

		stepsTree.refresh(recipe);
		stepCards.setVisible(EditorPanel.NONE);
		refreshEndResult(recipe);

		this.detectDirty = true;
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

	/*-------------------------------------------------------------------------*/
	@Override
	public void setDirty(Object dirtyFlag)
	{
		ProcessStep step = (ProcessStep)dirtyFlag;

		if (detectDirty)
		{
			if (step != null)
			{
				Recipe recipe = step.getRecipe();
				list.setDirty(recipe);

				stepsTree.setDirty(step);
			}

			parent.setDirty(this.dirtyFlag);
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void clearDirty()
	{
		stepsTree.clearAllDirty();
		list.clearAllDirty();
	}

	/*-------------------------------------------------------------------------*/
	private static class DirtyTreeView extends TreeView<Label>
	{
		private Map<Object, Label> nodes = new HashMap<>();
		private Map<Label, Object> values = new HashMap<>();

		public void refresh(Recipe recipe)
		{
			this.setRoot(null);
			TreeItem<Label> root = getTreeItem(recipe.getName(), recipe, JfxUi.recipeIcon);
			this.setRoot(root);
			root.setExpanded(true);

			List<ProcessStep> steps = recipe.getSteps();

			for (ProcessStep step : steps)
			{
				TreeItem<Label> stepItem = getTreeItem(step.getName(), step, JfxUi.stepIcon);

				for (IngredientAddition addition : step.getIngredients())
				{
					Image icon;
					if (addition instanceof WaterAddition)
					{
						icon = JfxUi.waterIcon;
					}
					else if (addition instanceof FermentableAddition)
					{
						icon = JfxUi.grainsIcon;
					}
					else if (addition instanceof HopAddition)
					{
						icon = JfxUi.hopsIcon;
					}
					else if (addition instanceof YeastAddition)
					{
						icon = JfxUi.yeastIcon;
					}
					else if (addition instanceof MiscAddition)
					{
						icon = JfxUi.miscIcon;
					}
					else
					{
						throw new BrewdayException("unrecognised: " + addition);
					}

					TreeItem<Label> additionItem = getTreeItem(addition.toString(), addition, icon);
					stepItem.getChildren().add(additionItem);
				}

				root.getChildren().add(stepItem);
			}
		}

		public Object getValue(Label label)
		{
			return this.values.get(label);
		}

		public void setDirty(Object obj)
		{
			Label label = nodes.get(obj);

			if (label != null)
			{
				getRoot().getValue().setStyle("-fx-font-weight: bold;");
				label.setStyle("-fx-font-weight: bold;");
			}
		}

		public void clearAllDirty()
		{
			for (Label l : nodes.values())
			{
				l.setStyle("");
			}
		}

		private TreeItem<Label> getTreeItem(String text, Object value, Image icon)
		{
			Label label = new Label(text);
			TreeItem<Label> result = new TreeItem<>(label, JfxUi.getImageView(icon, 24));

			nodes.put(value, label);
			values.put(label, value);

			return result;
		}
	}

	/*-------------------------------------------------------------------------*/
	private static class DirtyList<T extends V2DataObject> extends ListView<Label>
	{
		private Map<T, Label> nodes = new HashMap<>();
		private Map<Label, T> values = new HashMap<>();

		public void add(T t, Image graphic)
		{
			Label label = new DirtyLabel<>(t, t.getName(), JfxUi.getImageView(graphic, 24));
			nodes.put(t, label);
			values.put(label, t);
			super.getItems().add(label);
		}

		public void addAll(List<T> ts, Image graphic)
		{
			for (T t : ts)
			{
				add(t, graphic);
			}
		}

		public T getValue(Label label)
		{
			return values.get(label);
		}

		public void removeAll()
		{
			this.getChildren().clear();
		}

		public void select(T t)
		{
			getSelectionModel().select(nodes.get(t));
		}

		public void setDirty(T t)
		{
			Label label = nodes.get(t);
			label.setStyle("-fx-font-weight: bold;");
		}

		public void clearAllDirty()
		{
			for (Label l : nodes.values())
			{
				l.setStyle("");
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private static class DirtyLabel<T> extends Label
	{
		private T t;

		public DirtyLabel(T t, String text, Node graphic)
		{
			super(text, graphic);
			this.t = t;
		}
	}
}
