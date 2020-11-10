/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the equipmentProfilee that it will be useful,
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
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.Recipe;
import net.miginfocom.layout.AC;

/**
 *
 */
public class EquipmentProfilePane extends V2DataObjectPane<EquipmentProfile>
{
	/*-------------------------------------------------------------------------*/
	public EquipmentProfilePane(String dirtyFlag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "equipment", Icons.equipmentIcon, Icons.newIcon);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected V2ObjectEditor<EquipmentProfile> editItemDialog(EquipmentProfile obj, TrackDirty parent)
	{
		return new V2ObjectEditor<>(obj, parent)
		{
			@Override
			protected void buildUi(EquipmentProfile obj, TrackDirty parent)
			{
				this.setColumnConstraints(new AC().count(4).gap("20",1));

				this.add(new Label(StringUtils.getUiString("equipment.name")));
				this.add(new Label(obj.getName()), "wrap");

				// Elevation
				addQuantityWidget(obj, parent, "equipment.elevation",
					EquipmentProfile::getElevation,
					(BiConsumer<EquipmentProfile, LengthUnit>)EquipmentProfile::setElevation,
					Quantity.Unit.METRE,
					"wrap");

				// Mash Efficiency
				addQuantityWidget(obj, parent, "equipment.conversion.efficiency",
					EquipmentProfile::getConversionEfficiency,
					(BiConsumer<EquipmentProfile, PercentageUnit>)EquipmentProfile::setConversionEfficiency,
					Quantity.Unit.PERCENTAGE_DISPLAY,
					"wrap");

				// Mash Tun Vol
				addQuantityWidget(obj, parent, "equipment.mash.tun.volume",
					EquipmentProfile::getMashTunVolume,
					(BiConsumer<EquipmentProfile, VolumeUnit>)EquipmentProfile::setMashTunVolume,
					Quantity.Unit.LITRES,
					null);

				// Mash Tun Weight
				addQuantityWidget(obj, parent, "equipment.mash.tun.weight",
					EquipmentProfile::getMashTunWeight,
					(BiConsumer<EquipmentProfile, WeightUnit>)EquipmentProfile::setMashTunWeight,
					Quantity.Unit.KILOGRAMS,
					"wrap");

				// Mash Tun Spec Heat
				addQuantityWidget(obj, parent, "equipment.mash.tun.specific.heat",
					EquipmentProfile::getMashTunSpecificHeat,
					(BiConsumer<EquipmentProfile, ArbitraryPhysicalQuantity>)EquipmentProfile::setMashTunSpecificHeat,
					Quantity.Unit.JOULE_PER_KG_CELSIUS,
					null);

				// Lauter Loss
				addQuantityWidget(obj, parent, "equipment.lauter.loss",
					EquipmentProfile::getLauterLoss,
					(BiConsumer<EquipmentProfile, VolumeUnit>)EquipmentProfile::setLauterLoss,
					Quantity.Unit.LITRES,
					"wrap");

				// Kettle Vol
				addQuantityWidget(obj, parent, "equipment.boil.kettle.volume",
					EquipmentProfile::getBoilKettleVolume,
					(BiConsumer<EquipmentProfile, VolumeUnit>)EquipmentProfile::setBoilKettleVolume,
					Quantity.Unit.LITRES,
					null);

				// Kettle Evap Rate
				addQuantityWidget(obj, parent, "equipment.evapouration",
					EquipmentProfile::getBoilEvapourationRate,
					(BiConsumer<EquipmentProfile, PercentageUnit>)EquipmentProfile::setBoilEvapourationRate,
					Quantity.Unit.PERCENTAGE_DISPLAY,
					"wrap");

				// Boil element power
				addQuantityWidget(obj, parent, "equipment.boil.element.power",
					EquipmentProfile::getBoilElementPower,
					(BiConsumer<EquipmentProfile, PowerUnit>)EquipmentProfile::setBoilElementPower,
					Quantity.Unit.KILOWATT,
					"wrap");

				// Hop Util
				addQuantityWidget(obj, parent, "equipment.hop.utilisation",
					EquipmentProfile::getHopUtilisation,
					(BiConsumer<EquipmentProfile, PercentageUnit>)EquipmentProfile::setHopUtilisation,
					Quantity.Unit.PERCENTAGE_DISPLAY,
					null);

				// Trub & Chiller Loss
				addQuantityWidget(obj, parent, "equipment.trub.chiller.loss",
					EquipmentProfile::getTrubAndChillerLoss,
					(BiConsumer<EquipmentProfile, VolumeUnit>)EquipmentProfile::setTrubAndChillerLoss,
					Quantity.Unit.LITRES,
					"wrap");

				// Fermenter Vol
				addQuantityWidget(obj, parent, "equipment.fermenter.volume",
					EquipmentProfile::getFermenterVolume,
					(BiConsumer<EquipmentProfile, VolumeUnit>)EquipmentProfile::setFermenterVolume,
					Quantity.Unit.LITRES,
					"wrap");

				// Desc
				addTextArea(obj, parent, "equipment.desc", EquipmentProfile::getDescription, EquipmentProfile::setDescription, "span, wrap");
			}
		};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected EquipmentProfile createDuplicateItem(EquipmentProfile current, String newName)
	{
		EquipmentProfile result = new EquipmentProfile(current);
		result.setName(newName);
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected EquipmentProfile createNewItem(String name)
	{
		return new EquipmentProfile(name);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Map<String, EquipmentProfile> getMap(Database database)
	{
		return database.getEquipmentProfiles();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected TableColumn<EquipmentProfile, String>[] getTableColumns(String labelPrefix)
	{
		return (TableColumn<EquipmentProfile, String>[])new TableColumn[]
			{
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".conversion.efficiency", EquipmentProfile::getConversionEfficiency, Quantity.Unit.PERCENTAGE_DISPLAY),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".mash.tun.volume", EquipmentProfile::getMashTunVolume, Quantity.Unit.LITRES),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".boil.kettle.volume", EquipmentProfile::getBoilKettleVolume, Quantity.Unit.LITRES),
				getTableBuilder().getQuantityPropertyValueCol(labelPrefix + ".fermenter.volume", EquipmentProfile::getFermenterVolume, Quantity.Unit.LITRES),
			};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeRename(String oldName, String newName)
	{
		Database db = Database.getInstance();

		// recipes
		for (Recipe recipe : db.getRecipes().values())
		{
			if (recipe.getEquipmentProfile().equalsIgnoreCase(oldName))
			{
				recipe.setEquipmentProfile(newName);

				JfxUi.getInstance().setDirty(JfxUi.RECIPES);
				JfxUi.getInstance().setDirty(recipe);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeDelete(String deletedName)
	{
		Database db = Database.getInstance();

		// recipes
		for (Recipe recipe : db.getRecipes().values())
		{
			if (recipe.getEquipmentProfile().equalsIgnoreCase(deletedName))
			{
				// what to do? set to the default?
				recipe.setEquipmentProfile(db.getSettings().get(Settings.DEFAULT_EQUIPMENT_PROFILE));

				JfxUi.getInstance().setDirty(JfxUi.RECIPES);
				JfxUi.getInstance().setDirty(recipe);
			}
		}
	}

	@Override
	protected Image getIcon(EquipmentProfile equipmentProfile)
	{
		return Icons.equipmentIcon;
	}
}
