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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.process.Recipe;
import mclachlan.brewday.recipe.AdditionSchedule;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.HopAdditionList;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class HopAdditionPanel extends JPanel implements ActionListener, ChangeListener
{
	private JTextField name;
	private JSpinner time;
	private JTable hopAdditionTable;
	private HopAdditionTableModel hopAdditionTableModel;
	private JButton add, remove, increaseAmount, decreaseAmount;
	private Recipe recipe;
	private HopAdditionList ingredientAddition;
	private AdditionSchedule schedule;

	public HopAdditionPanel()
	{
		setLayout(new MigLayout());

		name = new JTextField(20);
		name.setEditable(false);

		time = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1D));
		time.addChangeListener(this);

		JPanel topPanel = new JPanel(new MigLayout());
		topPanel.add(new JLabel("Name:"));
		topPanel.add(name, "wrap");

		topPanel.add(new JLabel("Time (min):"));
		topPanel.add(time, "wrap");

		this.add(topPanel, "wrap");

		hopAdditionTableModel = new HopAdditionTableModel();
		hopAdditionTable = new JTable(hopAdditionTableModel);
		hopAdditionTable.setFillsViewportHeight(true);
		hopAdditionTable.setAutoCreateRowSorter(true);
		hopAdditionTable.setPreferredScrollableViewportSize(new Dimension(400, 200));
		hopAdditionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.add(new JScrollPane(hopAdditionTable), "span, wrap");

		JPanel buttons = new JPanel();

		add = new JButton("Add");
		add.addActionListener(this);

		remove = new JButton("Remove");
		remove.addActionListener(this);

		increaseAmount = new JButton("+1g");
		increaseAmount.addActionListener(this);

		decreaseAmount = new JButton("-1g");
		decreaseAmount.addActionListener(this);

		buttons.add(add);
		buttons.add(remove);
		buttons.add(increaseAmount);
		buttons.add(decreaseAmount);

		this.add(buttons, "wrap");
	}

	public void refresh(AdditionSchedule schedule, Recipe recipe)
	{
		this.schedule = schedule;
		this.ingredientAddition = (HopAdditionList)recipe.getVolumes().getVolume(schedule.getIngredientAddition());
		this.recipe = recipe;
		this.name.setText(ingredientAddition.getName());
		this.time.setValue(schedule.getTime());
		this.hopAdditionTableModel.clear();

		List<HopAddition> hopAdditions = ingredientAddition.getIngredients();
		if (hopAdditions.size() > 0)
		{
			for (HopAddition fa : hopAdditions)
			{
				this.hopAdditionTableModel.add(fa);
			}

			tableRepaint();
		}

		this.revalidate();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == add)
		{
			HopAdditionDialog dialog = new HopAdditionDialog(SwingUi.instance, "Add Hop", recipe);
			HopAddition fa = dialog.getResult();

			if (fa != null)
			{
				hopAdditionTableModel.add(fa);
				ingredientAddition.getIngredients().add(fa);

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == remove)
		{
			int selectedRow = hopAdditionTable.getSelectedRow();

			if (selectedRow > -1  && hopAdditionTableModel.data.size() > selectedRow)
			{
				HopAddition fa = hopAdditionTableModel.data.get(selectedRow);
				ingredientAddition.getIngredients().remove(fa);

				hopAdditionTableModel.remove(selectedRow);

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == increaseAmount)
		{
			int selectedRow = hopAdditionTable.getSelectedRow();

			if (selectedRow > -1 && hopAdditionTableModel.data.size() > selectedRow)
			{
				HopAddition fa = hopAdditionTableModel.data.get(selectedRow);
				fa.setWeight(fa.getWeight() + 1);

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == decreaseAmount)
		{
			int selectedRow = hopAdditionTable.getSelectedRow();

			if (selectedRow > -1 && hopAdditionTableModel.data.size() > selectedRow)
			{
				HopAddition fa = hopAdditionTableModel.data.get(selectedRow);
				double weight = fa.getWeight() - 1;
				fa.setWeight(Math.max(weight, 0));

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
	}

	protected void tableRepaint()
	{
		hopAdditionTableModel.fireTableDataChanged();
		hopAdditionTable.repaint();
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == time)
		{
			this.schedule.setTime((Double)time.getValue());
			SwingUi.instance.refreshProcessSteps();
		}
	}

	/*-------------------------------------------------------------------------*/
	public class HopAdditionTableModel extends AbstractTableModel
	{
		private List<HopAddition> data;

		public HopAdditionTableModel()
		{
			this.data = new ArrayList<HopAddition>();
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
				case 0: return "Amount";
				case 1: return "Hop";
				case 2: return "AA%";
				case 3: return "Type";
				default: throw new BrewdayException("Invalid "+columnIndex);
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
			HopAddition hopAddition = data.get(rowIndex);

			switch (columnIndex)
			{
				case 0: return String.format("%.2fg", hopAddition.getWeight());
				case 1: return hopAddition.getHop().getName();
				case 2: return String.format("%.1f%%", hopAddition.getHop().getAlphaAcid() * 100);
				case 3: return hopAddition.getHop().getType();
				default: throw new BrewdayException("Invalid "+columnIndex);
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

		public void add(HopAddition fa)
		{
			this.data.add(fa);
//			tableRepaint();
		}

		public void clear()
		{
			this.data.clear();
//			tableRepaint();
		}

		public void remove(int selectedRow)
		{
			this.data.remove(selectedRow);
//			tableRepaint();
		}
	}
}
