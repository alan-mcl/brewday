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

import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class ComputedVolumePane extends Pane
{
	private Label name;
	private Label text;

	public ComputedVolumePane(String title)
	{
		name = new Label();
		name.setStyle("-fx-font-weight: bold;");

		text = new Label();

		MigPane content = new MigPane("insets 3");

		content.add(name, "dock north");
		content.add(text, "dock center");

		TitledPane titledPane = new TitledPane(title, content);
		this.getChildren().add(titledPane);
	}

	public void refresh(String volName, Recipe recipe)
	{
		if (recipe.getVolumes().contains(volName))
		{
			Volume volume = recipe.getVolumes().getVolume(volName);
			name.setText(StringUtils.getUiString("volumes.name") + volName);
			text.setText(volume.describe());
		}
		else
		{
			name.setText(StringUtils.getUiString("volumes.error"));
			text.setText(StringUtils.getUiString("volumes.volume.does.not.exist", volName));
		}
	}
}
