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

import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.*;
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
	public RecipeInfoPane(RecipeEditor recipeEditor, RecipeTreeViewModel stepsTree)
	{
		this.parent = (TrackDirty)recipeEditor;

		Button addStep = new Button(
			StringUtils.getUiString("recipe.add.step"),
			JfxUi.getImageView(JfxUi.addStep, JfxUi.ICON_SIZE));

		Button applyProcessTemplate = new Button(
			StringUtils.getUiString("recipe.apply.process.template"),
			JfxUi.getImageView(JfxUi.processTemplateIcon, JfxUi.ICON_SIZE));

		Button rerunRecipe = new Button(
			StringUtils.getUiString("recipe.rerun"),
			JfxUi.getImageView(JfxUi.recipeIcon, JfxUi.ICON_SIZE));

		ToolBar recipeEditBar = new ToolBar();
		recipeEditBar.setPadding(new Insets(3, 3, 6, 3));

		recipeEditBar.getItems().add(addStep);
		recipeEditBar.getItems().add(applyProcessTemplate);
		recipeEditBar.getItems().add(rerunRecipe);

		add(recipeEditBar, "dock north");

		equipmentProfile = new ComboBox<>();
		recipeName = new Label();
		notes = new TextArea();
		notes.setPrefWidth(420);

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

		addStep.setOnAction(event ->
		{
			NewStepDialog dialog = new NewStepDialog();

			dialog.showAndWait();
			ProcessStep.Type result = dialog.getOutput();
			if (result != null)
			{
				ProcessStep step;

				switch (result)
				{
					case BATCH_SPARGE:
						step = new BatchSparge(recipe);
						break;
					case BOIL:
						step = new Boil(recipe);
						break;
					case COOL:
						step = new Cool(recipe);
						break;
					case HEAT:
						step = new Heat(recipe);
						break;
					case DILUTE:
						step = new Dilute(recipe);
						break;
					case FERMENT:
						step = new Ferment(recipe);
						break;
					case MASH:
						step = new Mash(recipe);
						break;
					case STAND:
						step = new Stand(recipe);
						break;
					case PACKAGE:
						step = new PackageStep(recipe);
						break;
					case LAUTER:
						step = new Lauter(recipe);
						break;
					case MASH_INFUSION:
						step = new MashInfusion(recipe);
						break;
					case SPLIT:
						step = new Split(recipe);
						break;
					case COMBINE:
						step = new Combine(recipe);
						break;
					default: throw new BrewdayException("invalid "+result);
				}

				recipe.getSteps().add(step);
//				recipe.run();
				stepsTree.addStep(step);
				parent.setDirty(step);
			}
		});

		applyProcessTemplate.setOnAction(event ->
		{
			ApplyNewProcessTemplateDialog dialog = new ApplyNewProcessTemplateDialog();

			dialog.showAndWait();
			String output = dialog.getOutput();

			if (output != null)
			{
				Recipe newProcessTemplate = Database.getInstance().getProcessTemplates().get(output);
				this.recipe.applyProcessTemplate(newProcessTemplate);
				this.refresh(this.recipe);
				stepsTree.refresh(recipe);
				parent.setDirty(this.recipe);
				for (ProcessStep step : this.recipe.getSteps())
				{
					parent.setDirty(step);
				}
			}
		});

		rerunRecipe.setOnAction(event -> recipeEditor.rerunRecipe(recipe));
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
