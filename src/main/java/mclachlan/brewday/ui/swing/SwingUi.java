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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Main class for the Swing UI. Equivalent to JfxUi in the JavaFX implementation.
 */
public class SwingUi extends JFrame implements TrackDirty
{
	/** Singleton instance */
	private static SwingUi instance;
	
	/** The application database */
	private Database database;
	
	/** Dirty objects that need to be saved */
	private Set<Object> dirtyObjects = new HashSet<>();
	
	/** Flag to enable/disable dirty detection */
	private boolean detectDirty = true;
	
	/** Main content pane */
	private JPanel contentPane;
	
	/** Card panel for content */
	private CardPanel cardPanel;
	
	/** Tree for navigation */
	private JTree navTree;
	
	/** Map of tree nodes to card keys */
	private Map<Object, String> nodeToCardMap = new HashMap<>();
	
	/** Map of card keys to tree nodes */
	private Map<String, DefaultMutableTreeNode> cardToNodeMap = new HashMap<>();
	
	// Constants for card keys
	public static final String RECIPES = "recipes";
	public static final String BATCHES = "batches";
	public static final String INVENTORY = "inventory";
	public static final String REFERENCE_DATA = "referenceData";
	public static final String TOOLS = "tools";
	public static final String ABOUT = "about";
	
	// Constants for parent node card keys
	public static final String BREWING = "brewing";
	public static final String REFERENCE_DATABASE = "referenceDatabase";
	public static final String SETTINGS = "settings";
	public static final String HELP = "help";
	
	// Constants for reference data subcategory card keys
	public static final String WATER = "water";
	public static final String WATER_PARAMETERS = "waterParameters";
	public static final String FERMENTABLES = "fermentables";
	public static final String HOPS = "hops";
	public static final String YEAST = "yeast";
	public static final String MISC = "misc";
	public static final String STYLES = "styles";
	
	// Constants for tools subcategory card keys
	public static final String IMPORT = "import";
	public static final String WATER_BUILDER = "waterBuilder";
	
	// Constants for settings subcategory card keys
	public static final String BREWING_SETTINGS = "brewingSettings";
	public static final String BREWING_SETTINGS_GENERAL = "brewingSettingsGeneral";
	public static final String BREWING_SETTINGS_MASH = "brewingSettingsMash";
	public static final String BREWING_SETTINGS_IBU = "brewingSettingsIbu";
	public static final String BACKEND_SETTINGS = "backendSettings";
	public static final String BACKEND_SETTINGS_LOCAL_FILESYSTEM = "backendSettingsLocalFileSystem";
	public static final String BACKEND_SETTINGS_GIT = "backendSettingsGit";
	public static final String UI_SETTINGS = "uiSettings";
	
	/**
	 * Main entry point for the Swing UI.
	 */
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(() -> {
			try
			{
				// Set look and feel
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
				
				// Create and show the UI
				SwingUi ui = new SwingUi();
				ui.setVisible(true);
			}
catch (Exception e)
{
				e.printStackTrace();
				Brewday.getInstance().getLog().log(e);
			}
		});
	}
	
	/**
	 * Get the singleton instance.
	 */
	public static SwingUi getInstance()
	{
		return instance;
	}
	
	/**
	 * Constructor.
	 */
	public SwingUi()
	{
		super("Brewday");
		instance = this;
		
		// Initialize the database
		database = Database.getInstance();
		database.loadAll();
		
		// Set up the frame
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setSize(1024, 768);
		setLocationRelativeTo(null);
		
		// Handle window closing
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				exitApplication();
			}
		});
		
		// Create the UI
		initUI();
	}
	
	/**
	 * Initialize the UI components.
	 */
	private void initUI()
	{
		// Initialize icons
		SwingIcons.init();
		
		// Create the main content pane
		contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);
		
		// Create the card panel for content
		cardPanel = new CardPanel();
		
		// Create the navigation tree
		navTree = createNavigationTree();
		
		// Create a split pane with the tree on the left and cards on the right
		JSplitPane splitPane = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			new JScrollPane(navTree),
			cardPanel);
		splitPane.setDividerLocation(200);
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		// Add content cards
		addContentCards();
		
		// Add status bar
		JPanel statusBar = createStatusBar();
		contentPane.add(statusBar, BorderLayout.SOUTH);
		
		// Set initial selection
		navTree.setSelectionRow(1); // Select the first visible node
	}
	
	
	/**
	 * Create the status bar.
	 */
	private JPanel createStatusBar()
	{
		JPanel statusBar = new JPanel(new BorderLayout());
		statusBar.setBorder(BorderFactory.createEtchedBorder());
		
		JLabel statusLabel = new JLabel(" Ready");
		statusBar.add(statusLabel, BorderLayout.WEST);
		
		return statusBar;
	}
	
	/**
	 * Create the navigation tree.
	 */
	private JTree createNavigationTree()
	{
		// Create the root node (not visible)
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
		
		// Create main category nodes
		DefaultMutableTreeNode brewing = new DefaultMutableTreeNode(getUiString("tab.brewing"));
		DefaultMutableTreeNode inventory = new DefaultMutableTreeNode(getUiString("tab.inventory"));
		DefaultMutableTreeNode referenceData = new DefaultMutableTreeNode(getUiString("tab.reference.database"));
		DefaultMutableTreeNode tools = new DefaultMutableTreeNode(getUiString("tab.tools"));
		DefaultMutableTreeNode settings = new DefaultMutableTreeNode(getUiString("tab.settings"));
		DefaultMutableTreeNode help = new DefaultMutableTreeNode(getUiString("ui.help"));
		
		// Add main categories to root
		root.add(brewing);
		root.add(inventory);
		root.add(referenceData);
		root.add(tools);
		root.add(settings);
		root.add(help);
		
		// Add brewing subcategories
		DefaultMutableTreeNode recipes = new DefaultMutableTreeNode(getUiString("ui.recipes"));
		DefaultMutableTreeNode batches = new DefaultMutableTreeNode(getUiString("ui.batches"));
		brewing.add(recipes);
		brewing.add(batches);
		
		// Add reference data subcategories
		DefaultMutableTreeNode water = new DefaultMutableTreeNode(getUiString("tab.water"));
		DefaultMutableTreeNode waterParameters = new DefaultMutableTreeNode(getUiString("tab.water.parameters"));
		DefaultMutableTreeNode fermentables = new DefaultMutableTreeNode(getUiString("tab.fermentables"));
		DefaultMutableTreeNode hops = new DefaultMutableTreeNode(getUiString("tab.hops"));
		DefaultMutableTreeNode yeast = new DefaultMutableTreeNode(getUiString("tab.yeast"));
		DefaultMutableTreeNode misc = new DefaultMutableTreeNode(getUiString("tab.misc"));
		DefaultMutableTreeNode styles = new DefaultMutableTreeNode(getUiString("tab.styles"));
		referenceData.add(water);
		referenceData.add(waterParameters);
		referenceData.add(fermentables);
		referenceData.add(hops);
		referenceData.add(yeast);
		referenceData.add(misc);
		referenceData.add(styles);
		
		// Add tools subcategories
		DefaultMutableTreeNode importNode = new DefaultMutableTreeNode(getUiString("tools.import"));
		DefaultMutableTreeNode waterBuilder = new DefaultMutableTreeNode(getUiString("tools.water.builder"));
		tools.add(importNode);
		tools.add(waterBuilder);
		
		// Add settings subcategories
		DefaultMutableTreeNode brewingSettings = new DefaultMutableTreeNode(getUiString("settings.brewing"));
		DefaultMutableTreeNode backendSettings = new DefaultMutableTreeNode(getUiString("settings.backend"));
		DefaultMutableTreeNode uiSettings = new DefaultMutableTreeNode(getUiString("settings.ui"));
		
		// Add brewing settings subcategories
		DefaultMutableTreeNode brewingSettingsGeneral = new DefaultMutableTreeNode(getUiString("settings.brewing.general"));
		DefaultMutableTreeNode brewingSettingsMash = new DefaultMutableTreeNode(getUiString("settings.brewing.mash"));
		DefaultMutableTreeNode brewingSettingsIbu = new DefaultMutableTreeNode(getUiString("settings.brewing.ibu"));
		brewingSettings.add(brewingSettingsGeneral);
		brewingSettings.add(brewingSettingsMash);
		brewingSettings.add(brewingSettingsIbu);
		
		// Add backend settings subcategories
		DefaultMutableTreeNode backendLocalFileSystem = new DefaultMutableTreeNode(getUiString("settings.backend.local.filesystem"));
		DefaultMutableTreeNode backendGit = new DefaultMutableTreeNode(getUiString("settings.backend.git"));
		backendSettings.add(backendLocalFileSystem);
		backendSettings.add(backendGit);
		
		// Add settings subcategories to settings node
		settings.add(brewingSettings);
		// Only add backend settings if the feature toggle is on
		if (Database.getInstance().getSettings().isFeatureOn(Settings.FEATURE_TOGGLE_REMOTE_BACKENDS))
		{
			settings.add(backendSettings);
		}
		settings.add(uiSettings);
		
		// Add help subcategories
		DefaultMutableTreeNode about = new DefaultMutableTreeNode(getUiString("ui.about"));
		help.add(about);
		
		// Create the tree with the root node
		JTree tree = new JTree(root);
		
		// Hide the root node
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		
		// Set the custom cell renderer for icons
		tree.setCellRenderer(new IconTreeCellRenderer());
		
		// Set up selection handling
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(e -> {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
			if (node == null) return;
			
			String cardKey = nodeToCardMap.get(node);
			if (cardKey != null)
			{
				cardPanel.setVisible(cardKey);
			}
		});
		
		// Map nodes to card keys
		nodeToCardMap.put(brewing, BREWING);
		nodeToCardMap.put(recipes, RECIPES);
		nodeToCardMap.put(batches, BATCHES);
		nodeToCardMap.put(inventory, INVENTORY);
		nodeToCardMap.put(referenceData, REFERENCE_DATABASE);
		nodeToCardMap.put(water, WATER);
		nodeToCardMap.put(waterParameters, WATER_PARAMETERS);
		nodeToCardMap.put(fermentables, FERMENTABLES);
		nodeToCardMap.put(hops, HOPS);
		nodeToCardMap.put(yeast, YEAST);
		nodeToCardMap.put(misc, MISC);
		nodeToCardMap.put(styles, STYLES);
		nodeToCardMap.put(tools, TOOLS);
		nodeToCardMap.put(importNode, IMPORT);
		nodeToCardMap.put(waterBuilder, WATER_BUILDER);
		nodeToCardMap.put(settings, SETTINGS);
		nodeToCardMap.put(brewingSettings, BREWING_SETTINGS);
		nodeToCardMap.put(brewingSettingsGeneral, BREWING_SETTINGS_GENERAL);
		nodeToCardMap.put(brewingSettingsMash, BREWING_SETTINGS_MASH);
		nodeToCardMap.put(brewingSettingsIbu, BREWING_SETTINGS_IBU);
		nodeToCardMap.put(backendSettings, BACKEND_SETTINGS);
		nodeToCardMap.put(backendLocalFileSystem, BACKEND_SETTINGS_LOCAL_FILESYSTEM);
		nodeToCardMap.put(backendGit, BACKEND_SETTINGS_GIT);
		nodeToCardMap.put(uiSettings, UI_SETTINGS);
		nodeToCardMap.put(help, HELP);
		nodeToCardMap.put(about, ABOUT);
		
		// Map card keys to nodes
		cardToNodeMap.put(BREWING, brewing);
		cardToNodeMap.put(RECIPES, recipes);
		cardToNodeMap.put(BATCHES, batches);
		cardToNodeMap.put(INVENTORY, inventory);
		cardToNodeMap.put(REFERENCE_DATABASE, referenceData);
		cardToNodeMap.put(WATER, water);
		cardToNodeMap.put(WATER_PARAMETERS, waterParameters);
		cardToNodeMap.put(FERMENTABLES, fermentables);
		cardToNodeMap.put(HOPS, hops);
		cardToNodeMap.put(YEAST, yeast);
		cardToNodeMap.put(MISC, misc);
		cardToNodeMap.put(STYLES, styles);
		cardToNodeMap.put(TOOLS, tools);
		cardToNodeMap.put(IMPORT, importNode);
		cardToNodeMap.put(WATER_BUILDER, waterBuilder);
		cardToNodeMap.put(SETTINGS, settings);
		cardToNodeMap.put(BREWING_SETTINGS, brewingSettings);
		cardToNodeMap.put(BREWING_SETTINGS_GENERAL, brewingSettingsGeneral);
		cardToNodeMap.put(BREWING_SETTINGS_MASH, brewingSettingsMash);
		cardToNodeMap.put(BREWING_SETTINGS_IBU, brewingSettingsIbu);
		cardToNodeMap.put(BACKEND_SETTINGS, backendSettings);
		cardToNodeMap.put(BACKEND_SETTINGS_LOCAL_FILESYSTEM, backendLocalFileSystem);
		cardToNodeMap.put(BACKEND_SETTINGS_GIT, backendGit);
		cardToNodeMap.put(UI_SETTINGS, uiSettings);
		cardToNodeMap.put(HELP, help);
		cardToNodeMap.put(ABOUT, about);
		
		// Expand the brewing node by default
		tree.expandPath(new TreePath(brewing.getPath()));
		
		return tree;
	}
	
	/**
	 * Add content cards to the card panel.
	 */
	private void addContentCards()
	{
		// Create button navigation panels for parent nodes
		createBrewingPanel();
		createReferenceDataPanel();
		createToolsPanel();
		createSettingsPanel();
		createHelpPanel();
		
		// Create content panels for leaf nodes
		
		// Recipes card
		JPanel recipesPanel = new JPanel();
		recipesPanel.add(new JLabel("Recipes - Not Implemented"));
		cardPanel.addCard(RECIPES, recipesPanel);
		
		// Batches card
		JPanel batchesPanel = new JPanel();
		batchesPanel.add(new JLabel("Batches - Not Implemented"));
		cardPanel.addCard(BATCHES, batchesPanel);
		
		// Inventory card
		SwingInventoryPanel inventoryPanel = new SwingInventoryPanel(this);
		cardPanel.addCard(INVENTORY, inventoryPanel);
		
		// Reference Data placeholder card (should never be shown directly)
		JPanel referencePanel = new JPanel();
		referencePanel.add(new JLabel("Reference Data - Not Implemented"));
		cardPanel.addCard(REFERENCE_DATA, referencePanel);
		
		// Add cards for reference data subcategories
		JPanel waterPanel = new JPanel();
		waterPanel.add(new JLabel("Water - Not Implemented"));
		cardPanel.addCard(WATER, waterPanel);
		
		JPanel waterParamsPanel = new JPanel();
		waterParamsPanel.add(new JLabel("Water Parameters - Not Implemented"));
		cardPanel.addCard(WATER_PARAMETERS, waterParamsPanel);
		
		JPanel fermentablesPanel = new JPanel();
		fermentablesPanel.add(new JLabel("Fermentables - Not Implemented"));
		cardPanel.addCard(FERMENTABLES, fermentablesPanel);
		
		JPanel hopsPanel = new JPanel();
		hopsPanel.add(new JLabel("Hops - Not Implemented"));
		cardPanel.addCard(HOPS, hopsPanel);
		
		JPanel yeastPanel = new JPanel();
		yeastPanel.add(new JLabel("Yeast - Not Implemented"));
		cardPanel.addCard(YEAST, yeastPanel);
		
		JPanel miscPanel = new JPanel();
		miscPanel.add(new JLabel("Misc - Not Implemented"));
		cardPanel.addCard(MISC, miscPanel);
		
		JPanel stylesPanel = new JPanel();
		stylesPanel.add(new JLabel("Styles - Not Implemented"));
		cardPanel.addCard(STYLES, stylesPanel);
		
		// Tools panel is created by createToolsPanel()
		
		// Add cards for tools subcategories
		JPanel importPanel = new JPanel();
		importPanel.add(new JLabel("Import - Not Implemented"));
		cardPanel.addCard(IMPORT, importPanel);
		
		JPanel waterBuilderPanel = new JPanel();
		waterBuilderPanel.add(new JLabel("Water Builder - Not Implemented"));
		cardPanel.addCard(WATER_BUILDER, waterBuilderPanel);
		
		// Settings panels
		// Brewing Settings subcategory panels
		JPanel brewingSettingsGeneralPanel = new JPanel();
		brewingSettingsGeneralPanel.add(new JLabel("Brewing Settings - General - Not Implemented"));
		cardPanel.addCard(BREWING_SETTINGS_GENERAL, brewingSettingsGeneralPanel);
		
		JPanel brewingSettingsMashPanel = new JPanel();
		brewingSettingsMashPanel.add(new JLabel("Brewing Settings - Mash - Not Implemented"));
		cardPanel.addCard(BREWING_SETTINGS_MASH, brewingSettingsMashPanel);
		
		JPanel brewingSettingsIbuPanel = new JPanel();
		brewingSettingsIbuPanel.add(new JLabel("Brewing Settings - IBU - Not Implemented"));
		cardPanel.addCard(BREWING_SETTINGS_IBU, brewingSettingsIbuPanel);
		
		// Backend Settings subcategory panels
		JPanel backendSettingsLocalFilesystemPanel = new JPanel();
		backendSettingsLocalFilesystemPanel.add(new JLabel("Backend Settings - Local Filesystem - Not Implemented"));
		cardPanel.addCard(BACKEND_SETTINGS_LOCAL_FILESYSTEM, backendSettingsLocalFilesystemPanel);
		
		JPanel backendSettingsGitPanel = new JPanel();
		backendSettingsGitPanel.add(new JLabel("Backend Settings - Git - Not Implemented"));
		cardPanel.addCard(BACKEND_SETTINGS_GIT, backendSettingsGitPanel);
		
		// UI Settings panel
		JPanel uiSettingsPanel = new JPanel();
		uiSettingsPanel.add(new JLabel("UI Settings - Not Implemented"));
		cardPanel.addCard(UI_SETTINGS, uiSettingsPanel);
		
		// About card
		JPanel aboutPanel = createAboutPanel();
		cardPanel.addCard(ABOUT, aboutPanel);
	}
	
	/**
	 * Create the brewing panel with buttons for its child nodes.
	 */
	private void createBrewingPanel()
	{
		DefaultMutableTreeNode brewingNode = cardToNodeMap.get(BREWING);
		java.util.List<DefaultMutableTreeNode> childNodes = new ArrayList<>();
		
		// Add all child nodes
		for (int i = 0; i < brewingNode.getChildCount(); i++)
		{
			childNodes.add((DefaultMutableTreeNode)brewingNode.getChildAt(i));
		}
		
		// Create the button panel
		ButtonNavigationPanel panel = new ButtonNavigationPanel(
			getUiString("ui.button.navigation.brewing"),
			childNodes,
			node -> {
				// Select the node in the tree when a button is clicked
				TreePath path = new TreePath(node.getPath());
				navTree.setSelectionPath(path);
			}
		);
		
		cardPanel.addCard(BREWING, panel);
	}
	
	/**
	 * Create the reference data panel with buttons for its child nodes.
	 */
	private void createReferenceDataPanel()
	{
		DefaultMutableTreeNode refNode = cardToNodeMap.get(REFERENCE_DATABASE);
		java.util.List<DefaultMutableTreeNode> childNodes = new ArrayList<>();
		
		// Add all child nodes
		for (int i = 0; i < refNode.getChildCount(); i++)
		{
			childNodes.add((DefaultMutableTreeNode)refNode.getChildAt(i));
		}
		
		// Create the button panel
		ButtonNavigationPanel panel = new ButtonNavigationPanel(
			getUiString("ui.button.navigation.reference"),
			childNodes,
			node -> {
				// Select the node in the tree when a button is clicked
				TreePath path = new TreePath(node.getPath());
				navTree.setSelectionPath(path);
			}
		);
		
		cardPanel.addCard(REFERENCE_DATABASE, panel);
	}
	
	/**
	 * Create the tools panel with buttons for its child nodes.
	 */
	private void createToolsPanel()
	{
		DefaultMutableTreeNode toolsNode = cardToNodeMap.get(TOOLS);
		java.util.List<DefaultMutableTreeNode> childNodes = new ArrayList<>();
		
		// Add all child nodes
		for (int i = 0; i < toolsNode.getChildCount(); i++)
		{
			childNodes.add((DefaultMutableTreeNode)toolsNode.getChildAt(i));
		}
		
		// Create the button panel
		ButtonNavigationPanel panel = new ButtonNavigationPanel(
			getUiString("ui.button.navigation.tools"),
			childNodes,
			node -> {
				// Select the node in the tree when a button is clicked
				TreePath path = new TreePath(node.getPath());
				navTree.setSelectionPath(path);
			}
		);
		
		cardPanel.addCard(TOOLS, panel);
	}
	
	/**
	 * Create the settings panel with buttons for its child nodes.
	 */
	private void createSettingsPanel()
	{
		DefaultMutableTreeNode settingsNode = cardToNodeMap.get(SETTINGS);
		java.util.List<DefaultMutableTreeNode> childNodes = new ArrayList<>();
		
		// Add all child nodes
		for (int i = 0; i < settingsNode.getChildCount(); i++)
		{
			childNodes.add((DefaultMutableTreeNode)settingsNode.getChildAt(i));
		}
		
		// Create the button panel
		ButtonNavigationPanel panel = new ButtonNavigationPanel(
			getUiString("ui.button.navigation.settings"),
			childNodes,
			node -> {
				// Select the node in the tree when a button is clicked
				TreePath path = new TreePath(node.getPath());
				navTree.setSelectionPath(path);
			}
		);
		
		cardPanel.addCard(SETTINGS, panel);
		
		// Create button panels for settings subcategories that have children
		createBrewingSettingsPanel();
		createBackendSettingsPanel();
	}
	
	/**
	 * Create the brewing settings panel with buttons for its child nodes.
	 */
	private void createBrewingSettingsPanel()
	{
		DefaultMutableTreeNode brewingSettingsNode = cardToNodeMap.get(BREWING_SETTINGS);
		java.util.List<DefaultMutableTreeNode> childNodes = new ArrayList<>();
		
		// Add all child nodes
		for (int i = 0; i < brewingSettingsNode.getChildCount(); i++)
		{
			childNodes.add((DefaultMutableTreeNode)brewingSettingsNode.getChildAt(i));
		}
		
		// Create the button panel
		ButtonNavigationPanel panel = new ButtonNavigationPanel(
			getUiString("settings.brewing"),
			childNodes,
			node -> {
				// Select the node in the tree when a button is clicked
				TreePath path = new TreePath(node.getPath());
				navTree.setSelectionPath(path);
			}
		);
		
		cardPanel.addCard(BREWING_SETTINGS, panel);
	}
	
	/**
	 * Create the backend settings panel with buttons for its child nodes.
	 */
	private void createBackendSettingsPanel()
	{
		DefaultMutableTreeNode backendSettingsNode = cardToNodeMap.get(BACKEND_SETTINGS);
		java.util.List<DefaultMutableTreeNode> childNodes = new ArrayList<>();
		
		// Add all child nodes
		for (int i = 0; i < backendSettingsNode.getChildCount(); i++)
		{
			childNodes.add((DefaultMutableTreeNode)backendSettingsNode.getChildAt(i));
		}
		
		// Create the button panel
		ButtonNavigationPanel panel = new ButtonNavigationPanel(
			getUiString("settings.backend"),
			childNodes,
			node -> {
				// Select the node in the tree when a button is clicked
				TreePath path = new TreePath(node.getPath());
				navTree.setSelectionPath(path);
			}
		);
		
		cardPanel.addCard(BACKEND_SETTINGS, panel);
	}
	
	/**
	 * Create the help panel with buttons for its child nodes.
	 */
	private void createHelpPanel()
	{
		DefaultMutableTreeNode helpNode = cardToNodeMap.get(HELP);
		java.util.List<DefaultMutableTreeNode> childNodes = new ArrayList<>();
		
		// Add all child nodes
		for (int i = 0; i < helpNode.getChildCount(); i++)
		{
			childNodes.add((DefaultMutableTreeNode)helpNode.getChildAt(i));
		}
		
		// Create the button panel
		ButtonNavigationPanel panel = new ButtonNavigationPanel(
			getUiString("ui.button.navigation.help"),
			childNodes,
			node -> {
				// Select the node in the tree when a button is clicked
				TreePath path = new TreePath(node.getPath());
				navTree.setSelectionPath(path);
			}
		);
		
		cardPanel.addCard(HELP, panel);
	}
	
	/**
	 * Show the preferences dialog.
	 */
	private void showPreferences()
	{
		JOptionPane.showMessageDialog(this, 
			getUiString("ui.swing.preferences.message"), 
			getUiString("ui.swing.preferences.title"), 
			JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Show the about dialog.
	 */
	private void showAboutDialog()
	{
		JOptionPane.showMessageDialog(this,
			getUiString("ui.swing.about.message", mclachlan.brewday.ui.UiUtils.getVersion()),
			getUiString("ui.swing.about.title"),
			JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Create the about panel.
	 */
	private JPanel createAboutPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(10, 10));
		
		// Create a panel for the content with some padding
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		// Add title
		JLabel titleLabel = new JLabel(getUiString("ui.swing.about.title"));
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18));
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPanel.add(titleLabel);
		
		// Add some spacing
		contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
		
		// Add version info
		JLabel versionLabel = new JLabel(getUiString("ui.swing.about.message", 
			mclachlan.brewday.ui.UiUtils.getVersion()));
		versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPanel.add(versionLabel);
		
		// Add some spacing
		contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		
		// Add database location
		JLabel dbLabel = new JLabel(getUiString("ui.about.db", 
			Database.getInstance().getLocalStorageDirectory().getAbsolutePath()));
		dbLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPanel.add(dbLabel);
		
		// Add some spacing
		contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		
		// Add log location
		JLabel logLabel = new JLabel(getUiString("ui.about.log", 
			Brewday.getInstance().getLog().getLogPath()));
		logLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPanel.add(logLabel);
		
		// Add the content panel to the main panel
		panel.add(contentPanel, BorderLayout.CENTER);
		
		return panel;
	}
	
	/**
	 * Exit the application, prompting to save if there are unsaved changes.
	 */
	private void exitApplication()
	{
		if (!dirtyObjects.isEmpty())
		{
			int result = JOptionPane.showConfirmDialog(this,
				getUiString("ui.save.changes.prompt"),
				getUiString("ui.save.changes"),
				JOptionPane.YES_NO_CANCEL_OPTION);
				
			if (result == JOptionPane.YES_OPTION)
			{
				// Save changes
				saveChanges();
				dispose();
				System.exit(0);
			}
			else if (result == JOptionPane.NO_OPTION)
			{
				// Discard changes
				dispose();
				System.exit(0);
			}
			// CANCEL_OPTION - do nothing
		}
else
{
			// No changes to save
			dispose();
			System.exit(0);
		}
	}
	
	/**
	 * Save all changes.
	 */
	private void saveChanges()
	{
		// This would save all dirty objects to the database
		// For now, just clear the dirty set
		dirtyObjects.clear();
	}
	
	/**
	 * Check if a feature toggle is enabled.
	 */
	public boolean isFeatureOn(String toggle)
	{
		Settings settings = database.getSettings();
		return settings.isFeatureOn(toggle);
	}
	
	/**
	 * Refresh the UI.
	 */
	public void refresh()
	{
		// This would refresh all components
		// To be implemented as we add actual components
	}
	
	/**
	 * Enable or disable dirty detection.
	 */
	public void setDetectDirty(boolean detectDirty)
	{
		this.detectDirty = detectDirty;
	}
	
	/**
	 * Mark objects as dirty.
	 */
	@Override
	public void setDirty(Object... objs)
	{
		if (!detectDirty)
		{
			return;
		}
		
		for (Object obj : objs)
		{
			if (obj != null)
			{
				dirtyObjects.add(obj);
			}
		}
	}
	
	/**
	 * Clear the dirty state.
	 */
	@Override
	public void clearDirty()
	{
		dirtyObjects.clear();
	}
	
	/**
	 * Check if an object is dirty.
	 */
	public boolean isDirty(Object obj)
	{
		return dirtyObjects.contains(obj);
	}
	
	/**
	 * Get the database.
	 */
	public Database getDatabase()
	{
		return database;
	}
}
