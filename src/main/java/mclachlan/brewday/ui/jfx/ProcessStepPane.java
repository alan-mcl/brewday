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

import java.util.*;
import java.util.function.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class ProcessStepPane<T extends ProcessStep> extends MigPane
{
	protected boolean detectDirty = true, refreshing = false;

	private T step;
	private final TrackDirty parent;
	private final RecipeTreeViewModel model;
	private boolean processTemplateMode;

	private final TextField name;
	private final TextArea desc;

	private final UnitControlUtils<T> unitControlUtils;

	// input volumes
	private final Map<ComboBox<String>, InputVolumeComboBoxInfo> inputVolumeCombos = new HashMap<>();

	// computed volume panes
	private final Map<ComputedVolumePane, Function<T, String>> computedVolumePanes = new HashMap<>();

	public UnitControlUtils<T> getUnitControlUtils()
	{
		return unitControlUtils;
	}

	/*-------------------------------------------------------------------------*/
	public enum ButtonType
	{
		ADD_FERMENTABLE, ADD_HOP, ADD_WATER, ADD_YEAST, ADD_MISC, DELETE, DUPLICATE
	}

	/*-------------------------------------------------------------------------*/
	public ProcessStepPane(TrackDirty parent, RecipeTreeViewModel model,
		boolean processTemplateMode)
	{
		this.parent = parent;
		this.model = model;
		this.processTemplateMode = processTemplateMode;

		name = new TextField();
		desc = new TextArea();

		unitControlUtils = new UnitControlUtils<>(this.getParentTrackDirty());

		detectDirty = false;
		buildUiInternal();
		detectDirty = true;
	}

	/*-------------------------------------------------------------------------*/
	protected void buildUiInternal()
	{

	}

	/*-------------------------------------------------------------------------*/
	public String getDescription()
	{
		return desc.getText();
	}

	/*-------------------------------------------------------------------------*/
	public T getStep()
	{
		return step;
	}

	/*-------------------------------------------------------------------------*/
	public TrackDirty getParentTrackDirty()
	{
		return parent;
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(T step, Recipe recipe)
	{
		detectDirty = false;
		refreshing = true;

		this.step = step;
		if (step != null)
		{
			name.setText(step.getName());
			desc.setText(step.getDescription());
		}

		// update input volumes
		for (ComboBox<String> cb : inputVolumeCombos.keySet())
		{
			InputVolumeComboBoxInfo comboBoxInfo = inputVolumeCombos.get(cb);
			cb.setItems(getVolumesOptions(recipe, comboBoxInfo.volumeTypes));

			if (step != null)
			{
				String vol = comboBoxInfo.getMethod.apply(step);
				cb.getSelectionModel().select(vol);
			}
		}

		// update the unit controls
		getUnitControlUtils().refresh(step, IngredientAddition.Type.FERMENTABLES, step.getType());

		// hook for subclasses
		refreshInternal(step, recipe);

		// update the computed volumes
		for (ComputedVolumePane cvp : computedVolumePanes.keySet())
		{
			Function<T, String> getMethod = this.computedVolumePanes.get(cvp);
			cvp.refresh(getMethod.apply(step), recipe);
		}

		detectDirty = true;
		refreshing = false;
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshInternal(T step, Recipe recipe)
	{

	}

	/*-------------------------------------------------------------------------*/
	protected void addToolbar(ButtonType... buttonTypes)
	{
		ToolBar buttonBar = new ToolBar();
		buttonBar.setPadding(new Insets(3, 3, 6, 3));

		for (ButtonType buttonType : buttonTypes)
		{
			String textKey;
			Image icon;

			switch (buttonType)
			{
				case ADD_FERMENTABLE:
					if (processTemplateMode) continue;

					textKey = "common.add.fermentable";
					icon = JfxUi.addFermentable;
					break;
				case ADD_HOP:
					if (processTemplateMode) continue;

					textKey = "common.add.hop";
					icon = JfxUi.addHops;
					break;
				case ADD_WATER:
					if (processTemplateMode) continue;

					textKey = "common.add.water";
					icon = JfxUi.addWater;
					break;
				case ADD_YEAST:
					if (processTemplateMode) continue;

					textKey = "common.add.yeast";
					icon = JfxUi.addYeast;
					break;
				case ADD_MISC:
					if (processTemplateMode) continue;

					textKey = "common.add.misc";
					icon = JfxUi.addMisc;
					break;
				case DELETE:
					textKey = "editor.delete";
					icon = JfxUi.deleteIcon;
					break;
				case DUPLICATE:
					textKey = "common.duplicate";
					icon = JfxUi.duplicateIcon;
					break;
				default:
					throw new BrewdayException("invalid: " + buttonType);
			}

			Button button = new Button(null, JfxUi.getImageView(icon, JfxUi.ICON_SIZE));
			button.setTooltip(new Tooltip(StringUtils.getUiString(textKey)));

			switch (buttonType)
			{
				case ADD_FERMENTABLE:
					button.setOnAction(event -> ingredientAdditionDialog(new FermentableAdditionDialog(step, null)));
					break;
				case ADD_HOP:
					button.setOnAction(event -> ingredientAdditionDialog(new HopAdditionDialog(step, null)));
					break;
				case ADD_WATER:
					button.setOnAction(event -> ingredientAdditionDialog(new WaterAdditionDialog(step, null)));
					break;
				case ADD_YEAST:
					button.setOnAction(event -> ingredientAdditionDialog(new YeastAdditionDialog(step, null)));
					break;
				case ADD_MISC:
					button.setOnAction(event -> ingredientAdditionDialog(new MiscAdditionDialog(step, null)));
					break;
				case DUPLICATE:
					button.setOnAction(event -> duplicateDialog(step));
					break;
				case DELETE:
					button.setOnAction(event -> deleteDialog());
					break;
				default:
					throw new BrewdayException("invalid: " + buttonType);
			}

			buttonBar.getItems().add(button);
		}

		this.add(buttonBar, "dock north");
	}

	/*-------------------------------------------------------------------------*/
	private void deleteDialog()
	{
		Alert alert = new Alert(
			Alert.AlertType.NONE,
			StringUtils.getUiString("editor.delete.msg"),
			javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

		Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
		stage.getIcons().add(JfxUi.deleteIcon);
		alert.setTitle(StringUtils.getUiString("process.step.delete"));
		alert.setGraphic(JfxUi.getImageView(JfxUi.deleteIcon, 32));

		JfxUi.styleScene(stage.getScene());

		alert.showAndWait();

		if (alert.getResult() == javafx.scene.control.ButtonType.OK)
		{
			step.getRecipe().getSteps().remove(step);
			model.removeStep(step);
			parent.setDirty(step.getRecipe());
		}
	}

	/*-------------------------------------------------------------------------*/
	private void duplicateDialog(T step)
	{
		DuplicateDialog<ProcessStep> dialog = new DuplicateDialog<>(step)
		{
			@Override
			protected boolean isDuplicate(String newValue)
			{
				return false;
			}

			@Override
			protected ProcessStep createDuplicate(ProcessStep current, String newName)
			{
				ProcessStep result = current.clone();
				result.setName(newName);
				return result;
			}
		};

		dialog.showAndWait();

		ProcessStep output = dialog.getOutput();
		if (output != null)
		{
			this.step.getRecipe().getSteps().add(output);
			model.addStep(output);

			parent.setDirty(output);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void ingredientAdditionDialog(IngredientAdditionDialog<?, ?> dialog)
	{
		dialog.showAndWait();

		IngredientAddition ingredientAddition = dialog.getOutput();
		if (ingredientAddition != null)
		{
			step.addIngredientAddition(ingredientAddition);
			model.addIngredientAddition(step, ingredientAddition);

			parent.setDirty(ingredientAddition);
		}
	}

	/*-------------------------------------------------------------------------*/
	protected void addInputVolumeComboBox(
		String uiLabelKey,
		Function<T, String> getMethod,
		BiConsumer<T, String> setMethod,
		Volume.Type... types)
	{
		ComboBox<String> combo = new ComboBox<>();
		this.add(new Label(StringUtils.getUiString(uiLabelKey)));
		this.add(combo, "wrap");

		this.addComboBoxListener(setMethod, combo);

		this.inputVolumeCombos.put(combo, new InputVolumeComboBoxInfo(getMethod, setMethod, types));
	}

	/*-------------------------------------------------------------------------*/
	protected void addComputedVolumePane(String uiLabelKey, Function<T, String> getMethod)
	{
		ComputedVolumePane cvp = new ComputedVolumePane(StringUtils.getUiString(uiLabelKey));
		this.add(cvp, "span, wrap");

		this.computedVolumePanes.put(cvp, getMethod);
	}

	/*-------------------------------------------------------------------------*/
	protected ObservableList<String> getVolumesOptions(Recipe recipe, Volume.Type... types)
	{
		List<String> vec = new ArrayList<>(recipe.getAllVolumeNames());
		Collections.sort(vec);
		vec.add(0, UiUtils.NONE);
		return FXCollections.observableList(vec);
	}

	/*-------------------------------------------------------------------------*/
	protected void addComboBoxListener(
		BiConsumer<T, String> setMethod,
		ComboBox<String> comboBox)
	{
		comboBox.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			if (step != null && newValue != null)
			{
				if (!refreshing)
				{
					setMethod.accept(step, comboBox.getSelectionModel().getSelectedItem());
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(step);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	private class InputVolumeComboBoxInfo
	{
		private final Function<T, String> getMethod;
		private final BiConsumer<T, String> setMethod;
		private final Volume.Type[] volumeTypes;

		public InputVolumeComboBoxInfo(Function<T, String> getMethod,
			BiConsumer<T, String> setMethod,
			Volume.Type... volumeTypes)
		{
			this.getMethod = getMethod;
			this.setMethod = setMethod;
			this.volumeTypes = volumeTypes;
		}
	}

}
