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
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.database.Database;

/**
 *
 */
public class SwingUi extends JFrame implements WindowListener
{
	public static SwingUi instance;
	private RecipesPanel recipesPanel;
	private JLabel status;
	private JTabbedPane tabs, brewingDataTabs, refDatabaseTabs;
	private BitSet dirty = new BitSet();
	private List<EditorPanel> editorPanels = new ArrayList<EditorPanel>();

	private Map<String, String> config;

	// editor panels
	// todo

	/*-------------------------------------------------------------------------*/
	public SwingUi() throws Exception
	{
		WebLookAndFeel.install();

		instance = this;
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.setIconImage(ImageIO.read(new File("img/brewday.png")));

//		config = Launcher.getConfig();
		config = new HashMap<String, String>();

		setTitle("Brewday"); // todo add version

		Database.getInstance().loadAll();

		EditingControls c = new EditingControls(this);
		JMenuBar menuBar = c.buildMenuBar();
		JPanel bottom = c.getBottomPanel();
		status = c.getStatus();

		brewingDataTabs = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
		refDatabaseTabs = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);

		// Brewing tabs
		recipesPanel = getRecipesPanel();
		addTab(brewingDataTabs, "Recipes", recipesPanel);
		addTab(brewingDataTabs, "Batches", new JPanel());
		addTab(brewingDataTabs, "Equipment", new JPanel());
		addTab(brewingDataTabs, "Settings", new JPanel());

		// Ref Database tabs, todo
		addTab(refDatabaseTabs, "Water", getWatersPanel());
		addTab(refDatabaseTabs, "Fermentables", getFermentablesPanel());
		addTab(refDatabaseTabs, "Hops", getHopsPanel());
		addTab(refDatabaseTabs, "Yeast", getYeastPanel());
		addTab(refDatabaseTabs, "Misc Ingredients", getMiscsPanel());
		addTab(refDatabaseTabs, "Styles", new JPanel());

		this.setJMenuBar(menuBar);

		this.setLayout(new BorderLayout(5,5));
		
		tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		
		tabs.add("Brewing", brewingDataTabs);
		tabs.add("Inventory", new JPanel());
		tabs.add("Reference Database", refDatabaseTabs);

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

	private Component getWatersPanel()
	{
		return new WatersPanel(Tab.REF_WATERS);
	}

	private Component getMiscsPanel()
	{
		return new MiscsPanel(Tab.REF_MISCS);
	}

	private Component getYeastPanel()
	{
		return new YeastsPanel(Tab.REF_YEASTS);
	}

	private Component getFermentablesPanel()
	{
		return new FermentablesPanel(Tab.REF_FERMENTABLES);
	}

	private Component getHopsPanel()
	{
		return new HopsPanel(Tab.REF_HOPS);
	}

	private RecipesPanel getRecipesPanel()
	{
		return new RecipesPanel(Tab.RECIPES);
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
			this.recipesPanel.refreshComputedVolumes();
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

		String message = "Dirty: ";
		for (int i=0; i<dirty.size(); i++)
		{
			if (dirty.get(i))
			{
				message += Tab.valueOf(i);

				if (i < dirty.size()-1)
				{
					message += ", ";
				}
			}
		}

		status.setText(message);
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
	public void saveChanges() throws Exception
	{
		commitAll();

		// save dirty changes to the database
		
		// static data
		// todo
		//if (dirty.get(Tab.GENDER)) Database.getInstance().getSaver().saveGenders(Database.getInstance().getGenders());

		// dynamic data
		// todo

		// update foreign keys
		for (EditorPanel editor : editorPanels)
		{
			editor.initForeignKeys();
			// that will have reset all the combo boxes, so refresh the view
			editor.refresh(editor.getCurrentName());
		}
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
			this, "Exit without saving?", "Exit", JOptionPane.YES_NO_OPTION);
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
		public static final int RECIPES = 0;
		public static final int REF_HOPS = 1;
		public static final int REF_FERMENTABLES = 2;
		public static final int REF_YEASTS = 3;
		public static final int REF_MISCS = 4;
		public static final int REF_WATERS = 5;
		// todo

		public static String valueOf(int tab)
		{
			switch (tab)
			{
				case RECIPES: return "Recipes";
				case REF_HOPS: return "Hops Database";
				case REF_FERMENTABLES: return "Fermentables Database";
				case REF_YEASTS: return "Yeasts Database";
				case REF_MISCS: return "Miscs Database";
				case REF_WATERS: return "Waters Database";
				default: throw new BrewdayException("invalid tab "+tab);
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	public static class EditingControls implements ActionListener
	{
		SwingUi parent;

		JMenuBar menuBar;
		JLabel status;
		JButton apply, applyAll, discard, exit, newItem,
			copyItem, renameItem, deleteItem;
		JMenuItem newMenuItem, copyMenuItem, renameMenuItem, deleteMenuItem,
			applyMenuItem, applyAllMenuItem, discardMenuItem, exitMenuItem,
			changeCampaignMenuItem, aboutMenuItem;

		/*----------------------------------------------------------------------*/
		public EditingControls(SwingUi parent)
		{
			this.parent = parent;
		}

		/*----------------------------------------------------------------------*/
		public JPanel getBottomPanel()
		{
			apply = new JButton("Apply");
			apply.addActionListener(this);
			apply.setMnemonic(KeyEvent.VK_A);
			applyAll = new JButton("Apply All");
			applyAll.addActionListener(this);
			discard = new JButton("Discard");
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
			buttonPanel.add(apply);
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
			applyMenuItem = new JMenuItem("Apply");
			applyMenuItem.addActionListener(this);
			applyMenuItem.setMnemonic(KeyEvent.VK_A);
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
			fileMenu.add(applyMenuItem);
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
			if (e.getSource() == apply || e.getSource() == applyMenuItem)
			{
				EditorPanel panel = getEditorPanel();
				panel.commit(panel.getCurrentName());

				int option = JOptionPane.showConfirmDialog(
					parent, "Save changes?", "Apply", JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.YES_OPTION)
				{
					try
					{
						parent.saveChanges();
					}
					catch (Exception x)
					{
						throw new BrewdayException(x);
					}
					parent.clearDirtyStatus();
				}
			}
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
				String name = JOptionPane.showInputDialog(
					parent, "Name:", "New Item", JOptionPane.QUESTION_MESSAGE);

				if (name != null)
				{
					EditorPanel panel = getEditorPanel();
					if (panel.getCurrentName() != null)
					{
						panel.commit(panel.getCurrentName());
					}
					panel.newItem(name);
					panel.refreshNames(name);
					panel.refresh(name);
					parent.setDirty(panel.getDirtyFlag());
				}
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
					JOptionPane.INFORMATION_MESSAGE);
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
