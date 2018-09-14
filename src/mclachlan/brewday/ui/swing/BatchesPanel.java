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
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.database.Database;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.ingredients.GrainBill;
import mclachlan.brewday.ingredients.HopAddition;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.process.*;

/**
 *
 */
public class BatchesPanel extends EditorPanel
{
	private JList<ProcessStep> steps;
	private JList<Volume> ingredients, computedVolumes;
	private BatchesListModel<ProcessStep> stepsModel;
	private BatchesListModel<Volume> ingredientsModel, computedVolumesModel;
	private JButton addStep, removeStep, editStep;
	private JButton addIngredient, removeIngredient, editIngredient;

	private JTabbedPane tabs;

//	private JPanel middleCards;
	private BatchSpargePanel batchSpargePanel;

	// todo other step panels
	private ProcessStepPanel boilPanel, coolPanel, dilutePanel, fermentPanel,
	mashInPanel, mashOutPanel, standPanel;
//	private CardLayout middleCardLayout;
//	private Map<Class, ProcessStepPanel> stepPanels;
	private Batch batch;
	private JTextArea stepText, ingredientText;

	public BatchesPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected Container getEditControls()
	{
		tabs = new JTabbedPane();

		stepsModel = new BatchesListModel<ProcessStep>(new ArrayList<ProcessStep>());
		ingredientsModel = new BatchesListModel<Volume>(new ArrayList<Volume>());
		computedVolumesModel = new BatchesListModel<Volume>(new ArrayList<Volume>());

		steps = new JList<ProcessStep>(stepsModel);
		steps.addMouseListener(this);
		ingredients = new JList<Volume>(ingredientsModel);
		ingredients.addMouseListener(this);
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

		JPanel stepsPanel = new JPanel(new BorderLayout());
//		stepsPanel.setBorder(BorderFactory.createTitledBorder("Steps"));
		stepsPanel.add(steps, BorderLayout.CENTER);
		stepsPanel.add(stepsButtons, BorderLayout.SOUTH);

		JPanel inputVolumesPanel = new JPanel();
		inputVolumesPanel.setLayout(new BoxLayout(inputVolumesPanel, BoxLayout.Y_AXIS));
//		inputVolumesPanel.setBorder(BorderFactory.createTitledBorder("Input Volumes"));
		inputVolumesPanel.add(new JScrollPane(ingredients));
		inputVolumesPanel.add(volsButtons);

		JPanel computedVolumesPanel = new JPanel();
		computedVolumesPanel.setLayout(new BoxLayout(computedVolumesPanel, BoxLayout.Y_AXIS));
//		computedVolumesPanel.setBorder(BorderFactory.createTitledBorder("Computed Volumes"));
		computedVolumesPanel.add(new JScrollPane(computedVolumes));

/*		middleCardLayout = new CardLayout();
		middleCards = new JPanel(middleCardLayout);
		batchSpargePanel = new BatchSpargePanel(false);
		boilPanel = new BoilPanel(false);
		coolPanel = new ProcessStepPanel(false);
		dilutePanel = new ProcessStepPanel(false);
		fermentPanel = new ProcessStepPanel(false);
		mashInPanel = new ProcessStepPanel(false);
		mashOutPanel = new ProcessStepPanel(false);
		standPanel = new ProcessStepPanel(false);

		stepPanels = new HashMap<Class, ProcessStepPanel>();
		initProcessStepPanel(BatchSparge.class, batchSpargePanel);
		initProcessStepPanel(Boil.class, boilPanel);
		initProcessStepPanel(Cool.class, coolPanel);
		initProcessStepPanel(Dilute.class, dilutePanel);
		initProcessStepPanel(Ferment.class, fermentPanel);
		initProcessStepPanel(SingleInfusionMash.class, mashInPanel);
		initProcessStepPanel(MashOut.class, mashOutPanel);
		initProcessStepPanel(Stand.class, standPanel);*/

		stepText = new JTextArea();
		stepText.setWrapStyleWord(true);
		stepText.setLineWrap(true);
		stepText.setEditable(false);
//		stepText.setEnabled(false);

		ingredientText = new JTextArea();
		ingredientText.setWrapStyleWord(true);
		ingredientText.setLineWrap(true);
		ingredientText.setEditable(false);
//		ingredientText.setEnabled(false);

		JScrollPane scrollerSteps = new JScrollPane(stepsPanel);

		JPanel stepsTab = new JPanel(new BorderLayout());
		stepsTab.add(scrollerSteps, BorderLayout.WEST);
//		stepsTab.add(middleCards, BorderLayout.CENTER);
		stepsTab.add(stepText, BorderLayout.CENTER);

		JPanel ingredientsTab = new JPanel();
		ingredientsTab.setLayout(new BoxLayout(ingredientsTab, BoxLayout.X_AXIS));
		ingredientsTab.add(inputVolumesPanel);
		ingredientsTab.add(ingredientText);

		JPanel computedVolumesTab = new JPanel();
		computedVolumesTab.setLayout(new BoxLayout(computedVolumesTab, BoxLayout.X_AXIS));

		tabs.add("Ingredients", ingredientsTab);
		tabs.add("Steps", stepsTab);
		tabs.add("Computed Volumes", computedVolumesPanel);

		return tabs;
	}

	private void initProcessStepPanel(Class key, ProcessStepPanel panel)
	{
//		middleCards.add(key.getName(), panel);
//		stepPanels.put(key, panel);
	}

	@Override
	public void refresh(String name)
	{
		refresh(Database.getInstance().getBatches().get(name));
	}

	protected void refresh(Batch newBatch)
	{
		batch = newBatch;

		batch.run();

		stepsModel.clear();
		ingredientsModel.clear();
		computedVolumesModel.clear();
		stepText.setText("");
		ingredientText.setText("");

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
			else
			{
				computedVolumesModel.add(v);
			}
		}
		Collections.sort(ingredientsModel.data, new VolumesComparator());
		Collections.sort(computedVolumesModel.data, new VolumesComparator());

		if (stepsModel.getSize() > 0)
		{
			steps.setSelectedIndex(0);
			refreshStepText();
		}
		if (ingredientsModel.getSize() > 0)
		{
			ingredients.setSelectedIndex(0);
			refreshIngredientText();
		}
		if (computedVolumesModel.getSize() > 0)
		{
			computedVolumes.setSelectedIndex(0);
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
		volumes.addInputVolume("grain bill", new GrainBill(new ArrayList<Fermentable>()));

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

	private void refreshIngredientText()
	{
		ingredientText.setText(((Object)ingredients.getSelectedValue()).toString());
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
			refreshIngredientText();
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
			order.add(GrainBill.class);
			order.add(HopAddition.class);
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
