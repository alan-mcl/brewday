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
import java.util.function.BiConsumer;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.*;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class IngredientAdditionPane<T extends IngredientAddition, V extends V2DataObject>
		extends MigPane
{
	protected boolean detectDirty = true, refreshing = false;

	private T addition;
	private ProcessStep step;
	private final TrackDirty parent;
	private final RecipeTreeViewModel model;

	// ingredients
	private final Map<ComboBox<String>, IngredientComboBoxInfo> ingredientCombos = new HashMap<>();

	// various unit controls
	private final UnitControlUtils<T> unitControlUtils;

	/*-------------------------------------------------------------------------*/
	public enum ButtonType
	{
		DUPLICATE,
		SUBSTITUTE,
		DELETE
	}

	/*-------------------------------------------------------------------------*/
	public IngredientAdditionPane(TrackDirty parent, RecipeTreeViewModel model)
	{
		this.parent = parent;

		this.unitControlUtils = new UnitControlUtils<>(parent);
		this.model = model;

		detectDirty = false;
		buildUiInternal();
		detectDirty = true;
	}

	/*-------------------------------------------------------------------------*/
	protected void buildUiInternal()
	{

	}

	/*-------------------------------------------------------------------------*/
	public void refresh(T addition, Recipe recipe)
	{
		detectDirty = false;
		refreshing = true;

		this.addition = addition;

		step = recipe.getStepOfAddition(addition);

		// update ingredient combos
		for (ComboBox<String> cb : ingredientCombos.keySet())
		{
			IngredientComboBoxInfo comboBoxInfo = ingredientCombos.get(cb);
			cb.setItems(getIngredientOptions(comboBoxInfo.volumeType));

			if (addition != null)
			{
				V vol = comboBoxInfo.getMethod.apply(addition);
				cb.getSelectionModel().select(vol.getName());
			}
		}

		// update the unit controls
		unitControlUtils.refresh(addition, addition.getType(), recipe.getStepOfAddition(addition).getType());

		// hook for subclasses
		refreshInternal(addition, recipe);

		detectDirty = true;
		refreshing = false;
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshInternal(T step, Recipe recipe)
	{

	}

	/*-------------------------------------------------------------------------*/
	public UnitControlUtils<T> getUnitControlUtils()
	{
		return unitControlUtils;
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
				case DELETE:
					textKey = "editor.delete";
					icon = Icons.deleteIcon;
					break;
				case DUPLICATE:
					textKey = "common.duplicate";
					icon = Icons.duplicateIcon;
					break;
				case SUBSTITUTE:
					textKey = "common.substitute";
					icon = Icons.substituteIcon;
					break;
				default:
					throw new BrewdayException("invalid: " + buttonType);
			}

			String text = StringUtils.getUiString(textKey);
			Button button = new Button(text, JfxUi.getImageView(icon, Icons.ICON_SIZE));
			button.setTooltip(new Tooltip(text));

			switch (buttonType)
			{
				case DUPLICATE:
					button.setOnAction(event -> duplicateDialog(addition));
					break;
				case DELETE:
					button.setOnAction(event -> deleteDialog(addition));
					break;
				case SUBSTITUTE:
					button.setOnAction(event ->
					{
						switch (addition.getType())
						{
							case FERMENTABLES:
								substitutionDialog(new FermentableAdditionDialog(step, (FermentableAddition)addition, true));
								break;
							case HOPS:
								substitutionDialog(new HopAdditionDialog(step, (HopAddition)addition, true));
								break;
							case WATER:
								substitutionDialog(new WaterAdditionDialog(step, (WaterAddition)addition, true));
								break;
							case YEAST:
								substitutionDialog(new YeastAdditionDialog(step, (YeastAddition)addition, true));
								break;
							case MISC:
								substitutionDialog(new MiscAdditionDialog(step, (MiscAddition)addition, true));
								break;
							default:
								throw new BrewdayException("invalid: " + buttonType);
						}

					});
					break;
				default:
					throw new BrewdayException("invalid: " + buttonType);
			}

			buttonBar.getItems().add(button);
		}

		this.add(buttonBar, "dock north");
	}

	/*-------------------------------------------------------------------------*/
	private void substitutionDialog(IngredientAdditionDialog<?, ?> dialog)
	{
		dialog.showAndWait();

		IngredientAddition substituteAddition = dialog.getOutput();
		if (substituteAddition != null)
		{
			step.removeIngredientAddition(this.addition);
			model.removeIngredientAddition(step, this.addition);

			step.addIngredientAddition(substituteAddition);
			model.addIngredientAddition(step, substituteAddition);

			parent.setDirty(substituteAddition);

			this.refresh((T)substituteAddition, step.getRecipe());
		}
	}

	/*-------------------------------------------------------------------------*/
	private void deleteDialog(T addition)
	{
		Alert alert = new Alert(
			Alert.AlertType.NONE,
			StringUtils.getUiString("editor.delete.msg"),
			javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

		Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Icons.deleteIcon);
		alert.setTitle(StringUtils.getUiString("process.step.delete.addition"));
		alert.setGraphic(JfxUi.getImageView(Icons.deleteIcon, 32));

		JfxUi.styleScene(stage.getScene());

		alert.showAndWait();

		if (alert.getResult() == javafx.scene.control.ButtonType.OK)
		{
			step.removeIngredientAddition(addition);
			model.removeIngredientAddition(step, addition);
			parent.setDirty(step.getRecipe());
		}
	}

	/*-------------------------------------------------------------------------*/
	private void duplicateDialog(T addition)
	{
		IngredientAddition newAddition = addition.clone();
		if (newAddition != null)
		{
			step.addIngredientAddition(newAddition);
			model.addIngredientAddition(step, newAddition);
			parent.setDirty(newAddition);
		}
	}

	/*-------------------------------------------------------------------------*/
	protected void addIngredientComboBox(
		String uiLabelKey,
		Function<T, V> getMethod,
		BiConsumer<T, V> setMethod,
		IngredientAddition.Type type)
	{
		ComboBox<String> combo = new ComboBox<>();
		this.add(new Label(StringUtils.getUiString(uiLabelKey)));
		this.add(combo, "wrap");

		this.addComboBoxListener(setMethod, combo);

		this.ingredientCombos.put(combo, new IngredientComboBoxInfo(getMethod, setMethod, type));
	}

	/*-------------------------------------------------------------------------*/
	protected ObservableList<String> getIngredientOptions(IngredientAddition.Type type)
	{
		List<String> vec = new ArrayList<>();

		switch (type)
		{
			case FERMENTABLES:
				vec.addAll(Database.getInstance().getFermentables().keySet());
				break;
			case HOPS:
				vec.addAll(Database.getInstance().getHops().keySet());
				break;
			case WATER:
				vec.addAll(Database.getInstance().getWaters().keySet());
				break;
			case YEAST:
				vec.addAll(Database.getInstance().getYeasts().keySet());
				break;
			case MISC:
				vec.addAll(Database.getInstance().getMiscs().keySet());
				break;
			default: throw new BrewdayException("invalid "+type);
		}

		Collections.sort(vec);
		return FXCollections.observableList(vec);
	}

	/*-------------------------------------------------------------------------*/
	public T getAddition()
	{
		return addition;
	}

	public ProcessStep getStep()
	{
		return step;
	}

	public TrackDirty getParentTrackDirty()
	{
		return parent;
	}

	/*-------------------------------------------------------------------------*/
	protected void addComboBoxListener(
		BiConsumer<T, V> setMethod,
		ComboBox<String> comboBox)
	{
		comboBox.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			if (addition != null && newValue != null)
			{
				if (!refreshing)
				{
					String selectedItem = comboBox.getSelectionModel().getSelectedItem();

					IngredientComboBoxInfo info = ingredientCombos.get(comboBox);

					V2DataObject ingredient;

					switch (info.volumeType)
					{
						case FERMENTABLES:
							ingredient = Database.getInstance().getFermentables().get(selectedItem);
							break;
						case HOPS:
							ingredient = Database.getInstance().getHops().get(selectedItem);
							break;
						case WATER:
							ingredient = Database.getInstance().getWaters().get(selectedItem);
							break;
						case YEAST:
							ingredient = Database.getInstance().getYeasts().get(selectedItem);
							break;
						case MISC:
							ingredient = Database.getInstance().getMiscs().get(selectedItem);
							break;
						default: throw new BrewdayException("invalid "+info.volumeType);
					}

					setMethod.accept(addition, (V)ingredient);
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(addition);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	protected void addQuantityListener(
		BiConsumer<T, Quantity> setMethod,
		TextField textField,
		Quantity.Unit unit)
	{
		textField.textProperty().addListener((observable, oldValue, newValue) ->
		{
			if (addition != null && newValue != null)
			{
				if (!refreshing)
				{
					setMethod.accept(addition, Quantity.parseQuantity(newValue, unit));
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(addition);
				}
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	private class IngredientComboBoxInfo
	{
		private Function<T, V> getMethod;
		private BiConsumer<T, V> setMethod;
		private IngredientAddition.Type volumeType;

		public IngredientComboBoxInfo(Function<T, V> getMethod,
									  BiConsumer<T, V> setMethod,
									  IngredientAddition.Type volumeType)
		{
			this.getMethod = getMethod;
			this.setMethod = setMethod;
			this.volumeType = volumeType;
		}
	}

	/*-------------------------------------------------------------------------*/
	private class QuantityInfo
	{
		private Function<T, Quantity> getMethod;
		private BiConsumer<T, Quantity> setMethod;
		private Quantity.Unit unit;

		public QuantityInfo(
			Function<T, Quantity> getMethod,
			BiConsumer<T, Quantity> setMethod,
			Quantity.Unit unit)
		{
			this.getMethod = getMethod;
			this.setMethod = setMethod;
			this.unit = unit;
		}
	}
}
