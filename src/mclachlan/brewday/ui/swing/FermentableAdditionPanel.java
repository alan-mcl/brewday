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
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.FermentableAdditionList;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class FermentableAdditionPanel extends JPanel implements ActionListener, ChangeListener
{
	private JTextField name;
	private JSpinner time;
	private JTable fermentablesAdditionTable;
	private FermentableAdditionTableModel fermentableAdditionTableModel;
	private JButton add, remove, increaseAmount, decreaseAmount;
	private Recipe recipe;
	private FermentableAdditionList ingredientAddition;
	private AdditionSchedule schedule;

	public FermentableAdditionPanel()
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

		fermentableAdditionTableModel = new FermentableAdditionTableModel();
		fermentablesAdditionTable = new JTable(fermentableAdditionTableModel);
		fermentablesAdditionTable.setFillsViewportHeight(true);
		fermentablesAdditionTable.setAutoCreateRowSorter(true);
		fermentablesAdditionTable.setPreferredScrollableViewportSize(new Dimension(400, 200));
		fermentablesAdditionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.add(new JScrollPane(fermentablesAdditionTable), "span, wrap");

		JPanel buttons = new JPanel();

		add = new JButton("Add");
		add.addActionListener(this);

		remove = new JButton("Remove");
		remove.addActionListener(this);

		increaseAmount = new JButton("+250g");
		increaseAmount.addActionListener(this);

		decreaseAmount = new JButton("-250g");
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
		this.ingredientAddition = (FermentableAdditionList)recipe.getVolumes().getVolume(schedule.getIngredientAddition());;
		this.recipe = recipe;
		this.name.setText(ingredientAddition.getName());
		this.fermentableAdditionTableModel.clear();

		List<FermentableAddition> fermentableAdditions = ingredientAddition.getIngredients();
		if (fermentableAdditions.size() > 0)
		{
			for (FermentableAddition fa : fermentableAdditions)
			{
				this.fermentableAdditionTableModel.add(fa);
			}
		}

		this.revalidate();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == add)
		{
			FermentableAdditionDialog dialog = new FermentableAdditionDialog(SwingUi.instance, "Add Fermentable", recipe);
			FermentableAddition fa = dialog.getResult();

			if (fa != null)
			{
				fermentableAdditionTableModel.add(fa);
				ingredientAddition.getIngredients().add(fa);

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == remove)
		{
			int selectedRow = fermentablesAdditionTable.getSelectedRow();

			if (selectedRow > -1  && fermentableAdditionTableModel.data.size() > selectedRow)
			{
				FermentableAddition fa = fermentableAdditionTableModel.data.get(selectedRow);
				ingredientAddition.getIngredients().remove(fa);
				fermentableAdditionTableModel.remove(selectedRow);

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == increaseAmount)
		{
			int selectedRow = fermentablesAdditionTable.getSelectedRow();

			if (selectedRow > -1 && fermentableAdditionTableModel.data.size() > selectedRow)
			{
				FermentableAddition fa = fermentableAdditionTableModel.data.get(selectedRow);
				fa.setWeight(fa.getWeight() + 250);

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == decreaseAmount)
		{
			int selectedRow = fermentablesAdditionTable.getSelectedRow();

			if (selectedRow > -1 && fermentableAdditionTableModel.data.size() > selectedRow)
			{
				FermentableAddition fa = fermentableAdditionTableModel.data.get(selectedRow);
				double weight = fa.getWeight() - 250;
				fa.setWeight(Math.max(weight, 0));

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
	}

	protected void tableRepaint()
	{
		fermentableAdditionTableModel.fireTableDataChanged();
		fermentablesAdditionTable.repaint();
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
	public class FermentableAdditionTableModel extends AbstractTableModel
	{
		private List<FermentableAddition> data;

		public FermentableAdditionTableModel()
		{
			this.data = new ArrayList<FermentableAddition>();
		}

		@Override
		public int getRowCount()
		{
			return data.size();
		}

		@Override
		public int getColumnCount()
		{
			return 3;
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0: return "Amount";
				case 1: return "Fermentable";
				case 2: return "Type";
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
			FermentableAddition fermentableAddition = data.get(rowIndex);

			switch (columnIndex)
			{
				case 0: return String.format("%.2fkg", fermentableAddition.getWeight() / 1000);
				case 1: return fermentableAddition.getFermentable().getName();
				case 2: return fermentableAddition.getFermentable().getType();
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

		public void add(FermentableAddition fa)
		{
			this.data.add(fa);
		}

		public void clear()
		{
			this.data.clear();
		}

		public void remove(int selectedRow)
		{
			this.data.remove(selectedRow);
		}
	}
}
