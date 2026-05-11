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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;
import mclachlan.brewday.ui.swing.widgets.SwingQuantitySelectAndEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code MiscAdditionDialog}.
 */
public class SwingMiscAdditionDialog extends SwingIngredientAdditionDialog<MiscAddition, Misc>
{
	private SwingQuantitySelectAndEditWidget quantity;
	private SwingQuantityEditWidget<TimeUnit> time;
	private Misc lastMiscForQuantityReset;

	public SwingMiscAdditionDialog(Window parent, ProcessStep step, MiscAddition addition, boolean captureTime)
	{
		super(parent, SwingIcons.IconKey.MISC, "common.add.misc", step, captureTime);

		getIngredientTable().getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
				{
					return;
				}
				Misc m = getSelectedReferenceIngredient();
				if (m == null || m.getMeasurementType() == null)
				{
					return;
				}
				if (lastMiscForQuantityReset == null || m.getMeasurementType() != lastMiscForQuantityReset.getMeasurementType())
				{
					lastMiscForQuantityReset = m;
					quantity.setUnitOptions(m.getMeasurementType().getDefaultUnit(), Quantity.Type.WEIGHT, Quantity.Type.VOLUME);
					quantity.setQuantity(Quantity.parseQuantity("0", m.getMeasurementType().getDefaultUnit()));
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
		return IngredientAddition.Type.MISC;
	}

	@Override
	protected int getTableColumnCount()
	{
		return 5;
	}

	@Override
	protected String getTableColumnKey(int column)
	{
		return switch (column)
		{
			case 0 -> "misc.name";
			case 1 -> "misc.type";
			case 2 -> "misc.use";
			case 3 -> "misc.usage.recommendation";
			case 4 -> "ingredient.addition.amount.in.inventory";
			default -> "";
		};
	}

	@Override
	protected Object getTableCellValue(Misc row, int column)
	{
		return switch (column)
		{
			case 0 -> row.getName();
			case 1 -> row.getType();
			case 2 -> row.getUse();
			case 3 -> row.getUsageRecommendation();
			case 4 -> formatInventoryCell(row);
			default -> "";
		};
	}

	@Override
	protected void addUiStuffs(JPanel pane)
	{
		Settings settings = Database.getInstance().getSettings();
		IngredientAddition.Type ingType = IngredientAddition.Type.MISC;
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
	protected MiscAddition createIngredientAddition(Misc selectedItem)
	{
		return new MiscAddition(
			selectedItem,
			quantity.getQuantity(),
			quantity.getUnit(),
			isCaptureTime() && time != null ? time.getQuantity() : null);
	}

	@Override
	protected Map<String, Misc> getReferenceIngredients()
	{
		return Database.getInstance().getMiscs();
	}
}
