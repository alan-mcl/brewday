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
import mclachlan.brewday.ingredients.GrainBill;
import mclachlan.brewday.ingredients.HopAddition;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.process.*;

/**
 *
 */
public class BatchesPanel extends EditorPanel
{
	private JList steps, inputVolumes, computedVolumes;
	private BatchesListModel<ProcessStep> stepsModel;
	private BatchesListModel<Volume> inputVolumesModel, computedVolumesModel;
	private JButton addStep, removeStep, editStep;
	private JButton addVol, removeVol, editVol;


	private JPanel middleCards;
	private BatchSpargePanel batchSpargePanel;

	// todo other step panels
	private ProcessStepPanel boilPanel, coolPanel, dilutePanel, fermentPanel,
	mashInPanel, mashOutPanel, standPanel;
	private CardLayout middleCardLayout;
	private Map<Class, ProcessStepPanel> stepPanels;
	private Batch batch;

	public BatchesPanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected Container getEditControls()
	{
		stepsModel = new BatchesListModel<ProcessStep>(new ArrayList<ProcessStep>());
		inputVolumesModel = new BatchesListModel<Volume>(new ArrayList<Volume>());
		computedVolumesModel = new BatchesListModel<Volume>(new ArrayList<Volume>());

		steps = new JList(stepsModel);
		steps.addMouseListener(this);
		inputVolumes = new JList(inputVolumesModel);
		inputVolumes.addMouseListener(this);
		computedVolumes = new JList(computedVolumesModel);
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

		addVol = new JButton("Add");
		addVol.addActionListener(this);
		editVol = new JButton("Edit");
		editVol.addActionListener(this);
		removeVol = new JButton("Remove");
		removeVol.addActionListener(this);

		JPanel volsButtons = new JPanel();
		volsButtons.add(addVol);
		volsButtons.add(editVol);
		volsButtons.add(removeVol);

		JPanel stepsPanel = new JPanel(new BorderLayout());
		stepsPanel.setBorder(BorderFactory.createTitledBorder("Steps"));
		stepsPanel.add(steps, BorderLayout.CENTER);
		stepsPanel.add(stepsButtons, BorderLayout.SOUTH);

		JPanel inputVolumesPanel = new JPanel();
		inputVolumesPanel.setLayout(new BoxLayout(inputVolumesPanel, BoxLayout.Y_AXIS));
		inputVolumesPanel.setBorder(BorderFactory.createTitledBorder("Input Volumes"));
		inputVolumesPanel.add(new JScrollPane(inputVolumes));
		inputVolumesPanel.add(volsButtons);

		JPanel computedVolumesPanel = new JPanel();
		computedVolumesPanel.setBorder(BorderFactory.createTitledBorder("Computed Volumes"));
		computedVolumesPanel.add(new JScrollPane(computedVolumes));

		JPanel volumesPanel = new JPanel();
		volumesPanel.setLayout(new BoxLayout(volumesPanel, BoxLayout.Y_AXIS));
		volumesPanel.add(inputVolumesPanel);
		volumesPanel.add(computedVolumesPanel);

		middleCardLayout = new CardLayout();
		middleCards = new JPanel(middleCardLayout);
		batchSpargePanel = new BatchSpargePanel(false);
		boilPanel = new ProcessStepPanel(false);
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
		initProcessStepPanel(MashIn.class, mashInPanel);
		initProcessStepPanel(MashOut.class, mashOutPanel);
		initProcessStepPanel(Stand.class, standPanel);

		JPanel panel = new JPanel(new BorderLayout(3,3));
		JScrollPane scrollerSteps = new JScrollPane(stepsPanel);

		panel.add(scrollerSteps, BorderLayout.WEST);
		panel.add(middleCards, BorderLayout.CENTER);
		panel.add(volumesPanel, BorderLayout.EAST);

		this.add(panel);

		return panel;
	}

	private void initProcessStepPanel(Class key, ProcessStepPanel panel)
	{
		middleCards.add(key.getName(), panel);
		stepPanels.put(key, panel);
	}

	@Override
	public void refresh(String name)
	{
		batch = Database.getInstance().getBatches().get(name);

		batch.run();

		for (ProcessStep ps : batch.getSteps())
		{
			stepsModel.add(ps);
		}
		for (Volume v : batch.getVolumes().getVolumes().values())
		{
			if (batch.getVolumes().getInputVolumes().contains(v.getName()))
			{
				inputVolumesModel.add(v);
			}
			else
			{
				computedVolumesModel.add(v);
			}
		}
		Collections.sort(inputVolumesModel.data, new VolumesComparator());
		Collections.sort(computedVolumesModel.data, new VolumesComparator());

		steps.setSelectedIndex(0);
		inputVolumes.setSelectedIndex(0);
		computedVolumes.setSelectedIndex(0);

		refreshMiddlePanel();
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

	private void refreshMiddlePanel()
	{
		int i = steps.getSelectedIndex();
		ProcessStep step = batch.getSteps().get(i);

		ProcessStepPanel processStepPanel = stepPanels.get(step.getClass());
		processStepPanel.refresh(step, batch);

		middleCardLayout.show(middleCards, step.getClass().getName());
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == steps)
		{
			refreshMiddlePanel();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == addStep)
		{
			ProcessStepDialog dialog = new ProcessStepDialog(SwingUi.instance, "Add Process Step");
			dialog.setVisible(true);

			//todo
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
