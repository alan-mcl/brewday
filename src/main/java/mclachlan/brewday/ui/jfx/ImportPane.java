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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;
import org.tbee.javafx.scene.layout.fxml.MigPane;

import static mclachlan.brewday.StringUtils.getUiString;
import static mclachlan.brewday.ui.jfx.ImportPane.Bit.*;

/**
 *
 */
public class ImportPane extends MigPane
{
	private final TrackDirty parent;

	public ImportPane(TrackDirty parent)
	{
		this.parent = parent;

		Button importBeerXml = new Button(
			getUiString("tools.import.beerxml"),
			JfxUi.getImageView(Icons.importXml, 32));
		Label importBeerXmlLabel = new Label(StringUtils.getUiString("tools.import.beerxml.label"));

		this.add(importBeerXml, "wrap");
		this.add(importBeerXmlLabel, "wrap");

		Button importBatchesCsv = new Button(
			getUiString("tools.import.batches.csv"),
			JfxUi.getImageView(Icons.importCsv, 32));
		Label importBatchesCsvLabel = new Label(StringUtils.getUiString("tools.import.batches.csv.label"));

		this.add(importBatchesCsv, "wrap");
		this.add(importBatchesCsvLabel, "wrap");

		Button importBrewday = new Button(
			getUiString("tools.import.brewday"),
			JfxUi.getImageView(Icons.brewdayIcon, 32));
		Label importBrewdayLabel = new Label(StringUtils.getUiString("tools.import.brewday.label"));

		this.add(importBrewday, "wrap");
		this.add(importBrewdayLabel, "wrap");


//		Button importInventoryCsv = new Button(
//			getUiString("tools.import.inventory.csv"),
//			JfxUi.getImageView(Icons.importCsv, 32));
//		Label importInventoryCsvLabel = new Label(StringUtils.getUiString("tools.import.inventory.csv.label"));

//		this.add(importInventoryCsv, "wrap");
//		this.add(importInventoryCsvLabel, "wrap");

		// ----

		importBeerXml.setOnAction(event -> importBeerXml());
		importBatchesCsv.setOnAction(event -> importBatchesCsv());
		importBrewday.setOnAction(event -> importBrewday());
	}

	/*-------------------------------------------------------------------------*/
	private void importBrewday()
	{
		try
		{
			ImportBrewdayDialog dialog = new ImportBrewdayDialog();
			dialog.showAndWait();

			if (!dialog.getOutput().isEmpty())
			{
				importData(dialog.getImportedObjs(), dialog.getOutput());
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void importBatchesCsv()
	{
		try
		{
			ImportBatchesCsvDialog dialog = new ImportBatchesCsvDialog();
			dialog.showAndWait();

			if (!dialog.getOutput().isEmpty())
			{
				importData(dialog.getImportedObjs(), dialog.getOutput());
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void importBeerXml()
	{
		try
		{
			ImportBeerXmlDialog dialog = new ImportBeerXmlDialog();
			dialog.showAndWait();

			if (!dialog.getOutput().isEmpty())
			{
				importData(dialog.getImportedObjs(), dialog.getOutput());
			}
		}
		catch (Exception e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void importData(
		Map<Class<?>, Map<String, V2DataObject>> objs,
		BitSet options)
	{
		Database db = Database.getInstance();

		importData(objs.get(Water.class), db.getWaters(), JfxUi.WATER, options.get(WATER_NEW.ordinal()), options.get(WATER_UPDATE.ordinal()));
		importData(objs.get(Fermentable.class), db.getFermentables(), JfxUi.FERMENTABLES, options.get(FERMENTABLE_NEW.ordinal()), options.get(FERMENTABLE_UPDATE.ordinal()));
		importData(objs.get(Hop.class), db.getHops(), JfxUi.HOPS, options.get(HOPS_NEW.ordinal()), options.get(HOPS_UPDATE.ordinal()));
		importData(objs.get(Yeast.class), db.getYeasts(), JfxUi.YEAST, options.get(YEASTS_NEW.ordinal()), options.get(YEASTS_UPDATE.ordinal()));
		importData(objs.get(Misc.class), db.getMiscs(), JfxUi.MISC, options.get(MISC_NEW.ordinal()), options.get(MISC_UPDATE.ordinal()));
		importData(objs.get(Style.class), db.getStyles(), JfxUi.STYLES, options.get(STYLE_NEW.ordinal()), options.get(STYLE_UPDATE.ordinal()));
		importData(objs.get(EquipmentProfile.class), db.getEquipmentProfiles(), JfxUi.EQUIPMENT_PROFILES, options.get(EQUIPMENT_NEW.ordinal()), options.get(EQUIPMENT_UPDATE.ordinal()));
		importData(objs.get(Recipe.class), db.getRecipes(), JfxUi.RECIPES, options.get(RECIPE_NEW.ordinal()), options.get(RECIPE_UPDATE.ordinal()));
		importData(objs.get(Batch.class), db.getBatches(), JfxUi.BATCHES, options.get(BATCH_NEW.ordinal()), options.get(BATCH_UDPATE.ordinal()));
	}

	/*-------------------------------------------------------------------------*/
	private void importData(
		Map<String, V2DataObject> imported,
		Map map,
		String dirtyFlag,
		boolean importNew,
		boolean importDupes)
	{
		if (imported != null && imported.size() > 0)
		{
			boolean dirty = false;

			for (String name : imported.keySet())
			{
				if (map.containsKey(name) && importDupes)
				{
					if (importDupes)
					{
						map.put(name, imported.get(name));
						dirty = true;
					}
				}
				else
				{
					if (importNew)
					{
						map.put(name, imported.get(name));
						dirty = true;
					}
				}

			}

			if (dirty)
			{
				parent.setDirty(dirtyFlag);
				parent.setDirty(imported.values().toArray());
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	enum Bit
	{
		WATER_NEW, WATER_UPDATE, FERMENTABLE_NEW, FERMENTABLE_UPDATE, HOPS_NEW,
		HOPS_UPDATE, YEASTS_NEW, YEASTS_UPDATE, MISC_NEW, MISC_UPDATE,
		STYLE_NEW, STYLE_UPDATE, EQUIPMENT_NEW, EQUIPMENT_UPDATE, RECIPE_NEW,
		RECIPE_UPDATE, BATCH_NEW, BATCH_UDPATE, PROCESS_TEMPLATE_NEW,
		PROCESS_TEMPLATE_UPDATE, WATER_PARAMETERS_NEW, WATER_PARAMETERS_UPDATE
	}

	/*-------------------------------------------------------------------------*/
	static class ProgressBarDialog extends Dialog<Boolean>
	{
		ProgressBar progressBar;

		public ProgressBarDialog(Image icon, String title)
		{
			Scene scene = this.getDialogPane().getScene();
			JfxUi.styleScene(scene);
			Stage stage = (Stage)scene.getWindow();
			stage.getIcons().add(icon);

			// dummy cancel button so that we can close
			getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);

			this.setTitle(title);

			MigPane content = new MigPane();

			progressBar = new ProgressBar(0);

			progressBar.setPrefSize(200, 20);

			content.add(progressBar);

			this.getDialogPane().setContent(content);
		}
	}

}
