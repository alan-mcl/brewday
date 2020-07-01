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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import javax.swing.*;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;

/**
 *
 */
public class WaterAdditionDialog extends JDialog implements ActionListener, KeyListener
{
	private JTextField searchBox;
	private JTable table;
	private WatersTableModel tableModel;
	private JSpinner volume, temperature, time;
	private JButton ok, cancel;
	private JComboBox<ProcessStep> usage;

	private IngredientAddition result;
	private ProcessStep stepResult;
	private TableRowSorter rowSorter;

	/*-------------------------------------------------------------------------*/
	public WaterAdditionDialog(
		Frame owner,
		String title,
		Recipe recipe,
		WaterAddition selected,
		ProcessStep step)
	{
		super(owner, title, true);

		this.setLayout(new BorderLayout());

		JPanel content = new JPanel(new BorderLayout());

		Map<String, Water> dbWaters = Database.getInstance().getWaters();
		List<Water> waters = new ArrayList<Water>(dbWaters.values());
		waters.sort(Comparator.comparing(Water::getName));

		tableModel = new WatersTableModel(waters);
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

		JLabel weightLabel = new JLabel(StringUtils.getUiString("water.addition.volume"), JLabel.TRAILING);
		volume = new JSpinner(new SpinnerNumberModel(0D, 0D, 999D,0.01));
		weightLabel.setLabelFor(volume);

		JPanel bottom = new JPanel();
		bottom.add(weightLabel);
		bottom.add(volume);

		if (recipe != null)
		{
			JLabel usageLabel = new JLabel(StringUtils.getUiString("water.addition.usage"));
			List<ProcessStep> possibleUsages = recipe.getStepsForIngredient(IngredientAddition.Type.WATER);
			usage = new JComboBox<>(new Vector<>(possibleUsages));
			usageLabel.setLabelFor(usage);

			JLabel tempLabel = new JLabel(StringUtils.getUiString("water.addition.temperature"));
			temperature = new JSpinner(new SpinnerNumberModel(0D, 0D, 999D, 1D));
			tempLabel.setLabelFor(temperature);

			JLabel timeLabel = new JLabel(StringUtils.getUiString("water.addition.time"));
			time = new JSpinner(new SpinnerNumberModel(0D, 0D, 999D, 1D));
			timeLabel.setLabelFor(time);

			bottom.add(usageLabel);
			bottom.add(usage);
			bottom.add(tempLabel);
			bottom.add(temperature);
			bottom.add(timeLabel);
			bottom.add(time);
		}

		content.add(top, BorderLayout.NORTH);
		content.add(new JScrollPane(table), BorderLayout.CENTER);
		content.add(bottom, BorderLayout.SOUTH);

		ok = new JButton(StringUtils.getUiString("ui.ok"));
		ok.addActionListener(this);

		cancel = new JButton(StringUtils.getUiString("ui.cancel"));
		cancel.addActionListener(this);

		JPanel buttons = new JPanel();
		buttons.add(ok);
		buttons.add(cancel);

		this.add(content, BorderLayout.CENTER);
		this.add(buttons, BorderLayout.SOUTH);

		if (selected != null)
		{
			volume.setValue(selected.getQuantity().get(Quantity.Unit.LITRES));
			usage.setSelectedItem(recipe.getStepOfAddition(selected));
			temperature.setValue(selected.getTemperature().get(Quantity.Unit.CELSIUS));
			time.setValue(selected.getTime().get(Quantity.Unit.MINUTES));

			int index = tableModel.getData().indexOf(selected.getWater());
			table.setRowSelectionInterval(index, index);
			table.scrollRectToVisible(new Rectangle(table.getCellRect(index, 0, true)));
		}

		if (step != null)
		{
			usage.setSelectedItem(step);
		}

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
				Water water = tableModel.get(selectedRow);
				result = new WaterAddition(
					water,
					new VolumeUnit((Double)volume.getValue(), Quantity.Unit.LITRES),
					new TemperatureUnit(getTemperature(), Quantity.Unit.CELSIUS, false),
					getTime());
				stepResult = usage==null ? null : (ProcessStep)usage.getSelectedItem();
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

	public double getTemperature()
	{
		return temperature == null ? 0 : (Double)temperature.getValue();
	}

	/*-------------------------------------------------------------------------*/
	public TimeUnit getTime()
	{
		double value = time == null ? 0 : (Double)time.getValue();
		return new TimeUnit(value, Quantity.Unit.MINUTES, false);
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

