package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.recipe.Recipe;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class RecipePane extends MigPane
{
	private final ListView<String> list;

	private Model<Recipe> model;
	private FormController formController;
	private ListController listController;
	private String dirtyFlag;

	public RecipePane(String dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;
		this.list = new ListView<>();

		GridPane form = new GridPane();
		form.setAlignment(Pos.TOP_LEFT);
		form.setHgap(5);
		form.setVgap(5);
		form.setPadding(new Insets(5, 5, 5, 5));

		this.add(list);
		this.add(form);
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
		Recipe ep = (Recipe)selected;

		// todo

	}

	/*-------------------------------------------------------------------------*/

	public static class Model<T extends V2DataObject>
	{
		private Map<String, V2DataObject> map;
		private ObjectProperty<String> current;
		private String dirtyFlag;

		public Model(Map<String, V2DataObject> map, String currentSelection, String dirtyFlag)
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
