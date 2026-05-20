/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingScreen;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing Tools &gt; Recipe Tag Manager: rename/delete tags globally and batch assign or unassign.
 */
public class RecipeTagManagerScreen extends JPanel implements SwingScreen
{
	private static final int COL_NAME = 0;
	private static final int COL_ASSIGNED = 1;

	private final JFrame parent;
	private final DirtyStateService dirtyState;
	private final Runnable onRecipeTagsDirty;

	private final DefaultListModel<String> tagListModel = new DefaultListModel<>();
	private final JList<String> tagList = new JList<>(tagListModel);
	private final RecipeTagTableModel recipeModel = new RecipeTagTableModel();
	private final JTable recipeTable;
	private TableRowSorter<RecipeTagTableModel> recipeSorter;

	private final JPanel recipeFilterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));

	private final Action saveAllAction = commandAction(
		getUiString("editor.apply.all"), "recipe.tag.manager.save", SwingIcons.IconKey.EDIT, this::saveAll);
	private final Action undoAllAction = commandAction(
		getUiString("editor.discard.all"), "recipe.tag.manager.undo", SwingIcons.IconKey.DELETE, this::undoAll);
	private final Action newTagAction = commandActionLabel(
		"tools.tag.manager.new.tag", "recipe.tag.manager.new.tag.act", SwingIcons.IconKey.RECIPE, this::createTag);
	private final Action renameTagAction = commandActionLabel(
		"tools.tag.manager.rename", "recipe.tag.manager.rename.act", SwingIcons.IconKey.RENAME, this::renameSelectedTag);
	private final Action deleteTagAction = commandActionLabel(
		"tools.tag.manager.delete", "recipe.tag.manager.delete.act", SwingIcons.IconKey.DELETE, this::deleteSelectedTag);
	private final Action assignAction = commandActionLabel(
		"tools.tag.manager.assign", "recipe.tag.manager.assign.act", SwingIcons.IconKey.EDIT, this::assignTagToSelection);
	private final Action removeAction = commandActionLabel(
		"tools.tag.manager.remove.from.selection", "recipe.tag.manager.remove.act", SwingIcons.IconKey.DELETE, this::removeTagFromSelection);
	private final Action selectTaggedAction = commandActionLabel(
		"tools.tag.manager.select.tagged", "recipe.tag.manager.select.tagged.act", SwingIcons.IconKey.EDIT, this::selectTaggedRows);
	private final Action clearSelectionAction = commandActionLabel(
		"tools.tag.manager.clear.selection", "recipe.tag.manager.clear.sel.act", SwingIcons.IconKey.DELETE, this::clearRecipeSelection);
	private final Action filterAction = commandAction(
		getUiString("common.filter"), "recipe.tag.manager.filter.action", SwingIcons.IconKey.EDIT, this::showRecipeFilterPanel);

	private JTextField recipeFilterField;

	public RecipeTagManagerScreen(JFrame parent, DirtyStateService dirtyState, Runnable onRecipeTagsDirty)
	{
		super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.onRecipeTagsDirty = onRecipeTagsDirty == null ? () -> {} : onRecipeTagsDirty;

		tagList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tagList.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean selected, boolean focus)
			{
				Component c = super.getListCellRendererComponent(list, value, index, selected, focus);
				if (value instanceof String s && !s.isBlank())
				{
					int n = Brewday.getInstance().countRecipesWithTag(s);
					setText(getUiString("tools.tag.manager.list.item", s, Integer.valueOf(n)));
				}
				return c;
			}
		});
		tagList.addListSelectionListener(e ->
		{
			if (!e.getValueIsAdjusting())
			{
				syncTagHighlightFromList();
			}
		});

		recipeTable = new JTable(recipeModel);
		recipeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		recipeTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		recipeTable.setName("recipe.tag.manager.recipe.table");
		recipeTable.setAutoCreateRowSorter(true);
		@SuppressWarnings("unchecked")
		TableRowSorter<RecipeTagTableModel> sorter =
			(TableRowSorter<RecipeTagTableModel>)recipeTable.getRowSorter();
		recipeSorter = sorter;
		recipeSorter.setSortKeys(List.of(new RowSorter.SortKey(COL_NAME, SortOrder.ASCENDING)));

		recipeFilterField = new JTextField(18);
		recipeFilterField.setName("recipe.tag.manager.filter.field");
		recipeFilterField.setToolTipText(getUiString("recipe.filter.tooltip"));
		JLabel recipeFilterLabel = new JLabel(getUiString("recipe.filter.label"));
		recipeFilterLabel.setLabelFor(recipeFilterField);
		recipeFilterPanel.add(recipeFilterLabel);
		recipeFilterPanel.add(recipeFilterField);
		recipeFilterPanel.setVisible(false);

		ActionHotkeySupport.applyTooltipText(filterAction, "tooltip.toolbar.filter");
		ActionHotkeySupport.setMnemonic(filterAction, KeyEvent.VK_F);

		recipeFilterField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				applyRecipeFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				applyRecipeFilter();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				applyRecipeFilter();
			}
		});

		JPanel east = new JPanel(new BorderLayout(4, 4));
		east.add(new JLabel(getUiString("tools.tag.manager.recipes.caption")), BorderLayout.NORTH);
		east.add(new JScrollPane(recipeTable), BorderLayout.CENTER);

		JScrollPane westScroll = new JScrollPane(tagList);
		westScroll.setBorder(BorderFactory.createTitledBorder(getUiString("tools.tag.manager.tags.caption")));
		JPanel west = new JPanel(new BorderLayout());

		JPanel tagButtons = new JPanel();
		tagButtons.setLayout(new BoxLayout(tagButtons, BoxLayout.PAGE_AXIS));
		tagButtons.add(tagToolbarButton(newTagAction));
		tagButtons.add(Box.createVerticalStrut(4));
		tagButtons.add(tagToolbarButton(renameTagAction));
		tagButtons.add(Box.createVerticalStrut(4));
		tagButtons.add(tagToolbarButton(deleteTagAction));
		west.add(tagButtons, BorderLayout.NORTH);
		west.add(westScroll, BorderLayout.CENTER);

		JPanel recipeButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		recipeButtons.add(new JButton(assignAction));
		recipeButtons.add(new JButton(removeAction));
		recipeButtons.add(new JButton(selectTaggedAction));
		recipeButtons.add(new JButton(clearSelectionAction));
		recipeButtons.add(new JButton(filterAction));

		JPanel recipeHeader = new JPanel(new BorderLayout(4, 4));
		recipeHeader.add(recipeButtons, BorderLayout.NORTH);
		recipeHeader.add(recipeFilterPanel, BorderLayout.SOUTH);

		JPanel recipesPanel = new JPanel(new BorderLayout(4, 4));
		recipesPanel.add(recipeHeader, BorderLayout.NORTH);
		recipesPanel.add(east, BorderLayout.CENTER);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, west, recipesPanel);
		split.setResizeWeight(0.28);
		split.setDividerLocation(304);

		JToolBar rootBar = buildRootToolbar();

		JPanel north = new JPanel(new BorderLayout(4, 4));
		north.add(rootBar, BorderLayout.NORTH);
		north.add(new JLabel("<html>" + getUiString("tools.tag.manager.hint")), BorderLayout.CENTER);

		add(north, BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);

		wireRecipeTableHotkeys();

		refresh();
		updateActions();
	}

	/*-------------------------------------------------------------------------*/
	private JToolBar buildRootToolbar()
	{
		JToolBar rootBar = new JToolBar();
		rootBar.setFloatable(false);
		rootBar.add(new JButton(saveAllAction));
		rootBar.add(new JButton(undoAllAction));
		return rootBar;
	}

	/*-------------------------------------------------------------------------*/
	private Action commandAction(String text, String actionKey, SwingIcons.IconKey iconKey, Runnable runnable)
	{
		Action a = new AbstractAction(text, SwingIcons.toolbarIcon(iconKey))
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				runnable.run();
			}
		};
		a.putValue(Action.ACTION_COMMAND_KEY, actionKey);
		return a;
	}

	/*-------------------------------------------------------------------------*/

	private static JButton tagToolbarButton(Action a)
	{
		JButton b = new JButton(a);
		b.setAlignmentX(Component.LEFT_ALIGNMENT);
		Dimension pref = b.getPreferredSize();
		b.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
		return b;
	}

	/*-------------------------------------------------------------------------*/
	private Action commandActionLabel(String textKey, String actionKey, SwingIcons.IconKey iconKey, Runnable runnable)
	{
		String text = getUiString(textKey);
		Action a = new AbstractAction(text, SwingIcons.toolbarIcon(iconKey))
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				runnable.run();
			}
		};
		a.putValue(Action.ACTION_COMMAND_KEY, actionKey);
		return a;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void refresh()
	{
		tagList.clearSelection();

		tagListModel.clear();
		for (String t : Brewday.getInstance().getRecipeTags())
		{
			tagListModel.addElement(t);
		}

		recipeModel.reloadRecipes();
		recipeModel.highlightTag(null);

		syncTagHighlightFromList();
		tagList.repaint();
		recipeTable.repaint();

		updateActions();

		applyRecipeFilter();
	}

	/*-------------------------------------------------------------------------*/
	private void syncTagHighlightFromList()
	{
		int i = tagList.getSelectedIndex();
		String highlighted = i < 0 ? null : tagListModel.get(i);
		recipeModel.highlightTag(highlighted);
		recipeTable.repaint();
		updateActions();
	}

	/*-------------------------------------------------------------------------*/
	private void updateActions()
	{
		String selectedTag = recipeModel.getHighlightedTag();
		boolean tagSelected = selectedTag != null && !selectedTag.isEmpty();
		renameTagAction.setEnabled(tagSelected);
		deleteTagAction.setEnabled(tagSelected);
		assignAction.setEnabled(tagSelected);
		removeAction.setEnabled(tagSelected);
		selectTaggedAction.setEnabled(tagSelected);
	}

	/*-------------------------------------------------------------------------*/
	private void wireRecipeTableHotkeys()
	{
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F), "recipe.tag.manager.hotkey.filterCtrl", filterAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), "recipe.tag.manager.hotkey.filterAlt", filterAction);
		ActionHotkeySupport.bindFocused(recipeFilterField, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "recipe.tag.manager.hotkey.filterEscape", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				hideRecipeFilterPanel();
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	private void showRecipeFilterPanel()
	{
		recipeFilterPanel.setVisible(true);
		recipeFilterPanel.revalidate();
		recipeFilterPanel.repaint();
		recipeFilterField.requestFocusInWindow();
		recipeFilterField.selectAll();
	}

	/*-------------------------------------------------------------------------*/
	private void hideRecipeFilterPanel()
	{
		recipeFilterField.setText("");
		recipeFilterPanel.setVisible(false);
		recipeFilterPanel.revalidate();
		recipeFilterPanel.repaint();
		if (recipeSorter != null)
		{
			recipeSorter.setRowFilter(null);
		}
		recipeTable.requestFocusInWindow();
	}

	/*-------------------------------------------------------------------------*/
	private void applyRecipeFilter()
	{
		if (recipeSorter == null)
		{
			return;
		}
		String raw = recipeFilterField.getText();
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty())
		{
			recipeSorter.setRowFilter(null);
			return;
		}

		final String needle = trimmed;
		recipeSorter.setRowFilter(new RowFilter<RecipeTagTableModel, Integer>()
		{
			@Override
			public boolean include(RowFilter.Entry<? extends RecipeTagTableModel, ? extends Integer> entry)
			{
				Object v = entry.getValue(COL_NAME);
				String name = v == null ? "" : v.toString();
				return Pattern.compile("(?i)" + Pattern.quote(needle)).matcher(name).find();
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	private void saveAll()
	{
		if (JOptionPane.showConfirmDialog(parent, getUiString("editor.apply.all.msg"), getUiString("editor.apply.all"),
				JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
		{
			return;
		}
		stopTableEdit();
		try
		{
			Database.getInstance().saveAll();
			dirtyState.clear();
			refresh();
			onRecipeTagsDirty.run();
		}
		catch (Exception ex)
		{
			ex.printStackTrace(System.out);
			JOptionPane.showMessageDialog(parent, ex.getMessage() == null ? ex.toString() : ex.getMessage(),
					getUiString("ui.error"), JOptionPane.ERROR_MESSAGE);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void undoAll()
	{
		if (JOptionPane.showConfirmDialog(parent, getUiString("editor.discard.all.msg"),
				getUiString("editor.discard.all"),
				JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
		{
			return;
		}
		stopTableEdit();
		try
		{
			Database.getInstance().loadAll();
			dirtyState.clear();
			refresh();
			onRecipeTagsDirty.run();
		}
		catch (Exception ex)
		{
			ex.printStackTrace(System.out);
			JOptionPane.showMessageDialog(parent, ex.getMessage() == null ? ex.toString() : ex.getMessage(),
					getUiString("ui.error"), JOptionPane.ERROR_MESSAGE);
		}
	}

	/*-------------------------------------------------------------------------*/
	private void stopTableEdit()
	{
		TableCellEditor ed = recipeTable.getCellEditor();
		if (ed != null)
		{
			ed.stopCellEditing();
		}
	}

	/*-------------------------------------------------------------------------*/
	private void markRecipesDirty(List<Recipe> touched)
	{
		for (Recipe r : touched)
		{
			dirtyState.markDirty(r, "recipes");
		}
		if (!touched.isEmpty())
		{
			onRecipeTagsDirty.run();
		}
	}

	/*-------------------------------------------------------------------------*/
	private String getSelectedTagOrWarn(String titleKey)
	{
		String t = recipeModel.getHighlightedTag();
		if (t == null || t.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, getUiString("tools.tag.manager.need.tag"),
					getUiString(titleKey), JOptionPane.WARNING_MESSAGE);
			return null;
		}
		return t;
	}

	/*-------------------------------------------------------------------------*/
	private List<Recipe> selectedRecipesInTable()
	{
		List<Recipe> out = new ArrayList<>();
		int[] rows = recipeTable.getSelectedRows();
		for (int viewRow : rows)
		{
			int mr = recipeTable.convertRowIndexToModel(viewRow);
			if (mr >= 0)
			{
				Recipe r = recipeModel.getRecipeAt(mr);
				if (r != null)
				{
					out.add(r);
				}
			}
		}
		return out;
	}

	/*-------------------------------------------------------------------------*/
	private void assignTagToSelection()
	{
		String tag = getSelectedTagOrWarn("tools.tag.manager.assign.title");
		if (tag == null)
		{
			return;
		}
		List<Recipe> sel = selectedRecipesInTable();
		if (sel.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, getUiString("tools.tag.manager.need.recipes"),
					getUiString("tools.tag.manager.assign.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		stopTableEdit();
		List<Recipe> touched = Brewday.getInstance().addTagToRecipesIfAbsent(tag, sel);
		markRecipesDirty(touched);
		recipeTable.repaint();
		tagList.repaint();
		syncTagHighlightFromList();
	}

	/*-------------------------------------------------------------------------*/
	private void removeTagFromSelection()
	{
		String tag = getSelectedTagOrWarn("tools.tag.manager.remove.title");
		if (tag == null)
		{
			return;
		}
		List<Recipe> sel = selectedRecipesInTable();
		if (sel.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, getUiString("tools.tag.manager.need.recipes"),
					getUiString("tools.tag.manager.remove.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		stopTableEdit();
		List<Recipe> touched = Brewday.getInstance().removeTagFromRecipes(tag, sel);
		markRecipesDirty(touched);
		recipeTable.repaint();
		tagList.repaint();
		syncTagHighlightFromList();
	}

	/*-------------------------------------------------------------------------*/
	private void renameSelectedTag()
	{
		String oldTag = getSelectedTagOrWarn("tools.tag.manager.rename.title");
		if (oldTag == null)
		{
			return;
		}
		stopTableEdit();
		String typed = JOptionPane.showInputDialog(parent,
				getUiString("tools.tag.manager.rename.prompt", oldTag),
				getUiString("tools.tag.manager.rename.title"), JOptionPane.PLAIN_MESSAGE);
		if (typed == null)
		{
			return;
		}
		String newTag = typed.trim();
		if (newTag.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, getUiString("tools.tag.manager.name.empty"),
					getUiString("tools.tag.manager.rename.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (newTag.equals(oldTag))
		{
			return;
		}

		List<Recipe> touched = Brewday.getInstance().renameRecipeTagAcrossAll(oldTag, newTag);
		markRecipesDirty(touched);
		refresh();
		selectTagInList(newTag);
	}

	/*-------------------------------------------------------------------------*/
	private void deleteSelectedTag()
	{
		String tag = getSelectedTagOrWarn("tools.tag.manager.delete.title");
		if (tag == null)
		{
			return;
		}
		int uses = Brewday.getInstance().countRecipesWithTag(tag);
		stopTableEdit();
		if (JOptionPane.showConfirmDialog(parent, getUiString("tools.tag.manager.delete.confirm.body", Integer.valueOf(uses)),
				getUiString("tools.tag.manager.delete.title"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
		{
			return;
		}
		List<Recipe> touched = Brewday.getInstance().deleteRecipeTagEverywhere(tag);
		markRecipesDirty(touched);
		refresh();
	}

	/*-------------------------------------------------------------------------*/
	private void createTag()
	{
		String raw = JOptionPane.showInputDialog(parent, getUiString("tools.tag.manager.new.prompt"),
				getUiString("tools.tag.manager.new.title"), JOptionPane.PLAIN_MESSAGE);
		String name = raw == null ? null : raw.trim();
		if (name == null || name.isEmpty())
		{
			return;
		}
		if (Brewday.getInstance().getRecipeTags().contains(name))
		{
			JOptionPane.showMessageDialog(parent, getUiString("tools.tag.manager.duplicate.name"),
					getUiString("tools.tag.manager.new.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		stopTableEdit();
		List<Recipe> sel = selectedRecipesInTable();
		if (sel.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, getUiString("tools.tag.manager.new.need.selection"),
					getUiString("tools.tag.manager.new.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		List<Recipe> touched = Brewday.getInstance().addTagToRecipesIfAbsent(name, sel);
		markRecipesDirty(touched);
		refresh();
		selectTagInList(name);
	}

	/*-------------------------------------------------------------------------*/
	private void selectTagInList(String tagName)
	{
		for (int i = 0; i < tagListModel.size(); i++)
		{
			if (tagName.equals(tagListModel.getElementAt(i)))
			{
				tagList.setSelectedIndex(i);
				tagList.ensureIndexIsVisible(i);
				return;
			}
		}
		tagList.clearSelection();
	}

	/*-------------------------------------------------------------------------*/
	private void selectTaggedRows()
	{
		String tag = recipeModel.getHighlightedTag();
		if (tag == null)
		{
			return;
		}
		recipeTable.clearSelection();
		for (int mr = 0; mr < recipeModel.getRowCount(); mr++)
		{
			Recipe r = recipeModel.getRecipeAt(mr);
			if (r != null && r.getTags().contains(tag))
			{
				int vr = recipeTable.convertRowIndexToView(mr);
				if (vr >= 0)
				{
					recipeTable.addRowSelectionInterval(vr, vr);
				}
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private void clearRecipeSelection()
	{
		recipeTable.clearSelection();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void onActivate()
	{
		refresh();
	}

	private final class RecipeTagTableModel extends AbstractTableModel
	{
		private final List<Recipe> rows = new ArrayList<>();
		private String highlightedTag;

		private void reloadRecipes()
		{
			rows.clear();
			for (Recipe r : Database.getInstance().getRecipes().values())
			{
				rows.add(r);
			}
			Collections.sort(rows, Comparator.comparing(Recipe::getName, Comparator.nullsFirst(String::compareTo)));
			fireTableDataChanged();
		}

		private void highlightTag(String tag)
		{
			this.highlightedTag = tag;
			fireTableDataChanged();
			updateActions();
		}

		private Recipe getRecipeAt(int modelRow)
		{
			if (modelRow < 0 || modelRow >= rows.size())
			{
				return null;
			}
			return rows.get(modelRow);
		}

		private String getHighlightedTag()
		{
			return highlightedTag;
		}

		/*-------------------------------------------------------------------------*/

		@Override
		public int getRowCount()
		{
			return rows.size();
		}

		@Override
		public int getColumnCount()
		{
			return 2;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			Recipe r = getRecipeAt(rowIndex);
			if (r == null)
			{
				return "";
			}
			if (columnIndex == COL_NAME)
			{
				return r.getName() == null ? "" : r.getName();
			}
			if (highlightedTag == null)
			{
				return Boolean.FALSE;
			}
			return Boolean.valueOf(r.getTags().contains(highlightedTag));
		}

		@Override
		public String getColumnName(int column)
		{
			return column == COL_NAME ? getUiString("recipe.name") : getUiString("tools.tag.manager.col.assigned");
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			if (columnIndex == COL_ASSIGNED)
			{
				return Boolean.class;
			}
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == COL_ASSIGNED && highlightedTag != null;
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex)
		{
			if (columnIndex != COL_ASSIGNED || highlightedTag == null)
			{
				return;
			}
			Recipe recipe = getRecipeAt(rowIndex);
			if (recipe == null)
			{
				return;
			}
			boolean wantAssigned = Boolean.TRUE.equals(value);
			boolean hasNow = recipe.getTags().contains(highlightedTag);
			if (wantAssigned == hasNow)
			{
				return;
			}
			Brewday bd = Brewday.getInstance();
			boolean changed = wantAssigned ?
				bd.addTagToRecipeIfAbsent(recipe, highlightedTag) :
				bd.removeTagFromRecipe(recipe, highlightedTag);
			if (changed)
			{
				dirtyState.markDirty(recipe, "recipes");
				onRecipeTagsDirty.run();
				fireTableRowsUpdated(rowIndex, rowIndex);
				tagList.repaint();
			}
		}
	}
}
