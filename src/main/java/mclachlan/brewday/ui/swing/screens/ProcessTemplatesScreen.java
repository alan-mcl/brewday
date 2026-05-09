package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.ProcessTemplateEditorNavPort;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code ProcessTemplatePane}: list and edit process templates.
 */
public class ProcessTemplatesScreen extends JPanel implements SwingScreen
{
	private final JFrame parent;
	private final DirtyStateService dirtyState;
	private final DialogPort dialogPort;
	private final DbPort dbPort;
	private final ProcessTemplateEditorNavPort editorNav;
	private final DefaultTableModel model;
	private final JTable table;
	private final Action saveAction;
	private final Action undoAction;
	private final Action addAction;
	private final Action editAction;
	private final Action duplicateAction;
	private final Action renameAction;
	private final Action deleteAction;

	public ProcessTemplatesScreen(JFrame parent, DirtyStateService dirtyState, ProcessTemplateEditorNavPort editorNav)
	{
		this(parent, dirtyState, new SwingDialogPort(), new DefaultDbPort(), editorNav);
	}

	ProcessTemplatesScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort,
		ProcessTemplateEditorNavPort editorNav)
	{
		super(new BorderLayout());
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.dialogPort = dialogPort;
		this.dbPort = dbPort;
		this.editorNav = editorNav != null ? editorNav : n -> {};

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		saveAction = commandAction("editor.apply.all", "process.template.save.action", SwingIcons.IconKey.EDIT, this::saveAll);
		undoAction = commandAction("editor.discard.all", "process.template.undo.action", SwingIcons.IconKey.DELETE, this::undoAll);
		addAction = commandAction("common.add", "process.template.add.action", SwingIcons.IconKey.PROCESS_TEMPLATE, this::addItem);
		editAction = commandAction("common.edit", "process.template.edit.action", SwingIcons.IconKey.EDIT, this::editSelected);
		duplicateAction = commandAction("common.duplicate", "process.template.duplicate.action", SwingIcons.IconKey.DUPLICATE,
			this::duplicateSelected);
		renameAction = commandAction("editor.rename", "process.template.rename.action", SwingIcons.IconKey.EDIT, this::renameSelected);
		deleteAction = commandAction("common.remove", "process.template.delete.action", SwingIcons.IconKey.DELETE, this::deleteSelected);
		addAction.putValue(Action.NAME, "Add New");
		duplicateAction.putValue(Action.NAME, "Duplicate");
		deleteAction.putValue(Action.NAME, "Delete");
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
		add(bar, BorderLayout.NORTH);

		model = new DefaultTableModel(new String[] {
			getUiString("process.template.name"),
			getUiString("process.template.steps")
		}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		table = new JTable(model);
		table.setName("process.template.table");
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column)
			{
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				Font base = table.getFont();
				c.setFont(base.deriveFont(isRowDirty(row) ? Font.BOLD : Font.PLAIN));
				return c;
			}
		});
		table.setAutoCreateRowSorter(true);
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)table.getRowSorter();
		sorter.setSortKeys(java.util.List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
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

		ActionHotkeySupport.setMnemonic(saveAction, KeyEvent.VK_S);
		ActionHotkeySupport.setMnemonic(undoAction, KeyEvent.VK_U);
		ActionHotkeySupport.setMnemonic(addAction, KeyEvent.VK_N);
		ActionHotkeySupport.setMnemonic(editAction, KeyEvent.VK_E);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_S), "processTemplate.hotkey.save", saveAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_Z), "processTemplate.hotkey.undo", undoAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_N), "processTemplate.hotkey.add", addAction);

		refresh();
	}

	private void updateSelectionActions()
	{
		Recipe r = selected();
		boolean has = r != null;
		editAction.setEnabled(has);
		duplicateAction.setEnabled(has);
		renameAction.setEnabled(has);
		deleteAction.setEnabled(has);
	}

	private Recipe selected()
	{
		int view = table.getSelectedRow();
		if (view < 0)
		{
			return null;
		}
		int mr = table.convertRowIndexToModel(view);
		String name = (String)model.getValueAt(mr, 0);
		return dbPort.processTemplates().get(name);
	}

	private boolean isRowDirty(int viewRow)
	{
		int mr = table.convertRowIndexToModel(viewRow);
		String name = (String)model.getValueAt(mr, 0);
		Recipe r = dbPort.processTemplates().get(name);
		return r != null && dirtyState.isDirty(r);
	}

	private void addItem()
	{
		String name = dialogPort.promptName(parent, getUiString("common.add"), getUiString("recipe.name"), "");
		if (name == null)
		{
			return;
		}
		name = name.trim();
		if (name.isEmpty())
		{
			dialogPort.showError(parent, getUiString("recipe.new.dialog.not.empty"), getUiString("ui.error"));
			return;
		}
		if (dbPort.processTemplates().containsKey(name))
		{
			dialogPort.showError(parent, getUiString("recipe.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		Recipe created = new Recipe(name);
		dbPort.processTemplates().put(name, created);
		dirtyState.markDirty(created, "processTemplates", "brewing");
		refresh();
	}

	private void editSelected()
	{
		Recipe r = selected();
		if (r != null)
		{
			editorNav.openProcessTemplateEditor(r.getName());
			refresh();
		}
	}

	private void duplicateSelected()
	{
		Recipe current = selected();
		if (current == null)
		{
			return;
		}
		String renamed = dialogPort.promptName(parent, getUiString("recipe.copy"), getUiString("recipe.duplicate"), "");
		if (renamed == null)
		{
			return;
		}
		String newName = renamed.trim();
		if (newName.isEmpty())
		{
			dialogPort.showError(parent, getUiString("recipe.new.dialog.not.empty"), getUiString("ui.error"));
			return;
		}
		if (dbPort.processTemplates().containsKey(newName))
		{
			dialogPort.showError(parent, getUiString("recipe.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		Recipe copy = new Recipe(current);
		copy.setName(newName);
		dbPort.processTemplates().put(newName, copy);
		dirtyState.markDirty(copy, "processTemplates", "brewing");
		refresh();
	}

	private void renameSelected()
	{
		Recipe current = selected();
		if (current == null)
		{
			return;
		}
		String oldName = current.getName();
		String renamed = dialogPort.promptName(parent, getUiString("recipe.rename"), getUiString("editor.rename"), oldName);
		if (renamed == null)
		{
			return;
		}
		String newName = renamed.trim();
		if (newName.isEmpty())
		{
			dialogPort.showError(parent, getUiString("recipe.new.dialog.not.empty"), getUiString("ui.error"));
			return;
		}
		if (newName.equals(oldName))
		{
			return;
		}
		if (dbPort.processTemplates().containsKey(newName))
		{
			dialogPort.showError(parent, getUiString("recipe.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.processTemplates().remove(oldName);
		current.setName(newName);
		dbPort.processTemplates().put(newName, current);
		dirtyState.markDirty(current, "processTemplates", "brewing");
		refresh();
	}

	private void deleteSelected()
	{
		Recipe current = selected();
		if (current == null)
		{
			return;
		}
		if (!dialogPort.confirm(parent, getUiString("recipe.delete.msg"), getUiString("recipe.delete.confirm.title")))
		{
			return;
		}
		String name = current.getName();
		dbPort.processTemplates().remove(name);
		dirtyState.removeDirty(current);
		dirtyState.markDirty("processTemplates", "brewing");
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

	@Override
	public void refresh()
	{
		model.setRowCount(0);
		ArrayList<String> names = new ArrayList<>(dbPort.processTemplates().keySet());
		names.sort(String::compareTo);
		for (String n : names)
		{
			Recipe r = dbPort.processTemplates().get(n);
			model.addRow(new Object[] { n, r != null ? r.getSteps().size() : 0 });
		}
	}

	@Override
	public void onActivate()
	{
	}

	/*-------------------------------------------------------------------------*/
	JTable getTable()
	{
		return table;
	}

	interface DialogPort
	{
		String promptName(JFrame parent, String message, String title, String currentName);

		boolean confirm(JFrame parent, String message, String title);

		void showError(JFrame parent, String message, String title);
	}

	static class SwingDialogPort implements DialogPort
	{
		@Override
		public String promptName(JFrame parent, String message, String title, String currentName)
		{
			return (String)JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE, null, null,
				currentName);
		}

		@Override
		public boolean confirm(JFrame parent, String message, String title)
		{
			int r = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION);
			return r == JOptionPane.YES_OPTION;
		}

		@Override
		public void showError(JFrame parent, String message, String title)
		{
			SwingUiErrors.showError(parent, message, title);
		}
	}

	public interface DbPort
	{
		Map<String, Recipe> processTemplates();

		void saveAll();

		void loadAll();
	}

	public static class DefaultDbPort implements DbPort
	{
		@Override
		public Map<String, Recipe> processTemplates()
		{
			return Database.getInstance().getProcessTemplates();
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
}
