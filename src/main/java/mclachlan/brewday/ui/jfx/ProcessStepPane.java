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
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.BatchSparge;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
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
	private MigPane computedVolumePanesPanel;

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
	public enum ToolbarButtonType
	{
		ADD_FERMENTABLE, ADD_HOP, ADD_WATER, ADD_YEAST, ADD_MISC, DELETE, DUPLICATE
	}

	/*-------------------------------------------------------------------------*/
	public enum UtilityType
	{
		WATER_BUILDER, ACIDIFIER, MASH_TEMP_TARGET
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

		computedVolumePanesPanel = new MigPane();

		unitControlUtils = new UnitControlUtils<>(this.getParentTrackDirty());

		detectDirty = false;
		buildUiInternal();
		this.add(computedVolumePanesPanel, "span");
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
	protected void addToolbar(ToolbarButtonType... toolbarButtonTypes)
	{
		ToolBar buttonBar = new ToolBar();
		buttonBar.setPadding(new Insets(3, 3, 6, 3));

		for (ToolbarButtonType toolbarButtonType : toolbarButtonTypes)
		{
			String textKey;
			Image icon;

			switch (toolbarButtonType)
			{
				case ADD_FERMENTABLE:
					if (processTemplateMode) continue;

					textKey = "common.add.fermentable";
					icon = Icons.addFermentable;
					break;
				case ADD_HOP:
					if (processTemplateMode) continue;

					textKey = "common.add.hop";
					icon = Icons.addHops;
					break;
				case ADD_WATER:
					if (processTemplateMode) continue;

					textKey = "common.add.water";
					icon = Icons.addWater;
					break;
				case ADD_YEAST:
					if (processTemplateMode) continue;

					textKey = "common.add.yeast";
					icon = Icons.addYeast;
					break;
				case ADD_MISC:
					if (processTemplateMode) continue;

					textKey = "common.add.misc";
					icon = Icons.addMisc;
					break;
				case DELETE:
					textKey = "editor.delete";
					icon = Icons.deleteIcon;
					break;
				case DUPLICATE:
					textKey = "common.duplicate";
					icon = Icons.duplicateIcon;
					break;
				default:
					throw new BrewdayException("invalid: " + toolbarButtonType);
			}

			Button button = new Button(null, JfxUi.getImageView(icon, Icons.ICON_SIZE));
			button.setTooltip(new Tooltip(StringUtils.getUiString(textKey)));

			switch (toolbarButtonType)
			{
				case ADD_FERMENTABLE:
					button.setOnAction(event -> ingredientAdditionDialog(new FermentableAdditionDialog(step, null, true)));
					break;
				case ADD_HOP:
					button.setOnAction(event -> ingredientAdditionDialog(new HopAdditionDialog(step, null, true)));
					break;
				case ADD_WATER:
					button.setOnAction(event -> ingredientAdditionDialog(new WaterAdditionDialog(step, null, true)));
					break;
				case ADD_YEAST:
					button.setOnAction(event -> ingredientAdditionDialog(new YeastAdditionDialog(step, null, true)));
					break;
				case ADD_MISC:
					button.setOnAction(event -> ingredientAdditionDialog(new MiscAdditionDialog(step, null, true)));
					break;
				case DUPLICATE:
					button.setOnAction(event -> duplicateDialog(step));
					break;
				case DELETE:
					button.setOnAction(event -> deleteDialog());
					break;
				default:
					throw new BrewdayException("invalid: " + toolbarButtonType);
			}

			buttonBar.getItems().add(button);
		}

		this.add(buttonBar, "dock north");
	}

	/*-------------------------------------------------------------------------*/
	protected void addUtilityBar(UtilityType... utilityTypes)
	{
		ToolBar utils = new ToolBar();
		utils.setPadding(new Insets(3,3,3,3));

		for (UtilityType type : utilityTypes)
		{
			switch (type)
			{
				case WATER_BUILDER:
					Button waterBuilder = new Button(
						StringUtils.getUiString("tools.water.builder"),
						JfxUi.getImageView(Icons.waterBuilderIcon, Icons.ICON_SIZE));
					utils.getItems().add(waterBuilder);

					waterBuilder.setOnAction(actionEvent ->
					{
						WaterBuilderDialog wbd = new WaterBuilderDialog(getStep());
						wbd.showAndWait();

						if (wbd.getOutput())
						{
							List<MiscAddition> waterAdditions = wbd.getWaterAdditions();

							// remove all current water additions
							for (MiscAddition ma : getStep().getMiscAdditions())
							{
								if (ma.getMisc().getWaterAdditionFormula() != null &&
									ma.getMisc().getWaterAdditionFormula() != Misc.WaterAdditionFormula.LACTIC_ACID)
								{
									getStep().removeIngredientAddition(ma);
									getModel().removeIngredientAddition(getStep(), ma);
								}
							}

							// add these
							for (MiscAddition ma : waterAdditions)
							{
								getStep().addIngredientAddition(ma);
								getModel().addIngredientAddition(getStep(), ma);
								getParentTrackDirty().setDirty(ma);
							}
							getParentTrackDirty().setDirty(getStep());
						}
					});

					break;

				case ACIDIFIER:
					Button acidifier = new Button(
						StringUtils.getUiString("tools.acidifier"),
						JfxUi.getImageView(Icons.acidifierIcon, Icons.ICON_SIZE));
					utils.getItems().add(acidifier);

					acidifier.setOnAction(actionEvent ->
					{
						ProcessStep step = getStep();
						TimeUnit time;

						AcidifierDialog acd;
						if (step instanceof Mash)
						{
							time = ((Mash)step).getDuration();
							acd = new AcidifierDialog(
								((Mash)step).getMashPh(),
								step.getCombinedWaterProfile(((Mash)step).getDuration()),
								step.getFermentableAdditions());
							acd.showAndWait();
						}
						else if (step instanceof BatchSparge)
						{
							time = new TimeUnit(0);
							WaterAddition water = step.getCombinedWaterProfile(time);

							if (water == null)
							{
								return;
							}

							acd = new AcidifierDialog(
								water.getWater().getPh(),
								water,
								step.getFermentableAdditions());
							acd.showAndWait();
						}
						else
						{
							throw new BrewdayException("invalid step type: "+step);
						}

						if (acd.getOutput())
						{
							List<MiscAddition> acidAdditions = acd.getAcidAdditions();

							// do not remove all current acids, because the current ph already
							// accounts for them

							// add these
							for (MiscAddition ma : acidAdditions)
							{
								ma.setTime(time);
								getStep().addIngredientAddition(ma);
								getModel().addIngredientAddition(getStep(), ma);
								getParentTrackDirty().setDirty(ma);
							}
							getParentTrackDirty().setDirty(getStep());
						}
					});


					break;

				case MASH_TEMP_TARGET:
					Button mashTempTarget = new Button(
						StringUtils.getUiString("tools.mash.temp"),
						JfxUi.getImageView(Icons.temperatureIcon, Icons.ICON_SIZE));
					utils.getItems().add(mashTempTarget);

					mashTempTarget.setOnAction(actionEvent ->
					{
						ProcessStep step = getStep();

						TargetMashTempDialog dialog;
						if (step instanceof Mash)
						{
							dialog = new TargetMashTempDialog(
								((Mash)step).getMashPh(),
								step.getCombinedWaterProfile(((Mash)step).getDuration()),
								step.getFermentableAdditions(),
								((Mash)step).getGrainTemp());
							dialog.showAndWait();
						}
						else
						{
							throw new BrewdayException("invalid step type: "+step);
						}

						if (dialog.getOutput())
						{
							TemperatureUnit temp = dialog.getTemp();

							// set water temps
							for (WaterAddition wa : step.getWaterAdditions())
							{
								wa.setTemperature(temp);

								getParentTrackDirty().setDirty(wa);
							}
							getParentTrackDirty().setDirty(getStep());
						}
					});


					break;
				default:
					throw new BrewdayException("Unexpected value: " + type);
			}
		}


		this.add(utils, "span, wrap");
	}

	/*-------------------------------------------------------------------------*/
	private void deleteDialog()
	{
		Alert alert = new Alert(
			Alert.AlertType.NONE,
			StringUtils.getUiString("editor.delete.msg"),
			javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

		Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Icons.deleteIcon);
		alert.setTitle(StringUtils.getUiString("process.step.delete"));
		alert.setGraphic(JfxUi.getImageView(Icons.deleteIcon, 32));

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

		this.computedVolumePanes.put(cvp, getMethod);

		if (this.computedVolumePanes.size() % 2 == 0)
		{
			computedVolumePanesPanel.add(cvp, "wrap");
		}
		else
		{
			computedVolumePanesPanel.add(cvp);
		}
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

	protected RecipeTreeViewModel getModel()
	{
		return model;
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
