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

import java.awt.Frame;
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
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;
import mclachlan.brewday.ui.swing.widgets.SwingQuantitySelectAndEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code HopAdditionDialog}.
 */
public class SwingHopAdditionDialog extends SwingIngredientAdditionDialog<HopAddition, Hop>
{
	private SwingQuantitySelectAndEditWidget quantity;
	private SwingQuantityEditWidget<TimeUnit> time;
	private Hop lastHopForQuantityReset;

	public SwingHopAdditionDialog(Frame parent, ProcessStep step, HopAddition addition, boolean captureTime)
	{
		super(parent, SwingIcons.IconKey.HOPS, "common.add.hop", step, captureTime);

		getIngredientTable().getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
				{
					return;
				}
				Hop h = getSelectedReferenceIngredient();
				if (h == null)
				{
					return;
				}
				if (lastHopForQuantityReset == null || h.getForm() != lastHopForQuantityReset.getForm())
				{
					lastHopForQuantityReset = h;
					quantity.setUnitOptions(h.getForm().getDefaultUnit(),
						Quantity.Type.WEIGHT, Quantity.Type.VOLUME);
					quantity.setQuantity(Quantity.parseQuantity("0", h.getForm().getDefaultUnit()));
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
		return IngredientAddition.Type.HOPS;
	}

	@Override
	protected int getTableColumnCount()
	{
		return 6;
	}

	@Override
	protected String getTableColumnKey(int column)
	{
		return switch (column)
		{
			case 0 -> "hop.name";
			case 1 -> "hop.type";
			case 2 -> "hop.origin";
			case 3 -> "hop.alpha";
			case 4 -> "hop.beta";
			case 5 -> "ingredient.addition.amount.in.inventory";
			default -> "";
		};
	}

	@Override
	protected Object getTableCellValue(Hop row, int column)
	{
		return switch (column)
		{
			case 0 -> row.getName();
			case 1 -> row.getType();
			case 2 -> row.getOrigin();
			case 3 -> row.getAlphaAcid() == null ? "" : row.getAlphaAcid().describe(Quantity.Unit.PERCENTAGE_DISPLAY);
			case 4 -> row.getBetaAcid() == null ? "" : row.getBetaAcid().describe(Quantity.Unit.PERCENTAGE_DISPLAY);
			case 5 -> formatInventoryCell(row);
			default -> "";
		};
	}

	@Override
	protected void addUiStuffs(JPanel pane)
	{
		Settings settings = Database.getInstance().getSettings();
		IngredientAddition.Type ingType = IngredientAddition.Type.HOPS;
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
	protected HopAddition createIngredientAddition(Hop selectedItem)
	{
		return new HopAddition(
			selectedItem,
			quantity.getQuantity(),
			quantity.getUnit(),
			isCaptureTime() && time != null ? time.getQuantity() : null);
	}

	@Override
	protected Map<String, Hop> getReferenceIngredients()
	{
		return Database.getInstance().getHops();
	}

	SwingQuantitySelectAndEditWidget getQuantityWidgetForTest()
	{
		return quantity;
	}

	SwingQuantityEditWidget<TimeUnit> getTimeWidgetForTest()
	{
		return time;
	}
}
