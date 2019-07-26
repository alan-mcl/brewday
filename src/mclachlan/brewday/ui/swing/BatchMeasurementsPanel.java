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

import java.util.*;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.MashVolume;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class BatchMeasurementsPanel extends JPanel
{
	private JTable table;
	private BatchMeasurementsTableModel model;
	private int dirtyFlag;

	public BatchMeasurementsPanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		model = new BatchMeasurementsTableModel();
		table = new JTable(model);

		this.add(new JScrollPane(table));

		refresh(null);
	}

	public void refresh(Batch batch)
	{
		model.data.clear();

		if (batch != null)
		{
			Recipe recipe = Database.getInstance().getRecipes().get(batch.getRecipe());

			for (Map.Entry<String, Volume> e : recipe.getVolumes().getVolumes().entrySet())
			{
				Volume estVol = e.getValue();

				Volume measuredVol = batch.getActualVolumes().getVolumes().get(e.getKey());

				if (estVol instanceof MashVolume)
				{
					if (measuredVol == null)
					{
						measuredVol = new MashVolume();
					}

					model.data.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							"batch.measurements.temperature",
							((MashVolume)estVol).getTemperature(),
							((MashVolume)measuredVol).getTemperature()));
				}
			}
		}
	}

	private class BatchVolumeEstimate
	{
		private Volume estimateVolume;
		private Volume measuredVolume;
		private String metric;
		private Object estimated;
		private Object measured;

		public BatchVolumeEstimate(
			Volume estimateVolume,
			Volume measuredVolume,
			String metric,
			Object estimated,
			Object measured)
		{
			this.estimateVolume = estimateVolume;
			this.measuredVolume = measuredVolume;
			this.metric = metric;
			this.estimated = estimated;
			this.measured = measured;
		}
	}

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
			return 4;
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0: return StringUtils.getUiString("batch.measurements.volume");
				case 1: return StringUtils.getUiString("batch.measurements.metric");
				case 2: return StringUtils.getUiString("batch.measurements.estimate");
				case 3: return StringUtils.getUiString("batch.measurements.measurement");
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
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			BatchVolumeEstimate cur = data.get(rowIndex);

			switch (columnIndex)
			{
				case 0: return cur.estimateVolume.getName();
				case 1: return StringUtils.getUiString(cur.metric);
				case 2: return ""+cur.estimated;
				case 3: return ""+cur.measured;
				default: throw new BrewdayException("Invalid column ["+columnIndex+"]");
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{

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
