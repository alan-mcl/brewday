/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing.widgets;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.ui.swing.app.SwingIcons;

/**
 * Renders reference-ingredient names in {@link javax.swing.JComboBox} lists with leading icons.
 */
public class IngredientComboBoxRenderer extends DefaultListCellRenderer
{
	private final IngredientAddition.Type category;

	public IngredientComboBoxRenderer(IngredientAddition.Type category)
	{
		this.category = category;
	}

	@Override
	public Component getListCellRendererComponent(
		JList<?> list,
		Object value,
		int index,
		boolean isSelected,
		boolean cellHasFocus)
	{
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (value instanceof String name)
		{
			setText(name);
			setIcon(SwingIcons.iconForReferenceName(category, name));
			setIconTextGap(6);
		}
		else
		{
			setIcon(null);
		}
		return this;
	}
}
