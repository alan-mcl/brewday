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

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;

/**
 *
 */
public class SwingUi extends JFrame implements WindowListener
{
	public static SwingUi instance;

	public static ImageIcon appIcon;
	public static ImageIcon grainsIcon;
	public static ImageIcon hopsIcon;
	public static ImageIcon waterIcon;
	public static ImageIcon stepIcon;
	public static ImageIcon recipeIcon;
	public static ImageIcon yeastIcon;
	public static ImageIcon miscIcon;
	public static ImageIcon removeIcon;
	public static ImageIcon increaseIcon;
	public static ImageIcon decreaseIcon;
	public static ImageIcon moreTimeIcon;
	public static ImageIcon lessTimeIcon;
	public static ImageIcon searchIcon;
	public static ImageIcon editIcon;
	public static ImageIcon newIcon;
	public static ImageIcon deleteIcon;
	public static ImageIcon duplicateIcon;
	public static ImageIcon substituteIcon;

	private RecipesPanel recipesPanel;
	private BatchesPanel batchesPanel;
	private ProcessTemplatePanel processTemplatePanel;
	private EquipmentProfilePanel equipmentProfilePanel;
	private InventoryPanel inventoryPanel;
	private SettingsPanel settingsPanel;
	private JLabel status;
	private JTabbedPane tabs, brewingDataTabs, refDatabaseTabs;
	private BitSet dirty = new BitSet();
	private List<EditorPanel> editorPanels = new ArrayList<EditorPanel>();

	/*-------------------------------------------------------------------------*/
	public SwingUi() throws Exception
	{
//		WebLookAndFeel.install();

		UIManager.setLookAndFeel(new Plastic3DLookAndFeel());

		instance = this;
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		appIcon = SwingUi.createImageIcon("img/brewday.png");
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

		this.setIconImage(appIcon.getImage());

		// how about we not localise this bit
		setTitle("Brewday"); // todo add version

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

		addTab(brewingDataTabs, StringUtils.getUiString("tab.batches"), batchesPanel);
		addTab(brewingDataTabs, StringUtils.getUiString("tab.recipes"), recipesPanel);
		addTab(brewingDataTabs, StringUtils.getUiString("tab.process.templates"), processTemplatePanel);
		addTab(brewingDataTabs, StringUtils.getUiString("tab.equipment.profiles"), equipmentProfilePanel);

		// Ref Database tabs
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.water"), getWatersPanel());
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.fermentables"), getFermentablesPanel());
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.hops"), getHopsPanel());
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.yeast"), getYeastPanel());
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.misc"), getMiscsPanel());
		addTab(refDatabaseTabs, StringUtils.getUiString("tab.styles"), getStylesPanel());

		this.setJMenuBar(menuBar);

		this.setLayout(new BorderLayout(5,5));
		
		tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

		inventoryPanel = new InventoryPanel(Tab.INVENTORY);
		inventoryPanel.refresh();

		settingsPanel = new SettingsPanel(Tab.SETTINGS);
		settingsPanel.refresh();

		tabs.add(StringUtils.getUiString("tab.brewing"), brewingDataTabs);
		tabs.add(StringUtils.getUiString("tab.inventory"), inventoryPanel);
		tabs.add(StringUtils.getUiString("tab.reference.database"), refDatabaseTabs);
		tabs.add(StringUtils.getUiString("tab.settings"), settingsPanel);

		this.add(tabs, BorderLayout.CENTER);
		this.add(bottom, BorderLayout.SOUTH);

		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)(d.getWidth()/2);
		int centerY = (int)(d.getHeight()/2);
		int width = (int)(d.getWidth()-20);
		int height = (int)(d.getHeight()-20);

		addWindowListener(this);
		this.setBounds(centerX-width/2, centerY-height/2, width, height);
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
	private void addTab(JTabbedPane parent, String title, Component panel)
	{
		parent.addTab(title, panel);
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
		for (int i=0; i<dirty.size(); i++)
		{
			if (dirty.get(i))
			{
				message.append(Tab.valueOf(i));

				if (i < dirty.size()-1)
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
				case REF_HOPS: return StringUtils.getUiString("tab.hops");
				case REF_FERMENTABLES: return StringUtils.getUiString("tab.fermentables");
				case REF_YEASTS: return StringUtils.getUiString("tab.yeast");
				case REF_MISCS: return StringUtils.getUiString("tab.misc");
				case REF_WATERS: return StringUtils.getUiString("tab.water");
				case REF_STYLES: return StringUtils.getUiString("tab.styles");
				case RECIPES: return StringUtils.getUiString("tab.recipes");
				case PROCESS_TEMPLATES: return StringUtils.getUiString("tab.process.templates");
				case EQUIPMENT_PROFILES: return StringUtils.getUiString("tab.equipment.profiles");
				case INVENTORY: return StringUtils.getUiString("tab.inventory");
				case SETTINGS: return StringUtils.getUiString("tab.settings");
				case BATCHES: return StringUtils.getUiString("tab.batches");
				default: throw new BrewdayException("invalid tab "+tab);
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
			applyAll = new JButton("Save All");
			applyAll.addActionListener(this);
			discard = new JButton("Undo All");
			discard.addActionListener(this);
			discard.setMnemonic(KeyEvent.VK_I);
			exit = new JButton("Exit");
			exit.addActionListener(this);
			exit.setMnemonic(KeyEvent.VK_E);

			newItem = new JButton("New");
			newItem.addActionListener(this);
			newItem.setMnemonic(KeyEvent.VK_N);
			copyItem = new JButton("Copy");
			copyItem.addActionListener(this);
			copyItem.setMnemonic(KeyEvent.VK_C);
			renameItem = new JButton("Rename");
			renameItem.addActionListener(this);
			renameItem.setMnemonic(KeyEvent.VK_R);
			deleteItem = new JButton("Delete");
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
			JMenu fileMenu = new JMenu("File");
			fileMenu.setMnemonic(KeyEvent.VK_F);
			newMenuItem = new JMenuItem("New...");
			newMenuItem.addActionListener(this);
			newMenuItem.setMnemonic(KeyEvent.VK_N);
			copyMenuItem = new JMenuItem("Copy...");
			copyMenuItem.addActionListener(this);
			copyMenuItem.setMnemonic(KeyEvent.VK_C);
			renameMenuItem = new JMenuItem("Rename...");
			renameMenuItem.addActionListener(this);
			renameMenuItem.setMnemonic(KeyEvent.VK_R);
			deleteMenuItem = new JMenuItem("Delete");
			deleteMenuItem.addActionListener(this);
			deleteMenuItem.setMnemonic(KeyEvent.VK_D);
			applyAllMenuItem = new JMenuItem("Apply All");
			applyAllMenuItem.addActionListener(this);
			discardMenuItem = new JMenuItem("Discard");
			discardMenuItem.addActionListener(this);
			discardMenuItem.setMnemonic(KeyEvent.VK_I);
			exitMenuItem = new JMenuItem("Exit");
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

			JMenu helpMenu = new JMenu("Help");
			helpMenu.setMnemonic(KeyEvent.VK_H);
			aboutMenuItem = new JMenuItem("About");
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
					parent, "Save ALL files?", "Apply", JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.YES_OPTION)
				{
					try
					{
						parent.saveAllChanges();
					}
					catch (Exception x)
					{
						throw new BrewdayException(x);
					}
					parent.clearDirtyStatus();
				}
			}
			else if (e.getSource() == discard || e.getSource() == discardMenuItem)
			{
				int option = JOptionPane.showConfirmDialog(
					parent, "Discard all changes?", "Discard", JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.YES_OPTION)
				{
					try
					{
						parent.discardChanges();
					}
					catch (Exception e1)
					{
						throw new BrewdayException(e1);
					}
					parent.clearDirtyStatus();
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
					parent, "New Name:", "Rename Item", JOptionPane.QUESTION_MESSAGE,
					null, null, panel.getCurrentName());

				if (name != null)
				{
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
					parent, "New Name:", "Copy Item", JOptionPane.QUESTION_MESSAGE,
					null, null, panel.getCurrentName());

				if (name != null)
				{
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
					parent, "Are you sure?", "Delete Item", JOptionPane.YES_NO_OPTION);

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
					"Brewday\n" +
						"version DEV\n\n", // todo version
					"Brewday",
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
	public static void main(String[] args) throws Exception
	{
		new SwingUi();
	}
}
