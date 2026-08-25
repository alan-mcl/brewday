package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingDocumentGeneration;
import mclachlan.brewday.ui.swing.app.SwingIcons;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing port of JFX {@code RecipeInfoPane} (minus deferred apply-process-template when no callback).
 */
public class SwingRecipeInfoPanel extends JPanel
{
	private final JFrame parent;
	private final DirtyStateService dirtyState;
	private final Runnable onRerun;
	private final Runnable onAddStep;
	private final Runnable onRecipeDirty;
	private final boolean emitNavDirtyTokens;
	private final Runnable onApplyProcessTemplate;

	private Recipe recipe;
	private boolean refreshing;

	private final JLabel recipeName = new JLabel();
	private final JTextArea recipeDescription = new JTextArea(6, 32);
	private final JComboBox<String> equipmentProfile = new JComboBox<>();
	private final SwingTagBarWidget tagBar = new SwingTagBarWidget();
	private final JButton genDocButton = new JButton();
	private final JButton applyTemplateButton = new JButton();
	private final JButton addStepButton = new JButton();
	private final JButton rerunButton = new JButton();

	public SwingRecipeInfoPanel(JFrame parent, DirtyStateService dirtyState, Runnable onRecipeDirty,
		Runnable onRerun, Runnable onAddStep)
	{
		this(parent, dirtyState, onRecipeDirty, onRerun, onAddStep, true, null);
	}

	public SwingRecipeInfoPanel(JFrame parent, DirtyStateService dirtyState, Runnable onRecipeDirty,
		Runnable onRerun, Runnable onAddStep, boolean emitNavDirtyTokens)
	{
		this(parent, dirtyState, onRecipeDirty, onRerun, onAddStep, emitNavDirtyTokens, null);
	}

	public SwingRecipeInfoPanel(JFrame parent, DirtyStateService dirtyState, Runnable onRecipeDirty,
		Runnable onRerun, Runnable onAddStep, boolean emitNavDirtyTokens, Runnable onApplyProcessTemplate)
	{
		super(new BorderLayout(6, 6));
		this.parent = parent;
		this.dirtyState = dirtyState;
		this.onRecipeDirty = onRecipeDirty;
		this.onRerun = onRerun;
		this.onAddStep = onAddStep;
		this.emitNavDirtyTokens = emitNavDirtyTokens;
		this.onApplyProcessTemplate = onApplyProcessTemplate;

		JPanel northBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		addStepButton.setText(getUiString("recipe.add.step"));
		addStepButton.setIcon(SwingIcons.toolbarIcon(SwingIcons.IconKey.ADD_STEP));
		addStepButton.setToolTipText(getUiString("recipe.editor.add.step.info.tooltip"));
		rerunButton.setText(getUiString("recipe.rerun"));
		rerunButton.setIcon(SwingIcons.toolbarIcon(SwingIcons.IconKey.RECIPE));
		rerunButton.setToolTipText(getUiString("recipe.editor.rerun.tooltip"));
		addStepButton.addActionListener(e -> onAddStep.run());
		rerunButton.addActionListener(e -> onRerun.run());
		northBar.add(addStepButton);
		northBar.add(rerunButton);
		add(northBar, BorderLayout.NORTH);

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 4, 3, 4);
		gbc.anchor = GridBagConstraints.NORTHWEST;

		gbc.gridx = 0;
		gbc.gridy = 0;
		form.add(new JLabel(getUiString("recipe.name") + ":"), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		form.add(recipeName, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		form.add(new JLabel(getUiString("recipe.equipment.profile") + ":"), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		equipmentProfile.setToolTipText(getUiString("recipe.editor.equipment.profile.tooltip"));
		form.add(equipmentProfile, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.weighty = 0;
		form.add(new JLabel(getUiString("recipe.tags") + ":"), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		tagBar.setAddButtonText(getUiString("recipe.tag.add"));
		tagBar.setAddButtonTooltip(getUiString("recipe.tag.add.tooltip"));
		tagBar.setInputTooltip(getUiString("recipe.tag.input.tooltip"));
		form.add(tagBar, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		form.add(new JLabel(getUiString("recipe.desc") + ":"), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1.0;
		recipeDescription.setLineWrap(true);
		recipeDescription.setWrapStyleWord(true);
		recipeDescription.setToolTipText(getUiString("recipe.editor.description.tooltip"));
		form.add(new JScrollPane(recipeDescription), gbc);

		add(form, BorderLayout.CENTER);

		JPanel extras = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		genDocButton.setText(getUiString("doc.gen.generate.document"));
		genDocButton.setIcon(SwingIcons.toolbarIcon(SwingIcons.IconKey.RECIPE));
		genDocButton.setEnabled(false);
		genDocButton.setToolTipText(getUiString("recipe.editor.docgen.tooltip"));
		genDocButton.addActionListener(e ->
		{
			if (recipe != null)
			{
				SwingDocumentGeneration.run(parent, recipe);
			}
		});
		applyTemplateButton.setText(getUiString("recipe.apply.process.template"));
		applyTemplateButton.setIcon(SwingIcons.toolbarIcon(SwingIcons.IconKey.PROCESS_TEMPLATE_APPLY));
		if (onApplyProcessTemplate != null)
		{
			applyTemplateButton.setEnabled(true);
			applyTemplateButton.setToolTipText(getUiString("recipe.apply.process.template"));
			applyTemplateButton.addActionListener(e -> onApplyProcessTemplate.run());
		}
		else
		{
			applyTemplateButton.setEnabled(false);
			applyTemplateButton.setToolTipText(getUiString("recipe.editor.template.coming.soon"));
		}
		extras.add(genDocButton);
		extras.add(applyTemplateButton);
		add(extras, BorderLayout.SOUTH);

		equipmentProfile.addActionListener(e ->
		{
			if (recipe == null || refreshing)
			{
				return;
			}
			String v = (String)equipmentProfile.getSelectedItem();
			if (v != null)
			{
				recipe.setEquipmentProfile(v);
				markDirty();
			}
		});

		recipeDescription.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				docChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				docChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				docChanged();
			}

			private void docChanged()
			{
				if (recipe == null || refreshing)
				{
					return;
				}
				recipe.setDescription(recipeDescription.getText());
				markDirty();
			}
		});

		tagBar.setOnAdd(tag ->
		{
			if (recipe == null || refreshing)
			{
				return;
			}
			recipe.getTags().add(tag);
			markDirty();
		});
		tagBar.setOnRemove(tag ->
		{
			if (recipe == null || refreshing)
			{
				return;
			}
			recipe.getTags().remove(tag);
			markDirty();
		});
	}

	private void markDirty()
	{
		if (recipe != null)
		{
			if (emitNavDirtyTokens)
			{
				dirtyState.markDirty(recipe, "recipes");
			}
			else
			{
				dirtyState.markDirty(recipe);
			}
		}
		if (onRecipeDirty != null)
		{
			onRecipeDirty.run();
		}
	}

	public void refresh(Recipe recipe)
	{
		this.recipe = recipe;
		refreshing = true;
		if (recipe != null)
		{
			recipeName.setText(recipe.getName());
			recipeDescription.setText(recipe.getDescription() == null ? "" : recipe.getDescription());
			ArrayList<String> equipmentProfiles = new ArrayList<>(Database.getInstance().getEquipmentProfiles().keySet());
			equipmentProfiles.sort(String::compareTo);
			equipmentProfile.removeAllItems();
			for (String ep : equipmentProfiles)
			{
				equipmentProfile.addItem(ep);
			}
			String ep = recipe.getEquipmentProfile();
			if (ep != null && equipmentProfiles.contains(ep))
			{
				equipmentProfile.setSelectedItem(ep);
			}
			else if (!equipmentProfiles.isEmpty())
			{
				equipmentProfile.setSelectedIndex(0);
			}
			tagBar.setTags(recipe.getTags(), Brewday.getInstance().getRecipeTags());
			genDocButton.setEnabled(true);
		}
		else
		{
			genDocButton.setEnabled(false);
		}
		refreshing = false;
	}
}
