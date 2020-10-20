
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

import java.io.File;
import java.util.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.importexport.csv.BatchesCsvParser;
import mclachlan.brewday.math.Quantity;
import org.tbee.javafx.scene.layout.fxml.MigPane;

import static mclachlan.brewday.StringUtils.getUiString;

class ImportBatchesCsvDialog extends Dialog<BitSet>
{
	private final BitSet output = new BitSet();
	private Map<Class<?>, Map<String, V2DataObject>> objs;

	public ImportBatchesCsvDialog(List<File> files) throws Exception
	{
		Scene scene = this.getDialogPane().getScene();
		JfxUi.styleScene(scene);
		Stage stage = (Stage)scene.getWindow();
		stage.getIcons().add(JfxUi.importCsv);

		ButtonType cancelButtonType = new ButtonType(
			getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(getUiString("tools.import.beersmith.batches.csv"));

		MigPane prepareImport = new MigPane();
		prepareImport.setPrefSize(550, 400);

		prepareImport.add(new Label(getUiString("tools.import.settings")), "span, wrap");
		TextArea details = new TextArea(getUiString("tools.import.beersmith.batches.csv.details"));
		details.setWrapText(true);
		details.setEditable(false);
		prepareImport.add(details, "span, wrap");

		ComboBox<BatchesCsvParser.CsvFormat> csvFormat = new ComboBox<>(
			FXCollections.observableArrayList(
				BatchesCsvParser.CsvFormat.EXCEL,
				BatchesCsvParser.CsvFormat.RFC_4180));
		ComboBox<Quantity.Unit> volumeUnit = new ComboBox<>(
			FXCollections.observableArrayList(
				Quantity.Unit.MILLILITRES,
				Quantity.Unit.LITRES,
				Quantity.Unit.US_FLUID_OUNCE,
				Quantity.Unit.US_GALLON));
		ComboBox<Quantity.Unit> densityUnit = new ComboBox<>(
			FXCollections.observableArrayList(
				Quantity.Unit.SPECIFIC_GRAVITY,
				Quantity.Unit.GU,
				Quantity.Unit.PLATO));

		csvFormat.getSelectionModel().select(BatchesCsvParser.CsvFormat.EXCEL);
		volumeUnit.getSelectionModel().select(Quantity.Unit.LITRES);
		densityUnit.getSelectionModel().select(Quantity.Unit.SPECIFIC_GRAVITY);

		prepareImport.add(new Label(StringUtils.getUiString("tools.import.beersmith.batches.csv.format")));
		prepareImport.add(csvFormat, "wrap");
		prepareImport.add(new Label(StringUtils.getUiString("tools.import.beersmith.batches.csv.volume.unit")));
		prepareImport.add(volumeUnit, "wrap");
		prepareImport.add(new Label(StringUtils.getUiString("tools.import.beersmith.batches.csv.density.unit")));
		prepareImport.add(densityUnit, "wrap");

		Button parse = new Button(getUiString("tools.import.parse"));
		prepareImport.add(new Label(), "wrap");
		prepareImport.add(parse);

		this.getDialogPane().setContent(prepareImport);

		// -----

		parse.setOnAction(event ->
		{
			try
			{
				objs = new BatchesCsvParser().parse(
					files,
					csvFormat.getSelectionModel().getSelectedItem(),
					volumeUnit.getSelectionModel().getSelectedItem(),
					densityUnit.getSelectionModel().getSelectedItem());
				setToImportOptions();
			}
			catch (Exception e)
			{
				throw new BrewdayException(e);
			}
		});


		final Button cancelButton = (Button)this.getDialogPane().lookupButton(cancelButtonType);
		cancelButton.addEventFilter(ActionEvent.ACTION, event -> output.clear());
	}

	/*-------------------------------------------------------------------------*/
	protected MigPane setToImportOptions()
	{
		MigPane importContent = new MigPane();

		importContent.add(new Label(getUiString("tools.import.imported")), "span, wrap");

		importContent.add(new Label(), "wrap");

		Database db = Database.getInstance();
		addCheckBoxs(importContent, output, ImportPane.Bit.BATCH_NEW, ImportPane.Bit.BATCH_UDPATE, objs.get(Batch.class), db.getBatches(), "tools.import.imported.batch");

		importContent.add(new Label(), "wrap");

		importContent.add(new Label(getUiString("tools.import.push.ok")), "span, wrap");
		importContent.add(new Label(getUiString("tools.import.then.save")), "span, wrap");

		this.getDialogPane().setContent(importContent);

		ButtonType okButtonType = new ButtonType(
			getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		this.getDialogPane().getButtonTypes().add(okButtonType);

		return importContent;
	}

	/*-------------------------------------------------------------------------*/
	public BitSet getOutput()
	{
		return output;
	}

	public Map<Class<?>, Map<String, V2DataObject>> getImportedObjs()
	{
		return objs;
	}

	private void addCheckBoxs(
		MigPane pane,
		BitSet bitSet,
		ImportPane.Bit newBit,
		ImportPane.Bit updateBit,
		Map<String, V2DataObject> v2DataObjects,
		Map<String, ?> dbObjs,
		String uiLabelPrefix)
	{
		if (v2DataObjects.size() > 0)
		{
			int newItems = 0;
			int dupes = 0;

			for (V2DataObject obj : v2DataObjects.values())
			{
				if (dbObjs.containsKey(obj.getName()))
				{
					dupes++;
				}
				else
				{
					newItems++;
				}
			}

			CheckBox newCheckBox = new CheckBox(getUiString(uiLabelPrefix + ".new", newItems));
			CheckBox dupeCheckBox = new CheckBox(getUiString(uiLabelPrefix + ".update", dupes));

			pane.add(newCheckBox);
			pane.add(dupeCheckBox, "wrap");

			newCheckBox.selectedProperty().addListener((observable, oldValue, newValue) ->
			{
				bitSet.set(newBit.ordinal(), newValue);
			});
			dupeCheckBox.selectedProperty().addListener((observable, oldValue, newValue) ->
			{
				bitSet.set(updateBit.ordinal(), newValue);
			});

			newCheckBox.setSelected(newItems > 0);
			dupeCheckBox.setSelected(dupes > 0);
		}
	}
}