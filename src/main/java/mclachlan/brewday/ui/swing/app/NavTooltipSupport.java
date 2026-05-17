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

package mclachlan.brewday.ui.swing.app;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Navigation tree and landing-screen tooltip text for {@link ScreenKey} routes.
 */
public final class NavTooltipSupport
{
	private NavTooltipSupport()
	{
	}

	public static String tooltipFor(ScreenKey key)
	{
		String propertyKey = switch (key)
		{
			case BREWING -> "nav.tooltip.brewing";
			case RECIPES -> "nav.tooltip.recipes";
			case BATCHES -> "nav.tooltip.batches";
			case PROCESS_TEMPLATES -> "nav.tooltip.process.templates";
			case EQUIPMENT_PROFILES -> "nav.tooltip.equipment.profiles";
			case INVENTORY_GROUP -> "nav.tooltip.inventory.group";
			case INVENTORY -> "nav.tooltip.inventory";
			case REFERENCE_DATABASE -> "nav.tooltip.reference.database";
			case WATER -> "nav.tooltip.water";
			case WATER_PARAMETERS -> "nav.tooltip.water.parameters";
			case FERMENTABLES -> "nav.tooltip.fermentables";
			case HOPS -> "nav.tooltip.hops";
			case YEAST -> "nav.tooltip.yeast";
			case MISC -> "nav.tooltip.misc";
			case STYLES -> "nav.tooltip.styles";
			case TOOLS -> "nav.tooltip.tools";
			case IMPORT -> "nav.tooltip.import";
			case WATER_BUILDER -> "nav.tooltip.water.builder";
			case KEG_LINE_LENGTH -> "nav.tooltip.keg.line.length";
			case SETTINGS -> "nav.tooltip.settings";
			case BREWING_SETTINGS -> "nav.tooltip.brewing.settings";
			case BREWING_SETTINGS_GENERAL -> "nav.tooltip.brewing.settings.general";
			case BREWING_SETTINGS_MASH -> "nav.tooltip.brewing.settings.mash";
			case BREWING_SETTINGS_IBU -> "nav.tooltip.brewing.settings.ibu";
			case BACKEND_SETTINGS -> "nav.tooltip.backend.settings";
			case BACKEND_SETTINGS_LOCAL_FILESYSTEM -> "nav.tooltip.backend.settings.local";
			case BACKEND_SETTINGS_GIT -> "nav.tooltip.backend.settings.git";
			case UI_SETTINGS -> "nav.tooltip.ui.settings";
			case HELP -> "nav.tooltip.help";
			case ABOUT -> "nav.tooltip.about";
		};
		return getUiString(propertyKey);
	}

	public static String tooltipForRecipeTag(String tag)
	{
		return getUiString("nav.tooltip.recipe.tag", tag);
	}
}
