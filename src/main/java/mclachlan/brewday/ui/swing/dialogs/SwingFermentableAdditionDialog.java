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

package mclachlan.brewday.ui.swing.dialogs;

import java.awt.Window;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;
import mclachlan.brewday.ui.swing.widgets.SwingQuantitySelectAndEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code FermentableAdditionDialog}.
 */
public class SwingFermentableAdditionDialog extends SwingIngredientAdditionDialog<FermentableAddition, Fermentable>
{
	private SwingQuantitySelectAndEditWidget quantity;
	private SwingQuantityEditWidget<TimeUnit> time;

	public SwingFermentableAdditionDialog(Window parent, ProcessStep step, FermentableAddition addition, boolean captureTime)
	{
		super(parent, SwingIcons.IconKey.FERMENTABLE, "common.add.fermentable", step, captureTime);

		if (addition != null)
		{
			quantity.setUnitOptions(addition.getUnit(), Quantity.Type.WEIGHT, Quantity.Type.VOLUME);
			quantity.setQuantity(addition.getQuantity());
			if (captureTime && time != null)
			{
				time.setQuantity(addition.getTime());
			}
		}
	}

	@Override
	protected IngredientAddition.Type getIngredientType()
	{
		return IngredientAddition.Type.FERMENTABLES;
	}

	@Override
	protected int getTableColumnCount()
	{
		return 7;
	}

	@Override
	protected String getTableColumnKey(int column)
	{
		return switch (column)
		{
			case 0 -> "fermentable.name";
			case 1 -> "fermentable.type";
			case 2 -> "fermentable.origin";
			case 3 -> "fermentable.supplier";
			case 4 -> "fermentable.yield";
			case 5 -> "fermentable.colour";
			case 6 -> "ingredient.addition.amount.in.inventory";
			default -> "";
		};
	}

	@Override
	protected Object getTableCellValue(Fermentable row, int column)
	{
		return switch (column)
		{
			case 0 -> row.getName();
			case 1 -> row.getType();
			case 2 -> row.getOrigin();
			case 3 -> row.getSupplier();
			case 4 -> row.getYield() == null ? "" : row.getYield().describe(Quantity.Unit.PERCENTAGE_DISPLAY);
			case 5 -> row.getColour() == null ? "" : row.getColour().describe(Quantity.Unit.SRM);
			case 6 -> formatInventoryCell(row);
			default -> "";
		};
	}

	@Override
	protected boolean mandatoryInputProvided()
	{
		return quantity.getQuantity() != null && (!isCaptureTime() || (time != null && time.getQuantity() != null));
	}

	@Override
	protected void addUiStuffs(JPanel pane)
	{
		Settings settings = Database.getInstance().getSettings();
		Quantity.Unit quantityUnit = settings.getUnitForStepAndIngredient(Quantity.Type.WEIGHT, getStep(),
			IngredientAddition.Type.FERMENTABLES);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep(),
			IngredientAddition.Type.FERMENTABLES);

		quantity = new SwingQuantitySelectAndEditWidget(quantityUnit, Quantity.Type.WEIGHT, Quantity.Type.VOLUME);

		pane.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 0;
		pane.add(new JLabel(getUiString("recipe.amount")), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		pane.add(quantity, gbc);

		if (isCaptureTime())
		{
			time = new SwingQuantityEditWidget<>(timeUnit);
			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 0;
			pane.add(new JLabel(getUiString("recipe.time")), gbc);
			gbc.gridx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1.0;
			pane.add(time, gbc);
		}
	}

	@Override
	protected FermentableAddition createIngredientAddition(Fermentable selectedItem)
	{
		return new FermentableAddition(
			selectedItem,
			quantity.getQuantity(),
			quantity.getUnit(),
			isCaptureTime() && time != null ? time.getQuantity() : null);
	}

	@Override
	protected Map<String, Fermentable> getReferenceIngredients()
	{
		return Database.getInstance().getFermentables();
	}
}
