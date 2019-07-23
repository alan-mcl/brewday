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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.*;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class ProcessTemplatePanel extends EditorPanel implements TreeSelectionListener
{
	private Recipe recipe;

	private JButton addStep, removeStep;
	private JPanel stepCards;
	private CardLayout stepCardLayout;
	private ProcessStepPanel mashInfusionPanel, batchSpargePanel, boilPanel,
		coolPanel, dilutePanel, fermentPanel, mashPanel,
		standPanel, packagePanel, splitByPercentPanel;
	private JTree stepsTree;
	private StepsTreeModel stepsTreeModel;

	/*-------------------------------------------------------------------------*/
	public ProcessTemplatePanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected Container getEditControls()
	{
		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));

		JPanel stepsPanel = getStepsTab();

		result.add(stepsPanel);

		return result;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel getStepsTab()
	{
		stepsTreeModel = new StepsTreeModel();
		stepsTree = new StepsTree(stepsTreeModel);
		stepsTree.addTreeSelectionListener(this);

		DefaultTreeCellRenderer renderer = new StepsTreeCellRenderer();

		stepsTree.setCellRenderer(renderer);

		addStep = new JButton(StringUtils.getUiString("recipe.add.step"));
		addStep.addActionListener(this);
		removeStep = new JButton(StringUtils.getUiString("recipe.remove.step"));
		removeStep.addActionListener(this);

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new MigLayout());

		buttonsPanel.add(addStep, "align center");
		buttonsPanel.add(removeStep, "align center, wrap");

		JPanel stepsPanel = new JPanel();
		stepsPanel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(
			stepsTree,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		stepsPanel.add(scrollPane, BorderLayout.CENTER);
		stepsPanel.add(buttonsPanel, BorderLayout.SOUTH);

		stepCardLayout = new CardLayout();
		stepCards = new JPanel(stepCardLayout);

		batchSpargePanel = new BatchSpargePanel(dirtyFlag);
		boilPanel = new BoilPanel(dirtyFlag);
		coolPanel = new CoolPanel(dirtyFlag);
		dilutePanel = new DilutePanel(dirtyFlag);
		fermentPanel = new FermentPanel(dirtyFlag);
		mashPanel = new MashPanel(dirtyFlag);
		standPanel = new StandPanel(dirtyFlag);
		packagePanel = new PackagePanel(dirtyFlag);
		splitByPercentPanel = new SplitByPercentPanel(dirtyFlag);
		mashInfusionPanel = new MashInfusionPanel(dirtyFlag);

		stepCards.add(EditorPanel.NONE, new JPanel());

		stepCards.add(ProcessStep.Type.BATCH_SPARGE.toString(), batchSpargePanel);
		stepCards.add(ProcessStep.Type.BOIL.toString(), boilPanel);
		stepCards.add(ProcessStep.Type.COOL.toString(), coolPanel);
		stepCards.add(ProcessStep.Type.DILUTE.toString(), dilutePanel);
		stepCards.add(ProcessStep.Type.FERMENT.toString(), fermentPanel);
		stepCards.add(ProcessStep.Type.MASH.toString(), mashPanel);
		stepCards.add(ProcessStep.Type.STAND.toString(), standPanel);
		stepCards.add(ProcessStep.Type.PACKAGE.toString(), packagePanel);
		stepCards.add(ProcessStep.Type.MASH_INFUSION.toString(), mashInfusionPanel);
		stepCards.add(ProcessStep.Type.SPLIT_BY_PERCENT.toString(), splitByPercentPanel);

		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));
		result.add(stepsPanel);
		result.add(stepCards);

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void refresh(String name)
	{
		stepsTree.clearSelection();

		refresh(Database.getInstance().getProcessTemplates().get(name));
	}

	/*-------------------------------------------------------------------------*/
	protected void refresh(Recipe newRecipe)
	{
		recipe = newRecipe;

		refreshSteps();

		stepsTree.setSelectionPaths(new TreePath[]{new TreePath(recipe)});
		stepsTree.requestFocusInWindow();
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshSteps()
	{
		recipe.dryRun();
		refreshStepCards();

		stepsTreeModel.fireFullRefresh();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void commit(String name)
	{
		// not needed since we operate directly on the recipe
	}

	@Override
	public Collection<String> loadData()
	{
		return Database.getInstance().getProcessTemplates().keySet();
	}

	@Override
	public void newItem(String name)
	{
		Recipe recipe = Brewday.getInstance().createNewRecipe(name);
		Database.getInstance().getProcessTemplates().put(recipe.getName(), recipe);
	}

	@Override
	public void renameItem(String newName)
	{
		Database.getInstance().getProcessTemplates().remove(currentName);
		recipe.setName(newName);
		Database.getInstance().getProcessTemplates().put(newName, recipe);
	}

	@Override
	public void copyItem(String newName)
	{
		// todo
	}

	@Override
	public void deleteItem()
	{
		Database.getInstance().getRecipes().remove(currentName);
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshStepCards()
	{
		TreePath selected = stepsTree.getSelectionPath();

		if (selected != null)
		{
			Object last = selected.getLastPathComponent();
			if (last instanceof ProcessStep)
			{
				refreshStepCards((ProcessStep)last);
			}
			else
			{
				refreshStepCards((ProcessStep)null);
			}

			stepsTreeModel.fireNodeChanged(last);
		}
	}

	/*-------------------------------------------------------------------------*/

	private void refreshStepCards(ProcessStep step)
	{
		if (step != null)
		{
			switch (step.getType())
			{
				case BATCH_SPARGE:
					batchSpargePanel.refresh(step, recipe);
					break;
				case BOIL:
					boilPanel.refresh(step, recipe);
					break;
				case COOL:
					coolPanel.refresh(step, recipe);
					break;
				case DILUTE:
					dilutePanel.refresh(step, recipe);
					break;
				case FERMENT:
					fermentPanel.refresh(step, recipe);
					break;
				case MASH:
					mashPanel.refresh(step, recipe);
					break;
				case STAND:
					standPanel.refresh(step, recipe);
					break;
				case PACKAGE:
					packagePanel.refresh(step, recipe);
					break;
				case MASH_INFUSION:
					mashInfusionPanel.refresh(step, recipe);
					break;
				case SPLIT_BY_PERCENT:
					splitByPercentPanel.refresh(step, recipe);
					break;
				default:
					throw new BrewdayException("Invalid step " + step.getType());
			}

			stepCardLayout.show(stepCards, step.getType().toString());
		}
		else
		{
			stepCardLayout.show(stepCards, EditorPanel.NONE);
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == addStep)
		{
			AddProcessStepDialog dialog = new AddProcessStepDialog(
				SwingUi.instance, StringUtils.getUiString("recipe.add.process.step"), recipe);

			ProcessStep newProcessStep = dialog.getResult();
			if (newProcessStep != null)
			{
				recipe.getSteps().add(newProcessStep);
				refreshSteps();
			}
		}
		else if (e.getSource() == removeStep)
		{
			Object obj = stepsTree.getLastSelectedPathComponent();
			if (obj instanceof ProcessStep)
			{
				recipe.getSteps().remove(obj);
				refreshSteps();
			}
		}
	}

	/*-------------------------------------------------------------------------*/

	public Recipe getRecipe()
	{
		return recipe;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e)
	{
		if (e.getSource() == stepsTree)
		{
			Object selected = stepsTree.getLastSelectedPathComponent();

			if (selected instanceof ProcessStep)
			{
				refreshStepCards((ProcessStep)selected);
			}
			else
			{
				refreshStepCards((ProcessStep)null);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private class StepsTreeModel implements TreeModel
	{
		private List<TreeModelListener> treeModelListeners = new ArrayList<>();

		@Override
		public Object getRoot()
		{
			return recipe;
		}

		@Override
		public Object getChild(Object parent, int index)
		{
			if (parent instanceof Recipe)
			{
				return recipe.getSteps().get(index);
			}
			else if (parent instanceof ProcessStep)
			{
				return 0;
			}
			else
			{
				throw new BrewdayException("invalid node type: " + parent.getClass());
			}
		}

		@Override
		public int getChildCount(Object parent)
		{
			if (parent instanceof Recipe)
			{
				return recipe.getSteps().size();
			}
			else if (parent instanceof ProcessStep)
			{
				return 0;
			}
			else
			{
				throw new BrewdayException("invalid node type: " + parent.getClass());
			}
		}

		@Override
		public boolean isLeaf(Object node)
		{
			if (node instanceof Recipe)
			{
				return false;
			}
			else if (node instanceof ProcessStep)
			{
				return true;
			}
			else
			{
				throw new BrewdayException("invalid node type: " + node.getClass());
			}
		}

		@Override
		public void valueForPathChanged(TreePath path, Object newValue)
		{

		}

		@Override
		public int getIndexOfChild(Object parent, Object child)
		{
			if (parent instanceof Recipe)
			{
				return recipe.getSteps().indexOf(child);
			}
			else if (parent instanceof ProcessStep)
			{
				ProcessStep step = (ProcessStep)parent;
				if (step.getSupportedIngredientAdditions().size() > 0)
				{
					return step.getIngredients().indexOf(child);
				}
				else
				{
					return -1;
				}
			}
			else
			{
				throw new BrewdayException("invalid node type: " + parent.getClass());
			}
		}

		@Override
		public void addTreeModelListener(TreeModelListener l)
		{
			treeModelListeners.add(l);
		}

		@Override
		public void removeTreeModelListener(TreeModelListener l)
		{
			treeModelListeners.remove(l);
		}

		protected void fireFullRefresh()
		{
			fireNodeChanged(recipe);
		}

		public void fireNodeChanged(Object step)
		{
			TreePath[] selectionPaths = stepsTree.getSelectionPaths();

			TreeModelEvent e = new TreeModelEvent(this, new Object[]{step});
			for (TreeModelListener tml : treeModelListeners)
			{
				tml.treeStructureChanged(e);
			}

			stepsTree.setSelectionPaths(selectionPaths);
		}
	}

	/*-------------------------------------------------------------------------*/
	private class StepsTreeCellRenderer extends DefaultTreeCellRenderer
	{
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

			if (value instanceof IngredientAddition)
			{
				switch (((IngredientAddition)value).getType())
				{
					case FERMENTABLES:
						setIcon(SwingUi.grainsIcon);
						break;
					case HOPS:
						setIcon(SwingUi.hopsIcon);
						break;
					case WATER:
						setIcon(SwingUi.waterIcon);
						break;
					case YEAST:
						setIcon(SwingUi.yeastIcon);
						break;
				}
			}
			else if (value instanceof ProcessStep)
			{
				setIcon(SwingUi.stepIcon);
			}
			else if (value instanceof Recipe)
			{
				setIcon(SwingUi.recipeIcon);
			}

			return this;
		}
	}

	/*-------------------------------------------------------------------------*/
	private class StepsTree extends JTree
	{
		public StepsTree(TreeModel stepsTreeModel)
		{
			super(stepsTreeModel);
		}

		@Override
		public String convertValueToText(Object value, boolean selected,
			boolean expanded, boolean leaf, int row, boolean hasFocus)
		{
			if (value instanceof Recipe)
			{
				return recipe.getName();
			}
			else if (value instanceof ProcessStep)
			{
				return ((ProcessStep)value).describe(recipe.getVolumes());
			}
			else
			{
				throw new BrewdayException("Invalid node type " + value.getClass());
			}
		}
	}
}
