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
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.database.Database;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.*;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class RecipesPanel extends EditorPanel implements TreeSelectionListener
{
	private Recipe recipe;

	private JTabbedPane tabs;
	private JTextArea stepsEndResult;

	// recipe tab
	private RecipeComponent recipeComponent;

	// advanced tab
	private JButton addStep, removeStep, addIng, removeIng;
	private JPanel stepCards;
	private CardLayout stepCardLayout;
	private ProcessStepPanel mashInfusionPanel, batchSpargePanel, boilPanel,
		coolPanel, dilutePanel, fermentPanel, mashPanel,
		standPanel, packagePanel, splitByPercentPanel;
	private FermentableAdditionPanel fermentableAdditionPanel;
	private HopAdditionPanel hopAdditionPanel;
	private WaterAdditionPanel waterAdditionPanel;
	private YeastAdditionPanel yeastAdditionPanel;
	private JTree stepsTree;
	private StepsTreeModel stepsTreeModel;

	/*-------------------------------------------------------------------------*/
	public RecipesPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected Container getEditControls()
	{
		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));

		tabs = new JTabbedPane();

		tabs.add("Recipe", getRecipeTab());
		tabs.add("Advanced", getStepsTab());

		stepsEndResult = new JTextArea();
		stepsEndResult.setWrapStyleWord(true);
		stepsEndResult.setLineWrap(true);
		stepsEndResult.setEditable(false);

		result.add(tabs);
		result.add(stepsEndResult);

		return result;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel getRecipeTab()
	{
		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));

		recipeComponent = new RecipeComponent();
		result.add(recipeComponent);

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

		addStep = new JButton("Add Step");
		addStep.addActionListener(this);
		removeStep = new JButton("Remove Step");
		removeStep.addActionListener(this);

		addIng = new JButton("Add Ingredient");
		addIng.addActionListener(this);
		removeIng = new JButton("Remove Ingredient");
		removeIng.addActionListener(this);

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new MigLayout());

		buttonsPanel.add(addStep, "align center");
		buttonsPanel.add(removeStep, "align center, wrap");
		buttonsPanel.add(addIng, "align center");
		buttonsPanel.add(removeIng, "align center, wrap");

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

		fermentableAdditionPanel = new FermentableAdditionPanel();
		hopAdditionPanel = new HopAdditionPanel();
		waterAdditionPanel = new WaterAdditionPanel();
		yeastAdditionPanel = new YeastAdditionPanel();
		stepCards.add(IngredientAddition.Type.HOPS.toString(), hopAdditionPanel);
		stepCards.add(IngredientAddition.Type.FERMENTABLES.toString(), fermentableAdditionPanel);
		stepCards.add(IngredientAddition.Type.WATER.toString(), waterAdditionPanel);
		stepCards.add(IngredientAddition.Type.YEAST.toString(), yeastAdditionPanel);

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
		stepsEndResult.setText("");
		stepsTree.clearSelection();

		refresh(Database.getInstance().getRecipes().get(name));
	}

	/*-------------------------------------------------------------------------*/
	protected void refresh(Recipe newRecipe)
	{
		recipe = newRecipe;

		recipe.run();

		refreshSteps();
		refreshEndResult();

		stepsTree.setSelectionPaths(new TreePath[]{new TreePath(recipe)});
		stepsTree.requestFocusInWindow();

		recipeComponent.refresh(recipe);
	}

	/*-------------------------------------------------------------------------*/
	protected void runRecipe()
	{
		recipe.run();
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshSteps()
	{
		refreshStepCards();

		stepsTreeModel.fireFullRefresh();
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshEndResult()
	{
		stepsEndResult.setText("");

		StringBuilder sb = new StringBuilder("End Result:\n");

		if (recipe.getErrors().size() > 0)
		{
			sb.append("\nERRORS:\n");
			for (String s : recipe.getErrors())
			{
				sb.append(s);
				sb.append('\n');
			}
		}

		if (recipe.getWarnings().size() > 0)
		{
			sb.append("\nWarnings:\n");
			for (String s : recipe.getWarnings())
			{
				sb.append(s);
				sb.append('\n');
			}
		}

		if (recipe.getVolumes().getOutputVolumes().size() > 0)
		{
			for (String s : recipe.getVolumes().getOutputVolumes())
			{
				FluidVolume v = (FluidVolume)recipe.getVolumes().getVolume(s);

				sb.append(String.format("\n'%s' (%.1fl)\n", v.getName(), v.getVolume() / 1000));
				if (v instanceof BeerVolume)
				{
					sb.append(String.format("OG %.3f\n", ((BeerVolume)v).getOriginalGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
					sb.append(String.format("FG %.3f\n", v.getGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
				}
				sb.append(String.format("%.1f%% ABV\n", v.getAbv()));
				sb.append(String.format("%.0f IBU\n", v.getBitterness()));
				sb.append(String.format("%.1f SRM\n", v.getColour()));
			}

		}
		else
		{
			sb.append("\nNo output volumes\n");
		}
		stepsEndResult.setText(sb.toString());
	}

	@Override
	public void commit(String name)
	{

	}

	@Override
	public Collection<String> loadData()
	{
		return Database.getInstance().getRecipes().keySet();
	}

	@Override
	public void newItem(String name)
	{
		Volumes volumes = new Volumes();

		ArrayList<ProcessStep> steps = new ArrayList<ProcessStep>();
		Recipe recipe = new Recipe(name, steps, volumes);
		Database.getInstance().getRecipes().put(recipe.getName(), recipe);
	}

	@Override
	public void renameItem(String newName)
	{

	}

	@Override
	public void copyItem(String newName)
	{

	}

	@Override
	public void deleteItem()
	{

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
			else if (last instanceof IngredientAddition)
			{
				refreshStepCards((IngredientAddition)last);
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

	private void refreshStepCards(IngredientAddition item)
	{
		if (item != null)
		{
			switch (item.getType())
			{
				case HOPS:
					hopAdditionPanel.refresh(item);
					break;
				case FERMENTABLES:
					fermentableAdditionPanel.refresh(item);
					break;
				case WATER:
					waterAdditionPanel.refresh(item);
					break;
				case YEAST:
					yeastAdditionPanel.refresh(item);
					break;
				default:
					throw new BrewdayException("Invalid: [" + item.getType() + "]");
			}

			stepCardLayout.show(stepCards, item.getType().toString());
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
			AddProcessStepDialog dialog = new AddProcessStepDialog(SwingUi.instance, "Add Process Step", recipe);

			ProcessStep newProcessStep = dialog.getResult();
			if (newProcessStep != null)
			{
				recipe.getSteps().add(newProcessStep);
				runRecipe();
				refreshSteps();
				refreshEndResult();
			}
		}
		else if (e.getSource() == removeStep)
		{
			Object obj = stepsTree.getLastSelectedPathComponent();
			if (obj != null && obj instanceof ProcessStep)
			{
				recipe.getSteps().remove(obj);
				runRecipe();
				refreshSteps();
				refreshEndResult();
			}
		}
		else if (e.getSource() == addIng)
		{
			Object obj = stepsTree.getLastSelectedPathComponent();
			if (obj != null)
			{
				ProcessStep step;
				if (obj instanceof ProcessStep)
				{
					step = (ProcessStep)obj;
				}
				else if (obj instanceof IngredientAddition)
				{
					step = (ProcessStep)stepsTree.getSelectionPath().getPath()[1];
				}
				else
				{
					return;
				}

				if (step.getSupportedIngredientAdditions().size() > 0)
				{
/*
					AddIngredientDialog dialog = new AddIngredientDialog(
						SwingUi.instance,
						"Add Ingredient",
						recipe);

					Volume v = dialog.getResult();
					if (v != null)
					{
						recipe.getVolumes().addInputVolume(v.getName(), v);
						AdditionSchedule schedule = step.addIngredientAddition(v, 60);
						runRecipe();

						stepsTreeModel.fireNodeChanged(step);
						stepsTree.setSelectionPath(new TreePath(new Object[]{recipe, step, schedule}));

						refreshStepCards();
						refreshEndResult();
					}
*/
				}
			}
		}
		else if (e.getSource() == removeIng)
		{
			Object obj = stepsTree.getLastSelectedPathComponent();
			if (obj != null && obj instanceof IngredientAddition)
			{
/*
				IngredientAddition schedule = (IngredientAddition)obj;
				ProcessStep step = (ProcessStep)stepsTree.getSelectionPath().getPath()[1];

				step.removeIngredientAddition(volume);
				recipe.getVolumes().removeInputVolume(volume);

				runRecipe();

				stepsTree.setSelectionPath(new TreePath(new Object[]{recipe, step}));
				stepsTreeModel.fireNodeChanged(step);
				stepsTree.expandPath(new TreePath(new Object[]{recipe, step}));

				refreshStepCards();
				refreshEndResult();
*/
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
			else if (selected instanceof IngredientAddition)
			{
				refreshStepCards((IngredientAddition)selected);
			}
			else
			{
				refreshStepCards((ProcessStep)null);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	class BatchesListModel<T> extends AbstractListModel
	{
		List<T> data;

		public BatchesListModel(List<T> data)
		{
			this.data = data;
		}

		public Object getElementAt(int index)
		{
			T t = data.get(index);
			String s;

			if (t instanceof Volume)
			{
				s = ((Volume)t).getName();
			}
			else if (t instanceof ProcessStep)
			{
				s = ((ProcessStep)t).describe(RecipesPanel.this.recipe.getVolumes());
			}
			else
			{
				s = t.getClass().getSimpleName();
			}
			if (s.length() > 75)
			{
				s = s.substring(0, 73) + "...";
			}
			return s;
		}

		public int getSize()
		{
			return data.size();
		}

		public void add(T vol)
		{
			data.add(vol);
			fireContentsChanged(this, data.size(), data.size());
		}

		public void remove(int index)
		{
			data.remove(index);
			fireIntervalRemoved(this, index, index);
		}

		public void update(T t, int index)
		{
			data.set(index, t);
			fireContentsChanged(this, index, index);
		}

		public void moveUp(int index)
		{
			if (index > 0)
			{
				T t = data.remove(index);
				data.add(index - 1, t);
				fireContentsChanged(this, index - 1, index);
			}
		}

		public void moveDown(int index)
		{
			if (index < data.size() - 1)
			{
				T t = data.remove(index);
				data.add(index + 1, t);
				fireContentsChanged(this, index, index + 1);
			}
		}

		public void clear()
		{
			int size = data.size();
			data.clear();
			fireContentsChanged(this, 0, size - 1);
		}
	}

	/*-------------------------------------------------------------------------*/
	private class StepsTreeModel implements TreeModel
	{
		private List<TreeModelListener> treeModelListeners = new ArrayList<TreeModelListener>();

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
				if (((ProcessStep)parent).getSupportedIngredientAdditions().size() > 0)
				{
					return ((ProcessStep)parent).getIngredients().get(index);
				}
				else
				{
					return 0;
				}
			}
			else if (parent instanceof IngredientAddition)
			{
				return null;
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
				if (((ProcessStep)parent).getSupportedIngredientAdditions().size() > 0)
				{
					return ((ProcessStep)parent).getIngredients().size();
				}
				else
				{
					return 0;
				}
			}
			else if (parent instanceof IngredientAddition)
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
				return ((ProcessStep)node).getSupportedIngredientAdditions().isEmpty();
			}
			else if (node instanceof IngredientAddition)
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
			else if (value instanceof IngredientAddition)
			{
				IngredientAddition item = (IngredientAddition)value;

				if (item instanceof HopAddition)
				{
					return String.format("%s - %.0fg (%.0f min)",
						item.getName(),
						item.getWeight(),
						item.getTime());
				}
				else if (item instanceof FermentableAddition)
				{
					return String.format("%s - %.1fkg (%.0f min)",
						item.getName(),
						item.getWeight() /1000,
						item.getTime());
				}
				else if (item instanceof YeastAddition)
				{
					return String.format("%s - %.0fg (%.0f d)",
						item.getName(),
						item.getWeight(),
						item.getTime());
				}
				else
				{
					return String.format("%s (%.0f min)",
						item.getName(),
						item.getTime());
				}
			}
			else
			{
				throw new BrewdayException("Invalid node type " + value.getClass());
			}
		}
	}
}
