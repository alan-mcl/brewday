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
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.dialogs.EditMiscDialog;

import static mclachlan.brewday.util.StringUtils.format;
import static mclachlan.brewday.util.StringUtils.getUiString;

public class MiscsScreen extends JPanel implements SwingScreen
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

	public MiscsScreen(JFrame parent, DirtyStateService dirtyState)
	{
		this(parent, dirtyState, new SwingDialogPort(), new DefaultDbPort(), new NoOpRenameHook());
	}

	MiscsScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort)
	{
		this(parent, dirtyState, dialogPort, dbPort, new NoOpRenameHook());
	}

	MiscsScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort, RenameHook renameHook)
	{
		super(new BorderLayout());
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.dialogPort = dialogPort;
		this.dbPort = dbPort;
		this.renameHook = renameHook;

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		saveAction = commandAction("editor.apply.all", "misc.save.action", SwingIcons.IconKey.EDIT, this::saveAll);
		undoAction = commandAction("editor.discard.all", "misc.undo.action", SwingIcons.IconKey.DELETE, this::undoAll);
		bar.add(button(saveAction));
		bar.add(button(undoAction));
		bar.addSeparator();
		addAction = commandAction("common.add.new", "misc.add.action", SwingIcons.IconKey.ADD_MISC, this::addItem);
		bar.add(button(addAction));
		editAction = commandAction("common.edit", "misc.edit.action", SwingIcons.IconKey.EDIT, this::editSelected);
		duplicateAction = commandAction("common.duplicate", "misc.duplicate.action", SwingIcons.IconKey.DUPLICATE, this::duplicateSelected);
		renameAction = commandAction("editor.rename", "misc.rename.action", SwingIcons.IconKey.EDIT, this::renameSelected);
		deleteAction = commandAction("common.remove", "misc.delete.action", SwingIcons.IconKey.DELETE, this::deleteSelected);
		editAction.setEnabled(false);
		duplicateAction.setEnabled(false);
		renameAction.setEnabled(false);
		deleteAction.setEnabled(false);
		bar.add(button(editAction));
		bar.add(button(duplicateAction));
		bar.add(button(renameAction));
		bar.add(button(deleteAction));
		filterAction = commandAction("common.filter", "misc.filter.action", SwingIcons.IconKey.EDIT, this::showFilterPanel);
		bar.add(button(filterAction));
		exportAction = commandAction("common.export.csv", "misc.export.action", SwingIcons.IconKey.EXPORT_CSV, this::exportCsv);
		bar.add(button(exportAction));
		JPanel north = new JPanel(new BorderLayout());
		north.add(bar, BorderLayout.NORTH);

		filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JLabel filterLabel = new JLabel(getUiString("misc.filter.label"));
		filterField = new JTextField(20);
		filterField.setName("misc.filter.field");
		filterField.setToolTipText(getUiString("misc.filter.tooltip"));
		filterLabel.setLabelFor(filterField);
		filterPanel.add(filterLabel);
		filterPanel.add(filterField);
		filterPanel.setVisible(false);
		north.add(filterPanel, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		model = new DefaultTableModel(new String[] {
			getUiString("misc.name"),
			getUiString("misc.type"),
			getUiString("misc.use"),
			getUiString("misc.usage.recommendation"),
			getUiString("misc.measurementType"),
			getUiString("misc.water.addition.formula"),
			getUiString("misc.acid.content")
		}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		table = new JTable(model);
		table.setName("misc.table");
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

		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_N), "misc.hotkey.add", addAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_E), "misc.hotkey.editCtrl", editAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_R), "misc.hotkey.renameCtrl", renameAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "misc.hotkey.renameF2", renameAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_D), "misc.hotkey.duplicateCtrl", duplicateAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F), "misc.hotkey.filterCtrl", filterAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), "misc.hotkey.filterAlt", filterAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "misc.hotkey.export", exportAction);
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "misc.hotkey.export.window");
		getActionMap().put("misc.hotkey.export.window", exportAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "misc.hotkey.deleteKey", deleteAction);
		ActionHotkeySupport.bindFocused(table, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "misc.hotkey.editEnter", editAction);
		ActionHotkeySupport.bindFocused(filterField, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "misc.hotkey.export.filterFocused", exportAction);
		ActionHotkeySupport.bindFocused(filterField, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "misc.hotkey.filterEscape", new AbstractAction()
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

	private Misc selected()
	{
		int row = table.getSelectedRow();
		if (row < 0)
		{
			return null;
		}
		String name = (String)model.getValueAt(table.convertRowIndexToModel(row), 0);
		return dbPort.miscs().get(name);
	}

	private void addItem()
	{
		Misc draft = new Misc("");
		draft.setType(Misc.Type.OTHER);
		Misc created = dialogPort.showEditMiscDialog(parent, draft, true);
		if (created == null)
		{
			return;
		}
		if (dbPort.miscs().containsKey(created.getName()))
		{
			dialogPort.showError(parent, getUiString("misc.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.miscs().put(created.getName(), created);
		dirtyState.markDirty(created, "reference.database", "misc");
		refresh();
	}

	private void duplicateSelected()
	{
		Misc current = selected();
		if (current == null)
		{
			return;
		}
		Misc draft = new Misc(current);
		draft.setName("");
		Misc created = dialogPort.showEditMiscDialog(parent, draft, true);
		if (created == null)
		{
			return;
		}
		if (dbPort.miscs().containsKey(created.getName()))
		{
			dialogPort.showError(parent, getUiString("misc.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.miscs().put(created.getName(), created);
		dirtyState.markDirty(created, "reference.database", "misc");
		refresh();
	}

	private void editSelected()
	{
		Misc current = selected();
		if (current == null)
		{
			return;
		}
		Misc edited = dialogPort.showEditMiscDialog(parent, new Misc(current), false);
		if (edited == null)
		{
			return;
		}
		current.setType(edited.getType());
		current.setUse(edited.getUse());
		current.setMeasurementType(edited.getMeasurementType());
		current.setWaterAdditionFormula(edited.getWaterAdditionFormula());
		current.setAcidContent(edited.getAcidContent());
		current.setUsageRecommendation(edited.getUsageRecommendation());
		current.setDescription(edited.getDescription());
		dirtyState.markDirty(current, "reference.database", "misc");
		refresh();
	}

	private void deleteSelected()
	{
		Misc current = selected();
		if (current == null)
		{
			return;
		}
		if (!dialogPort.confirm(parent, getUiString("misc.delete.msg"), getUiString("common.remove")))
		{
			return;
		}
		dbPort.miscs().remove(current.getName());
		dirtyState.markDirty("reference.database", "misc");
		refresh();
	}

	private void renameSelected()
	{
		Misc current = selected();
		if (current == null)
		{
			return;
		}
		String oldName = current.getName();
		String renamed = dialogPort.promptName(parent, getUiString("misc.rename"), getUiString("editor.rename"), oldName);
		if (renamed == null)
		{
			return;
		}
		String newName = renamed.trim();
		if (newName.isEmpty())
		{
			dialogPort.showError(parent, getUiString("misc.new.dialog.not.empty"), getUiString("ui.error"));
			return;
		}
		if (oldName.equals(newName))
		{
			return;
		}
		if (dbPort.miscs().containsKey(newName))
		{
			dialogPort.showError(parent, getUiString("misc.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}

		dbPort.miscs().remove(oldName);
		current.setName(newName);
		dbPort.miscs().put(newName, current);
		renameHook.onMiscRenamed(oldName, newName);
		dirtyState.markDirty(current, "reference.database", "misc");
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
		File selected = dialogPort.chooseExportFile(parent, new File("miscs.csv"));
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

	private Collection<Misc> visibleItems()
	{
		Collection<Misc> items = new ArrayList<>();
		for (int row = 0; row < table.getRowCount(); row++)
		{
			int modelRow = table.convertRowIndexToModel(row);
			String name = (String)model.getValueAt(modelRow, 0);
			Misc item = dbPort.miscs().get(name);
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
		for (Misc item : dbPort.miscs().values())
		{
			model.addRow(new Object[] {
				item.getName(),
				item.getType(),
				item.getUse(),
				item.getUsageRecommendation(),
				item.getMeasurementType(),
				item.getWaterAdditionFormula(),
				fmtPct(item.getAcidContent())
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
		Misc item = dbPort.miscs().get(name);
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
		Misc showEditMiscDialog(JFrame parent, Misc current, boolean createMode);

		String promptName(JFrame parent, String message, String title, String currentName);

		boolean confirm(JFrame parent, String message, String title);

		File chooseExportFile(JFrame parent, File defaultFile);

		void writeCsv(File target, Collection<Misc> miscs) throws IOException;

		void showError(JFrame parent, String message, String title);
	}

	interface DbPort
	{
		Map<String, Misc> miscs();

		void saveAll();

		void loadAll();
	}

	interface RenameHook
	{
		void onMiscRenamed(String oldName, String newName);
	}

	static class NoOpRenameHook implements RenameHook
	{
		@Override
		public void onMiscRenamed(String oldName, String newName)
		{
			// Hook for future cascading rename across recipes/batches/inventory.
		}
	}

	static class DefaultDbPort implements DbPort
	{
		@Override
		public Map<String, Misc> miscs()
		{
			return Database.getInstance().getMiscs();
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
		public Misc showEditMiscDialog(JFrame parent, Misc current, boolean createMode)
		{
			EditMiscDialog dialog = new EditMiscDialog(parent, current, createMode);
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
		public void writeCsv(File target, Collection<Misc> miscs) throws IOException
		{
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8)))
			{
				writer.println("Name,Type,Use,UsageRecommendation,MeasurementType,WaterAdditionFormula,AcidContent,Description");
				for (Misc misc : miscs)
				{
					writer.printf("%s,%s,%s,%s,%s,%s,%.2f,%s%n",
						misc.getName(),
						misc.getType(),
						misc.getUse(),
						misc.getUsageRecommendation() == null ? "" : misc.getUsageRecommendation(),
						misc.getMeasurementType(),
						misc.getWaterAdditionFormula(),
						misc.getAcidContent() == null ? 0D : misc.getAcidContent().get(),
						misc.getDescription() == null ? "" : misc.getDescription());
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
