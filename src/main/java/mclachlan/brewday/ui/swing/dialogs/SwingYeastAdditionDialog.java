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

package mclachlan.brewday.ui.swing.dialogs;

import java.awt.Window;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.YeastAddition;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;
import mclachlan.brewday.ui.swing.widgets.SwingQuantitySelectAndEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code YeastAdditionDialog}.
 */
public class SwingYeastAdditionDialog extends SwingIngredientAdditionDialog<YeastAddition, Yeast>
{
	private SwingQuantitySelectAndEditWidget quantity;
	private SwingQuantityEditWidget<TimeUnit> time;
	private Yeast lastYeastForQuantityReset;

	public SwingYeastAdditionDialog(Window parent, ProcessStep step, YeastAddition addition, boolean captureTime)
	{
		super(parent, SwingIcons.IconKey.YEAST, "common.add.yeast", step, captureTime);

		getIngredientTable().getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
				{
					return;
				}
				Yeast y = getSelectedReferenceIngredient();
				if (y == null)
				{
					return;
				}
				if (lastYeastForQuantityReset == null || y.getForm() != lastYeastForQuantityReset.getForm())
				{
					lastYeastForQuantityReset = y;
					quantity.setUnitOptions(y.getForm().getDefaultUnit(), Quantity.Type.WEIGHT, Quantity.Type.VOLUME);
					quantity.setQuantity(Quantity.parseQuantity("0", y.getForm().getDefaultUnit()));
				}
			}
		});

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
		return IngredientAddition.Type.YEAST;
	}

	@Override
	protected int getTableColumnCount()
	{
		return 8;
	}

	@Override
	protected String getTableColumnKey(int column)
	{
		return switch (column)
		{
			case 0 -> "yeast.name";
			case 1 -> "yeast.laboratory";
			case 2 -> "yeast.product.id";
			case 3 -> "yeast.type";
			case 4 -> "yeast.form";
			case 5 -> "yeast.attenuation";
			case 6 -> "yeast.flocculation";
			case 7 -> "ingredient.addition.amount.in.inventory";
			default -> "";
		};
	}

	@Override
	protected Object getTableCellValue(Yeast row, int column)
	{
		return switch (column)
		{
			case 0 -> row.getName();
			case 1 -> row.getLaboratory();
			case 2 -> row.getProductId();
			case 3 -> row.getType();
			case 4 -> row.getForm();
			case 5 -> row.getAttenuation() == null ? "" : row.getAttenuation().describe(Quantity.Unit.PERCENTAGE_DISPLAY);
			case 6 -> row.getFlocculation();
			case 7 -> formatInventoryCell(row);
			default -> "";
		};
	}

	@Override
	protected boolean getFilterPredicate(String searchText, Yeast yeast)
	{
		String s = searchText.toLowerCase();
		return yeast.getName().toLowerCase().contains(s) ||
			(yeast.getLaboratory() != null && yeast.getLaboratory().toLowerCase().contains(s)) ||
			(yeast.getProductId() != null && yeast.getProductId().toLowerCase().contains(s)) ||
			(yeast.getRecommendedStyles() != null && yeast.getRecommendedStyles().toLowerCase().contains(s)) ||
			(yeast.getDescription() != null && yeast.getDescription().toLowerCase().contains(s));
	}

	@Override
	protected void addUiStuffs(JPanel pane)
	{
		Settings settings = Database.getInstance().getSettings();
		IngredientAddition.Type ingType = IngredientAddition.Type.YEAST;
		Quantity.Unit quantityUnit = settings.getUnitForStepAndIngredient(Quantity.Type.WEIGHT, getStep(), ingType);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep(), ingType);

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
	protected YeastAddition createIngredientAddition(Yeast selectedItem)
	{
		return new YeastAddition(
			selectedItem,
			quantity.getQuantity(),
			quantity.getUnit(),
			isCaptureTime() && time != null ? time.getQuantity() : null);
	}

	@Override
	protected Map<String, Yeast> getReferenceIngredients()
	{
		return Database.getInstance().getYeasts();
	}
}
