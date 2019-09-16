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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class AddProcessStepDialog extends JDialog implements ActionListener
{
	private JComboBox<ProcessStep.Type> stepType;
	private JButton ok, cancel;

	private Recipe recipe;
	private ProcessStep result;

	public AddProcessStepDialog(Frame owner, String title, Recipe recipe)
	{
		super(owner, title, true);
		this.recipe = recipe;

		JPanel type = new JPanel();
		stepType = new JComboBox<>(ProcessStep.Type.values());
		stepType.addActionListener(this);
		type.add(new JLabel(StringUtils.getUiString("process.step.type")));
		type.add(stepType);

		ok = new JButton(StringUtils.getUiString("ui.ok"));
		ok.addActionListener(this);

		cancel = new JButton(StringUtils.getUiString("ui.cancel"));
		cancel.addActionListener(this);

		JPanel buttons = new JPanel();
		buttons.add(ok);
		buttons.add(cancel);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.add(type);
		content.add(buttons);

		this.add(content);
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == ok)
		{
			switch ((ProcessStep.Type)stepType.getSelectedItem())
			{
				case BATCH_SPARGE:
					result = new BatchSparge(recipe);
					break;
				case BOIL:
					result = new Boil(recipe);
					break;
				case COOL:
					result = new Cool(recipe);
					break;
				case DILUTE:
					result = new Dilute(recipe);
					break;
				case FERMENT:
					result = new Ferment(recipe);
					break;
				case MASH:
					result = new Mash(recipe);
					break;
				case STAND:
					result = new Stand(recipe);
					break;
				case PACKAGE:
					result = new PackageStep(recipe);
					break;
				case MASH_INFUSION:
					result = new MashInfusion(recipe);
					break;
				case SPLIT_BY_PERCENT:
					result = new SplitByPercent(recipe);
					break;
				default: throw new BrewdayException("invalid "+stepType.getSelectedItem());
			}
			setVisible(false);
		}
		else if (e.getSource() == cancel)
		{
			result = null;
			setVisible(false);
		}
	}

	public ProcessStep getResult()
	{
		return result;
	}
}
