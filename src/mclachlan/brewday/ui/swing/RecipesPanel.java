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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.database.Database;
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

	// steps tab
	private JButton addStep, removeStep, addIng, removeIng;
	private JPanel stepCards;
	private CardLayout stepCardLayout;
	private ProcessStepPanel mashInfusionPanel, batchSpargePanel, boilPanel,
		coolPanel, dilutePanel, fermentPanel, mashInPanel, mashOutPanel,
		standPanel, packagePanel, splitByPercentPanel;
	private FermentableAdditionPanel fermentableAdditionPanel;
	private HopAdditionPanel hopAdditionPanel;
	private WaterPanel waterPanel;
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

		tabs.add("Recipe", getStepsTab());
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
		mashInPanel = new MashPanel(dirtyFlag);
		mashOutPanel = new MashOutPanel(dirtyFlag);
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
		stepCards.add(ProcessStep.Type.MASH.toString(), mashInPanel);
		stepCards.add(ProcessStep.Type.FIRST_RUNNING.toString(), mashOutPanel);
		stepCards.add(ProcessStep.Type.STAND.toString(), standPanel);
		stepCards.add(ProcessStep.Type.PACKAGE.toString(), packagePanel);
		stepCards.add(ProcessStep.Type.MASH_INFUSION.toString(), mashInfusionPanel);
		stepCards.add(ProcessStep.Type.SPLIT_BY_PERCENT.toString(), splitByPercentPanel);

		fermentableAdditionPanel = new FermentableAdditionPanel();
		hopAdditionPanel = new HopAdditionPanel();
		waterPanel = new WaterPanel();
		stepCards.add(Volume.Type.HOPS.toString(), hopAdditionPanel);
		stepCards.add(Volume.Type.FERMENTABLES.toString(), fermentableAdditionPanel);
		stepCards.add(Volume.Type.WATER.toString(), waterPanel);

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
	@Override
	public void refresh(String name)
	{
		stepsEndResult.setText("");
		stepsTree.clearSelection();

		refresh(Database.getInstance().getBatches().get(name));
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

		refreshStepCards();
	}

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
		return Database.getInstance().getBatches().keySet();
	}

	@Override
	public void newItem(String name)
	{
		Volumes volumes = new Volumes();

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
			else if (last instanceof AdditionSchedule)
			{
				refreshStepCards((AdditionSchedule)last);
			}
			else
			{
				refreshStepCards((ProcessStep)null);
			}
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
					mashInPanel.refresh(step, recipe);
					break;
				case FIRST_RUNNING:
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

	private void refreshStepCards(AdditionSchedule schedule)
	{
		if (schedule != null)
		{
			Volume volume = recipe.getVolumes().getVolume(schedule.getIngredientAddition());

			switch (volume.getType())
			{
				case HOPS:
					hopAdditionPanel.refresh(schedule, recipe);
					break;
				case FERMENTABLES:
					fermentableAdditionPanel.refresh(schedule, recipe);
					break;
				case WATER:
					waterPanel.refresh(schedule, recipe);
					break;
				default:
					throw new BrewdayException("Invalid: [" + volume.getType() + "]");
			}

			stepCardLayout.show(stepCards, volume.getType().toString());
		}
		else
		{
			stepCardLayout.show(stepCards, EditorPanel.NONE);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void listListener(EventObject e)
	{
		if (e.getSource() == computedVolumes && computedVolumesModel.getSize() > 0)
		{
			int selectedIndex = computedVolumes.getSelectedIndex();
			String v = (String)computedVolumesModel.getElementAt(selectedIndex);
			computedVolumePanel.refresh(v, recipe);
		}
	}

	/*-------------------------------------------------------------------------*/
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
				else if (obj instanceof AdditionSchedule)
				{
					step = (ProcessStep)stepsTree.getSelectionPath().getPath()[1];
				}
				else
				{
					return;
				}

				if (step.getSupportedIngredientAdditions().size() > 0)
				{
					AddIngredientDialog dialog = new AddIngredientDialog(
						SwingUi.instance,
						"Add Ingredient",
						recipe);

					Volume v = dialog.getResult();
					if (v != null)
					{
						recipe.getVolumes().addInputVolume(v.getName(), v);
						AdditionSchedule schedule = step.addIngredientAddition(v);
						runRecipe();

						stepsTreeModel.fireNodeChanged(step);
						stepsTree.setSelectionPath(new TreePath(new Object[]{recipe, step, schedule}));

						refreshStepCards();
						refreshEndResult();
					}
				}
			}
		}
		else if (e.getSource() == removeIng)
		{
			Object obj = stepsTree.getLastSelectedPathComponent();
			if (obj != null && obj instanceof AdditionSchedule)
			{
				AdditionSchedule schedule = (AdditionSchedule)obj;
				ProcessStep step = (ProcessStep)stepsTree.getSelectionPath().getPath()[1];
				Volume volume = recipe.getVolumes().getVolume(schedule.getIngredientAddition());

				step.removeIngredientAddition(volume);
				recipe.getVolumes().removeInputVolume(volume);

				runRecipe();

				stepsTree.setSelectionPath(new TreePath(new Object[]{recipe, step}));
				stepsTreeModel.fireNodeChanged(step);
				stepsTree.expandPath(new TreePath(new Object[]{recipe, step}));

				refreshStepCards();
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
			else if (selected instanceof AdditionSchedule)
			{
				refreshStepCards((AdditionSchedule)selected);
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

	/**
	 * Returns an ImageIcon, or null if the path was invalid.
	 */
	protected ImageIcon createImageIcon(String path)
	{
		Image image = Toolkit.getDefaultToolkit().getImage(path);
		Image scaledInstance = image.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
		return new ImageIcon(scaledInstance);
	}

	/*-------------------------------------------------------------------------*/
	private class VolumesComparator implements Comparator<Volume>
	{
		@Override
		public int compare(Volume o1, Volume o2)
		{
			return o1.getType().getSortOrder() - o2.getType().getSortOrder();
		}
	}

	/*-------------------------------------------------------------------------*/
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
				if (((ProcessStep)parent).getSupportedIngredientAdditions().size() > 0)
				{
					return ((ProcessStep)parent).getIngredientAdditions().get(index);
				}
				else
				{
					return 0;
				}
			}
			else if (parent instanceof AdditionSchedule)
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
					return ((ProcessStep)parent).getIngredientAdditions().size();
				}
				else
				{
					return 0;
				}
			}
			else if (parent instanceof AdditionSchedule)
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
			else if (node instanceof AdditionSchedule)
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
		private ImageIcon grainsIcon;
		private ImageIcon hopsIcon;
		private ImageIcon waterIcon;
		private ImageIcon stepIcon;
		private ImageIcon recipeIcon;

		private StepsTreeCellRenderer()
		{
			recipeIcon = createImageIcon("img/icons8-beer-recipe-48.png");
			stepIcon = createImageIcon("img/icons8-file-48.png");
			hopsIcon = createImageIcon("img/icons8-hops-48.png");
			grainsIcon = createImageIcon("img/icons8-carbohydrates-48.png");
			waterIcon = createImageIcon("img/icons8-water-48.png");
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

			if (value instanceof AdditionSchedule)
			{
				String ingredientAddition = ((AdditionSchedule)value).getIngredientAddition();

				if (recipe.getVolumes().contains(ingredientAddition))
				{
					Volume v = recipe.getVolumes().getVolume(ingredientAddition);

					switch (v.getType())
					{
						case FERMENTABLES:
							setIcon(grainsIcon);
							break;
						case HOPS:
							setIcon(hopsIcon);
							break;
						case WATER:
							setIcon(waterIcon);
							break;
						case YEAST:
							// todo
							break;
						case MASH:
							// todo
							break;
						case WORT:
							// todo
							break;
						case BEER:
							// todo
							break;
					}
				}
			}
			else if (value instanceof ProcessStep)
			{
				setIcon(stepIcon);
			}
			else if (value instanceof Recipe)
			{
				setIcon(recipeIcon);
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
			else if (value instanceof AdditionSchedule)
			{
				AdditionSchedule as = (AdditionSchedule)value;

				Volume volume = recipe.getVolumes().getVolume(as.getIngredientAddition());

				if (volume instanceof HopAdditionList)
				{
					return String.format("%s - %.0fg (%.0f min)",
						volume.getName(),
						((HopAdditionList)volume).getCombinedWeight(),
						as.getTime());
				}
				else if (volume instanceof FermentableAdditionList)
				{
					return String.format("%s - %.1fkg (%.0f min)",
						volume.getName(),
						((FermentableAdditionList)volume).getCombinedWeight() /1000,
						as.getTime());
				}
				else
				{
					return String.format("%s (%.0f min)",
						volume.getName(),
						as.getTime());
				}
			}
			else
			{
				throw new BrewdayException("Invalid node type " + value.getClass());
			}
		}
	}
}
