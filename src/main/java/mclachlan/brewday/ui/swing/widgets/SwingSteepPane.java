package mclachlan.brewday.ui.swing.widgets;

import java.awt.FlowLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Steep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingSteepPane extends SwingProcessStepPane<Steep>
{
	private static final double K_SEALED_FERMENTER = 0.035D;
	private static final double K_COVERED_KETTLE = 0.1D;
	private static final double K_OPEN_HOT_WORT = 0.3D;
	private static final double K_REHYDRATION_FLASK = 0.1D;
	private static final double K_ICE_BATH = 1.0D;

	private static final double[] PRESET_K =
	{
		K_SEALED_FERMENTER,
		K_COVERED_KETTLE,
		K_OPEN_HOT_WORT,
		K_REHYDRATION_FLASK,
		K_ICE_BATH
	};

	private JTextField coolingCoefficientField;
	private JComboBox<String> coolingPresets;
	private boolean applyingPreset;

	public SwingSteepPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addInputVolumeComboBox("volumes.in", Steep::getInputVolume, Steep::setInputVolume,
			Volume.Type.WORT);
		addTimeUnitControl("steep.duration", Steep::getDuration, Steep::setDuration, Quantity.Unit.MINUTES);

		coolingCoefficientField = new JTextField(8);
		coolingCoefficientField.setToolTipText(getUiString("stand.cooling.coefficient.tooltip"));
		coolingPresets = new JComboBox<>(new String[]
		{
			getUiString("stand.cooling.preset.sealed.fermenter"),
			getUiString("stand.cooling.preset.covered.kettle"),
			getUiString("stand.cooling.preset.open.hot.wort"),
			getUiString("stand.cooling.preset.rehydration.flask"),
			getUiString("stand.cooling.preset.ice.bath")
		});
		coolingPresets.setToolTipText(getUiString("stand.cooling.preset.tooltip"));
		coolingPresets.addActionListener(e -> applyCoolingPreset());

		JPanel coolingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		coolingRow.add(coolingCoefficientField);
		coolingRow.add(coolingPresets);
		addLabeledWidgetToForm("stand.cooling.coefficient", coolingRow);

		coolingCoefficientField.addActionListener(e -> commitCoolingCoefficient());
		coolingCoefficientField.addFocusListener(new java.awt.event.FocusAdapter()
		{
			@Override
			public void focusLost(java.awt.event.FocusEvent e)
			{
				commitCoolingCoefficient();
			}
		});

		addAddIngredientButton(IngredientAddition.Type.WATER);
		addAddIngredientButton(IngredientAddition.Type.FERMENTABLES);
		addAddIngredientButton(IngredientAddition.Type.MISC);
		addComputedVolumePane("volumes.out", Steep::getOutputVolume);
	}

	@Override
	protected void refreshInternal(Steep step, Recipe recipe)
	{
		if (step != null && coolingCoefficientField != null)
		{
			coolingCoefficientField.setText(formatCoolingK(step.getCoolingCoefficient()));
			syncCoolingPresetSelection(step.getCoolingCoefficient());
		}
	}

	/*-------------------------------------------------------------------------*/
	private void applyCoolingPreset()
	{
		if (isStepPaneRefreshing() || applyingPreset)
		{
			return;
		}
		int idx = coolingPresets.getSelectedIndex();
		if (idx < 0 || idx >= PRESET_K.length)
		{
			return;
		}
		applyingPreset = true;
		try
		{
			coolingCoefficientField.setText(formatCoolingK(PRESET_K[idx]));
			commitCoolingCoefficient();
		}
		finally
		{
			applyingPreset = false;
		}
	}

	/*-------------------------------------------------------------------------*/
	private void commitCoolingCoefficient()
	{
		if (isStepPaneRefreshing() || applyingPreset)
		{
			return;
		}
		Steep s = getStepForTest();
		if (s == null)
		{
			return;
		}
		try
		{
			double k = Double.parseDouble(coolingCoefficientField.getText().trim());
			if (k != s.getCoolingCoefficient())
			{
				s.setCoolingCoefficient(k);
				dirtyState.markDirty(s);
			}
			syncCoolingPresetSelection(k);
		}
		catch (NumberFormatException ignored)
		{
			coolingCoefficientField.setText(formatCoolingK(s.getCoolingCoefficient()));
		}
	}

	/*-------------------------------------------------------------------------*/
	private void syncCoolingPresetSelection(double k)
	{
		for (int i = 0; i < PRESET_K.length; i++)
		{
			if (Math.abs(PRESET_K[i] - k) < 1e-6)
			{
				if (coolingPresets.getSelectedIndex() != i)
				{
					applyingPreset = true;
					try
					{
						coolingPresets.setSelectedIndex(i);
					}
					finally
					{
						applyingPreset = false;
					}
				}
				return;
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private static String formatCoolingK(double k)
	{
		if (k == Equations.DEFAULT_STAND_COOLING_COEFFICIENT)
		{
			return "0.1";
		}
		return String.valueOf(k);
	}
}
