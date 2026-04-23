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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.PpmUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.dialogs.EditWaterDialog;

import static mclachlan.brewday.util.StringUtils.getUiString;
import static mclachlan.brewday.util.StringUtils.format;

public class WaterScreen extends JPanel implements SwingScreen
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
	private final Action renameAction;
	private final Action deleteAction;
	private final Action filterAction;
	private final Action exportAction;

	public WaterScreen(JFrame parent, DirtyStateService dirtyState)
	{
		this(parent, dirtyState, new SwingDialogPort(), new DefaultDbPort(), new NoOpRenameHook());
	}

	WaterScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort)
	{
		this(parent, dirtyState, dialogPort, dbPort, new NoOpRenameHook());
	}

	WaterScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort, RenameHook renameHook)
	{
		super(new BorderLayout());
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.dialogPort = dialogPort;
		this.dbPort = dbPort;
		this.renameHook = renameHook;

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		saveAction = commandAction("editor.apply.all", "water.save.action", SwingIcons.IconKey.EDIT, this::saveAll);
		undoAction = commandAction("editor.discard.all", "water.undo.action", SwingIcons.IconKey.DELETE, this::undoAll);
		bar.add(button(saveAction));
		bar.add(button(undoAction));
		bar.addSeparator();
		addAction = commandAction("common.add", "water.add.action", SwingIcons.IconKey.ADD_WATER, this::addWater);
		addAction.putValue(Action.NAME, "Add New");
		bar.add(button(addAction));
		editAction = commandAction("common.edit", "water.edit.action", SwingIcons.IconKey.EDIT, this::editSelected);
		renameAction = commandAction("editor.rename", "water.rename.action", SwingIcons.IconKey.EDIT, this::renameSelected);
		deleteAction = commandAction("common.remove", "water.delete.action", SwingIcons.IconKey.DELETE, this::deleteSelected);
		deleteAction.putValue(Action.NAME, "Delete");
		editAction.setEnabled(false);
		renameAction.setEnabled(false);
		deleteAction.setEnabled(false);
		bar.add(button(editAction));
		bar.add(button(renameAction));
		bar.add(button(deleteAction));
		filterAction = commandAction("common.edit", "water.filter.action", SwingIcons.IconKey.EDIT, this::showFilterPanel);
		filterAction.putValue(Action.NAME, "Filter");
		bar.add(button(filterAction));
		exportAction = commandAction("common.export.csv", "water.export.action", SwingIcons.IconKey.EXPORT_CSV, this::exportCsv);
		bar.add(button(exportAction));
		JPanel north = new JPanel(new BorderLayout());
		north.add(bar, BorderLayout.NORTH);

		filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JLabel filterLabel = new JLabel(getUiString("water.filter.label"));
		filterField = new JTextField(20);
		filterField.setName("water.filter.field");
		filterField.setToolTipText(getUiString("water.filter.tooltip"));
		filterLabel.setLabelFor(filterField);
		filterPanel.add(filterLabel);
		filterPanel.add(filterField);
		filterPanel.setVisible(false);
		north.add(filterPanel, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		model = new DefaultTableModel(new String[] {
			getUiString("water.name"),
			getUiString("water.calcium.abbr"),
			getUiString("water.bicarbonate.abbr"),
			getUiString("water.sulfate.abbr"),
			getUiString("water.chloride.abbr"),
			getUiString("water.ph"),
			getUiString("water.alkalinity"),
			getUiString("water.ra")
		}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		table = new JTable(model);
		table.setName("water.table");
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
		table.setTableHeader(new JTableHeader(table.getColumnModel())
		{
			@Override
			public String getToolTipText(MouseEvent e)
			{
				int viewColumn = columnAtPoint(e.getPoint());
				if (viewColumn < 0)
				{
					return null;
				}
				int modelColumn = table.convertColumnIndexToModel(viewColumn);
				return headerTooltip(modelColumn);
			}
		});
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
		ActionHotkeySupport.setMnemonic(renameAction, KeyEvent.VK_R);
		ActionHotkeySupport.setMnemonic(deleteAction, KeyEvent.VK_D);
		ActionHotkeySupport.setMnemonic(filterAction, KeyEvent.VK_F);
		ActionHotkeySupport.setMnemonic(exportAction, KeyEvent.VK_X);

		ActionHotkeySupport.setTooltip(saveAction, "Save All (Alt+S, Ctrl/Cmd+S)");
		ActionHotkeySupport.setTooltip(undoAction, "Undo All (Alt+U, Ctrl/Cmd+U, Ctrl/Cmd+Z)");
		ActionHotkeySupport.setTooltip(addAction, "Add New (Alt+N, Ctrl/Cmd+N)");
		ActionHotkeySupport.setTooltip(editAction, "Edit (Alt+E, Ctrl/Cmd+E, Enter, Double-click)");
		ActionHotkeySupport.setTooltip(renameAction, "Rename (Alt+R, Ctrl/Cmd+R, F2)");
		ActionHotkeySupport.setTooltip(deleteAction, "Delete (Alt+D, Ctrl/Cmd+D, Delete)");
		ActionHotkeySupport.setTooltip(filterAction, "Filter (Alt+F, Ctrl/Cmd+F, Escape hides)");
		ActionHotkeySupport.setTooltip(exportAction, "Export CSV (Alt+X, Ctrl/Cmd+X)");

		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_S), "water.hotkey.save", saveAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_U), "water.hotkey.undoU", undoAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_Z), "water.hotkey.undoZ", undoAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_N), "water.hotkey.add", addAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_E), "water.hotkey.editCtrl", editAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_R), "water.hotkey.renameCtrl", renameAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "water.hotkey.renameF2", renameAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_D), "water.hotkey.deleteCtrl", deleteAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F), "water.hotkey.filterCtrl", filterAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), "water.hotkey.filterAlt", filterAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "water.hotkey.export", exportAction);
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "water.hotkey.export.window");
		getActionMap().put("water.hotkey.export.window", exportAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "water.hotkey.deleteKey", deleteAction);
		ActionHotkeySupport.bindFocused(table, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "water.hotkey.editEnter", editAction);
		ActionHotkeySupport.bindFocused(filterField, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "water.hotkey.export.filterFocused", exportAction);
		ActionHotkeySupport.bindFocused(filterField, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "water.hotkey.filterEscape", new AbstractAction()
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
		renameAction.setEnabled(hasSelection);
		deleteAction.setEnabled(hasSelection);
	}

	private Water selected()
	{
		int row = table.getSelectedRow();
		if (row < 0)
		{
			return null;
		}
		String name = (String)model.getValueAt(table.convertRowIndexToModel(row), 0);
		return dbPort.waters().get(name);
	}

	private void addWater()
	{
		Water created = dialogPort.showEditWaterDialog(parent, new Water(""), true);
		if (created == null)
		{
			return;
		}
		if (dbPort.waters().containsKey(created.getName()))
		{
			dialogPort.showError(parent, getUiString("water.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.waters().put(created.getName(), created);
		dirtyState.markDirty(created, "reference.database", "water");
		refresh();
	}

	private void editSelected()
	{
		Water current = selected();
		if (current == null)
		{
			return;
		}
		Water edited = dialogPort.showEditWaterDialog(parent, new Water(current), false);
		if (edited == null)
		{
			return;
		}
		current.setCalcium(edited.getCalcium());
		current.setBicarbonate(edited.getBicarbonate());
		current.setSulfate(edited.getSulfate());
		current.setChloride(edited.getChloride());
		current.setSodium(edited.getSodium());
		current.setMagnesium(edited.getMagnesium());
		current.setPh(edited.getPh());
		current.setDescription(edited.getDescription());
		dirtyState.markDirty(current, "reference.database", "water");
		refresh();
	}

	private void deleteSelected()
	{
		Water current = selected();
		if (current == null)
		{
			return;
		}
		if (!dialogPort.confirm(parent, getUiString("editor.delete.msg"), getUiString("common.remove")))
		{
			return;
		}
		dbPort.waters().remove(current.getName());
		dirtyState.markDirty("reference.database", "water");
		refresh();
	}

	private void renameSelected()
	{
		Water current = selected();
		if (current == null)
		{
			return;
		}
		String oldName = current.getName();
		String renamed = dialogPort.promptName(parent, getUiString("editor.rename.msg"), getUiString("editor.rename"), oldName);
		if (renamed == null)
		{
			return;
		}
		String newName = renamed.trim();
		if (newName.isEmpty())
		{
			dialogPort.showError(parent, getUiString("water.new.dialog.not.empty"), getUiString("ui.error"));
			return;
		}
		if (oldName.equals(newName))
		{
			return;
		}
		if (dbPort.waters().containsKey(newName))
		{
			dialogPort.showError(parent, getUiString("water.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}

		dbPort.waters().remove(oldName);
		current.setName(newName);
		dbPort.waters().put(newName, current);
		renameHook.onWaterRenamed(oldName, newName);
		dirtyState.markDirty(current, "reference.database", "water");
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
		File selected = dialogPort.chooseExportFile(parent, new File("waters.csv"));
		if (selected == null)
		{
			return;
		}
		try
		{
			dialogPort.writeCsv(selected, visibleWaters());
		}
		catch (Exception e)
		{
			dialogPort.showError(parent, e.getMessage(), getUiString("ui.error"));
		}
	}

	private Collection<Water> visibleWaters()
	{
		Collection<Water> waters = new ArrayList<>();
		for (int row = 0; row < table.getRowCount(); row++)
		{
			int modelRow = table.convertRowIndexToModel(row);
			String name = (String)model.getValueAt(modelRow, 0);
			Water water = dbPort.waters().get(name);
			if (water != null)
			{
				waters.add(water);
			}
		}
		return waters;
	}

	@Override
	public void refresh()
	{
		model.setRowCount(0);
		for (Water water : dbPort.waters().values())
		{
			model.addRow(new Object[] {
				water.getName(),
				fmtPpm(water.getCalcium()),
				fmtPpm(water.getBicarbonate()),
				fmtPpm(water.getSulfate()),
				fmtPpm(water.getChloride()),
				fmtPh(water.getPh()),
				fmtPpm(water.getAlkalinity()),
				fmtPpm(water.getResidualAlkalinity())
			});
		}
	}

	private String fmtPpm(PpmUnit value)
	{
		return value == null ? "" : format(value.get(), Quantity.Unit.PPM);
	}

	private String fmtPh(PhUnit value)
	{
		return value == null ? "" : format(value.get(), Quantity.Unit.PH);
	}

	private String headerTooltip(int modelColumn)
	{
		return switch (modelColumn)
		{
			case 0 -> getUiString("water.tooltip.name");
			case 1 -> getUiString("water.tooltip.calcium");
			case 2 -> getUiString("water.tooltip.bicarbonate");
			case 3 -> getUiString("water.tooltip.sulfate");
			case 4 -> getUiString("water.tooltip.chloride");
			case 5 -> getUiString("water.tooltip.ph");
			case 6 -> getUiString("water.tooltip.alkalinity");
			case 7 -> getUiString("water.tooltip.ra");
			default -> null;
		};
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
		Water water = dbPort.waters().get(name);
		return dirtyState.isDirty(water);
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

	DefaultTableModel getModel()
	{
		return model;
	}

	Action getEditAction()
	{
		return editAction;
	}

	Action getRenameAction()
	{
		return renameAction;
	}

	Action getSaveAction()
	{
		return saveAction;
	}

	Action getUndoAction()
	{
		return undoAction;
	}

	Action getAddAction()
	{
		return addAction;
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

	int rowFontStyle(int viewRow)
	{
		Component comp = table.prepareRenderer(table.getCellRenderer(viewRow, 0), viewRow, 0);
		return comp.getFont().getStyle();
	}

	interface DialogPort
	{
		Water showEditWaterDialog(JFrame parent, Water current, boolean createMode);

		String promptName(JFrame parent, String message, String title, String currentName);

		boolean confirm(JFrame parent, String message, String title);

		File chooseExportFile(JFrame parent, File defaultFile);

		void writeCsv(File target, Collection<Water> waters) throws IOException;

		void showError(JFrame parent, String message, String title);
	}

	interface DbPort
	{
		Map<String, Water> waters();

		void saveAll();

		void loadAll();
	}

	interface RenameHook
	{
		void onWaterRenamed(String oldName, String newName);
	}

	static class NoOpRenameHook implements RenameHook
	{
		@Override
		public void onWaterRenamed(String oldName, String newName)
		{
			// Hook for future cascading rename across recipes/batches/inventory.
		}
	}

	static class DefaultDbPort implements DbPort
	{
		@Override
		public Map<String, Water> waters()
		{
			return Database.getInstance().getWaters();
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
		public Water showEditWaterDialog(JFrame parent, Water current, boolean createMode)
		{
			EditWaterDialog dialog = new EditWaterDialog(parent, current, createMode);
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
		public void writeCsv(File target, Collection<Water> waters) throws IOException
		{
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8)))
			{
				writer.println("Name,Calcium,Bicarbonate,Sulfate,Chloride,pH,Alkalinity,ResidualAlkalinity");
				for (Water water : waters)
				{
					writer.printf("%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
						water.getName(),
						water.getCalcium() == null ? 0D : water.getCalcium().get(),
						water.getBicarbonate() == null ? 0D : water.getBicarbonate().get(),
						water.getSulfate() == null ? 0D : water.getSulfate().get(),
						water.getChloride() == null ? 0D : water.getChloride().get(),
						water.getPh() == null ? 0D : water.getPh().get(),
						water.getAlkalinity() == null ? 0D : water.getAlkalinity().get(),
						water.getResidualAlkalinity() == null ? 0D : water.getResidualAlkalinity().get());
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
