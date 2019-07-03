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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import javax.swing.*;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.database.Database;
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.YeastAddition;

/**
 *
 */
public class YeastAdditionDialog extends JDialog implements ActionListener, KeyListener
{
	private Recipe recipe;
	private JTextField searchBox;
	private JTable table;
	private YeastTableModel tableModel;
	private JSpinner weight, time;
	private JButton ok, cancel;
	private JComboBox<ProcessStep> usage;

	private IngredientAddition result;
	private ProcessStep stepResult;
	private TableRowSorter rowSorter;

	/*-------------------------------------------------------------------------*/
	public YeastAdditionDialog(Frame owner, String title, Recipe recipe)
	{
		super(owner, title, true);
		this.recipe = recipe;

		this.setLayout(new BorderLayout());

		JPanel content = new JPanel(new BorderLayout());

		Map<String, Yeast> dbYeasts = Database.getInstance().getReferenceYeasts();
		List<Yeast> yeasts = new ArrayList<Yeast>(dbYeasts.values());
		Collections.sort(yeasts, new Comparator<Yeast>()
		{
			@Override
			public int compare(Yeast o1, Yeast o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		});

		tableModel = new YeastTableModel(yeasts);
		table = new JTable(tableModel);
		table.setFillsViewportHeight(true);
		table.setAutoCreateRowSorter(true);
		table.setPreferredScrollableViewportSize(new Dimension(700, 200));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoCreateRowSorter(true);
		rowSorter = (TableRowSorter)table.getRowSorter();

		JLabel searchLabel = new JLabel(SwingUi.searchIcon);
		searchBox = new JTextField(30);
		searchLabel.setLabelFor(searchBox);
		searchBox.addKeyListener(this);

		JPanel top = new JPanel();
		top.add(searchLabel);
		top.add(searchBox);

		JLabel weightLabel = new JLabel("Weight (g):", JLabel.TRAILING);
		weight = new JSpinner(new SpinnerNumberModel(0D, 0D, 999D,0.01));
		weightLabel.setLabelFor(weight);

		JLabel usageLabel = new JLabel("Usage:");
		List<ProcessStep> possibleUsages = recipe.getStepsForIngredient(IngredientAddition.Type.YEAST);
		usage = new JComboBox<ProcessStep>(new Vector<ProcessStep>(possibleUsages));
		usageLabel.setLabelFor(usage);

		JLabel timeLabel = new JLabel("Time:");
		time = new JSpinner(new SpinnerNumberModel(0D, 0D, 999D, 1D));
		timeLabel.setLabelFor(time);

		JPanel bottom = new JPanel();
		bottom.add(weightLabel);
		bottom.add(weight);
		bottom.add(usageLabel);
		bottom.add(usage);
		bottom.add(timeLabel);
		bottom.add(time);

		content.add(top, BorderLayout.NORTH);
		content.add(new JScrollPane(table), BorderLayout.CENTER);
		content.add(bottom, BorderLayout.SOUTH);

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

	/*-------------------------------------------------------------------------*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == ok)
		{
			int selectedRow = table.getSelectedRow();
			if (selectedRow > -1)
			{
				selectedRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
				Yeast y = tableModel.getData().get(selectedRow);
				result = new YeastAddition(y, (Double)weight.getValue(), getTime());
				stepResult = (ProcessStep)usage.getSelectedItem();
				setVisible(false);
			}
		}
		else if (e.getSource() == cancel)
		{
			result = null;
			stepResult = null;
			setVisible(false);
		}
	}

	/*-------------------------------------------------------------------------*/
	public IngredientAddition getResult()
	{
		return result;
	}

	public ProcessStep getStepResult()
	{
		return stepResult;
	}

	public double getTime()
	{
		return (Double)time.getValue();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void keyTyped(KeyEvent e)
	{
		// "(?i)" makes it case insensitive
		rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchBox.getText()));
	}

	@Override
	public void keyPressed(KeyEvent e)
	{

	}

	@Override
	public void keyReleased(KeyEvent e)
	{

	}
}
