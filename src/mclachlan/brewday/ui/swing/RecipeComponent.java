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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Recipe;
import mclachlan.brewday.recipe.*;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class RecipeComponent extends JPanel implements ActionListener
{
	private RecipeTableModel recipeTableModel;
	private JTable recipeTable;
	private JButton addFermentable, remove, increaseAmount, decreaseAmount;
	private JButton addHop, addMisc, addYeast, addWater, moreTime, lessTime;
	private Recipe recipe;

	public RecipeComponent()
	{
		setLayout(new MigLayout());

		JPanel topPanel = new JPanel(new BorderLayout());

		this.add(topPanel, "wrap");

		recipeTableModel = new RecipeTableModel();
		recipeTable = new JTable(recipeTableModel);
		recipeTable.setFillsViewportHeight(true);
		recipeTable.setAutoCreateRowSorter(true);
		recipeTable.setPreferredScrollableViewportSize(new Dimension(700, 200));
		recipeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		recipeTable.setDefaultRenderer(LabelIcon.class, new LabelIconRenderer());
		recipeTable.getColumnModel().getColumn(0).setPreferredWidth(30);
		recipeTable.getColumnModel().getColumn(1).setPreferredWidth(400);
		recipeTable.getColumnModel().getColumn(2).setPreferredWidth(70);

		this.add(new JScrollPane(recipeTable), BorderLayout.CENTER);

		JPanel buttons = new JPanel(new MigLayout());

		addFermentable = new JButton("Add Fermentable", SwingUi.grainsIcon);
		addFermentable.addActionListener(this);

		addHop = new JButton("Add Hop", SwingUi.hopsIcon);
		addHop.addActionListener(this);

		addMisc = new JButton("Add Misc", SwingUi.miscIcon);
		addMisc.addActionListener(this);

		addYeast = new JButton("Add Yeast", SwingUi.yeastIcon);
		addYeast.addActionListener(this);

		addWater = new JButton("Add Water", SwingUi.waterIcon);
		addWater.addActionListener(this);

		remove = new JButton("Remove", SwingUi.removeIcon);
		remove.addActionListener(this);

		increaseAmount = new JButton("Increase Amount", SwingUi.increaseIcon);
		increaseAmount.addActionListener(this);

		decreaseAmount = new JButton("Decrease Amount", SwingUi.decreaseIcon);
		decreaseAmount.addActionListener(this);

		moreTime = new JButton("Increase Time", SwingUi.moreTimeIcon);
		moreTime.addActionListener(this);

		lessTime = new JButton("Decrease Time", SwingUi.lessTimeIcon);
		lessTime.addActionListener(this);

		buttons.add(addFermentable, "");
		buttons.add(addHop, "");
		buttons.add(addMisc, "wrap");
		buttons.add(addYeast, "");
		buttons.add(addWater, "");
		buttons.add(remove, "wrap");
		buttons.add(new JLabel(""));
		buttons.add(new JLabel(""));
		buttons.add(new JLabel(""), "wrap");
		buttons.add(increaseAmount, "");
		buttons.add(decreaseAmount, "wrap");
		buttons.add(moreTime, "");
		buttons.add(lessTime, "wrap");

		this.add(buttons, BorderLayout.SOUTH);
	}

	public void refresh(Recipe recipe)
	{
		this.recipe = recipe;
		this.revalidate();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == addFermentable)
		{
			FermentableAdditionDialog dialog = new FermentableAdditionDialog(SwingUi.instance, "Add Fermentable", recipe);
			RecipeLineItem item = dialog.getResult();

			if (item != null)
			{
				ProcessStep step = dialog.getStepResult();
				step.getIngredients().add(item);
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == addHop)
		{
			HopAdditionDialog dialog = new HopAdditionDialog(SwingUi.instance, "Add Hop", recipe);
			RecipeLineItem item = dialog.getResult();
		
			if (item != null)
			{
				ProcessStep step = dialog.getStepResult();
				step.getIngredients().add(item);
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == addYeast)
		{
			YeastAdditionDialog dialog = new YeastAdditionDialog(SwingUi.instance, "Add Yeast", recipe);
			RecipeLineItem item = dialog.getResult();
		
			if (item != null)
			{
				ProcessStep step = dialog.getStepResult();
				step.getIngredients().add(item);
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == remove)
		{
			int selectedRow = recipeTable.getSelectedRow();

/*
			if (selectedRow > -1)
			{
				FermentableAddition fa = recipeTableModel.data.get(selectedRow);
				ingredientAddition.getIngredients().remove(fa);
				recipeTableModel.remove(selectedRow);

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
*/
		}
		else if (e.getSource() == increaseAmount)
		{
			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				RecipeLineItem item = recipe.getIngredients().get(selectedRow);

				IngredientAddition ingredient = item.getIngredient();

				int amt = 0;
				if (ingredient instanceof FermentableAddition)
				{
					amt = 250;
				}
				else if (ingredient instanceof HopAddition)
				{
					amt = 5;
				}
				else if (ingredient instanceof WaterAddition)
				{
					amt = 1000;
				}
				else if (ingredient instanceof YeastAddition)
				{
					amt = 1;
				}

				ingredient.setWeight(ingredient.getWeight() +amt);
				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == decreaseAmount)
		{
			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				RecipeLineItem item = recipe.getIngredients().get(selectedRow);

				IngredientAddition ingredient = item.getIngredient();

				int amt = 0;
				if (ingredient instanceof FermentableAddition)
				{
					amt = 250;
				}
				else if (ingredient instanceof HopAddition)
				{
					amt = 5;
				}
				else if (ingredient instanceof WaterAddition)
				{
					amt = 1000;
				}
				else if (ingredient instanceof YeastAddition)
				{
					amt = 1;
				}

				ingredient.setWeight(Math.max(0, ingredient.getWeight() -amt));
				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}
		}
		else if (e.getSource() == moreTime)
		{
/*			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				RecipeLineItem item = recipe.getIngredients().get(selectedRow);
				IngredientAddition ingredient = item.getIngredient();
				ProcessStep step = item.getStep();

				int amt = 1;

				for (AdditionSchedule as : step.getIngredientAdditions())
				{
					Volume vol = recipe.getVolumes().getVolume(as.getIngredientAddition());

					if (vol.contains(ingredient))
					{
						if (vol instanceof FermentableAdditionList)
						{
							if (((FermentableAdditionList)vol).getIngredients().size() == 1)
							{
								// contains only this ingredient. just change the time.
								as.setTime(as.getTime() +amt);
							}
							else
							{
								((FermentableAdditionList)vol).getIngredients().remove(ingredient);
								FermentableAdditionList newVol = new FermentableAdditionList(
									recipe.getUniqueInputVolumeName(step.getType()+" fermentables"),
									(FermentableAddition)ingredient);
								step.addIngredientAddition(newVol, ingredient, as.getTime() +amt, recipe);
							}
						}
						else if (vol instanceof HopAdditionList)
						{

						}
						else if (vol instanceof WaterAddition)
						{

						}
						else if (vol instanceof YeastAdditionList)
						{

						}

					}
				}

				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}*/
		}
		else if (e.getSource() == lessTime)
		{
			/*int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				RecipeLineItem item = recipe.getIngredients().get(selectedRow);

				IngredientAddition ingredient = item.getIngredient();

				int amt = 0;
				if (ingredient instanceof FermentableAddition)
				{
					amt = 1;
				}
				else if (ingredient instanceof HopAddition)
				{
					amt = 1;
				}
				else if (ingredient instanceof WaterAddition)
				{
					amt = 1;
				}
				else if (ingredient instanceof YeastAddition)
				{
					amt = 1;
				}

				item.setTime(Math.max(0, item.getTime() - amt));
				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
			}*/
		}
	}

	protected void tableRepaint()
	{
		recipeTableModel.fireTableDataChanged();
		recipeTable.repaint();
	}

	/*-------------------------------------------------------------------------*/
	public class RecipeTableModel extends AbstractTableModel
	{
		@Override
		public int getRowCount()
		{
			if (recipe == null)
			{
				return 0;
			}
			else
			{
				return recipe.getIngredients().size();
			}
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
				case 0:
					return "Amount";
				case 1:
					return "Ingredient";
				case 2:
					return "Usage";
				default:
					throw new BrewdayException("Invalid " + columnIndex);
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0:
					return String.class;
				case 1:
					return LabelIcon.class;
				case 2:
					return String.class;
				default:
					throw new BrewdayException("Invalid " + columnIndex);
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			RecipeLineItem lineItem = recipe.getIngredients().get(rowIndex);

			IngredientAddition ingredient = lineItem.getIngredient();

			if (ingredient instanceof FermentableAddition)
			{
				switch (columnIndex)
				{
					case 0:
						return String.format("%.2fkg", ingredient.getWeight() / 1000);
					case 1:
						return new LabelIcon(
							SwingUi.grainsIcon,
							((FermentableAddition)ingredient).getFermentable().getName());
					case 2:
						return String.format("%s %d min", lineItem.getIngredient().getType(), (int)lineItem.getTime());
					default:
						throw new BrewdayException("Invalid " + columnIndex);
				}
			}
			else if (ingredient instanceof HopAddition)
			{
				switch (columnIndex)
				{
					case 0:
						return String.format("%.0fg", ingredient.getWeight());
					case 1:
						return new LabelIcon(
							SwingUi.hopsIcon,
							((HopAddition)ingredient).getHop().getName());
					case 2:
						return String.format("%s %d min", lineItem.getIngredient().getType(), (int)lineItem.getTime());
					default:
						throw new BrewdayException("Invalid " + columnIndex);
				}
			}
			else if (ingredient instanceof WaterAddition)
			{
				switch (columnIndex)
				{
					case 0:
						return String.format("%.2fl", ingredient.getWeight() / 1000);
					case 1:
						return new LabelIcon(
							SwingUi.waterIcon,
							((WaterAddition)ingredient).getName());
					case 2:
						return lineItem.getIngredient().getType();
					default:
						throw new BrewdayException("Invalid " + columnIndex);
				}
			}
			else if (ingredient instanceof YeastAddition)
			{
				switch (columnIndex)
				{
					case 0:
						return String.format("%.0fg", ingredient.getWeight());
					case 1:
						return new LabelIcon(
							SwingUi.yeastIcon,
							((YeastAddition)ingredient).getYeast().getName());
					case 2:
						return String.format("%s %d days", lineItem.getIngredient().getType(), (int)lineItem.getTime());
					default:
						throw new BrewdayException("Invalid " + columnIndex);
				}
			}
			else
			{
				throw new BrewdayException("Invalid addition: " + ingredient);
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
			// todo
		}

		public void clear()
		{
			// todo
		}

		public void remove(int selectedRow)
		{
			// todo
		}
	}

	/*-------------------------------------------------------------------------*/
	private static class LabelIcon
	{
		private Icon icon;
		private String label;

		public LabelIcon(Icon icon, String label)
		{
			this.icon = icon;
			this.label = label;
		}
	}

	/*-------------------------------------------------------------------------*/
	private static class LabelIconRenderer extends DefaultTableCellRenderer
	{
		public LabelIconRenderer()
		{
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object
			value, boolean isSelected, boolean hasFocus, int row, int col)
		{
			JLabel r = (JLabel)super.getTableCellRendererComponent(
				table, value, isSelected, hasFocus, row, col);
			setIcon(((LabelIcon)value).icon);
			setText(((LabelIcon)value).label);
			return r;
		}
	}
}
