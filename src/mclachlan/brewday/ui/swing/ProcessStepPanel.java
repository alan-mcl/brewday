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

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class ProcessStepPanel extends JPanel implements ActionListener, ChangeListener
{
	protected JTextField name;
	protected JTextArea desc;
	protected int dirtyFlag;
	private ProcessStep step;

	public ProcessStepPanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		name = new JTextField(20);
		desc = new JTextArea(4, 30);

		GridBagConstraints gbc = EditorPanel.createGridBagConstraints();

		buildUiInternal(gbc);
	}

	protected void buildUiInternal(GridBagConstraints gbc)
	{

	}

	public void refresh(ProcessStep step, Recipe recipe)
	{
		this.step = step;
		if (step != null)
		{
			name.setText(step.getName());
			desc.setText(step.getDescription());
		}

		refreshInternal(step, recipe);
	}

	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{

	}

	protected DefaultComboBoxModel<String> getVolumesOptions(Recipe recipe, Volume.Type... types)
	{
		Vector<String> vec = new Vector<>(recipe.getVolumes().getVolumes(types));
		Collections.sort(vec);
		vec.add(0, EditorPanel.NONE);
		return new DefaultComboBoxModel<>(vec);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{

	}

	public String getDescription()
	{
		return desc.getText();
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{

	}

	public ProcessStep getStep()
	{
		return step;
	}

	public int getDirtyFlag()
	{
		return dirtyFlag;
	}

	protected void triggerUiRefresh()
	{
		SwingUi.instance.setDirty(dirtyFlag);
		SwingUi.instance.refreshProcessSteps();
	}
}
