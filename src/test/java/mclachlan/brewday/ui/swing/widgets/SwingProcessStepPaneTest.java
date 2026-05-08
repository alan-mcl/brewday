package mclachlan.brewday.ui.swing.widgets;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Heat;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Stand;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SwingProcessStepPaneTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	static final class TestStepPane extends SwingProcessStepPane<Heat>
	{
		TestStepPane(DirtyStateService d, SwingRecipeTree t)
		{
			super(d, t, false);
		}

		@Override
		protected void buildUiInternal()
		{
			addInputVolumeComboBox("volumes.in", Heat::getInputVolume, Heat::setInputVolume);
			addTemperatureUnitControl("heat.target.temp", Heat::getTargetTemp, Heat::setTargetTemp, Quantity.Unit.CELSIUS);
		}
	}

	@Test
	public void inputVolumeComboPopulatesFromRecipeVolumeNames() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		Recipe r = new Recipe("PaneVol");
		Heat h1 = (Heat)RecipeEditorSteps.createStep(r, ProcessStep.Type.HEAT);
		Heat h2 = (Heat)RecipeEditorSteps.createStep(r, ProcessStep.Type.HEAT);
		r.getSteps().add(h1);
		r.getSteps().add(h2);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		TestStepPane pane = new TestStepPane(dirty, tree);
		Heat heat = (Heat)RecipeEditorSteps.createStep(r, ProcessStep.Type.HEAT);

		SwingUtilities.invokeAndWait(() -> pane.refresh(heat, r));

		JComboBox<String> cb = pane.getInputVolumeComboForTest(0);
		List<String> names = new ArrayList<>(r.getAllVolumeNames());
		Collections.sort(names);
		assertEquals(UiUtils.NONE, cb.getItemAt(0));
		for (int i = 0; i < names.size(); i++)
		{
			assertEquals(names.get(i), cb.getItemAt(i + 1));
		}
	}

	@Test
	public void selectingNoneInComboSetsStepFieldToNull() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		Recipe r = new Recipe("PaneNone");
		Heat h1 = (Heat)RecipeEditorSteps.createStep(r, ProcessStep.Type.HEAT);
		r.getSteps().add(h1);
		List<String> names = new ArrayList<>(r.getAllVolumeNames());
		Collections.sort(names);
		assertFalse(names.isEmpty());
		String firstVol = names.get(0);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		TestStepPane pane = new TestStepPane(dirty, tree);
		Heat heat = (Heat)RecipeEditorSteps.createStep(r, ProcessStep.Type.HEAT);
		heat.setInputVolume(firstVol);

		SwingUtilities.invokeAndWait(() -> pane.refresh(heat, r));
		dirty.removeDirty(heat);

		SwingUtilities.invokeAndWait(() ->
		{
			pane.getInputVolumeComboForTest(0).setSelectedItem(UiUtils.NONE);
		});

		SwingUtilities.invokeAndWait(() ->
		{
			assertNull(heat.getInputVolume());
			assertTrue(dirty.isDirty(heat));
		});
	}

	@Test
	public void editingTempWidgetUpdatesStepAndMarksDirty() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		Recipe r = new Recipe("PaneTemp");
		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		TestStepPane pane = new TestStepPane(dirty, tree);
		Heat heat = (Heat)RecipeEditorSteps.createStep(r, ProcessStep.Type.HEAT);

		SwingUtilities.invokeAndWait(() -> pane.refresh(heat, r));
		dirty.removeDirty(heat);

		SwingUtilities.invokeAndWait(() ->
		{
			SwingQuantityEditWidget<?> w = findFirstQuantityWidget(pane.getFormForTest());
			assertNotNull(w);
			w.getTextField().setText("33");
			w.getTextField().transferFocus();
		});

		SwingUtilities.invokeAndWait(() ->
		{
			assertNotNull(heat.getTargetTemp());
			assertEquals(33.0, heat.getTargetTemp().get(Quantity.Unit.CELSIUS), 0.001);
			assertTrue(dirty.isDirty(heat));
		});
	}

	@Test
	public void refreshDuringSetupDoesNotMarkDirty() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		Recipe r = new Recipe("PaneRd");
		Heat h1 = (Heat)RecipeEditorSteps.createStep(r, ProcessStep.Type.HEAT);
		r.getSteps().add(h1);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		TestStepPane pane = new TestStepPane(dirty, tree);
		Heat heat = (Heat)RecipeEditorSteps.createStep(r, ProcessStep.Type.HEAT);

		SwingUtilities.invokeAndWait(() ->
		{
			pane.refresh(heat, r);
			pane.refresh(heat, r);
		});

		SwingUtilities.invokeAndWait(() -> assertFalse(dirty.isDirty(heat)));
	}

	@Test
	public void standPaneToolbarHasAddHopAndAddWaterButtons() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		Recipe r = new Recipe("PaneToolbar");
		Stand stand = (Stand)RecipeEditorSteps.createStep(r, ProcessStep.Type.STAND);
		r.getSteps().add(stand);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		SwingStandPane pane = new SwingStandPane(dirty, tree, false);

		SwingUtilities.invokeAndWait(() -> pane.refresh(stand, r));

		SwingUtilities.invokeAndWait(() ->
			assertEquals(2, pane.getStepToolbarForTest().getComponentCount()));
	}

	@Test
	public void commitIngredientAdditionForTestAddsToStepAndTree() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		Assume.assumeFalse(Database.getInstance().getHops().isEmpty());

		Hop hop = Database.getInstance().getHops().values().iterator().next();
		Recipe r = new Recipe("PaneCommit");
		Stand stand = (Stand)RecipeEditorSteps.createStep(r, ProcessStep.Type.STAND);
		r.getSteps().add(stand);

		DirtyStateService dirty = new DirtyStateService();
		SwingRecipeTree tree = new SwingRecipeTree(dirty);
		tree.setRecipe(r);
		SwingStandPane pane = new SwingStandPane(dirty, tree, false);

		SwingUtilities.invokeAndWait(() -> pane.refresh(stand, r));

		HopAddition ha = new HopAddition(hop,
			Quantity.parseQuantity("7", Quantity.Unit.GRAMS),
			Quantity.Unit.GRAMS,
			new mclachlan.brewday.math.TimeUnit(1, MINUTES, false));

		SwingUtilities.invokeAndWait(() -> pane.commitIngredientAdditionForTest(ha));

		SwingUtilities.invokeAndWait(() ->
		{
			assertTrue(stand.getIngredientAdditions().contains(ha));
			assertTrue(dirty.isDirty(ha));
		});
	}

	private static SwingQuantityEditWidget<?> findFirstQuantityWidget(JPanel form)
	{
		for (java.awt.Component c : form.getComponents())
		{
			if (c instanceof SwingQuantityEditWidget<?> w)
			{
				return w;
			}
		}
		return null;
	}
}
