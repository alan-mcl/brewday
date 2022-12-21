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
import mclachlan.brewday.util.StringUtils;
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
	private final RecipeTreeView recipeTreeView;
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
		ADD_FERMENTABLE, ADD_HOP, ADD_WATER, ADD_YEAST, ADD_MISC, DELETE, DUPLICATE, RENAME_STEP
	}

	/*-------------------------------------------------------------------------*/
	public enum UtilityType
	{
		WATER_BUILDER, ACIDIFIER, MASH_TEMP_TARGET
	}

	/*-------------------------------------------------------------------------*/
	public ProcessStepPane(TrackDirty parent, RecipeTreeView recipeTreeView,
		boolean processTemplateMode)
	{
		this.parent = parent;
		this.recipeTreeView = recipeTreeView;
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
				if (vol == null)
				{
					cb.getSelectionModel().select(UiUtils.NONE);
				}
				else
				{
					cb.getSelectionModel().select(vol);
				}
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
	protected void addToolbar(
		List<IngredientAddition.Type> supportedIngredientAdditions,
		ToolbarButtonType... toolbarButtonTypes)
	{
		ArrayList<ToolbarButtonType> buttonTypes = new ArrayList<>();

		for (IngredientAddition.Type ingType : supportedIngredientAdditions)
		{
			buttonTypes.add(switch (ingType)
			{
				case FERMENTABLES -> ToolbarButtonType.ADD_FERMENTABLE;
				case HOPS -> ToolbarButtonType.ADD_HOP;
				case WATER -> ToolbarButtonType.ADD_WATER;
				case YEAST -> ToolbarButtonType.ADD_YEAST;
				case MISC -> ToolbarButtonType.ADD_MISC;
				default -> throw new BrewdayException("invalid: "+ingType);
			});
		}

		buttonTypes.addAll(Arrays.asList(toolbarButtonTypes));

		ToolBar buttonBar = new ToolBar();
		buttonBar.setPadding(new Insets(3, 3, 6, 3));

		for (ToolbarButtonType toolbarButtonType : buttonTypes)
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
				case RENAME_STEP:
					textKey = "editor.rename";
					icon = Icons.renameIcon;
					break;
				default:
					throw new BrewdayException("invalid: " + toolbarButtonType);
			}

			Button button = new Button(null, JfxUi.getImageView(icon, Icons.ICON_SIZE));
			button.setTooltip(new Tooltip(StringUtils.getUiString(textKey)));

			switch (toolbarButtonType)
			{
				case ADD_FERMENTABLE ->
					button.setOnAction(event -> ingredientAdditionDialog(new FermentableAdditionDialog(step, null, true)));
				case ADD_HOP ->
					button.setOnAction(event -> ingredientAdditionDialog(new HopAdditionDialog(step, null, true)));
				case ADD_WATER ->
					button.setOnAction(event -> ingredientAdditionDialog(new WaterAdditionDialog(step, null, true)));
				case ADD_YEAST ->
					button.setOnAction(event -> ingredientAdditionDialog(new YeastAdditionDialog(step, null, true)));
				case ADD_MISC ->
					button.setOnAction(event -> ingredientAdditionDialog(new MiscAdditionDialog(step, null, true)));
				case DUPLICATE ->
					button.setOnAction(event -> duplicateDialog(step));
				case RENAME_STEP ->
					button.setOnAction(event -> renameStepDialog(step));
				case DELETE -> button.setOnAction(event -> deleteDialog());
				default ->
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
				case WATER_BUILDER ->
				{
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
									ma.getMisc().isAcidAddition())
								{
									getStep().removeIngredientAddition(ma);
									getRecipeTreeView().removeIngredientAddition(getStep(), ma);
								}
							}

							// add these
							for (MiscAddition ma : waterAdditions)
							{
								getStep().addIngredientAddition(ma);
								getRecipeTreeView().addIngredientAddition(getStep(), ma);
								getParentTrackDirty().setDirty(ma);
							}
							getParentTrackDirty().setDirty(getStep());
						}
					});
				}
				case ACIDIFIER ->
				{
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
								step.getFermentableAdditions(), step.getMiscAdditions());
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
								step.getFermentableAdditions(), step.getMiscAdditions());
							acd.showAndWait();
						}
						else
						{
							throw new BrewdayException("invalid step type: " + step);
						}

						if (acd.getOutput())
						{
							List<MiscAddition> acidAdditions = acd.getAcidAdditions();

							// do not remove all current acids

							// add these
							for (MiscAddition ma : acidAdditions)
							{
								ma.setTime(time);
								getStep().addIngredientAddition(ma);
								getRecipeTreeView().addIngredientAddition(getStep(), ma);
								getParentTrackDirty().setDirty(ma);
							}
							getParentTrackDirty().setDirty(getStep());
						}
					});
				}
				case MASH_TEMP_TARGET ->
				{
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
							throw new BrewdayException("invalid step type: " + step);
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
				}
				default -> throw new BrewdayException("Unexpected value: " + type);
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
			recipeTreeView.removeStep(step);
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
				return step.getRecipe().containsStepWithName(newValue);
			}

			@Override
			protected ProcessStep createDuplicate(ProcessStep current, String newName)
			{
				ProcessStep result = current.clone(newName);
				result.setName(newName);
				return result;
			}
		};

		dialog.showAndWait();

		ProcessStep output = dialog.getOutput();
		if (output != null)
		{
			this.step.getRecipe().getSteps().add(output);
			recipeTreeView.addStep(output);

			parent.setDirty(output);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void renameStepDialog(T step)
	{
		RenameItemDialog<ProcessStep> dialog = new RenameItemDialog<>(
			step, "process.step")
		{
			@Override
			protected Map<String, ProcessStep> getMap()
			{
				return new HashMap<>();
			}
		};

		dialog.showAndWait();

		String result = dialog.getOutput();

		if (result != null)
		{
			step.setName(result);

			parent.setDirty(step);
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
			recipeTreeView.addIngredientAddition(step, ingredientAddition);

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
					String selectedItem = comboBox.getSelectionModel().getSelectedItem();

					if (UiUtils.NONE.equals(selectedItem))
					{
						setMethod.accept(step, null);
					}
					else
					{
						setMethod.accept(step, selectedItem);
					}
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(step);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/

	protected RecipeTreeView getRecipeTreeView()
	{
		return recipeTreeView;
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
