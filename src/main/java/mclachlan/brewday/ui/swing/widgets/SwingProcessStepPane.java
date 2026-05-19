package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.dialogs.SwingFermentableAdditionDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingHopAdditionDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingMiscAdditionDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingRenameOutputVolumeDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingWaterBuilderDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingWaterAdditionDialog;
import mclachlan.brewday.ui.swing.dialogs.SwingYeastAdditionDialog;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code ProcessStepPane}: shared volume combos, unit controls, computed volume tiles.
 *
 * @param <T> concrete {@link ProcessStep} type edited by the subclass
 */
public abstract class SwingProcessStepPane<T extends ProcessStep> extends JPanel
{
	protected final DirtyStateService dirtyState;
	protected final SwingRecipeTree recipeTree;
	private final boolean processTemplateMode;

	private T step;
	private Recipe recipe;
	private boolean refreshing;
	private boolean detectDirty;

	/** Callback invoked after a successful output-volume rename so the host editor
	 * can refresh ancillary surfaces (end-result text, recipe tree labels, etc.). */
	private Runnable onVolumesChanged;

	private final JToolBar stepToolbar;
	private final JPanel form;
	private final JPanel computedVolumesHost;
	private final SwingUnitControlUtils<T> unitControlUtils;

	private final List<VolumeComboRow<T>> volumeRows = new ArrayList<>();
	private final List<SwingComputedVolumePane> computedPanes = new ArrayList<>();
	private final List<Function<T, String>> computedGetters = new ArrayList<>();

	private int formRow;

	public SwingProcessStepPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(new BorderLayout(8, 8));
		this.dirtyState = dirtyState;
		this.recipeTree = recipeTree;
		this.processTemplateMode = processTemplateMode;
		setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

		stepToolbar = new JToolBar();
		stepToolbar.setFloatable(false);
		add(stepToolbar, BorderLayout.NORTH);

		form = new JPanel(new GridBagLayout());
		JPanel centerHost = new JPanel(new BorderLayout());
		centerHost.add(form, BorderLayout.NORTH);
		add(centerHost, BorderLayout.CENTER);

		computedVolumesHost = new JPanel(new GridLayout(0, 2, 8, 8));
		computedVolumesHost.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		add(computedVolumesHost, BorderLayout.SOUTH);

		unitControlUtils = new SwingUnitControlUtils<>(dirtyState, () -> detectDirty && !refreshing && step != null);

		detectDirty = false;
		buildUiInternal();
		detectDirty = true;

		setFocusCycleRoot(true);
	}

	protected abstract void buildUiInternal();

	protected void refreshInternal(T step, Recipe recipe)
	{
	}

	/**
	 * Toolbar reserved for Phase 13c/13d ingredient-add actions.
	 */
	protected final JToolBar getStepToolbar()
	{
		return stepToolbar;
	}

	protected final SwingUnitControlUtils<T> getUnitControlUtils()
	{
		return unitControlUtils;
	}

	protected final boolean isStepPaneRefreshing()
	{
		return refreshing;
	}

	/**
	 * Full-width checkbox row in the form grid (both label columns spanned).
	 */
	protected final void addSpanningCheckboxRow(JCheckBox checkBox)
	{
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = formRow;
		g.gridwidth = 2;
		g.anchor = GridBagConstraints.WEST;
		g.fill = GridBagConstraints.NONE;
		g.weightx = 1.0;
		g.insets = new Insets(3, 4, 3, 4);
		form.add(checkBox, g);
		advanceFormRow();
	}

	protected final void addFullWidthComponentRow(JComponent row)
	{
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = formRow;
		g.gridwidth = 2;
		g.anchor = GridBagConstraints.WEST;
		g.fill = GridBagConstraints.HORIZONTAL;
		g.weightx = 1.0;
		g.insets = new Insets(3, 4, 3, 4);
		form.add(row, g);
		advanceFormRow();
	}

	protected final void addLabeledWidgetToForm(String labelKey, JComponent widget)
	{
		form.add(new JLabel(getUiString(labelKey) + ":"), labelGbc());
		applyLabelTooltip(labelKey, widget);
		form.add(widget, widgetGbc());
		advanceFormRow();
	}

	protected final void addFormSecondaryMessageWidgets(JComponent message)
	{
		form.add(new JLabel(), labelGbc());
		form.add(message, widgetGbc());
		advanceFormRow();
	}

	/**
	 * Adds one ingredient toolbar button per type supported by {@code prototype} (usually {@code new MyStep()}).
	 */
	protected final void addIngredientButtonsForPrototype(ProcessStep prototype)
	{
		if (prototype == null || processTemplateMode)
		{
			return;
		}
		for (IngredientAddition.Type t : prototype.getSupportedIngredientAdditions())
		{
			addAddIngredientButton(t);
		}
	}

	@SuppressWarnings("unchecked")
	public void refresh(ProcessStep step, Recipe recipe)
	{
		T typed = (T)step;
		this.step = typed;
		this.recipe = recipe;
		detectDirty = false;
		refreshing = true;

		for (VolumeComboRow<T> row : volumeRows)
		{
			DefaultComboBoxModel<String> model = buildVolumeModel(recipe);
			row.combo.setModel(model);
			String cur = typed != null ? row.getter.apply(typed) : null;
			if (cur == null || !modelContains(model, cur))
			{
				row.combo.setSelectedItem(UiUtils.NONE);
			}
			else
			{
				row.combo.setSelectedItem(cur);
			}
		}

		unitControlUtils.refresh(typed);

		for (int i = 0; i < computedPanes.size(); i++)
		{
			String volName = typed != null ? computedGetters.get(i).apply(typed) : null;
			computedPanes.get(i).refresh(volName, recipe);
		}

		refreshInternal(typed, recipe);

		refreshing = false;
		detectDirty = true;
	}

	private GridBagConstraints labelGbc()
	{
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = formRow;
		g.insets = new Insets(3, 4, 3, 4);
		g.anchor = GridBagConstraints.WEST;
		g.fill = GridBagConstraints.NONE;
		g.weightx = 0;
		g.weighty = 0;
		return g;
	}

	private GridBagConstraints widgetGbc()
	{
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 1;
		g.gridy = formRow;
		g.insets = new Insets(3, 4, 3, 4);
		g.anchor = GridBagConstraints.WEST;
		g.fill = GridBagConstraints.HORIZONTAL;
		g.weightx = 1.0;
		g.weighty = 0;
		return g;
	}

	private void advanceFormRow()
	{
		formRow++;
	}

	/**
	 * Label + read-only or externally bound quantity widget (row appended to the main form grid).
	 */
	protected final void addReadOnlyQuantityWidgetRow(String labelKey, SwingQuantityEditWidget<? extends Quantity> w)
	{
		form.add(new JLabel(getUiString(labelKey) + ":"), labelGbc());
		applyLabelTooltip(labelKey, w);
		form.add(w, widgetGbc());
		advanceFormRow();
	}

	protected final void addInputVolumeComboBox(String labelKey,
		Function<T, String> getter, BiConsumer<T, String> setter, Volume.Type... volumeTypes)
	{
		form.add(new JLabel(getUiString(labelKey) + ":"), labelGbc());
		JComboBox<String> combo = new JComboBox<>();
		applyLabelTooltip(labelKey, combo);
		form.add(combo, widgetGbc());
		advanceFormRow();

		VolumeComboRow<T> row = new VolumeComboRow<>(combo, getter, setter, volumeTypes);
		volumeRows.add(row);

		combo.addActionListener(e ->
		{
			if (refreshing || step == null)
			{
				return;
			}
			String selected = (String)combo.getSelectedItem();
			if (UiUtils.NONE.equals(selected))
			{
				setter.accept(step, null);
			}
			else
			{
				setter.accept(step, selected);
			}
			if (detectDirty)
			{
				dirtyState.markDirty(step);
			}
		});
	}

	protected final void addTimeUnitControl(String labelKey,
		Function<T, TimeUnit> get, BiConsumer<T, TimeUnit> set, Quantity.Unit unit)
	{
		form.add(new JLabel(getUiString(labelKey) + ":"), labelGbc());
		SwingQuantityEditWidget<TimeUnit> w = new SwingQuantityEditWidget<>(unit);
		applyLabelTooltip(labelKey, w);
		form.add(w, widgetGbc());
		advanceFormRow();
		unitControlUtils.registerTimeUnit(w, get, set, unit);
	}

	protected final void addTemperatureUnitControl(String labelKey,
		Function<T, TemperatureUnit> get, BiConsumer<T, TemperatureUnit> set, Quantity.Unit unit)
	{
		form.add(new JLabel(getUiString(labelKey) + ":"), labelGbc());
		SwingQuantityEditWidget<TemperatureUnit> w = new SwingQuantityEditWidget<>(unit);
		applyLabelTooltip(labelKey, w);
		form.add(w, widgetGbc());
		advanceFormRow();
		unitControlUtils.registerTemperatureUnit(w, get, set, unit);
	}

	protected final void addVolumeUnitControl(String labelKey,
		Function<T, VolumeUnit> get, BiConsumer<T, VolumeUnit> set, Quantity.Unit unit)
	{
		form.add(new JLabel(getUiString(labelKey) + ":"), labelGbc());
		SwingQuantityEditWidget<VolumeUnit> w = new SwingQuantityEditWidget<>(unit);
		applyLabelTooltip(labelKey, w);
		form.add(w, widgetGbc());
		advanceFormRow();
		unitControlUtils.registerQuantityEdit(w, get, set);
	}

	/**
	 * Sets tooltip from {@code labelKey + ".tooltip"} in ui strings when defined.
	 */
	protected static void applyLabelTooltip(String labelKey, JComponent component)
	{
		String tooltip = Database.getInstance().getStrings("ui").getProperty(labelKey + ".tooltip");
		if (tooltip != null && !tooltip.isBlank())
		{
			component.setToolTipText(tooltip);
		}
	}

	protected final void addComputedVolumePane(String labelKey, Function<T, String> getter)
	{
		SwingComputedVolumePane cvp = new SwingComputedVolumePane(
			getUiString(labelKey),
			oldName -> requestRenameOutputVolume(oldName));
		computedPanes.add(cvp);
		computedGetters.add(getter);
		computedVolumesHost.add(cvp);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Editor-supplied callback fired after a successful output-volume rename so
	 * the host {@code RecipeEditorDialog} can rerun the recipe, refresh tree
	 * labels, and any other ancillary surfaces.
	 */
	public final void setOnVolumesChanged(Runnable callback)
	{
		this.onVolumesChanged = callback;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Opens the rename-output-volume dialog and, on confirm, propagates the
	 * rename through {@link Recipe#renameVolume(String, String)}, marks the
	 * step dirty and refreshes the pane plus any host-editor surfaces.
	 */
	private void requestRenameOutputVolume(String oldName)
	{
		if (oldName == null || step == null || recipe == null || refreshing)
		{
			return;
		}

		Window owner = SwingUtilities.getWindowAncestor(this);
		SwingRenameOutputVolumeDialog dialog = new SwingRenameOutputVolumeDialog(owner, recipe, oldName);
		dialog.setVisible(true);
		String newName = dialog.getResult();
		if (newName == null || newName.equals(oldName))
		{
			return;
		}

		try
		{
			recipe.renameVolume(oldName, newName);
		}
		catch (BrewdayException ex)
		{
			// Validation in the dialog already guards against the common rejections
			// (blank/duplicate). Treat this as a no-op rather than crashing the UI.
			return;
		}

		dirtyState.markDirty(step);
		refresh(step, recipe);
		if (onVolumesChanged != null)
		{
			onVolumesChanged.run();
		}
	}

	/**
	 * Adds a toolbar button that opens the add-ingredient dialog for {@code type} (Hop / Water in Phase 13c).
	 */
	protected final void addAddIngredientButton(IngredientAddition.Type type)
	{
		if (processTemplateMode)
		{
			return;
		}
		JButton b = new JButton(SwingIcons.toolbarIcon(additionToolbarIcon(type)));
		b.setToolTipText(getUiString(additionToolbarTitleKey(type)));
		b.addActionListener(e -> openAddIngredientDialog(type));
		stepToolbar.add(b);
	}

	void openAddIngredientDialogForTest(IngredientAddition.Type type)
	{
		openAddIngredientDialog(type);
	}

	private void openAddIngredientDialog(IngredientAddition.Type type)
	{
		if (step == null || recipe == null)
		{
			return;
		}
		java.awt.Window parent = SwingUtilities.getWindowAncestor(this);
		IngredientAddition out = switch (type)
		{
			case HOPS ->
			{
				SwingHopAdditionDialog d = new SwingHopAdditionDialog(parent, step, null, true);
				d.setVisible(true);
				yield d.getOutput();
			}
			case WATER ->
			{
				SwingWaterAdditionDialog d = new SwingWaterAdditionDialog(parent, step, null, true);
				d.setVisible(true);
				yield d.getOutput();
			}
			case FERMENTABLES ->
			{
				SwingFermentableAdditionDialog d = new SwingFermentableAdditionDialog(parent, step, null, true);
				d.setVisible(true);
				yield d.getOutput();
			}
			case YEAST ->
			{
				SwingYeastAdditionDialog d = new SwingYeastAdditionDialog(parent, step, null);
				d.setVisible(true);
				yield d.getOutput();
			}
			case MISC ->
			{
				SwingMiscAdditionDialog d = new SwingMiscAdditionDialog(parent, step, null, true);
				d.setVisible(true);
				yield d.getOutput();
			}
			default -> null;
		};
		if (out != null)
		{
			commitNewIngredientAddition(out);
		}
	}

	private void commitNewIngredientAddition(IngredientAddition out)
	{
		if (out == null || step == null)
		{
			return;
		}
		step.addIngredientAddition(out);
		recipeTree.addAddition(step, out);
		dirtyState.markDirty(out);
		recipeTree.selectUserObject(out);
	}

	protected final void runWaterBuilderUtility(ProcessStep currentStep)
	{
		if (currentStep == null)
		{
			return;
		}
		java.awt.Window parent = SwingUtilities.getWindowAncestor(this);
		SwingWaterBuilderDialog dialog = new SwingWaterBuilderDialog(parent, currentStep);
		dialog.setVisible(true);
		if (!dialog.getOutput())
		{
			return;
		}

		List<MiscAddition> existing = new ArrayList<>(currentStep.getMiscAdditions());
		for (MiscAddition ma : existing)
		{
			if (ma.getMisc().getWaterAdditionFormula() != null && ma.getMisc().isAcidAddition())
			{
				currentStep.removeIngredientAddition(ma);
				recipeTree.removeAddition(currentStep, ma);
			}
		}

		for (MiscAddition ma : dialog.getWaterAdditions())
		{
			currentStep.addIngredientAddition(ma);
			recipeTree.addAddition(currentStep, ma);
			dirtyState.markDirty(ma);
		}
		dirtyState.markDirty(currentStep);
	}

	/** Package hook for tests: same post-dialog mutation as a successful add. */
	void commitIngredientAdditionForTest(IngredientAddition out)
	{
		commitNewIngredientAddition(out);
	}

	private static SwingIcons.IconKey additionToolbarIcon(IngredientAddition.Type type)
	{
		return switch (type)
		{
			case FERMENTABLES -> SwingIcons.IconKey.ADD_FERMENTABLE;
			case HOPS -> SwingIcons.IconKey.ADD_HOPS;
			case WATER -> SwingIcons.IconKey.ADD_WATER;
			case YEAST -> SwingIcons.IconKey.ADD_YEAST;
			case MISC -> SwingIcons.IconKey.ADD_MISC;
			default -> SwingIcons.IconKey.ADD_STEP;
		};
	}

	private static String additionToolbarTitleKey(IngredientAddition.Type type)
	{
		return switch (type)
		{
			case FERMENTABLES -> "recipe.add.ingredient.fermentables.tooltip";
			case HOPS -> "recipe.add.ingredient.hops.tooltip";
			case WATER -> "recipe.add.ingredient.water.tooltip";
			case YEAST -> "recipe.add.ingredient.yeast.tooltip";
			case MISC -> "recipe.add.ingredient.misc.tooltip";
			default -> "common.add";
		};
	}

	private static DefaultComboBoxModel<String> buildVolumeModel(Recipe recipe)
	{
		List<String> names = new ArrayList<>(recipe.getAllVolumeNames());
		Collections.sort(names);
		DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
		m.addElement(UiUtils.NONE);
		for (String n : names)
		{
			m.addElement(n);
		}
		return m;
	}

	private static boolean modelContains(DefaultComboBoxModel<String> m, String value)
	{
		for (int i = 0; i < m.getSize(); i++)
		{
			if (value.equals(m.getElementAt(i)))
			{
				return true;
			}
		}
		return false;
	}

	private static final class VolumeComboRow<T extends ProcessStep>
	{
		final JComboBox<String> combo;
		final Function<T, String> getter;
		final BiConsumer<T, String> setter;
		@SuppressWarnings("unused")
		final Volume.Type[] volumeTypes;

		VolumeComboRow(JComboBox<String> combo, Function<T, String> getter, BiConsumer<T, String> setter,
			Volume.Type[] volumeTypes)
		{
			this.combo = combo;
			this.getter = getter;
			this.setter = setter;
			this.volumeTypes = volumeTypes;
		}
	}

	/*-------------------------------------------------------------------------*/
	/** Package-local hooks for tests. */

	JPanel getFormForTest()
	{
		return form;
	}

	JComboBox<String> getInputVolumeComboForTest(int index)
	{
		return volumeRows.get(index).combo;
	}

	T getStepForTest()
	{
		return step;
	}

	JToolBar getStepToolbarForTest()
	{
		return stepToolbar;
	}
}
