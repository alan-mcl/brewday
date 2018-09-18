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
import java.awt.CardLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.process.*;

/**
 *
 */
public class ProcessStepDialog extends JDialog implements ActionListener
{
	private JComboBox<ProcessStep.Type> stepType;
	private JPanel middleCards;
	// todo other step panels
	private ProcessStepPanel coolPanel, dilutePanel, fermentPanel,
	standPanel;
	private SingleInfusionMashPanel singleInfusionMashPanel;
	private BatchSpargePanel batchSpargePanel;
	private BoilPanel boilPanel;
	private MashOutPanel mashOutPanel;
	private CardLayout middleCardLayout;
	private Map<ProcessStep.Type, ProcessStepPanel> stepPanels;
	private JButton ok, cancel;

	private ProcessStep result;

	public ProcessStepDialog(Frame owner, String title, Batch batch)
	{
		super(owner, title, true);

		JPanel type = new JPanel();
		stepType = new JComboBox<ProcessStep.Type>(ProcessStep.Type.values());
		stepType.addActionListener(this);
		type.add(new JLabel("Step Type"));
		type.add(stepType);

		ok = new JButton("OK");
		ok.addActionListener(this);

		cancel = new JButton("Cancel");
		cancel.addActionListener(this);

		JPanel buttons = new JPanel();
		buttons.add(ok);
		buttons.add(cancel);

		middleCardLayout = new CardLayout();
		middleCards = new JPanel(middleCardLayout);
		singleInfusionMashPanel = new SingleInfusionMashPanel();
		mashOutPanel = new MashOutPanel();
		batchSpargePanel = new BatchSpargePanel();
		boilPanel = new BoilPanel();
		coolPanel = new ProcessStepPanel();
		dilutePanel = new ProcessStepPanel();
		fermentPanel = new ProcessStepPanel();
		standPanel = new ProcessStepPanel();

		stepPanels = new HashMap<ProcessStep.Type, ProcessStepPanel>();
		initProcessStepPanel(ProcessStep.Type.BATCH_SPARGE, batchSpargePanel, batch);
		initProcessStepPanel(ProcessStep.Type.BOIL, boilPanel, batch);
		initProcessStepPanel(ProcessStep.Type.COOL, coolPanel, batch);
		initProcessStepPanel(ProcessStep.Type.DILUTE, dilutePanel, batch);
		initProcessStepPanel(ProcessStep.Type.FERMENT, fermentPanel, batch);
		initProcessStepPanel(ProcessStep.Type.MASH_IN, singleInfusionMashPanel, batch);
		initProcessStepPanel(ProcessStep.Type.MASH_OUT, mashOutPanel, batch);
		initProcessStepPanel(ProcessStep.Type.STAND, standPanel, batch);

		JPanel content = new JPanel(new BorderLayout());
		content.add(type, BorderLayout.NORTH);
		content.add(middleCards, BorderLayout.CENTER);
		content.add(buttons, BorderLayout.SOUTH);

		this.add(content);
		pack();
		setLocationRelativeTo(owner);
	}

	private void initProcessStepPanel(ProcessStep.Type key, ProcessStepPanel panel, Batch batch)
	{
		panel.refresh(null, batch);
		middleCards.add(key.toString(), panel);
		stepPanels.put(key, panel);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == stepType)
		{
			middleCardLayout.show(middleCards, stepType.getSelectedItem().toString());
		}
		else if (e.getSource() == ok)
		{
			ProcessStepPanel processStepPanel = stepPanels.get((ProcessStep.Type)stepType.getSelectedItem());
			result = processStepPanel.getStep();
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
