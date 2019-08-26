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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.batch.BatchVolumeEstimate;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.Volume;

/**
 *
 */
public class BatchMeasurementsPanel extends JPanel implements ActionListener
{
	private JCheckBox keyOnly;
	private JTextArea batchAnalysis;

	private JTable table;
	private BatchMeasurementsTableModel model;
	private TableRowSorter rowSorter;

	private int dirtyFlag;
	private Batch batch;

	/*-------------------------------------------------------------------------*/
	public BatchMeasurementsPanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new BorderLayout());

		JPanel tablePanel = new JPanel(new BorderLayout());

		JPanel topPanel = new JPanel();
		keyOnly = new JCheckBox(StringUtils.getUiString("batch.measurements.key.only"));
		keyOnly.setSelected(true);
		keyOnly.addActionListener(this);

		topPanel.add(keyOnly);

		model = new BatchMeasurementsTableModel(dirtyFlag);
		table = new JTable(model);
		table.setFillsViewportHeight(true);
		table.setPreferredScrollableViewportSize(new Dimension(550, 400));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getColumnModel().getColumn(0).setPreferredWidth(200);
		table.getColumnModel().getColumn(1).setPreferredWidth(50);
		table.getColumnModel().getColumn(2).setPreferredWidth(100);
		table.getColumnModel().getColumn(3).setPreferredWidth(100);
		table.getColumnModel().getColumn(4).setPreferredWidth(100);
		table.setAutoCreateRowSorter(true);
		rowSorter = (TableRowSorter)table.getRowSorter();
		rowSorter.setRowFilter(new BatchMeasurementsRowFilter());

		tablePanel.add(topPanel, BorderLayout.NORTH);
		tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);

		this.add(tablePanel, BorderLayout.CENTER);

		batchAnalysis = new JTextArea(20,40);
		batchAnalysis.setWrapStyleWord(true);
		batchAnalysis.setLineWrap(true);
		batchAnalysis.setEditable(false);
		batchAnalysis.setBorder(BorderFactory.createTitledBorder(StringUtils.getUiString("batch.analysis")));

		this.add(batchAnalysis, BorderLayout.EAST);

		refresh(null);
	}

	/*-------------------------------------------------------------------------*/

	public void refresh(Batch batch)
	{
		this.batch = batch;

		refreshCurrent();
	}

	/*-------------------------------------------------------------------------*/
	private void refreshCurrent()
	{
		model.data.clear();
		if (batch != null)
		{
			List<BatchVolumeEstimate> estimates = Brewday.getInstance().getBatchVolumeEstimates(batch);
			model.data.addAll(estimates);
			rowSorter.sort();

			StringBuilder sb = new StringBuilder();
			List<String> batchAnalysis = Brewday.getInstance().getBatchAnalysis(batch);

			for (String s : batchAnalysis)
			{
				sb.append(s+"\n");
			}

			this.batchAnalysis.setText(sb.toString());
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
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == keyOnly)
		{
			rowSorter.sort();
		}
	}

	/*-------------------------------------------------------------------------*/
	public class BatchMeasurementsTableModel extends AbstractTableModel
	{
		private List<BatchVolumeEstimate> data;
		private int dirtyFlag;

		public BatchMeasurementsTableModel(int dirtyFlag)
		{
			this.dirtyFlag = dirtyFlag;
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
					if (cur.getEstimateVolume().getType() == Volume.Type.MASH)
					{
						return StringUtils.getUiString("batch.measurements.mash");
					}
					else if (cur.getEstimateVolume().getType() == Volume.Type.WORT)
					{
						return StringUtils.getUiString("batch.measurements.wort");
					}
					else if (cur.getEstimateVolume().getType() == Volume.Type.BEER)
					{
						return StringUtils.getUiString("batch.measurements.beer");
					}
					else
					{
						throw new BrewdayException("Invalid :"+cur.getEstimateVolume());
					}
				case 2: return StringUtils.getUiString(cur.getMetric());
				case 3: return formatQuantity(cur.getEstimated());
				case 4:
					// only display measured quantities
					if (cur.getMeasured() != null && !cur.getMeasured().isEstimated())
					{
						return formatQuantity(cur.getMeasured());
					}
					else
					{
						return formatQuantity(null);
					}
				default: throw new BrewdayException("Invalid column ["+columnIndex+"]");
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			if (columnIndex == 4)
			{
				SwingUi.instance.setDirty(dirtyFlag);

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

				estimate.setMeasured(Brewday.getInstance().parseQuantity(quantityString, hint));

				refreshCurrent();
			}
		}
	}

	private class BatchMeasurementsRowFilter extends RowFilter<BatchMeasurementsTableModel, Integer>
	{
		@Override
		public boolean include(Entry<? extends BatchMeasurementsTableModel, ? extends Integer> entry)
		{
			if (keyOnly.isSelected())
			{
				BatchMeasurementsTableModel model = entry.getModel();
				BatchVolumeEstimate bve = model.data.get((entry.getIdentifier()));

				return bve.isKey();
			}
			else
			{
				return true;
			}
		}
	}
}
