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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.process.Batch;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.FermentableAdditionList;
import mclachlan.brewday.recipe.HopAdditionList;

/**
 *
 */
public class AddIngredientDialog extends JDialog implements ActionListener
{
	private JTextField name;
	private JComboBox type;
	private JButton ok, cancel;

	private Volume result;

	public AddIngredientDialog(Frame owner, String title, Batch batch)
	{
		super(owner, title, true);

		this.setLayout(new BorderLayout());

		JPanel content = new JPanel();
		SpringLayout layout = new SpringLayout();
		content.setLayout(layout);

		JLabel typeLabel = new JLabel("Ingredient Type:", JLabel.TRAILING);
		content.add(typeLabel);

		Vector<String> vector = new Vector<String>(
			Arrays.asList(new String[] {BatchesPanel.FERMENTABLES, BatchesPanel.HOPS, BatchesPanel.WATER}));
		DefaultComboBoxModel model = new DefaultComboBoxModel(vector);
		this.type = new JComboBox(model);
		content.add(this.type);
		typeLabel.setLabelFor(this.type);

		JLabel nameLabel = new JLabel("Name:", JLabel.TRAILING);
		content.add(nameLabel);

		name = new JTextField(20);
		content.add(name);
		nameLabel.setLabelFor(name);

		// Lay out the panel.
		SpringUtilities.makeCompactGrid(
			content,
			2, 2, //rows, cols
			6, 6, //initX, initY
			6, 6);//xPad, yPad

		ok = new JButton("OK");
		ok.addActionListener(this);

		cancel = new JButton("Cancel");
		cancel.addActionListener(this);

		JPanel buttons = new JPanel();
		buttons.add(ok);
		buttons.add(cancel);

		this.add(content, BorderLayout.CENTER);
		this.add(buttons, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == ok)
		{
			String nameText = name.getText();
			if (BatchesPanel.FERMENTABLES.equals(type.getSelectedItem()))
			{
				result = new FermentableAdditionList(nameText);
			}
			else if (BatchesPanel.HOPS.equals(type.getSelectedItem()))
			{
				result = new HopAdditionList(nameText);
			}
			else if (BatchesPanel.WATER.equals(type.getSelectedItem()))
			{
				result = new Water(nameText, 10000, 20);
			}
			else
			{
				throw new BrewdayException("Invalid ingredient type ["+type.getSelectedItem()+"]");
			}

			setVisible(false);
		}
		else if (e.getSource() == cancel)
		{
			result = null;
			setVisible(false);
		}
	}

	public Volume getResult()
	{
		return result;
	}
}
