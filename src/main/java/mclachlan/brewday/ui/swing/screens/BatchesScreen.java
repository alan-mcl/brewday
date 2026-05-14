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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
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
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.BatchEditorNavPort;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.dialogs.NewBatchDialog;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class BatchesScreen extends JPanel implements SwingScreen
{
	private static final int COL_DATE_SORT = 4;

	private final JFrame parent;
	private final DirtyStateService dirtyState;
	private final DialogPort dialogPort;
	private final DbPort dbPort;
	private final RenameHook renameHook;
	private final DeleteHook deleteHook;
	private final BatchEditorNavPort batchEditorNav;
	private final DefaultTableModel model;
	private final JTable table;
	private final JTextField filterField;
	private final JPanel filterPanel;
	private final TableRowSorter<DefaultTableModel> sorter;
	private final Action saveAction, undoAction, addAction, editAction, duplicateAction, renameAction, deleteAction, filterAction, exportAction;
	private final DateTimeFormatter dateDisplay = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault());

	public BatchesScreen(JFrame parent, DirtyStateService dirtyState)
	{
		this(parent, dirtyState, null);
	}

	public BatchesScreen(JFrame parent, DirtyStateService dirtyState, BatchEditorNavPort batchEditorNav)
	{
		this(parent, dirtyState, new SwingDialogPort(), new DefaultDbPort(), new NoOpRenameHook(), new NoOpDeleteHook(), batchEditorNav);
	}

	BatchesScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort)
	{
		this(parent, dirtyState, dialogPort, dbPort, new NoOpRenameHook(), new NoOpDeleteHook(), null);
	}

	BatchesScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort,
		RenameHook renameHook, DeleteHook deleteHook)
	{
		this(parent, dirtyState, dialogPort, dbPort, renameHook, deleteHook, null);
	}

	BatchesScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort,
		RenameHook renameHook, DeleteHook deleteHook, BatchEditorNavPort batchEditorNav)
	{
		super(new BorderLayout());
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.dialogPort = dialogPort;
		this.dbPort = dbPort;
		this.renameHook = renameHook;
		this.deleteHook = deleteHook;
		this.batchEditorNav = batchEditorNav;

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		saveAction = commandAction("editor.apply.all", "batch.save.action", SwingIcons.IconKey.EDIT, this::saveAll);
		undoAction = commandAction("editor.discard.all", "batch.undo.action", SwingIcons.IconKey.DELETE, this::undoAll);
		addAction = commandAction("common.add.new", "batch.add.action", SwingIcons.IconKey.BEER, this::addItem);
		editAction = commandAction("common.edit", "batch.edit.action", SwingIcons.IconKey.EDIT, this::editSelected);
		duplicateAction = commandAction("common.duplicate", "batch.duplicate.action", SwingIcons.IconKey.DUPLICATE, this::duplicateSelected);
		renameAction = commandAction("editor.rename", "batch.rename.action", SwingIcons.IconKey.EDIT, this::renameSelected);
		deleteAction = commandAction("common.remove", "batch.delete.action", SwingIcons.IconKey.DELETE, this::deleteSelected);
		filterAction = commandAction("common.filter", "batch.filter.action", SwingIcons.IconKey.EDIT, this::showFilterPanel);
		exportAction = commandAction("common.export.csv", "batch.export.action", SwingIcons.IconKey.EXPORT_CSV, this::exportCsv);
		editAction.setEnabled(false);
		duplicateAction.setEnabled(false);
		renameAction.setEnabled(false);
		deleteAction.setEnabled(false);
		bar.add(button(saveAction));
		bar.add(button(undoAction));
		bar.addSeparator();
		bar.add(button(addAction));
		bar.add(button(editAction));
		bar.add(button(duplicateAction));
		bar.add(button(renameAction));
		bar.add(button(deleteAction));
		bar.add(button(filterAction));
		bar.add(button(exportAction));

		JPanel north = new JPanel(new BorderLayout());
		north.add(bar, BorderLayout.NORTH);
		filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JLabel filterLabel = new JLabel(getUiString("batch.filter.label"));
		filterField = new JTextField(16);
		filterField.setName("batch.filter.field");
		filterField.setToolTipText(getUiString("batch.filter.tooltip"));
		filterLabel.setLabelFor(filterField);
		filterPanel.add(filterLabel);
		filterPanel.add(filterField);
		filterPanel.setVisible(false);
		north.add(filterPanel, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		model = new DefaultTableModel(new String[] {
			getUiString("batch.name"),
			getUiString("batch.recipe"),
			getUiString("batch.date"),
			getUiString("batch.desc"),
			""
		}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}

			@Override
			public Class<?> getColumnClass(int columnIndex)
			{
				return columnIndex == COL_DATE_SORT ? LocalDate.class : String.class;
			}
		};
		table = new JTable(model);
		table.setName("batch.table");
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
			{
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				Font base = table.getFont();
				c.setFont(base.deriveFont(isRowDirty(row) ? Font.BOLD : Font.PLAIN));
				return c;
			}
		});
		table.setAutoCreateRowSorter(true);
		sorter = (TableRowSorter<DefaultTableModel>)table.getRowSorter();
		sorter.setComparator(COL_DATE_SORT, Comparator.nullsLast(Comparator.naturalOrder()));
		sorter.setSortKeys(java.util.List.of(new RowSorter.SortKey(COL_DATE_SORT, SortOrder.DESCENDING)));
		if (table.getColumnModel().getColumnCount() > COL_DATE_SORT)
		{
			table.getColumnModel().getColumn(COL_DATE_SORT).setMinWidth(0);
			table.getColumnModel().getColumn(COL_DATE_SORT).setMaxWidth(0);
			table.getColumnModel().getColumn(COL_DATE_SORT).setPreferredWidth(0);
			table.getColumnModel().getColumn(COL_DATE_SORT).setResizable(false);
		}
		table.getSelectionModel().addListSelectionListener(e -> updateSelectionActions());
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

	private Action commandAction(String key, String actionKey, SwingIcons.IconKey iconKey, Runnable runnable)
	{
		String text = getUiString(key);
		Action a = new AbstractAction(text, SwingIcons.toolbarIcon(iconKey))
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				runnable.run();
			}
		};
		a.putValue(Action.SHORT_DESCRIPTION, text);
		a.putValue(Action.ACTION_COMMAND_KEY, actionKey);
		return a;
	}

	private JButton button(Action action)
	{
		JButton b = new JButton(action);
		b.setText((String)action.getValue(Action.NAME));
		return b;
	}

	private void wireHotkeys()
	{
		ActionHotkeySupport.setMnemonic(saveAction, KeyEvent.VK_S);
		ActionHotkeySupport.setMnemonic(undoAction, KeyEvent.VK_U);
		ActionHotkeySupport.setMnemonic(addAction, KeyEvent.VK_N);
		ActionHotkeySupport.setMnemonic(editAction, KeyEvent.VK_E);
		ActionHotkeySupport.setMnemonic(duplicateAction, KeyEvent.VK_D);
		ActionHotkeySupport.setMnemonic(renameAction, KeyEvent.VK_R);
		ActionHotkeySupport.setMnemonic(filterAction, KeyEvent.VK_F);
		ActionHotkeySupport.setMnemonic(exportAction, KeyEvent.VK_X);
		ActionHotkeySupport.setTooltip(saveAction, "Save All (Alt+S toolbar; Ctrl/Cmd+S anywhere in main window)");
		ActionHotkeySupport.setTooltip(undoAction, "Undo All (Alt+U; Ctrl/Cmd+U or Ctrl/Cmd+Z in main window)");
		ActionHotkeySupport.setTooltip(addAction, "Add New (Alt+N, Ctrl/Cmd+N)");
		ActionHotkeySupport.setTooltip(editAction, "Edit (Alt+E, Ctrl/Cmd+E, Enter, Double-click)");
		ActionHotkeySupport.setTooltip(duplicateAction, "Duplicate (Alt+D, Ctrl/Cmd+D)");
		ActionHotkeySupport.setTooltip(renameAction, "Rename (Alt+R, Ctrl/Cmd+R, F2)");
		ActionHotkeySupport.setTooltip(deleteAction, "Delete (Delete)");
		ActionHotkeySupport.setTooltip(filterAction, "Filter (Alt+F, Ctrl/Cmd+F, Escape hides)");
		ActionHotkeySupport.setTooltip(exportAction, "Export CSV (Alt+X, Ctrl/Cmd+X)");
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_N), "batch.hotkey.add", addAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_E), "batch.hotkey.editCtrl", editAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_R), "batch.hotkey.renameCtrl", renameAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "batch.hotkey.renameF2", renameAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_D), "batch.hotkey.duplicateCtrl", duplicateAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F), "batch.hotkey.filterCtrl", filterAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), "batch.hotkey.filterAlt", filterAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "batch.hotkey.export", exportAction);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "batch.hotkey.export.window");
		getActionMap().put("batch.hotkey.export.window", exportAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "batch.hotkey.deleteKey", deleteAction);
		ActionHotkeySupport.bindFocused(table, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "batch.hotkey.editEnter", editAction);
		ActionHotkeySupport.bindFocused(filterField, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "batch.hotkey.export.filterFocused", exportAction);
		ActionHotkeySupport.bindFocused(filterField, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "batch.hotkey.filterEscape", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				hideFilterPanel();
			}
		});
	}

	private void updateSelectionActions()
	{
		boolean has = table.getSelectedRow() >= 0;
		editAction.setEnabled(has);
		duplicateAction.setEnabled(has);
		renameAction.setEnabled(has);
		deleteAction.setEnabled(has);
	}

	private Batch selected()
	{
		int row = table.getSelectedRow();
		if (row < 0)
		{
			return null;
		}
		String id = (String)model.getValueAt(table.convertRowIndexToModel(row), 0);
		return dbPort.batches().get(id);
	}

	private void addItem()
	{
		Batch created = dialogPort.showNewBatchDialog(parent);
		if (created == null)
		{
			return;
		}
		if (dbPort.batches().containsKey(created.getName()))
		{
			dialogPort.showError(parent, getUiString("batch.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.batches().put(created.getName(), created);
		dirtyState.markDirty(created, "batches");
		refresh();
	}

	private void duplicateSelected()
	{
		Batch current = selected();
		if (current == null)
		{
			return;
		}
		String renamed = dialogPort.promptName(parent, getUiString("batch.copy"), getUiString("batch.duplicate"), "");
		if (renamed == null)
		{
			return;
		}
		String newName = renamed.trim();
		if (newName.isEmpty())
		{
			dialogPort.showError(parent, getUiString("batch.new.dialog.not.empty"), getUiString("ui.error"));
			return;
		}
		if (dbPort.batches().containsKey(newName))
		{
			dialogPort.showError(parent, getUiString("batch.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		Batch copy = new Batch(current);
		copy.setName(newName);
		dbPort.batches().put(newName, copy);
		dirtyState.markDirty(copy, "batches");
		refresh();
	}

	private void editSelected()
	{
		Batch current = selected();
		if (current == null)
		{
			return;
		}
		if (batchEditorNav != null)
		{
			batchEditorNav.openBatchEditor(current.getName());
		}
	}

	private void deleteSelected()
	{
		Batch current = selected();
		if (current == null)
		{
			return;
		}
		if (!dialogPort.confirm(parent, getUiString("batch.delete.msg"), getUiString("batch.delete.confirm.title")))
		{
			return;
		}
		String id = current.getName();
		dbPort.batches().remove(id);
		deleteHook.onBatchDeleted(id);
		dirtyState.markDirty("batches");
		refresh();
	}

	private void renameSelected()
	{
		Batch current = selected();
		if (current == null)
		{
			return;
		}
		String oldId = current.getName();
		String renamed = dialogPort.promptName(parent, getUiString("batch.rename"), getUiString("editor.rename"), oldId);
		if (renamed == null)
		{
			return;
		}
		String newId = renamed.trim();
		if (newId.isEmpty())
		{
			dialogPort.showError(parent, getUiString("batch.new.dialog.not.empty"), getUiString("ui.error"));
			return;
		}
		if (oldId.equals(newId))
		{
			return;
		}
		if (dbPort.batches().containsKey(newId))
		{
			dialogPort.showError(parent, getUiString("batch.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.batches().remove(oldId);
		current.setName(newId);
		dbPort.batches().put(newId, current);
		renameHook.onBatchRenamed(oldId, newId);
		dirtyState.markDirty(current, "batches");
		refresh();
	}

	private void saveAll()
	{
		if (!dialogPort.confirm(parent, getUiString("editor.apply.all.msg"), getUiString("editor.apply.all")))
		{
			return;
		}
		try
		{
			dbPort.saveAll();
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
			dbPort.loadAll();
			dirtyState.clear();
			refresh();
		}
		catch (Exception e)
		{
			dialogPort.showError(parent, e, getUiString("ui.error"));
		}
	}

	private void exportCsv()
	{
		File selected = dialogPort.chooseExportFile(parent, new File("batches.csv"));
		if (selected == null)
		{
			return;
		}
		try
		{
			dialogPort.writeBatchCsv(selected, visibleBatches());
		}
		catch (Exception e)
		{
			dialogPort.showError(parent, e, getUiString("ui.error"));
		}
	}

	private Collection<Batch> visibleBatches()
	{
		Collection<Batch> items = new ArrayList<>();
		for (int row = 0; row < table.getRowCount(); row++)
		{
			int modelRow = table.convertRowIndexToModel(row);
			String id = (String)model.getValueAt(modelRow, 0);
			Batch item = dbPort.batches().get(id);
			if (item != null)
			{
				items.add(item);
			}
		}
		return items;
	}

	@Override
	public void refresh()
	{
		model.setRowCount(0);
		for (Batch b : dbPort.batches().values())
		{
			LocalDate d = b.getDate();
			String dateStr = d == null ? "" : d.format(dateDisplay);
			model.addRow(new Object[] {
				b.getName(),
				b.getRecipe() == null ? "" : b.getRecipe(),
				dateStr,
				b.getDescription() == null ? "" : b.getDescription(),
				d
			});
		}
		applyFilter();
	}

	private void applyFilter()
	{
		final String raw = filterField.getText();
		final String trimmed = raw == null ? "" : raw.trim();

		sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>()
		{
			@Override
			public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry)
			{
				int modelRow = entry.getIdentifier();
				if (trimmed.isEmpty())
				{
					return true;
				}
				StringBuilder rowText = new StringBuilder();
				for (int c = 0; c < model.getColumnCount(); c++)
				{
					if (c == COL_DATE_SORT)
					{
						continue;
					}
					Object v = model.getValueAt(modelRow, c);
					if (v != null)
					{
						rowText.append(v).append('\t');
					}
				}
				Object sortVal = model.getValueAt(modelRow, COL_DATE_SORT);
				if (sortVal != null)
				{
					rowText.append(sortVal).append('\t');
				}
				return Pattern.compile("(?i)" + Pattern.quote(trimmed)).matcher(rowText).find();
			}
		});
	}

	private boolean isRowDirty(int viewRow)
	{
		if (viewRow < 0 || viewRow >= table.getRowCount())
		{
			return false;
		}
		int modelRow = table.convertRowIndexToModel(viewRow);
		String id = (String)model.getValueAt(modelRow, 0);
		Batch item = dbPort.batches().get(id);
		return dirtyState.isDirty(item);
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

	JTable getTable()
	{
		return table;
	}

	Action getSaveAction()
	{
		return saveAction;
	}

	Action getAddAction()
	{
		return addAction;
	}

	Action getExportAction()
	{
		return exportAction;
	}

	Action getEditAction()
	{
		return editAction;
	}

	Action getDuplicateAction()
	{
		return duplicateAction;
	}

	Action getRenameAction()
	{
		return renameAction;
	}

	Action getDeleteAction()
	{
		return deleteAction;
	}

	Action getUndoAction()
	{
		return undoAction;
	}

	Action getFilterAction()
	{
		return filterAction;
	}

	JTextField getFilterField()
	{
		return filterField;
	}

	TableRowSorter<DefaultTableModel> getRowSorter()
	{
		return sorter;
	}

	int rowFontStyle(int viewRow)
	{
		Component comp = table.prepareRenderer(table.getCellRenderer(viewRow, 0), viewRow, 0);
		return comp.getFont().getStyle();
	}

	interface DialogPort
	{
		Batch showNewBatchDialog(JFrame parent);

		String promptName(JFrame parent, String message, String title, String currentName);

		boolean confirm(JFrame parent, String message, String title);

		File chooseExportFile(JFrame parent, File defaultFile);

		void writeBatchCsv(File target, Collection<Batch> batches) throws IOException;

		void showError(JFrame parent, String message, String title);

		void showError(JFrame parent, Throwable throwable, String title);
	}

	interface DbPort
	{
		Map<String, Batch> batches();

		void saveAll();

		void loadAll();
	}

	public interface RenameHook
	{
		void onBatchRenamed(String oldId, String newId);
	}

	public interface DeleteHook
	{
		void onBatchDeleted(String batchId);
	}

	static class NoOpRenameHook implements RenameHook
	{
		@Override
		public void onBatchRenamed(String oldId, String newId)
		{
		}
	}

	static class NoOpDeleteHook implements DeleteHook
	{
		@Override
		public void onBatchDeleted(String batchId)
		{
		}
	}

	static class DefaultDbPort implements DbPort
	{
		@Override
		public Map<String, Batch> batches()
		{
			return Database.getInstance().getBatches();
		}

		@Override
		public void saveAll()
		{
			Database.getInstance().saveAll();
		}

		@Override
		public void loadAll()
		{
			Database.getInstance().loadAll();
		}
	}

	static class SwingDialogPort implements DialogPort
	{
		@Override
		public Batch showNewBatchDialog(JFrame parent)
		{
			NewBatchDialog d = new NewBatchDialog(parent);
			d.setVisible(true);
			return d.getResult();
		}

		@Override
		public String promptName(JFrame parent, String message, String title, String currentName)
		{
			return (String)JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE, null, null, currentName);
		}

		@Override
		public boolean confirm(JFrame parent, String message, String title)
		{
			int r = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION);
			return r == JOptionPane.YES_OPTION;
		}

		@Override
		public File chooseExportFile(JFrame parent, File defaultFile)
		{
			JFileChooser c = new JFileChooser();
			c.setSelectedFile(defaultFile);
			if (c.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
			{
				return null;
			}
			return c.getSelectedFile();
		}

		@Override
		public void writeBatchCsv(File target, Collection<Batch> batches) throws IOException
		{
			try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8)))
			{
				w.println("Name,Recipe,Date,Description");
				for (Batch b : batches)
				{
					String name = csvEscape(b.getName());
					String recipe = csvEscape(b.getRecipe() == null ? "" : b.getRecipe());
					String dateIso = b.getDate() == null ? "" : b.getDate().toString();
					String desc = csvEscape(b.getDescription() == null ? "" : b.getDescription());
					w.printf("%s,%s,%s,%s%n", name, recipe, dateIso, desc);
				}
			}
		}

		private static String csvEscape(String s)
		{
			if (s.indexOf(',') < 0 && s.indexOf('"') < 0 && s.indexOf('\n') < 0 && s.indexOf('\r') < 0)
			{
				return s;
			}
			return "\"" + s.replace("\"", "\"\"") + "\"";
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
