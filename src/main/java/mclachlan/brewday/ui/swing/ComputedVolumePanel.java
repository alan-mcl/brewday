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
import java.awt.Font;
import javax.swing.*;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class ComputedVolumePanel extends JPanel
{
	private JLabel name;
	private JTextArea text;

	public ComputedVolumePanel(String title)
	{
		super(new BorderLayout());

		name = new JLabel();
		name.setFont(name.getFont().deriveFont(Font.BOLD));

		text = new JTextArea(5, 30);
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		text.setEditable(false);
		text.setBackground(name.getBackground());

		this.add(name, BorderLayout.NORTH);
		this.add(text, BorderLayout.CENTER);

		this.setBorder(BorderFactory.createTitledBorder(title));
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
