package mclachlan.brewday.ui.swing.app;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import com.formdev.flatlaf.FlatLightLaf;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.ui.swing.screens.AboutScreen;
import mclachlan.brewday.ui.swing.screens.InventoryScreen;
import mclachlan.brewday.ui.swing.screens.PlaceholderScreen;
import mclachlan.brewday.ui.swing.screens.WaterScreen;
import mclachlan.brewday.ui.swing.screens.WaterParametersScreen;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingAppFrame extends JFrame
{
	private final DirtyStateService dirtyState = new DirtyStateService();
	private final CardLayout cards = new CardLayout();
	private final JPanel cardsHost = new JPanel(cards);
	private final Map<ScreenKey, SwingScreen> screens = new EnumMap<>(ScreenKey.class);
	private final Map<DefaultMutableTreeNode, ScreenKey> nodeMap = new HashMap<>();
	private final Map<ScreenKey, DefaultMutableTreeNode> keyNodeMap = new EnumMap<>(ScreenKey.class);
	private final Map<ScreenKey, Set<Object>> dirtyTokensByKey = new EnumMap<>(ScreenKey.class);
	private final JLabel status = new JLabel("Ready");
	private ScreenKey currentScreenKey;
	private JTree navTree;

	public SwingAppFrame()
	{
		this(true);
	}

	SwingAppFrame(boolean loadDatabase)
	{
		super("Brewday");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1280, 768);
		setLocationRelativeTo(null);
		initTheme();
		if (loadDatabase)
		{
			Database.getInstance().loadAll();
			setTitle(getUiString("ui.about.msg", UiUtils.getVersion()));
		}
		initUi();
	}

	private void initTheme()
	{
		try
		{
			UIManager.setLookAndFeel(new FlatLightLaf());
		}
		catch (Exception e)
		{
			Brewday.getInstance().getLog().log(e);
		}
	}

	private void initUi()
	{
		setLayout(new BorderLayout());
		setIconImages(Arrays.asList(
			windowIcon(SwingIcons.WINDOW_ICON_16),
			windowIcon(SwingIcons.WINDOW_ICON_32),
			windowIcon(SwingIcons.WINDOW_ICON_64)));

		navTree = buildTree();
		navTree.setName("navigation.tree");
		navTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		initDirtyTokenMapping();
		navTree.setCellRenderer(new NavigationTreeCellRenderer(nodeMap, this::isNodeDirty));
		navTree.addTreeSelectionListener(this::onTreeSelection);
		ToolTipManager.sharedInstance().registerComponent(navTree);
		dirtyState.addListener(() -> SwingUtilities.invokeLater(navTree::repaint));

		registerScreens();

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navTree, cardsHost);
		split.setDividerLocation(230);
		add(split, BorderLayout.CENTER);

		status.setHorizontalAlignment(SwingConstants.LEFT);
		status.setName("status.label");
		add(status, BorderLayout.SOUTH);

		registerHotkeys();

		navTree.setSelectionRow(1);
	}

	private void registerScreens()
	{
		for (ScreenKey key : ScreenKey.values())
		{
			register(key, createScreen(key));
		}
	}

	SwingScreen createScreen(ScreenKey key)
	{
		return switch (key)
		{
			case INVENTORY -> new InventoryScreen(this, dirtyState);
			case BREWING -> new PlaceholderScreen(getUiString("tab.brewing"));
			case RECIPES -> new PlaceholderScreen(getUiString("tab.recipes"));
			case BATCHES -> new PlaceholderScreen(getUiString("tab.batches"));
			case PROCESS_TEMPLATES -> new PlaceholderScreen(getUiString("tab.process.templates"));
			case EQUIPMENT_PROFILES -> new PlaceholderScreen(getUiString("tab.equipment.profiles"));
			case INVENTORY_GROUP -> new PlaceholderScreen(getUiString("tab.inventory"));
			case REFERENCE_DATABASE -> new PlaceholderScreen(getUiString("tab.reference.database"));
			case WATER -> new WaterScreen(this, dirtyState);
			case WATER_PARAMETERS -> new WaterParametersScreen(this, dirtyState);
			case FERMENTABLES -> new PlaceholderScreen(getUiString("tab.fermentables"));
			case HOPS -> new PlaceholderScreen(getUiString("tab.hops"));
			case YEAST -> new PlaceholderScreen(getUiString("tab.yeast"));
			case MISC -> new PlaceholderScreen(getUiString("tab.misc"));
			case STYLES -> new PlaceholderScreen(getUiString("tab.styles"));
			case TOOLS -> new PlaceholderScreen(getUiString("tab.tools"));
			case IMPORT -> new PlaceholderScreen(getUiString("tools.import"));
			case WATER_BUILDER -> new PlaceholderScreen(getUiString("tools.water.builder"));
			case SETTINGS -> new PlaceholderScreen(getUiString("tab.settings"));
			case BREWING_SETTINGS -> new PlaceholderScreen(getUiString("settings.brewing"));
			case BREWING_SETTINGS_GENERAL -> new PlaceholderScreen(getUiString("settings.brewing.general"));
			case BREWING_SETTINGS_MASH -> new PlaceholderScreen(getUiString("settings.brewing.mash"));
			case BREWING_SETTINGS_IBU -> new PlaceholderScreen(getUiString("settings.brewing.ibu"));
			case BACKEND_SETTINGS -> new PlaceholderScreen(getUiString("settings.backend"));
			case BACKEND_SETTINGS_LOCAL_FILESYSTEM -> new PlaceholderScreen(getUiString("settings.backend.local.filesystem"));
			case BACKEND_SETTINGS_GIT -> new PlaceholderScreen(getUiString("settings.backend.git"));
			case UI_SETTINGS -> new PlaceholderScreen(getUiString("settings.ui"));
			case HELP -> new PlaceholderScreen(getUiString("ui.help"));
			case ABOUT -> new AboutScreen();
		};
	}

	private void register(ScreenKey key, SwingScreen screen)
	{
		screens.put(key, screen);
		cardsHost.add((JComponent)screen, key.name());
	}

	private JTree buildTree()
	{
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
		DefaultMutableTreeNode brewing = node(root, getUiString("tab.brewing"), ScreenKey.BREWING);
		node(brewing, getUiString("tab.recipes"), ScreenKey.RECIPES);
		node(brewing, getUiString("tab.batches"), ScreenKey.BATCHES);
		node(brewing, getUiString("tab.process.templates"), ScreenKey.PROCESS_TEMPLATES);
		node(brewing, getUiString("tab.equipment.profiles"), ScreenKey.EQUIPMENT_PROFILES);

		DefaultMutableTreeNode inventory = node(root, getUiString("tab.inventory"), ScreenKey.INVENTORY_GROUP);
		node(inventory, getUiString("tab.inventory"), ScreenKey.INVENTORY);

		DefaultMutableTreeNode ref = node(root, getUiString("tab.reference.database"), ScreenKey.REFERENCE_DATABASE);
		node(ref, getUiString("tab.water"), ScreenKey.WATER);
		node(ref, getUiString("tab.water.parameters"), ScreenKey.WATER_PARAMETERS);
		node(ref, getUiString("tab.fermentables"), ScreenKey.FERMENTABLES);
		node(ref, getUiString("tab.hops"), ScreenKey.HOPS);
		node(ref, getUiString("tab.yeast"), ScreenKey.YEAST);
		node(ref, getUiString("tab.misc"), ScreenKey.MISC);
		node(ref, getUiString("tab.styles"), ScreenKey.STYLES);

		DefaultMutableTreeNode tools = node(root, getUiString("tab.tools"), ScreenKey.TOOLS);
		node(tools, getUiString("tools.import"), ScreenKey.IMPORT);
		node(tools, getUiString("tools.water.builder"), ScreenKey.WATER_BUILDER);

		DefaultMutableTreeNode settings = node(root, getUiString("tab.settings"), ScreenKey.SETTINGS);
		DefaultMutableTreeNode brewingSettings = node(settings, getUiString("settings.brewing"), ScreenKey.BREWING_SETTINGS);
		node(brewingSettings, getUiString("settings.brewing.general"), ScreenKey.BREWING_SETTINGS_GENERAL);
		node(brewingSettings, getUiString("settings.brewing.mash"), ScreenKey.BREWING_SETTINGS_MASH);
		node(brewingSettings, getUiString("settings.brewing.ibu"), ScreenKey.BREWING_SETTINGS_IBU);
		DefaultMutableTreeNode backend = node(settings, getUiString("settings.backend"), ScreenKey.BACKEND_SETTINGS);
		node(backend, getUiString("settings.backend.local.filesystem"), ScreenKey.BACKEND_SETTINGS_LOCAL_FILESYSTEM);
		node(backend, getUiString("settings.backend.git"), ScreenKey.BACKEND_SETTINGS_GIT);
		node(settings, getUiString("settings.ui"), ScreenKey.UI_SETTINGS);

		DefaultMutableTreeNode help = node(root, getUiString("ui.help"), ScreenKey.HELP);
		node(help, getUiString("ui.about"), ScreenKey.ABOUT);

		JTree tree = new JTree(new DefaultTreeModel(root));
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		return tree;
	}

	private DefaultMutableTreeNode node(DefaultMutableTreeNode parent, String label, ScreenKey key)
	{
		DefaultMutableTreeNode n = new DefaultMutableTreeNode(label);
		parent.add(n);
		nodeMap.put(n, key);
		keyNodeMap.put(key, n);
		return n;
	}

	private void onTreeSelection(TreeSelectionEvent event)
	{
		Object selected = event.getPath().getLastPathComponent();
		if (!(selected instanceof DefaultMutableTreeNode node))
		{
			return;
		}
		ScreenKey key = nodeMap.get(node);
		if (key == null)
		{
			return;
		}
		showScreen(key, node.getUserObject().toString());
	}

	private void initDirtyTokenMapping()
	{
		dirtyTokensByKey.put(ScreenKey.WATER, Set.of("water"));
		dirtyTokensByKey.put(ScreenKey.WATER_PARAMETERS, Set.of("water.parameters"));
		dirtyTokensByKey.put(ScreenKey.REFERENCE_DATABASE, Set.of("reference.database"));
		dirtyTokensByKey.put(ScreenKey.INVENTORY, Set.of("inventory"));
		dirtyTokensByKey.put(ScreenKey.INVENTORY_GROUP, Set.of("inventory"));
	}

	private boolean isNodeDirty(DefaultMutableTreeNode node)
	{
		ScreenKey key = nodeMap.get(node);
		if (key != null && isKeyDirty(key))
		{
			return true;
		}
		for (int i = 0; i < node.getChildCount(); i++)
		{
			Object child = node.getChildAt(i);
			if (child instanceof DefaultMutableTreeNode childNode && isNodeDirty(childNode))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isKeyDirty(ScreenKey key)
	{
		Set<Object> tokens = dirtyTokensByKey.get(key);
		if (tokens == null)
		{
			return false;
		}
		for (Object token : tokens)
		{
			if (dirtyState.isDirty(token))
			{
				return true;
			}
		}
		return false;
	}

	private void showScreen(ScreenKey key, String statusText)
	{
		SwingScreen screen = screens.get(key);
		if (screen != null)
		{
			screen.onActivate();
			screen.refresh();
		}
		currentScreenKey = key;
		cards.show(cardsHost, key.name());
		status.setText(statusText);
	}

	private void registerHotkeys()
	{
		InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		javax.swing.ActionMap actionMap = getRootPane().getActionMap();
		int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, mask), "refreshCurrent");
		actionMap.put("refreshCurrent", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				status.setText("Refreshed");
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, mask | InputEvent.SHIFT_DOWN_MASK), "quit");
		actionMap.put("quit", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				dispose();
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "helpAbout");
		actionMap.put("helpAbout", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				selectScreen(ScreenKey.ABOUT);
			}
		});
	}

	private Image windowIcon(int size)
	{
		return SwingIcons.windowIcon(size);
	}

	ScreenKey getCurrentScreenKey()
	{
		return currentScreenKey;
	}

	void selectScreen(ScreenKey key)
	{
		DefaultMutableTreeNode node = keyNodeMap.get(key);
		if (node == null || navTree == null)
		{
			return;
		}
		TreePath path = new TreePath(node.getPath());
		navTree.setSelectionPath(path);
	}

	DirtyStateService getDirtyStateService()
	{
		return dirtyState;
	}

	int navNodeFontStyle(ScreenKey key)
	{
		DefaultMutableTreeNode node = keyNodeMap.get(key);
		if (node == null || navTree == null)
		{
			return -1;
		}
		TreePath path = new TreePath(node.getPath());
		int row = navTree.getRowForPath(path);
		boolean selected = path.equals(navTree.getSelectionPath());
		boolean expanded = navTree.isExpanded(path);
		boolean leaf = navTree.getModel().isLeaf(node);
		java.awt.Component c = navTree.getCellRenderer()
			.getTreeCellRendererComponent(navTree, node, selected, expanded, leaf, Math.max(0, row), false);
		return c.getFont().getStyle();
	}
}
