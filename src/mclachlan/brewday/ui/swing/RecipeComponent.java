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
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.*;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class RecipeComponent extends JPanel implements ActionListener
{
	private RecipeTableModel recipeTableModel;
	private JTable recipeTable;
	private JButton remove, increaseAmount, decreaseAmount;
	private JButton addFermentable, addHop, addMisc, addYeast, addWater, moreTime, lessTime;
	private Recipe recipe;
	private int dirtyFlag;

	public RecipeComponent(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;
		setLayout(new MigLayout());

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

		addFermentable = new JButton(StringUtils.getUiString("common.add.fermentable"), SwingUi.grainsIcon);
		addFermentable.addActionListener(this);

		addHop = new JButton(StringUtils.getUiString("common.add.hop"), SwingUi.hopsIcon);
		addHop.addActionListener(this);

		addMisc = new JButton(StringUtils.getUiString("common.add.misc"), SwingUi.miscIcon);
		addMisc.addActionListener(this);

		addYeast = new JButton(StringUtils.getUiString("common.add.yeast"), SwingUi.yeastIcon);
		addYeast.addActionListener(this);

		addWater = new JButton(StringUtils.getUiString("common.add.water"), SwingUi.waterIcon);
		addWater.addActionListener(this);

		remove = new JButton(StringUtils.getUiString("common.remove"), SwingUi.removeIcon);
		remove.addActionListener(this);

		increaseAmount = new JButton(StringUtils.getUiString("common.increase.amount"), SwingUi.increaseIcon);
		increaseAmount.addActionListener(this);

		decreaseAmount = new JButton(StringUtils.getUiString("common.decrease.amount"), SwingUi.decreaseIcon);
		decreaseAmount.addActionListener(this);

		moreTime = new JButton(StringUtils.getUiString("common.increase.time"), SwingUi.moreTimeIcon);
		moreTime.addActionListener(this);

		lessTime = new JButton(StringUtils.getUiString("common.decrease.time"), SwingUi.lessTimeIcon);
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
			FermentableAdditionDialog dialog = new FermentableAdditionDialog(
				SwingUi.instance, StringUtils.getUiString("common.add.fermentable"), recipe);
			IngredientAddition item = dialog.getResult();

			if (item != null)
			{
				ProcessStep step = dialog.getStepResult();
				step.getIngredients().add(item);
				SwingUi.instance.refreshProcessSteps();
				SwingUi.instance.setDirty(dirtyFlag);
			}
		}
		else if (e.getSource() == addHop)
		{
			HopAdditionDialog dialog = new HopAdditionDialog(
				SwingUi.instance, StringUtils.getUiString("common.add.hop"), recipe);
			IngredientAddition item = dialog.getResult();
		
			if (item != null)
			{
				ProcessStep step = dialog.getStepResult();
				step.getIngredients().add(item);
				SwingUi.instance.refreshProcessSteps();
				SwingUi.instance.setDirty(dirtyFlag);
			}
		}
		else if (e.getSource() == addYeast)
		{
			YeastAdditionDialog dialog = new YeastAdditionDialog(
				SwingUi.instance, StringUtils.getUiString("common.add.yeast"), recipe);
			IngredientAddition item = dialog.getResult();
		
			if (item != null)
			{
				ProcessStep step = dialog.getStepResult();
				step.getIngredients().add(item);
				SwingUi.instance.refreshProcessSteps();
				SwingUi.instance.setDirty(dirtyFlag);
			}
		}
		else if (e.getSource() == addMisc)
		{
			MiscAdditionDialog dialog = new MiscAdditionDialog(
				SwingUi.instance, StringUtils.getUiString("common.add.misc"), recipe);
			IngredientAddition item = dialog.getResult();

			if (item != null)
			{
				ProcessStep step = dialog.getStepResult();
				step.getIngredients().add(item);
				SwingUi.instance.refreshProcessSteps();
				SwingUi.instance.setDirty(dirtyFlag);
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
				SwingUi.instance.setDirty(dirtyFlag);
			}
*/
		}
		else if (e.getSource() == increaseAmount)
		{
			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				IngredientAddition ingredient = recipe.getIngredients().get(selectedRow);

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

				ingredient.setWeight(
					new WeightUnit(
						ingredient.getWeight().get(Quantity.Unit.GRAMS)+amt));
				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
				SwingUi.instance.setDirty(dirtyFlag);
			}
		}
		else if (e.getSource() == decreaseAmount)
		{
			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				IngredientAddition ingredient = recipe.getIngredients().get(selectedRow);

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

				ingredient.setWeight(
					new WeightUnit(
						ingredient.getWeight().get(Quantity.Unit.GRAMS)+amt));
				tableRepaint();
				SwingUi.instance.refreshProcessSteps();
				SwingUi.instance.setDirty(dirtyFlag);
			}
		}
		else if (e.getSource() == moreTime)
		{
/*			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				IngredientAddition item = recipe.getIngredients().get(selectedRow);
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
				IngredientAddition item = recipe.getIngredients().get(selectedRow);

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
					return StringUtils.getUiString("recipe.amount");
				case 1:
					return StringUtils.getUiString("recipe.ingredient");
				case 2:
					return StringUtils.getUiString("recipe.usage");
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
			IngredientAddition ingredient = recipe.getIngredients().get(rowIndex);

			if (ingredient instanceof FermentableAddition)
			{
				switch (columnIndex)
				{
					case 0:
						return StringUtils.getUiString(
							"recipe.fermentable.amount",
							ingredient.getWeight().get(Quantity.Unit.KILOGRAMS));
					case 1:
						return new LabelIcon(
							SwingUi.grainsIcon,
							((FermentableAddition)ingredient).getFermentable().getName());
					case 2:
						return StringUtils.getUiString(
							"recipe.fermentable.time",
							ingredient.getType(),
							(int)ingredient.getTime());
					default:
						throw new BrewdayException("Invalid " + columnIndex);
				}
			}
			else if (ingredient instanceof HopAddition)
			{
				switch (columnIndex)
				{
					case 0:
						return StringUtils.getUiString(
							"recipe.hop.amount",
							ingredient.getWeight().get(Quantity.Unit.GRAMS));
					case 1:
						return new LabelIcon(
							SwingUi.hopsIcon,
							((HopAddition)ingredient).getHop().getName());
					case 2:
						return StringUtils.getUiString("recipe.hop.time",
							ingredient.getType(), (int)ingredient.getTime());
					default:
						throw new BrewdayException("Invalid " + columnIndex);
				}
			}
			else if (ingredient instanceof WaterAddition)
			{
				switch (columnIndex)
				{
					case 0:
						return StringUtils.getUiString("recipe.water.amount",
							ingredient.getWeight().get(Quantity.Unit.KILOGRAMS));
					case 1:
						return new LabelIcon(
							SwingUi.waterIcon,
							((WaterAddition)ingredient).getName());
					case 2:
						return ingredient.getType();
					default:
						throw new BrewdayException("Invalid " + columnIndex);
				}
			}
			else if (ingredient instanceof YeastAddition)
			{
				switch (columnIndex)
				{
					case 0:
						return StringUtils.getUiString("recipe.yeast.amount",
							ingredient.getWeight().get(Quantity.Unit.GRAMS));
					case 1:
						return new LabelIcon(
							SwingUi.yeastIcon,
							((YeastAddition)ingredient).getYeast().getName());
					case 2:
						return StringUtils.getUiString(
							"recipe.yeast.time",
							ingredient.getType(),
							(int)ingredient.getTime());
					default:
						throw new BrewdayException("Invalid " + columnIndex);
				}
			}
			else if (ingredient instanceof MiscAddition)
			{
				switch (columnIndex)
				{
					case 0:
						return StringUtils.getUiString("recipe.misc.amount",
							ingredient.getWeight().get(Quantity.Unit.GRAMS));
					case 1:
						return new LabelIcon(
							SwingUi.miscIcon,
							((MiscAddition)ingredient).getMisc().getName());
					case 2:
						return StringUtils.getUiString("recipe.misc.time",
							ingredient.getType(), (int)ingredient.getTime());
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
	}
}
