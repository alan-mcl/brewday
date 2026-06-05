package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.BitternessUnit;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.RecipeEditorNavPort;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingWindowGeometry;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import mclachlan.brewday.ui.swing.widgets.SwingCardStack;
import mclachlan.brewday.ui.swing.widgets.SwingBatchSpargePane;
import mclachlan.brewday.ui.swing.widgets.SwingBoilPane;
import mclachlan.brewday.ui.swing.widgets.SwingCombinePane;
import mclachlan.brewday.ui.swing.widgets.SwingCoolPane;
import mclachlan.brewday.ui.swing.widgets.SwingDilutePane;
import mclachlan.brewday.ui.swing.widgets.SwingFermentPane;
import mclachlan.brewday.ui.swing.widgets.SwingFreezeConcentratePane;
import mclachlan.brewday.ui.swing.widgets.SwingFermentableAdditionPane;
import mclachlan.brewday.ui.swing.widgets.SwingHeatPane;
import mclachlan.brewday.ui.swing.widgets.SwingFlySpargePane;
import mclachlan.brewday.ui.swing.widgets.SwingLauterPane;
import mclachlan.brewday.ui.swing.widgets.SwingMashInfusionPane;
import mclachlan.brewday.ui.swing.widgets.SwingMashPane;
import mclachlan.brewday.ui.swing.widgets.SwingPackagePane;
import mclachlan.brewday.ui.swing.widgets.SwingHopAdditionPane;
import mclachlan.brewday.ui.swing.widgets.SwingIngredientAdditionPane;
import mclachlan.brewday.ui.swing.widgets.SwingMiscAdditionPane;
import mclachlan.brewday.ui.swing.widgets.SwingProcessStepGraphScrollPane;
import mclachlan.brewday.ui.swing.widgets.SwingProcessStepPane;
import mclachlan.brewday.ui.swing.widgets.SwingRecipeInfoPanel;
import mclachlan.brewday.ui.swing.widgets.SwingRecipeTree;
import mclachlan.brewday.ui.swing.widgets.SwingSplitPane;
import mclachlan.brewday.ui.swing.widgets.SwingStandPane;
import mclachlan.brewday.ui.swing.widgets.SwingSteepPane;
import mclachlan.brewday.ui.swing.widgets.SwingWaterAdditionPane;
import mclachlan.brewday.ui.swing.widgets.SwingYeastAdditionPane;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Application-modal recipe editor (draft {@link Recipe} with OK/Cancel apply semantics).
 */
public class RecipeEditorDialog extends JDialog
{
	private final JFrame ownerFrame;
	private final DirtyStateService dirtyState;
	private final DbPort dbPort;
	private final Runnable navTagsRefresh;
	private final RecipeEditorNavPort navPort;
	private final Recipe liveRecipe;
	private final boolean processTemplateMode;
	private Recipe draft;
	private boolean dismissedCleanly;

	private final SwingRecipeTree recipeTree;
	private final SwingCardStack cardStack;
	private final SwingRecipeInfoPanel infoPanel;
	private final JTextArea logArea;
	private final JCheckBox verboseLogCheckbox;
	private final JTextArea endResultArea;
	private final SwingProcessStepGraphScrollPane processGraphView;
	private final JTabbedPane tabs;

	private final Action addStepAction;
	private final Action renameStepAction;
	private final Action duplicateStepAction;
	private final Action deleteStepAction;
	private final Action okAction;
	private final Action cancelAction;

	private final Map<ProcessStep.Type, SwingProcessStepPane<?>> stepPanes = new EnumMap<>(ProcessStep.Type.class);
	private final Map<IngredientAddition.Type, SwingIngredientAdditionPane<?, ?>> additionPanes =
		new EnumMap<>(IngredientAddition.Type.class);

	public RecipeEditorDialog(JFrame ownerFrame, DirtyStateService dirtyState, Runnable navTagsRefresh,
		RecipeEditorNavPort navPort, Recipe liveRecipe)
	{
		this(ownerFrame, dirtyState, navTagsRefresh, navPort, liveRecipe, new EditorDefaultDbPort(), false);
	}

	/**
	 * Opens the editor for a process template (dry-run, no ingredient addition cards).
	 */
	public RecipeEditorDialog(JFrame ownerFrame, DirtyStateService dirtyState, Runnable navTagsRefresh,
		RecipeEditorNavPort navPort, Recipe liveTemplate, boolean processTemplateMode)
	{
		this(ownerFrame, dirtyState, navTagsRefresh, navPort, liveTemplate, new EditorDefaultDbPort(),
			processTemplateMode);
	}

	RecipeEditorDialog(JFrame ownerFrame, DirtyStateService dirtyState, Runnable navTagsRefresh,
		RecipeEditorNavPort navPort, Recipe liveRecipe, DbPort dbPort)
	{
		this(ownerFrame, dirtyState, navTagsRefresh, navPort, liveRecipe, dbPort, false);
	}

	RecipeEditorDialog(JFrame ownerFrame, DirtyStateService dirtyState, Runnable navTagsRefresh,
		RecipeEditorNavPort navPort, Recipe liveRecipe, DbPort dbPort, boolean processTemplateMode)
	{
		super(ownerFrame, getUiString("recipe.editor.title", liveRecipe.getName()), true);
		this.ownerFrame = ownerFrame;
		this.dirtyState = dirtyState;
		this.navTagsRefresh = navTagsRefresh == null ? () -> {} : navTagsRefresh;
		this.navPort = navPort;
		this.dbPort = dbPort;
		this.liveRecipe = liveRecipe;
		this.processTemplateMode = processTemplateMode;
		this.draft = new Recipe(liveRecipe);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (!dismissedCleanly)
				{
					removeDraftSubtreeFromDirty();
					dismissedCleanly = true;
				}
			}
		});

		JPanel root = new JPanel(new BorderLayout());
		getContentPane().add(root, BorderLayout.CENTER);

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		addStepAction = commandAction("recipe.add.step", "recipe.editor.add.step.action", SwingIcons.IconKey.ADD_STEP, this::showAddStepDialog);
		renameStepAction = commandAction("process.step.rename", "recipe.editor.rename.step.action", SwingIcons.IconKey.RENAME, this::renameStep);
		duplicateStepAction = commandAction("process.step.duplicate", "recipe.editor.duplicate.step.action", SwingIcons.IconKey.DUPLICATE, this::duplicateStep);
		deleteStepAction = commandAction("process.step.delete", "recipe.editor.delete.step.action", SwingIcons.IconKey.DELETE, this::deleteStep);
		renameStepAction.setEnabled(false);
		duplicateStepAction.setEnabled(false);
		deleteStepAction.setEnabled(false);
		bar.add(button(addStepAction));
		bar.add(button(renameStepAction));
		bar.add(button(duplicateStepAction));
		bar.add(button(deleteStepAction));
		root.add(bar, BorderLayout.NORTH);

		recipeTree = new SwingRecipeTree(dirtyState);
		cardStack = new SwingCardStack();
		infoPanel = new SwingRecipeInfoPanel(ownerFrame, dirtyState, this::afterRecipeFieldsMutated, this::rerunAndRefreshOutput,
			this::showAddStepDialog, false,
			processTemplateMode ? null : this::applyProcessTemplateFromDatabase);

		JPanel infoCard = new JPanel(new BorderLayout());
		infoCard.add(infoPanel, BorderLayout.CENTER);
		cardStack.addCard(UiUtils.NONE, infoCard);

		for (ProcessStep.Type t : ProcessStep.Type.values())
		{
			SwingProcessStepPane<?> pane = switch (t)
			{
				case MASH -> new SwingMashPane(dirtyState, recipeTree, processTemplateMode);
				case MASH_INFUSION -> new SwingMashInfusionPane(dirtyState, recipeTree, processTemplateMode);
				case LAUTER -> new SwingLauterPane(dirtyState, recipeTree, processTemplateMode);
				case BATCH_SPARGE -> new SwingBatchSpargePane(dirtyState, recipeTree, processTemplateMode);
				case FLY_SPARGE -> new SwingFlySpargePane(dirtyState, recipeTree, processTemplateMode);
				case BOIL -> new SwingBoilPane(dirtyState, recipeTree, processTemplateMode);
				case FERMENT -> new SwingFermentPane(dirtyState, recipeTree, processTemplateMode);
				case SPLIT -> new SwingSplitPane(dirtyState, recipeTree, processTemplateMode);
				case PACKAGE -> new SwingPackagePane(dirtyState, recipeTree, processTemplateMode);
				case HEAT -> new SwingHeatPane(dirtyState, recipeTree, processTemplateMode);
				case COOL -> new SwingCoolPane(dirtyState, recipeTree, processTemplateMode);
				case STEEP -> new SwingSteepPane(dirtyState, recipeTree, processTemplateMode);
				case STAND -> new SwingStandPane(dirtyState, recipeTree, processTemplateMode);
				case DILUTE -> new SwingDilutePane(dirtyState, recipeTree, processTemplateMode);
				case COMBINE -> new SwingCombinePane(dirtyState, recipeTree, processTemplateMode);
				case FREEZE_CONCENTRATE -> new SwingFreezeConcentratePane(dirtyState, recipeTree, processTemplateMode);
				default -> null;
			};
			if (pane != null)
			{
				pane.setOnVolumesChanged(() ->
				{
					recipeTree.refreshNodeLabels();
					rerunAndRefreshOutput();
				});
				stepPanes.put(t, pane);
				cardStack.addCard(t.name(), pane);
			}
			else
			{
				cardStack.addCard(t.name(), placeholderPanel(getUiString("recipe.editor.step.coming.soon"), t.name()));
			}
		}
		if (!processTemplateMode)
		{
			for (IngredientAddition.Type ingType : IngredientAddition.Type.values())
			{
				JPanel card = switch (ingType)
				{
					case FERMENTABLES -> new SwingFermentableAdditionPane(dirtyState, recipeTree);
					case HOPS -> new SwingHopAdditionPane(dirtyState, recipeTree);
					case WATER -> new SwingWaterAdditionPane(dirtyState, recipeTree);
					case YEAST -> new SwingYeastAdditionPane(dirtyState, recipeTree);
					case MISC -> new SwingMiscAdditionPane(dirtyState, recipeTree);
					default -> placeholderPanel(getUiString("recipe.editor.ingredient.coming.soon"), ingType.name());
				};
				if (card instanceof SwingIngredientAdditionPane<?, ?> pane)
				{
					additionPanes.put(ingType, pane);
				}
				cardStack.addCard(ingType.name(), card);
			}
		}

		// Focus-cycle root for the editor column keeps Tab/Shift-Tab inside step/ingredient cards instead of
		// crossing JSplitPane into the recipe tree (focus-cycle / navigation).
		JPanel cardColumnHost = new JPanel(new BorderLayout());
		cardColumnHost.add(cardStack, BorderLayout.CENTER);
		cardColumnHost.setFocusCycleRoot(true);
		JScrollPane cardScroll = new JScrollPane(cardColumnHost);
		cardScroll.setBorder(null);
		cardScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		cardScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		JScrollPane treeScroll = new JScrollPane(recipeTree.getTree());
		treeScroll.setMinimumSize(new Dimension(200, 120));
		cardScroll.setMinimumSize(new Dimension(200, 120));

		JSplitPane procSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, cardScroll);

		logArea = new JTextArea();
		logArea.setEditable(false);
		logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		verboseLogCheckbox = new JCheckBox(getUiString("recipe.log.verbose.checkbox"));
		verboseLogCheckbox.addActionListener(e -> rerunAndRefreshOutput());

		JPanel logPanel = new JPanel(new BorderLayout());
		logPanel.add(verboseLogCheckbox, BorderLayout.NORTH);
		logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

		endResultArea = new JTextArea();
		endResultArea.setEditable(false);
		endResultArea.setLineWrap(true);
		endResultArea.setWrapStyleWord(true);
		endResultArea.setColumns(28);
		JScrollPane endResultScroll = new JScrollPane(endResultArea);
		endResultScroll.setMinimumSize(new Dimension(120, 120));

		processGraphView = new SwingProcessStepGraphScrollPane(ownerFrame);

		tabs = new JTabbedPane();
		tabs.addTab(getUiString("recipe.process"), procSplit);
		tabs.setToolTipTextAt(0, getUiString("recipe.editor.process.tab.tooltip"));
		tabs.addTab(getUiString("recipe.process.graph"), processGraphView);
		tabs.setToolTipTextAt(
			SwingProcessStepGraphScrollPane.getProcessGraphTabIndex(),
			getUiString("recipe.process.graph.tooltip"));
		tabs.addTab(getUiString("recipe.log"), logPanel);
		tabs.setToolTipTextAt(2, getUiString("recipe.editor.log.tab.tooltip"));
		tabs.addChangeListener(e ->
		{
			if (tabs.getSelectedIndex() == SwingProcessStepGraphScrollPane.getProcessGraphTabIndex()
				&& draft != null)
			{
				processGraphView.ensureLaidOut(draft);
			}
		});

		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, endResultScroll);
		root.add(mainSplit, BorderLayout.CENTER);

		okAction = commandAction("ui.ok", "recipe.editor.ok.action", SwingIcons.IconKey.OK, this::onOkClicked);
		cancelAction = commandAction("ui.cancel", "recipe.editor.cancel.action", SwingIcons.IconKey.CANCEL, this::onCancelClicked);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
		JButton okButton = new JButton(okAction);
		south.add(okButton);
		south.add(new JButton(cancelAction));
		root.add(south, BorderLayout.SOUTH);

		recipeTree.setSelectionListener(this::onTreeSelection);
		dirtyState.addListener(() -> SwingUtilities.invokeLater(this::onDirtyStateChanged));

		wireHotkeys();
		getRootPane().setDefaultButton(okButton);

		setRecipe(draft);
		Dimension editorSize = SwingWindowGeometry.defaultRecipeEditorSize(ownerFrame);
		setSize(editorSize.width, editorSize.height);
		setLocationRelativeTo(ownerFrame);
		final JSplitPane mainSplitFinal = mainSplit;
		final JSplitPane procSplitFinal = procSplit;
		SwingUtilities.invokeLater(() ->
			SwingWindowGeometry.applyRecipeEditorSplitDividers(mainSplitFinal, procSplitFinal));
	}


	private void onOkClicked()
	{
		removeDraftSubtreeFromDirty();
		applyDraftToLive();
		dismissedCleanly = true;
		dispose();
	}

	private void onCancelClicked()
	{
		onCancel();
	}

	private static JPanel placeholderPanel(String message, String typeName)
	{
		JPanel p = new JPanel(new BorderLayout(8, 8));
		String body = typeName.isEmpty() ? message : message + "\n\n" + typeName;
		p.add(new JLabel("<html><body style='width:280px'>" + body.replace("\n", "<br/>") + "</body></html>"), BorderLayout.NORTH);
		return p;
	}

	private void onDirtyStateChanged()
	{
		if (draft == null)
		{
			return;
		}
		if (dirtyState.isDirty(draft))
		{
			rerunAndRefreshOutput();
			recipeTree.refreshNodeLabels();
			refreshVisibleEditorSurfaces();
			return;
		}
		for (ProcessStep s : draft.getSteps())
		{
			if (dirtyState.isDirty(s))
			{
				rerunAndRefreshOutput();
				recipeTree.refreshNodeLabels();
				refreshVisibleEditorSurfaces();
				return;
			}
			for (IngredientAddition a : s.getIngredientAdditions())
			{
				if (dirtyState.isDirty(a))
				{
					rerunAndRefreshOutput();
					recipeTree.refreshNodeLabels();
					refreshVisibleEditorSurfaces();
					return;
				}
			}
		}
	}

	private void afterRecipeFieldsMutated()
	{
		rerunAndRefreshOutput();
		recipeTree.refreshNodeLabels();
		refreshVisibleEditorSurfaces();
	}

	private void onTreeSelection(Object selected)
	{
		boolean stepSelected = selected instanceof ProcessStep;
		renameStepAction.setEnabled(stepSelected);
		duplicateStepAction.setEnabled(stepSelected);
		deleteStepAction.setEnabled(stepSelected);

		if (selected == null || selected instanceof Recipe)
		{
			cardStack.setVisibleCard(UiUtils.NONE);
			return;
		}
		if (selected instanceof ProcessStep ps)
		{
			SwingProcessStepPane<?> pane = stepPanes.get(ps.getType());
			if (pane != null)
			{
				pane.refresh(ps, draft);
			}
			cardStack.setVisibleCard(ps.getType().name());
			return;
		}
		if (selected instanceof IngredientAddition ia)
		{
			if (processTemplateMode)
			{
				cardStack.setVisibleCard(UiUtils.NONE);
				return;
			}
			SwingIngredientAdditionPane<?, ?> pane = additionPanes.get(ia.getType());
			if (pane != null)
			{
				pane.refresh(ia, draft);
			}
			cardStack.setVisibleCard(ia.getType().name());
		}
	}

	private ProcessStep selectedStep()
	{
		Object u = recipeTree.getSelectedUserObject();
		return u instanceof ProcessStep ps ? ps : null;
	}

	private void setRecipe(Recipe r)
	{
		this.draft = r;
		if (r == null)
		{
			return;
		}
		recipeTree.setRecipe(r);
		infoPanel.refresh(r);
		rerunAndRefreshOutput();
		SwingUtilities.invokeLater(() ->
		{
			recipeTree.selectRoot();
			cardStack.setVisibleCard(UiUtils.NONE);
		});
	}

	public void rerunAndRefreshOutput()
	{
		if (draft == null)
		{
			return;
		}
		try
		{
			boolean verbose = verboseLogCheckbox.isSelected();
			if (processTemplateMode)
			{
				draft.dryRun();
			}
			else
			{
				draft.run(verbose);
			}
		}
		catch (Exception e)
		{
			Brewday.getInstance().getLog().log(e);
			e.printStackTrace(System.out);
		}
		refreshLog();
		refreshEndResult();
		processGraphView.updateAfterRun(draft);
		refreshVisibleEditorSurfaces();
	}

	private void refreshVisibleEditorSurfaces()
	{
		if (draft == null)
		{
			return;
		}
		Object u = recipeTree.getSelectedUserObject();
		if (u instanceof ProcessStep ps)
		{
			SwingProcessStepPane<?> pane = stepPanes.get(ps.getType());
			if (pane != null)
			{
				pane.refresh(ps, draft);
			}
			return;
		}
		if (u instanceof IngredientAddition ia)
		{
			if (processTemplateMode)
			{
				return;
			}
			SwingIngredientAdditionPane<?, ?> pane = additionPanes.get(ia.getType());
			if (pane != null)
			{
				pane.refresh(ia, draft);
			}
		}
	}

	private void refreshLog()
	{
		logArea.setText("");
		if (draft == null)
		{
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (String s : draft.getLog().getMsgs())
		{
			s = s.replaceAll("\n", "; ");
			sb.append(s).append("\n");
		}
		logArea.setText(sb.toString());
	}

	private void refreshEndResult()
	{
		endResultArea.setText("");
		if (draft == null)
		{
			return;
		}
		StringBuilder sb = new StringBuilder(getUiString("recipe.end.result") + "\n");

		if (!draft.getErrors().isEmpty())
		{
			sb.append("\n").append(getUiString("recipe.errors")).append("\n");
			for (String s : draft.getErrors())
			{
				sb.append(s).append("\n");
			}
		}

		if (!draft.getWarnings().isEmpty())
		{
			sb.append("\n").append(getUiString("recipe.warnings")).append("\n");
			for (String s : draft.getWarnings())
			{
				sb.append(s).append("\n");
			}
		}

		if (draft.getVolumes().getOutputVolumes().size() > 0)
		{
			for (String s : draft.getVolumes().getOutputVolumes())
			{
				Volume v = (Volume)draft.getVolumes().getVolume(s);
				if (processTemplateMode)
				{
					sb.append(String.format("\n'%s'\n", v.getName()));
				}
				else
				{
					sb.append(String.format("\n'%s' (%.1fl)\n", v.getName(), v.getVolume().get(Quantity.Unit.LITRES)));
					if (v.getType() == Volume.Type.BEER)
					{
						sb.append(String.format("OG %.3f\n", v.getOriginalGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
						sb.append(String.format("FG %.3f\n", v.getGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY)));
					}
					sb.append(String.format("%.1f%% ABV\n", v.getAbv().get() * 100));
					if (v.getType() == Volume.Type.BEER)
					{
						double carbVol = v.getCarbonation() == null
							? 0D
							: v.getCarbonation().get(Quantity.Unit.VOLUMES);
						sb.append(String.format(
							getUiString("recipe.end.result.carbonation") + "\n",
							carbVol));
					}
					Settings settings = Database.getInstance().getSettings();
					for (Settings.HopBitternessFormula formula :
						Settings.parseReportedFormulas(settings))
					{
						BitternessUnit ibu = v.getBitterness(formula);
						if (ibu != null)
						{
							sb.append(String.format(
								"%.0f IBU (%s)\n",
								ibu.get(Quantity.Unit.IBU),
								formula.toString()));
						}
					}
					for (Settings.MashPhModel model : Settings.parseReportedModels(settings))
					{
						PhUnit ph = v.getPh(model);
						if (ph != null)
						{
							sb.append(String.format(
								"pH %.2f (%s)\n",
								ph.get(Quantity.Unit.PH),
								model.toString()));
						}
					}
					sb.append(String.format("%.1f SRM\n", v.getColour().get(Quantity.Unit.SRM)));
				}
			}
		}
		else
		{
			sb.append("\n").append(getUiString("recipe.no.output.volumes")).append("\n");
		}

		endResultArea.setText(sb.toString());
	}

	private void showAddStepDialog()
	{
		if (draft == null)
		{
			return;
		}
		SwingNewStepDialog d = new SwingNewStepDialog(ownerFrame);
		d.setVisible(true);
		ProcessStep.Type t = d.getResult();
		if (t == null)
		{
			return;
		}
		addChosenStep(t);
	}

	/** Package visibility for tests; mirrors the post-dialog path of {@link #showAddStepDialog}. */
	void addChosenStep(ProcessStep.Type t)
	{
		if (draft == null || t == null)
		{
			return;
		}
		ProcessStep step = RecipeEditorSteps.createStep(draft, t);
		draft.getSteps().add(step);
		recipeTree.addStep(step);
		dirtyState.markDirty(step);
		rerunAndRefreshOutput();
	}

	private void renameStep()
	{
		ProcessStep step = selectedStep();
		if (draft == null || step == null)
		{
			return;
		}
		SwingRenameStepDialog d = new SwingRenameStepDialog(ownerFrame, draft, step);
		d.setVisible(true);
		String newName = d.getResult();
		if (newName == null || newName.equalsIgnoreCase(step.getName()))
		{
			return;
		}
		step.setName(newName);
		dirtyState.markDirty(step);
		recipeTree.refreshNodeLabels();
		rerunAndRefreshOutput();
	}

	private void duplicateStep()
	{
		ProcessStep step = selectedStep();
		if (draft == null || step == null)
		{
			return;
		}
		SwingDuplicateStepDialog d = new SwingDuplicateStepDialog(ownerFrame, draft, step);
		d.setVisible(true);
		ProcessStep clone = d.getResult();
		if (clone == null)
		{
			return;
		}
		draft.getSteps().add(clone);
		recipeTree.addStep(clone);
		dirtyState.markDirty(clone);
		recipeTree.selectStep(clone);
		rerunAndRefreshOutput();
	}

	private void deleteStep()
	{
		ProcessStep step = selectedStep();
		if (draft == null || step == null)
		{
			return;
		}
		int r = JOptionPane.showConfirmDialog(this,
			getUiString("editor.delete.msg"),
			getUiString("process.step.delete"),
			JOptionPane.YES_NO_OPTION);
		if (r != JOptionPane.YES_OPTION)
		{
			return;
		}
		doDeleteStep(step);
	}

	private void doDeleteStep(ProcessStep step)
	{
		for (IngredientAddition a : new ArrayList<>(step.getIngredientAdditions()))
		{
			dirtyState.removeDirty(a);
		}
		dirtyState.removeDirty(step);
		draft.getSteps().remove(step);
		recipeTree.removeStep(step);
		dirtyState.markDirty(draft);
		recipeTree.selectRoot();
		rerunAndRefreshOutput();
	}

	private void applyDraftToLive()
	{
		liveRecipe.setDescription(draft.getDescription());
		liveRecipe.setEquipmentProfile(draft.getEquipmentProfile());
		liveRecipe.setTags(new ArrayList<>(draft.getTags()));
		liveRecipe.getSteps().clear();
		for (ProcessStep ps : draft.getSteps())
		{
			liveRecipe.getSteps().add(ps.clone(ps.getName()));
		}
		if (processTemplateMode)
		{
			dirtyState.markDirty(liveRecipe, "processTemplates");
			for (ProcessStep s : liveRecipe.getSteps())
			{
				dirtyState.markDirty(s, "processTemplates");
			}
		}
		else
		{
			dirtyState.markDirty(liveRecipe, "recipes");
			for (ProcessStep s : liveRecipe.getSteps())
			{
				dirtyState.markDirty(s, "recipes");
			}
		}
		navTagsRefresh.run();
	}

	private void applyProcessTemplateFromDatabase()
	{
		if (draft == null || processTemplateMode)
		{
			return;
		}
		SwingApplyNewProcessTemplateDialog d = new SwingApplyNewProcessTemplateDialog(ownerFrame);
		d.setVisible(true);
		String name = d.getOutput();
		if (name == null)
		{
			return;
		}
		Recipe tmpl = Database.getInstance().getProcessTemplates().get(name);
		if (tmpl == null)
		{
			return;
		}
		draft.applyProcessTemplate(tmpl);
		recipeTree.setRecipe(draft);
		dirtyState.markDirty(draft, "recipes");
		for (ProcessStep s : draft.getSteps())
		{
			dirtyState.markDirty(s, "recipes");
		}
		infoPanel.refresh(draft);
		recipeTree.selectRoot();
		cardStack.setVisibleCard(UiUtils.NONE);
		rerunAndRefreshOutput();
		recipeTree.refreshNodeLabels();
	}

	private void onCancel()
	{
		if (!dismissedCleanly)
		{
			removeDraftSubtreeFromDirty();
			dismissedCleanly = true;
		}
		dispose();
	}

	private void removeDraftSubtreeFromDirty()
	{
		for (ProcessStep s : new ArrayList<>(draft.getSteps()))
		{
			for (IngredientAddition a : new ArrayList<>(s.getIngredientAdditions()))
			{
				dirtyState.removeDirty(a);
			}
			dirtyState.removeDirty(s);
		}
		dirtyState.removeDirty(draft);
	}

	private void wireHotkeys()
	{
		ActionHotkeySupport.setMnemonic(addStepAction, KeyEvent.VK_N);
		ActionHotkeySupport.setMnemonic(renameStepAction, KeyEvent.VK_R);
		ActionHotkeySupport.setMnemonic(duplicateStepAction, KeyEvent.VK_D);
		ActionHotkeySupport.applyTooltipText(addStepAction, "recipe.editor.add.step.tooltip");
		ActionHotkeySupport.applyTooltipText(renameStepAction, "recipe.editor.rename.step.tooltip");
		ActionHotkeySupport.applyTooltipText(duplicateStepAction, "recipe.editor.duplicate.step.tooltip");
		ActionHotkeySupport.applyTooltipText(deleteStepAction, "recipe.editor.delete.step.tooltip");
		ActionHotkeySupport.applyTooltipText(okAction, "recipe.editor.ok.tooltip");
		ActionHotkeySupport.applyTooltipText(cancelAction, "recipe.editor.cancel.tooltip");
		ActionHotkeySupport.bind(getRootPane(), ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_N), "recipeEditor.hotkey.addStep", addStepAction);
		ActionHotkeySupport.bind(getRootPane(), ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_R), "recipeEditor.hotkey.renameStep", renameStepAction);
		ActionHotkeySupport.bind(getRootPane(), KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "recipeEditor.hotkey.renameStepF2", renameStepAction);
		ActionHotkeySupport.bind(getRootPane(), ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_D), "recipeEditor.hotkey.dupStep", duplicateStepAction);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "recipeEditor.deleteStep");
		getRootPane().getActionMap().put("recipeEditor.deleteStep", deleteStepAction);
		ActionHotkeySupport.bind(getRootPane(), ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_ENTER), "recipeEditor.hotkey.ok", okAction);
		ActionHotkeySupport.bind(getRootPane(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "recipeEditor.hotkey.cancel", cancelAction);
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

	/*-------------------------------------------------------------------------*/
	/** Test hook: apply draft to live (does not dispose; clears draft dirty markers first). */
	void applyForTest()
	{
		removeDraftSubtreeFromDirty();
		applyDraftToLive();
	}

	/** Test hook: cancel without double-removal when window listener also runs. */
	void cancelForTest()
	{
		removeDraftSubtreeFromDirty();
		dismissedCleanly = true;
		dispose();
	}

	void selectStepInTreeForTest(ProcessStep step)
	{
		recipeTree.selectStep(step);
	}

	void renameSelectedStepForTest(String newName)
	{
		ProcessStep step = selectedStep();
		if (draft == null || step == null || newName == null)
		{
			return;
		}
		step.setName(newName.trim());
		dirtyState.markDirty(step);
		recipeTree.refreshNodeLabels();
		rerunAndRefreshOutput();
	}

	void duplicateSelectedStepForTest(String newName)
	{
		ProcessStep step = selectedStep();
		if (draft == null || step == null || newName == null || newName.trim().isEmpty())
		{
			return;
		}
		String name = newName.trim();
		ProcessStep clone = step.clone(name);
		clone.setName(name);
		draft.getSteps().add(clone);
		recipeTree.addStep(clone);
		dirtyState.markDirty(clone);
		recipeTree.selectStep(clone);
		rerunAndRefreshOutput();
	}

	void deleteSelectedStepForTest()
	{
		ProcessStep step = selectedStep();
		if (draft == null || step == null)
		{
			return;
		}
		doDeleteStep(step);
	}

	SwingRecipeTree getRecipeTree()
	{
		return recipeTree;
	}

	SwingProcessStepPane<?> getStepPaneForTest(ProcessStep.Type type)
	{
		return stepPanes.get(type);
	}

	SwingCardStack getCardStack()
	{
		return cardStack;
	}

	JTextArea getLogArea()
	{
		return logArea;
	}

	JTextArea getEndResultArea()
	{
		return endResultArea;
	}

	public int rowFontStyle(int row)
	{
		return recipeTree.rowFontStyle(row);
	}

	Recipe getDraftForTest()
	{
		return draft;
	}

	Recipe getLiveRecipeForTest()
	{
		return liveRecipe;
	}

	interface DbPort
	{
		Map<String, Recipe> recipes();

		void saveAll() throws Exception;

		void loadAll() throws Exception;
	}

	static class EditorDefaultDbPort implements DbPort
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

}
