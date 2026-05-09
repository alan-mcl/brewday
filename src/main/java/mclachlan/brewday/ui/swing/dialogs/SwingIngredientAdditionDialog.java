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

package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.ui.swing.app.SwingIcons;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code IngredientAdditionDialog}.
 *
 * @param <T> concrete {@link IngredientAddition} produced on OK
 * @param <S> reference ingredient row type from {@link Database}
 */
public abstract class SwingIngredientAdditionDialog<T extends IngredientAddition, S extends V2DataObject> extends JDialog
{
	private final ProcessStep step;
	private final boolean captureTime;
	private T output;

	private final List<S> dataRows = new ArrayList<>();
	private final JTextField searchField = new JTextField(32);
	private final JCheckBox onlyInventory = new JCheckBox(getUiString("ingredient.addition.only.in.inventory"));
	private final JTable table;
	private final TableModel tableModel;
	private final TableRowSorter<TableModel> sorter;
	private final JButton okButton = new JButton(getUiString("ui.ok"));
	private final JButton cancelButton = new JButton(getUiString("ui.cancel"));

	protected SwingIngredientAdditionDialog(Frame parent, SwingIcons.IconKey windowIcon, String titleKey,
		ProcessStep step, boolean captureTime)
	{
		super(parent, getUiString(titleKey), true);
		this.step = step;
		this.captureTime = captureTime;
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		dataRows.addAll(loadSortedRows());
		tableModel = new AbstractTableModel()
		{
			@Override
			public int getRowCount()
			{
				return dataRows.size();
			}

			@Override
			public int getColumnCount()
			{
				return SwingIngredientAdditionDialog.this.getTableColumnCount();
			}

			@Override
			public String getColumnName(int column)
			{
				return getUiString(SwingIngredientAdditionDialog.this.getTableColumnKey(column));
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex)
			{
				return SwingIngredientAdditionDialog.this.getTableCellValue(dataRows.get(rowIndex), columnIndex);
			}
		};
		table = new JTable(tableModel);
		sorter = new TableRowSorter<>(tableModel);
		table.setRowSorter(sorter);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoCreateRowSorter(false);
		table.getTableHeader().setReorderingAllowed(false);

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		north.add(new JLabel(SwingIcons.toolbarIcon(SwingIcons.IconKey.EDIT)));
		north.add(searchField);
		north.add(onlyInventory);

		String invOnly = Database.getInstance().getSettings().get(Settings.INGREDIENT_ADDITIONS_FROM_INVENTORY_ONLY);
		onlyInventory.setSelected(Boolean.parseBoolean(invOnly));

		JPanel bottomFields = new JPanel();
		bottomFields.setLayout(new java.awt.GridBagLayout());
		addUiStuffs(bottomFields);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(okButton);
		buttons.add(cancelButton);

		JPanel south = new JPanel(new BorderLayout());
		south.add(bottomFields, BorderLayout.CENTER);
		south.add(buttons, BorderLayout.SOUTH);

		JPanel content = new JPanel(new BorderLayout(8, 8));
		content.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
		content.add(north, BorderLayout.NORTH);
		content.add(new JScrollPane(table), BorderLayout.CENTER);
		content.add(south, BorderLayout.SOUTH);
		setContentPane(content);

		if (SwingIcons.icon(windowIcon, 32) != null)
		{
			setIconImage(SwingIcons.icon(windowIcon, 32).getImage());
		}

		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			private void go()
			{
				applyFilters();
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				go();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				go();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				go();
			}
		});

		onlyInventory.addActionListener(e ->
		{
			applyFilters();
			Database db = Database.getInstance();
			db.getSettings().set(
				Settings.INGREDIENT_ADDITIONS_FROM_INVENTORY_ONLY,
				Boolean.toString(onlyInventory.isSelected()));
			db.saveSettings();
		});

		okButton.addActionListener(e -> onOk());
		cancelButton.addActionListener(e -> dispose());

		applyFilters();
		int sortCol = Math.min(getInitialSortColumn(), tableModel.getColumnCount() - 1);
		if (sortCol >= 0)
		{
			java.util.List<javax.swing.RowSorter.SortKey> keys = new ArrayList<>();
			keys.add(new javax.swing.RowSorter.SortKey(sortCol, javax.swing.SortOrder.ASCENDING));
			sorter.setSortKeys(keys);
		}

		pack();
		setLocationRelativeTo(parent);

		SwingUtilities.invokeLater(searchField::requestFocus);
	}

	private List<S> loadSortedRows()
	{
		List<S> list = new ArrayList<>(getReferenceIngredients().values());
		list.sort(Comparator.comparing(V2DataObject::getName, String.CASE_INSENSITIVE_ORDER));
		return list;
	}

	private void applyFilters()
	{
		String text = searchField.getText();
		Predicate<S> textPred = s -> text == null || getFilterPredicate(text, s);
		Predicate<S> invPred = s -> true;
		if (onlyInventory.isSelected())
		{
			Map<String, InventoryLineItem> inventory = Database.getInstance().getInventory();
			invPred = s -> inventory.get(InventoryLineItem.getUniqueId(s.getName(), getIngredientType())) != null;
		}
		Predicate<S> combined = textPred.and(invPred);
		sorter.setRowFilter(new RowFilter<>()
		{
			@Override
			public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
			{
				int mi = entry.getIdentifier();
				S row = dataRows.get(mi);
				return combined.test(row);
			}
		});
	}

	private void onOk()
	{
		int view = table.getSelectedRow();
		if (view < 0)
		{
			return;
		}
		int modelRow = table.convertRowIndexToModel(view);
		S selected = dataRows.get(modelRow);
		if (mandatoryInputProvided())
		{
			output = createIngredientAddition(selected);
			dispose();
		}
	}

	protected boolean mandatoryInputProvided()
	{
		return table.getSelectedRow() >= 0;
	}

	protected boolean getFilterPredicate(String searchText, S s)
	{
		return s.getName().toLowerCase().contains(searchText.toLowerCase());
	}

	public ProcessStep getStep()
	{
		return step;
	}

	public T getOutput()
	{
		return output;
	}

	public boolean isCaptureTime()
	{
		return captureTime;
	}

	protected abstract IngredientAddition.Type getIngredientType();

	protected abstract int getTableColumnCount();

	protected abstract String getTableColumnKey(int column);

	protected abstract Object getTableCellValue(S row, int column);

	protected abstract void addUiStuffs(JPanel pane);

	protected abstract T createIngredientAddition(S selectedItem);

	protected abstract Map<String, S> getReferenceIngredients();

	protected int getInitialSortColumn()
	{
		return 0;
	}

	protected String formatInventoryCell(S row)
	{
		Map<String, InventoryLineItem> inventory = Database.getInstance().getInventory();
		InventoryLineItem ili = inventory.get(InventoryLineItem.getUniqueId(row.getName(), getIngredientType()));
		if (ili == null)
		{
			return "";
		}
		return ili.getQuantity().describe(ili.getUnit());
	}

	protected final JTable getIngredientTable()
	{
		return table;
	}

	JTable getTableForTest()
	{
		return table;
	}

	JTextField getSearchFieldForTest()
	{
		return searchField;
	}

	JCheckBox getOnlyInventoryCheckboxForTest()
	{
		return onlyInventory;
	}

	JButton getOkButtonForTest()
	{
		return okButton;
	}

	int getVisibleRowCountForTest()
	{
		return table.getRowCount();
	}

	@SuppressWarnings("unchecked")
	protected final S getSelectedReferenceIngredient()
	{
		int v = table.getSelectedRow();
		if (v < 0)
		{
			return null;
		}
		int model = table.convertRowIndexToModel(v);
		return (S)dataRows.get(model);
	}
}
