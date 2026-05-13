package mclachlan.brewday.ui.swing.widgets;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SwingRecipeTreeTest
{
	@Test
	public void recipeAndStepRowsReflectDirtyState() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		DirtyStateService dirty = new DirtyStateService();
		Recipe r = new Recipe("TreeT");
		ProcessStep boil = RecipeEditorSteps.createStep(r, ProcessStep.Type.BOIL);
		r.getSteps().add(boil);

		final SwingRecipeTree[] holder = new SwingRecipeTree[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new SwingRecipeTree(dirty));
		SwingRecipeTree tree = holder[0];

		SwingUtilities.invokeAndWait(() -> tree.setRecipe(r));
		SwingUtilities.invokeAndWait(() ->
		{
			tree.getTree().expandRow(0);
			assertEquals(Font.PLAIN, tree.rowFontStyle(0));
			assertEquals(Font.PLAIN, tree.rowFontStyle(1));
			dirty.markDirty(boil, "recipes");
			tree.refreshNodeLabels();
		});
		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals(Font.PLAIN, tree.rowFontStyle(0));
			assertEquals(Font.BOLD, tree.rowFontStyle(1));
		});
	}

	@Test
	public void addStepInsertsModelNode() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());

		DirtyStateService dirty = new DirtyStateService();
		Recipe r = new Recipe("TreeAdd");
		final SwingRecipeTree[] holder = new SwingRecipeTree[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new SwingRecipeTree(dirty));
		SwingRecipeTree tree = holder[0];

		SwingUtilities.invokeAndWait(() -> tree.setRecipe(r));
		ProcessStep mash = RecipeEditorSteps.createStep(r, ProcessStep.Type.MASH);
		SwingUtilities.invokeAndWait(() ->
		{
			r.getSteps().add(mash);
			tree.addStep(mash);
		});
		SwingUtilities.invokeAndWait(() ->
		{
			tree.getTree().expandRow(0);
			assertEquals(2, tree.getTree().getRowCount());
		});
	}
}
