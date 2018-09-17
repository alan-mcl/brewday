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
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.database.Database;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.FermentableAdditionList;
import mclachlan.brewday.recipe.HopAdditionList;

/**
 *
 */
public class BatchesPanel extends EditorPanel
{
	private static final String FERMENTABLES = "fermentables";
	private static final String HOPS = "HOPS";

	private JList<ProcessStep> steps;
	private JList<Volume> ingredients;
	private JList<Volume> computedVolumes;
	private BatchesListModel<ProcessStep> stepsModel;
	private BatchesListModel<Volume> ingredientsModel, computedVolumesModel;
	private JButton addStep, removeStep, editStep;
	private JButton addIngredient, removeIngredient, editIngredient;

	private JTabbedPane tabs;

	private BatchSpargePanel batchSpargePanel;

	// todo other step panels
	private ProcessStepPanel boilPanel, coolPanel, dilutePanel, fermentPanel,
	mashInPanel, mashOutPanel, standPanel;
	private Batch batch;
	private JTextArea stepText, ingredientText, ingredientEndResult;

	// ingredient panels
	private JPanel ingredientCards;
	private CardLayout ingredientCardLayout;
	private FermentableAdditionPanel fermentableAdditionPanel;
	private HopAdditionPanel hopAdditionPanel;

	public BatchesPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected Container getEditControls()
	{
		tabs = new JTabbedPane();

		stepsModel = new BatchesListModel<ProcessStep>(new ArrayList<ProcessStep>());
		computedVolumesModel = new BatchesListModel<Volume>(new ArrayList<Volume>());

		steps = new JList<ProcessStep>(stepsModel);
		steps.addMouseListener(this);
		computedVolumes = new JList<Volume>(computedVolumesModel);
		computedVolumes.addMouseListener(this);

		addStep = new JButton("Add");
		addStep.addActionListener(this);
		editStep = new JButton("Edit");
		editStep.addActionListener(this);
		removeStep = new JButton("Remove");
		removeStep.addActionListener(this);

		JPanel stepsButtons = new JPanel();
		stepsButtons.add(addStep);
		stepsButtons.add(editStep);
		stepsButtons.add(removeStep);

		JPanel stepsPanel = new JPanel(new BorderLayout());
		stepsPanel.add(steps, BorderLayout.CENTER);
		stepsPanel.add(stepsButtons, BorderLayout.SOUTH);

		JPanel computedVolumesPanel = new JPanel();
		computedVolumesPanel.setLayout(new BoxLayout(computedVolumesPanel, BoxLayout.Y_AXIS));
		computedVolumesPanel.add(new JScrollPane(computedVolumes));

		stepText = new JTextArea();
		stepText.setWrapStyleWord(true);
		stepText.setLineWrap(true);
		stepText.setEditable(false);

		JScrollPane scrollerSteps = new JScrollPane(stepsPanel);

		JPanel stepsTab = new JPanel(new BorderLayout());
		stepsTab.add(scrollerSteps, BorderLayout.WEST);
		stepsTab.add(stepText, BorderLayout.CENTER);

		JPanel computedVolumesTab = new JPanel();
		computedVolumesTab.setLayout(new BoxLayout(computedVolumesTab, BoxLayout.X_AXIS));

		tabs.add("Ingredients", getIngredientsTab());
		tabs.add("Steps", stepsTab);
		tabs.add("Computed Volumes", computedVolumesPanel);

		return tabs;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel getIngredientsTab()
	{
		ingredientsModel = new BatchesListModel<Volume>(new ArrayList<Volume>());

		ingredientText = new JTextArea();
		ingredientText.setWrapStyleWord(true);
		ingredientText.setLineWrap(true);
		ingredientText.setEditable(false);

		addIngredient = new JButton("Add");
		addIngredient.addActionListener(this);
		editIngredient = new JButton("Edit");
		editIngredient.addActionListener(this);
		removeIngredient = new JButton("Remove");
		removeIngredient.addActionListener(this);

		JPanel volsButtons = new JPanel();
		volsButtons.add(addIngredient);
		volsButtons.add(editIngredient);
		volsButtons.add(removeIngredient);

		ingredients = new JList<Volume>(ingredientsModel);
		ingredients.addMouseListener(this);

		JPanel ingredientsPanel = new JPanel();
		ingredientsPanel.setLayout(new BoxLayout(ingredientsPanel, BoxLayout.Y_AXIS));
		ingredientsPanel.add(new JScrollPane(ingredients));
		ingredientsPanel.add(volsButtons);

		ingredientCardLayout = new CardLayout();
		ingredientCards = new JPanel(ingredientCardLayout);

		fermentableAdditionPanel = new FermentableAdditionPanel();
		hopAdditionPanel = new HopAdditionPanel();

		ingredientCards.add("text", ingredientText);
		ingredientCards.add(FERMENTABLES, fermentableAdditionPanel);
		ingredientCards.add(HOPS, hopAdditionPanel);

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
		refresh(Database.getInstance().getBatches().get(name));
	}

	/*-------------------------------------------------------------------------*/
	protected void refresh(Batch newBatch)
	{
		batch = newBatch;

		stepsModel.clear();
		ingredientsModel.clear();
		stepText.setText("");
		ingredientText.setText("");
		ingredientEndResult.setText("");

		for (ProcessStep ps : batch.getSteps())
		{
			stepsModel.add(ps);
		}
		for (Volume v : batch.getVolumes().getVolumes().values())
		{
			if (batch.getVolumes().getInputVolumes().contains(v.getName()))
			{
				ingredientsModel.add(v);
			}
		}
		Collections.sort(ingredientsModel.data, new VolumesComparator());

		if (stepsModel.getSize() > 0)
		{
			steps.setSelectedIndex(0);
			refreshStepText();
		}
		if (ingredientsModel.getSize() > 0)
		{
			ingredients.setSelectedIndex(0);
			refreshIngredientCards();
		}

		refreshComputedVolumes();
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshComputedVolumes()
	{
		batch.run();

		computedVolumesModel.clear();
		ingredientEndResult.setText("End Result: none");

		for (Volume v : batch.getVolumes().getVolumes().values())
		{
			if (!batch.getVolumes().getInputVolumes().contains(v.getName()))
			{
				computedVolumesModel.add(v);
			}
		}
		Collections.sort(computedVolumesModel.data, new VolumesComparator());

		if (computedVolumesModel.getSize() > 0)
		{
			computedVolumes.setSelectedIndex(0);
		}

		if (batch.getVolumes().getOutputVolumes().size() > 0)
		{
			StringBuffer sb = new StringBuffer("End Result:\n");

			for (String s : batch.getVolumes().getOutputVolumes())
			{
				FluidVolume v = (FluidVolume)batch.getVolumes().getVolume(s);

				sb.append(String.format("\n'%s' (%.1fl)\n", v.getName(), v.getVolume()/1000));
				sb.append(String.format("%.1f%% ABV\n", v.getAbv()));
				sb.append(String.format("%.0f IBU\n", v.getBitterness()));
				sb.append(String.format("%.1f SRM\n", v.getColour()));
			}

			ingredientEndResult.setText(sb.toString());
		}
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
		volumes.addInputVolume("mash water", new Water(20, 66));
		volumes.addInputVolume("grain bill", new FermentableAdditionList());

		ArrayList<ProcessStep> steps = new ArrayList<ProcessStep>();
		Batch batch = new Batch(name, steps, volumes);
		Database.getInstance().getBatches().put(batch.getName(), batch);
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

	private void refreshStepText()
	{
		int i = steps.getSelectedIndex();
		ProcessStep step = batch.getSteps().get(i);

		stepText.setText(step.describe(batch.getVolumes()));
	}

	/*-------------------------------------------------------------------------*/

	private void refreshIngredientCards()
	{
		Volume selectedIngredientVolume = ingredientsModel.data.get(ingredients.getSelectedIndex());

		if (selectedIngredientVolume instanceof FermentableAdditionList)
		{
			fermentableAdditionPanel.refresh((FermentableAdditionList)selectedIngredientVolume, batch);
			ingredientCardLayout.show(ingredientCards, FERMENTABLES);
		}
		else if (selectedIngredientVolume instanceof HopAdditionList)
		{
			hopAdditionPanel.refresh((HopAdditionList)selectedIngredientVolume, batch);
			ingredientCardLayout.show(ingredientCards, HOPS);
		}
		else
		{
			ingredientCardLayout.show(ingredientCards, "text");
			ingredientText.setText(selectedIngredientVolume.toString());
		}
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == steps && stepsModel.getSize() > 0)
		{
			refreshStepText();
		}
		else if (e.getSource() == ingredients && ingredientsModel.getSize() > 0)
		{
			refreshIngredientCards();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == addStep)
		{
			ProcessStepDialog dialog = new ProcessStepDialog(SwingUi.instance, "Add Process Step", batch);
			dialog.setVisible(true);

			ProcessStep newProcessStep = dialog.getResult();
			if (newProcessStep != null)
			{
				batch.getSteps().add(newProcessStep);
				refresh(batch);
			}
		}
		else if (e.getSource() == addIngredient)
		{
			// todo

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
				s = ((Volume)t).describe();
			}
			else if (t instanceof ProcessStep)
			{
				s = ((ProcessStep)t).describe(BatchesPanel.this.batch.getVolumes());
			}
			else
			{
				s = t.getClass().getSimpleName();
			}
			if (s.length() > 75)
			{
				s = s.substring(0, 73)+"...";
			}
			return s;
		}

		public int getSize()
		{
			return data.size();
		}

		public void add(T step)
		{
			data.add(step);
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
				data.add(index-1, t);
				fireContentsChanged(this, index-1, index);
			}
		}
		
		public void moveDown(int index)
		{
			if (index < data.size()-1)
			{
				T t = data.remove(index);
				data.add(index+1, t);
				fireContentsChanged(this, index, index+1);
			}
		}

		public void clear()
		{
			int size = data.size();
			data.clear();
			fireContentsChanged(this, 0, size-1);
		}
	}

	private class VolumesComparator implements Comparator<Volume>
	{
		List<Class> order = new ArrayList<Class>();

		private VolumesComparator()
		{
			order.add(Water.class);
			order.add(FermentableAdditionList.class);
			order.add(HopAdditionList.class);
			order.add(MashVolume.class);
			order.add(WortVolume.class);
			order.add(BeerVolume.class);
		}

		@Override
		public int compare(Volume o1, Volume o2)
		{
			int i1 = order.indexOf(o1.getClass());
			int i2 = order.indexOf(o2.getClass());

			return i1 - i2;
		}
	}
}
