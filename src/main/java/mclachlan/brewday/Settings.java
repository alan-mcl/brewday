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

package mclachlan.brewday;

import java.util.Map;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.util.StringUtils;

/**
 *
 */
public class Settings
{
	/*-------------------------------------------------------------------------*/
	//------ brewing settings
	// equipment
	public static final String DEFAULT_EQUIPMENT_PROFILE = "default.equipment.profile";

	// mash ph
	public static final String MASH_PH_MODEL = "mash.ph.model";
	public static final String MPH_MALT_BUFFERING_CORRECTION_FACTOR = "mph.malt.buffering.correction.factor";

	// hops
	public static final String MASH_HOP_UTILISATION = "mash.hop.utilisation";
	public static final String FIRST_WORT_HOP_UTILISATION = "first.wort.hop.utilisation";
	public static final String LEAF_HOP_ADJUSTMENT = "hop.adjustment.leaf";
	public static final String PLUG_HOP_ADJUSTMENT = "hop.adjustment.plug";
	public static final String PELLET_HOP_ADJUSTMENT = "hop.adjustment.pellet";
	public static final String HOP_BITTERNESS_FORMULA = "hop.bitterness.formula";
	public static final String TINSETH_MAX_UTILISATION = "tinseth.max.utilisation";
	public static final String GARETZ_YEAST_FACTOR = "garetz.yeast.factor";
	public static final String GARETZ_PELLET_FACTOR = "garetz.pellet.factor";
	public static final String GARETZ_BAG_FACTOR = "garetz.bag.factor";
	public static final String GARETZ_FILTER_FACTOR = "garetz.filter.factor";

	// backend settings
	public static final String GOOGLE_DRIVE_DIRECTORY_NAME = "backend.google.drive.directory.name";
	public static final String GOOGLE_DRIVE_DIRECTORY_ID = "backend.google.drive.directory.id";
	public static final String GOOGLE_DRIVE_AUTO_SYNC = "backend.google.drive.auto.sync";

	public static final String GIT_BACKEND_ENABLED = "backend.git.enabled";
	public static final String GIT_REMOTE_REPO = "backend.git.remote.repo";


	// ui theme settings
	public static final String UI_THEME = "ui.theme";
	public static final String JMETRO_LIGHT = "jmetro.light";
	public static final String JMETRO_DARK = "jmetro.dark";
	public static final String MODENA = "modena";
	public static final String CASPIAN = "caspian";

	// random ux settings
	public static final String INGREDIENT_ADDITIONS_FROM_INVENTORY_ONLY = "ux.ingredient.additions.from.inventory.only";

	// import/export settings
	public static final String LAST_IMPORT_DIRECTORY = "last.import.directory";
	public static final String LAST_EXPORT_DIRECTORY = "last.export.directory";

	// feature toggles
	public static final String FEATURE_TOGGLE_REMOTE_BACKENDS = "feature.remote.backends";

	/*-------------------------------------------------------------------------*/
	private final Map<String, String> settings;

	public Settings(Map<String, String> settings)
	{
		this.settings = settings;
	}

	public String get(String name)
	{
		return settings.get(name);
	}

	public void set(String name, String value)
	{
		settings.put(name, value);
	}

	public Map<String, String> getSettings()
	{
		return settings;
	}

	/*-------------------------------------------------------------------------*/
	public boolean isFeatureOn(String toggle)
	{
		return Boolean.parseBoolean(this.get(toggle));
	}

	/*-------------------------------------------------------------------------*/
	public Quantity.Unit getUnitForStepAndIngredient(
		Quantity.Type quantityType,
		ProcessStep step,
		IngredientAddition.Type ingredient)
	{
		ProcessStep.Type stepType;
		if (step != null)
		{
			stepType = step.getType();
		}
		else
		{
			// default
			stepType = ProcessStep.Type.MASH;
		}

		return getUnitForStepAndIngredient(quantityType, stepType, ingredient);
	}

	/*-------------------------------------------------------------------------*/
	public Quantity.Unit getUnitForStepAndIngredient(Quantity.Type quantityType,
		ProcessStep.Type stepType, IngredientAddition.Type ingredient)
	{
		return switch (ingredient)
			{
				case FERMENTABLES -> switch (quantityType)
					{
						case WEIGHT -> Quantity.Unit.KILOGRAMS;
						case VOLUME -> Quantity.Unit.LITRES;
						case LENGTH -> Quantity.Unit.MILLILITRES;
						case TEMPERATURE -> Quantity.Unit.CELSIUS;
						case TIME -> switch (stepType)
							{
								case MASH, MASH_INFUSION, LAUTER, BATCH_SPARGE, BOIL,
									DILUTE, COOL, HEAT, STAND, SPLIT, COMBINE ->
									Quantity.Unit.MINUTES;
								case FERMENT, PACKAGE -> Quantity.Unit.DAYS;
							};
						case FLUID_DENSITY -> Quantity.Unit.SPECIFIC_GRAVITY;
						case COLOUR -> Quantity.Unit.SRM;
						case BITTERNESS -> Quantity.Unit.IBU;
						case CARBONATION -> Quantity.Unit.VOLUMES;
						case PRESSURE -> Quantity.Unit.KPA;
						case SPECIFIC_HEAT -> Quantity.Unit.JOULE_PER_KG_CELSIUS;
						case DIASTATIC_POWER -> Quantity.Unit.LINTNER;
						case POWER -> Quantity.Unit.KILOWATT;
						case OTHER -> Quantity.Unit.PERCENTAGE;
					};
				case HOPS, MISC -> switch (quantityType)
					{
						case WEIGHT -> Quantity.Unit.GRAMS;
						case LENGTH, VOLUME -> Quantity.Unit.MILLILITRES;
						case TEMPERATURE -> Quantity.Unit.CELSIUS;
						case TIME -> switch (stepType)
							{
								case MASH, MASH_INFUSION, LAUTER, BATCH_SPARGE,
									BOIL, DILUTE, COOL, HEAT, STAND, SPLIT, COMBINE ->
									Quantity.Unit.MINUTES;
								case FERMENT, PACKAGE -> Quantity.Unit.DAYS;
							};
						case FLUID_DENSITY -> Quantity.Unit.SPECIFIC_GRAVITY;
						case COLOUR -> Quantity.Unit.SRM;
						case BITTERNESS -> Quantity.Unit.IBU;
						case CARBONATION -> Quantity.Unit.VOLUMES;
						case PRESSURE -> Quantity.Unit.KPA;
						case SPECIFIC_HEAT -> Quantity.Unit.JOULE_PER_KG_CELSIUS;
						case DIASTATIC_POWER -> Quantity.Unit.LINTNER;
						case POWER -> Quantity.Unit.KILOWATT;
						case OTHER -> Quantity.Unit.PERCENTAGE;
					};
				case WATER -> switch (quantityType)
					{
						case WEIGHT -> Quantity.Unit.KILOGRAMS;
						case LENGTH -> Quantity.Unit.MILLILITRES;
						case VOLUME -> Quantity.Unit.LITRES;
						case TEMPERATURE -> Quantity.Unit.CELSIUS;
						case TIME -> switch (stepType)
							{
								case MASH, MASH_INFUSION, LAUTER, BATCH_SPARGE, BOIL,
									DILUTE, COOL, HEAT, STAND, SPLIT, COMBINE ->
									Quantity.Unit.MINUTES;
								case FERMENT, PACKAGE -> Quantity.Unit.DAYS;
							};
						case FLUID_DENSITY -> Quantity.Unit.SPECIFIC_GRAVITY;
						case COLOUR -> Quantity.Unit.SRM;
						case BITTERNESS -> Quantity.Unit.IBU;
						case CARBONATION -> Quantity.Unit.VOLUMES;
						case PRESSURE -> Quantity.Unit.KPA;
						case SPECIFIC_HEAT -> Quantity.Unit.JOULE_PER_KG_CELSIUS;
						case DIASTATIC_POWER -> Quantity.Unit.LINTNER;
						case POWER -> Quantity.Unit.KILOWATT;
						case OTHER -> Quantity.Unit.PERCENTAGE;
					};
				case YEAST -> switch (quantityType)
					{
						case WEIGHT -> Quantity.Unit.PACKET_11_G;
						case LENGTH, VOLUME -> Quantity.Unit.MILLILITRES;
						case TEMPERATURE -> Quantity.Unit.CELSIUS;
						case TIME -> Quantity.Unit.DAYS;
						case FLUID_DENSITY -> Quantity.Unit.SPECIFIC_GRAVITY;
						case COLOUR -> Quantity.Unit.SRM;
						case BITTERNESS -> Quantity.Unit.IBU;
						case CARBONATION -> Quantity.Unit.VOLUMES;
						case PRESSURE -> Quantity.Unit.KPA;
						case SPECIFIC_HEAT -> Quantity.Unit.JOULE_PER_KG_CELSIUS;
						case DIASTATIC_POWER -> Quantity.Unit.LINTNER;
						case POWER -> Quantity.Unit.KILOWATT;
						case OTHER -> Quantity.Unit.PERCENTAGE;
					};
				default -> throw new BrewdayException("invalid " + quantityType);
			};
	}

	/*-------------------------------------------------------------------------*/
	public String getDecimalFormatter(double v)
	{
		double abs = Math.abs(v);
		if (abs > 1000)
		{
			return "0";
		}
		else if (abs > 100)
		{
			return "0.#";
		}
		else if (abs > 2)
		{
			return "0.##";
		}
		else
		{
			return "0.###";
		}
	}

	/*-------------------------------------------------------------------------*/
	public enum HopBitternessFormula
	{
		TINSETH, TINSETH_BEERSMITH, RAGER, GARETZ, DANIELS;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("hop.bitterness.formula." + name());
		}
	}

	/*-------------------------------------------------------------------------*/
	public enum MashPhModel
	{
		EZ_WATER, MPH;

		@Override
		public String toString()
		{
			return StringUtils.getUiString("mash.ph.model."+name());
		}
	}
}
