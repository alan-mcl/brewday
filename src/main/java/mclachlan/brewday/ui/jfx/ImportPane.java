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

import java.io.File;
import java.util.*;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.importexport.beerxml.BeerXmlParser;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;
import org.tbee.javafx.scene.layout.fxml.MigPane;

import static mclachlan.brewday.StringUtils.getUiString;

/**
 *
 */
public class ImportPane extends MigPane
{
	private TrackDirty parent;

	public ImportPane(TrackDirty parent)
	{
		this.parent = parent;
		Button importBeerXml = new Button(
			getUiString("tools.import.beerxml"),
			JfxUi.getImageView(JfxUi.importIcon, 32));

		this.add(importBeerXml, "wrap");

		// ----

		importBeerXml.setOnAction(event -> importBeerXml());
	}

	/*-------------------------------------------------------------------------*/
	private void importBeerXml()
	{
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(StringUtils.getUiString("tools.import.beerxml.title"));

		Settings settings = Database.getInstance().getSettings();
		String dir = settings.get(Settings.LAST_IMPORT_DIRECTORY);
		if (dir != null)
		{
			fileChooser.setInitialDirectory(new File(dir));
		}

		List<File> files = fileChooser.showOpenMultipleDialog(JfxUi.getInstance().getMainScene().getWindow());

		if (files != null)
		{
			String parent = files.get(0).getParent();
			if (parent != null)
			{
				settings.set(Settings.LAST_IMPORT_DIRECTORY, parent);
				Database.getInstance().saveSettings();
			}

			try
			{
				Map<Class<?>, Map<String, V2DataObject>> objs = new BeerXmlParser().parse(files);

				ImportDialog importDialog = new ImportDialog(objs);
				importDialog.showAndWait();

				if (importDialog.output)
				{
					importData(objs);
				}
			}
			catch (Exception e)
			{
				throw new BrewdayException(e);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private void importData(Map<Class<?>, Map<String, V2DataObject>> objs)
	{
		Database db = Database.getInstance();

		importData(objs.get(Water.class), db.getWaters(), JfxUi.WATER);
		importData(objs.get(Fermentable.class), db.getFermentables(), JfxUi.FERMENTABLES);
		importData(objs.get(Hop.class), db.getHops(), JfxUi.HOPS);
		importData(objs.get(Yeast.class), db.getYeasts(), JfxUi.YEAST);
		importData(objs.get(Misc.class), db.getMiscs(), JfxUi.MISC);
		importData(objs.get(Style.class), db.getStyles(), JfxUi.STYLES);
		importData(objs.get(EquipmentProfile.class), db.getEquipmentProfiles(), JfxUi.EQUIPMENT_PROFILES);
		importData(objs.get(Recipe.class), db.getRecipes(), JfxUi.RECIPES);
		importData(objs.get(Batch.class), db.getBatches(), JfxUi.BATCHES);
	}

	/*-------------------------------------------------------------------------*/
	private void importData(
		Map<String, V2DataObject> imported,
		Map map,
		String dirtyFlag)
	{
		if (imported.size() > 0)
		{
			for (String name : imported.keySet())
			{
				map.put(name, imported.get(name));
			}

			parent.setDirty(dirtyFlag);
			parent.setDirty(imported.values().toArray());
		}
	}

	/*-------------------------------------------------------------------------*/
	private static class ImportDialog extends Dialog<Boolean>
	{
		private boolean output = false;

		public ImportDialog(Map<Class<?>, Map<String, V2DataObject>> objs)
		{
			Scene scene = this.getDialogPane().getScene();
			JfxUi.styleScene(scene);
			Stage stage = (Stage)scene.getWindow();
			stage.getIcons().add(JfxUi.importIcon);

			ButtonType okButtonType = new ButtonType(
				getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
			ButtonType cancelButtonType = new ButtonType(
				getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
			this.getDialogPane().getButtonTypes().add(okButtonType);
			this.getDialogPane().getButtonTypes().add(cancelButtonType);

			this.setTitle(StringUtils.getUiString("tools.import.beerxml"));

			MigPane content = new MigPane();

			TextArea textArea = new TextArea();
			content.add(textArea, "span, wrap");

			content.add(new Label(StringUtils.getUiString("tools.import.beerxml.push.ok")), "wrap");
			content.add(new Label(StringUtils.getUiString("tools.import.beerxml.then.save")));

			StringBuilder sb = new StringBuilder();
			sb.append(StringUtils.getUiString("tools.import.beerxml.imported"));
			sb.append("\n");

			Database db = Database.getInstance();
			addText(sb, objs.get(Water.class), db.getWaters(), "tools.import.beerxml.imported.water");
			addText(sb, objs.get(Fermentable.class), db.getFermentables(), "tools.import.beerxml.imported.fermentable");
			addText(sb, objs.get(Hop.class), db.getHops(), "tools.import.beerxml.imported.hops");
			addText(sb, objs.get(Yeast.class), db.getYeasts(), "tools.import.beerxml.imported.yeast");
			addText(sb, objs.get(Misc.class), db.getMiscs(), "tools.import.beerxml.imported.misc");
			addText(sb, objs.get(Style.class), db.getStyles(), "tools.import.beerxml.imported.style");
			addText(sb, objs.get(EquipmentProfile.class), db.getEquipmentProfiles(), "tools.import.beerxml.imported.equipment");

			Map<String, V2DataObject> recipes = objs.get(Recipe.class);
			Map<String, V2DataObject> batches = objs.get(Batch.class);
			if (recipes.size() > 0)
			{
				int recipeDupes = 0;

				for (V2DataObject obj : recipes.values())
				{
					if (db.getRecipes().containsKey(obj.getName()))
					{
						recipeDupes++;
					}
				}

				int batchDupes = 0;

				for (V2DataObject obj : batches.values())
				{
					if (db.getBatches().containsKey(obj.getName()))
					{
						batchDupes++;
					}
				}

				sb.append(StringUtils.getUiString("tools.import.beerxml.imported.recipes", recipes.size(), recipes.size(), batches.size(), recipeDupes, batchDupes));
				sb.append("\n");
			}

			textArea.setText(sb.toString());

			this.getDialogPane().setContent(content);

			// -----

			final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
			btOk.addEventFilter(ActionEvent.ACTION, event -> output = true);
		}

		private void addText(
			StringBuilder sb,
			Map<String, V2DataObject> v2DataObjects,
			Map<String, ?> dbObjs,
			String key)
		{
			if (v2DataObjects.size() > 0)
			{
				int dupes = 0;

				for (V2DataObject obj : v2DataObjects.values())
				{
					if (dbObjs.containsKey(obj.getName()))
					{
						dupes++;
					}
				}

				sb.append(StringUtils.getUiString(key, v2DataObjects.size(), dupes));
				sb.append("\n");
			}
		}

	}
}
