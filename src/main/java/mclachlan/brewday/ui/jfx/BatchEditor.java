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
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.batch.BatchVolumeEstimate;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.*;
import org.tbee.javafx.scene.layout.MigPane;

import static mclachlan.brewday.StringUtils.getUiString;

/**
 *
 */
class BatchEditor extends MigPane
{
	private final TrackDirty parent;
	private boolean detectDirty = true;
	private TableView<BatchVolumeEstimate> table;
	private DatePicker batchDate;
	private ComboBox<String> batchRecipe;
	private TextArea analysis;
	private TextArea batchNotes;
	private CheckBox keyOnly;
	private FilteredList<BatchVolumeEstimate> filteredList;

	/*-------------------------------------------------------------------------*/
	public BatchEditor(
		final Batch batch,
		final TrackDirty parent)
	{
		super("gap 3");

		this.setPrefSize(1200, 750);

		this.parent = parent;

		MigPane detailsPane = getDetailsPane(batch);
		MigPane measurementsPane = getMeasurementsPane(batch);

		this.add(detailsPane);
		this.add(measurementsPane);

		refresh(batch);
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(Batch batch)
	{
		detectDirty = false;

		// recipes FK
		List<BatchVolumeEstimate> bves = Brewday.getInstance().getBatchVolumeEstimates(batch);
		filteredList = new FilteredList<>(FXCollections.observableList(bves));
		table.setItems(filteredList);

		// batch details
		batchDate.valueProperty().setValue(batch.getDate());
		batchRecipe.getSelectionModel().select(batch.getRecipe());
		batchNotes.setText(batch.getDescription());

		// batch analysis
		refreshBatchAnalysis(batch);

		// start filtered
		keyOnly.setSelected(true);

		detectDirty = true;
	}

	/*-------------------------------------------------------------------------*/
	private void refreshBatchAnalysis(Batch batch)
	{
		StringBuilder sb = new StringBuilder();
		List<String> batchAnalysis = Brewday.getInstance().getBatchAnalysis(batch);
		for (String s : batchAnalysis)
		{
			sb.append(s).append("\n");
		}
		analysis.setText(sb.toString());
	}

	/*-------------------------------------------------------------------------*/
	protected MigPane getMeasurementsPane(Batch batch)
	{
		table = new TableView<>();
		DirtyTableViewRowFactory<BatchVolumeEstimate> rowFactory = new DirtyTableViewRowFactory<>(table);
		table.setRowFactory(rowFactory);

		TableColumn<BatchVolumeEstimate, String> volCol = getStringPropertyValueCol("batch.measurements.volume", "volumeName");
		TableColumn<BatchVolumeEstimate, String> typeCol = getStringPropertyValueCol("batch.measurements.volume.type", "type");
		TableColumn<BatchVolumeEstimate, String> metricCol = getStringPropertyValueCol("batch.measurements.metric", "metric");
		TableColumn<BatchVolumeEstimate, String> estCol = getQuantityPropertyValueCol("batch.measurements.estimate", BatchVolumeEstimate::getEstimated, true);
		TableColumn<BatchVolumeEstimate, String> measCol = getQuantityPropertyValueCol("batch.measurements.measurement", BatchVolumeEstimate::getMeasured, false);

		// size these for the column headers to be readable
		estCol.setPrefWidth(90);
		measCol.setPrefWidth(120);

		// set the measurement column to be editable
		table.setEditable(true);
		volCol.setEditable(false);
		typeCol.setEditable(false);
		metricCol.setEditable(false);
		estCol.setEditable(false);
		measCol.setCellFactory(TextFieldTableCell.forTableColumn());

		table.getColumns().add(volCol);
		table.getColumns().add(typeCol);
		table.getColumns().add(metricCol);
		table.getColumns().add(estCol);
		table.getColumns().add(measCol);

		table.setPrefSize(600, 400);

		MigPane result = new MigPane();

		keyOnly = new CheckBox(StringUtils.getUiString("batch.measurements.key.only"));

		result.add(keyOnly, "wrap");
		result.add(table, "span");

		// -------

		keyOnly.selectedProperty().addListener((observable, oldValue, newValue) ->
			filteredList.setPredicate(batchVolumeEstimate -> !newValue || batchVolumeEstimate.isKey() == newValue));

		// table cell editor
		measCol.setOnEditCommit(event ->
		{
			BatchVolumeEstimate bve = event.getRowValue();
			Quantity quantity = parseMeasured(event.getNewValue(), bve);

			bve.setMeasured(quantity);

			if (detectDirty)
			{
				refreshBatchAnalysis(batch);

				rowFactory.setDirty(bve);
				parent.setDirty(batch);
			}
		});

		return result;
	}

	/*-------------------------------------------------------------------------*/
	protected MigPane getDetailsPane(Batch batch)
	{
		MigPane result = new MigPane();

		result.add(new Label(StringUtils.getUiString("batch.date")));
		batchDate = new DatePicker();
		result.add(batchDate, "wrap");

		result.add(new Label(StringUtils.getUiString("batch.recipe")));
		batchRecipe = new ComboBox<>();
		result.add(batchRecipe, "wrap");

		result.add(new Label(StringUtils.getUiString("batch.desc")), "wrap");
		batchNotes = new TextArea();
		batchNotes.setPrefHeight(200);
		batchNotes.setWrapText(true);
		result.add(batchNotes, "span, grow, wrap");

		analysis = new TextArea();
		analysis.setEditable(false);
		analysis.setPrefHeight(350);
		result.add(new Label(StringUtils.getUiString("batch.analysis")), "wrap");
		result.add(analysis, "span");

		// ------

		ArrayList<String> recipes = new ArrayList<>(Database.getInstance().getRecipes().keySet());
		recipes.sort(String::compareTo);
		batchRecipe.setItems(FXCollections.observableList(recipes));

		// -----

		batchDate.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null && !newValue.equals(oldValue))
			{
				batch.setDate(newValue);
				if (detectDirty) parent.setDirty(batch);
			}
		});

		batchRecipe.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null && !newValue.equals(oldValue))
			{
				batch.setRecipe(newValue);
				if (detectDirty) parent.setDirty(batch);
			}
		});

		batchNotes.textProperty().addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null && !newValue.equals(oldValue))
			{
				batch.setDescription(newValue);
				if (detectDirty) parent.setDirty(batch);
			}
		});

		return result;
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<BatchVolumeEstimate, String> getStringPropertyValueCol(
		String heading,
		String property)
	{
		TableColumn<BatchVolumeEstimate, String> col = new TableColumn<>(getUiString(heading));
		col.setCellValueFactory(new PropertyValueFactory<>(property));
		return col;
	}

	/*-------------------------------------------------------------------------*/
	protected TableColumn<BatchVolumeEstimate, String> getQuantityPropertyValueCol(
		String heading,
		Function<BatchVolumeEstimate, Quantity> getter,
		boolean displayEstimates)
	{
		TableColumn<BatchVolumeEstimate, String> col = new TableColumn<>(getUiString(heading));
		col.setCellValueFactory(param ->
		{
			Quantity quantity = getter.apply(param.getValue());
			return new SimpleObjectProperty<>(formatQuantity(quantity, displayEstimates));
		});

		return col;
	}

	/*-------------------------------------------------------------------------*/
	private static String formatQuantity(Quantity quantity, boolean displayEstimates)
	{
		if (quantity == null || (!displayEstimates && quantity.isEstimated()))
		{
			return StringUtils.getUiString("quantity.unknown");
		}
		else if (quantity instanceof TemperatureUnit)
		{
			return StringUtils.getUiString("quantity.celsius",
				((TemperatureUnit)quantity).get(Quantity.Unit.CELSIUS));
		}
		else if (quantity instanceof VolumeUnit)
		{
			return StringUtils.getUiString("quantity.litre",
				((VolumeUnit)quantity).get(Quantity.Unit.LITRES));
		}
		else if (quantity instanceof WeightUnit)
		{
			return StringUtils.getUiString("quantity.kilogram",
				((WeightUnit)quantity).get(Quantity.Unit.KILOGRAMS));
		}
		else if (quantity instanceof DensityUnit)
		{
			return StringUtils.getUiString("quantity.sg",
				((DensityUnit)quantity).get(Quantity.Unit.SPECIFIC_GRAVITY));
		}
		else if (quantity instanceof ColourUnit)
		{
			return StringUtils.getUiString("quantity.srm",
				((ColourUnit)quantity).get(Quantity.Unit.SRM));
		}
		else
		{
			throw new BrewdayException("Invalid quantity type:"+quantity);
		}
	}

	/*-------------------------------------------------------------------------*/
	private Quantity parseMeasured(String quantityString, BatchVolumeEstimate estimate)
	{
		Quantity.Unit hint = null;

		if (estimate.getMeasured() != null)
		{
			if (estimate.getMeasured().getType() == Quantity.Type.VOLUME)
			{
				hint = Quantity.Unit.LITRES;
			}
			else if (estimate.getMeasured().getType() == Quantity.Type.TEMPERATURE)
			{
				hint = Quantity.Unit.CELSIUS;
			}
			else if (estimate.getMeasured().getType() == Quantity.Type.FLUID_DENSITY)
			{
				hint = Quantity.Unit.SPECIFIC_GRAVITY;
			}
			else if (estimate.getMeasured().getType() == Quantity.Type.COLOUR)
			{
				hint = Quantity.Unit.SRM;
			}
		}

		return Brewday.getInstance().parseQuantity(quantityString, hint);
	}

}
