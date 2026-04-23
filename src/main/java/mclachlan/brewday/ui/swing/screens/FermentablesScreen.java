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
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.dialogs.EditFermentableDialog;

import static mclachlan.brewday.util.StringUtils.format;
import static mclachlan.brewday.util.StringUtils.getUiString;

public class FermentablesScreen extends JPanel implements SwingScreen
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

	public FermentablesScreen(JFrame parent, DirtyStateService dirtyState)
	{
		this(parent, dirtyState, new SwingDialogPort(), new DefaultDbPort(), new NoOpRenameHook());
	}

	FermentablesScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort)
	{
		this(parent, dirtyState, dialogPort, dbPort, new NoOpRenameHook());
	}

	FermentablesScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort, RenameHook renameHook)
	{
		super(new BorderLayout());
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.dialogPort = dialogPort;
		this.dbPort = dbPort;
		this.renameHook = renameHook;

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		saveAction = commandAction("editor.apply.all", "fermentable.save.action", SwingIcons.IconKey.EDIT, this::saveAll);
		undoAction = commandAction("editor.discard.all", "fermentable.undo.action", SwingIcons.IconKey.DELETE, this::undoAll);
		bar.add(button(saveAction));
		bar.add(button(undoAction));
		bar.addSeparator();
		addAction = commandAction("common.add", "fermentable.add.action", SwingIcons.IconKey.ADD_FERMENTABLE, this::addItem);
		addAction.putValue(Action.NAME, "Add New");
		bar.add(button(addAction));
		editAction = commandAction("common.edit", "fermentable.edit.action", SwingIcons.IconKey.EDIT, this::editSelected);
		renameAction = commandAction("editor.rename", "fermentable.rename.action", SwingIcons.IconKey.EDIT, this::renameSelected);
		deleteAction = commandAction("common.remove", "fermentable.delete.action", SwingIcons.IconKey.DELETE, this::deleteSelected);
		deleteAction.putValue(Action.NAME, "Delete");
		editAction.setEnabled(false);
		renameAction.setEnabled(false);
		deleteAction.setEnabled(false);
		bar.add(button(editAction));
		bar.add(button(renameAction));
		bar.add(button(deleteAction));
		filterAction = commandAction("common.edit", "fermentable.filter.action", SwingIcons.IconKey.EDIT, this::showFilterPanel);
		filterAction.putValue(Action.NAME, "Filter");
		bar.add(button(filterAction));
		exportAction = commandAction("common.export.csv", "fermentable.export.action", SwingIcons.IconKey.EXPORT_CSV, this::exportCsv);
		bar.add(button(exportAction));
		JPanel north = new JPanel(new BorderLayout());
		north.add(bar, BorderLayout.NORTH);

		filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JLabel filterLabel = new JLabel(getUiString("fermentable.filter.label"));
		filterField = new JTextField(20);
		filterField.setName("fermentable.filter.field");
		filterField.setToolTipText(getUiString("fermentable.filter.tooltip"));
		filterLabel.setLabelFor(filterField);
		filterPanel.add(filterLabel);
		filterPanel.add(filterField);
		filterPanel.setVisible(false);
		north.add(filterPanel, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		model = new DefaultTableModel(new String[] {
			getUiString("fermentable.name"),
			getUiString("fermentable.type"),
			getUiString("fermentable.origin"),
			getUiString("fermentable.supplier"),
			getUiString("fermentable.colour"),
			getUiString("fermentable.yield"),
			getUiString("fermentable.distilled.water.ph")
		}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		table = new JTable(model);
		table.setName("fermentable.table");
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

		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_S), "fermentable.hotkey.save", saveAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_U), "fermentable.hotkey.undoU", undoAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_Z), "fermentable.hotkey.undoZ", undoAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_N), "fermentable.hotkey.add", addAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_E), "fermentable.hotkey.editCtrl", editAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_R), "fermentable.hotkey.renameCtrl", renameAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "fermentable.hotkey.renameF2", renameAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_D), "fermentable.hotkey.deleteCtrl", deleteAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F), "fermentable.hotkey.filterCtrl", filterAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), "fermentable.hotkey.filterAlt", filterAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "fermentable.hotkey.export", exportAction);
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "fermentable.hotkey.export.window");
		getActionMap().put("fermentable.hotkey.export.window", exportAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "fermentable.hotkey.deleteKey", deleteAction);
		ActionHotkeySupport.bindFocused(table, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "fermentable.hotkey.editEnter", editAction);
		ActionHotkeySupport.bindFocused(filterField, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "fermentable.hotkey.export.filterFocused", exportAction);
		ActionHotkeySupport.bindFocused(filterField, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fermentable.hotkey.filterEscape", new AbstractAction()
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

	private Fermentable selected()
	{
		int row = table.getSelectedRow();
		if (row < 0)
		{
			return null;
		}
		String name = (String)model.getValueAt(table.convertRowIndexToModel(row), 0);
		return dbPort.fermentables().get(name);
	}

	private void addItem()
	{
		Fermentable draft = new Fermentable("");
		draft.setType(Fermentable.Type.GRAIN);
		Fermentable created = dialogPort.showEditFermentableDialog(parent, draft, true);
		if (created == null)
		{
			return;
		}
		if (dbPort.fermentables().containsKey(created.getName()))
		{
			dialogPort.showError(parent, getUiString("fermentable.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.fermentables().put(created.getName(), created);
		dirtyState.markDirty(created, "reference.database", "fermentables");
		refresh();
	}

	private void editSelected()
	{
		Fermentable current = selected();
		if (current == null)
		{
			return;
		}
		Fermentable edited = dialogPort.showEditFermentableDialog(parent, new Fermentable(current), false);
		if (edited == null)
		{
			return;
		}
		current.setType(edited.getType());
		current.setOrigin(edited.getOrigin());
		current.setSupplier(edited.getSupplier());
		current.setYield(edited.getYield());
		current.setColour(edited.getColour());
		current.setCoarseFineDiff(edited.getCoarseFineDiff());
		current.setMoisture(edited.getMoisture());
		current.setDiastaticPower(edited.getDiastaticPower());
		current.setMaxInBatch(edited.getMaxInBatch());
		current.setDistilledWaterPh(edited.getDistilledWaterPh());
		current.setBufferingCapacity(edited.getBufferingCapacity());
		current.setLacticAcidContent(edited.getLacticAcidContent());
		current.setAddAfterBoil(edited.isAddAfterBoil());
		current.setRecommendMash(edited.isRecommendMash());
		current.setDescription(edited.getDescription());
		dirtyState.markDirty(current, "reference.database", "fermentables");
		refresh();
	}

	private void deleteSelected()
	{
		Fermentable current = selected();
		if (current == null)
		{
			return;
		}
		if (!dialogPort.confirm(parent, getUiString("fermentable.delete.msg"), getUiString("common.remove")))
		{
			return;
		}
		dbPort.fermentables().remove(current.getName());
		dirtyState.markDirty("reference.database", "fermentables");
		refresh();
	}

	private void renameSelected()
	{
		Fermentable current = selected();
		if (current == null)
		{
			return;
		}
		String oldName = current.getName();
		String renamed = dialogPort.promptName(parent, getUiString("fermentable.rename"), getUiString("editor.rename"), oldName);
		if (renamed == null)
		{
			return;
		}
		String newName = renamed.trim();
		if (newName.isEmpty())
		{
			dialogPort.showError(parent, getUiString("fermentable.new.dialog.not.empty"), getUiString("ui.error"));
			return;
		}
		if (oldName.equals(newName))
		{
			return;
		}
		if (dbPort.fermentables().containsKey(newName))
		{
			dialogPort.showError(parent, getUiString("fermentable.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}

		dbPort.fermentables().remove(oldName);
		current.setName(newName);
		dbPort.fermentables().put(newName, current);
		renameHook.onFermentableRenamed(oldName, newName);
		dirtyState.markDirty(current, "reference.database", "fermentables");
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
		File selected = dialogPort.chooseExportFile(parent, new File("fermentables.csv"));
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

	private Collection<Fermentable> visibleItems()
	{
		Collection<Fermentable> items = new ArrayList<>();
		for (int row = 0; row < table.getRowCount(); row++)
		{
			int modelRow = table.convertRowIndexToModel(row);
			String name = (String)model.getValueAt(modelRow, 0);
			Fermentable item = dbPort.fermentables().get(name);
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
		for (Fermentable item : dbPort.fermentables().values())
		{
			model.addRow(new Object[] {
				item.getName(),
				item.getType(),
				item.getOrigin(),
				item.getSupplier(),
				fmtLovibond(item.getColour()),
				fmtPct(item.getYield()),
				fmtPh(item.getDistilledWaterPh())
			});
		}
	}

	private String fmtPct(PercentageUnit value)
	{
		return value == null ? "" : format(value.get(), Quantity.Unit.PERCENTAGE_DISPLAY);
	}

	private String fmtLovibond(ColourUnit value)
	{
		return value == null ? "" : format(value.get(), Quantity.Unit.LOVIBOND);
	}

	private String fmtPh(PhUnit value)
	{
		return value == null ? "" : format(value.get(), Quantity.Unit.PH);
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
		Fermentable item = dbPort.fermentables().get(name);
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
		Fermentable showEditFermentableDialog(JFrame parent, Fermentable current, boolean createMode);

		String promptName(JFrame parent, String message, String title, String currentName);

		boolean confirm(JFrame parent, String message, String title);

		File chooseExportFile(JFrame parent, File defaultFile);

		void writeCsv(File target, Collection<Fermentable> fermentables) throws IOException;

		void showError(JFrame parent, String message, String title);
	}

	interface DbPort
	{
		Map<String, Fermentable> fermentables();

		void saveAll();

		void loadAll();
	}

	interface RenameHook
	{
		void onFermentableRenamed(String oldName, String newName);
	}

	static class NoOpRenameHook implements RenameHook
	{
		@Override
		public void onFermentableRenamed(String oldName, String newName)
		{
			// Hook for future cascading rename across recipes/batches/inventory.
		}
	}

	static class DefaultDbPort implements DbPort
	{
		@Override
		public Map<String, Fermentable> fermentables()
		{
			return Database.getInstance().getFermentables();
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
		public Fermentable showEditFermentableDialog(JFrame parent, Fermentable current, boolean createMode)
		{
			EditFermentableDialog dialog = new EditFermentableDialog(parent, current, createMode);
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
		public void writeCsv(File target, Collection<Fermentable> fermentables) throws IOException
		{
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8)))
			{
				writer.println("Name,Type,Origin,Supplier,Colour,Yield,DistilledWaterPh");
				for (Fermentable fermentable : fermentables)
				{
					writer.printf("%s,%s,%s,%s,%.2f,%.2f,%.2f%n",
						fermentable.getName(),
						fermentable.getType(),
						fermentable.getOrigin() == null ? "" : fermentable.getOrigin(),
						fermentable.getSupplier() == null ? "" : fermentable.getSupplier(),
						fermentable.getColour() == null ? 0D : fermentable.getColour().get(),
						fermentable.getYield() == null ? 0D : fermentable.getYield().get(),
						fermentable.getDistilledWaterPh() == null ? 0D : fermentable.getDistilledWaterPh().get());
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
