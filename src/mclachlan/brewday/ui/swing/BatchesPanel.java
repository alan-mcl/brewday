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
import mclachlan.brewday.BrewdayException;
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
	private Batch batch;

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
	private JList<ProcessStep> steps;
	private BatchesListModel<ProcessStep> stepsModel;
	private JButton addStep, removeStep;
	private JPanel stepCards;
	private CardLayout stepCardLayout;
	private BatchSpargePanel batchSpargePanel;
	private ProcessStepPanel boilPanel, coolPanel, dilutePanel, fermentPanel, mashInPanel, mashOutPanel, standPanel, packagePanel;
	private JTextArea stepText;
	private JTextArea stepEndResult;

	// computed volumes tab
	private JList<Volume> computedVolumes;
	private BatchesListModel<Volume> computedVolumesModel;

	/*-------------------------------------------------------------------------*/
	public BatchesPanel(int dirtyFlag)
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
		computedVolumes.addMouseListener(this);

		JPanel computedVolumesPanel = new JPanel();
		computedVolumesPanel.setLayout(new BoxLayout(computedVolumesPanel, BoxLayout.Y_AXIS));
		computedVolumesPanel.add(new JScrollPane(computedVolumes));

		JPanel computedVolumesTab = new JPanel();
		computedVolumesTab.setLayout(new BoxLayout(computedVolumesTab, BoxLayout.X_AXIS));
		return computedVolumesPanel;
	}

	/*-------------------------------------------------------------------------*/
	private JPanel getStepsTab()
	{
		stepsModel = new BatchesListModel<ProcessStep>(new ArrayList<ProcessStep>());

		steps = new JList<ProcessStep>(stepsModel);
		steps.addMouseListener(this);

		addStep = new JButton("Add");
		addStep.addActionListener(this);
		removeStep = new JButton("Remove");
		removeStep.addActionListener(this);

		JPanel stepsButtons = new JPanel();
		stepsButtons.add(addStep);
		stepsButtons.add(removeStep);

		JPanel stepsPanel = new JPanel();
		stepsPanel.setLayout(new BoxLayout(stepsPanel, BoxLayout.Y_AXIS));
		stepsPanel.add(new JScrollPane(steps));
		stepsPanel.add(stepsButtons);

		stepCardLayout = new CardLayout();
		stepCards = new JPanel(stepCardLayout);

		batchSpargePanel = new BatchSpargePanel(dirtyFlag);
		boilPanel = new BoilPanel(dirtyFlag);
		coolPanel = new CoolPanel(dirtyFlag);
		dilutePanel = new DilutePanel(dirtyFlag);
		fermentPanel = new FermentPanel(dirtyFlag);
		mashInPanel = new SingleInfusionMashPanel(dirtyFlag);
		mashOutPanel = new MashOutPanel(dirtyFlag);
		standPanel = new StandPanel(dirtyFlag);
		packagePanel = new PackagePanel(dirtyFlag);

		stepCards.add(ProcessStep.Type.BATCH_SPARGE.toString(), batchSpargePanel);
		stepCards.add(ProcessStep.Type.BOIL.toString(), boilPanel);
		stepCards.add(ProcessStep.Type.COOL.toString(), coolPanel);
		stepCards.add(ProcessStep.Type.DILUTE.toString(), dilutePanel);
		stepCards.add(ProcessStep.Type.FERMENT.toString(), fermentPanel);
		stepCards.add(ProcessStep.Type.MASH_IN.toString(), mashInPanel);
		stepCards.add(ProcessStep.Type.MASH_OUT.toString(), mashOutPanel);
		stepCards.add(ProcessStep.Type.STAND.toString(), standPanel);
		stepCards.add(ProcessStep.Type.PACKAGE.toString(), packagePanel);

		stepEndResult = new JTextArea();
		stepEndResult.setWrapStyleWord(true);
		stepEndResult.setLineWrap(true);
		stepEndResult.setEditable(false);

		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));
		result.add(stepsPanel);
		result.add(stepCards);
		result.add(stepEndResult);

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
		ingredients.addMouseListener(this);

		JPanel ingredientsPanel = new JPanel();
		ingredientsPanel.setLayout(new BoxLayout(ingredientsPanel, BoxLayout.Y_AXIS));
		ingredientsPanel.add(new JScrollPane(ingredients));
		ingredientsPanel.add(volsButtons);

		ingredientCardLayout = new CardLayout();
		ingredientCards = new JPanel(ingredientCardLayout);

		fermentableAdditionPanel = new FermentableAdditionPanel();
		hopAdditionPanel = new HopAdditionPanel();
		waterPanel = new WaterPanel();

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
		refresh(Database.getInstance().getBatches().get(name));
	}

	/*-------------------------------------------------------------------------*/
	protected void refresh(Batch newBatch)
	{
		batch = newBatch;

		stepsModel.clear();
		ingredientsModel.clear();
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

		refreshComputedVolumes();

		if (stepsModel.getSize() > 0)
		{
			steps.setSelectedIndex(0);
			refreshStepCards();
		}
		if (ingredientsModel.getSize() > 0)
		{
			ingredients.setSelectedIndex(0);
			refreshIngredientCards();
		}
	}

	/*-------------------------------------------------------------------------*/
	protected void refreshComputedVolumes()
	{
		batch.run();

		computedVolumesModel.clear();
		ingredientEndResult.setText("");
		stepEndResult.setText("");

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

		refreshStepCards();
		refreshIngredientCards();

		StringBuilder sb = new StringBuilder("End Result:\n");

		if (batch.getErrors().size() > 0)
		{
			sb.append("\nERRORS:\n");
			for (String s : batch.getErrors())
			{
				sb.append(s);
				sb.append('\n');
			}
		}

		if (batch.getVolumes().getOutputVolumes().size() > 0)
		{
			for (String s : batch.getVolumes().getOutputVolumes())
			{
				FluidVolume v = (FluidVolume)batch.getVolumes().getVolume(s);

				sb.append(String.format("\n'%s' (%.1fl)\n", v.getName(), v.getVolume()/1000));
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
		stepEndResult.setText(sb.toString());

		ingredients.repaint();
		steps.repaint();
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
		volumes.addInputVolume("mash water", new WaterAddition("mash water", 20, 66));
		volumes.addInputVolume("grain bill", new FermentableAdditionList("grain bill"));

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

	private void refreshStepCards()
	{
		int i = steps.getSelectedIndex();
		if (i > -1)
		{
			ProcessStep step = batch.getSteps().get(i);

			switch (step.getType())
			{
				case BATCH_SPARGE:
					batchSpargePanel.refresh(step, batch);
					break;
				case BOIL:
					boilPanel.refresh(step, batch);
					break;
				case COOL:
					coolPanel.refresh(step, batch);
					break;
				case DILUTE:
					dilutePanel.refresh(step, batch);
					break;
				case FERMENT:
					fermentPanel.refresh(step, batch);
					break;
				case MASH_IN:
					mashInPanel.refresh(step, batch);
					break;
				case MASH_OUT:
					mashOutPanel.refresh(step, batch);
					break;
				case STAND:
					standPanel.refresh(step, batch);
					break;
				case PACKAGE:
					packagePanel.refresh(step, batch);
					break;
				default:
					throw new BrewdayException("Invalid step " + step.getType());
			}

			stepCardLayout.show(stepCards, step.getType().toString());
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
				fermentableAdditionPanel.refresh((FermentableAdditionList)selected, batch);
				ingredientCardLayout.show(ingredientCards, Volume.Type.FERMENTABLES.toString());
			}
			else if (selected instanceof HopAdditionList)
			{
				hopAdditionPanel.refresh((HopAdditionList)selected, batch);
				ingredientCardLayout.show(ingredientCards, Volume.Type.HOPS.toString());
			}
			else if (selected instanceof WaterAddition)
			{
				waterPanel.refresh((WaterAddition)selected, batch);
				ingredientCardLayout.show(ingredientCards, Volume.Type.WATER.toString());
			}
			else
			{
				throw new BrewdayException("Invalid input volume: "+selected);
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == steps && stepsModel.getSize() > 0)
		{
			refreshStepCards();
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
			AddProcessStepDialog dialog = new AddProcessStepDialog(SwingUi.instance, "Add Process Step", batch);

			ProcessStep newProcessStep = dialog.getResult();
			if (newProcessStep != null)
			{
				batch.getSteps().add(newProcessStep);
				refresh(batch);
			}
		}
		else if (e.getSource() == removeStep)
		{
			int selectedIndex = steps.getSelectedIndex();
			if (selectedIndex > -1)
			{
				ProcessStep selected = stepsModel.data.get(selectedIndex);

				batch.getSteps().remove(selected);
				stepsModel.remove(selectedIndex);
				refreshComputedVolumes();
			}

		}
		else if (e.getSource() == addIngredient)
		{
			AddIngredientDialog dialog = new AddIngredientDialog(SwingUi.instance, "Add Ingredient Volume", batch);

			Volume v = dialog.getResult();
			if (v != null)
			{
				batch.getVolumes().addInputVolume(v.getName(), v);
				refresh(batch);
			}
		}
		else if (e.getSource() == removeIngredient)
		{
			int selectedIndex = ingredients.getSelectedIndex();
			if (selectedIndex > -1)
			{
				Volume selected = ingredientsModel.data.get(selectedIndex);

				batch.getVolumes().removeInputVolume(selected);
				ingredientsModel.remove(selectedIndex);
				refreshComputedVolumes();
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
