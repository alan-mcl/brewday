/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the yeaste that it will be useful,
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
import mclachlan.brewday.database.Database;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.process.Recipe;
import mclachlan.brewday.recipe.YeastAddition;

/**
 *
 */
public class YeastAdditionDialog extends JDialog implements ActionListener
{
	private JComboBox yeast;
	private JSpinner weight;
	private JButton ok, cancel;

	private YeastAddition result;

	public YeastAdditionDialog(Frame owner, String title, Recipe recipe)
	{
		super(owner, title, true);

		this.setLayout(new BorderLayout());

		JPanel content = new JPanel();
		SpringLayout layout = new SpringLayout();
		content.setLayout(layout);

		JLabel yeastLabel = new JLabel("Yeast:", JLabel.TRAILING);
		content.add(yeastLabel);

		Vector<String> vector = new Vector<String>(Database.getInstance().getReferenceYeasts().keySet());
		Collections.sort(vector);
		DefaultComboBoxModel model = new DefaultComboBoxModel(vector);
		yeast = new JComboBox(model);
		content.add(yeast);

		yeastLabel.setLabelFor(yeast);

		JLabel weightLabel = new JLabel("Weight (g):", JLabel.TRAILING);
		content.add(weightLabel);

		weight = new JSpinner(new SpinnerNumberModel(0D, 0D, 999D, 0.1));
		content.add(weight);

		// Lay out the panel.
		SpringUtilities.makeCompactGrid(content,
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
			Yeast f = Database.getInstance().getReferenceYeasts().get(
				(String)yeast.getSelectedItem());

			result = new YeastAddition(f, (Double)weight.getValue());

			setVisible(false);
		}
		else if (e.getSource() == cancel)
		{
			result = null;
			setVisible(false);
		}
	}

	public YeastAddition getResult()
	{
		return result;
	}
}
