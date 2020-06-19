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
import java.util.function.BiConsumer;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.EditorPanel;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class ProcessStepPane<T extends ProcessStep> extends MigPane
{
	protected boolean detectDirty = true, refreshing = false;

	private T step;
	private final TrackDirty parent;

	private final TextField name;
	private final TextArea desc;

	// input volumes
	private Map<ComboBox, InputVolumeComboBoxInfo> inputVolumeCombos = new HashMap<>();

	// various unit controls
	private Map<TextField, TimeUnitInfo> timeUnitControls = new HashMap<>();
	private Map<TextField, TempUnitInfo> tempUnitControls = new HashMap<>();
	private Map<TextField, VolUnitInfo> volumeUnitControls = new HashMap<>();

	// computed volume panes
	private Map<ComputedVolumePane, Function<T, String>> computedVolumePanes = new HashMap<>();

	/*-------------------------------------------------------------------------*/
	public enum ButtonType
	{
		ADD_FERMENTABLE, ADD_HOP, ADD_WATER, ADD_YEAST, ADD_MISC, DELETE, DUPLICATE, SUBSTITUTE
	}

	/*-------------------------------------------------------------------------*/
	public ProcessStepPane(TrackDirty parent)
	{
		this.parent = parent;

		name = new TextField();
		desc = new TextArea();

		detectDirty = false;
		buildUiInternal();
		detectDirty = true;
	}

	/*-------------------------------------------------------------------------*/
	protected void buildUiInternal()
	{

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
		for (TextField tf : timeUnitControls.keySet())
		{
			if (step != null)
			{
				TimeUnitInfo info = timeUnitControls.get(tf);
				tf.setText(""+info.getMethod.apply(step).get(info.unit));
			}
		}
		for (TextField tf : tempUnitControls.keySet())
		{
			if (step != null)
			{
				TempUnitInfo info = tempUnitControls.get(tf);
				tf.setText(""+info.getMethod.apply(step).get(info.unit));
			}
		}
		for (TextField tf : volumeUnitControls.keySet())
		{
			if (step != null)
			{
				VolUnitInfo info = volumeUnitControls.get(tf);
				tf.setText(""+info.getMethod.apply(step).get(info.unit));
			}
		}

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
					textKey = "common.add.fermentable";
					icon = JfxUi.addFermentable;
					break;
				case ADD_HOP:
					textKey = "common.add.hop";
					icon = JfxUi.addHops;
					break;
				case ADD_WATER:
					textKey = "common.add.water";
					icon = JfxUi.addWater;
					break;
				case ADD_YEAST:
					textKey = "common.add.yeast";
					icon = JfxUi.addYeast;
					break;
				case ADD_MISC:
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
				case SUBSTITUTE:
					textKey = "common.substitute";
					icon = JfxUi.substituteIcon;
					break;
				default: throw new BrewdayException("invalid: "+buttonType);
			}

			Button button = new Button(null, JfxUi.getImageView(icon, RecipesPane3.ICON_SIZE));
			button.setTooltip(new Tooltip(StringUtils.getUiString(textKey)));

			switch (buttonType)
			{
				case ADD_FERMENTABLE:
					button.setOnAction(event ->
					{
						IngredientAdditionDialog<FermentableAddition, Fermentable> dialog = new
							IngredientAdditionDialog<>(JfxUi.grainsIcon, "common.add.fermentable");

						dialog.showAndWait();

						FermentableAddition output = dialog.getOutput();

						if (output != null)
						{
							step.addIngredientAddition(output);
						}
					});
					break;
				case ADD_HOP:
					break;
				case ADD_WATER:
					break;
				case ADD_YEAST:
					break;
				case ADD_MISC:
					break;
				case DELETE:
					break;
				case DUPLICATE:
					break;
				case SUBSTITUTE:
					break;
				default:
					throw new BrewdayException("invalid: "+buttonType);
			}

			buttonBar.getItems().add(button);
		}

		this.add(buttonBar, "dock north");
	}

	/*-------------------------------------------------------------------------*/
	protected void addInputVolumeComboBox(
		String uiLabelKey,
		Function<T, String> getMethod,
		BiConsumer<T,String> setMethod,
		Volume.Type... types)
	{
		ComboBox<String> combo = new ComboBox<>();
		this.add(new Label(StringUtils.getUiString(uiLabelKey)));
		this.add(combo, "wrap");

		this.addComboBoxListener(setMethod, combo);

		this.inputVolumeCombos.put(combo, new InputVolumeComboBoxInfo(getMethod, setMethod, types));
	}

	/*-------------------------------------------------------------------------*/
	protected void addTimeUnitControl(
		String uiLabelKey,
		Function<T, TimeUnit> getMethod,
		BiConsumer<T, TimeUnit> setMethod,
		Quantity.Unit unit)
	{
		TextField tf = new TextField();
		this.add(new Label(StringUtils.getUiString(uiLabelKey)));
		this.add(tf, "wrap");

		this.timeUnitControls.put(tf, new TimeUnitInfo(getMethod, setMethod, unit));

		this.addTimeUnitListener(setMethod, tf, unit);
	}

	/*-------------------------------------------------------------------------*/
	protected void addTemperatureUnitControl(
		String uiLabelKey,
		Function<T, TemperatureUnit> getMethod,
		BiConsumer<T, TemperatureUnit> setMethod,
		Quantity.Unit unit)
	{
		TextField tf = new TextField();
		this.add(new Label(StringUtils.getUiString(uiLabelKey)));
		this.add(tf, "wrap");

		this.tempUnitControls.put(tf, new TempUnitInfo(getMethod, setMethod, unit));

		this.addTemperatureUnitListener(setMethod, tf, unit);
	}

	/*-------------------------------------------------------------------------*/
	protected void addVolumeUnitControl(
		String uiLabelKey,
		Function<T, VolumeUnit> getMethod,
		BiConsumer<T, VolumeUnit> setMethod,
		Quantity.Unit unit)
	{
		TextField tf = new TextField();
		this.add(new Label(StringUtils.getUiString(uiLabelKey)));
		this.add(tf, "wrap");

		this.volumeUnitControls.put(tf, new VolUnitInfo(getMethod, setMethod, unit));

		this.addVolumeUnitListener(setMethod, tf, unit);
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
		List<String> vec = new ArrayList<>(recipe.getVolumes().getVolumes(types));
		Collections.sort(vec);
		vec.add(0, EditorPanel.NONE);
		return FXCollections.observableList(vec);
	}

	/*-------------------------------------------------------------------------*/
	public String getDescription()
	{
		return desc.getText();
	}

	public T getStep()
	{
		return step;
	}

	public TrackDirty getParentTrackDirty()
	{
		return parent;
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
	protected void addTemperatureUnitListener(
		BiConsumer<T, TemperatureUnit> setMethod,
		TextField textField,
		Quantity.Unit unit)
	{
		textField.textProperty().addListener((observable, oldValue, newValue) ->
		{
			if (step != null && newValue != null)
			{
				if (!refreshing)
				{
					setMethod.accept(step, new TemperatureUnit(new Double(newValue), unit, false));
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(step);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	protected void addVolumeUnitListener(
		BiConsumer<T, VolumeUnit> setMethod,
		TextField textField,
		Quantity.Unit unit)
	{
		textField.textProperty().addListener((observable, oldValue, newValue) ->
		{
			if (step != null && newValue != null)
			{
				if (!refreshing)
				{
					setMethod.accept(step, new VolumeUnit(new Double(newValue), unit, false));
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(step);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	protected void addTimeUnitListener(
		BiConsumer<T, TimeUnit> setMethod,
		TextField textField,
		Quantity.Unit unit)
	{
		textField.textProperty().addListener((observable, oldValue, newValue) ->
		{
			if (step != null && newValue != null)
			{
				if (!refreshing)
				{
					setMethod.accept(step, new TimeUnit(new Double(newValue), unit, false));
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
		private Function<T, String> getMethod;
		private BiConsumer<T, String> setMethod;
		private Volume.Type[] volumeTypes;

		public InputVolumeComboBoxInfo(Function<T, String> getMethod,
			BiConsumer<T, String> setMethod,
			Volume.Type... volumeTypes)
		{
			this.getMethod = getMethod;
			this.setMethod = setMethod;
			this.volumeTypes = volumeTypes;
		}
	}

	/*-------------------------------------------------------------------------*/
	private class TimeUnitInfo
	{
		private Function<T, TimeUnit> getMethod;
		private BiConsumer<T, TimeUnit> setMethod;
		private Quantity.Unit unit;

		public TimeUnitInfo(Function<T, TimeUnit> getMethod,
			BiConsumer<T, TimeUnit> setMethod,
			Quantity.Unit unit)
		{
			this.getMethod = getMethod;
			this.setMethod = setMethod;
			this.unit = unit;
		}
	}

	/*-------------------------------------------------------------------------*/
	private class TempUnitInfo
	{
		private Function<T, TemperatureUnit> getMethod;
		private BiConsumer<T, TemperatureUnit> setMethod;
		private Quantity.Unit unit;

		public TempUnitInfo(
			Function<T, TemperatureUnit> getMethod,
			BiConsumer<T, TemperatureUnit> setMethod,
			Quantity.Unit unit)
		{
			this.getMethod = getMethod;
			this.setMethod = setMethod;
			this.unit = unit;
		}
	}

	/*-------------------------------------------------------------------------*/
	private class VolUnitInfo
	{
		private Function<T, VolumeUnit> getMethod;
		private BiConsumer<T, VolumeUnit> setMethod;
		private Quantity.Unit unit;

		public VolUnitInfo(
			Function<T, VolumeUnit> getMethod,
			BiConsumer<T, VolumeUnit> setMethod,
			Quantity.Unit unit)
		{
			this.getMethod = getMethod;
			this.setMethod = setMethod;
			this.unit = unit;
		}
	}
}
