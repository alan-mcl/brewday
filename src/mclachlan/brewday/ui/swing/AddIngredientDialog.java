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
import mclachlan.brewday.process.Recipe;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.recipe.FermentableAdditionList;
import mclachlan.brewday.recipe.HopAdditionList;

/**
 *
 */
public class AddIngredientDialog extends JDialog implements ActionListener
{
	private JTextField name;
	private JComboBox<Volume.Type> type;
	private JButton ok, cancel;

	private Volume result;

	public AddIngredientDialog(Frame owner, String title, Recipe recipe)
	{
		super(owner, title, true);

		this.setLayout(new BorderLayout());

		JPanel content = new JPanel();
		SpringLayout layout = new SpringLayout();
		content.setLayout(layout);

		JLabel typeLabel = new JLabel("Ingredient Type:", JLabel.TRAILING);
		content.add(typeLabel);

		Vector<Volume.Type> vector = new Vector<Volume.Type>(
			Arrays.asList(new Volume.Type[] {
				Volume.Type.FERMENTABLES,
				Volume.Type.HOPS,
				Volume.Type.WATER}));
		DefaultComboBoxModel<Volume.Type> model = new DefaultComboBoxModel<Volume.Type>(vector);
		this.type = new JComboBox<Volume.Type>(model);
		content.add(this.type);
		typeLabel.setLabelFor(this.type);

		JLabel nameLabel = new JLabel("Name:", JLabel.TRAILING);
		content.add(nameLabel);

		name = new JTextField(20);
		name.addActionListener(this);
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
		if (e.getSource() == ok || e.getSource() == name)
		{
			String nameText = name.getText();
			if (Volume.Type.FERMENTABLES.equals(type.getSelectedItem()))
			{
				result = new FermentableAdditionList(nameText);
			}
			else if (Volume.Type.HOPS.equals(type.getSelectedItem()))
			{
				result = new HopAdditionList(nameText);
			}
			else if (Volume.Type.WATER.equals(type.getSelectedItem()))
			{
				result = new WaterAddition(nameText, 10000, 20);
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
