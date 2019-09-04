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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.document.DocumentCreator;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.*;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class RecipeComponent extends JPanel implements ActionListener
{
	private RecipeTableModel recipeTableModel;
	private JTable recipeTable;
	private JButton remove, increaseAmount, decreaseAmount, duplicate, substitute, edit,
		addFermentable, addHop, addMisc, addYeast, addWater, moreTime, lessTime;
	private JButton generateDocument;
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
		recipeTable.getColumnModel().getColumn(1).setPreferredWidth(350);
		recipeTable.getColumnModel().getColumn(2).setPreferredWidth(100);
		recipeTable.getColumnModel().getColumn(3).setPreferredWidth(30);

		this.add(new JScrollPane(recipeTable), BorderLayout.CENTER);

		JPanel southPanel = new JPanel(new BorderLayout());

		JPanel ingredientButtons = new JPanel(new MigLayout());

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

		duplicate = new JButton(StringUtils.getUiString("common.duplicate"), SwingUi.duplicateIcon);
		duplicate.addActionListener(this);

		substitute = new JButton(StringUtils.getUiString("common.substitute"), SwingUi.substituteIcon);
		substitute.addActionListener(this);

		edit = new JButton(StringUtils.getUiString("common.edit"), SwingUi.editIcon);
		edit.addActionListener(this);

		ingredientButtons.add(addFermentable, "grow");
		ingredientButtons.add(addWater, "grow");
		ingredientButtons.add(addHop, "grow");
		ingredientButtons.add(addYeast, "grow");
		ingredientButtons.add(addMisc, "grow,wrap");

		ingredientButtons.add(edit, "grow");
		ingredientButtons.add(substitute, "grow");
		ingredientButtons.add(duplicate, "grow");
		ingredientButtons.add(remove, "grow,wrap");

		ingredientButtons.add(increaseAmount, "grow");
		ingredientButtons.add(decreaseAmount, "grow");
		ingredientButtons.add(moreTime, "grow");
		ingredientButtons.add(lessTime, "grow,wrap");

		ingredientButtons.setBorder(
			BorderFactory.createTitledBorder(
				StringUtils.getUiString("recipe.ingredients")));

		generateDocument = new JButton(StringUtils.getUiString("doc.gen.generate.document"), SwingUi.documentIcon);
		generateDocument.addActionListener(this);

		JPanel toolsButtons = new JPanel(new MigLayout());

		toolsButtons.add(generateDocument, "grow");

		toolsButtons.setBorder(
			BorderFactory.createTitledBorder(
				StringUtils.getUiString("recipe.tools")));

		southPanel.add(ingredientButtons, BorderLayout.CENTER);
		southPanel.add(toolsButtons, BorderLayout.SOUTH);

		this.add(southPanel, BorderLayout.SOUTH);
	}

	public void refresh(Recipe recipe)
	{
		this.recipe = recipe;
		this.revalidate();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == addFermentable)
		{
			FermentableStepAddition fermentableStepAddition = new FermentableStepAddition().invoke(null);
			IngredientAddition item = fermentableStepAddition.getItem();
			ProcessStep step = fermentableStepAddition.getStep();

			if (item != null)
			{
				step.getIngredients().add(item);
				justRefreshDammit();
			}
		}
		else if (e.getSource() == addHop)
		{
			HopStepAddition hopStepAddition = new HopStepAddition().invoke(null);
			IngredientAddition item = hopStepAddition.getItem();
			ProcessStep step = hopStepAddition.getStep();

			if (item != null)
			{
				step.getIngredients().add(item);
				justRefreshDammit();
			}
		}
		else if (e.getSource() == addYeast)
		{
			YeastStepAddition yeastStepAddition = new YeastStepAddition().invoke(null);
			IngredientAddition item = yeastStepAddition.getItem();
			ProcessStep step = yeastStepAddition.getStep();

			if (item != null)
			{
				step.getIngredients().add(item);
				justRefreshDammit();
			}
		}
		else if (e.getSource() == addMisc)
		{
			MiscStepAddition miscStepAddition = new MiscStepAddition().invoke(null);
			IngredientAddition item = miscStepAddition.getItem();
			ProcessStep step = miscStepAddition.getStep();

			if (item != null)
			{
				step.getIngredients().add(item);
				justRefreshDammit();
			}
		}
		else if (e.getSource() == addWater)
		{
			WaterStepAddition waterStepAddition = new WaterStepAddition().invoke(null);
			IngredientAddition item = waterStepAddition.getItem();
			ProcessStep step = waterStepAddition.getStep();

			if (item != null)
			{
				step.getIngredients().add(item);
				justRefreshDammit();
			}
		}
		else if (e.getSource() == remove)
		{
			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				IngredientAddition ia = recipe.getIngredients().get(selectedRow);
				recipe.removeIngredient(ia);
				justRefreshDammit();
			}
		}
		else if (e.getSource() == increaseAmount)
		{
			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				IngredientAddition ingredient = recipe.getIngredients().get(selectedRow);

				int maxAmt = Integer.MAX_VALUE;
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
				else if (ingredient instanceof MiscAddition)
				{
					amt = 1;
				}
				else
				{
					throw new BrewdayException("Invalid: " + ingredient);
				}

				ingredient.getQuantity().set(
					Math.min(maxAmt, ingredient.getQuantity().get() + amt));
				justRefreshDammit();
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
				else if (ingredient instanceof MiscAddition)
				{
					amt = 1;
				}
				else
				{
					throw new BrewdayException("Invalid: " + ingredient);
				}

				ingredient.getQuantity().set(
					Math.max(0, ingredient.getQuantity().get() - amt));
				justRefreshDammit();
			}
		}
		else if (e.getSource() == moreTime)
		{
			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				IngredientAddition ia = recipe.getIngredients().get(selectedRow);
				ProcessStep ps = recipe.getStepOfAddition(ia);

				Quantity.Unit unit = Quantity.Unit.SECONDS;
				int maxAmt = Integer.MAX_VALUE;

				if (ps.getType() == ProcessStep.Type.BOIL)
				{
					maxAmt = (int)((Boil)ps).getDuration().get(Quantity.Unit.MINUTES);
					unit = Quantity.Unit.MINUTES;
				}
				if (ps.getType() == ProcessStep.Type.STAND)
				{
					maxAmt = (int)((Stand)ps).getDuration().get(Quantity.Unit.MINUTES);
					unit = Quantity.Unit.MINUTES;
				}
				if (ps.getType() == ProcessStep.Type.MASH)
				{
					maxAmt = (int)((Mash)ps).getDuration().get(Quantity.Unit.MINUTES);
					unit = Quantity.Unit.MINUTES;
				}
				else if (ps.getType() == ProcessStep.Type.FERMENT)
				{
					maxAmt = (int)((Ferment)ps).getDuration().get(Quantity.Unit.DAYS);
					unit = Quantity.Unit.DAYS;
				}

				double value = ia.getTime().get(unit);

				int amt = 1;
				value = Math.min(value + amt, maxAmt);
				ia.setTime(new TimeUnit(value, unit, false));
				justRefreshDammit();
			}
		}
		else if (e.getSource() == lessTime)
		{
			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				IngredientAddition ia = recipe.getIngredients().get(selectedRow);
				ProcessStep ps = recipe.getStepOfAddition(ia);

				Quantity.Unit unit = Quantity.Unit.SECONDS;

				if (ps.getType() == ProcessStep.Type.BOIL)
				{
					unit = Quantity.Unit.MINUTES;
				}
				if (ps.getType() == ProcessStep.Type.STAND)
				{
					unit = Quantity.Unit.MINUTES;
				}
				if (ps.getType() == ProcessStep.Type.MASH)
				{
					unit = Quantity.Unit.MINUTES;
				}
				else if (ps.getType() == ProcessStep.Type.FERMENT)
				{
					unit = Quantity.Unit.DAYS;
				}

				double value = ia.getTime().get(unit);


				int amt = 1;
				value = Math.max(value - amt, 0);
				ia.setTime(new TimeUnit(value, unit, false));
				justRefreshDammit();
			}
		}
		else if (e.getSource() == substitute || e.getSource() == edit)
		{
			//
			// The "or edit" bit is kind of a hack, but hey
			//

			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				IngredientAddition ia = recipe.getIngredients().get(selectedRow);
				ProcessStep ps = recipe.getStepOfAddition(ia);

				IngredientAddition newItem;
				ProcessStep newStep;

				if (ia instanceof FermentableAddition)
				{
					FermentableStepAddition sa = new FermentableStepAddition().invoke(
						(FermentableAddition)ia);
					newItem = sa.getItem();
					newStep = sa.getStep();
				}
				else if (ia instanceof HopAddition)
				{
					HopStepAddition sa = new HopStepAddition().invoke((HopAddition)ia);
					newItem = sa.getItem();
					newStep = sa.getStep();
				}
				else if (ia instanceof WaterAddition)
				{
					WaterStepAddition sa = new WaterStepAddition().invoke((WaterAddition)ia);
					newItem = sa.getItem();
					newStep = sa.getStep();
				}
				else if (ia instanceof YeastAddition)
				{
					YeastStepAddition sa = new YeastStepAddition().invoke((YeastAddition)ia);
					newItem = sa.getItem();
					newStep = sa.getStep();
				}
				else if (ia instanceof MiscAddition)
				{
					MiscStepAddition sa = new MiscStepAddition().invoke((MiscAddition)ia);
					newItem = sa.getItem();
					newStep = sa.getStep();
				}
				else
				{
					throw new BrewdayException("Invalid: " + ia);
				}

				if (newItem != null)
				{
					ps.removeIngredientAddition(ia);
					newStep.addIngredientAddition(newItem);

					justRefreshDammit();
				}
			}
		}
		else if (e.getSource() == duplicate)
		{
			int selectedRow = recipeTable.getSelectedRow();

			if (selectedRow > -1)
			{
				IngredientAddition ia = recipe.getIngredients().get(selectedRow);
				ProcessStep ps = recipe.getStepOfAddition(ia);
				IngredientAddition newAddition = ia.clone();
				ps.addIngredientAddition(newAddition);

				justRefreshDammit();
			}
		}
		else if (e.getSource() == generateDocument)
		{
			List<String> documentTemplates = Database.getInstance().getDocumentTemplates();

			String template = (String)JOptionPane.showInputDialog(
				SwingUi.instance,
				StringUtils.getUiString("doc.gen.choose.template"),
				StringUtils.getUiString("doc.gen.generate.document"),
				JOptionPane.PLAIN_MESSAGE,
				SwingUi.documentIcon,
				documentTemplates.toArray(),
				null);

			if (template != null)
			{
				String defaultSuffix = template.substring(0, template.indexOf("."));

				String extension = template.substring(
					template.indexOf(".")+1,
					template.lastIndexOf("."));

				JFileChooser chooser = new JFileChooser();

				FileNameExtensionFilter filter = new FileNameExtensionFilter(
					"."+extension, extension);
				chooser.setFileFilter(filter);

				chooser.setSelectedFile(
					new File(
						recipe.getName().replaceAll("\\W", "_")+
							"_"+defaultSuffix+"."+ extension));

				int returnVal = chooser.showSaveDialog(SwingUi.instance);
				if (returnVal == JFileChooser.APPROVE_OPTION)
				{
					DocumentCreator dc = DocumentCreator.getInstance();

					try
					{
						File outputFile = chooser.getSelectedFile();
						dc.createDocument(recipe, template, outputFile);
						Desktop.getDesktop().open(outputFile);
					}
					catch (IOException ex)
					{
						throw new BrewdayException(ex);
					}
				}
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private void justRefreshDammit()
	{
		tableRepaint();
		SwingUi.instance.refreshProcessSteps();
		SwingUi.instance.setDirty(dirtyFlag);
	}

	/*-------------------------------------------------------------------------*/
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
			return 4;
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
					return StringUtils.getUiString("recipe.step");
				case 3:
					return StringUtils.getUiString("recipe.time");
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
				case 3:
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

			ProcessStep ps = recipe.getStepOfAddition(ingredient);
			String stepOfAddition = ps == null ? "?" : ps.getName();

			if (ingredient instanceof FermentableAddition)
			{
				switch (columnIndex)
				{
					case 0:
						return StringUtils.getUiString(
							"recipe.fermentable.amount",
							ingredient.getQuantity().get(Quantity.Unit.KILOGRAMS));
					case 1:
						return new LabelIcon(
							SwingUi.grainsIcon,
							((FermentableAddition)ingredient).getFermentable().getName());
					case 2:
						return stepOfAddition;
					case 3:
						return StringUtils.getUiString(
							"recipe.fermentable.time",
							(int)ingredient.getTime().get(Quantity.Unit.MINUTES));
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
							ingredient.getQuantity().get(Quantity.Unit.GRAMS));
					case 1:
						return new LabelIcon(
							SwingUi.hopsIcon,
							((HopAddition)ingredient).getHop().getName());
					case 2:
						return stepOfAddition;
					case 3:
						return StringUtils.getUiString("recipe.hop.time",
							(int)ingredient.getTime().get(Quantity.Unit.MINUTES));
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
							ingredient.getQuantity().get(Quantity.Unit.LITRES));
					case 1:
						return new LabelIcon(
							SwingUi.waterIcon,
							((WaterAddition)ingredient).getName());
					case 2:
						return stepOfAddition;
					case 3:
						return StringUtils.getUiString(
							"recipe.water.time",
							(int)ingredient.getTime().get(Quantity.Unit.MINUTES));
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
							ingredient.getQuantity().get(Quantity.Unit.GRAMS));
					case 1:
						return new LabelIcon(
							SwingUi.yeastIcon,
							((YeastAddition)ingredient).getYeast().getName());
					case 2:
						return stepOfAddition;
					case 3:
						return StringUtils.getUiString(
							"recipe.yeast.time",
							(int)ingredient.getTime().get(Quantity.Unit.DAYS));
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
							ingredient.getQuantity().get(Quantity.Unit.GRAMS));
					case 1:
						return new LabelIcon(
							SwingUi.miscIcon,
							((MiscAddition)ingredient).getMisc().getName());
					case 2:
						return stepOfAddition;
					case 3:
						return StringUtils.getUiString("recipe.misc.time",
							(int)ingredient.getTime().get(Quantity.Unit.MINUTES));
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

	private class FermentableStepAddition
	{
		private IngredientAddition item;
		private ProcessStep step;

		public IngredientAddition getItem()
		{
			return item;
		}

		public ProcessStep getStep()
		{
			return step;
		}

		public FermentableStepAddition invoke(
			FermentableAddition selected)
		{
			FermentableAdditionDialog dialog = new FermentableAdditionDialog(
				SwingUi.instance,
				StringUtils.getUiString("common.add.fermentable"),
				recipe,
				selected,
				null);
			item = dialog.getResult();
			step = dialog.getStepResult();
			return this;
		}
	}

	private class HopStepAddition
	{
		private IngredientAddition item;
		private ProcessStep step;

		public IngredientAddition getItem()
		{
			return item;
		}

		public ProcessStep getStep()
		{
			return step;
		}

		public HopStepAddition invoke(
			HopAddition selected)
		{
			HopAdditionDialog dialog = new HopAdditionDialog(
				SwingUi.instance,
				StringUtils.getUiString("common.add.hop"),
				recipe,
				selected,
				null);
			item = dialog.getResult();
			step = dialog.getStepResult();
			return this;
		}
	}

	private class YeastStepAddition
	{
		private IngredientAddition item;
		private ProcessStep step;

		public IngredientAddition getItem()
		{
			return item;
		}

		public ProcessStep getStep()
		{
			return step;
		}

		public YeastStepAddition invoke(
			YeastAddition selected)
		{
			YeastAdditionDialog dialog = new YeastAdditionDialog(
				SwingUi.instance,
				StringUtils.getUiString("common.add.yeast"),
				recipe,
				selected,
				null);
			item = dialog.getResult();
			step = dialog.getStepResult();
			return this;
		}
	}

	private class MiscStepAddition
	{
		private IngredientAddition item;
		private ProcessStep step;

		public IngredientAddition getItem()
		{
			return item;
		}

		public ProcessStep getStep()
		{
			return step;
		}

		public MiscStepAddition invoke(
			MiscAddition selected)
		{
			MiscAdditionDialog dialog = new MiscAdditionDialog(
				SwingUi.instance,
				StringUtils.getUiString("common.add.misc"),
				recipe,
				selected,
				null);
			item = dialog.getResult();
			step = dialog.getStepResult();
			return this;
		}
	}

	private class WaterStepAddition
	{
		private IngredientAddition item;
		private ProcessStep step;

		public IngredientAddition getItem()
		{
			return item;
		}

		public ProcessStep getStep()
		{
			return step;
		}

		public WaterStepAddition invoke(
			WaterAddition selected)
		{
			WaterAdditionDialog dialog = new WaterAdditionDialog(
				SwingUi.instance,
				StringUtils.getUiString("common.add.water"),
				recipe,
				selected,
				null);
			item = dialog.getResult();
			step = dialog.getStepResult();
			return this;
		}
	}
}
