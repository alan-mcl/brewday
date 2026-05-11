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
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingScreen;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.dialogs.EditEquipmentProfileDialog;

import static mclachlan.brewday.util.StringUtils.format;
import static mclachlan.brewday.util.StringUtils.getUiString;

public class EquipmentProfilesScreen extends JPanel implements SwingScreen
{
	private final JFrame parent;
	private final DirtyStateService dirtyState;
	private final DialogPort dialogPort;
	private final DbPort dbPort;
	private final RenameHook renameHook;
	private final DeleteHook deleteHook;
	private final DefaultTableModel model;
	private final JTable table;
	private final JTextField filterField;
	private final JPanel filterPanel;
	private final TableRowSorter<DefaultTableModel> sorter;
	private final Action saveAction, undoAction, addAction, editAction, duplicateAction, renameAction, deleteAction, filterAction, exportAction;

	public EquipmentProfilesScreen(JFrame parent, DirtyStateService dirtyState)
	{
		this(parent, dirtyState, new SwingDialogPort(), new DefaultDbPort(), new NoOpRenameHook(), new NoOpDeleteHook());
	}

	public EquipmentProfilesScreen(JFrame parent, DirtyStateService dirtyState, RenameHook renameHook, DeleteHook deleteHook)
	{
		this(parent, dirtyState, new SwingDialogPort(), new DefaultDbPort(), renameHook, deleteHook);
	}

	EquipmentProfilesScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort)
	{
		this(parent, dirtyState, dialogPort, dbPort, new NoOpRenameHook(), new NoOpDeleteHook());
	}

	EquipmentProfilesScreen(JFrame parent, DirtyStateService dirtyState, DialogPort dialogPort, DbPort dbPort,
		RenameHook renameHook, DeleteHook deleteHook)
	{
		super(new BorderLayout());
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.dialogPort = dialogPort;
		this.dbPort = dbPort;
		this.renameHook = renameHook;
		this.deleteHook = deleteHook;

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		saveAction = commandAction("editor.apply.all", "equipment.save.action", SwingIcons.IconKey.EDIT, this::saveAll);
		undoAction = commandAction("editor.discard.all", "equipment.undo.action", SwingIcons.IconKey.DELETE, this::undoAll);
		addAction = commandAction("common.add", "equipment.add.action", SwingIcons.IconKey.EQUIPMENT, this::addItem);
		editAction = commandAction("common.edit", "equipment.edit.action", SwingIcons.IconKey.EDIT, this::editSelected);
		duplicateAction = commandAction("common.duplicate", "equipment.duplicate.action", SwingIcons.IconKey.DUPLICATE, this::duplicateSelected);
		renameAction = commandAction("editor.rename", "equipment.rename.action", SwingIcons.IconKey.EDIT, this::renameSelected);
		deleteAction = commandAction("common.remove", "equipment.delete.action", SwingIcons.IconKey.DELETE, this::deleteSelected);
		filterAction = commandAction("equipment.filter.action", "equipment.filter.action", SwingIcons.IconKey.EDIT, this::showFilterPanel);
		exportAction = commandAction("common.export.csv", "equipment.export.action", SwingIcons.IconKey.EXPORT_CSV, this::exportCsv);
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
		JLabel filterLabel = new JLabel(getUiString("equipment.filter.label"));
		filterField = new JTextField(20);
		filterField.setName("equipment.filter.field");
		filterField.setToolTipText(getUiString("equipment.filter.tooltip"));
		filterLabel.setLabelFor(filterField);
		filterPanel.add(filterLabel);
		filterPanel.add(filterField);
		filterPanel.setVisible(false);
		north.add(filterPanel, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		model = new DefaultTableModel(new String[] {
			getUiString("equipment.name"),
			getUiString("equipment.conversion.efficiency"),
			getUiString("equipment.mash.tun.volume"),
			getUiString("equipment.boil.kettle.volume"),
			getUiString("equipment.fermenter.volume")
		}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		table = new JTable(model);
		table.setName("equipment.table");
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
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_N), "equipment.hotkey.add", addAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_E), "equipment.hotkey.editCtrl", editAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_R), "equipment.hotkey.renameCtrl", renameAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "equipment.hotkey.renameF2", renameAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_D), "equipment.hotkey.duplicateCtrl", duplicateAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_F), "equipment.hotkey.filterCtrl", filterAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), "equipment.hotkey.filterAlt", filterAction);
		ActionHotkeySupport.bind(this, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "equipment.hotkey.export", exportAction);
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "equipment.hotkey.export.window");
		getActionMap().put("equipment.hotkey.export.window", exportAction);
		ActionHotkeySupport.bind(this, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "equipment.hotkey.deleteKey", deleteAction);
		ActionHotkeySupport.bindFocused(table, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "equipment.hotkey.editEnter", editAction);
		ActionHotkeySupport.bindFocused(filterField, ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_X), "equipment.hotkey.export.filterFocused", exportAction);
		ActionHotkeySupport.bindFocused(filterField, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "equipment.hotkey.filterEscape", new AbstractAction()
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

	private EquipmentProfile selected()
	{
		int row = table.getSelectedRow();
		if (row < 0)
		{
			return null;
		}
		String name = (String)model.getValueAt(table.convertRowIndexToModel(row), 0);
		return dbPort.equipmentProfiles().get(name);
	}

	private void addItem()
	{
		EquipmentProfile draft = new EquipmentProfile("");
		EquipmentProfile created = dialogPort.showEditEquipmentProfileDialog(parent, draft, true);
		if (created == null)
		{
			return;
		}
		if (dbPort.equipmentProfiles().containsKey(created.getName()))
		{
			dialogPort.showError(parent, getUiString("equipment.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.equipmentProfiles().put(created.getName(), created);
		dirtyState.markDirty(created, "brewing", "equipment.profiles");
		refresh();
	}

	private void duplicateSelected()
	{
		EquipmentProfile current = selected();
		if (current == null)
		{
			return;
		}
		EquipmentProfile draft = new EquipmentProfile(current);
		draft.setName("");
		EquipmentProfile created = dialogPort.showEditEquipmentProfileDialog(parent, draft, true);
		if (created == null)
		{
			return;
		}
		if (dbPort.equipmentProfiles().containsKey(created.getName()))
		{
			dialogPort.showError(parent, getUiString("equipment.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.equipmentProfiles().put(created.getName(), created);
		dirtyState.markDirty(created, "brewing", "equipment.profiles");
		refresh();
	}

	private void editSelected()
	{
		EquipmentProfile current = selected();
		if (current == null)
		{
			return;
		}
		EquipmentProfile edited = dialogPort.showEditEquipmentProfileDialog(parent, new EquipmentProfile(current), false);
		if (edited == null)
		{
			return;
		}
		current.setDescription(edited.getDescription());
		current.setElevation(edited.getElevation());
		current.setConversionEfficiency(edited.getConversionEfficiency());
		current.setMashTunVolume(edited.getMashTunVolume());
		current.setMashTunWeight(edited.getMashTunWeight());
		current.setMashTunSpecificHeat(edited.getMashTunSpecificHeat());
		current.setLauterLoss(edited.getLauterLoss());
		current.setBoilKettleVolume(edited.getBoilKettleVolume());
		current.setBoilEvapourationRate(edited.getBoilEvapourationRate());
		current.setBoilElementPower(edited.getBoilElementPower());
		current.setHopUtilisation(edited.getHopUtilisation());
		current.setTrubAndChillerLoss(edited.getTrubAndChillerLoss());
		current.setFermenterVolume(edited.getFermenterVolume());
		dirtyState.markDirty(current, "brewing", "equipment.profiles");
		refresh();
	}

	private void deleteSelected()
	{
		EquipmentProfile current = selected();
		if (current == null)
		{
			return;
		}
		if (!dialogPort.confirm(parent, getUiString("equipment.delete.msg"), getUiString("common.remove")))
		{
			return;
		}
		String name = current.getName();
		dbPort.equipmentProfiles().remove(name);
		deleteHook.onEquipmentProfileDeleted(name);
		dirtyState.markDirty("brewing", "equipment.profiles");
		refresh();
	}

	private void renameSelected()
	{
		EquipmentProfile current = selected();
		if (current == null)
		{
			return;
		}
		String oldName = current.getName();
		String renamed = dialogPort.promptName(parent, getUiString("equipment.rename"), getUiString("editor.rename"), oldName);
		if (renamed == null)
		{
			return;
		}
		String newName = renamed.trim();
		if (newName.isEmpty())
		{
			dialogPort.showError(parent, getUiString("equipment.new.dialog.not.empty"), getUiString("ui.error"));
			return;
		}
		if (oldName.equals(newName))
		{
			return;
		}
		if (dbPort.equipmentProfiles().containsKey(newName))
		{
			dialogPort.showError(parent, getUiString("equipment.new.dialog.already.exists"), getUiString("ui.error"));
			return;
		}
		dbPort.equipmentProfiles().remove(oldName);
		current.setName(newName);
		dbPort.equipmentProfiles().put(newName, current);
		renameHook.onEquipmentProfileRenamed(oldName, newName);
		dirtyState.markDirty(current, "brewing", "equipment.profiles");
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
		File selected = dialogPort.chooseExportFile(parent, new File("equipment-profiles.csv"));
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

	private Collection<EquipmentProfile> visibleItems()
	{
		Collection<EquipmentProfile> items = new ArrayList<>();
		for (int row = 0; row < table.getRowCount(); row++)
		{
			int modelRow = table.convertRowIndexToModel(row);
			String name = (String)model.getValueAt(modelRow, 0);
			EquipmentProfile item = dbPort.equipmentProfiles().get(name);
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
		for (EquipmentProfile p : dbPort.equipmentProfiles().values())
		{
			model.addRow(new Object[] {
				p.getName(),
				fmtPercent(p.getConversionEfficiency()),
				fmtVolume(p.getMashTunVolume()),
				fmtVolume(p.getBoilKettleVolume()),
				fmtVolume(p.getFermenterVolume())
			});
		}
	}

	private static String fmtPercent(PercentageUnit u)
	{
		return u == null ? "" : format(u.get(), Quantity.Unit.PERCENTAGE_DISPLAY);
	}

	private static String fmtVolume(VolumeUnit u)
	{
		return u == null ? "" : format(u.get(Quantity.Unit.LITRES), Quantity.Unit.LITRES);
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
		String name = (String)model.getValueAt(modelRow, 0);
		EquipmentProfile item = dbPort.equipmentProfiles().get(name);
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

	Action getUndoAction()
	{
		return undoAction;
	}

	Action getAddAction()
	{
		return addAction;
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

	int rowFontStyle(int viewRow)
	{
		Component comp = table.prepareRenderer(table.getCellRenderer(viewRow, 0), viewRow, 0);
		return comp.getFont().getStyle();
	}

	interface DialogPort
	{
		EquipmentProfile showEditEquipmentProfileDialog(JFrame parent, EquipmentProfile current, boolean createMode);

		String promptName(JFrame parent, String message, String title, String currentName);

		boolean confirm(JFrame parent, String message, String title);

		File chooseExportFile(JFrame parent, File defaultFile);

		void writeCsv(File target, Collection<EquipmentProfile> profiles) throws IOException;

		void showError(JFrame parent, String message, String title);
	}

	interface DbPort
	{
		Map<String, EquipmentProfile> equipmentProfiles();

		void saveAll();

		void loadAll();
	}

	public interface RenameHook
	{
		void onEquipmentProfileRenamed(String oldName, String newName);
	}

	public interface DeleteHook
	{
		void onEquipmentProfileDeleted(String name);
	}

	static class NoOpRenameHook implements RenameHook
	{
		@Override
		public void onEquipmentProfileRenamed(String oldName, String newName)
		{
		}
	}

	static class NoOpDeleteHook implements DeleteHook
	{
		@Override
		public void onEquipmentProfileDeleted(String name)
		{
		}
	}

	static class DefaultDbPort implements DbPort
	{
		@Override
		public Map<String, EquipmentProfile> equipmentProfiles()
		{
			return Database.getInstance().getEquipmentProfiles();
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
		public EquipmentProfile showEditEquipmentProfileDialog(JFrame parent, EquipmentProfile current, boolean createMode)
		{
			EditEquipmentProfileDialog d = new EditEquipmentProfileDialog(parent, current, createMode);
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
		public void writeCsv(File target, Collection<EquipmentProfile> profiles) throws IOException
		{
			try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8)))
			{
				w.println("Name,Description,Elevation_m,ConversionEfficiency_pct,MashTunVolume_L,MashTunWeight_kg,MashTunSpecificHeat_J_per_kg_C,LauterLoss_L,BoilKettleVolume_L,BoilEvaporation_pct,BoilElementPower_kW,HopUtilisation_pct,TrubChillerLoss_L,FermenterVolume_L");
				for (EquipmentProfile p : profiles)
				{
					String desc = p.getDescription() == null ? "" : p.getDescription().replace('\n', ' ').replace('\r', ' ');
					double elev = p.getElevation() == null ? 0D : p.getElevation().get(Quantity.Unit.METRE);
					double conv = p.getConversionEfficiency() == null ? 0D : p.getConversionEfficiency().get(Quantity.Unit.PERCENTAGE_DISPLAY);
					double mashVol = p.getMashTunVolume() == null ? 0D : p.getMashTunVolume().get(Quantity.Unit.LITRES);
					double mashWt = p.getMashTunWeight() == null ? 0D : p.getMashTunWeight().get(Quantity.Unit.KILOGRAMS);
					double sh = p.getMashTunSpecificHeat() == null ? 0D : p.getMashTunSpecificHeat().get();
					double lauter = p.getLauterLoss() == null ? 0D : p.getLauterLoss().get(Quantity.Unit.LITRES);
					double boilVol = p.getBoilKettleVolume() == null ? 0D : p.getBoilKettleVolume().get(Quantity.Unit.LITRES);
					double evap = p.getBoilEvapourationRate() == null ? 0D : p.getBoilEvapourationRate().get(Quantity.Unit.PERCENTAGE_DISPLAY);
					double power = p.getBoilElementPower() == null ? 0D : p.getBoilElementPower().get(Quantity.Unit.KILOWATT);
					double hop = p.getHopUtilisation() == null ? 0D : p.getHopUtilisation().get(Quantity.Unit.PERCENTAGE_DISPLAY);
					double trub = p.getTrubAndChillerLoss() == null ? 0D : p.getTrubAndChillerLoss().get(Quantity.Unit.LITRES);
					double ferm = p.getFermenterVolume() == null ? 0D : p.getFermenterVolume().get(Quantity.Unit.LITRES);
					w.printf("%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n",
						p.getName(), desc, elev, conv, mashVol, mashWt, sh, lauter, boilVol, evap, power, hop, trub, ferm);
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
