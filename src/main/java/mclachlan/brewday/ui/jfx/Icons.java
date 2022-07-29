
/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javafx.scene.image.Image;
import mclachlan.brewday.BrewdayException;

public class Icons
{
	/*-------------------------------------------------------------------------*/
	public static final int ICON_SIZE = 32;
	public static final int NAV_ICON_SIZE = 24;

	/*-------------------------------------------------------------------------*/
	public static Image brewdayIcon = createImage("data/img/brewday.png");
	public static Image fermentableIconGeneric = createImage("data/img/icons8-carbohydrates-48.png");
	public static Image fermentableIconGrain = createImage("data/img/icons8-carbohydrates-48.png");
	public static Image fermentableIconSugar = createImage("data/img/icons8-sugar-cube-48.png");
	public static Image fermentableIconLiquidExtract = createImage("data/img/icons8-tin-can-48.png");
	public static Image fermentableIconDryExtract = createImage("data/img/icons8-flour-48.png");
	public static Image fermentableIconAdjunct = createImage("data/img/icons8-grains-of-rice-48.png");
	public static Image fermentableIconJuice = createImage("data/img/icons8-orange-juice-48.png");
	public static Image fermentableIconHoney = createImage("data/img/icons8-honey-48.png");
	public static Image hopsIcon = createImage("data/img/icons8-hops-48.png");
	public static Image waterIcon = createImage("data/img/icons8-water-48.png");
	public static Image waterParametersIcon = createImage("data/img/water_parameters.png");
	public static Image stepIcon = createImage("data/img/icons8-file-48.png");
	public static Image recipeIcon = createImage("data/img/icons8-beer-recipe-48.png");
	public static Image yeastIcon = createImage("data/img/icons8-experiment-48.png");
	public static Image miscIconGeneric = createImage("data/img/icons8-sugar-cubes-48.png");
	public static Image miscIconSpice = createImage("data/img/icons8-spice-48.png");
	public static Image miscIconHerb = createImage("data/img/icons8-natural-food-48.png");
	public static Image miscIconWaterAgent = createImage("data/img/icons8-spoon-of-sugar-48.png");
	public static Image miscIconAcid = createImage("data/img/icons8-acid-flask-48.png");
	public static Image miscIconFining = createImage("data/img/icons8-mana-48.png");
	public static Image miscIconFlavour = createImage("data/img/icons8-jam-48.png");
	public static Image removeIcon = createImage("data/img/icons8-delete-48.png");
	public static Image increaseIcon = createImage("data/img/icons8-plus-48.png");
	public static Image decreaseIcon = createImage("data/img/icons8-minus-48.png");
	public static Image moreTimeIcon = createImage("data/img/icons8-future-48.png");
	public static Image lessTimeIcon = createImage("data/img/icons8-time-machine-48.png");
	public static Image searchIcon = createImage("data/img/icons8-search-48.png");
	public static Image editIcon = createImage("data/img/icons8-edit-property-48.png");
	public static Image newIcon = createImage("data/img/icons8-add-new-48.png");
	public static Image deleteIcon = createImage("data/img/icons8-delete-48.png");
	public static Image duplicateIcon = createImage("data/img/icons8-transfer-48.png");
	public static Image substituteIcon = createImage("data/img/icons8-replace-48.png");
	public static Image processTemplateIcon = createImage("data/img/icons8-flow-48.png");
	public static Image beerIcon = createImage("data/img/icons8-beer-glass-48.png");
	public static Image equipmentIcon = createImage("data/img/icons8-brewsystem-48.png");
	public static Image settingsIcon = createImage("data/img/icons8-settings-48.png");
	public static Image stylesIcon = createImage("data/img/icons8-test-passed-48.png");
	public static Image databaseIcon = createImage("data/img/icons8-database-48.png");
	public static Image inventoryIcon = createImage("data/img/icons8-trolley-48.png");
	public static Image exitIcon = createImage("data/img/icons8-close-window-48.png");
	public static Image saveIcon = createImage("data/img/icons8-save-48.png");
	public static Image undoIcon = createImage("data/img/icons8-undo-48.png");
	public static Image renameIcon = createImage("data/img/icons8-rename-48.png");
	public static Image helpIcon = createImage("data/img/icons8-help-48.png");
	public static Image documentIcon = createImage("data/img/icons8-document-48.png");
	public static Image addRecipe = createImage("data/img/add_recipe.png");
	public static Image addStep = createImage("data/img/add_step.png");
	public static Image addFermentable = createImage("data/img/add_fermentable.png");
	public static Image addHops = createImage("data/img/add_hop.png");
	public static Image addWater = createImage("data/img/add_water.png");
	public static Image addYeast = createImage("data/img/add_yeast.png");
	public static Image addMisc = createImage("data/img/add_misc.png");
	public static Image toolsIcon = createImage("data/img/icons8-full-tool-storage-box-48.png");
	public static Image importIcon = createImage("data/img/icons8-import-48.png");
	public static Image boilIcon = createImage("data/img/icons8-boiling-48.png");
	public static Image mashIcon = createImage("data/img/icons8-mash-in.png");
	public static Image mashInfusionIcon = createImage("data/img/icons8-mash-infusion.png");
	public static Image heatIcon = createImage("data/img/icons8-heating-48.png");
	public static Image coolIcon = createImage("data/img/icons8-cooling-48.png");
	public static Image splitIcon = createImage("data/img/icons8-split-48.png");
	public static Image combineIcon = createImage("data/img/icons8-merge-48.png");
	public static Image packageIcon = createImage("data/img/icons8-package-48.png");
	public static Image standIcon = createImage("data/img/icons8-sleep-mode-48.png");
	public static Image diluteIcon = Icons.addWater;
	public static Image fermentIcon = createImage("data/img/icons8-glass-jar-48.png");
	public static Image batchSpargeIcon = createImage("data/img/icons8-batch-sparge.png");
	public static Image lauterIcon = createImage("data/img/icons8-lauter.png");
	public static Image csvIcon = createImage("data/img/icons8-csv-48.png");
	public static Image xmlIcon = createImage("data/img/icons8-xml-file-48.png");
	public static Image importCsv = createImage("data/img/icons8-import-csv-48.png");
	public static Image importXml = createImage("data/img/import_xml.png");
	public static Image exportCsv = createImage("data/img/icons8-export-csv-48.png");
	public static Image waterBuilderIcon = createImage("data/img/water_builder.png");
	public static Image acidifierIcon = createImage("data/img/acidifier.png");
	public static Image temperatureIcon = createImage("data/img/icons8-temperature-48.png");
	public static Image graphIcon = createImage("data/img/icons8-graph-48.png");
	public static Image gitIcon = createImage("data/img/icons8-git-48.png");
	public static Image infoIcon = createImage("data/img/icons8-information-48.png");

	public static void init()
	{
		// eh
	}

	private static Image createImage(String s)
	{
		try
		{
			return new Image(new FileInputStream(s));
		}
		catch (FileNotFoundException e)
		{
			throw new BrewdayException(e);
		}
	}

}
