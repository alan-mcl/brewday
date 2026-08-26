package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.UiQuantityDisplay;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.EntityListToolbarTooltips;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingIcons.IconKey;
import mclachlan.brewday.ui.swing.widgets.IngredientNameTableCellRenderer;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
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
	private final Action filterAction;
	private final Action exportAction;
	private final JTextField filterField;
	private final JPanel filterPanel;
	private final TableRowSorter<DefaultTableModel> sorter;
	private final ArrayList<InventoryLineItem> modelLineItems = new ArrayList<>();

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
		saveAction = commandAction("editor.apply.all", "inventory.save.action", IconKey.SAVE, this::saveAll);
		undoAction = commandAction("editor.discard.all", "inventory.undo.action", IconKey.UNDO, this::undoAll);
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
		filterAction = commandAction("common.filter", "inventory.filter.action", IconKey.FILTER, this::showFilterPanel);
		exportAction = commandAction("common.export.csv", "inventory.export.action", IconKey.EXPORT_CSV, this::exportCsv);
		editAction.setEnabled(false);
		deleteAction.setEnabled(false);
		bar.add(button(editAction));
		bar.add(button(deleteAction));
		bar.add(button(filterAction));
		bar.add(button(exportAction));

		JPanel north = new JPanel(new BorderLayout());
		north.add(bar, BorderLayout.NORTH);
		filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JLabel filterLabel = new JLabel(getUiString("inventory.filter.label"));
		filterField = new JTextField(20);
		filterField.setName("inventory.filter.field");
		filterField.setToolTipText(getUiString("inventory.filter.tooltip"));
		filterLabel.setLabelFor(filterField);
		filterPanel.add(filterLabel);
		filterPanel.add(filterField);
		filterPanel.setVisible(false);
		north.add(filterPanel, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

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
		table.setRowHeight(SwingIcons.TABLE_ROW_HEIGHT);
		table.getColumnModel().getColumn(0).setCellRenderer(new IngredientNameTableCellRenderer(
			modelRow ->
			{
				if (modelRow < 0 || modelRow >= modelLineItems.size())
				{
					return SwingIcons.emptyIcon();
				}
				return SwingIcons.iconForInventoryLine(modelLineItems.get(modelRow));
			},
			(t, row) -> isRowDirty(row)));
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
			{
				Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				Font base = table.getFont();
				component.setFont(base.deriveFont(isRowDirty(row) ? Font.BOLD : Font.PLAIN));
				return component;
			}
		});
		table.setAutoCreateRowSorter(true);
		sorter = (TableRowSorter<DefaultTableModel>)table.getRowSorter();
		sorter.setSortKeys(java.util.List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
		filterField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				applyFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				applyFilter();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				applyFilter();
			}
		});
		table.getSelectionModel().addListSelectionListener(e -> updateSelectionActions());
		table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() >= 2 && table.getSelectedRow() >= 0 && editAction.isEnabled())
				{
					editAction.actionPerformed(null);
				}
			}
		});
		add(new JScrollPane(table), BorderLayout.CENTER);

		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		wireHotkeys();
		refresh();
	}

	private void wireHotkeys()
	{
		ActionHotkeySupport.setMnemonic(saveAction, KeyEvent.VK_S);
		ActionHotkeySupport.setMnemonic(undoAction, KeyEvent.VK_U);
		ActionHotkeySupport.setMnemonic(editAction, KeyEvent.VK_E);
		ActionHotkeySupport.setMnemonic(filterAction, KeyEvent.VK_F);
		ActionHotkeySupport.setMnemonic(exportAction, KeyEvent.VK_X);

		EntityListToolbarTooltips.wireInventoryToolbar(
			saveAction, undoAction, editAction, deleteAction, filterAction, exportAction);

		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_E), "inventory.hotkey.edit", editAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "inventory.hotkey.delete", deleteAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F), "inventory.hotkey.filterCtrl", filterAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), "inventory.hotkey.filterAlt", filterAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "inventory.hotkey.export", exportAction);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "inventory.hotkey.exportWin");
		getActionMap().put("inventory.hotkey.exportWin", exportAction);
		ActionHotkeySupport.bindFocused(table, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "inventory.hotkey.enter", editAction);
		ActionHotkeySupport.bindFocused(filterField, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "inventory.hotkey.exportFilter", exportAction);
		ActionHotkeySupport.bindFocused(filterField, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "inventory.hotkey.filterEscape", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				hideFilterPanel();
			}
		});
	}

	private void applyFilter()
	{
		String raw = filterField.getText();
		if (raw == null || raw.trim().isEmpty())
		{
			sorter.setRowFilter(null);
			return;
		}
		sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(raw.trim())));
	}

	private boolean isRowDirty(int viewRow)
	{
		if (viewRow < 0 || viewRow >= table.getRowCount())
		{
			return false;
		}
		int modelRow = table.convertRowIndexToModel(viewRow);
		if (modelRow < 0 || modelRow >= modelLineItems.size())
		{
			return false;
		}
		InventoryLineItem item = modelLineItems.get(modelRow);
		return item != null && dirtyState.isDirty(item);
	}

	private void showFilterPanel()
	{
		filterPanel.setVisible(true);
		filterPanel.revalidate();
		filterPanel.repaint();
		filterField.requestFocusInWindow();
		filterField.selectAll();
	}

	private void hideFilterPanel()
	{
		filterField.setText("");
		filterPanel.setVisible(false);
		filterPanel.revalidate();
		filterPanel.repaint();
		table.requestFocusInWindow();
	}

	private List<InventoryLineItem> visibleItems()
	{
		ArrayList<InventoryLineItem> items = new ArrayList<>();
		for (int row = 0; row < table.getRowCount(); row++)
		{
			int modelRow = table.convertRowIndexToModel(row);
			if (modelRow >= 0 && modelRow < modelLineItems.size())
			{
				InventoryLineItem item = modelLineItems.get(modelRow);
				if (item != null)
				{
					items.add(item);
				}
			}
		}
		return items;
	}

	private Action addAction(String key, String actionKey, IconKey iconKey, Runnable action)
	{
		Action result = commandAction(key, actionKey, iconKey, action);
		String tooltipKey = actionKey.replace(".action", ".tooltip");
		ActionHotkeySupport.applyTooltipText(result, tooltipKey);
		return result;
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
			dialogPort.writeCsv(selected, visibleItems());
		}
		catch (Exception e)
		{
			dialogPort.showError(parent, e, getUiString("ui.error"));
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
			dialogPort.showError(parent, e, getUiString("ui.error"));
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
			dialogPort.showError(parent, e, getUiString("ui.error"));
		}
	}

	@Override
	public void refresh()
	{
		model.setRowCount(0);
		modelLineItems.clear();
		for (InventoryLineItem item : Database.getInstance().getInventory().values())
		{
			model.addRow(new Object[] {
				item.getIngredient(),
				item.getType().toString(),
				UiQuantityDisplay.formatInventoryQuantity(
					item,
					Database.getInstance().getSettings())
			});
			modelLineItems.add(item);
		}
		applyFilter();
	}

	int rowFontStyle(int viewRow)
	{
		Component comp = table.prepareRenderer(table.getCellRenderer(viewRow, 0), viewRow, 0);
		return comp.getFont().getStyle();
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

	Action getFilterAction()
	{
		return filterAction;
	}

	JTextField getFilterField()
	{
		return filterField;
	}

	boolean isFilterPanelVisible()
	{
		return filterPanel.isVisible();
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

		void showError(JFrame parent, Throwable throwable, String title);
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
					writer.printf("%s,%s,%s%n",
						item.getIngredient(),
						item.getType(),
						UiQuantityDisplay.formatInventoryQuantity(
							item,
							Database.getInstance().getSettings()));
				}
			}
		}

		@Override
		public void showError(JFrame parent, String message, String title)
		{
			SwingUiErrors.showError(parent, message, title);
		}

		@Override
		public void showError(JFrame parent, Throwable throwable, String title)
		{
			SwingUiErrors.showError(parent, throwable, title);
		}
	}
}
