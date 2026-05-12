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
import java.util.Collection;
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
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.dialogs.EditHopDialog;

import static mclachlan.brewday.util.StringUtils.format;
import static mclachlan.brewday.util.StringUtils.getUiString;

public class HopsScreen extends JPanel implements SwingScreen
{
	private final JFrame parent;
	private final DirtyStateService dirtyState;
	private final DialogPort dialogPort;
	private final DbPort dbPort;
	private final RenameHook renameHook;
	private final DefaultTableModel model;
	private final JTable table;
	private final JTextField filterField;
	private final JPanel filterPanel;
	private final TableRowSorter<DefaultTableModel> sorter;
	private final Action saveAction;
	private final Action undoAction;
	private final Action addAction;
	private final Action editAction;
	private final Action duplicateAction;
	private final Action renameAction;
	private final Action deleteAction;
	private final Action filterAction;
	private final Action exportAction;

	public HopsScreen(JFrame parent, DirtyStateService dirtyState)
	{
		this(parent, dirtyState, new SwingDialogPort(), new DefaultDbPort(), new NoOpRenameHook());
	}

	HopsScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort)
	{
		this(parent, dirtyState, dialogPort, dbPort, new NoOpRenameHook());
	}

	HopsScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort, RenameHook renameHook)
	{
		super(new BorderLayout());
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.dialogPort = dialogPort;
		this.dbPort = dbPort;
		this.renameHook = renameHook;

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		saveAction = commandAction("editor.apply.all", "hop.save.action", SwingIcons.IconKey.EDIT, this::saveAll);
		undoAction = commandAction("editor.discard.all", "hop.undo.action", SwingIcons.IconKey.DELETE, this::undoAll);
		bar.add(button(saveAction));
		bar.add(button(undoAction));
		bar.addSeparator();
		addAction = commandAction("common.add.new", "hop.add.action", SwingIcons.IconKey.ADD_HOPS, this::addItem);
		bar.add(button(addAction));
		editAction = commandAction("common.edit", "hop.edit.action", SwingIcons.IconKey.EDIT, this::editSelected);
		duplicateAction = commandAction("common.duplicate", "hop.duplicate.action", SwingIcons.IconKey.DUPLICATE, this::duplicateSelected);
		renameAction = commandAction("editor.rename", "hop.rename.action", SwingIcons.IconKey.EDIT, this::renameSelected);
		deleteAction = commandAction("common.remove", "hop.delete.action", SwingIcons.IconKey.DELETE, this::deleteSelected);
		editAction.setEnabled(false);
		duplicateAction.setEnabled(false);
		renameAction.setEnabled(false);
		deleteAction.setEnabled(false);
		bar.add(button(editAction));
		bar.add(button(duplicateAction));
		bar.add(button(renameAction));
		bar.add(button(deleteAction));
		filterAction = commandAction("common.filter", "hop.filter.action", SwingIcons.IconKey.EDIT, this::showFilterPanel);
		bar.add(button(filterAction));
		exportAction = commandAction("common.export.csv", "hop.export.action", SwingIcons.IconKey.EXPORT_CSV, this::exportCsv);
		bar.add(button(exportAction));
		JPanel north = new JPanel(new BorderLayout());
		north.add(bar, BorderLayout.NORTH);

		filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JLabel filterLabel = new JLabel(getUiString("hop.filter.label"));
		filterField = new JTextField(20);
		filterField.setName("hop.filter.field");
		filterField.setToolTipText(getUiString("hop.filter.tooltip"));
		filterLabel.setLabelFor(filterField);
		filterPanel.add(filterLabel);
		filterPanel.add(filterField);
		filterPanel.setVisible(false);
		north.add(filterPanel, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		model = new DefaultTableModel(new String[] {
			getUiString("hop.name"),
			getUiString("hop.type"),
			getUiString("hop.form"),
			getUiString("hop.origin"),
			getUiString("hop.alpha"),
			getUiString("hop.beta"),
			getUiString("hop.humulene"),
			getUiString("hop.myrcene")
		}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		table = new JTable(model);
		table.setName("hop.table");
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
		table.getSelectionModel().addListSelectionListener(e -> updateSelectionActions());
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
		Action result = new AbstractAction(text, SwingIcons.toolbarIcon(iconKey))
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				runnable.run();
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

		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_N), "hop.hotkey.add", addAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_E), "hop.hotkey.editCtrl", editAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_R), "hop.hotkey.renameCtrl", renameAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "hop.hotkey.renameF2", renameAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_D), "hop.hotkey.duplicateCtrl", duplicateAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F), "hop.hotkey.filterCtrl", filterAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), "hop.hotkey.filterAlt", filterAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "hop.hotkey.export", exportAction);
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "hop.hotkey.export.window");
		getActionMap().put("hop.hotkey.export.window", exportAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "hop.hotkey.deleteKey", deleteAction);
		ActionHotkeySupport.bindFocused(table, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "hop.hotkey.editEnter", editAction);
		ActionHotkeySupport.bindFocused(filterField, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "hop.hotkey.export.filterFocused", exportAction);
		ActionHotkeySupport.bindFocused(filterField, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "hop.hotkey.filterEscape", new AbstractAction()
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
		boolean hasSelection = table.getSelectedRow() >= 0;
		editAction.setEnabled(hasSelection);
		duplicateAction.setEnabled(hasSelection);
		renameAction.setEnabled(hasSelection);
		deleteAction.setEnabled(hasSelection);
	}

	private Hop selected()
	{
		int row = table.getSelectedRow();
		if (row < 0)
		{
			return null;
		}
		String name = (String)model.getValueAt(table.convertRowIndexToModel(row), 0);
		return dbPort.hops().get(name);
	}

	private void addItem()
	{
		Hop draft = new Hop("");
		draft.setType(Hop.Type.BOTH);
		Hop created = dialogPort.showEditHopDialog(parent, draft, true);
		if (created == null)
		{
			return;
		}
		if (dbPort.hops().containsKey(created.getName()))
		{
			dialogPort.showError(parent, getUiString("hop.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.hops().put(created.getName(), created);
		dirtyState.markDirty(created, "reference.database", "hops");
		refresh();
	}

	private void duplicateSelected()
	{
		Hop current = selected();
		if (current == null)
		{
			return;
		}
		Hop draft = new Hop(current);
		draft.setName("");
		Hop created = dialogPort.showEditHopDialog(parent, draft, true);
		if (created == null)
		{
			return;
		}
		if (dbPort.hops().containsKey(created.getName()))
		{
			dialogPort.showError(parent, getUiString("hop.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.hops().put(created.getName(), created);
		dirtyState.markDirty(created, "reference.database", "hops");
		refresh();
	}

	private void editSelected()
	{
		Hop current = selected();
		if (current == null)
		{
			return;
		}
		Hop edited = dialogPort.showEditHopDialog(parent, new Hop(current), false);
		if (edited == null)
		{
			return;
		}
		current.setType(edited.getType());
		current.setForm(edited.getForm());
		current.setOrigin(edited.getOrigin());
		current.setAlphaAcid(edited.getAlphaAcid());
		current.setBetaAcid(edited.getBetaAcid());
		current.setHumulene(edited.getHumulene());
		current.setCaryophyllene(edited.getCaryophyllene());
		current.setCohumulone(edited.getCohumulone());
		current.setMyrcene(edited.getMyrcene());
		current.setHopStorageIndex(edited.getHopStorageIndex());
		current.setSubstitutes(edited.getSubstitutes());
		current.setDescription(edited.getDescription());
		dirtyState.markDirty(current, "reference.database", "hops");
		refresh();
	}

	private void deleteSelected()
	{
		Hop current = selected();
		if (current == null)
		{
			return;
		}
		if (!dialogPort.confirm(parent, getUiString("hop.delete.msg"), getUiString("common.remove")))
		{
			return;
		}
		dbPort.hops().remove(current.getName());
		dirtyState.markDirty("reference.database", "hops");
		refresh();
	}

	private void renameSelected()
	{
		Hop current = selected();
		if (current == null)
		{
			return;
		}
		String oldName = current.getName();
		String renamed = dialogPort.promptName(parent, getUiString("hop.rename"), getUiString("editor.rename"), oldName);
		if (renamed == null)
		{
			return;
		}
		String newName = renamed.trim();
		if (newName.isEmpty())
		{
			dialogPort.showError(parent, getUiString("hop.new.dialog.not.empty"), getUiString("ui.error"));
			return;
		}
		if (oldName.equals(newName))
		{
			return;
		}
		if (dbPort.hops().containsKey(newName))
		{
			dialogPort.showError(parent, getUiString("hop.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}

		dbPort.hops().remove(oldName);
		current.setName(newName);
		dbPort.hops().put(newName, current);
		renameHook.onHopRenamed(oldName, newName);
		dirtyState.markDirty(current, "reference.database", "hops");
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
			dbPort.loadAll();
			dirtyState.clear();
			refresh();
		}
		catch (Exception e)
		{
			dialogPort.showError(parent, e.getMessage(), getUiString("ui.error"));
		}
	}

	private void exportCsv()
	{
		File selected = dialogPort.chooseExportFile(parent, new File("hops.csv"));
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
			dialogPort.showError(parent, e.getMessage(), getUiString("ui.error"));
		}
	}

	private Collection<Hop> visibleItems()
	{
		Collection<Hop> items = new ArrayList<>();
		for (int row = 0; row < table.getRowCount(); row++)
		{
			int modelRow = table.convertRowIndexToModel(row);
			String name = (String)model.getValueAt(modelRow, 0);
			Hop item = dbPort.hops().get(name);
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
		for (Hop item : dbPort.hops().values())
		{
			model.addRow(new Object[] {
				item.getName(),
				item.getType(),
				item.getForm(),
				item.getOrigin(),
				fmtPct(item.getAlphaAcid()),
				fmtPct(item.getBetaAcid()),
				fmtPct(item.getHumulene()),
				fmtPct(item.getMyrcene())
			});
		}
	}

	private String fmtPct(PercentageUnit value)
	{
		return value == null ? "" : format(value.get(), Quantity.Unit.PERCENTAGE_DISPLAY);
	}

	private void applyFilter()
	{
		String raw = filterField.getText();
		if (raw == null || raw.trim().isEmpty())
		{
			sorter.setRowFilter(null);
			return;
		}
		String query = Pattern.quote(raw.trim());
		sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
	}

	private boolean isRowDirty(int viewRow)
	{
		if (viewRow < 0 || viewRow >= table.getRowCount())
		{
			return false;
		}
		int modelRow = table.convertRowIndexToModel(viewRow);
		String name = (String)model.getValueAt(modelRow, 0);
		Hop item = dbPort.hops().get(name);
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

	interface DialogPort
	{
		Hop showEditHopDialog(JFrame parent, Hop current, boolean createMode);

		String promptName(JFrame parent, String message, String title, String currentName);

		boolean confirm(JFrame parent, String message, String title);

		File chooseExportFile(JFrame parent, File defaultFile);

		void writeCsv(File target, Collection<Hop> hops) throws IOException;

		void showError(JFrame parent, String message, String title);
	}

	interface DbPort
	{
		Map<String, Hop> hops();

		void saveAll();

		void loadAll();
	}

	interface RenameHook
	{
		void onHopRenamed(String oldName, String newName);
	}

	static class NoOpRenameHook implements RenameHook
	{
		@Override
		public void onHopRenamed(String oldName, String newName)
		{
			// Hook for future cascading rename across recipes/batches/inventory.
		}
	}

	static class DefaultDbPort implements DbPort
	{
		@Override
		public Map<String, Hop> hops()
		{
			return Database.getInstance().getHops();
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
		public Hop showEditHopDialog(JFrame parent, Hop current, boolean createMode)
		{
			EditHopDialog dialog = new EditHopDialog(parent, current, createMode);
			dialog.setVisible(true);
			return dialog.getResult();
		}

		@Override
		public String promptName(JFrame parent, String message, String title, String currentName)
		{
			return (String)JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE, null, null, currentName);
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
		public void writeCsv(File target, Collection<Hop> hops) throws IOException
		{
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8)))
			{
				writer.println("Name,Type,Form,Origin,AlphaAcid,BetaAcid,Humulene,Caryophyllene,Cohumulone,Myrcene,HopStorageIndex,Substitutes");
				for (Hop hop : hops)
				{
					writer.printf("%s,%s,%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s%n",
						hop.getName(),
						hop.getType(),
						hop.getForm(),
						hop.getOrigin() == null ? "" : hop.getOrigin(),
						hop.getAlphaAcid() == null ? 0D : hop.getAlphaAcid().get(),
						hop.getBetaAcid() == null ? 0D : hop.getBetaAcid().get(),
						hop.getHumulene() == null ? 0D : hop.getHumulene().get(),
						hop.getCaryophyllene() == null ? 0D : hop.getCaryophyllene().get(),
						hop.getCohumulone() == null ? 0D : hop.getCohumulone().get(),
						hop.getMyrcene() == null ? 0D : hop.getMyrcene().get(),
						hop.getHopStorageIndex() == null ? 0D : hop.getHopStorageIndex().get(),
						hop.getSubstitutes() == null ? "" : hop.getSubstitutes());
				}
			}
		}

		@Override
		public void showError(JFrame parent, String message, String title)
		{
			SwingUiErrors.showError(parent, message, title);
		}
	}
}
