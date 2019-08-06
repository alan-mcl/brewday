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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.*;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.batch.BatchVolumeEstimate;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.BeerVolume;
import mclachlan.brewday.process.MashVolume;
import mclachlan.brewday.process.WortVolume;

/**
 *
 */
public class BatchMeasurementsPanel extends JPanel
{
	private JTable table;
	private JTextArea batchAnalysis;
	private BatchMeasurementsTableModel model;
	private int dirtyFlag;

	public BatchMeasurementsPanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new BorderLayout());

		model = new BatchMeasurementsTableModel();
		table = new JTable(model);
		table.setFillsViewportHeight(true);
		table.setPreferredScrollableViewportSize(new Dimension(550, 700));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getColumnModel().getColumn(0).setPreferredWidth(200);
		table.getColumnModel().getColumn(1).setPreferredWidth(50);
		table.getColumnModel().getColumn(2).setPreferredWidth(100);
		table.getColumnModel().getColumn(3).setPreferredWidth(100);
		table.getColumnModel().getColumn(4).setPreferredWidth(100);

		this.add(new JScrollPane(table), BorderLayout.CENTER);

		batchAnalysis = new JTextArea(20,20);
		batchAnalysis.setWrapStyleWord(true);
		batchAnalysis.setLineWrap(true);
		batchAnalysis.setEditable(false);

		this.add(batchAnalysis, BorderLayout.EAST);

		refresh(null);
	}

	public void refresh(Batch batch)
	{
		model.data.clear();

		if (batch != null)
		{
			List<BatchVolumeEstimate> estimates = Brewday.getInstance().getBatchVolumeEstimates(batch);

			model.data.addAll(estimates);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static String formatQuantity(Quantity quantity)
	{
		if (quantity == null)
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
	public static class BatchMeasurementsTableModel implements TableModel
	{
		private List<BatchVolumeEstimate> data;

		public BatchMeasurementsTableModel()
		{
			data = new ArrayList<>();
		}

		@Override
		public int getRowCount()
		{
			return data.size();
		}

		@Override
		public int getColumnCount()
		{
			return 5;
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0: return StringUtils.getUiString(BatchVolumeEstimate.MEASUREMENTS_VOLUME);
				case 1: return StringUtils.getUiString("batch.measurements.volume.type");
				case 2: return StringUtils.getUiString("batch.measurements.metric");
				case 3: return StringUtils.getUiString("batch.measurements.estimate");
				case 4: return StringUtils.getUiString("batch.measurements.measurement");
				default: throw new BrewdayException("Invalid column ["+columnIndex+"]");
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			// only the "measurement" column is editable
			return columnIndex==4;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			BatchVolumeEstimate cur = data.get(rowIndex);

			switch (columnIndex)
			{
				case 0: return cur.getEstimateVolume().getName();
				case 1:
					if (cur.getEstimateVolume() instanceof MashVolume)
					{
						return StringUtils.getUiString("batch.measurements.mash");
					}
					else if (cur.getEstimateVolume() instanceof WortVolume)
					{
						return StringUtils.getUiString("batch.measurements.wort");
					}
					else if (cur.getEstimateVolume() instanceof BeerVolume)
					{
						return StringUtils.getUiString("batch.measurements.beer");
					}
					else
					{
						throw new BrewdayException("Invalid :"+cur.getEstimateVolume());
					}
				case 2: return StringUtils.getUiString(cur.getMetric());
				case 3: return formatQuantity(cur.getEstimated());
				case 4: return formatQuantity(cur.getMeasured());
				default: throw new BrewdayException("Invalid column ["+columnIndex+"]");
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			if (columnIndex == 4)
			{
				BatchVolumeEstimate estimate = this.data.get(rowIndex);

				String quantityString = (String)aValue;

				Quantity.Unit hint;
				if (BatchVolumeEstimate.MEASUREMENTS_VOLUME.equals(estimate.getMetric()))
				{
					hint = Quantity.Unit.LITRES;
				}
				else if (BatchVolumeEstimate.MEASUREMENTS_TEMPERATURE.equals(estimate.getMetric()))
				{
					hint = Quantity.Unit.CELSIUS;
				}
				else if (BatchVolumeEstimate.MEASUREMENTS_DENSITY.equals(estimate.getMetric()))
				{
					hint = Quantity.Unit.SPECIFIC_GRAVITY;
				}
				else if (BatchVolumeEstimate.MEASUREMENTS_COLOUR.equals(estimate.getMetric()))
				{
					hint = Quantity.Unit.SRM;
				}
				else
				{
					hint = null;
				}

				estimate.setMeasured(Brewday.getInstance().getQuantity(quantityString, hint));
			}
		}

		@Override
		public void addTableModelListener(TableModelListener l)
		{

		}

		@Override
		public void removeTableModelListener(TableModelListener l)
		{

		}
	}
}
