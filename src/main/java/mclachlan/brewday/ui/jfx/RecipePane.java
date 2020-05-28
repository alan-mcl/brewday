package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.*;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class RecipePane extends MigPane
{
	private final ListView<String> list;

	private TreeView stepsTree;

	private Model<Recipe> model;
	private FormController formController;
	private ListController listController;
	private String dirtyFlag;

	public RecipePane(String dirtyFlag)
	{
		this.setPadding(new Insets(5, 5, 5, 5));

		this.dirtyFlag = dirtyFlag;
		this.list = new ListView<>();

		list.setCellFactory(param -> new ListCell<>()
		{
			private ImageView imageView = JfxUi.getImageView(JfxUi.recipeIcon, 24);

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

		this.add(list, "dock west");
		this.add(stepsTree, "dock center, gap 5");
	}

	public void refresh(Database db)
	{
		model = new Model(db.getRecipes(), null, dirtyFlag);
		formController = new FormController(model, this);
		listController = new ListController(list, model);

		if (model.getItems().size() > 0)
		{
			model.setCurrent(model.getItems().get(0));
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
		private RecipePane form;

		public FormController(Model model, RecipePane form)
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
