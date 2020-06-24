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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class IngredientAdditionPane<T extends IngredientAddition, V extends V2DataObject>
		extends MigPane
{
	protected boolean detectDirty = true, refreshing = false;

	private T addition;
	private final TrackDirty parent;

	// ingredients
	private final Map<ComboBox<String>, IngredientComboBoxInfo> ingredientCombos = new HashMap<>();

	// various unit controls
	private final ControlUtils<T> controlUtils;

	/*-------------------------------------------------------------------------*/
	public IngredientAdditionPane(TrackDirty parent)
	{
		this.parent = parent;

		this.controlUtils = new ControlUtils<>(parent);

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
		controlUtils.refresh(addition, recipe);

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
	public ControlUtils<T> getControlUtils()
	{
		return controlUtils;
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
