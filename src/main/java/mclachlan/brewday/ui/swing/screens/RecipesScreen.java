package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import mclachlan.brewday.Brewday;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.BitternessUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.EntityListToolbarTooltips;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.RecipeEditorNavPort;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.util.Log;
import mclachlan.brewday.ui.swing.dialogs.NewRecipeDialog;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class RecipesScreen extends JPanel implements SwingScreen
{
	private final JFrame parent;
	private final DirtyStateService dirtyState;
	private final DialogPort dialogPort;
	private final DbPort dbPort;
	private final RenameHook renameHook;
	private final DeleteHook deleteHook;
	private final RecipeEditorNavPort recipeEditorNav;
	private final Runnable navTagsRefresh;
	private final DefaultTableModel model;
	private final JTable table;
	private final JTextField filterField;
	private final JComboBox<String> tagCombo;
	private final JPanel filterPanel;
	private final TableRowSorter<DefaultTableModel> sorter;
	private final Action saveAction, undoAction, addAction, editAction, duplicateAction, renameAction, deleteAction, filterAction, exportAction;
	private String activeTagFilter;
	private boolean suppressTagCombo;

	public RecipesScreen(JFrame parent, DirtyStateService dirtyState, Runnable navTagsRefresh)
	{
		this(parent, dirtyState, navTagsRefresh, new SwingDialogPort(), new DefaultDbPort(), new NoOpRenameHook(), new NoOpDeleteHook(), null);
	}

	public RecipesScreen(JFrame parent, DirtyStateService dirtyState, Runnable navTagsRefresh,
		RenameHook renameHook, DeleteHook deleteHook)
	{
		this(parent, dirtyState, navTagsRefresh, new SwingDialogPort(), new DefaultDbPort(), renameHook, deleteHook, null);
	}

	public RecipesScreen(JFrame parent, DirtyStateService dirtyState, Runnable navTagsRefresh,
		RenameHook renameHook, DeleteHook deleteHook, RecipeEditorNavPort recipeEditorNav)
	{
		this(parent, dirtyState, navTagsRefresh, new SwingDialogPort(), new DefaultDbPort(), renameHook, deleteHook, recipeEditorNav);
	}

	RecipesScreen(JFrame parent, DirtyStateService dirtyState, Runnable navTagsRefresh, DialogPort dialogPort, DbPort dbPort)
	{
		this(parent, dirtyState, navTagsRefresh, dialogPort, dbPort, new NoOpRenameHook(), new NoOpDeleteHook(), null);
	}

	RecipesScreen(JFrame parent, DirtyStateService dirtyState, Runnable navTagsRefresh, DialogPort dialogPort, DbPort dbPort,
		RenameHook renameHook, DeleteHook deleteHook)
	{
		this(parent, dirtyState, navTagsRefresh, dialogPort, dbPort, renameHook, deleteHook, null);
	}

	RecipesScreen(JFrame parent, DirtyStateService dirtyState, Runnable navTagsRefresh, DialogPort dialogPort, DbPort dbPort,
		RenameHook renameHook, DeleteHook deleteHook, RecipeEditorNavPort recipeEditorNav)
	{
		super(new BorderLayout());
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.navTagsRefresh = navTagsRefresh == null ? () -> {} : navTagsRefresh;
		this.dialogPort = dialogPort;
		this.dbPort = dbPort;
		this.renameHook = renameHook;
		this.deleteHook = deleteHook;
		this.recipeEditorNav = recipeEditorNav != null ? recipeEditorNav : new DefaultRecipeEditorNavPort(parent, dialogPort);

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		saveAction = commandAction("editor.apply.all", "recipe.save.action", SwingIcons.IconKey.EDIT, this::saveAll);
		undoAction = commandAction("editor.discard.all", "recipe.undo.action", SwingIcons.IconKey.DELETE, this::undoAll);
		addAction = commandAction("common.add.new", "recipe.add.action", SwingIcons.IconKey.RECIPE, this::addItem);
		editAction = commandAction("common.edit", "recipe.edit.action", SwingIcons.IconKey.EDIT, this::editSelected);
		duplicateAction = commandAction("common.duplicate", "recipe.duplicate.action", SwingIcons.IconKey.DUPLICATE, this::duplicateSelected);
		renameAction = commandAction("editor.rename", "recipe.rename.action", SwingIcons.IconKey.EDIT, this::renameSelected);
		deleteAction = commandAction("common.remove", "recipe.delete.action", SwingIcons.IconKey.DELETE, this::deleteSelected);
		filterAction = commandAction("common.filter", "recipe.filter.action", SwingIcons.IconKey.EDIT, this::showFilterPanel);
		exportAction = commandAction("common.export.csv", "recipe.export.action", SwingIcons.IconKey.EXPORT_CSV, this::exportCsv);
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
		JLabel filterLabel = new JLabel(getUiString("recipe.filter.label"));
		filterField = new JTextField(16);
		filterField.setName("recipe.filter.field");
		filterField.setToolTipText(getUiString("recipe.filter.tooltip"));
		filterLabel.setLabelFor(filterField);
		JLabel tagLabel = new JLabel(getUiString("recipe.tag.filter.label"));
		tagCombo = new JComboBox<>();
		tagCombo.setName("recipe.tag.combo");
		tagCombo.setToolTipText(getUiString("recipe.tag.filter.tooltip"));
		tagCombo.addItem(getUiString("recipe.tag.all"));
		tagLabel.setLabelFor(tagCombo);
		tagCombo.addItemListener(e ->
		{
			if (e.getStateChange() != ItemEvent.SELECTED || suppressTagCombo)
			{
				return;
			}
			Object sel = tagCombo.getSelectedItem();
			String allLabel = getUiString("recipe.tag.all");
			activeTagFilter = allLabel.equals(sel) ? null : (String)sel;
			applyFilter();
		});
		filterPanel.add(filterLabel);
		filterPanel.add(filterField);
		filterPanel.add(tagLabel);
		filterPanel.add(tagCombo);
		filterPanel.setVisible(false);
		north.add(filterPanel, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		model = new DefaultTableModel(new String[] {
			getUiString("recipe.name"),
			getUiString("recipe.equipment.profile"),
			getUiString("recipe.tags")
		}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		table = new JTable(model);
		table.setName("recipe.table");
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
		sorter.setSortKeys(java.util.List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
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

	/**
	 * Tag filter from navigation tree; does not change tree selection.
	 *
	 * @param tag {@code null} means show all recipes (no tag filter).
	 */
	public void setTag(String tag)
	{
		activeTagFilter = tag;
		suppressTagCombo = true;
		try
		{
			rebuildTagComboOptions();
			String allLabel = getUiString("recipe.tag.all");
			if (tag == null)
			{
				tagCombo.setSelectedItem(allLabel);
			}
			else
			{
				if (!comboHasItem(tagCombo, tag))
				{
					tagCombo.addItem(tag);
				}
				tagCombo.setSelectedItem(tag);
			}
		}
		finally
		{
			suppressTagCombo = false;
		}
		applyFilter();
	}

	private static boolean comboHasItem(JComboBox<String> combo, String value)
	{
		for (int i = 0; i < combo.getItemCount(); i++)
		{
			Object o = combo.getItemAt(i);
			if (value.equals(o))
			{
				return true;
			}
		}
		return false;
	}

	public String getActiveTagFilter()
	{
		return activeTagFilter;
	}

	public void onTagsMayHaveChanged()
	{
		navTagsRefresh.run();
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
		EntityListToolbarTooltips.wireFullToolbar(
			saveAction, undoAction, addAction, editAction,
			duplicateAction, renameAction, deleteAction, filterAction, exportAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_N), "recipe.hotkey.add", addAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_E), "recipe.hotkey.editCtrl", editAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_R), "recipe.hotkey.renameCtrl", renameAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "recipe.hotkey.renameF2", renameAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_D), "recipe.hotkey.duplicateCtrl", duplicateAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F), "recipe.hotkey.filterCtrl", filterAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), "recipe.hotkey.filterAlt", filterAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "recipe.hotkey.export", exportAction);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "recipe.hotkey.export.window");
		getActionMap().put("recipe.hotkey.export.window", exportAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "recipe.hotkey.deleteKey", deleteAction);
		ActionHotkeySupport.bindFocused(table, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "recipe.hotkey.editEnter", editAction);
		ActionHotkeySupport.bindFocused(filterField, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "recipe.hotkey.export.filterFocused", exportAction);
		ActionHotkeySupport.bindFocused(filterField, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "recipe.hotkey.filterEscape", new AbstractAction()
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

	private Recipe selected()
	{
		int row = table.getSelectedRow();
		if (row < 0)
		{
			return null;
		}
		String name = (String)model.getValueAt(table.convertRowIndexToModel(row), 0);
		return dbPort.recipes().get(name);
	}

	private void addItem()
	{
		Recipe created = dialogPort.showNewRecipeDialog(parent);
		if (created == null)
		{
			return;
		}
		if (dbPort.recipes().containsKey(created.getName()))
		{
			dialogPort.showError(parent, getUiString("recipe.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.recipes().put(created.getName(), created);
		dirtyState.markDirty(created, "recipes");
		refresh();
		onTagsMayHaveChanged();
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
		if (dbPort.recipes().containsKey(newName))
		{
			dialogPort.showError(parent, getUiString("recipe.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		Recipe copy = new Recipe(current);
		copy.setName(newName);
		dbPort.recipes().put(newName, copy);
		dirtyState.markDirty(copy, "recipes");
		refresh();
		onTagsMayHaveChanged();
	}

	private void editSelected()
	{
		Recipe current = selected();
		if (current == null)
		{
			return;
		}
		recipeEditorNav.openRecipeEditor(current.getName());
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
		dbPort.recipes().remove(name);
		deleteHook.onRecipeDeleted(name);
		dirtyState.markDirty("recipes");
		refresh();
		onTagsMayHaveChanged();
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
		if (oldName.equals(newName))
		{
			return;
		}
		if (dbPort.recipes().containsKey(newName))
		{
			dialogPort.showError(parent, getUiString("recipe.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.recipes().remove(oldName);
		current.setName(newName);
		dbPort.recipes().put(newName, current);
		renameHook.onRecipeRenamed(oldName, newName);
		dirtyState.markDirty(current, "recipes");
		refresh();
		onTagsMayHaveChanged();
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
			onTagsMayHaveChanged();
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
			onTagsMayHaveChanged();
		}
		catch (Exception e)
		{
			dialogPort.showError(parent, e, getUiString("ui.error"));
		}
	}

	private void exportCsv()
	{
		File selected = dialogPort.chooseExportFile(parent, new File("recipes.csv"));
		if (selected == null)
		{
			return;
		}
		try
		{
			dialogPort.writeRecipeCsv(selected, visibleRecipes());
		}
		catch (Exception e)
		{
			dialogPort.showError(parent, e, getUiString("ui.error"));
		}
	}

	private Collection<Recipe> visibleRecipes()
	{
		Collection<Recipe> items = new ArrayList<>();
		for (int row = 0; row < table.getRowCount(); row++)
		{
			int modelRow = table.convertRowIndexToModel(row);
			String name = (String)model.getValueAt(modelRow, 0);
			Recipe item = dbPort.recipes().get(name);
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
		for (Recipe r : dbPort.recipes().values())
		{
			model.addRow(new Object[] {
				r.getName(),
				r.getEquipmentProfile() == null ? "" : r.getEquipmentProfile(),
				formatTags(r)
			});
		}
		rebuildTagComboOptions();
		suppressTagCombo = true;
		try
		{
			String allLabel = getUiString("recipe.tag.all");
			if (activeTagFilter == null)
			{
				tagCombo.setSelectedItem(allLabel);
			}
			else
			{
				tagCombo.setSelectedItem(activeTagFilter);
				if (activeTagFilter != null && !activeTagFilter.equals(tagCombo.getSelectedItem()))
				{
					tagCombo.setSelectedItem(allLabel);
					activeTagFilter = null;
				}
			}
		}
		finally
		{
			suppressTagCombo = false;
		}
		applyFilter();
	}

	private static String formatTags(Recipe r)
	{
		List<String> tags = r.getTags();
		if (tags == null || tags.isEmpty())
		{
			return "";
		}
		return String.join(", ", tags);
	}

	private void rebuildTagComboOptions()
	{
		suppressTagCombo = true;
		try
		{
			tagCombo.removeAllItems();
			tagCombo.addItem(getUiString("recipe.tag.all"));
			TreeSet<String> tags = new TreeSet<>();
			for (Recipe r : dbPort.recipes().values())
			{
				if (r.getTags() != null)
				{
					tags.addAll(r.getTags());
				}
			}
			for (String t : tags)
			{
				tagCombo.addItem(t);
			}
		}
		finally
		{
			suppressTagCombo = false;
		}
	}

	private void applyFilter()
	{
		final String raw = filterField.getText();
		final String trimmed = raw == null ? "" : raw.trim();
		final String tag = activeTagFilter;

		sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>()
		{
			@Override
			public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry)
			{
				int modelRow = entry.getIdentifier();
				String name = (String)model.getValueAt(modelRow, 0);
				Recipe r = dbPort.recipes().get(name);
				if (tag != null && (r == null || !r.getTags().contains(tag)))
				{
					return false;
				}
				if (trimmed.isEmpty())
				{
					return true;
				}
				StringBuilder rowText = new StringBuilder();
				for (int c = 0; c < model.getColumnCount(); c++)
				{
					Object v = model.getValueAt(modelRow, c);
					if (v != null)
					{
						rowText.append(v).append('\t');
					}
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
		String name = (String)model.getValueAt(modelRow, 0);
		Recipe item = dbPort.recipes().get(name);
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

	JComboBox<String> getTagCombo()
	{
		return tagCombo;
	}

	int rowFontStyle(int viewRow)
	{
		Component comp = table.prepareRenderer(table.getCellRenderer(viewRow, 0), viewRow, 0);
		return comp.getFont().getStyle();
	}

	interface DialogPort
	{
		Recipe showNewRecipeDialog(JFrame parent);

		void showRecipeEditorComingSoon(JFrame parent);

		String promptName(JFrame parent, String message, String title, String currentName);

		boolean confirm(JFrame parent, String message, String title);

		File chooseExportFile(JFrame parent, File defaultFile);

		void writeRecipeCsv(File target, Collection<Recipe> recipes) throws IOException;

		void showError(JFrame parent, String message, String title);

		void showError(JFrame parent, Throwable throwable, String title);
	}

	interface DbPort
	{
		Map<String, Recipe> recipes();

		void saveAll();

		void loadAll();
	}

	public interface RenameHook
	{
		void onRecipeRenamed(String oldName, String newName);
	}

	public interface DeleteHook
	{
		void onRecipeDeleted(String name);
	}

	public static class NoOpRenameHook implements RenameHook
	{
		@Override
		public void onRecipeRenamed(String oldName, String newName)
		{
		}
	}

	static class NoOpDeleteHook implements DeleteHook
	{
		@Override
		public void onRecipeDeleted(String name)
		{
		}
	}

	private static final class DefaultRecipeEditorNavPort implements RecipeEditorNavPort
	{
		private final JFrame parent;
		private final DialogPort dialogPort;

		DefaultRecipeEditorNavPort(JFrame parent, DialogPort dialogPort)
		{
			this.parent = parent;
			this.dialogPort = dialogPort;
		}

		@Override
		public void openRecipeEditor(String recipeName)
		{
			dialogPort.showRecipeEditorComingSoon(parent);
		}
	}

	static class DefaultDbPort implements DbPort
	{
		@Override
		public Map<String, Recipe> recipes()
		{
			return Database.getInstance().getRecipes();
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
		public Recipe showNewRecipeDialog(JFrame parent)
		{
			NewRecipeDialog d = new NewRecipeDialog(parent);
			d.setVisible(true);
			return d.getResult();
		}

		@Override
		public void showRecipeEditorComingSoon(JFrame parent)
		{
			JOptionPane.showMessageDialog(parent, getUiString("recipe.editor.coming.soon"), getUiString("recipe.edit.action"),
				JOptionPane.INFORMATION_MESSAGE);
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
		public void writeRecipeCsv(File target, Collection<Recipe> recipes) throws IOException
		{
			List<Settings.HopBitternessFormula> formulas =
				Settings.parseReportedFormulas(Database.getInstance().getSettings());

			try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8)))
			{
				w.println(csvHeader(formulas));
				for (Recipe recipe : recipes)
				{
					w.println(String.join(",", csvColumnsForRecipe(recipe, formulas)));
				}
			}
		}

		private static String csvHeader(List<Settings.HopBitternessFormula> formulas)
		{
			StringBuilder header = new StringBuilder("Name,Est OG,Est FG,Est ABV");
			for (Settings.HopBitternessFormula formula : formulas)
			{
				header.append(",IBU (");
				header.append(formula.toString());
				header.append(')');
			}
			header.append(",Color");
			return header.toString();
		}

		private static String[] csvColumnsForRecipe(
			Recipe recipe,
			List<Settings.HopBitternessFormula> formulas)
		{
			int colCount = 4 + formulas.size() + 1;
			String[] empty = new String[colCount];
			empty[0] = recipe.getName();
			for (int i = 1; i < colCount; i++)
			{
				empty[i] = "";
			}

			List<Volume> beers = null;
			try
			{
				recipe.run();
				beers = recipe.getBeers();
			}
			catch (Exception e)
			{
				e.printStackTrace(System.out);
				try
				{
					Brewday.getInstance().getLog().log(Log.LOUD, e);
				}
				catch (Throwable logEx)
				{
					logEx.printStackTrace(System.out);
				}
				return empty;
			}
			if (beers != null && !beers.isEmpty())
			{
				Volume mainBeer = beers.get(0);
				for (Volume beer : beers)
				{
					if (beer.getVolume().get() > mainBeer.getVolume().get())
					{
						mainBeer = beer;
					}
				}
				List<String> cols = new ArrayList<>();
				cols.add(recipe.getName());
				cols.add("" + mainBeer.getOriginalGravity().get(Quantity.Unit.SPECIFIC_GRAVITY));
				cols.add("" + mainBeer.getGravity().get(Quantity.Unit.SPECIFIC_GRAVITY));
				cols.add("" + mainBeer.getAbv().get(Quantity.Unit.PERCENTAGE_DISPLAY));
				for (Settings.HopBitternessFormula formula : formulas)
				{
					BitternessUnit ibu = mainBeer.getBitterness(formula);
					cols.add(ibu == null ? "" : "" + ibu.get(Quantity.Unit.IBU));
				}
				cols.add("" + mainBeer.getColour().get(Quantity.Unit.SRM));
				return cols.toArray(new String[0]);
			}
			return empty;
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
