package mclachlan.brewday.ui.jfx;

import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class RecipeInfoPane extends MigPane
{
	private Label recipeName;
	private TextArea notes;
	private ComboBox<String> equipmentProfile;

	private TrackDirty parent;
	private boolean refreshing = false;
	private Recipe recipe;

	/*-------------------------------------------------------------------------*/
	public RecipeInfoPane(TrackDirty parent)
	{
		this.parent = parent;

		equipmentProfile = new ComboBox<>();
		recipeName = new Label();
		notes = new TextArea();

		add(new Label(StringUtils.getUiString("recipe.name")));
		add(recipeName, "wrap");

		add(new Label(StringUtils.getUiString("recipe.equipment.profile")));
		add(equipmentProfile, "wrap");

		add(new Label(StringUtils.getUiString("recipe.notes")));
		add(notes, "span");

		// -------------

		equipmentProfile.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			if (recipe != null && newValue != null && !refreshing)
			{
				recipe.setEquipmentProfile(newValue);

				parent.setDirty(recipe);
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Recipe recipe)
	{
		refreshing = true;

		this.recipe = recipe;

		ArrayList<String> equipmentProfiles = new ArrayList<>(
			Database.getInstance().getEquipmentProfiles().keySet());
		equipmentProfiles.sort(String::compareTo);
		equipmentProfile.setItems(FXCollections.observableList(equipmentProfiles));

		if (recipe != null)
		{
			recipeName.setText(recipe.getName());
			equipmentProfile.getSelectionModel().select(recipe.getEquipmentProfile());
		}

		refreshing = false;
	}
}
