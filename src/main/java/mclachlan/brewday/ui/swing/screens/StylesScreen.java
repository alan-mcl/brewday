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
import mclachlan.brewday.style.Style;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.dialogs.EditStyleDialog;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class StylesScreen extends JPanel implements SwingScreen
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
	private final Action saveAction, undoAction, addAction, editAction, duplicateAction, renameAction, deleteAction, filterAction, exportAction;

	public StylesScreen(JFrame parent, DirtyStateService dirtyState)
	{
		this(parent, dirtyState, new SwingDialogPort(), new DefaultDbPort(), new NoOpRenameHook());
	}

	StylesScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort)
	{
		this(parent, dirtyState, dialogPort, dbPort, new NoOpRenameHook());
	}

	StylesScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort, RenameHook renameHook)
	{
		super(new BorderLayout());
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.dialogPort = dialogPort;
		this.dbPort = dbPort;
		this.renameHook = renameHook;

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		saveAction = commandAction("editor.apply.all", "style.save.action", SwingIcons.IconKey.EDIT, this::saveAll);
		undoAction = commandAction("editor.discard.all", "style.undo.action", SwingIcons.IconKey.DELETE, this::undoAll);
		addAction = commandAction("common.add", "style.add.action", SwingIcons.IconKey.STYLES, this::addItem);
		editAction = commandAction("common.edit", "style.edit.action", SwingIcons.IconKey.EDIT, this::editSelected);
		duplicateAction = commandAction("common.duplicate", "style.duplicate.action", SwingIcons.IconKey.DUPLICATE, this::duplicateSelected);
		duplicateAction.putValue(Action.NAME, "Duplicate");
		renameAction = commandAction("editor.rename", "style.rename.action", SwingIcons.IconKey.EDIT, this::renameSelected);
		deleteAction = commandAction("common.remove", "style.delete.action", SwingIcons.IconKey.DELETE, this::deleteSelected);
		filterAction = commandAction("common.edit", "style.filter.action", SwingIcons.IconKey.EDIT, this::showFilterPanel);
		exportAction = commandAction("common.export.csv", "style.export.action", SwingIcons.IconKey.EXPORT_CSV, this::exportCsv);
		addAction.putValue(Action.NAME, "Add New");
		deleteAction.putValue(Action.NAME, "Delete");
		filterAction.putValue(Action.NAME, "Filter");
		editAction.setEnabled(false); duplicateAction.setEnabled(false); renameAction.setEnabled(false); deleteAction.setEnabled(false);
		bar.add(button(saveAction)); bar.add(button(undoAction)); bar.addSeparator();
		bar.add(button(addAction)); bar.add(button(editAction)); bar.add(button(duplicateAction)); bar.add(button(renameAction)); bar.add(button(deleteAction)); bar.add(button(filterAction)); bar.add(button(exportAction));

		JPanel north = new JPanel(new BorderLayout());
		north.add(bar, BorderLayout.NORTH);
		filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JLabel filterLabel = new JLabel(getUiString("style.filter.label"));
		filterField = new JTextField(20);
		filterField.setName("style.filter.field");
		filterField.setToolTipText(getUiString("style.filter.tooltip"));
		filterLabel.setLabelFor(filterField);
		filterPanel.add(filterLabel); filterPanel.add(filterField); filterPanel.setVisible(false);
		north.add(filterPanel, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		model = new DefaultTableModel(new String[] {
			getUiString("style.name"), getUiString("style.guide"), getUiString("style.number"),
			getUiString("style.category"), getUiString("style.type"),
			getUiString("style.og.min"), getUiString("style.og.max"),
			getUiString("style.ibu.min"), getUiString("style.ibu.max")
		}, 0){ @Override public boolean isCellEditable(int row, int column){ return false; }};
		table = new JTable(model);
		table.setName("style.table");
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
		sorter.setSortKeys(java.util.List.of(new RowSorter.SortKey(2, SortOrder.ASCENDING)));
		table.getSelectionModel().addListSelectionListener(e -> updateSelectionActions());
		filterField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent e){ applyFilter(); }
			@Override public void removeUpdate(DocumentEvent e){ applyFilter(); }
			@Override public void changedUpdate(DocumentEvent e){ applyFilter(); }
		});
		table.addMouseListener(new MouseAdapter()
		{
			@Override public void mouseClicked(MouseEvent e){ if (e.getClickCount() >= 2 && table.getSelectedRow() >= 0 && editAction.isEnabled()) editAction.actionPerformed(null); }
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
			@Override public void actionPerformed(java.awt.event.ActionEvent e){ runnable.run(); }
		};
		a.putValue(Action.SHORT_DESCRIPTION, text);
		a.putValue(Action.ACTION_COMMAND_KEY, actionKey);
		return a;
	}

	private JButton button(Action action){ JButton b = new JButton(action); b.setText((String)action.getValue(Action.NAME)); return b; }

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
		ActionHotkeySupport.setTooltip(saveAction, "Save All (Alt+S, Ctrl/Cmd+S)");
		ActionHotkeySupport.setTooltip(undoAction, "Undo All (Alt+U, Ctrl/Cmd+U, Ctrl/Cmd+Z)");
		ActionHotkeySupport.setTooltip(addAction, "Add New (Alt+N, Ctrl/Cmd+N)");
		ActionHotkeySupport.setTooltip(editAction, "Edit (Alt+E, Ctrl/Cmd+E, Enter, Double-click)");
		ActionHotkeySupport.setTooltip(duplicateAction, "Duplicate (Alt+D, Ctrl/Cmd+D)");
		ActionHotkeySupport.setTooltip(renameAction, "Rename (Alt+R, Ctrl/Cmd+R, F2)");
		ActionHotkeySupport.setTooltip(deleteAction, "Delete (Delete)");
		ActionHotkeySupport.setTooltip(filterAction, "Filter (Alt+F, Ctrl/Cmd+F, Escape hides)");
		ActionHotkeySupport.setTooltip(exportAction, "Export CSV (Alt+X, Ctrl/Cmd+X)");
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_S), "style.hotkey.save", saveAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_U), "style.hotkey.undoU", undoAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_Z), "style.hotkey.undoZ", undoAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_N), "style.hotkey.add", addAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_E), "style.hotkey.editCtrl", editAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_R), "style.hotkey.renameCtrl", renameAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "style.hotkey.renameF2", renameAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_D), "style.hotkey.duplicateCtrl", duplicateAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F), "style.hotkey.filterCtrl", filterAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), "style.hotkey.filterAlt", filterAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "style.hotkey.export", exportAction);
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "style.hotkey.export.window");
		getActionMap().put("style.hotkey.export.window", exportAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "style.hotkey.deleteKey", deleteAction);
		ActionHotkeySupport.bindFocused(table, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "style.hotkey.editEnter", editAction);
		ActionHotkeySupport.bindFocused(filterField, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "style.hotkey.filterEscape", new AbstractAction()
		{
			@Override public void actionPerformed(java.awt.event.ActionEvent e){ hideFilterPanel(); }
		});
	}

	private void updateSelectionActions(){ boolean has = table.getSelectedRow() >= 0; editAction.setEnabled(has); duplicateAction.setEnabled(has); renameAction.setEnabled(has); deleteAction.setEnabled(has); }
	private Style selected(){ int row = table.getSelectedRow(); if (row < 0) return null; String name = (String)model.getValueAt(table.convertRowIndexToModel(row), 0); return dbPort.styles().get(name); }

	private void addItem()
	{
		Style draft = new Style(""); draft.setType(Style.Type.ALE);
		Style created = dialogPort.showEditStyleDialog(parent, draft, true);
		if (created == null) return;
		if (dbPort.styles().containsKey(created.getName())) { dialogPort.showError(parent, getUiString("style.new.dialog.already.exists"), getUiString("ui.error")); return; }
		dbPort.styles().put(created.getName(), created);
		dirtyState.markDirty(created, "reference.database", "styles");
		refresh();
	}

	private void duplicateSelected()
	{
		Style current = selected(); if (current == null) return;
		Style draft = new Style(current); draft.setName("");
		Style created = dialogPort.showEditStyleDialog(parent, draft, true);
		if (created == null) return;
		if (dbPort.styles().containsKey(created.getName())) { dialogPort.showError(parent, getUiString("style.new.dialog.already.exists"), getUiString("ui.error")); return; }
		dbPort.styles().put(created.getName(), created);
		dirtyState.markDirty(created, "reference.database", "styles");
		refresh();
	}

	private void editSelected()
	{
		Style current = selected(); if (current == null) return;
		Style edited = dialogPort.showEditStyleDialog(parent, new Style(current), false); if (edited == null) return;
		current.setDisplayName(edited.getDisplayName());
		current.setStyleGuide(edited.getStyleGuide());
		current.setCategoryNumber(edited.getCategoryNumber());
		current.setCategory(edited.getCategory());
		current.setStyleLetter(edited.getStyleLetter());
		current.setStyleGuideName(edited.getStyleGuideName());
		current.setType(edited.getType());
		current.setOgMin(edited.getOgMin()); current.setOgMax(edited.getOgMax());
		current.setFgMin(edited.getFgMin()); current.setFgMax(edited.getFgMax());
		current.setIbuMin(edited.getIbuMin()); current.setIbuMax(edited.getIbuMax());
		current.setColourMin(edited.getColourMin()); current.setColourMax(edited.getColourMax());
		current.setCarbMin(edited.getCarbMin()); current.setCarbMax(edited.getCarbMax());
		current.setAbvMin(edited.getAbvMin()); current.setAbvMax(edited.getAbvMax());
		current.setNotes(edited.getNotes()); current.setProfile(edited.getProfile()); current.setIngredients(edited.getIngredients()); current.setExamples(edited.getExamples());
		dirtyState.markDirty(current, "reference.database", "styles");
		refresh();
	}

	private void deleteSelected()
	{
		Style current = selected(); if (current == null) return;
		if (!dialogPort.confirm(parent, getUiString("style.delete.msg"), getUiString("common.remove"))) return;
		dbPort.styles().remove(current.getName());
		dirtyState.markDirty("reference.database", "styles");
		refresh();
	}

	private void renameSelected()
	{
		Style current = selected(); if (current == null) return;
		String oldName = current.getName();
		String renamed = dialogPort.promptName(parent, getUiString("style.rename"), getUiString("editor.rename"), oldName);
		if (renamed == null) return;
		String newName = renamed.trim();
		if (newName.isEmpty()){ dialogPort.showError(parent, getUiString("style.new.dialog.not.empty"), getUiString("ui.error")); return; }
		if (oldName.equals(newName)) return;
		if (dbPort.styles().containsKey(newName)){ dialogPort.showError(parent, getUiString("style.new.dialog.already.exists"), getUiString("ui.error")); return; }
		dbPort.styles().remove(oldName);
		current.setName(newName);
		dbPort.styles().put(newName, current);
		renameHook.onStyleRenamed(oldName, newName);
		dirtyState.markDirty(current, "reference.database", "styles");
		refresh();
	}

	private void saveAll()
	{
		if (!dialogPort.confirm(parent, getUiString("editor.apply.all.msg"), getUiString("editor.apply.all"))) return;
		try { dbPort.saveAll(); dirtyState.clear(); refresh(); } catch (Exception e){ dialogPort.showError(parent, e.getMessage(), getUiString("ui.error")); }
	}

	private void undoAll()
	{
		if (!dialogPort.confirm(parent, getUiString("editor.discard.all.msg"), getUiString("editor.discard.all"))) return;
		try { dbPort.loadAll(); dirtyState.clear(); refresh(); } catch (Exception e){ dialogPort.showError(parent, e.getMessage(), getUiString("ui.error")); }
	}

	private void exportCsv()
	{
		File selected = dialogPort.chooseExportFile(parent, new File("styles.csv"));
		if (selected == null) return;
		try { dialogPort.writeCsv(selected, visibleItems()); } catch (Exception e){ dialogPort.showError(parent, e.getMessage(), getUiString("ui.error")); }
	}

	private Collection<Style> visibleItems()
	{
		Collection<Style> items = new ArrayList<>();
		for (int row = 0; row < table.getRowCount(); row++)
		{
			int modelRow = table.convertRowIndexToModel(row);
			String name = (String)model.getValueAt(modelRow, 0);
			Style item = dbPort.styles().get(name);
			if (item != null) items.add(item);
		}
		return items;
	}

	@Override
	public void refresh()
	{
		model.setRowCount(0);
		for (Style s : dbPort.styles().values())
		{
			model.addRow(new Object[] { s.getName(), s.getStyleGuide(), s.getStyleNumber(), s.getCategory(), s.getType(), s.getOgMin(), s.getOgMax(), s.getIbuMin(), s.getIbuMax() });
		}
	}

	private void applyFilter()
	{
		String raw = filterField.getText();
		if (raw == null || raw.trim().isEmpty()) { sorter.setRowFilter(null); return; }
		sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(raw.trim())));
	}

	private boolean isRowDirty(int viewRow)
	{
		if (viewRow < 0 || viewRow >= table.getRowCount()) return false;
		int modelRow = table.convertRowIndexToModel(viewRow);
		String name = (String)model.getValueAt(modelRow, 0);
		Style item = dbPort.styles().get(name);
		return dirtyState.isDirty(item);
	}

	private void showFilterPanel(){ filterPanel.setVisible(true); filterPanel.revalidate(); filterPanel.repaint(); filterField.requestFocusInWindow(); filterField.selectAll(); }
	private void hideFilterPanel(){ filterField.setText(""); filterPanel.setVisible(false); filterPanel.revalidate(); filterPanel.repaint(); table.requestFocusInWindow(); }

	interface DialogPort
	{
		Style showEditStyleDialog(JFrame parent, Style current, boolean createMode);
		String promptName(JFrame parent, String message, String title, String currentName);
		boolean confirm(JFrame parent, String message, String title);
		File chooseExportFile(JFrame parent, File defaultFile);
		void writeCsv(File target, Collection<Style> styles) throws IOException;
		void showError(JFrame parent, String message, String title);
	}

	interface DbPort
	{
		Map<String, Style> styles();
		void saveAll();
		void loadAll();
	}

	interface RenameHook{ void onStyleRenamed(String oldName, String newName); }
	static class NoOpRenameHook implements RenameHook{ @Override public void onStyleRenamed(String oldName, String newName){ } }
	static class DefaultDbPort implements DbPort
	{
		@Override public Map<String, Style> styles(){ return Database.getInstance().getStyles(); }
		@Override public void saveAll(){ Database.getInstance().saveAll(); }
		@Override public void loadAll(){ Database.getInstance().loadAll(); }
	}

	static class SwingDialogPort implements DialogPort
	{
		@Override public Style showEditStyleDialog(JFrame parent, Style current, boolean createMode){ EditStyleDialog d = new EditStyleDialog(parent, current, createMode); d.setVisible(true); return d.getResult(); }
		@Override public String promptName(JFrame parent, String message, String title, String currentName){ return (String)JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE, null, null, currentName); }
		@Override public boolean confirm(JFrame parent, String message, String title){ int r = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION); return r == JOptionPane.YES_OPTION; }
		@Override public File chooseExportFile(JFrame parent, File defaultFile){ JFileChooser c = new JFileChooser(); c.setSelectedFile(defaultFile); if (c.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null; return c.getSelectedFile(); }
		@Override public void writeCsv(File target, Collection<Style> styles) throws IOException
		{
			try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8)))
			{
				w.println("Name,StyleGuide,StyleNumber,Category,Type,OgMin,OgMax,FgMin,FgMax,IbuMin,IbuMax,ColourMin,ColourMax,CarbMin,CarbMax,AbvMin,AbvMax");
				for (Style s : styles)
				{
					w.printf("%s,%s,%s,%s,%s,%.3f,%.3f,%.3f,%.3f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
						s.getName(), s.getStyleGuide(), s.getStyleNumber(), s.getCategory(), s.getType(),
						s.getOgMin() == null ? 0D : s.getOgMin().get(), s.getOgMax() == null ? 0D : s.getOgMax().get(),
						s.getFgMin() == null ? 0D : s.getFgMin().get(), s.getFgMax() == null ? 0D : s.getFgMax().get(),
						s.getIbuMin() == null ? 0D : s.getIbuMin().get(), s.getIbuMax() == null ? 0D : s.getIbuMax().get(),
						s.getColourMin() == null ? 0D : s.getColourMin().get(), s.getColourMax() == null ? 0D : s.getColourMax().get(),
						s.getCarbMin() == null ? 0D : s.getCarbMin().get(), s.getCarbMax() == null ? 0D : s.getCarbMax().get(),
						s.getAbvMin() == null ? 0D : s.getAbvMin().get(), s.getAbvMax() == null ? 0D : s.getAbvMax().get());
				}
			}
		}
		@Override public void showError(JFrame parent, String message, String title){ SwingUiErrors.showError(parent, message, title); }
	}
}
