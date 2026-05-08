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
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;
import mclachlan.brewday.ui.swing.widgets.SwingQuantitySelectAndEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code WaterAdditionDialog}.
 */
public class SwingWaterAdditionDialog extends SwingIngredientAdditionDialog<WaterAddition, Water>
{
	private SwingQuantityEditWidget<TemperatureUnit> temperature;
	private SwingQuantitySelectAndEditWidget quantity;
	private SwingQuantityEditWidget<TimeUnit> time;

	public SwingWaterAdditionDialog(Frame parent, ProcessStep step, WaterAddition addition, boolean captureTimeAndTemp)
	{
		super(parent, SwingIcons.IconKey.WATER, "common.add.water", step, captureTimeAndTemp);

		getIngredientTable().getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
				{
					return;
				}
				Water w = getSelectedReferenceIngredient();
				if (w != null)
				{
					quantity.setQuantity(Quantity.parseQuantity("0", quantity.getUnit()));
				}
			}
		});

		if (addition != null)
		{
			quantity.setUnitOptions(addition.getUnit(), Quantity.Type.VOLUME);
			quantity.setQuantity(addition.getQuantity());
			if (captureTimeAndTemp && temperature != null && time != null)
			{
				temperature.setQuantity(addition.getTemperature());
				time.setQuantity(addition.getTime());
			}
		}
	}

	@Override
	protected IngredientAddition.Type getIngredientType()
	{
		return IngredientAddition.Type.WATER;
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
			case 0 -> "water.name";
			case 1 -> "water.calcium.abbr";
			case 2 -> "water.bicarbonate.abbr";
			case 3 -> "water.sulfate.abbr";
			case 4 -> "water.chloride.abbr";
			case 5 -> "water.alkalinity";
			case 6 -> "water.ra";
			case 7 -> "ingredient.addition.amount.in.inventory";
			default -> "";
		};
	}

	@Override
	protected Object getTableCellValue(Water row, int column)
	{
		return switch (column)
		{
			case 0 -> row.getName();
			case 1 -> ppmCell(row.getCalcium());
			case 2 -> ppmCell(row.getBicarbonate());
			case 3 -> ppmCell(row.getSulfate());
			case 4 -> ppmCell(row.getChloride());
			case 5 -> ppmCell(row.getAlkalinity());
			case 6 -> ppmCell(row.getResidualAlkalinity());
			case 7 -> formatInventoryCell(row);
			default -> "";
		};
	}

	private static String ppmCell(mclachlan.brewday.math.PpmUnit u)
	{
		if (u == null)
		{
			return "";
		}
		return u.describe(Quantity.Unit.PPM);
	}

	@Override
	protected void addUiStuffs(JPanel pane)
	{
		Settings settings = Database.getInstance().getSettings();
		Quantity.Unit tempUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TEMPERATURE, getStep(),
			IngredientAddition.Type.WATER);
		Quantity.Unit volUnit = settings.getUnitForStepAndIngredient(Quantity.Type.VOLUME, getStep(),
			IngredientAddition.Type.WATER);
		Quantity.Unit timeUnit = settings.getUnitForStepAndIngredient(Quantity.Type.TIME, getStep(),
			IngredientAddition.Type.WATER);

		quantity = new SwingQuantitySelectAndEditWidget(volUnit, Quantity.Type.VOLUME);

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

			temperature = new SwingQuantityEditWidget<>(tempUnit);
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 0;
			pane.add(new JLabel(getUiString("water.addition.temperature")), gbc);
			gbc.gridx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1.0;
			pane.add(temperature, gbc);
		}
	}

	@Override
	protected WaterAddition createIngredientAddition(Water selectedItem)
	{
		return new WaterAddition(
			selectedItem,
			quantity.getQuantity(),
			quantity.getUnit(),
			isCaptureTime() && temperature != null ? temperature.getQuantity() : null,
			isCaptureTime() && time != null ? time.getQuantity() : null);
	}

	@Override
	protected Map<String, Water> getReferenceIngredients()
	{
		return Database.getInstance().getWaters();
	}

	SwingQuantitySelectAndEditWidget getQuantityWidgetForTest()
	{
		return quantity;
	}

	SwingQuantityEditWidget<TimeUnit> getTimeWidgetForTest()
	{
		return time;
	}

	SwingQuantityEditWidget<TemperatureUnit> getTemperatureWidgetForTest()
	{
		return temperature;
	}
}
