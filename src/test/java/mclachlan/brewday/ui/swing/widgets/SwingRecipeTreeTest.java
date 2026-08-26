package mclachlan.brewday.ui.swing.widgets;

import mclachlan.brewday.ui.swing.SwingTestSupport;

import java.awt.Font;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.WeightUnit;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.screens.RecipeEditorSteps;
import org.junit.BeforeClass;
import org.junit.Test;

import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SwingRecipeTreeTest
{
	@BeforeClass
	public static void setupDb()
	{
		Database.getInstance().loadAll();
	}

	@Test
	public void recipeAndStepRowsReflectDirtyState() throws Exception
	{
		SwingTestSupport.assumeDisplay();

		DirtyStateService dirty = new DirtyStateService();
		Recipe r = new Recipe();
		r.setName("TreeT");
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
		SwingTestSupport.assumeDisplay();

		DirtyStateService dirty = new DirtyStateService();
		Recipe r = new Recipe();
		r.setName("TreeAdd");
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

	@Test
	public void refreshNodeLabelsPreservesExpansionAndSelection() throws Exception
	{
		SwingTestSupport.assumeDisplay();

		DirtyStateService dirty = new DirtyStateService();
		Recipe r = new Recipe();
		r.setName("TreeRefresh");
		ProcessStep boil = RecipeEditorSteps.createStep(r, ProcessStep.Type.BOIL);
		Hop hop = new Hop();
		hop.setName("TestHop");
		HopAddition ha = new HopAddition(hop, new WeightUnit(20, GRAMS), GRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES));
		boil.getIngredientAdditions().add(ha);
		r.getSteps().add(boil);

		final SwingRecipeTree[] holder = new SwingRecipeTree[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new SwingRecipeTree(dirty));
		SwingRecipeTree recipeTree = holder[0];

		SwingUtilities.invokeAndWait(() -> recipeTree.setRecipe(r));
		SwingUtilities.invokeAndWait(() ->
		{
			JTree jtree = recipeTree.getTree();
			jtree.expandRow(0);
			jtree.expandRow(1);
			TreePath additionPath = jtree.getPathForRow(2);
			jtree.setSelectionPath(additionPath);
			assertTrue(jtree.isExpanded(additionPath.getParentPath()));
			dirty.markDirty(ha);
			recipeTree.refreshNodeLabels();
		});
		SwingUtilities.invokeAndWait(() ->
		{
			JTree jtree = recipeTree.getTree();
			TreePath selection = jtree.getSelectionPath();
			assertSame(ha, recipeTree.getSelectedUserObject());
			assertTrue(jtree.isExpanded(selection.getParentPath()));
			assertEquals(Font.BOLD, recipeTree.rowFontStyle(2));
		});
	}

	@Test
	public void addStepPreservesExpansionAndSelection() throws Exception
	{
		SwingTestSupport.assumeDisplay();

		DirtyStateService dirty = new DirtyStateService();
		Recipe r = new Recipe();
		r.setName("TreeAddStep");
		ProcessStep boil = RecipeEditorSteps.createStep(r, ProcessStep.Type.BOIL);
		Hop hop = new Hop();
		hop.setName("TestHop");
		HopAddition ha = new HopAddition(hop, new WeightUnit(20, GRAMS), GRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES));
		boil.getIngredientAdditions().add(ha);
		r.getSteps().add(boil);

		final SwingRecipeTree[] holder = new SwingRecipeTree[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new SwingRecipeTree(dirty));
		SwingRecipeTree recipeTree = holder[0];

		SwingUtilities.invokeAndWait(() -> recipeTree.setRecipe(r));
		ProcessStep mash = RecipeEditorSteps.createStep(r, ProcessStep.Type.MASH);
		SwingUtilities.invokeAndWait(() ->
		{
			JTree jtree = recipeTree.getTree();
			jtree.expandRow(0);
			jtree.expandRow(1);
			TreePath additionPath = jtree.getPathForRow(2);
			jtree.setSelectionPath(additionPath);
			r.getSteps().add(mash);
			recipeTree.addStep(mash);
		});
		SwingUtilities.invokeAndWait(() ->
		{
			JTree jtree = recipeTree.getTree();
			TreePath selection = jtree.getSelectionPath();
			assertSame(ha, recipeTree.getSelectedUserObject());
			assertTrue(jtree.isExpanded(selection.getParentPath()));
			assertEquals(4, jtree.getRowCount());
		});
	}

	@Test
	public void addAdditionPreservesExpansionOnOtherSteps() throws Exception
	{
		SwingTestSupport.assumeDisplay();

		DirtyStateService dirty = new DirtyStateService();
		Recipe r = new Recipe();
		r.setName("TreeAddIng");
		ProcessStep boil = RecipeEditorSteps.createStep(r, ProcessStep.Type.BOIL);
		Hop boilHop = new Hop();
		boilHop.setName("BoilHop");
		HopAddition boilHa = new HopAddition(boilHop, new WeightUnit(20, GRAMS), GRAMS,
			new TimeUnit(60, Quantity.Unit.MINUTES));
		boil.getIngredientAdditions().add(boilHa);
		ProcessStep mash = RecipeEditorSteps.createStep(r, ProcessStep.Type.MASH);
		r.getSteps().add(boil);
		r.getSteps().add(mash);

		final SwingRecipeTree[] holder = new SwingRecipeTree[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new SwingRecipeTree(dirty));
		SwingRecipeTree recipeTree = holder[0];

		Hop mashHop = new Hop();
		mashHop.setName("MashHop");
		HopAddition mashHa = new HopAddition(mashHop, new WeightUnit(10, GRAMS), GRAMS,
			new TimeUnit(15, Quantity.Unit.MINUTES));
		SwingUtilities.invokeAndWait(() -> recipeTree.setRecipe(r));
		SwingUtilities.invokeAndWait(() ->
		{
			JTree jtree = recipeTree.getTree();
			jtree.expandRow(0);
			jtree.expandRow(1);
			TreePath boilStepPath = jtree.getPathForRow(1);
			assertTrue(jtree.isExpanded(boilStepPath));
			mash.addIngredientAddition(mashHa);
			recipeTree.addAddition(mash, mashHa);
		});
		SwingUtilities.invokeAndWait(() ->
		{
			JTree jtree = recipeTree.getTree();
			TreePath boilStepPath = jtree.getPathForRow(1);
			assertTrue(jtree.isExpanded(boilStepPath));
			assertEquals(4, jtree.getRowCount());
		});
	}
}
