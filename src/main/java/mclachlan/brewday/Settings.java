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

/**
 *
 */
public class Settings
{
	/*-------------------------------------------------------------------------*/
	// brewing settings
	public static final String DEFAULT_EQUIPMENT_PROFILE = "default.equipment.profile";

	// backend settings
	public static final String GOOGLE_DRIVE_DIRECTORY_NAME = "backend.google.drive.directory.name";
	public static final String GOOGLE_DRIVE_DIRECTORY_ID = "backend.google.drive.directory.id";
	public static final String GOOGLE_DRIVE_AUTO_SYNC = "backend.google.drive.auto.sync";

	// ui settings
	public static final String UI_THEME = "ui.theme";
	public static final String JMETRO_LIGHT = "jmetro.light";
	public static final String JMETRO_DARK = "jmetro.dark";
	public static final String MODENA = "modena";
	public static final String CASPIAN = "caspian";

	// import settings
	public static final String LAST_IMPORT_DIRECTORY = "last.import.directory";

	/*-------------------------------------------------------------------------*/
	private Map<String, String> settings;

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
	public Quantity.Unit getUnitForStepAndIngredient(
		Quantity.Type quantityType,
		ProcessStep.Type step,
		IngredientAddition.Type ingredient)
	{
		switch (ingredient)
		{
			case FERMENTABLES:
				switch (quantityType)
				{
					case WEIGHT:
						return Quantity.Unit.KILOGRAMS;
					case VOLUME:
						return Quantity.Unit.LITRES;
					case TEMPERATURE:
						return Quantity.Unit.CELSIUS;
					case TIME:
						switch (step)
						{
							case MASH:
							case MASH_INFUSION:
							case BATCH_SPARGE:
							case BOIL:
							case DILUTE:
							case COOL:
							case STAND:
							case SPLIT_BY_PERCENT:
								return Quantity.Unit.MINUTES;
							case FERMENT:
							case PACKAGE:
								return Quantity.Unit.DAYS;
						}
					case FLUID_DENSITY:
						return Quantity.Unit.SPECIFIC_GRAVITY;
					case COLOUR:
						return Quantity.Unit.SRM;
					case BITTERNESS:
						return Quantity.Unit.IBU;
					case CARBONATION:
						return Quantity.Unit.VOLUMES;
					case PRESSURE:
						return Quantity.Unit.KPA;
					case SPECIFIC_HEAT:
						return Quantity.Unit.JOULE_PER_KG_CELSIUS;
					case DIASTATIC_POWER:
						return Quantity.Unit.LINTNER;
					case OTHER:
						return Quantity.Unit.PERCENTAGE;
				}
			case HOPS:
				switch (quantityType)
				{
					case WEIGHT:
						return Quantity.Unit.GRAMS;
					case VOLUME:
						return Quantity.Unit.MILLILITRES;
					case TEMPERATURE:
						return Quantity.Unit.CELSIUS;
					case TIME:
						switch (step)
						{
							case MASH:
							case MASH_INFUSION:
							case BATCH_SPARGE:
							case BOIL:
							case DILUTE:
							case COOL:
							case STAND:
							case SPLIT_BY_PERCENT:
								return Quantity.Unit.MINUTES;
							case FERMENT:
							case PACKAGE:
								return Quantity.Unit.DAYS;
						}
					case FLUID_DENSITY:
						return Quantity.Unit.SPECIFIC_GRAVITY;
					case COLOUR:
						return Quantity.Unit.SRM;
					case BITTERNESS:
						return Quantity.Unit.IBU;
					case CARBONATION:
						return Quantity.Unit.VOLUMES;
					case PRESSURE:
						return Quantity.Unit.KPA;
					case SPECIFIC_HEAT:
						return Quantity.Unit.JOULE_PER_KG_CELSIUS;
					case DIASTATIC_POWER:
						return Quantity.Unit.LINTNER;
					case OTHER:
						return Quantity.Unit.PERCENTAGE;
				}
			case WATER:
				switch (quantityType)
				{
					case WEIGHT:
						return Quantity.Unit.KILOGRAMS;
					case VOLUME:
						return Quantity.Unit.LITRES;
					case TEMPERATURE:
						return Quantity.Unit.CELSIUS;
					case TIME:
						switch (step)
						{
							case MASH:
							case MASH_INFUSION:
							case BATCH_SPARGE:
							case BOIL:
							case DILUTE:
							case COOL:
							case STAND:
							case SPLIT_BY_PERCENT:
								return Quantity.Unit.MINUTES;
							case FERMENT:
							case PACKAGE:
								return Quantity.Unit.DAYS;
						}
					case FLUID_DENSITY:
						return Quantity.Unit.SPECIFIC_GRAVITY;
					case COLOUR:
						return Quantity.Unit.SRM;
					case BITTERNESS:
						return Quantity.Unit.IBU;
					case CARBONATION:
						return Quantity.Unit.VOLUMES;
					case PRESSURE:
						return Quantity.Unit.KPA;
					case SPECIFIC_HEAT:
						return Quantity.Unit.JOULE_PER_KG_CELSIUS;
					case DIASTATIC_POWER:
						return Quantity.Unit.LINTNER;
					case OTHER:
						return Quantity.Unit.PERCENTAGE;
				}
			case YEAST:
				switch (quantityType)
				{
					case WEIGHT:
						return Quantity.Unit.GRAMS;
					case VOLUME:
						return Quantity.Unit.MILLILITRES;
					case TEMPERATURE:
						return Quantity.Unit.CELSIUS;
					case TIME:
						return Quantity.Unit.DAYS;
					case FLUID_DENSITY:
						return Quantity.Unit.SPECIFIC_GRAVITY;
					case COLOUR:
						return Quantity.Unit.SRM;
					case BITTERNESS:
						return Quantity.Unit.IBU;
					case CARBONATION:
						return Quantity.Unit.VOLUMES;
					case PRESSURE:
						return Quantity.Unit.KPA;
					case SPECIFIC_HEAT:
						return Quantity.Unit.JOULE_PER_KG_CELSIUS;
					case DIASTATIC_POWER:
						return Quantity.Unit.LINTNER;
					case OTHER:
						return Quantity.Unit.PERCENTAGE;
				}
			case MISC:
				switch (quantityType)
				{
					case WEIGHT:
						return Quantity.Unit.GRAMS;
					case VOLUME:
						return Quantity.Unit.MILLILITRES;
					case TEMPERATURE:
						return Quantity.Unit.CELSIUS;
					case TIME:
						switch (step)
						{
							case MASH:
							case MASH_INFUSION:
							case BATCH_SPARGE:
							case BOIL:
							case DILUTE:
							case COOL:
							case STAND:
							case SPLIT_BY_PERCENT:
								return Quantity.Unit.MINUTES;
							case FERMENT:
							case PACKAGE:
								return Quantity.Unit.DAYS;
						}
					case FLUID_DENSITY:
						return Quantity.Unit.SPECIFIC_GRAVITY;
					case COLOUR:
						return Quantity.Unit.SRM;
					case BITTERNESS:
						return Quantity.Unit.IBU;
					case CARBONATION:
						return Quantity.Unit.VOLUMES;
					case PRESSURE:
						return Quantity.Unit.KPA;
					case SPECIFIC_HEAT:
						return Quantity.Unit.JOULE_PER_KG_CELSIUS;
					case DIASTATIC_POWER:
						return Quantity.Unit.LINTNER;
					case OTHER:
						return Quantity.Unit.PERCENTAGE;
				}
			default:
				throw new BrewdayException("invalid " + quantityType);
		}
	}

	/*-------------------------------------------------------------------------*/
	public String getStringFormatter(double v)
	{
		double abs = Math.abs(v);
		if (abs > 1000)
		{
			return "%.0f";
		}
		else if (abs > 100)
		{
			return "%.1f";
		}
		else if (abs > 2)
		{
			return "%.2f";
		}
		else
		{
			return "%.3f";
		}
	}
}
