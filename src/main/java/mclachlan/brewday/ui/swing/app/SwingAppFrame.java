package mclachlan.brewday.ui.swing.app;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.ui.swing.dialogs.RecipeEditorDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingBatchEditorDialog;
import mclachlan.brewday.ui.swing.screens.*;

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
	private final Map<DefaultMutableTreeNode, String> tagNodeMap = new HashMap<>();
	private final JLabel status = new JLabel("Ready");
	private ScreenKey currentScreenKey;
	private JTree navTree;
	private DefaultMutableTreeNode recipesNavNode;
	private RecipesScreen recipesScreen;
	private BatchesScreen batchesScreen;
	private ProcessTemplatesScreen processTemplatesScreen;

	public SwingAppFrame()
	{
		this(true);
	}

	SwingAppFrame(boolean loadDatabase)
	{
		super("Brewday");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Dimension starter = SwingWindowGeometry.defaultMainFrameSize();
		setSize(starter.width, starter.height);
		setLocationRelativeTo(null);
		if (loadDatabase)
		{
			Database.getInstance().loadAll();
			setTitle(getUiString("ui.about.msg", UiUtils.getVersion()));
		}
		SwingThemeSupport.applySwingLafFromSettings(Database.getInstance().getSettings());
		initUi();
	}

	private void initUi()
	{
		setLayout(new BorderLayout());
		setIconImages(SwingIcons.brewdayWindowImages());

		try
		{
			if (Taskbar.isTaskbarSupported())
			{
				Taskbar taskbar = Taskbar.getTaskbar();

				if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE))
				{
					taskbar.setIconImage(SwingIcons.windowIcon(128));
				}
			}
		}
		catch (UnsupportedOperationException e)
		{
			e.printStackTrace(System.out);
			try
			{
				Brewday.getInstance().getLog().log(e);
			}
			catch (Throwable logEx)
			{
				logEx.printStackTrace(System.out);
			}
		}
		catch (SecurityException e)
		{
			e.printStackTrace(System.out);
			try
			{
				Brewday.getInstance().getLog().log(e);
			}
			catch (Throwable logEx)
			{
				logEx.printStackTrace(System.out);
			}
		}

		navTree = buildTree();
		navTree.setName("navigation.tree");
		navTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		initDirtyTokenMapping();
		navTree.setCellRenderer(new NavigationTreeCellRenderer(nodeMap, tagNodeMap, this::isNodeDirty));
		navTree.addTreeSelectionListener(this::onTreeSelection);
		ToolTipManager.sharedInstance().registerComponent(navTree);
		dirtyState.addListener(() -> SwingUtilities.invokeLater(navTree::repaint));

		registerScreens();
		refreshRecipeTagNodes();

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navTree, cardsHost);
		split.setDividerLocation(230);
		add(split, BorderLayout.CENTER);

		status.setHorizontalAlignment(SwingConstants.LEFT);
		status.setName("status.label");
		add(status, BorderLayout.SOUTH);

		registerHotkeys();

		selectScreen(ScreenKey.RECIPES);
		SwingUtilities.invokeLater(() -> navTree.requestFocusInWindow());

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				requestApplicationExit();
			}
		});
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
			case BREWING -> new NavLandingScreen(this::selectScreen, getUiString("tab.brewing"),
				new NavLandingScreen.Destination(ScreenKey.RECIPES, getUiString("tab.recipes")),
				new NavLandingScreen.Destination(ScreenKey.BATCHES, getUiString("tab.batches")),
				new NavLandingScreen.Destination(ScreenKey.PROCESS_TEMPLATES, getUiString("tab.process.templates")),
				new NavLandingScreen.Destination(ScreenKey.EQUIPMENT_PROFILES, getUiString("tab.equipment.profiles")));
			case RECIPES ->
			{
				RecipeBatchCascade recipeBatchCascade = new RecipeBatchCascade(dirtyState, () -> this.batchesScreen);
				this.recipesScreen = new RecipesScreen(this, dirtyState, this::refreshRecipeTagNodes, recipeBatchCascade, recipeBatchCascade,
					new RecipeEditorNavPort()
					{
						@Override
						public void openRecipeEditor(String recipeName)
						{
							SwingAppFrame.this.openRecipeEditor(recipeName);
						}
					});
				yield this.recipesScreen;
			}
			case BATCHES ->
			{
				this.batchesScreen = new BatchesScreen(this, dirtyState, this::openBatchEditor);
				yield this.batchesScreen;
			}
			case PROCESS_TEMPLATES ->
			{
				this.processTemplatesScreen = new ProcessTemplatesScreen(this, dirtyState, this::openProcessTemplateEditor);
				yield this.processTemplatesScreen;
			}
			case EQUIPMENT_PROFILES ->
			{
				EquipmentProfileRecipeCascade cascade = new EquipmentProfileRecipeCascade(recipesScreen, dirtyState);
				yield new EquipmentProfilesScreen(this, dirtyState, cascade, cascade);
			}
			case INVENTORY_GROUP -> new NavLandingScreen(this::selectScreen, getUiString("tab.inventory"),
				new NavLandingScreen.Destination(ScreenKey.INVENTORY, getUiString("tab.inventory")));
			case REFERENCE_DATABASE -> new NavLandingScreen(this::selectScreen, getUiString("tab.reference.database"),
				new NavLandingScreen.Destination(ScreenKey.WATER, getUiString("tab.water")),
				new NavLandingScreen.Destination(ScreenKey.WATER_PARAMETERS, getUiString("tab.water.parameters")),
				new NavLandingScreen.Destination(ScreenKey.FERMENTABLES, getUiString("tab.fermentables")),
				new NavLandingScreen.Destination(ScreenKey.HOPS, getUiString("tab.hops")),
				new NavLandingScreen.Destination(ScreenKey.YEAST, getUiString("tab.yeast")),
				new NavLandingScreen.Destination(ScreenKey.MISC, getUiString("tab.misc")),
				new NavLandingScreen.Destination(ScreenKey.STYLES, getUiString("tab.styles")));
			case WATER -> new WaterScreen(this, dirtyState);
			case WATER_PARAMETERS -> new WaterParametersScreen(this, dirtyState);
			case FERMENTABLES -> new FermentablesScreen(this, dirtyState);
			case HOPS -> new HopsScreen(this, dirtyState);
			case YEAST -> new YeastScreen(this, dirtyState);
			case MISC -> new MiscsScreen(this, dirtyState);
			case STYLES -> new StylesScreen(this, dirtyState);
			case TOOLS -> new NavLandingScreen(this::selectScreen, getUiString("tab.tools"),
				new NavLandingScreen.Destination(ScreenKey.IMPORT, getUiString("tools.import")),
				new NavLandingScreen.Destination(ScreenKey.WATER_BUILDER, getUiString("tools.water.builder")),
				new NavLandingScreen.Destination(ScreenKey.KEG_LINE_LENGTH, getUiString("tools.keg.line.length")),
				new NavLandingScreen.Destination(ScreenKey.YEAST_CALCULATOR, getUiString("tools.yeast.calculator")),
				new NavLandingScreen.Destination(ScreenKey.RECIPE_TAG_MANAGER, getUiString("tools.tag.manager")));
			case IMPORT -> new ImportDataScreen(this, dirtyState);
			case WATER_BUILDER -> new WaterBuilderScreen();
			case KEG_LINE_LENGTH -> new KegLineLengthScreen();
			case YEAST_CALCULATOR -> new YeastCalculatorScreen();
			case RECIPE_TAG_MANAGER ->
				new RecipeTagManagerScreen(this, dirtyState, () ->
				{
					refreshRecipeTagNodes();
					if (recipesScreen != null)
					{
						recipesScreen.refresh();
					}
				});
			case SETTINGS -> new NavLandingScreen(this::selectScreen, getUiString("tab.settings"),
				new NavLandingScreen.Destination(ScreenKey.BREWING_SETTINGS, getUiString("settings.brewing")),
				new NavLandingScreen.Destination(ScreenKey.BACKEND_SETTINGS, getUiString("settings.backend")),
				new NavLandingScreen.Destination(ScreenKey.UI_SETTINGS, getUiString("settings.ui")));
			case BREWING_SETTINGS -> new NavLandingScreen(this::selectScreen, getUiString("settings.brewing"),
				new NavLandingScreen.Destination(ScreenKey.BREWING_SETTINGS_GENERAL, getUiString("settings.brewing.general")),
				new NavLandingScreen.Destination(ScreenKey.BREWING_SETTINGS_MASH, getUiString("settings.brewing.mash")),
				new NavLandingScreen.Destination(ScreenKey.BREWING_SETTINGS_IBU, getUiString("settings.brewing.ibu")));
			case BREWING_SETTINGS_GENERAL -> new BrewingSettingsGeneralScreen();
			case BREWING_SETTINGS_MASH -> new BrewingSettingsMashScreen();
			case BREWING_SETTINGS_IBU -> new BrewingSettingsIbuScreen();
			case BACKEND_SETTINGS -> new NavLandingScreen(this::selectScreen, getUiString("settings.backend"),
				new NavLandingScreen.Destination(ScreenKey.BACKEND_SETTINGS_LOCAL_FILESYSTEM, getUiString("settings.backend.local.filesystem")),
				new NavLandingScreen.Destination(ScreenKey.BACKEND_SETTINGS_GIT, getUiString("settings.backend.git")));
			case BACKEND_SETTINGS_LOCAL_FILESYSTEM -> new BackendSettingsLocalFilesystemScreen(this);
			case BACKEND_SETTINGS_GIT -> new GitBackendScreen();
			case UI_SETTINGS -> new UiSettingsScreen();
			case HELP -> new NavLandingScreen(this::selectScreen, getUiString("ui.help"),
				new NavLandingScreen.Destination(ScreenKey.ABOUT, getUiString("ui.about")));
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
		recipesNavNode = node(brewing, getUiString("tab.recipes"), ScreenKey.RECIPES);
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
		node(tools, getUiString("tools.keg.line.length"), ScreenKey.KEG_LINE_LENGTH);
		node(tools, getUiString("tools.yeast.calculator"), ScreenKey.YEAST_CALCULATOR);
		node(tools, getUiString("tools.tag.manager"), ScreenKey.RECIPE_TAG_MANAGER);

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
		if (key == ScreenKey.RECIPES && recipesScreen != null)
		{
			recipesScreen.setTag(tagNodeMap.get(node));
		}
		displayScreen(key, node.getUserObject().toString());
	}

	void refreshRecipeTagNodes()
	{
		if (recipesNavNode == null || navTree == null)
		{
			return;
		}
		while (recipesNavNode.getChildCount() > 0)
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)recipesNavNode.getFirstChild();
			tagNodeMap.remove(child);
			nodeMap.remove(child);
			recipesNavNode.remove(child);
		}
		for (String tag : Brewday.getInstance().getRecipeTags())
		{
			DefaultMutableTreeNode tagNode = new DefaultMutableTreeNode(tag);
			recipesNavNode.add(tagNode);
			nodeMap.put(tagNode, ScreenKey.RECIPES);
			tagNodeMap.put(tagNode, tag);
		}
		((DefaultTreeModel)navTree.getModel()).nodeStructureChanged(recipesNavNode);
	}

	private void initDirtyTokenMapping()
	{
		dirtyTokensByKey.put(ScreenKey.WATER, Set.of("water"));
		dirtyTokensByKey.put(ScreenKey.WATER_PARAMETERS, Set.of("water.parameters"));
		dirtyTokensByKey.put(ScreenKey.FERMENTABLES, Set.of("fermentables"));
		dirtyTokensByKey.put(ScreenKey.HOPS, Set.of("hops"));
		dirtyTokensByKey.put(ScreenKey.YEAST, Set.of("yeast"));
		dirtyTokensByKey.put(ScreenKey.MISC, Set.of("misc"));
		dirtyTokensByKey.put(ScreenKey.STYLES, Set.of("styles"));
		dirtyTokensByKey.put(ScreenKey.REFERENCE_DATABASE, Set.of("reference.database"));
		dirtyTokensByKey.put(ScreenKey.INVENTORY, Set.of("inventory"));
		dirtyTokensByKey.put(ScreenKey.INVENTORY_GROUP, Set.of("inventory"));
		dirtyTokensByKey.put(ScreenKey.BREWING, Set.of("brewing"));
		dirtyTokensByKey.put(ScreenKey.RECIPES, Set.of("recipes"));
		dirtyTokensByKey.put(ScreenKey.BATCHES, Set.of("batches"));
		dirtyTokensByKey.put(ScreenKey.PROCESS_TEMPLATES, Set.of("processTemplates"));
		dirtyTokensByKey.put(ScreenKey.EQUIPMENT_PROFILES, Set.of("equipment.profiles"));
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

	void refreshAllScreens()
	{
		for (SwingScreen s : screens.values())
		{
			if (s != null)
			{
				s.refresh();
			}
		}
	}

	private void globalSaveAll()
	{
		int opt = JOptionPane.showConfirmDialog(this,
			getUiString("editor.apply.all.msg"),
			getUiString("editor.apply.all"),
			JOptionPane.OK_CANCEL_OPTION);
		if (opt != JOptionPane.OK_OPTION)
		{
			return;
		}
		status.setText(getUiString("swing.status.saving"));
		new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				Database.getInstance().saveAll();
				return null;
			}

			@Override
			protected void done()
			{
				try
				{
					get();
					dirtyState.clear();
					refreshRecipeTagNodes();
					refreshAllScreens();
					navTree.repaint();
					status.setText(getUiString("swing.status.save.all.done"));
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					System.out.println("[Swing] global save/load task interrupted");
					status.setText("Ready");
				}
				catch (ExecutionException e)
				{
					Throwable c = e.getCause() != null ? e.getCause() : e;
					SwingUiErrors.showError(SwingAppFrame.this, c, getUiString("ui.error"));
					status.setText(getUiString("ui.error"));
				}
			}
		}.execute();
	}

	private void globalUndoAll()
	{
		int opt = JOptionPane.showConfirmDialog(this,
			getUiString("editor.discard.all.msg"),
			getUiString("editor.discard.all"),
			JOptionPane.OK_CANCEL_OPTION);
		if (opt != JOptionPane.OK_OPTION)
		{
			return;
		}
		reloadDatabaseFromDisk(getUiString("swing.status.reloading"), getUiString("swing.status.undo.all.done"), false);
	}

	/**
	 * Restores {@code dbDir/backup/*.json} over the live database, reloads memory, and refreshes UI.
	 * Caller must show confirmation before invoking (settings local-storage screen).
	 */
	public void reloadAfterLocalBackupRestore(Component dialogParent)
	{
		reloadDatabaseFromDisk(
			getUiString("swing.status.restoring.backup"),
			getUiString("settings.local.storage.restore.backup.success"),
			true,
			dialogParent);
	}

	private void reloadDatabaseFromDisk(String statusInProgress, String statusDone, boolean restoreFromBackup)
	{
		reloadDatabaseFromDisk(statusInProgress, statusDone, restoreFromBackup, this);
	}

	private void reloadDatabaseFromDisk(
		String statusInProgress,
		String statusDone,
		boolean restoreFromBackup,
		Component errorParent)
	{
		status.setText(statusInProgress);
		Component errorDialogParent = errorParent != null ? errorParent : this;
		new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				if (restoreFromBackup)
				{
					Database.getInstance().restoreDb();
				}
				Database.getInstance().loadAll();
				return null;
			}

			@Override
			protected void done()
			{
				try
				{
					get();
					dirtyState.clear();
					refreshRecipeTagNodes();
					refreshAllScreens();
					navTree.repaint();
					status.setText(statusDone);
					if (restoreFromBackup)
					{
						JOptionPane.showMessageDialog(
							errorDialogParent,
							getUiString("settings.local.storage.restore.backup.success"),
							getUiString("settings.local.storage.restore.backup.title"),
							JOptionPane.INFORMATION_MESSAGE);
					}
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					System.out.println("[Swing] global save/load task interrupted");
					status.setText("Ready");
				}
				catch (ExecutionException e)
				{
					Throwable c = e.getCause() != null ? e.getCause() : e;
					SwingUiErrors.showError(errorDialogParent, c, getUiString("ui.error"));
					status.setText(getUiString("ui.error"));
				}
			}
		}.execute();
	}

	private void displayScreen(ScreenKey key, String statusText)
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

	public void openBatchEditor(String batchName)
	{
		Batch b = Database.getInstance().getBatches().get(batchName);
		if (b == null)
		{
			return;
		}
		SwingBatchEditorDialog d = new SwingBatchEditorDialog(this, dirtyState, b);
		d.setLocationRelativeTo(this);
		d.setVisible(true);
		if (batchesScreen != null)
		{
			batchesScreen.refresh();
		}
	}

	public void openRecipeEditor(String recipeName)
	{
		Recipe r = Database.getInstance().getRecipes().get(recipeName);
		if (r == null)
		{
			return;
		}
		RecipeEditorNavPort reopenNav = new RecipeEditorNavPort()
		{
			@Override
			public void openRecipeEditor(String name)
			{
				SwingAppFrame.this.openRecipeEditor(name);
			}
		};
		RecipeEditorDialog d = new RecipeEditorDialog(this, dirtyState, this::refreshRecipeTagNodes, reopenNav, r);
		d.setLocationRelativeTo(this);
		d.setVisible(true);
		if (recipesScreen != null)
		{
			recipesScreen.refresh();
		}
		refreshRecipeTagNodes();
	}

	/**
	 * Edits a process template (dry-run recipe editor).
	 */
	public void openProcessTemplateEditor(String templateName)
	{
		Recipe r = Database.getInstance().getProcessTemplates().get(templateName);
		if (r == null)
		{
			return;
		}
		RecipeEditorNavPort reopenNav = new RecipeEditorNavPort()
		{
			@Override
			public void openRecipeEditor(String name)
			{
				SwingAppFrame.this.openRecipeEditor(name);
			}
		};
		RecipeEditorDialog d = new RecipeEditorDialog(this, dirtyState, this::refreshRecipeTagNodes, reopenNav, r, true);
		d.setLocationRelativeTo(this);
		d.setVisible(true);
		if (processTemplatesScreen != null)
		{
			processTemplatesScreen.refresh();
		}
		refreshRecipeTagNodes();
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
				if (currentScreenKey != null)
				{
					SwingScreen screen = screens.get(currentScreenKey);
					if (screen != null)
					{
						screen.refresh();
					}
				}
				status.setText(getUiString("swing.status.refreshed"));
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, mask), "globalSaveAll");
		actionMap.put("globalSaveAll", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				globalSaveAll();
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, mask), "globalUndoAll");
		actionMap.put("globalUndoAll", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				globalUndoAll();
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask), "globalUndoFromZ");
		actionMap.put("globalUndoFromZ", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				globalUndoAll();
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, mask | InputEvent.SHIFT_DOWN_MASK), "quit");
		actionMap.put("quit", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				requestApplicationExit();
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

	/**
	 * Quit path shared by window close and the quit hotkey. When dirty, asks for confirmation.
	 */
	void requestApplicationExit()
	{
		if (!dirtyState.hasDirty())
		{
			terminateApplicationForExit();
			return;
		}
		if (confirmExitDespiteDirty())
		{
			terminateApplicationForExit();
		}
	}

	protected boolean confirmExitDespiteDirty()
	{
		return JOptionPane.showConfirmDialog(this,
			getUiString("editor.discard.all.msg"),
			getUiString("ui.exit"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
	}

	protected void terminateApplicationForExit()
	{
		System.exit(0);
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

	public RecipesScreen getRecipesScreen()
	{
		return recipesScreen;
	}

	/** For tests: recipes tree node (tag children are added under this). */
	public DefaultMutableTreeNode getRecipesNavNodeForTest()
	{
		return recipesNavNode;
	}

	/** For tests: main navigation tree. */
	public JTree getNavigationTreeForTest()
	{
		return navTree;
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
