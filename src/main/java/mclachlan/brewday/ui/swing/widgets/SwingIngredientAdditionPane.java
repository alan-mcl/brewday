/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.recipe.YeastAddition;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.dialogs.SwingFermentableAdditionDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingHopAdditionDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingMiscAdditionDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingWaterAdditionDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingYeastAdditionDialog;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code IngredientAdditionPane}.
 *
 * @param <T> concrete {@link IngredientAddition} type
 * @param <V> ingredient {@link V2DataObject} type (Hop, Water, ...)
 */
public abstract class SwingIngredientAdditionPane<T extends IngredientAddition, V extends V2DataObject> extends JPanel
{
	public enum ButtonType
	{
		DUPLICATE,
		SUBSTITUTE,
		DELETE
	}

	protected final DirtyStateService dirtyState;
	protected final SwingRecipeTree recipeTree;

	private T addition;
	private Recipe recipe;
	private ProcessStep step;
	private boolean refreshing;
	private boolean detectDirty;

	private final JToolBar additionToolbar;
	private final JPanel form;
	private final SwingUnitControlUtils<T> unitControlUtils;

	private final List<IngredientLabelRow<T, V>> labelRows = new ArrayList<>();
	private int formRow;

	public SwingIngredientAdditionPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree)
	{
		super(new BorderLayout(8, 8));
		this.dirtyState = dirtyState;
		this.recipeTree = recipeTree;
		setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

		additionToolbar = new JToolBar();
		additionToolbar.setFloatable(false);
		add(additionToolbar, BorderLayout.NORTH);

		form = new JPanel(new GridBagLayout());
		JPanel centerHost = new JPanel(new BorderLayout());
		centerHost.add(form, BorderLayout.NORTH);
		add(centerHost, BorderLayout.CENTER);

		unitControlUtils = new SwingUnitControlUtils<>(dirtyState,
			() -> detectDirty && !refreshing && addition != null);

		detectDirty = false;
		buildUiInternal();
		detectDirty = true;
	}

	protected abstract void buildUiInternal();

	protected void refreshInternal(T addition, Recipe recipe)
	{
	}

	protected final void addToolbar(ButtonType... buttonTypes)
	{
		for (ButtonType bt : buttonTypes)
		{
			switch (bt)
			{
				case DUPLICATE ->
				{
					JButton b = new JButton(SwingIcons.toolbarIcon(SwingIcons.IconKey.DUPLICATE));
					b.setToolTipText(getUiString("tooltip.ingredient.duplicate"));
					b.addActionListener(e -> duplicateAddition());
					additionToolbar.add(b);
				}
				case SUBSTITUTE ->
				{
					JButton b = new JButton(SwingIcons.toolbarIcon(SwingIcons.IconKey.SUBSTITUTE));
					b.setToolTipText(getUiString("tooltip.ingredient.substitute"));
					b.addActionListener(e -> substituteAddition());
					additionToolbar.add(b);
				}
				case DELETE ->
				{
					JButton b = new JButton(SwingIcons.toolbarIcon(SwingIcons.IconKey.DELETE));
					b.setToolTipText(getUiString("tooltip.ingredient.delete"));
					b.addActionListener(e -> deleteAddition());
					additionToolbar.add(b);
				}
				default ->
				{
				}
			}
		}
	}

	protected final void addIngredientLabel(String labelKey,
		Function<T, V> getIngredient, Function<V, Object> propertyMethod)
	{
		form.add(new JLabel(getUiString(labelKey) + ":"), labelGbc());
		JLabel value = new JLabel();
		form.add(value, widgetGbc());
		advanceFormRow();
		labelRows.add(new IngredientLabelRow<>(value, getIngredient, propertyMethod));
	}

	protected final void addQuantitySelectAndEditControl(String labelKey,
		Function<T, Quantity> qGet, BiConsumer<T, Quantity> qSet,
		Function<T, Quantity.Unit> unitGet, BiConsumer<T, Quantity.Unit> unitSet,
		Quantity.Unit defaultUnitWhenNull, Quantity.Type... allowedTypes)
	{
		addQuantitySelectAndEditControl(labelKey, qGet, qSet, unitGet, unitSet, defaultUnitWhenNull, null,
			allowedTypes);
	}

	/**
	 * Merges {@code extraMeasurementType} into selectable units on refresh (misc additions).
	 */
	protected final void addQuantitySelectAndEditControl(String labelKey,
		Function<T, Quantity> qGet, BiConsumer<T, Quantity> qSet,
		Function<T, Quantity.Unit> unitGet, BiConsumer<T, Quantity.Unit> unitSet,
		Quantity.Unit defaultUnitWhenNull,
		Function<T, Quantity.Type> extraMeasurementType,
		Quantity.Type... allowedTypes)
	{
		form.add(new JLabel(getUiString(labelKey) + ":"), labelGbc());
		SwingQuantitySelectAndEditWidget w = new SwingQuantitySelectAndEditWidget(defaultUnitWhenNull, allowedTypes);
		form.add(w, widgetGbc());
		advanceFormRow();
		unitControlUtils.registerQuantitySelect(w, qGet, qSet, unitGet, unitSet, extraMeasurementType,
			allowedTypes);
	}

	protected final void addTimeUnitControl(String labelKey,
		Function<T, TimeUnit> get, BiConsumer<T, TimeUnit> set, Quantity.Unit unit)
	{
		form.add(new JLabel(getUiString(labelKey) + ":"), labelGbc());
		SwingQuantityEditWidget<TimeUnit> w = new SwingQuantityEditWidget<>(unit);
		form.add(w, widgetGbc());
		advanceFormRow();
		unitControlUtils.registerTimeUnit(w, get, set, unit);
	}

	protected final void addTemperatureUnitControl(String labelKey,
		Function<T, TemperatureUnit> get, BiConsumer<T, TemperatureUnit> set, Quantity.Unit unit)
	{
		form.add(new JLabel(getUiString(labelKey) + ":"), labelGbc());
		SwingQuantityEditWidget<TemperatureUnit> w = new SwingQuantityEditWidget<>(unit);
		form.add(w, widgetGbc());
		advanceFormRow();
		unitControlUtils.registerTemperatureUnit(w, get, set, unit);
	}

	@SuppressWarnings("unchecked")
	public void refresh(IngredientAddition untyped, Recipe recipe)
	{
		T typed = (T)untyped;
		this.addition = typed;
		this.recipe = recipe;
		this.step = recipe != null && typed != null ? recipe.getStepOfAddition(typed) : null;
		detectDirty = false;
		refreshing = true;

		for (IngredientLabelRow<T, V> row : labelRows)
		{
			if (typed != null)
			{
				V ing = row.getter().apply(typed);
				Object v = ing != null ? row.property().apply(ing) : null;
				row.label().setText(v != null ? String.valueOf(v) : "");
			}
			else
			{
				row.label().setText("");
			}
		}

		unitControlUtils.refresh(typed);
		refreshInternal(typed, recipe);

		refreshing = false;
		detectDirty = true;
	}

	private GridBagConstraints labelGbc()
	{
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = formRow;
		g.insets = new Insets(3, 4, 3, 4);
		g.anchor = GridBagConstraints.WEST;
		g.fill = GridBagConstraints.NONE;
		g.weightx = 0;
		g.weighty = 0;
		return g;
	}

	private GridBagConstraints widgetGbc()
	{
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 1;
		g.gridy = formRow;
		g.insets = new Insets(3, 4, 3, 4);
		g.anchor = GridBagConstraints.WEST;
		g.fill = GridBagConstraints.HORIZONTAL;
		g.weightx = 1.0;
		g.weighty = 0;
		return g;
	}

	private void advanceFormRow()
	{
		formRow++;
	}

	private Window parentWindow()
	{
		return SwingUtilities.getWindowAncestor(this);
	}

	private void substituteAddition()
	{
		if (addition == null || step == null || recipe == null)
		{
			return;
		}
		IngredientAddition replacement = switch (addition.getType())
		{
			case HOPS ->
			{
				SwingHopAdditionDialog d = new SwingHopAdditionDialog(parentWindow(), step, (HopAddition)addition, true);
				d.setVisible(true);
				yield d.getOutput();
			}
			case WATER ->
			{
				SwingWaterAdditionDialog d = new SwingWaterAdditionDialog(parentWindow(), step, (WaterAddition)addition, true);
				d.setVisible(true);
				yield d.getOutput();
			}
			case FERMENTABLES ->
			{
				SwingFermentableAdditionDialog d = new SwingFermentableAdditionDialog(parentWindow(), step, (FermentableAddition)addition, true);
				d.setVisible(true);
				yield d.getOutput();
			}
			case YEAST ->
			{
				SwingYeastAdditionDialog d = new SwingYeastAdditionDialog(parentWindow(), step, (YeastAddition)addition, true);
				d.setVisible(true);
				yield d.getOutput();
			}
			case MISC ->
			{
				SwingMiscAdditionDialog d = new SwingMiscAdditionDialog(parentWindow(), step, (MiscAddition)addition, true);
				d.setVisible(true);
				yield d.getOutput();
			}
			default -> null;
		};
		if (replacement != null)
		{
			step.removeIngredientAddition(addition);
			recipeTree.removeAddition(step, addition);
			step.addIngredientAddition(replacement);
			recipeTree.addAddition(step, replacement);
			dirtyState.markDirty(replacement);
			recipeTree.selectUserObject(replacement);
		}
	}

	private void duplicateAddition()
	{
		if (addition == null || step == null)
		{
			return;
		}
		IngredientAddition copy = addition.clone();
		if (copy != null)
		{
			step.addIngredientAddition(copy);
			recipeTree.addAddition(step, copy);
			dirtyState.markDirty(copy);
			recipeTree.selectUserObject(copy);
		}
	}

	private void deleteAddition()
	{
		if (addition == null || step == null || recipe == null)
		{
			return;
		}
		int r = JOptionPane.showConfirmDialog(this,
			getUiString("editor.delete.msg"),
			getUiString("process.step.delete.addition"),
			JOptionPane.OK_CANCEL_OPTION);
		if (r != JOptionPane.OK_OPTION)
		{
			return;
		}
		step.removeIngredientAddition(addition);
		recipeTree.removeAddition(step, addition);
		dirtyState.removeDirty(addition);
		dirtyState.markDirty(recipe);
		recipeTree.selectRoot();
	}

	private record IngredientLabelRow<T extends IngredientAddition, V>(
		JLabel label,
		Function<T, V> getter,
		Function<V, Object> property)
	{
	}

	JPanel getFormForTest()
	{
		return form;
	}
}
