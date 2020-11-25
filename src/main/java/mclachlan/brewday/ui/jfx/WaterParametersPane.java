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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.WaterParameters;
import net.miginfocom.layout.AC;

/**
 *
 */
public class WaterParametersPane extends V2DataObjectPane<WaterParameters>
{
	/*-------------------------------------------------------------------------*/
	public WaterParametersPane(String dirtyFlag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "water.parameters", Icons.waterIcon, Icons.addWater);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected V2ObjectEditor<WaterParameters> editItemDialog(WaterParameters water, TrackDirty parent)
	{
		return new V2ObjectEditor<>(water, parent)
		{
			@Override
			protected void buildUi(WaterParameters obj, TrackDirty parent)
			{
				this.setColumnConstraints(new AC().count(4).gap("20",1));

				this.add(new Label(StringUtils.getUiString("water.parameters.name")));
				this.add(new Label(obj.getName()), "wrap");

				addQuantityWidget(obj, parent, "water.parameters.min.calcium",
					WaterParameters::getMinCalcium, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMinCalcium,
					Quantity.Unit.PPM, null);
				addQuantityWidget(obj, parent, "water.parameters.max.calcium",
					WaterParameters::getMaxCalcium, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMaxCalcium,
					Quantity.Unit.PPM, "wrap");

				addQuantityWidget(obj, parent, "water.parameters.min.bicarbonate",
					WaterParameters::getMinBicarbonate, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMinBicarbonate,
					Quantity.Unit.PPM, null);
				addQuantityWidget(obj, parent, "water.parameters.max.bicarbonate",
					WaterParameters::getMaxBicarbonate, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMaxBicarbonate,
					Quantity.Unit.PPM, "wrap");

				addQuantityWidget(obj, parent, "water.parameters.min.sulfate",
					WaterParameters::getMinSulfate, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMinSulfate,
					Quantity.Unit.PPM, null);
				addQuantityWidget(obj, parent, "water.parameters.max.sulfate",
					WaterParameters::getMaxSulfate, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMaxSulfate,
					Quantity.Unit.PPM, "wrap");

				addQuantityWidget(obj, parent, "water.parameters.min.chloride",
					WaterParameters::getMinChloride, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMinChloride,
					Quantity.Unit.PPM, null);
				addQuantityWidget(obj, parent, "water.parameters.max.chloride",
					WaterParameters::getMaxChloride, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMaxChloride,
					Quantity.Unit.PPM, "wrap");

				addQuantityWidget(obj, parent, "water.parameters.min.sodium",
					WaterParameters::getMinSodium, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMinSodium,
					Quantity.Unit.PPM, null);
				addQuantityWidget(obj, parent, "water.parameters.max.sodium",
					WaterParameters::getMaxSodium, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMaxSodium,
					Quantity.Unit.PPM, "wrap");

				addQuantityWidget(obj, parent, "water.parameters.min.magnesium",
					WaterParameters::getMinMagnesium, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMinMagnesium,
					Quantity.Unit.PPM, null);
				addQuantityWidget(obj, parent, "water.parameters.max.magnesium",
					WaterParameters::getMaxMagnesium, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMaxMagnesium,
					Quantity.Unit.PPM, "wrap");

				addQuantityWidget(obj, parent, "water.parameters.min.alkalinity",
					WaterParameters::getMinAlkalinity, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMinAlkalinity,
					Quantity.Unit.PPM, null);
				addQuantityWidget(obj, parent, "water.parameters.max.alkalinity",
					WaterParameters::getMaxAlkalinity, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMaxAlkalinity,
					Quantity.Unit.PPM, "wrap");

				addQuantityWidget(obj, parent, "water.parameters.min.residual.alkalinity",
					WaterParameters::getMinResidualAlkalinity, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMinResidualAlkalinity,
					Quantity.Unit.PPM, null);
				addQuantityWidget(obj, parent, "water.parameters.max.residual.alkalinity",
					WaterParameters::getMaxResidualAlkalinity, (BiConsumer<WaterParameters, PpmUnit>)WaterParameters::setMaxResidualAlkalinity,
					Quantity.Unit.PPM, "wrap");

				addTextArea(obj, parent, "water.addition.desc", WaterParameters::getDescription, WaterParameters::setDescription, "span, wrap");
			}
		};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected WaterParameters createDuplicateItem(WaterParameters current, String newName)
	{
		WaterParameters result = new WaterParameters(current);
		result.setName(newName);
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected WaterParameters createNewItem(String name)
	{
		return new WaterParameters(name);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Map<String, WaterParameters> getMap(Database database)
	{
		return database.getWaterParameters();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected TableColumn<WaterParameters, String>[] getTableColumns(String labelPrefix)
	{
		return (TableColumn<WaterParameters, String>[])new TableColumn[]
			{
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".min.calcium", WaterParameters::getMinCalcium, Quantity.Unit.PPM),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".max.calcium", WaterParameters::getMaxCalcium, Quantity.Unit.PPM),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".min.bicarbonate", WaterParameters::getMinBicarbonate, Quantity.Unit.PPM),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".max.bicarbonate", WaterParameters::getMaxBicarbonate, Quantity.Unit.PPM),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".min.sulfate", WaterParameters::getMinSulfate, Quantity.Unit.PPM),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".max.sulfate", WaterParameters::getMaxSulfate, Quantity.Unit.PPM),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".min.alkalinity", WaterParameters::getMinAlkalinity, Quantity.Unit.PPM),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".max.alkalinity", WaterParameters::getMaxAlkalinity, Quantity.Unit.PPM),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".min.residual.alkalinity", WaterParameters::getMinResidualAlkalinity, Quantity.Unit.PPM),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".max.residual.alkalinity", WaterParameters::getMaxResidualAlkalinity, Quantity.Unit.PPM),
			};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeRename(String oldName, String newName)
	{
		// no op
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeDelete(String deletedName)
	{
		// no op
	}

	@Override
	protected Image getIcon(WaterParameters water)
	{
		return Icons.waterParametersIcon;
	}
}
