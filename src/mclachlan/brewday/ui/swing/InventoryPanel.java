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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.ArbitraryPhysicalQuantity;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.recipe.IngredientAddition;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class InventoryPanel extends JPanel implements ActionListener, KeyListener
{
	private JTable table;
	private InventoryLineItemsTableModel tableModel;
	private TableRowSorter rowSorter;
	private int dirtyFlag;
	private JTextField searchField;
	private JButton increaseAmount, decreaseAmount, edit, delete;
	private JButton addFermentable, addHop, addMisc, addYeast, addWater;

	/*-------------------------------------------------------------------------*/
	public InventoryPanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new BorderLayout());

		tableModel = new InventoryLineItemsTableModel();
		table = new JTable(tableModel);
		table.setFillsViewportHeight(true);
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setDefaultRenderer(LabelIcon.class, new LabelIconRenderer());
		rowSorter = (TableRowSorter)table.getRowSorter();

		searchField = new JTextField(50);
		searchField.addKeyListener(this);
		JLabel searchLabel = new JLabel(SwingUi.searchIcon);
		searchLabel.setLabelFor(searchField);

		JPanel topPanel = new JPanel();
		topPanel.add(searchLabel);
		topPanel.add(searchField);

		addFermentable = new JButton("Add Fermentable", SwingUi.grainsIcon);
		addFermentable.addActionListener(this);

		addHop = new JButton("Add Hop", SwingUi.hopsIcon);
		addHop.addActionListener(this);

		addMisc = new JButton("Add Misc", SwingUi.miscIcon);
		addMisc.addActionListener(this);

		addYeast = new JButton("Add Yeast", SwingUi.yeastIcon);
		addYeast.addActionListener(this);

		addWater = new JButton("Add Water", SwingUi.waterIcon);
		addWater.addActionListener(this);

		edit = new JButton("Edit Item", SwingUi.editIcon);
		edit.addActionListener(this);

		delete = new JButton("Delete Item", SwingUi.deleteIcon);
		delete.addActionListener(this);

		increaseAmount = new JButton("Increase Amount", SwingUi.increaseIcon);
		increaseAmount.addActionListener(this);

		decreaseAmount = new JButton("Decrease Amount", SwingUi.decreaseIcon);
		decreaseAmount.addActionListener(this);

		JPanel bottomPanel = new JPanel(new MigLayout("center"));
		bottomPanel.add(addFermentable);
		bottomPanel.add(addHop);
		bottomPanel.add(addYeast);
		bottomPanel.add(addWater);
		bottomPanel.add(addMisc, "wrap");

		bottomPanel.add(edit);
		bottomPanel.add(delete);
		bottomPanel.add(increaseAmount);
		bottomPanel.add(increaseAmount);
		bottomPanel.add(decreaseAmount, "wrap");

		this.add(topPanel, BorderLayout.NORTH);
		this.add(new JScrollPane(table), BorderLayout.CENTER);
		this.add(bottomPanel, BorderLayout.SOUTH);

		refresh();
	}

	/*-------------------------------------------------------------------------*/
	public void refresh()
	{
		Map<String, InventoryLineItem> dbInventoryLineItems = Database.getInstance().getInventory();

		List<InventoryLineItem> inventoryLineItems = new ArrayList<>(dbInventoryLineItems.values());
		inventoryLineItems.sort(Comparator.comparing(InventoryLineItem::getName));

		tableModel.data.clear();
		tableModel.data.addAll(inventoryLineItems);
		tableRepaint();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == addFermentable)
		{
			FermentableAdditionDialog dialog =
				new FermentableAdditionDialog(
					SwingUi.instance,
					StringUtils.getUiString("common.add.fermentable"),
					null,
					null,
					null);
			IngredientAddition result = dialog.getResult();

			if (result != null)
			{
				InventoryLineItem item = new InventoryLineItem(
					getNewID(),
					result.getName(),
					IngredientAddition.Type.FERMENTABLES,
					new ArbitraryPhysicalQuantity(result.getQuantity()),
					0);

				Database.getInstance().getInventory().put(item.getName(), item);

				this.refresh();

				SwingUi.instance.setDirty(dirtyFlag);
			}
		}
		if (e.getSource() == addHop)
		{
			HopAdditionDialog dialog =
				new HopAdditionDialog(
					SwingUi.instance,
					StringUtils.getUiString("common.add.hop"),
					null,
					null,
					null);
			IngredientAddition result = dialog.getResult();

			if (result != null)
			{
				InventoryLineItem item = new InventoryLineItem(
					getNewID(),
					result.getName(),
					IngredientAddition.Type.HOPS,
					new ArbitraryPhysicalQuantity(result.getQuantity()),
					0);

				Database.getInstance().getInventory().put(item.getName(), item);

				this.refresh();

				SwingUi.instance.setDirty(dirtyFlag);
			}
		}
		if (e.getSource() == addYeast)
		{
			YeastAdditionDialog dialog =
				new YeastAdditionDialog(
					SwingUi.instance,
					StringUtils.getUiString("common.add.yeast"),
					null,
					null,
					null);
			IngredientAddition result = dialog.getResult();

			if (result != null)
			{
				InventoryLineItem item = new InventoryLineItem(
					getNewID(),
					result.getName(),
					IngredientAddition.Type.YEAST,
					new ArbitraryPhysicalQuantity(result.getQuantity()),
					0);

				Database.getInstance().getInventory().put(item.getName(), item);

				this.refresh();

				SwingUi.instance.setDirty(dirtyFlag);
			}
		}
		else if (e.getSource() == addWater)
		{
			// todo
		}
		else if (e.getSource() == addMisc)
		{
			MiscAdditionDialog dialog =
				new MiscAdditionDialog(
					SwingUi.instance,
					StringUtils.getUiString("common.add.misc"),
					null,
					null,
					null);
			IngredientAddition result = dialog.getResult();

			if (result != null)
			{
				InventoryLineItem item = new InventoryLineItem(
					getNewID(),
					result.getName(),
					IngredientAddition.Type.MISC,
					new ArbitraryPhysicalQuantity(result.getQuantity()),
					0);

				Database.getInstance().getInventory().put(item.getName(), item);

				this.refresh();

				SwingUi.instance.setDirty(dirtyFlag);
			}
		}
		else if (e.getSource() == edit)
		{
			// todo
			SwingUi.instance.setDirty(dirtyFlag);
		}
		else if (e.getSource() == delete)
		{
			int selectedRow = table.getSelectedRow();

			if (selectedRow > -1)
			{
				selectedRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
				InventoryLineItem inventoryLineItem = tableModel.data.get(selectedRow);

				if (inventoryLineItem != null)
				{
					Database.getInstance().getInventory().remove(inventoryLineItem.getName());

					this.refresh();
					SwingUi.instance.setDirty(dirtyFlag);
				}
			}
		}
		else if (e.getSource() == increaseAmount)
		{
			// todo
			SwingUi.instance.setDirty(dirtyFlag);
		}
		else if (e.getSource() == decreaseAmount)
		{
			// todo
			SwingUi.instance.setDirty(dirtyFlag);
		}
	}

	/*-------------------------------------------------------------------------*/
	private String getNewID()
	{
		UUID uuid = UUID.randomUUID();

		while (Database.getInstance().getInventory().containsKey(uuid.toString()))
		{
			uuid = UUID.randomUUID();
		}

		return uuid.toString();
	}

	/*-------------------------------------------------------------------------*/
	protected void tableRepaint()
	{
		tableModel.fireTableDataChanged();
		table.repaint();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void keyTyped(KeyEvent e)
	{
		// "(?i)" makes it case insensitive
		rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchField.getText()));
	}

	@Override
	public void keyPressed(KeyEvent e)
	{

	}

	@Override
	public void keyReleased(KeyEvent e)
	{

	}

	private static String formatQuantity(
			ArbitraryPhysicalQuantity q)
		{
			double amount = q.get();
			Quantity.Unit unit = q.getUnit();

			switch (unit)
			{
				case GRAMS:
					return String.format("%.2fkg", amount/1000D);
				case KILOGRAMS:
					return String.format("%.2fkg", amount);
				case OUNCES:
					return "todo";
				case POUNDS:
					return "todo";
				case MILLILITRES:
					return String.format("%.2fl", amount/1000D);
				case LITRES:
					return String.format("%.2fl", amount);
				case US_FLUID_OUNCE:
					return "todo";
				case US_GALLON:
					return "todo";
				case CELSIUS:
					return "todo";
				case KELVIN:
					return "todo";
				case FAHRENHEIT:
					return "todo";
				case GU:
					return "todo";
				case SPECIFIC_GRAVITY:
					return "todo";
				case PLATO:
					return "todo";
				default:
					throw new BrewdayException("invalid: "+unit);
			}
		}

	/*-------------------------------------------------------------------------*/
	public static class InventoryLineItemsTableModel extends AbstractTableModel
	{
		private List<InventoryLineItem> data;

		public InventoryLineItemsTableModel()
		{
			data = new ArrayList<>();
		}

		@Override
		public int getRowCount()
		{
			return data.size();
		}

		@Override
		public int getColumnCount()
		{
			return 3;
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0: return StringUtils.getUiString("inventory.ingredient");
				case 1: return StringUtils.getUiString("inventory.item.type");
				case 2: return StringUtils.getUiString("inventory.amount");
				default: throw new BrewdayException("Invalid column ["+columnIndex+"]");
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0:
					return LabelIcon.class;
				case 1:
					return String.class;
				case 2:
					return String.class;
				default:
					throw new BrewdayException("Invalid " + columnIndex);
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			InventoryLineItem cur = data.get(rowIndex);

			switch (columnIndex)
			{
				case 0: return getLabelIcon(cur);
				case 1: return cur.getType();
				case 2: return formatQuantity(cur.getAmount());
				default: throw new BrewdayException("Invalid column ["+columnIndex+"]");
			}
		}

		private LabelIcon getLabelIcon(InventoryLineItem item)
		{
			switch (item.getType())
			{
				case FERMENTABLES:
					return new LabelIcon(SwingUi.grainsIcon, item.getIngredient());
				case HOPS:
					return new LabelIcon(SwingUi.hopsIcon, item.getIngredient());
				case WATER:
					return new LabelIcon(SwingUi.waterIcon, item.getIngredient());
				case YEAST:
					return new LabelIcon(SwingUi.yeastIcon, item.getIngredient());
				case MISC:
					return new LabelIcon(SwingUi.miscIcon, item.getIngredient());
				default:
					throw new BrewdayException("Invalid: "+item.getType());
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{

		}

		@Override
		public void addTableModelListener(TableModelListener l)
		{

		}

		@Override
		public void removeTableModelListener(TableModelListener l)
		{

		}
	}
}
