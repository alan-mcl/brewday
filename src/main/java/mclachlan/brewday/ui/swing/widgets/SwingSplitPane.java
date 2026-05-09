package mclachlan.brewday.ui.swing.widgets;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.FlowLayout;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.Split;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code SplitPane}.
 */
public class SwingSplitPane extends SwingProcessStepPane<Split>
{
	private JRadioButton byPerc;
	private JRadioButton byVol;
	private SwingQuantityEditWidget<PercentageUnit> splitPercent;
	private SwingQuantityEditWidget<VolumeUnit> splitVolume;

	public SwingSplitPane(DirtyStateService dirtyState, SwingRecipeTree recipeTree, boolean processTemplateMode)
	{
		super(dirtyState, recipeTree, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addIngredientButtonsForPrototype(new Split());

		addInputVolumeComboBox("volumes.in",
			Split::getInputVolume,
			Split::setInputVolume,
			Volume.Type.BEER, Volume.Type.WORT, Volume.Type.MASH);

		byPerc = new JRadioButton(getUiString("split.by.percentage"));
		byVol = new JRadioButton(getUiString("split.by.volume"));
		ButtonGroup tg = new ButtonGroup();
		tg.add(byPerc);
		tg.add(byVol);

		splitPercent = new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
		p1.add(byPerc);
		p1.add(splitPercent);
		addFullWidthComponentRow(p1);

		splitVolume = new SwingQuantityEditWidget<>(Quantity.Unit.LITRES);
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
		p2.add(byVol);
		p2.add(splitVolume);
		addFullWidthComponentRow(p2);

		splitPercent.addQuantityChangeListener(v ->
		{
			Split s = getStepForTest();
			if (s != null && v != null && !isStepPaneRefreshing())
			{
				s.setSplitPercent(v);
				dirtyState.markDirty(s);
			}
		});

		splitVolume.addQuantityChangeListener(v ->
		{
			Split s = getStepForTest();
			if (s != null && v != null && !isStepPaneRefreshing())
			{
				s.setSplitVolume(v);
				dirtyState.markDirty(s);
			}
		});

		byPerc.addActionListener(e ->
		{
			Split s = getStepForTest();
			if (s != null)
			{
				splitPercent.setEditable(true);
				splitVolume.setEditable(false);
				if (!isStepPaneRefreshing())
				{
					s.setSplitType(Split.Type.PERCENTAGE);
					s.setSplitPercent(splitPercent.getQuantity());
					dirtyState.markDirty(s);
				}
			}
		});

		byVol.addActionListener(e ->
		{
			splitPercent.setEditable(false);
			splitVolume.setEditable(true);
			Split s = getStepForTest();
			if (s != null && !isStepPaneRefreshing())
			{
				s.setSplitType(Split.Type.ABSOLUTE);
				s.setSplitVolume(splitVolume.getQuantity());
				dirtyState.markDirty(s);
			}
		});

		addComputedVolumePane("volumes.out.1", Split::getOutputVolume);
		addComputedVolumePane("volumes.out.2", Split::getOutputVolume2);
	}

	@Override
	protected void refreshInternal(Split step, Recipe recipe)
	{
		if (step == null)
		{
			return;
		}
		switch (step.getSplitType())
		{
			case PERCENTAGE:
				byPerc.setSelected(true);
				splitPercent.setEditable(true);
				splitVolume.setEditable(false);
				break;
			case ABSOLUTE:
				byVol.setSelected(true);
				splitPercent.setEditable(false);
				splitVolume.setEditable(true);
				break;
			default:
				throw new BrewdayException("Invalid " + step.getSplitType());
		}
		if (step.getSplitPercent() != null)
		{
			splitPercent.setQuantity(step.getSplitPercent());
		}
		if (step.getSplitVolume() != null)
		{
			splitVolume.setQuantity(step.getSplitVolume());
		}
	}
}
