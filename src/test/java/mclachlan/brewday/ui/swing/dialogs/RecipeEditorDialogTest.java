package mclachlan.brewday.ui.swing.dialogs;

import java.awt.Font;

import java.awt.GraphicsEnvironment;

import java.util.LinkedHashMap;

import java.util.Map;

import javax.swing.JFrame;

import javax.swing.JTree;

import javax.swing.SwingUtilities;

import javax.swing.tree.TreePath;

import mclachlan.brewday.process.ProcessStep;

import mclachlan.brewday.recipe.Recipe;

import mclachlan.brewday.ui.UiUtils;

import mclachlan.brewday.ui.swing.widgets.SwingHeatPane;

import mclachlan.brewday.ui.swing.app.DirtyStateService;

import mclachlan.brewday.ui.swing.app.RecipeEditorNavPort;

import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;

import org.junit.Assume;

import org.junit.Test;

import static mclachlan.brewday.util.StringUtils.getUiString;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertFalse;

import static org.junit.Assert.assertTrue;

public class RecipeEditorDialogTest

{

	@Test

	public void setRecipePopulatesTreeLogEndResultDefaultInfoCard() throws Exception

	{

		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		FakeDbPort db = new FakeDbPort();

		Recipe live = new Recipe("EdShell");

		ProcessStep mash = RecipeEditorSteps.createStep(live, ProcessStep.Type.MASH_INFUSION);

		live.getSteps().add(mash);

		db.recipes.put("EdShell", live);

		RecordingNavPort nav = new RecordingNavPort();

		DirtyStateService dirty = new DirtyStateService();

		final RecipeEditorDialog[] holder = new RecipeEditorDialog[1];

		SwingUtilities.invokeAndWait(() ->

			holder[0] = new RecipeEditorDialog(new JFrame(), dirty, () -> {}, nav, live, db));

		RecipeEditorDialog editor = holder[0];

		SwingUtilities.invokeAndWait(() -> {});

		SwingUtilities.invokeAndWait(() ->

		{

			JTree tr = editor.getRecipeTree().getTree();

			tr.expandRow(0);

			assertEquals(2, tr.getRowCount());

			assertEquals(UiUtils.NONE, editor.getCardStack().getVisibleKey());

			assertTrue(editor.getLogArea().getText() != null);

			assertTrue(editor.getEndResultArea().getText().contains(getUiString("recipe.end.result").trim()));

		});

		SwingUtilities.invokeAndWait(editor::dispose);

	}

	@Test

	public void addChosenStepAddsToDraftOnlyUntilApply() throws Exception

	{

		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		FakeDbPort db = new FakeDbPort();

		Recipe live = new Recipe("EdAdd");

		db.recipes.put("EdAdd", live);

		RecordingNavPort nav = new RecordingNavPort();

		DirtyStateService dirty = new DirtyStateService();

		final RecipeEditorDialog[] holder = new RecipeEditorDialog[1];

		SwingUtilities.invokeAndWait(() ->

			holder[0] = new RecipeEditorDialog(new JFrame(), dirty, () -> {}, nav, live, db));

		RecipeEditorDialog editor = holder[0];

		SwingUtilities.invokeAndWait(() -> {});

		SwingUtilities.invokeAndWait(() -> editor.addChosenStep(ProcessStep.Type.MASH));

		assertEquals(0, live.getSteps().size());

		assertEquals(1, editor.getDraftForTest().getSteps().size());

		SwingUtilities.invokeAndWait(editor::applyForTest);

		assertEquals(1, live.getSteps().size());

		SwingUtilities.invokeAndWait(editor::dispose);

	}

	@Test

	public void okAppliesDraftMarksDirty() throws Exception

	{

		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		FakeDbPort db = new FakeDbPort();

		Recipe live = new Recipe("EdOk");

		db.recipes.put("EdOk", live);

		RecordingNavPort nav = new RecordingNavPort();

		DirtyStateService dirty = new DirtyStateService();

		final RecipeEditorDialog[] holder = new RecipeEditorDialog[1];

		SwingUtilities.invokeAndWait(() ->

			holder[0] = new RecipeEditorDialog(new JFrame(), dirty, () -> {}, nav, live, db));

		RecipeEditorDialog editor = holder[0];

		SwingUtilities.invokeAndWait(() -> {});

		SwingUtilities.invokeAndWait(() -> editor.addChosenStep(ProcessStep.Type.MASH));

		SwingUtilities.invokeAndWait(editor::applyForTest);

		assertTrue(dirty.isDirty(live));

		assertTrue(dirty.isDirty(live.getSteps().get(0)));

		SwingUtilities.invokeAndWait(editor::dispose);

	}

	@Test

	public void cancelDiscardsDraftChanges() throws Exception

	{

		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		FakeDbPort db = new FakeDbPort();

		Recipe live = new Recipe("EdCancel");

		db.recipes.put("EdCancel", live);

		RecordingNavPort nav = new RecordingNavPort();

		DirtyStateService dirty = new DirtyStateService();

		final RecipeEditorDialog[] holder = new RecipeEditorDialog[1];

		SwingUtilities.invokeAndWait(() ->

			holder[0] = new RecipeEditorDialog(new JFrame(), dirty, () -> {}, nav, live, db));

		RecipeEditorDialog editor = holder[0];

		SwingUtilities.invokeAndWait(() -> {});

		SwingUtilities.invokeAndWait(() -> editor.addChosenStep(ProcessStep.Type.MASH));

		assertEquals(0, live.getSteps().size());

		SwingUtilities.invokeAndWait(editor::cancelForTest);

		assertEquals(0, live.getSteps().size());

		assertFalse(dirty.isDirty(live));

	}

	@Test

	public void selectingStepShowsPlaceholderCardForType() throws Exception

	{

		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		FakeDbPort db = new FakeDbPort();

		Recipe live = new Recipe("EdCard");

		ProcessStep mash = RecipeEditorSteps.createStep(live, ProcessStep.Type.MASH_INFUSION);

		live.getSteps().add(mash);

		db.recipes.put("EdCard", live);

		RecordingNavPort nav = new RecordingNavPort();

		final RecipeEditorDialog[] holder = new RecipeEditorDialog[1];

		SwingUtilities.invokeAndWait(() ->

			holder[0] = new RecipeEditorDialog(new JFrame(), new DirtyStateService(), () -> {}, nav, live, db));

		RecipeEditorDialog editor = holder[0];

		SwingUtilities.invokeAndWait(() -> {});

		SwingUtilities.invokeAndWait(() -> {});

		SwingUtilities.invokeAndWait(() ->

		{

			JTree tree = editor.getRecipeTree().getTree();

			tree.expandRow(0);

			TreePath p = tree.getPathForRow(1);

			tree.setSelectionPath(p);

		});

		SwingUtilities.invokeAndWait(() -> {});

		SwingUtilities.invokeAndWait(() ->

			assertEquals(ProcessStep.Type.MASH_INFUSION.name(), editor.getCardStack().getVisibleKey()));

		SwingUtilities.invokeAndWait(editor::dispose);

	}

	@Test

	public void dirtyStepRowIsBold() throws Exception

	{

		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		FakeDbPort db = new FakeDbPort();

		Recipe live = new Recipe("EdBold");

		ProcessStep mash = RecipeEditorSteps.createStep(live, ProcessStep.Type.BOIL);

		live.getSteps().add(mash);

		db.recipes.put("EdBold", live);

		DirtyStateService dirty = new DirtyStateService();

		RecordingNavPort nav = new RecordingNavPort();

		final RecipeEditorDialog[] holder = new RecipeEditorDialog[1];

		SwingUtilities.invokeAndWait(() ->

			holder[0] = new RecipeEditorDialog(new JFrame(), dirty, () -> {}, nav, live, db));

		RecipeEditorDialog editor = holder[0];

		SwingUtilities.invokeAndWait(() -> {});

		SwingUtilities.invokeAndWait(() -> {});

		ProcessStep draftStep = editor.getDraftForTest().getSteps().get(0);

		SwingUtilities.invokeAndWait(() ->

		{

			assertEquals(Font.PLAIN, editor.rowFontStyle(1));

			dirty.markDirty(draftStep);

			editor.getRecipeTree().refreshNodeLabels();

		});

		SwingUtilities.invokeAndWait(() ->

			assertEquals(Font.BOLD, editor.rowFontStyle(1)));

		SwingUtilities.invokeAndWait(editor::dispose);

	}

	@Test

	public void renameStepUpdatesNameAndMarksDirty() throws Exception

	{

		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		FakeDbPort db = new FakeDbPort();

		Recipe live = new Recipe("EdRenameStep");

		ProcessStep mash = RecipeEditorSteps.createStep(live, ProcessStep.Type.MASH);

		live.getSteps().add(mash);

		db.recipes.put("EdRenameStep", live);

		DirtyStateService dirty = new DirtyStateService();

		RecordingNavPort nav = new RecordingNavPort();

		final RecipeEditorDialog[] holder = new RecipeEditorDialog[1];

		SwingUtilities.invokeAndWait(() ->

			holder[0] = new RecipeEditorDialog(new JFrame(), dirty, () -> {}, nav, live, db));

		RecipeEditorDialog editor = holder[0];

		SwingUtilities.invokeAndWait(() -> {});

		ProcessStep draftStep = editor.getDraftForTest().getSteps().get(0);

		SwingUtilities.invokeAndWait(() -> editor.selectStepInTreeForTest(draftStep));

		SwingUtilities.invokeAndWait(() -> editor.renameSelectedStepForTest("New Name"));

		assertEquals("New Name", draftStep.getName());

		assertTrue(dirty.isDirty(draftStep));

		SwingUtilities.invokeAndWait(editor::dispose);

	}

	@Test

	public void duplicateStepAddsClonedStepToDraft() throws Exception

	{

		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		FakeDbPort db = new FakeDbPort();

		Recipe live = new Recipe("EdDupStep");

		ProcessStep mash = RecipeEditorSteps.createStep(live, ProcessStep.Type.MASH);

		live.getSteps().add(mash);

		db.recipes.put("EdDupStep", live);

		DirtyStateService dirty = new DirtyStateService();

		RecordingNavPort nav = new RecordingNavPort();

		final RecipeEditorDialog[] holder = new RecipeEditorDialog[1];

		SwingUtilities.invokeAndWait(() ->

			holder[0] = new RecipeEditorDialog(new JFrame(), dirty, () -> {}, nav, live, db));

		RecipeEditorDialog editor = holder[0];

		SwingUtilities.invokeAndWait(() -> {});

		assertEquals(1, live.getSteps().size());

		ProcessStep draftStep = editor.getDraftForTest().getSteps().get(0);

		SwingUtilities.invokeAndWait(() -> editor.selectStepInTreeForTest(draftStep));

		SwingUtilities.invokeAndWait(() -> editor.duplicateSelectedStepForTest("Step 2"));

		assertEquals(2, editor.getDraftForTest().getSteps().size());

		assertEquals(1, live.getSteps().size());

		ProcessStep clone = editor.getDraftForTest().getSteps().get(1);

		assertTrue(dirty.isDirty(clone));

		SwingUtilities.invokeAndWait(editor::dispose);

	}

	@Test

	public void deleteStepRemovesFromDraftAndDirty() throws Exception

	{

		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		FakeDbPort db = new FakeDbPort();

		Recipe live = new Recipe("EdDelStep");

		ProcessStep mash = RecipeEditorSteps.createStep(live, ProcessStep.Type.MASH);

		live.getSteps().add(mash);

		db.recipes.put("EdDelStep", live);

		DirtyStateService dirty = new DirtyStateService();

		RecordingNavPort nav = new RecordingNavPort();

		final RecipeEditorDialog[] holder = new RecipeEditorDialog[1];

		SwingUtilities.invokeAndWait(() ->

			holder[0] = new RecipeEditorDialog(new JFrame(), dirty, () -> {}, nav, live, db));

		RecipeEditorDialog editor = holder[0];

		SwingUtilities.invokeAndWait(() -> {});

		ProcessStep draftStep = editor.getDraftForTest().getSteps().get(0);

		SwingUtilities.invokeAndWait(() ->

		{

			editor.selectStepInTreeForTest(draftStep);

			dirty.markDirty(draftStep);

		});

		SwingUtilities.invokeAndWait(() -> editor.deleteSelectedStepForTest());

		assertTrue(editor.getDraftForTest().getSteps().isEmpty());

		assertFalse(dirty.isDirty(draftStep));

		assertTrue(dirty.isDirty(editor.getDraftForTest()));

		SwingUtilities.invokeAndWait(editor::dispose);

	}

	@Test

	public void selectingHeatStepShowsRealHeatPane() throws Exception

	{

		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		FakeDbPort db = new FakeDbPort();

		Recipe live = new Recipe("EdHeatPane");

		ProcessStep heat = RecipeEditorSteps.createStep(live, ProcessStep.Type.HEAT);

		live.getSteps().add(heat);

		db.recipes.put("EdHeatPane", live);

		RecordingNavPort nav = new RecordingNavPort();

		final RecipeEditorDialog[] holder = new RecipeEditorDialog[1];

		SwingUtilities.invokeAndWait(() ->

			holder[0] = new RecipeEditorDialog(new JFrame(), new DirtyStateService(), () -> {}, nav, live, db));

		RecipeEditorDialog editor = holder[0];

		SwingUtilities.invokeAndWait(() -> {});

		ProcessStep draftHeat = editor.getDraftForTest().getSteps().get(0);

		SwingUtilities.invokeAndWait(() -> editor.selectStepInTreeForTest(draftHeat));

		SwingUtilities.invokeAndWait(() -> {});

		SwingUtilities.invokeAndWait(() ->

		{

			assertEquals(ProcessStep.Type.HEAT.name(), editor.getCardStack().getVisibleKey());

			assertTrue(editor.getStepPaneForTest(ProcessStep.Type.HEAT) instanceof SwingHeatPane);

		});

		SwingUtilities.invokeAndWait(editor::dispose);

	}

	private static final class RecordingNavPort implements RecipeEditorNavPort

	{

		@Override

		public void openRecipeEditor(String recipeName)

		{

		}

	}

	private static final class FakeDbPort implements RecipeEditorDialog.DbPort

	{

		private final Map<String, Recipe> recipes = new LinkedHashMap<>();

		@Override

		public Map<String, Recipe> recipes()

		{

			return recipes;

		}

		@Override

		public void saveAll()

		{

		}

		@Override

		public void loadAll()

		{

		}

	}

}

