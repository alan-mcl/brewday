package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.batch.BatchVolumeEstimate;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.inventory.InventoryFacade;
import mclachlan.brewday.inventory.InventoryLineItem;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.Volumes;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingDocumentGeneration;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.widgets.SwingRecipeBillOfMaterialsPanel;
import org.jdatepicker.JDatePicker;
import org.jdatepicker.LocalDateModel;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Application-modal batch editor (draft {@link Batch} with OK/Cancel apply semantics).
 * Consume/undo inventory is an exception: it mutates the inventory silo and live
 * {@code inventoryConsumed} immediately.
 */
public class SwingBatchEditorDialog extends JDialog
{
	private static final int COL_MEAS = 4;

	/** Preferred dialog size; clamped to available screen in constructor. */
	private static final int PREFERRED_WIDTH = 1400;
	private static final int PREFERRED_HEIGHT = 880;
	/** Minimum size; each dimension is capped to screen when smaller displays. */
	private static final int MIN_WIDTH = 960;
	private static final int MIN_HEIGHT = 600;

	private final DirtyStateService dirtyState;
	private final Batch liveBatch;
	private final Batch draft;
	private final MeasurementsTableModel measurementsModel;
	private final JTable measurementsTable;
	private final JCheckBox keyOnlyCheck;
	private final LocalDateModel dateModel;
	private final JDatePicker datePicker;
	private final JComboBox<String> recipeCombo;
	private final JTextArea batchNotes;
	private final JTextArea analysis;
	private final JToggleButton consumeToggle;
	private final SwingRecipeBillOfMaterialsPanel recipeBom;
	private final Action okAction;
	private final Action cancelAction;
	private boolean dismissedCleanly;
	private boolean detectDirty = true;
	private boolean suppressConsumeHandler;
	private boolean suppressRecipeHandler;

	public SwingBatchEditorDialog(JFrame owner, DirtyStateService dirtyState, Batch liveBatch)
	{
		super(owner, getUiString("batch.edit.action"), true);
		this.dirtyState = dirtyState;
		this.liveBatch = liveBatch;
		this.draft = new Batch(liveBatch);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (!dismissedCleanly)
				{
					removeDraftFromDirty();
					dismissedCleanly = true;
				}
			}
		});

		measurementsModel = new MeasurementsTableModel();
		measurementsTable = new JTable(measurementsModel);
		measurementsTable.setAutoCreateRowSorter(true);
		measurementsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		measurementsTable.getColumnModel().getColumn(COL_MEAS).setPreferredWidth(140);
		measurementsTable.getColumnModel().getColumn(COL_MEAS).setCellEditor(new DefaultCellEditor(new JTextField()));

		keyOnlyCheck = new JCheckBox(getUiString("batch.measurements.key.only"));
		keyOnlyCheck.setSelected(true);
		keyOnlyCheck.addActionListener(e -> measurementsModel.setKeyOnly(keyOnlyCheck.isSelected()));

		dateModel = new LocalDateModel(draft.getDate() != null ? draft.getDate() : LocalDate.now());
		datePicker = new JDatePicker(dateModel);
		datePicker.setTextfieldColumns(12);

		recipeCombo = new JComboBox<>();
		suppressRecipeHandler = true;
		for (String r : sortedRecipeNames())
		{
			recipeCombo.addItem(r);
		}
		recipeCombo.setSelectedItem(draft.getRecipe());
		suppressRecipeHandler = false;

		batchNotes = new JTextArea(7, 36);
		batchNotes.setLineWrap(true);
		batchNotes.setWrapStyleWord(true);
		batchNotes.setText(draft.getDescription() == null ? "" : draft.getDescription());

		analysis = new JTextArea(15, 44);
		analysis.setEditable(false);
		analysis.setLineWrap(true);
		analysis.setWrapStyleWord(true);

		boolean consumed = draft.isInventoryConsumed();
		consumeToggle = new JToggleButton(
			consumed ? getUiString("batch.consume.inventory.undo") : getUiString("batch.consume.inventory"),
			SwingIcons.toolbarIcon(SwingIcons.IconKey.INVENTORY));
		consumeToggle.setSelected(consumed);
		consumeToggle.setToolTipText(getUiString("batch.consume.inventory.tooltip"));

		JButton genDocButton = new JButton(getUiString("doc.gen.generate.document"),
			SwingIcons.toolbarIcon(SwingIcons.IconKey.RECIPE));
		genDocButton.setToolTipText(getUiString("batch.docgen.tooltip"));
		genDocButton.addActionListener(e ->
		{
			Recipe r = Database.getInstance().getRecipes().get(draft.getRecipe());
			if (r != null)
			{
				SwingDocumentGeneration.run(this, r);
			}
		});

		batchNotes.setToolTipText(
			getUiString("batch.tooltip.notes") + " " + getUiString("batch.tooltip.notes.substitutions"));
		analysis.setToolTipText(getUiString("ui.readonly.copy.tooltip"));

		recipeBom = new SwingRecipeBillOfMaterialsPanel();

		JPanel left = new JPanel(new GridBagLayout());
		left.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 6));
		GridBagConstraints g = new GridBagConstraints();
		g.insets = new Insets(3, 4, 3, 4);
		g.anchor = GridBagConstraints.WEST;
		g.gridx = 0;
		g.gridy = 0;
		left.add(new JLabel(getUiString("batch.date") + ":"), g);
		g.gridx = 1;
		g.fill = GridBagConstraints.HORIZONTAL;
		g.weightx = 1.0;
		left.add(datePicker, g);
		g.gridx = 0;
		g.gridy = 1;
		g.fill = GridBagConstraints.NONE;
		g.weightx = 0;
		left.add(new JLabel(getUiString("batch.recipe") + ":"), g);
		g.gridx = 1;
		g.fill = GridBagConstraints.HORIZONTAL;
		g.weightx = 1.0;
		left.add(recipeCombo, g);

		g.gridx = 0;
		g.gridy = 2;
		g.gridwidth = 2;
		g.fill = GridBagConstraints.HORIZONTAL;
		JPanel invRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		invRow.add(consumeToggle);
		invRow.add(genDocButton);
		left.add(invRow, g);

		g.gridx = 0;
		g.gridy = 3;
		g.gridwidth = 2;
		g.fill = GridBagConstraints.HORIZONTAL;
		g.weightx = 1.0;
		g.weighty = 0;
		g.anchor = GridBagConstraints.WEST;
		g.insets = new Insets(6, 4, 2, 4);
		left.add(new JLabel(getUiString("batch.desc") + ":"), g);

		g.gridy = 4;
		g.fill = GridBagConstraints.BOTH;
		g.weighty = 1.0;
		g.insets = new Insets(0, 4, 3, 4);
		left.add(new JScrollPane(batchNotes), g);

		g.gridy = 5;
		g.fill = GridBagConstraints.HORIZONTAL;
		g.weighty = 0;
		g.insets = new Insets(3, 4, 2, 4);
		left.add(new JLabel(getUiString("batch.analysis")), g);

		g.gridy = 6;
		g.fill = GridBagConstraints.BOTH;
		g.weighty = 1.0;
		g.insets = new Insets(0, 4, 3, 4);
		left.add(new JScrollPane(analysis), g);

		JPanel metricsTab = new JPanel(new BorderLayout(4, 4));
		metricsTab.add(keyOnlyCheck, BorderLayout.NORTH);
		metricsTab.add(new JScrollPane(measurementsTable), BorderLayout.CENTER);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab(getUiString("batch.tab.metrics"), metricsTab);
		tabs.addTab(getUiString("batch.tab.recipe"), recipeBom);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, tabs);
		split.setResizeWeight(0.38);

		okAction = commandAction("ui.ok", "batch.editor.ok.action", SwingIcons.IconKey.EDIT, this::onOkClicked);
		cancelAction = commandAction("ui.cancel", "batch.editor.cancel.action", SwingIcons.IconKey.DELETE, this::onCancelClicked);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
		JButton okButton = new JButton(okAction);
		south.add(okButton);
		south.add(new JButton(cancelAction));

		getContentPane().add(split, BorderLayout.CENTER);
		getContentPane().add(south, BorderLayout.SOUTH);

		dateModel.addChangeListener(e ->
		{
			LocalDate ld = dateModel.getValue();
			if (ld != null && detectDirty && !ld.equals(draft.getDate()))
			{
				draft.setDate(ld);
				dirtyState.markDirty(draft, "batches");
			}
		});

		recipeCombo.addActionListener(e ->
		{
			if (suppressRecipeHandler)
			{
				return;
			}
			String sel = (String)recipeCombo.getSelectedItem();
			if (sel != null && !sel.equals(draft.getRecipe()))
			{
				draft.setRecipe(sel);
				if (detectDirty)
				{
					dirtyState.markDirty(draft, "batches");
				}
				reloadMeasurementsAndAnalysisAndBom();
			}
		});

		batchNotes.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				doc();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				doc();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				doc();
			}

			private void doc()
			{
				if (!detectDirty)
				{
					return;
				}
				draft.setDescription(batchNotes.getText());
				dirtyState.markDirty(draft, "batches");
			}
		});

		consumeToggle.addActionListener(e ->
		{
			if (suppressConsumeHandler)
			{
				return;
			}
			boolean want = consumeToggle.isSelected();
			if (want == draft.isInventoryConsumed())
			{
				return;
			}
			boolean consume = want;
			SwingBatchInventoryDeltaDialog dlg = new SwingBatchInventoryDeltaDialog(this,
				draft.getRecipe(), consume);
			dlg.setVisible(true);
			if (!dlg.isAccepted())
			{
				suppressConsumeHandler = true;
				consumeToggle.setSelected(!want);
				suppressConsumeHandler = false;
				return;
			}
			List<InventoryFacade.InventoryLineItemDelta> deltas =
				InventoryFacade.getInventoryDelta(draft.getRecipe(), true);
			boolean inventoryMutated;
			if (consume)
			{
				inventoryMutated = InventoryFacade.consumeInventory(deltas);
				liveBatch.setInventoryConsumed(true);
				draft.setInventoryConsumed(true);
				consumeToggle.setText(getUiString("batch.consume.inventory.undo"));
			}
			else
			{
				inventoryMutated = InventoryFacade.restoreInventory(deltas);
				liveBatch.setInventoryConsumed(false);
				draft.setInventoryConsumed(false);
				consumeToggle.setText(getUiString("batch.consume.inventory"));
			}
			if (inventoryMutated)
			{
				for (InventoryFacade.InventoryLineItemDelta ilid : deltas)
				{
					InventoryLineItem ili = Database.getInstance().getInventory().get(ilid.getInventoryId());
					if (ili != null)
					{
						dirtyState.markDirty(ili, "inventory");
					}
				}
				dirtyState.markDirty("inventory");
			}
		});

		wireHotkeys();
		getRootPane().setDefaultButton(okButton);

		applyDialogSizeAndMinimum();
		setLocationRelativeTo(owner);
		SwingUtilities.invokeLater(() -> split.setDividerLocation(0.38));

		detectDirty = false;
		reloadMeasurementsAndAnalysisAndBom();
		detectDirty = true;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Sets minimum size (capped on very small screens) and preferred size clamped
	 * to {@link GraphicsEnvironment#getMaximumWindowBounds()}.
	 */
	private void applyDialogSizeAndMinimum()
	{
		Rectangle max = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int margin = 24;
		Dimension minDim = new Dimension(
			Math.min(MIN_WIDTH, Math.max(1, max.width - margin)),
			Math.min(MIN_HEIGHT, Math.max(1, max.height - margin)));
		setMinimumSize(minDim);

		int w = Math.min(PREFERRED_WIDTH, max.width - margin);
		int h = Math.min(PREFERRED_HEIGHT, max.height - margin);
		w = Math.max(w, minDim.width);
		h = Math.max(h, minDim.height);
		w = Math.min(w, max.width);
		h = Math.min(h, max.height);
		setSize(w, h);
	}

	/*-------------------------------------------------------------------------*/
	private void onOkClicked()
	{
		removeDraftFromDirty();
		applyDraftToLive();
		dismissedCleanly = true;
		dispose();
	}

	private void onCancelClicked()
	{
		onCancel();
	}

	private void onCancel()
	{
		if (!dismissedCleanly)
		{
			removeDraftFromDirty();
			dismissedCleanly = true;
		}
		dispose();
	}

	private void removeDraftFromDirty()
	{
		dirtyState.removeDirty(draft);
	}

	private void applyDraftToLive()
	{
		liveBatch.setDescription(draft.getDescription());
		liveBatch.setRecipe(draft.getRecipe());
		liveBatch.setDate(draft.getDate());
		liveBatch.setActualVolumes(new Volumes(draft.getActualVolumes()));
		liveBatch.setInventoryConsumed(draft.isInventoryConsumed());
		dirtyState.markDirty(liveBatch, "batches");
	}

	private void wireHotkeys()
	{
		ActionHotkeySupport.applyTooltipText(okAction, "ui.ok.tooltip");
		ActionHotkeySupport.applyTooltipText(cancelAction, "ui.cancel.tooltip");
		ActionHotkeySupport.bind(getRootPane(), ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_ENTER),
			"batchEditor.hotkey.ok", okAction);
		ActionHotkeySupport.bind(getRootPane(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			"batchEditor.hotkey.cancel", cancelAction);
	}

	private static Action commandAction(String key, String actionKey, SwingIcons.IconKey iconKey, Runnable runnable)
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

	private static List<String> sortedRecipeNames()
	{
		ArrayList<String> recipes = new ArrayList<>(Database.getInstance().getRecipes().keySet());
		Collections.sort(recipes);
		return recipes;
	}

	private void reloadMeasurementsAndAnalysisAndBom()
	{
		List<BatchVolumeEstimate> est = Brewday.getInstance().getBatchVolumeEstimates(draft);
		measurementsModel.setSourceRows(est);
		measurementsModel.setKeyOnly(keyOnlyCheck.isSelected());
		refreshBatchAnalysis();
		Recipe r = Database.getInstance().getRecipes().get(draft.getRecipe());
		recipeBom.refresh(r);
	}

	private void refreshBatchAnalysis()
	{
		StringBuilder sb = new StringBuilder();
		for (String s : Brewday.getInstance().getBatchAnalysis(draft))
		{
			sb.append(s).append('\n');
		}
		analysis.setText(sb.toString());
	}

	private final class MeasurementsTableModel extends AbstractTableModel
	{
		private List<BatchVolumeEstimate> rows = new ArrayList<>();
		private boolean keyOnly = true;

		void setSourceRows(List<BatchVolumeEstimate> list)
		{
			this.rows = list != null ? list : new ArrayList<>();
			fireTableDataChanged();
		}

		void setKeyOnly(boolean keyOnly)
		{
			this.keyOnly = keyOnly;
			fireTableDataChanged();
		}

		private List<BatchVolumeEstimate> visible()
		{
			ArrayList<BatchVolumeEstimate> out = new ArrayList<>();
			for (BatchVolumeEstimate b : rows)
			{
				if (!keyOnly || b.isKey())
				{
					out.add(b);
				}
			}
			return out;
		}

		@Override
		public int getRowCount()
		{
			return visible().size();
		}

		@Override
		public int getColumnCount()
		{
			return 5;
		}

		@Override
		public String getColumnName(int column)
		{
			return switch (column)
			{
				case 0 -> getUiString("batch.measurements.volume");
				case 1 -> getUiString("batch.measurements.volume.type");
				case 2 -> getUiString("batch.measurements.metric");
				case 3 -> getUiString("batch.measurements.estimate");
				case 4 -> getUiString("batch.measurements.measurement");
				default -> "";
			};
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			return columnIndex == COL_MEAS ? String.class : Object.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == COL_MEAS;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			BatchVolumeEstimate b = visible().get(rowIndex);
			return switch (columnIndex)
			{
				case 0 -> b.getVolumeName();
				case 1 -> b.getType().toString();
				case 2 -> b.getMetric();
				case 3 -> formatQuantity(b.getEstimated(), true);
				case 4 -> formatQuantity(b.getMeasured(), false);
				default -> "";
			};
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			if (columnIndex != COL_MEAS || !(aValue instanceof String text))
			{
				return;
			}
			BatchVolumeEstimate bve = visible().get(rowIndex);
			Quantity q = parseMeasured(text.trim(), bve);
			if (q != null)
			{
				q.setEstimated(false);
			}
			bve.setMeasured(q);
			if (detectDirty)
			{
				refreshBatchAnalysis();
				dirtyState.markDirty(draft, "batches");
			}
			fireTableRowsUpdated(rowIndex, rowIndex);
		}
	}

	private static String formatQuantity(Quantity quantity, boolean displayEstimates)
	{
		if (quantity == null || (!displayEstimates && quantity.isEstimated()))
		{
			return getUiString("quantity.unknown");
		}
		else if (quantity instanceof TemperatureUnit tu)
		{
			return getUiString("quantity.celsius", tu.get(Quantity.Unit.CELSIUS));
		}
		else if (quantity instanceof VolumeUnit vu)
		{
			return getUiString("quantity.litre", vu.get(Quantity.Unit.LITRES));
		}
		else if (quantity instanceof WeightUnit wu)
		{
			return getUiString("quantity.kilogram", wu.get(Quantity.Unit.KILOGRAMS));
		}
		else if (quantity instanceof DensityUnit du)
		{
			return getUiString("quantity.sg", du.get(Quantity.Unit.SPECIFIC_GRAVITY));
		}
		else if (quantity instanceof ColourUnit cu)
		{
			return getUiString("quantity.srm", cu.get(Quantity.Unit.SRM));
		}
		else
		{
			throw new BrewdayException("Invalid quantity type:" + quantity);
		}
	}

	private static Quantity parseMeasured(String quantityString, BatchVolumeEstimate estimate)
	{
		if (quantityString.isEmpty())
		{
			return null;
		}
		Quantity.Unit hint = null;
		if (estimate.getMeasured() != null)
		{
			if (estimate.getMeasured().getType() == Quantity.Type.VOLUME)
			{
				hint = Quantity.Unit.LITRES;
			}
			else if (estimate.getMeasured().getType() == Quantity.Type.TEMPERATURE)
			{
				hint = Quantity.Unit.CELSIUS;
			}
			else if (estimate.getMeasured().getType() == Quantity.Type.FLUID_DENSITY)
			{
				hint = Quantity.Unit.SPECIFIC_GRAVITY;
			}
			else if (estimate.getMeasured().getType() == Quantity.Type.COLOUR)
			{
				hint = Quantity.Unit.SRM;
			}
		}
		return Brewday.getInstance().parseQuantity(quantityString, hint);
	}
}
