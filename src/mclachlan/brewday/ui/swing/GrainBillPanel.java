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
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.process.Batch;
import mclachlan.brewday.recipe.IngredientAddition;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class GrainBillPanel extends JPanel implements ActionListener, ChangeListener
{
	private JTextField name;
	private JList<Fermentable> grainsList;

	public GrainBillPanel(boolean addMode)
	{
		super(new GridBagLayout());
		name = new JTextField(20);

		GridBagConstraints gbc = EditorPanel.createGridBagConstraints();
		dodgyGridBagShite(this, new JLabel("Name:"), name, gbc);

		dodgyGridBagShite(this, new JLabel("Grains:"), new JLabel(), gbc);
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.gridx = 0;
		gbc.gridy++;
		add(grainsList, gbc);
	}

	public void refresh(IngredientAddition ingredientAddition, Batch batch)
	{
/*
		if (ingredientAddition != null)
		{
			name.setText(ingredientAddition.getName());
			DefaultListModel<Fermentable> model = new DefaultListModel<Fermentable>();
			for (FermentableAddition g : ingredientAddition.getIngredients())
			{
				model.addElement(g);
			}
			grainsList.setModel(model);
		}
*/
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{

	}

	public String getVolumeName()
	{
		return name.getText();
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{

	}

	public IngredientAddition getGrainBill()
	{
		return null;
	};
}
