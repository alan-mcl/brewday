package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingIcons.IconKey;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.dialogs.AddInventoryItemDialog;
import mclachlan.brewday.recipe.IngredientAddition;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class InventoryScreen extends JPanel implements SwingScreen
{
	private final JFrame parent;
	private final DirtyStateService dirtyState;
	private final DialogPort dialogPort;
	private final DefaultTableModel model;
	private final JTable table;
	private final Action saveAction;
	private final Action undoAction;
	private final Action editAction;
	private final Action deleteAction;
	private final Action exportAction;

	public InventoryScreen(JFrame parent, DirtyStateService dirtyState)
	{
		this(parent, dirtyState, new SwingDialogPort());
	}

	InventoryScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort)
	{
		super(new BorderLayout());
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.dialogPort = dialogPort;

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		saveAction = commandAction("editor.apply.all", "inventory.save.action", IconKey.EDIT, this::saveAll);
		undoAction = commandAction("editor.discard.all", "inventory.undo.action", IconKey.DELETE, this::undoAll);
		bar.add(button(saveAction));
		bar.add(button(undoAction));
		bar.addSeparator();
		bar.add(button(addAction("inventory.add.water", "inventory.add.water.action", IconKey.ADD_WATER,
			() -> addItem(IngredientAddition.Type.WATER, "inventory.add.water", "water.name",
				new Quantity.Unit[] { Quantity.Unit.LITRES, Quantity.Unit.MILLILITRES, Quantity.Unit.US_GALLON, Quantity.Unit.US_FLUID_OUNCE }))));
		bar.add(button(addAction("inventory.add.fermentable", "inventory.add.fermentable.action", IconKey.ADD_FERMENTABLE,
			() -> addItem(IngredientAddition.Type.FERMENTABLES, "inventory.add.fermentable", "fermentable.name",
				new Quantity.Unit[] { Quantity.Unit.KILOGRAMS, Quantity.Unit.GRAMS, Quantity.Unit.POUNDS, Quantity.Unit.OUNCES }))));
		bar.add(button(addAction("inventory.add.hop", "inventory.add.hop.action", IconKey.ADD_HOPS,
			() -> addItem(IngredientAddition.Type.HOPS, "inventory.add.hop", "hop.name",
				new Quantity.Unit[] { Quantity.Unit.GRAMS, Quantity.Unit.KILOGRAMS, Quantity.Unit.OUNCES, Quantity.Unit.POUNDS }))));
		bar.add(button(addAction("inventory.add.yeast", "inventory.add.yeast.action", IconKey.ADD_YEAST,
			() -> addItem(IngredientAddition.Type.YEAST, "inventory.add.yeast",
			"yeast.name", new Quantity.Unit[] { Quantity.Unit.GRAMS, Quantity.Unit.PACKET_11_G, Quantity.Unit.KILOGRAMS, Quantity.Unit.OUNCES, Quantity.Unit.POUNDS }))));
		bar.add(button(addAction("inventory.add.misc", "inventory.add.misc.action", IconKey.ADD_MISC,
			() -> addItem(IngredientAddition.Type.MISC, "inventory.add.misc", "misc.name",
				new Quantity.Unit[] { Quantity.Unit.GRAMS, Quantity.Unit.KILOGRAMS, Quantity.Unit.OUNCES, Quantity.Unit.POUNDS }))));
		bar.addSeparator();

		editAction = commandAction("common.edit", "inventory.edit.action", IconKey.EDIT, this::editSelected);
		deleteAction = commandAction("common.remove", "inventory.delete.action", IconKey.DELETE, this::deleteSelected);
		exportAction = commandAction("common.export.csv", "inventory.export.action", IconKey.EXPORT_CSV, this::exportCsv);
		editAction.setEnabled(false);
		deleteAction.setEnabled(false);
		bar.add(button(editAction));
		bar.add(button(deleteAction));
		bar.add(button(exportAction));

		add(bar, BorderLayout.NORTH);

		model = new DefaultTableModel(new String[] {
			getUiString("inventory.ingredient"),
			getUiString("inventory.item.type"),
			getUiString("inventory.quantity")
		}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		table = new JTable(model);
		table.setName("inventory.table");
		table.setAutoCreateRowSorter(true);
		table.getSelectionModel().addListSelectionListener(e -> updateSelectionActions());
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)table.getRowSorter();
		sorter.setSortKeys(java.util.List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
		add(new JScrollPane(table), BorderLayout.CENTER);

		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		refresh();
	}

	private Action addAction(String key, String actionKey, IconKey iconKey, Runnable action)
	{
		return commandAction(key, actionKey, iconKey, action);
	}

	private Action commandAction(String key, String actionKey, IconKey iconKey, Runnable action)
	{
		String text = getUiString(key);
		Action result = new AbstractAction(text, SwingIcons.toolbarIcon(iconKey))
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				action.run();
			}
		};
		result.putValue(Action.SHORT_DESCRIPTION, text);
		result.putValue(Action.ACTION_COMMAND_KEY, actionKey);
		return result;
	}

	private JButton button(Action action)
	{
		JButton button = new JButton(action);
		button.setText((String)action.getValue(Action.NAME));
		return button;
	}

	private void updateSelectionActions()
	{
		boolean hasSelection = table.getSelectedRow() >= 0;
		editAction.setEnabled(hasSelection);
		deleteAction.setEnabled(hasSelection);
	}

	private void addItem(IngredientAddition.Type type, String titleKey, String nameKey, Quantity.Unit[] units)
	{
		AddInventoryItemDialog d = new AddInventoryItemDialog(parent, type, titleKey, nameKey, units);
		InventoryLineItem item = dialogPort.showAddItemDialog(parent, d);
		if (item != null)
		{
			Database.getInstance().getInventory().put(item.getName(), item);
			dirtyState.markDirty(item, "inventory");
			refresh();
		}
	}

	private InventoryLineItem selected()
	{
		int row = table.getSelectedRow();
		if (row < 0)
		{
			return null;
		}
		int modelRow = table.convertRowIndexToModel(row);
		String ingredient = (String)model.getValueAt(modelRow, 0);
		String type = (String)model.getValueAt(modelRow, 1);
		for (InventoryLineItem item : Database.getInstance().getInventory().values())
		{
			if (ingredient.equals(item.getIngredient()) && type.equals(item.getType().toString()))
			{
				return item;
			}
		}
		return null;
	}

	private void editSelected()
	{
		InventoryLineItem item = selected();
		if (item == null)
		{
			return;
		}

		Double quantity = dialogPort.promptEditQuantity(parent, item.getQuantity().get(item.getUnit()));
		if (quantity != null)
		{
			item.setQuantity(Quantity.parseQuantity(String.valueOf(quantity), item.getUnit()));
			dirtyState.markDirty(item, "inventory");
			refresh();
		}
	}

	private void deleteSelected()
	{
		InventoryLineItem item = selected();
		if (item == null)
		{
			return;
		}

		if (dialogPort.confirmDelete(parent, getUiString("editor.delete.msg"), getUiString("common.remove")))
		{
			Database.getInstance().getInventory().remove(item.getName());
			dirtyState.markDirty("inventory");
			refresh();
		}
	}

	private void exportCsv()
	{
		File selected = dialogPort.chooseExportFile(parent, new File("inventory.csv"));
		if (selected == null)
		{
			return;
		}
		try
		{
			dialogPort.writeCsv(selected, Database.getInstance().getInventory().values());
		}
		catch (Exception e)
		{
			dialogPort.showError(parent, e.getMessage(), getUiString("ui.error"));
		}
	}

	private void saveAll()
	{
		if (!dialogPort.confirm(parent, getUiString("editor.apply.all.msg"), getUiString("editor.apply.all")))
		{
			return;
		}
		try
		{
			Database.getInstance().saveAll();
			dirtyState.clear();
			refresh();
		}
		catch (Exception e)
		{
			dialogPort.showError(parent, e.getMessage(), getUiString("ui.error"));
		}
	}

	private void undoAll()
	{
		if (!dialogPort.confirm(parent, getUiString("editor.discard.all.msg"), getUiString("editor.discard.all")))
		{
			return;
		}
		try
		{
			Database.getInstance().loadAll();
			dirtyState.clear();
			refresh();
		}
		catch (Exception e)
		{
			dialogPort.showError(parent, e.getMessage(), getUiString("ui.error"));
		}
	}

	@Override
	public void refresh()
	{
		model.setRowCount(0);
		for (InventoryLineItem item : Database.getInstance().getInventory().values())
		{
			model.addRow(new Object[] {
				item.getIngredient(),
				item.getType().toString(),
				String.format("%.3f %s", item.getQuantity().get(item.getUnit()), item.getUnit())
			});
		}
	}

	Action getEditAction()
	{
		return editAction;
	}

	Action getSaveAction()
	{
		return saveAction;
	}

	Action getUndoAction()
	{
		return undoAction;
	}

	Action getDeleteAction()
	{
		return deleteAction;
	}

	Action getExportAction()
	{
		return exportAction;
	}

	JTable getTable()
	{
		return table;
	}

	DefaultTableModel getModel()
	{
		return model;
	}

	interface DialogPort
	{
		InventoryLineItem showAddItemDialog(JFrame parent, AddInventoryItemDialog dialog);

		Double promptEditQuantity(JFrame parent, double currentValue);

		boolean confirmDelete(JFrame parent, String message, String title);

		boolean confirm(JFrame parent, String message, String title);

		File chooseExportFile(JFrame parent, File defaultFile);

		void writeCsv(File file, Iterable<InventoryLineItem> items) throws IOException;

		void showError(JFrame parent, String message, String title);
	}

	static class SwingDialogPort implements DialogPort
	{
		@Override
		public InventoryLineItem showAddItemDialog(JFrame parent, AddInventoryItemDialog dialog)
		{
			dialog.setVisible(true);
			return dialog.getResult();
		}

		@Override
		public Double promptEditQuantity(JFrame parent, double currentValue)
		{
			JSpinner quantity = new JSpinner(new SpinnerNumberModel(currentValue, 0.0, 10000.0, 0.1));
			JPanel panel = new JPanel();
			panel.add(new JLabel(getUiString("inventory.quantity") + ":"));
			panel.add(quantity);
			int result = JOptionPane.showConfirmDialog(parent, panel, getUiString("common.edit"), JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION)
			{
				return ((Number)quantity.getValue()).doubleValue();
			}
			return null;
		}

		@Override
		public boolean confirmDelete(JFrame parent, String message, String title)
		{
			int result = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION);
			return result == JOptionPane.YES_OPTION;
		}

		@Override
		public boolean confirm(JFrame parent, String message, String title)
		{
			int result = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION);
			return result == JOptionPane.YES_OPTION;
		}

		@Override
		public File chooseExportFile(JFrame parent, File defaultFile)
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setSelectedFile(defaultFile);
			if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
			{
				return null;
			}
			return chooser.getSelectedFile();
		}

		@Override
		public void writeCsv(File file, Iterable<InventoryLineItem> items) throws IOException
		{
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)))
			{
				writer.println("Ingredient,Type,Quantity");
				for (InventoryLineItem item : items)
				{
					writer.printf("%s,%s,%.3f %s%n",
						item.getIngredient(),
						item.getType(),
						item.getQuantity().get(item.getUnit()),
						item.getUnit());
				}
			}
		}

		@Override
		public void showError(JFrame parent, String message, String title)
		{
			JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
		}
	}
}
