/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.ui.swing;

import com.alee.laf.WebLookAndFeel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;

/**
 *
 */
public class SwingUi extends JFrame implements WindowListener
{
	public static SwingUi instance;

	public static ImageIcon brewdayIcon, grainsIcon, hopsIcon, waterIcon, stepIcon, recipeIcon,
		yeastIcon, miscIcon, removeIcon, increaseIcon, decreaseIcon, moreTimeIcon, lessTimeIcon,
		searchIcon, editIcon, newIcon, deleteIcon, duplicateIcon, substituteIcon, processTemplateIcon,
		beerIcon, equipmentIcon, settingsIcon, stylesIcon, databaseIcon, inventoryIcon, exitIcon,
		saveIcon, undoIcon, renameIcon, helpIcon, documentIcon;

	private RecipesPanel recipesPanel;
	private BatchesPanel batchesPanel;
	private ProcessTemplatePanel processTemplatePanel;
	private EquipmentProfilePanel equipmentProfilePanel;
	private InventoryPanel inventoryPanel;
	private SettingsPanel settingsPanel;
	private JLabel status;
	private JTabbedPane tabs, brewingDataTabs, refDatabaseTabs;
	private BitSet dirty = new BitSet();
	private List<EditorPanel> editorPanels = new ArrayList<>();

	/*-------------------------------------------------------------------------*/
	public SwingUi() throws Exception
	{
		// install our global exception handler
		EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		queue.push(new EventQueueProxy());

		// WebLAF
		UIManager.setLookAndFeel(new WebLookAndFeel());

		// JGoodies
//		UIManager.setLookAndFeel(new Plastic3DLookAndFeel());

		instance = this;
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		brewdayIcon = SwingUi.createImageIcon("img/brewday.png");
		recipeIcon = SwingUi.createImageIcon("img/icons8-beer-recipe-48.png");
		stepIcon = SwingUi.createImageIcon("img/icons8-file-48.png");
		hopsIcon = SwingUi.createImageIcon("img/icons8-hops-48.png");
		grainsIcon = SwingUi.createImageIcon("img/icons8-carbohydrates-48.png");
		waterIcon = SwingUi.createImageIcon("img/icons8-water-48.png");
		yeastIcon = SwingUi.createImageIcon("img/icons8-experiment-48.png");
		miscIcon = SwingUi.createImageIcon("img/icons8-sugar-cubes-48.png");
		removeIcon = SwingUi.createImageIcon("img/icons8-delete-48.png");
		increaseIcon = SwingUi.createImageIcon("img/icons8-plus-48.png");
		decreaseIcon = SwingUi.createImageIcon("img/icons8-minus-48.png");
		moreTimeIcon = SwingUi.createImageIcon("img/icons8-future-48.png");
		lessTimeIcon = SwingUi.createImageIcon("img/icons8-time-machine-48.png");
		searchIcon = SwingUi.createImageIcon("img/icons8-search-48.png");
		editIcon = SwingUi.createImageIcon("img/icons8-edit-property-48.png");
		newIcon = SwingUi.createImageIcon("img/icons8-add-new-48.png");
		deleteIcon = SwingUi.createImageIcon("img/icons8-delete-48.png");
		duplicateIcon = SwingUi.createImageIcon("img/icons8-transfer-48.png");
		substituteIcon = SwingUi.createImageIcon("img/icons8-replace-48.png");
		processTemplateIcon = SwingUi.createImageIcon("img/icons8-flow-48.png");
		beerIcon = SwingUi.createImageIcon("img/icons8-beer-glass-48.png");
		equipmentIcon = SwingUi.createImageIcon("img/icons8-brewsystem-48.png");
		stylesIcon = SwingUi.createImageIcon("img/icons8-test-passed-48.png");
		settingsIcon = SwingUi.createImageIcon("img/icons8-settings-48.png");
		databaseIcon = SwingUi.createImageIcon("img/icons8-database-48.png");
		inventoryIcon = SwingUi.createImageIcon("img/icons8-trolley-48.png");
		exitIcon = SwingUi.createImageIcon("img/icons8-close-window-48.png");
		saveIcon = SwingUi.createImageIcon("img/icons8-save-48.png");
		undoIcon = SwingUi.createImageIcon("img/icons8-undo-48.png");
		renameIcon = SwingUi.createImageIcon("img/icons8-rename-48.png");
		helpIcon = SwingUi.createImageIcon("img/icons8-help-48.png");
		documentIcon = SwingUi.createImageIcon("img/icons8-document-48.png");

		this.setIconImage(brewdayIcon.getImage());

		// how about we not localise this bit huh
		setTitle("Brewday " + getVersion());

		Database.getInstance().loadAll();

		EditingControls c = new EditingControls(this);
		JMenuBar menuBar = c.buildMenuBar();
		JPanel bottom = c.getBottomPanel();
		status = c.getStatus();

		brewingDataTabs = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
		refDatabaseTabs = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);

		// Brewing tabs
		recipesPanel = new RecipesPanel(Tab.RECIPES);
		processTemplatePanel = new ProcessTemplatePanel(Tab.PROCESS_TEMPLATES);
		equipmentProfilePanel = new EquipmentProfilePanel(Tab.EQUIPMENT_PROFILES);
		batchesPanel = new BatchesPanel(Tab.BATCHES);

		addTab(brewingDataTabs, StringUtils.getUiString("tab.batches"), beerIcon, batchesPanel);
		addTab(brewingDataTabs, StringUtils.getUiString("tab.recipes"), recipeIcon, recipesPanel);
		addTab(brewingDataTabs, StringUtils.getUiString("tab.process.templates"), processTemplateIcon, processTemplatePanel);
		addTab(brewingDataTabs, StringUtils.getUiString("tab.equipment.profiles"), equipmentIcon, equipmentProfilePanel);

		// Ref Database tabs
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.water"), waterIcon, getWatersPanel());
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.fermentables"), grainsIcon, getFermentablesPanel());
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.hops"), hopsIcon, getHopsPanel());
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.yeast"), yeastIcon, getYeastPanel());
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.misc"), miscIcon, getMiscsPanel());
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.styles"), stylesIcon, getStylesPanel());

		this.setJMenuBar(menuBar);

		this.setLayout(new BorderLayout(5, 5));

		tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

		inventoryPanel = new InventoryPanel(Tab.INVENTORY);
		inventoryPanel.refresh();

		settingsPanel = new SettingsPanel(Tab.SETTINGS);
		settingsPanel.refresh();

		tabs.addTab(StringUtils.getUiString("tab.brewing"), beerIcon, brewingDataTabs);
		tabs.addTab(StringUtils.getUiString("tab.inventory"), inventoryIcon, inventoryPanel);
		tabs.addTab(StringUtils.getUiString("tab.reference.database"), databaseIcon, refDatabaseTabs);
		tabs.addTab(StringUtils.getUiString("tab.settings"), settingsIcon, settingsPanel);

		this.add(tabs, BorderLayout.CENTER);
		this.add(bottom, BorderLayout.SOUTH);

		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)(d.getWidth() / 2);
		int centerY = (int)(d.getHeight() / 2);
		int width = (int)(d.getWidth() - 20);
		int height = (int)(d.getHeight() - 20);

		addWindowListener(this);
		this.setBounds(centerX - width / 2, centerY - height / 2, width, height);
		this.setVisible(true);
	}

	/**
	 * Returns an ImageIcon, or null if the path was invalid.
	 */
	protected static ImageIcon createImageIcon(String path)
	{
		Image image = Toolkit.getDefaultToolkit().getImage(path);
		Image scaledInstance = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
		return new ImageIcon(scaledInstance);
	}

	private Component getWatersPanel()
	{
		return new WatersReferencePanel(Tab.REF_WATERS);
	}

	private Component getMiscsPanel()
	{
		return new MiscsReferencePanel(Tab.REF_MISCS);
	}

	private Component getStylesPanel()
	{
		return new StylesReferencePanel(Tab.REF_STYLES);
	}

	private Component getYeastPanel()
	{
		return new YeastsReferencePanel(Tab.REF_YEASTS);
	}

	private Component getFermentablesPanel()
	{
		return new FermentablesReferencePanel(Tab.REF_FERMENTABLES);
	}

	private Component getHopsPanel()
	{
		return new HopsReferencePanel(Tab.REF_HOPS);
	}

	/*-------------------------------------------------------------------------*/
	private void addTab(JTabbedPane parent, String title, Icon icon,
		Component panel)
	{
		parent.addTab(title, icon, panel);
		if (panel instanceof EditorPanel)
		{
			this.editorPanels.add((EditorPanel)panel);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void refreshRecipesPanel()
	{
		if (recipesPanel != null)
		{
			this.recipesPanel.runRecipe();
		}
	}

	/*-------------------------------------------------------------------------*/
	public void refreshProcessSteps()
	{
		if (recipesPanel != null)
		{
			this.recipesPanel.runRecipe();
			this.recipesPanel.refreshStepCards();
			this.recipesPanel.refreshEndResult();
		}
	}

	/*-------------------------------------------------------------------------*/
	public void setDirty(int tab)
	{
		if (tab < 0)
		{
			return;
		}

		dirty.set(tab);

		setDirtyStatusMessage();
	}

	/*-------------------------------------------------------------------------*/
	private void setDirtyStatusMessage()
	{
		if (dirty.isEmpty())
		{
			status.setText("");
			return;
		}

		StringBuilder message = new StringBuilder(StringUtils.getUiString("ui.dirty"));
		for (int i = 0; i < dirty.size(); i++)
		{
			if (dirty.get(i))
			{
				message.append(Tab.valueOf(i));

				if (i < dirty.size() - 2)
				{
					message.append(", ");
				}
			}
		}

		status.setText(message.toString());
	}

	/*-------------------------------------------------------------------------*/
	public void clearDirty(int tab)
	{
		dirty.clear(tab);
		setDirtyStatusMessage();
	}

	/*-------------------------------------------------------------------------*/
	public boolean isDirty(int tab)
	{
		return dirty.get(tab);
	}

	/*-------------------------------------------------------------------------*/
	public void saveAllChanges() throws Exception
	{
		commitAll();

		// save all changes to the database
		Database.getInstance().saveAll();

		// update foreign keys
		for (EditorPanel editor : editorPanels)
		{
			editor.initForeignKeys();

			// that will have reset all the combo boxes, so refresh the view
			editor.refresh(editor.getCurrentName());
		}
	}

	/*-------------------------------------------------------------------------*/
	public void discardChanges() throws Exception
	{
		// load everything from disk again
		Database.getInstance().loadAll();

		// reload editor panels
		reloadAll();
	}

	/*-------------------------------------------------------------------------*/
	private void commitAll()
	{
		for (EditorPanel editor : editorPanels)
		{
			editor.commit(editor.getCurrentName());
		}
	}

	/*-------------------------------------------------------------------------*/
	private void reloadAll()
	{
		for (EditorPanel editor : editorPanels)
		{
			editor.reload();
		}
	}

	/*-------------------------------------------------------------------------*/
	private void clearDirtyStatus()
	{
		dirty.clear();
		setDirtyStatusMessage();
	}

	/*-------------------------------------------------------------------------*/
	private void exit()
	{
		if (dirty.isEmpty())
		{
			// don't ask, just exit
			System.exit(0);
		}

		int option = JOptionPane.showConfirmDialog(
			this,
			StringUtils.getUiString("ui.exit.without.saving"),
			StringUtils.getUiString("ui.exit"),
			JOptionPane.YES_NO_OPTION);
		if (option == JOptionPane.YES_OPTION)
		{
			System.exit(0);
		}
	}

	/*-------------------------------------------------------------------------*/
	public EditorPanel getEditorPanel()
	{
		return (EditorPanel)brewingDataTabs.getSelectedComponent();
	}

	/*-------------------------------------------------------------------------*/
	public String getVersion()
	{
		return Brewday.getInstance().getAppConfig().getProperty("mclachlan.brewday.version");
	}

	/*-------------------------------------------------------------------------*/
	public void refreshEditorPanel(EditorPanel other)
	{
		for (EditorPanel p : editorPanels)
		{
			if (p.getClass() == other.getClass())
			{
				p.refreshNames(p.getCurrentName());
			}
		}
	}

	public void windowOpened(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		exit();
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

	/*-------------------------------------------------------------------------*/
	public static class Tab
	{
		public static final int REF_HOPS = 1;
		public static final int REF_FERMENTABLES = 2;
		public static final int REF_YEASTS = 3;
		public static final int REF_MISCS = 4;
		public static final int REF_WATERS = 5;
		public static final int RECIPES = 6;
		public static final int PROCESS_TEMPLATES = 7;
		public static final int EQUIPMENT_PROFILES = 8;
		public static final int REF_STYLES = 9;
		public static final int INVENTORY = 10;
		public static final int SETTINGS = 11;
		public static final int BATCHES = 12;
		// todo

		public static String valueOf(int tab)
		{
			switch (tab)
			{
				case REF_HOPS:
					return StringUtils.getUiString("tab.hops");
				case REF_FERMENTABLES:
					return StringUtils.getUiString("tab.fermentables");
				case REF_YEASTS:
					return StringUtils.getUiString("tab.yeast");
				case REF_MISCS:
					return StringUtils.getUiString("tab.misc");
				case REF_WATERS:
					return StringUtils.getUiString("tab.water");
				case REF_STYLES:
					return StringUtils.getUiString("tab.styles");
				case RECIPES:
					return StringUtils.getUiString("tab.recipes");
				case PROCESS_TEMPLATES:
					return StringUtils.getUiString("tab.process.templates");
				case EQUIPMENT_PROFILES:
					return StringUtils.getUiString("tab.equipment.profiles");
				case INVENTORY:
					return StringUtils.getUiString("tab.inventory");
				case SETTINGS:
					return StringUtils.getUiString("tab.settings");
				case BATCHES:
					return StringUtils.getUiString("tab.batches");
				default:
					throw new BrewdayException("invalid tab " + tab);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public class EditingControls implements ActionListener
	{
		SwingUi parent;

		JMenuBar menuBar;
		JLabel status;
		JButton applyAll, discard, exit, newItem,
			copyItem, renameItem, deleteItem;
		JMenuItem newMenuItem, copyMenuItem, renameMenuItem, deleteMenuItem,
			applyAllMenuItem, discardMenuItem, exitMenuItem,
			changeCampaignMenuItem, aboutMenuItem;

		/*----------------------------------------------------------------------*/
		public EditingControls(SwingUi parent)
		{
			this.parent = parent;
		}

		/*----------------------------------------------------------------------*/
		public JPanel getBottomPanel()
		{
			applyAll = new JButton(StringUtils.getUiString("editor.apply.all"), saveIcon);
			applyAll.addActionListener(this);
			applyAll.setMnemonic(KeyEvent.VK_S);

			discard = new JButton(StringUtils.getUiString("editor.discard.all"), undoIcon);
			discard.addActionListener(this);
			discard.setMnemonic(KeyEvent.VK_I);

			exit = new JButton(StringUtils.getUiString("ui.exit"), exitIcon);
			exit.addActionListener(this);
			exit.setMnemonic(KeyEvent.VK_E);

			newItem = new JButton(StringUtils.getUiString("ui.new.item"), newIcon);
			newItem.addActionListener(this);
			newItem.setMnemonic(KeyEvent.VK_N);

			copyItem = new JButton(StringUtils.getUiString("editor.copy"), duplicateIcon);
			copyItem.addActionListener(this);
			copyItem.setMnemonic(KeyEvent.VK_C);

			renameItem = new JButton(StringUtils.getUiString("editor.rename"), renameIcon);
			renameItem.addActionListener(this);
			renameItem.setMnemonic(KeyEvent.VK_R);

			deleteItem = new JButton(StringUtils.getUiString("editor.delete"), removeIcon);
			deleteItem.addActionListener(this);
			deleteItem.setMnemonic(KeyEvent.VK_D);

			JPanel bottom = new JPanel(new GridLayout(2, 1));

			status = new JLabel();
			bottom.add(status);

			JPanel buttonPanel = new JPanel(new FlowLayout());
			buttonPanel.add(newItem);
			buttonPanel.add(copyItem);
			buttonPanel.add(renameItem);
			buttonPanel.add(deleteItem);
			buttonPanel.add(applyAll);
			buttonPanel.add(discard);
			buttonPanel.add(exit);
			bottom.add(buttonPanel);
			return bottom;
		}

		/*----------------------------------------------------------------------*/
		public JMenuBar buildMenuBar()
		{
			JMenuBar menuBar = new JMenuBar();
			JMenu fileMenu = new JMenu(StringUtils.getUiString("editor.menu"));
			fileMenu.setMnemonic(KeyEvent.VK_F);
			fileMenu.setIcon(stepIcon);

			newMenuItem = new JMenuItem(StringUtils.getUiString("ui.new.item"), newIcon);
			newMenuItem.addActionListener(this);
			newMenuItem.setMnemonic(KeyEvent.VK_N);

			copyMenuItem = new JMenuItem(StringUtils.getUiString("editor.copy"), duplicateIcon);
			copyMenuItem.addActionListener(this);
			copyMenuItem.setMnemonic(KeyEvent.VK_C);

			renameMenuItem = new JMenuItem(StringUtils.getUiString("editor.rename"), renameIcon);
			renameMenuItem.addActionListener(this);
			renameMenuItem.setMnemonic(KeyEvent.VK_R);

			deleteMenuItem = new JMenuItem(StringUtils.getUiString("editor.delete"), deleteIcon);
			deleteMenuItem.addActionListener(this);
			deleteMenuItem.setMnemonic(KeyEvent.VK_D);

			applyAllMenuItem = new JMenuItem(StringUtils.getUiString("editor.apply.all"), saveIcon);
			applyAllMenuItem.addActionListener(this);
			applyAllMenuItem.setMnemonic(KeyEvent.VK_S);

			discardMenuItem = new JMenuItem(StringUtils.getUiString("editor.discard.all"), undoIcon);
			discardMenuItem.addActionListener(this);
			discardMenuItem.setMnemonic(KeyEvent.VK_I);

			exitMenuItem = new JMenuItem(StringUtils.getUiString("ui.exit"), exitIcon);
			exitMenuItem.addActionListener(this);
			exitMenuItem.setMnemonic(KeyEvent.VK_E);

			fileMenu.add(newMenuItem);
			fileMenu.add(copyMenuItem);
			fileMenu.add(renameMenuItem);
			fileMenu.add(deleteMenuItem);
			fileMenu.addSeparator();
			fileMenu.add(applyAllMenuItem);
			fileMenu.add(discardMenuItem);
			fileMenu.addSeparator();
			fileMenu.add(exitMenuItem);

			JMenu helpMenu = new JMenu(StringUtils.getUiString("ui.help"));
			helpMenu.setMnemonic(KeyEvent.VK_H);
			helpMenu.setIcon(helpIcon);

			aboutMenuItem = new JMenuItem(StringUtils.getUiString("ui.about"), brewdayIcon);
			aboutMenuItem.addActionListener(this);
			aboutMenuItem.setMnemonic(KeyEvent.VK_A);
			helpMenu.add(aboutMenuItem);

			menuBar.add(fileMenu);
			menuBar.add(helpMenu);

			this.menuBar = menuBar;
			return menuBar;
		}

		/*-------------------------------------------------------------------------*/
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == applyAll || e.getSource() == applyAllMenuItem)
			{
				EditorPanel panel = getEditorPanel();
				panel.commit(panel.getCurrentName());

				int option = JOptionPane.showConfirmDialog(
					parent,
					StringUtils.getUiString("editor.apply.all.msg"),
					StringUtils.getUiString("editor.apply.all"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					saveIcon);
				if (option == JOptionPane.YES_OPTION)
				{
					try
					{
						parent.saveAllChanges();
						parent.clearDirtyStatus();
					}
					catch (Exception x)
					{
						throw new BrewdayException(x);
					}
				}
			}
			else if (e.getSource() == discard || e.getSource() == discardMenuItem)
			{
				int option = JOptionPane.showConfirmDialog(
					parent,
					StringUtils.getUiString("editor.discard.all.msg"),
					StringUtils.getUiString("editor.discard.all"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					undoIcon);
				if (option == JOptionPane.YES_OPTION)
				{
					try
					{
						parent.discardChanges();
						parent.clearDirtyStatus();
					}
					catch (Exception e1)
					{
						throw new BrewdayException(e1);
					}
				}
			}
			else if (e.getSource() == exit || e.getSource() == exitMenuItem)
			{
				exit();
			}
			else if (e.getSource() == newItem || e.getSource() == newMenuItem)
			{
				EditorPanel panel = getEditorPanel();
				panel.createNewItem();
			}
			else if (e.getSource() == renameItem || e.getSource() == renameMenuItem)
			{
				EditorPanel panel = getEditorPanel();

				String name = (String)JOptionPane.showInputDialog(
					parent,
					StringUtils.getUiString("editor.rename.msg"),
					StringUtils.getUiString("editor.rename"), JOptionPane.QUESTION_MESSAGE,
					renameIcon, null, panel.getCurrentName());

				if (name != null)
				{
					if (!panel.checkAndConfirmDuplicateOverwrite(name))
					{
						return;
					}

					panel.commit(panel.getCurrentName());
					panel.renameItem(name);
					panel.refreshNames(name);
					panel.refresh(name);
					parent.setDirty(panel.getDirtyFlag());
				}
			}
			else if (e.getSource() == copyItem || e.getSource() == copyMenuItem)
			{
				EditorPanel panel = getEditorPanel();

				String name = (String)JOptionPane.showInputDialog(
					parent, StringUtils.getUiString("editor.copy.msg"),
					StringUtils.getUiString("editor.copy"), JOptionPane.QUESTION_MESSAGE,
					duplicateIcon, null, panel.getCurrentName());

				if (name != null)
				{
					if (!panel.checkAndConfirmDuplicateOverwrite(name))
					{
						return;
					}

					panel.commit(panel.getCurrentName());
					panel.copyItem(name);
					panel.refreshNames(name);
					panel.refresh(name);
					parent.setDirty(panel.getDirtyFlag());
				}
			}
			else if (e.getSource() == deleteItem || e.getSource() == deleteMenuItem)
			{
				EditorPanel panel = getEditorPanel();

				panel.commit(panel.getCurrentName());

				int option = JOptionPane.showConfirmDialog(
					parent,
					StringUtils.getUiString("editor.delete.msg"),
					StringUtils.getUiString("editor.delete"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					deleteIcon);

				if (option == JOptionPane.YES_OPTION)
				{
					panel.deleteItem();
					panel.refreshNames(null);
					parent.setDirty(panel.getDirtyFlag());
				}
			}
			else if (e.getSource() == aboutMenuItem)
			{
				JOptionPane.showMessageDialog(
					parent,
					StringUtils.getUiString("ui.about.msg", getVersion()),
					StringUtils.getUiString("ui.about.title"),
					JOptionPane.INFORMATION_MESSAGE,
					new ImageIcon(SwingUi.this.getIconImage()));
			}
		}

		/*----------------------------------------------------------------------*/
		public EditorPanel getEditorPanel()
		{
			return parent.getEditorPanel();
		}

		/*----------------------------------------------------------------------*/
		public JLabel getStatus()
		{
			return status;
		}

		/*----------------------------------------------------------------------*/
		public void exit()
		{
			parent.exit();
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Functions as a global exception handler
	 */
	class EventQueueProxy extends EventQueue
	{
		protected void dispatchEvent(AWTEvent newEvent)
		{
			try
			{
				super.dispatchEvent(newEvent);
			}
			catch (Throwable t)
			{
				t.printStackTrace();
				String message;

				message = t.getMessage();

				if (message == null || message.length() == 0)
				{
					message = "Fatal: " + t.getClass();
				}

				JOptionPane.showMessageDialog(
					SwingUi.instance,
					message,
					StringUtils.getUiString("ui.error"),
					JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		new SwingUi();
	}
}
