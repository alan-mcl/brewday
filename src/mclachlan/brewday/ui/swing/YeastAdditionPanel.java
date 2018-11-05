/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the yeaste that it will be useful,
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
import mclachlan.brewday.recipe.RecipeLineItem;
import mclachlan.brewday.recipe.YeastAddition;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class YeastAdditionPanel extends JPanel implements ActionListener, ChangeListener
{
	private JTextField name;
	private JSpinner time;
	private JButton add, remove, increaseAmount, decreaseAmount;
	private Recipe recipe;

	public YeastAdditionPanel()
	{
		setLayout(new MigLayout());

		name = new JTextField(20);
		name.setEditable(false);

		time = new JSpinner(new SpinnerNumberModel(60, 0, 9999, 1D));
		time.addChangeListener(this);

		JPanel topPanel = new JPanel(new MigLayout());
		topPanel.add(new JLabel("Name:"));
		topPanel.add(name, "wrap");

		topPanel.add(new JLabel("Time (days):"));
		topPanel.add(time, "wrap");

		this.add(topPanel, "wrap");


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

	public void refresh(RecipeLineItem schedule, Recipe recipe)
	{
/*		this.schedule = schedule;
		this.ingredientAddition = (YeastAdditionList)recipe.getVolumes().getVolume(schedule.getIngredientAddition());
		this.recipe = recipe;
		this.name.setText(ingredientAddition.getName());
		this.time.setValue(schedule.getTime());
		this.yeastAdditionTableModel.clear();

		List<YeastAddition> yeastAdditions = ingredientAddition.getIngredients();
		if (yeastAdditions.size() > 0)
		{
			for (YeastAddition fa : yeastAdditions)
			{
				this.yeastAdditionTableModel.add(fa);
			}

			tableRepaint();
		}

		this.name.removeActionListener(this);
		this.time.removeChangeListener(this);

		this.name.setText(this.ingredientAddition.getName());
		this.time.setValue(schedule.getTime());

		this.name.addActionListener(this);
		this.time.addChangeListener(this);


		this.revalidate();*/
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
/*		if (e.getSource() == add)
		{
			YeastAdditionDialog dialog = new YeastAdditionDialog(SwingUi.instance, "Add Yeast", recipe);
			YeastAddition fa = dialog.getResult();

			if (fa != null)
			{
				yeastAdditionTableModel.add(fa);
				ingredientAddition.getIngredients().add(fa);

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == remove)
		{
			int selectedRow = yeastAdditionTable.getSelectedRow();

			if (selectedRow > -1  && yeastAdditionTableModel.data.size() > selectedRow)
			{
				YeastAddition fa = yeastAdditionTableModel.data.get(selectedRow);
				ingredientAddition.getIngredients().remove(fa);

				yeastAdditionTableModel.remove(selectedRow);

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == increaseAmount)
		{
			int selectedRow = yeastAdditionTable.getSelectedRow();

			if (selectedRow > -1 && yeastAdditionTableModel.data.size() > selectedRow)
			{
				YeastAddition fa = yeastAdditionTableModel.data.get(selectedRow);
				fa.setWeight(fa.getWeight() + 1);

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == decreaseAmount)
		{
			int selectedRow = yeastAdditionTable.getSelectedRow();

			if (selectedRow > -1 && yeastAdditionTableModel.data.size() > selectedRow)
			{
				YeastAddition fa = yeastAdditionTableModel.data.get(selectedRow);
				double weight = fa.getWeight() - 1;
				fa.setWeight(Math.max(weight, 0));

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}*/
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == time)
		{
//			this.schedule.setTime((Double)time.getValue());
			SwingUi.instance.refreshProcessSteps();
		}
	}

	/*-------------------------------------------------------------------------*/
	public class YeastAdditionTableModel extends AbstractTableModel
	{
		private List<YeastAddition> data;

		public YeastAdditionTableModel()
		{
			this.data = new ArrayList<YeastAddition>();
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
				case 1: return "Yeast";
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
			YeastAddition yeastAddition = data.get(rowIndex);

			switch (columnIndex)
			{
				case 0: return String.format("%.2fg", yeastAddition.getWeight());
				case 1: return yeastAddition.getYeast().getName();
				case 2: return yeastAddition.getYeast().getType();
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

		public void add(YeastAddition fa)
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
