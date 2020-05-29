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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.ui.swing.*;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class RecipesPane2 extends MigPane
{
	private TextArea stepsEndResult, log;

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
	private FermentableAdditionPanel fermentableAdditionPanel;
	private HopAdditionPanel hopAdditionPanel;
	private WaterAdditionPanel waterAdditionPanel;
	private YeastAdditionPanel yeastAdditionPanel;
	private MiscAdditionPanel miscAdditionPanel;
	private TreeView stepsTree;

	private ListView<String> list;

	private Model<Recipe> model;
	private FormController formController;
	private ListController listController;
	private String dirtyFlag;

	/*-------------------------------------------------------------------------*/
	public RecipesPane2(String dirtyFlag)
	{
		this.setPadding(new Insets(5, 5, 5, 5));

		this.dirtyFlag = dirtyFlag;
		this.list = new ListView<>();

		list.setCellFactory(param -> new ListCell<>()
		{
			private ImageView imageView = getImageView(JfxUi.recipeIcon, 24);

			@Override
			public void updateItem(String name, boolean empty)
			{
				super.updateItem(name, empty);
				if (empty)
				{
					setText(null);
					setGraphic(null);
				}
				else
				{
					setText(name);
					setGraphic(imageView);
				}
			}
		});

		stepsTree = new TreeView<>();

		addStep = new Button(StringUtils.getUiString("recipe.add.step"), getImageView(JfxUi.newIcon, 32));
		addFermentable = new Button(StringUtils.getUiString("common.add"), getImageView(JfxUi.grainsIcon, 32));
		addHop = new Button(StringUtils.getUiString("common.add"), getImageView(JfxUi.hopsIcon, 32));
		addMisc = new Button(StringUtils.getUiString("common.add"), getImageView(JfxUi.miscIcon, 32));
		addYeast = new Button(StringUtils.getUiString("common.add"), getImageView(JfxUi.yeastIcon, 32));
		addWater = new Button(StringUtils.getUiString("common.add"), getImageView(JfxUi.waterIcon, 32));
		remove = new Button(StringUtils.getUiString("common.remove"), getImageView(JfxUi.removeIcon, 32));
		duplicate = new Button(StringUtils.getUiString("common.duplicate"), getImageView(JfxUi.duplicateIcon, 32));
		substitute = new Button(StringUtils.getUiString("common.substitute"), getImageView(JfxUi.substituteIcon, 32));
		applyProcessTemplate = new Button(StringUtils.getUiString("recipe.apply.process.template"), getImageView(JfxUi.processTemplateIcon, 32));

		MigPane buttons = new MigPane();

		buttons.add(addStep, "grow");
		buttons.add(addFermentable, "grow");
		buttons.add(addWater, "grow,wrap");

		buttons.add(addHop, "grow");
		buttons.add(addYeast, "grow");
		buttons.add(addMisc, "grow,wrap");

		buttons.add(substitute, "grow");
		buttons.add(duplicate, "grow");
		buttons.add(remove, "grow,wrap");

		buttons.add(applyProcessTemplate, "span, wrap");

		stepCards = new CardGroup();

//		batchSpargePanel = new BatchSpargePanel(dirtyFlag);
//		boilPanel = new BoilPanel(dirtyFlag);
//		coolPanel = new CoolPanel(dirtyFlag);
//		dilutePanel = new DilutePanel(dirtyFlag);
//		fermentPanel = new FermentPanel(dirtyFlag);
		mashPane = new MashPane(dirtyFlag);
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

//		fermentableAdditionPanel = new FermentableAdditionPanel();
//		hopAdditionPanel = new HopAdditionPanel();
//		waterAdditionPanel = new WaterAdditionPanel();
//		yeastAdditionPanel = new YeastAdditionPanel();
//		miscAdditionPanel = new MiscAdditionPanel();

//		stepCards.add(IngredientAddition.Type.HOPS.toString(), hopAdditionPanel);
//		stepCards.add(IngredientAddition.Type.FERMENTABLES.toString(), fermentableAdditionPanel);
//		stepCards.add(IngredientAddition.Type.WATER.toString(), waterAdditionPanel);
//		stepCards.add(IngredientAddition.Type.YEAST.toString(), yeastAdditionPanel);
//		stepCards.add(IngredientAddition.Type.MISC.toString(), miscAdditionPanel);

		// todo remove---
//		stepCards.setVisible(ProcessStep.Type.MASH.toString());
		// --------------


		MigPane stepsAndButtons = new MigPane();
		stepsAndButtons.add(stepsTree, "dock center");
		stepsAndButtons.add(buttons, "dock south");

		MigPane stepCardsPane = new MigPane();
		stepCardsPane.add(stepCards);

		MigPane center = new MigPane();

		center.add(stepsAndButtons);
		center.add(stepCardsPane);

		this.add(list);
		this.add(stepsAndButtons);
		this.add(stepCardsPane);

		stepsTree.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				if (newValue != null && newValue != oldValue)
				{
					TreeItem treeItem = (TreeItem)newValue;
					Object value = treeItem.getValue();
					if (value instanceof Mash)
					{
						stepCards.setVisible(ProcessStep.Type.MASH.toString());
						mashPane.refresh((Mash)value, model.getCurrent());
					}
				}
			});
	}

	/*-------------------------------------------------------------------------*/
	private ImageView getImageView(Image newIcon, int i)
	{
		return JfxUi.getImageView(newIcon, i);
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Database db)
	{
		model = new Model(db.getRecipes(), null, dirtyFlag);
		formController = new FormController(model, this);
		listController = new ListController(list, model);

		if (model.getItems().size() > 0)
		{
			list.getSelectionModel().select(model.getItems().get(0));
		}
	}

	private void refresh(V2DataObject selected)
	{
		Recipe recipe = (Recipe)selected;

		List<ProcessStep> steps = recipe.getSteps();

		// clear the tree
		stepsTree.setRoot(null);
		TreeItem root = new TreeItem(recipe.getName(), JfxUi.getImageView(JfxUi.recipeIcon, 24));
		stepsTree.setRoot(root);
		root.setExpanded(true);

		for (ProcessStep step : steps)
		{
			TreeItem stepItem = new TreeItem(step, JfxUi.getImageView(JfxUi.stepIcon, 24));

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

				TreeItem<IngredientAddition> additionItem = new TreeItem<>(
					addition, JfxUi.getImageView(icon, 24));
				stepItem.getChildren().add(additionItem);
			}

			root.getChildren().add(stepItem);
		}
	}

	/*-------------------------------------------------------------------------*/
/*	protected void runRecipe()
	{
		recipe.run();
	}

	private void refreshLog()
	{
		log.setText("");

		StringBuilder sb = new StringBuilder();

		for (String s : recipe.getLog().getMsgs())
		{
			s = s.replaceAll("\n", "; ");
			sb.append(s).append("\n");
		}

		log.setText(sb.toString());
	}*/

	/*-------------------------------------------------------------------------*/
/*
	protected void refreshEndResult()
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

*/
	/*-------------------------------------------------------------------------*/
/*	protected void refreshStepCards()
	{
		TreePath selected = stepsTree.getSelectionPath();

		if (selected != null)
		{
			Object last = selected.getLastPathComponent();
			if (last instanceof ProcessStep)
			{
				refreshStepCards((ProcessStep)last);
			}
			else if (last instanceof IngredientAddition)
			{
				refreshStepCards((IngredientAddition)last);
			}
			else
			{
				refreshStepCards((ProcessStep)null);
			}

			stepsTreeModel.fireNodeChanged(last);
		}
	}*/

	/*-------------------------------------------------------------------------*/
/*
	private void refreshStepCards(ProcessStep step)
	{
		if (step != null)
		{
			switch (step.getType())
			{
				case BATCH_SPARGE:
					batchSpargePanel.refresh(step, recipe);
					break;
				case BOIL:
					boilPanel.refresh(step, recipe);
					break;
				case COOL:
					coolPanel.refresh(step, recipe);
					break;
				case DILUTE:
					dilutePanel.refresh(step, recipe);
					break;
				case FERMENT:
					fermentPanel.refresh(step, recipe);
					break;
				case MASH:
					mashPane.refresh(step, recipe);
					break;
				case STAND:
					standPanel.refresh(step, recipe);
					break;
				case PACKAGE:
					packagePanel.refresh(step, recipe);
					break;
				case MASH_INFUSION:
					mashInfusionPanel.refresh(step, recipe);
					break;
				case SPLIT_BY_PERCENT:
					splitByPercentPanel.refresh(step, recipe);
					break;
				default:
					throw new BrewdayException("Invalid step " + step.getType());
			}

			stepCardLayout.show(stepCards, step.getType().toString());
		}
		else
		{
			stepCardLayout.show(stepCards, EditorPanel.NONE);
		}
	}*/

	/*-------------------------------------------------------------------------*/
/*
	private void refreshStepCards(IngredientAddition item)
	{
		if (item != null)
		{
			switch (item.getType())
			{
				case HOPS:
					hopAdditionPanel.refresh((HopAddition)item);
					break;
				case FERMENTABLES:
					fermentableAdditionPanel.refresh((FermentableAddition)item);
					break;
				case WATER:
					waterAdditionPanel.refresh((WaterAddition)item);
					break;
				case YEAST:
					yeastAdditionPanel.refresh((YeastAddition)item);
					break;
				case MISC:
					miscAdditionPanel.refresh((MiscAddition)item);
					break;
				default:
					throw new BrewdayException("Invalid: [" + item.getType() + "]");
			}

			stepCardLayout.show(stepCards, item.getType().toString());
		}
		else
		{
			stepCardLayout.show(stepCards, EditorPanel.NONE);
		}
	}*/

	/*-------------------------------------------------------------------------*/

	public static class Model<T extends V2DataObject>
	{
		private Map<String, V2DataObject> map;
		private ObjectProperty<String> current;
		private String dirtyFlag;

		public Model(Map<String, V2DataObject> map, String currentSelection,
			String dirtyFlag)
		{
			this.map = map;
			this.current = new SimpleObjectProperty<>(currentSelection);
			this.dirtyFlag = dirtyFlag;

			addDataListeners();
		}

		public void addDataListeners()
		{

		}

		public ObservableList<String> getItems()
		{
			ArrayList<String> keys = new ArrayList<>(map.keySet());
			keys.sort(String::compareTo);

			return FXCollections.observableList(keys);
		}

		public void setCurrent(String newSelection)
		{
			this.current.setValue(newSelection);

			getCurrent().run();
		}

		public Recipe getCurrent()
		{
			return (Recipe)map.get(current.getValue());
		}

		public Map<String, V2DataObject> getMap()
		{
			return map;
		}
	}

	/*-------------------------------------------------------------------------*/
	public static class FormController
	{
		private Model model;
		private RecipesPane2 form;

		public FormController(Model model, RecipesPane2 form)
		{
			this.model = model;
			this.form = form;

			model.current.addListener((observable, oldValue, newValue) ->
			{
				V2DataObject selected = (V2DataObject)model.getMap().get(newValue);
				form.refresh(selected);
			});
		}
	}

	/*-------------------------------------------------------------------------*/
	public static class ListController
	{
		private ListView<String> list;
		private Model model;

		public ListController(ListView<String> list, Model model)
		{
			this.list = list;
			this.model = model;

			this.list.setItems(model.getItems());

			list.getSelectionModel().selectedItemProperty().addListener(
				(obs, oldSelection, newSelection) -> model.setCurrent(newSelection));
		}
	}
}
