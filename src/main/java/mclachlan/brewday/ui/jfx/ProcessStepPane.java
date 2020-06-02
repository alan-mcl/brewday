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

package mclachlan.brewday.ui.jfx;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class ProcessStepPane extends MigPane
{
	protected TextField name;
	protected TextArea desc;
	protected boolean detectDirty = true;

	private ProcessStep step;
	private TrackDirty parent;

	public ProcessStepPane(TrackDirty parent)
	{
		this.parent = parent;

		name = new TextField();
		desc = new TextArea();

		detectDirty = false;
		buildUiInternal();
		detectDirty = true;
	}

	protected void buildUiInternal()
	{

	}

	public void refresh(ProcessStep step, Recipe recipe)
	{
		detectDirty = false;

		this.step = step;
		if (step != null)
		{
			name.setText(step.getName());
			desc.setText(step.getDescription());
		}

		refreshInternal(step, recipe);

		detectDirty = true;
	}

	protected void refreshInternal(ProcessStep step, Recipe recipe)
	{

	}

/*
	protected DefaultComboBoxModel<String> getVolumesOptions(Recipe recipe, Volume.Type... types)
	{
		Vector<String> vec = new Vector<>(recipe.getVolumes().getVolumes(types));
		Collections.sort(vec);
		vec.add(0, EditorPanel.NONE);
		return new DefaultComboBoxModel<>(vec);
	}
*/

	public String getDescription()
	{
		return desc.getText();
	}

	public ProcessStep getStep()
	{
		return step;
	}

	public TrackDirty getParentTrackDirty()
	{
		return parent;
	}
}
