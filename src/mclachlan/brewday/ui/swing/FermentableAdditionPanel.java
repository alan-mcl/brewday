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
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.FermentableAdditionList;

/**
 *
 */
public class FermentableAdditionPanel extends JPanel implements ActionListener, ChangeListener
{
	private JTextField name;
	private JTable fermentablesAdditionTable;
	private FermentableAdditionTableModel fermentableAdditionTableModel;
	private JButton add, remove, increaseAmount, decreaseAmount;
	private Recipe recipe;
	private FermentableAdditionList ingredientAddition;

	public FermentableAdditionPanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		name = new JTextField(20);
		name.setEditable(false);

		fermentableAdditionTableModel = new FermentableAdditionTableModel();
		fermentablesAdditionTable = new JTable(fermentableAdditionTableModel);

		fermentablesAdditionTable.setFillsViewportHeight(true);
		fermentablesAdditionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.add(new JScrollPane(fermentablesAdditionTable));

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

		this.add(buttons);
	}

	public void refresh(FermentableAdditionList ingredientAddition, Recipe recipe)
	{
		this.ingredientAddition = ingredientAddition;
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
			}
		}
	}

	protected void tableRepaint()
	{
		if (this.fermentableAdditionTableModel.data.size() > 0)
		{
			Collections.sort(this.fermentableAdditionTableModel.data, new Comparator<FermentableAddition>()
			{
				@Override
				public int compare(FermentableAddition o1, FermentableAddition o2)
				{
					// desc order of weight
					return (int)(o2.getWeight() - o1.getWeight());
				}
			});

			this.fermentablesAdditionTable.setRowSelectionInterval(0, 0);
		}

		fermentableAdditionTableModel.fireTableDataChanged();
		fermentablesAdditionTable.repaint();

		SwingUi.instance.refreshComputedVolumes();
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{

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
			tableRepaint();
		}

		public void clear()
		{
			this.data.clear();
			tableRepaint();
		}

		public void remove(int selectedRow)
		{
			this.data.remove(selectedRow);
			tableRepaint();
		}
	}
}
