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

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.database.Database;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.FermentableAdditionList;
import mclachlan.brewday.recipe.HopAdditionList;

/**
 *
 */
public class RecipesPanel extends EditorPanel implements TreeSelectionListener
{
	private Recipe recipe;

	private JTabbedPane tabs;

	// ingredient tab
	private JList<Volume> ingredients;
	private BatchesListModel<Volume> ingredientsModel;
	private JButton addIngredient, removeIngredient;
	private JPanel ingredientCards;
	private CardLayout ingredientCardLayout;
	private FermentableAdditionPanel fermentableAdditionPanel;
	private HopAdditionPanel hopAdditionPanel;
	private WaterPanel waterPanel;
	private JTextArea ingredientEndResult;

	// steps tab
	private JButton addStep, removeStep;
	private JPanel stepCards;
	private CardLayout stepCardLayout;
	private ProcessStepPanel mashInfusionPanel, batchSpargePanel, boilPanel, coolPanel, dilutePanel, fermentPanel, mashInPanel, mashOutPanel, standPanel, packagePanel;
	private JTextArea stepsEndResult;
	private JTree stepsTree;
	private StepsTreeModel stepsTreeModel;

	// computed volumes tab
	private JList<Volume> computedVolumes;
	private BatchesListModel<Volume> computedVolumesModel;
	private ComputedVolumePanel computedVolumePanel;

	/*-------------------------------------------------------------------------*/
	public RecipesPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected Container getEditControls()
	{
		tabs = new JTabbedPane();

		tabs.add("Ingredients", getIngredientsTab());
		tabs.add("Steps", getStepsTab());
		tabs.add("Computed Volumes", getComputedVolumesTab());

		return tabs;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel getComputedVolumesTab()
	{
		computedVolumesModel = new BatchesListModel<Volume>(new ArrayList<Volume>());

		computedVolumes = new JList<Volume>(computedVolumesModel);
		computedVolumes.addListSelectionListener(new RecipesPanelListSelectionListener());

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new JScrollPane(computedVolumes));

		computedVolumePanel = new ComputedVolumePanel("Details:");
		panel.add(computedVolumePanel);

		JPanel computedVolumesTab = new JPanel();
		computedVolumesTab.setLayout(new BoxLayout(computedVolumesTab, BoxLayout.X_AXIS));
		return panel;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel getStepsTab()
	{
		stepsTreeModel = new StepsTreeModel();
		stepsTree = new JTree(stepsTreeModel)
		{
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
					throw new BrewdayException("Invalid node type "+value.getClass());
				}
			}
		};
		stepsTree.addTreeSelectionListener(this);

		addStep = new JButton("Add");
		addStep.addActionListener(this);
		removeStep = new JButton("Remove");
		removeStep.addActionListener(this);

		JPanel stepsButtons = new JPanel();
		stepsButtons.add(addStep);
		stepsButtons.add(removeStep);

		JPanel stepsPanel = new JPanel();
		stepsPanel.setLayout(new BoxLayout(stepsPanel, BoxLayout.Y_AXIS));
//		stepsPanel.add(new JScrollPane(steps));
		stepsPanel.add(new JScrollPane(stepsTree));
		stepsPanel.add(stepsButtons);

		stepCardLayout = new CardLayout();
		stepCards = new JPanel(stepCardLayout);

		batchSpargePanel = new BatchSpargePanel(dirtyFlag);
		boilPanel = new BoilPanel(dirtyFlag);
		coolPanel = new CoolPanel(dirtyFlag);
		dilutePanel = new DilutePanel(dirtyFlag);
		fermentPanel = new FermentPanel(dirtyFlag);
		mashInPanel = new MashInPanel(dirtyFlag);
		mashOutPanel = new MashOutPanel(dirtyFlag);
		standPanel = new StandPanel(dirtyFlag);
		packagePanel = new PackagePanel(dirtyFlag);
		mashInfusionPanel = new MashInfusionPanel(dirtyFlag);

		stepCards.add(EditorPanel.NONE, new JPanel());
		stepCards.add(ProcessStep.Type.BATCH_SPARGE.toString(), batchSpargePanel);
		stepCards.add(ProcessStep.Type.BOIL.toString(), boilPanel);
		stepCards.add(ProcessStep.Type.COOL.toString(), coolPanel);
		stepCards.add(ProcessStep.Type.DILUTE.toString(), dilutePanel);
		stepCards.add(ProcessStep.Type.FERMENT.toString(), fermentPanel);
		stepCards.add(ProcessStep.Type.MASH_IN.toString(), mashInPanel);
		stepCards.add(ProcessStep.Type.MASH_OUT.toString(), mashOutPanel);
		stepCards.add(ProcessStep.Type.STAND.toString(), standPanel);
		stepCards.add(ProcessStep.Type.PACKAGE.toString(), packagePanel);
		stepCards.add(ProcessStep.Type.MASH_INFUSION.toString(), mashInfusionPanel);

		stepsEndResult = new JTextArea();
		stepsEndResult.setWrapStyleWord(true);
		stepsEndResult.setLineWrap(true);
		stepsEndResult.setEditable(false);

		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));
		result.add(stepsPanel);
		result.add(stepCards);
		result.add(stepsEndResult);

		return result;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel getIngredientsTab()
	{
		ingredientsModel = new BatchesListModel<Volume>(new ArrayList<Volume>());

		addIngredient = new JButton("Add");
		addIngredient.addActionListener(this);
		removeIngredient = new JButton("Remove");
		removeIngredient.addActionListener(this);

		JPanel volsButtons = new JPanel();
		volsButtons.add(addIngredient);
		volsButtons.add(removeIngredient);

		ingredients = new JList<Volume>(ingredientsModel);
		ingredients.addListSelectionListener(new RecipesPanelListSelectionListener());

		JPanel ingredientsPanel = new JPanel();
		ingredientsPanel.setLayout(new BoxLayout(ingredientsPanel, BoxLayout.Y_AXIS));
		ingredientsPanel.add(new JScrollPane(ingredients));
		ingredientsPanel.add(volsButtons);

		ingredientCardLayout = new CardLayout();
		ingredientCards = new JPanel(ingredientCardLayout);

		fermentableAdditionPanel = new FermentableAdditionPanel();
		hopAdditionPanel = new HopAdditionPanel();
		waterPanel = new WaterPanel();

		ingredientCards.add(EditorPanel.NONE, new JPanel());
		ingredientCards.add(Volume.Type.FERMENTABLES.toString(), fermentableAdditionPanel);
		ingredientCards.add(Volume.Type.HOPS.toString(), hopAdditionPanel);
		ingredientCards.add(Volume.Type.WATER.toString(), waterPanel);
		// todo yeast, carbonation sugars

		ingredientEndResult = new JTextArea();
		ingredientEndResult.setWrapStyleWord(true);
		ingredientEndResult.setLineWrap(true);
		ingredientEndResult.setEditable(false);

		JPanel result = new JPanel();

		result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));
		result.add(ingredientsPanel);
		result.add(ingredientCards);
		result.add(ingredientEndResult);

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void refresh(String name)
	{
		ingredientsModel.clear();
		ingredientEndResult.setText("");
		ingredients.clearSelection();

		stepsEndResult.setText("");
		stepsTree.clearSelection();

		refresh(Database.getInstance().getBatches().get(name));
	}

	/*-------------------------------------------------------------------------*/
	protected void refresh(Recipe newRecipe)
	{
		recipe = newRecipe;

		refreshIngredients();
		refreshSteps();
	}

	/*-------------------------------------------------------------------------*/
	private void refreshIngredients()
	{
		for (Volume v : recipe.getVolumes().getVolumes().values())
		{
			if (recipe.getVolumes().getInputVolumes().contains(v.getName()))
			{
				ingredientsModel.add(v);
			}
		}
		Collections.sort(ingredientsModel.data, new VolumesComparator());

		refreshIngredientCards();
		refreshComputedVolumes();

		if (ingredientsModel.getSize() > 0)
		{
			ingredients.setSelectedIndex(0);
			refreshIngredientCards();
		}
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshComputedVolumes()
	{
		runRecipe();

		computedVolumesModel.clear();

		for (Volume v : recipe.getVolumes().getVolumes().values())
		{
			if (!recipe.getVolumes().getInputVolumes().contains(v.getName()))
			{
				computedVolumesModel.add(v);
			}
		}
		Collections.sort(computedVolumesModel.data, new VolumesComparator());

		if (computedVolumesModel.getSize() > 0)
		{
			computedVolumes.setSelectedIndex(0);
		}


		refreshEndResult();

		ingredients.repaint();

		refreshSteps();
	}

	protected void runRecipe()
	{
		recipe.run();
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshSteps()
	{
		TreePath selected = stepsTree.getSelectionPath();

		if (recipe.getSteps().size() > 0)
		{
			if (selected == null)
			{
				stepsTree.clearSelection();
			}
			else
			{
				stepsTree.setSelectionPaths(new TreePath[]{selected});
			}
		}

		if (selected != null)
		{
			refreshStepCards((ProcessStep)selected.getLastPathComponent());
		}
		else
		{
			refreshStepCards(null);
		}

		stepsTreeModel.fireRefresh();
		stepsTree.repaint();
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshEndResult()
	{
		ingredientEndResult.setText("");
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
				sb.append(String.format("%.1f%% ABV\n", v.getAbv()));
				sb.append(String.format("%.0f IBU\n", v.getBitterness()));
				sb.append(String.format("%.1f SRM\n", v.getColour()));
			}

		}
		else
		{
			sb.append("\nNo output volumes\n");
		}
		ingredientEndResult.setText(sb.toString());
		stepsEndResult.setText(sb.toString());
	}

	@Override
	public void commit(String name)
	{

	}

	@Override
	public Collection<String> loadData()
	{
		return Database.getInstance().getBatches().keySet();
	}

	@Override
	public void newItem(String name)
	{
		Volumes volumes = new Volumes();
//		volumes.addInputVolume("mash water", new WaterAddition("mash water", 20, 66));
//		volumes.addInputVolume("grain bill", new FermentableAdditionList("grain bill"));

		ArrayList<ProcessStep> steps = new ArrayList<ProcessStep>();
		Recipe recipe = new Recipe(name, steps, volumes);
		Database.getInstance().getBatches().put(recipe.getName(), recipe);
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
				case MASH_IN:
					mashInPanel.refresh(step, recipe);
					break;
				case MASH_OUT:
					mashOutPanel.refresh(step, recipe);
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

	private void refreshIngredientCards()
	{
		int selectedIndex = ingredients.getSelectedIndex();
		if (selectedIndex > -1)
		{
			Volume selected = ingredientsModel.data.get(selectedIndex);

			if (selected instanceof FermentableAdditionList)
			{
				fermentableAdditionPanel.refresh((FermentableAdditionList)selected, recipe);
				ingredientCardLayout.show(ingredientCards, Volume.Type.FERMENTABLES.toString());
			}
			else if (selected instanceof HopAdditionList)
			{
				hopAdditionPanel.refresh((HopAdditionList)selected, recipe);
				ingredientCardLayout.show(ingredientCards, Volume.Type.HOPS.toString());
			}
			else if (selected instanceof WaterAddition)
			{
				waterPanel.refresh((WaterAddition)selected, recipe);
				ingredientCardLayout.show(ingredientCards, Volume.Type.WATER.toString());
			}
			else
			{
				throw new BrewdayException("Invalid input volume: " + selected);
			}
		}
		else
		{
			ingredientCardLayout.show(ingredientCards, EditorPanel.NONE);
		}
	}

	private void listListener(EventObject e)
	{
		if (e.getSource() == ingredients && ingredientsModel.getSize() > 0)
		{
			refreshIngredientCards();
		}
		else
		{
			if (e.getSource() == computedVolumes && computedVolumesModel.getSize() > 0)
			{
				int selectedIndex = computedVolumes.getSelectedIndex();
				String v = (String)computedVolumesModel.getElementAt(selectedIndex);
				computedVolumePanel.refresh(v, recipe);
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		listListener(e);
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		listListener(e);
	}

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
				refreshSteps();
				refreshEndResult();
			}
		}
		else if (e.getSource() == addIngredient)
		{
			AddIngredientDialog dialog = new AddIngredientDialog(SwingUi.instance, "Add Ingredient Volume", recipe);

			Volume v = dialog.getResult();
			if (v != null)
			{
				recipe.getVolumes().addInputVolume(v.getName(), v);
				ingredientsModel.add(v);
				ingredients.setSelectedIndex(ingredientsModel.data.indexOf(v));
				refreshIngredientCards();
			}
		}
		else if (e.getSource() == removeIngredient)
		{
			int selectedIndex = ingredients.getSelectedIndex();
			if (selectedIndex > -1)
			{
				Volume selected = ingredientsModel.data.get(selectedIndex);

				recipe.getVolumes().removeInputVolume(selected);
				ingredientsModel.remove(selectedIndex);
				refreshIngredientCards();
				refreshEndResult();
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

	private class VolumesComparator implements Comparator<Volume>
	{
		@Override
		public int compare(Volume o1, Volume o2)
		{
			return o1.getType().getSortOrder() - o2.getType().getSortOrder();
		}
	}

	private class RecipesPanelListSelectionListener implements ListSelectionListener
	{
		@Override
		public void valueChanged(ListSelectionEvent e)
		{
			listListener(e);
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
				// todo: additions
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
				// todo: additions
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
				// todo: additions
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
				// todo: additions
				return -1;
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

		protected void fireRefresh()
		{
			TreeModelEvent e = new TreeModelEvent(this, new Object[]{recipe});
			for (TreeModelListener tml : treeModelListeners)
			{
				tml.treeStructureChanged(e);
			}
		}
	}
}
