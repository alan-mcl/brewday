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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.document.DocumentCreator;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.Recipe;
import org.tbee.javafx.scene.layout.MigPane;
import mclachlan.brewday.ui.jfx.tagbar.TagPane;

/**
 *
 */
public class RecipeInfoPane extends MigPane
{
	private final Label recipeName;
	private final TextArea recipeDescription;
	private final ComboBox<String> equipmentProfile;

	private final TagPane tagBar; // todo tag editing

	private final TrackDirty parent;
	private boolean refreshing = false;
	private Recipe recipe;

	/*-------------------------------------------------------------------------*/
	public RecipeInfoPane(RecipeEditor recipeEditor, RecipeTreeViewModel stepsTree)
	{
		this.parent = (TrackDirty)recipeEditor;

		Button addStep = new Button(
			StringUtils.getUiString("recipe.add.step"),
			JfxUi.getImageView(JfxUi.addStep, JfxUi.ICON_SIZE));

		Button rerunRecipe = new Button(
			StringUtils.getUiString("recipe.rerun"),
			JfxUi.getImageView(JfxUi.recipeIcon, JfxUi.ICON_SIZE));

		ToolBar recipeEditBar = new ToolBar();
		recipeEditBar.setPadding(new Insets(3, 3, 6, 3));

		recipeEditBar.getItems().add(addStep);
		recipeEditBar.getItems().add(rerunRecipe);

		add(recipeEditBar, "dock north");

		equipmentProfile = new ComboBox<>();
		recipeName = new Label();
		recipeDescription = new TextArea();
		recipeDescription.setPrefWidth(420);
		recipeDescription.setWrapText(true);

		tagBar = new TagPane(new ArrayList<>());

		add(new Label(StringUtils.getUiString("recipe.name")));
		add(recipeName, "wrap");

		add(new Label(StringUtils.getUiString("recipe.equipment.profile")));
		add(equipmentProfile, "wrap");

		add(new Label(StringUtils.getUiString("recipe.tags")));
		add(tagBar, "wrap");

		add(new Label(StringUtils.getUiString("recipe.desc")));
		add(recipeDescription, "span");

		MigPane buttonGrid = new MigPane();

		Button genDoc = new Button(
			StringUtils.getUiString("doc.gen.generate.document"),
			JfxUi.getImageView(JfxUi.documentIcon, JfxUi.ICON_SIZE));

		Button applyProcessTemplate = new Button(
			StringUtils.getUiString("recipe.apply.process.template"),
			JfxUi.getImageView(JfxUi.processTemplateIcon, JfxUi.ICON_SIZE));

		buttonGrid.add(genDoc, "wrap");
		buttonGrid.add(applyProcessTemplate);

		add(buttonGrid, "span");

		// -------------
		tagBar.onTagAdd(s ->
			{
				recipe.getTags().add(s);
				parent.setDirty(recipe);
			});

		tagBar.onTagRemove(s ->
			{
				recipe.getTags().remove(s);
				parent.setDirty(recipe);
			});

		equipmentProfile.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			if (recipe != null && newValue != null && !refreshing)
			{
				recipe.setEquipmentProfile(newValue);

				parent.setDirty(recipe);
			}
		});

		recipeDescription.textProperty().addListener((observable, oldValue, newValue) ->
		{
			if (!refreshing)
			{
				recipe.setDescription(recipeDescription.getText());
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

		genDoc.setOnAction(event ->
		{
			List<String> documentTemplates = Database.getInstance().getDocumentTemplates();

			if (documentTemplates == null || documentTemplates.isEmpty())
			{
				return;
			}

			ChoiceDialog<String> dialog = new ChoiceDialog<>(
				documentTemplates.get(0),
				documentTemplates.toArray(new String[0]));
			dialog.setTitle(StringUtils.getUiString("doc.gen.generate.document"));
			dialog.setContentText(StringUtils.getUiString("doc.gen.choose.template"));
			dialog.setGraphic(JfxUi.getImageView(JfxUi.documentIcon, JfxUi.ICON_SIZE));

			JfxUi.styleScene(dialog.getDialogPane().getScene());

			dialog.showAndWait();
			String template = dialog.getResult();

			if (template != null)
			{
				String defaultSuffix = template.substring(0, template.indexOf("."));

				String extension = template.substring(
					template.indexOf(".")+1,
					template.lastIndexOf("."));

				FileChooser chooser = new FileChooser();

				FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
					"."+extension, extension);
				chooser.setSelectedExtensionFilter(filter);

				chooser.setInitialFileName(
					recipe.getName().replaceAll("\\W", "_")+ "_"+defaultSuffix+"."+ extension);

				File file = chooser.showSaveDialog(JfxUi.getInstance().getMainScene().getWindow());
				if (file != null)
				{
					DocumentCreator dc = DocumentCreator.getInstance();

					try
					{
						dc.createDocument(recipe, template, file);
						Desktop.getDesktop().open(file);
					}
					catch (IOException ex)
					{
						throw new BrewdayException(ex);
					}
				}
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
			recipeDescription.setText(recipe.getDescription());
			equipmentProfile.getSelectionModel().select(recipe.getEquipmentProfile());
//			tags.setText(String.join(",", recipe.getTags()));
//			tagBar.getEntries().addAll(recipe.getTags());

			tagBar.refresh(recipe.getTags(), Brewday.getInstance().getRecipeTags());
		}

		refreshing = false;
	}
}
