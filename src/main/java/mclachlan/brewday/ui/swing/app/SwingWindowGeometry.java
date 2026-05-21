package mclachlan.brewday.ui.swing.app;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;

import mclachlan.brewday.Brewday;

/**
 * Default window sizes from the usable screen/work area. Headless or errors fall back to legacy fixed sizes.
 */
public final class SwingWindowGeometry
{
	private static final double MAIN_FRAME_WIDTH_FRAC = 0.89;
	private static final double MAIN_FRAME_HEIGHT_FRAC = 0.87;
	private static final int MAIN_FRAME_MIN_WIDTH = 1280;
	private static final int MAIN_FRAME_MIN_HEIGHT = 720;

	private static final double RECIPE_EDITOR_WIDTH_FRAC = 0.91;
	private static final double RECIPE_EDITOR_HEIGHT_FRAC = 0.88;
	private static final int RECIPE_EDITOR_MIN_WIDTH = 1100;
	private static final int RECIPE_EDITOR_MIN_HEIGHT = 720;

	/** East end-result column: ~25% of recipe editor width. */
	public static final double RECIPE_EDITOR_END_RESULT_FRACTION = 0.25;
	/** Process/log tabbed area (tree + cards): ~75% of editor width. */
	public static final double RECIPE_EDITOR_PROCESS_TAB_FRACTION = 0.75;
	/**
	 * Recipe tree within the process tab: 25% / 75% ≈ one-third (~25% of dialog).
	 */
	public static final double RECIPE_EDITOR_PROC_TREE_FRACTION = 0.5;

	/**
	 * Applies 25% / 50% / 25% (tree / cards / end result) divider positions after the dialog is sized.
	 * {@link javax.swing.JSplitPane#setResizeWeight(double)} matches each split so resize does not
	 * dump extra width into the card column.
	 */
	public static void applyRecipeEditorSplitDividers(javax.swing.JSplitPane mainSplit,
		javax.swing.JSplitPane procSplit)
	{
		mainSplit.setResizeWeight(RECIPE_EDITOR_PROCESS_TAB_FRACTION);
		procSplit.setResizeWeight(RECIPE_EDITOR_PROC_TREE_FRACTION);

		int mainW = mainSplit.getWidth();
		if (mainW > 0)
		{
			mainSplit.setDividerLocation((int)Math.round(mainW * RECIPE_EDITOR_PROCESS_TAB_FRACTION));
		}
		else
		{
			mainSplit.setDividerLocation(RECIPE_EDITOR_PROCESS_TAB_FRACTION);
		}

		int procW = procSplit.getWidth();
		if (procW > 0)
		{
			procSplit.setDividerLocation((int)Math.round(procW * RECIPE_EDITOR_PROC_TREE_FRACTION));
		}
		else
		{
			procSplit.setDividerLocation(RECIPE_EDITOR_PROC_TREE_FRACTION);
		}
	}

	private static final Dimension FALLBACK_MAIN_FRAME = new Dimension(1280, 768);
	private static final Dimension FALLBACK_RECIPE_EDITOR = new Dimension(1100, 720);

	private SwingWindowGeometry()
	{
	}

	public static Dimension defaultMainFrameSize()
	{
		if (GraphicsEnvironment.isHeadless())
		{
			return new Dimension(FALLBACK_MAIN_FRAME);
		}
		try
		{
			Rectangle b = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			return sizeWithinBounds(b, MAIN_FRAME_WIDTH_FRAC, MAIN_FRAME_HEIGHT_FRAC,
				MAIN_FRAME_MIN_WIDTH, MAIN_FRAME_MIN_HEIGHT);
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.out);
			try
			{
				Brewday.getInstance().getLog().log(t);
			}
			catch (Throwable logEx)
			{
				logEx.printStackTrace(System.out);
			}
			return new Dimension(FALLBACK_MAIN_FRAME);
		}
	}

	/**
	 * @param owner preferred for multi-monitor; falls back to maximum window bounds when null or not yet displayable.
	 */
	public static Dimension defaultRecipeEditorSize(Window owner)
	{
		if (GraphicsEnvironment.isHeadless())
		{
			return new Dimension(FALLBACK_RECIPE_EDITOR);
		}
		try
		{
			Rectangle b = referenceBounds(owner);
			if (b == null || b.width < 1 || b.height < 1)
			{
				return new Dimension(FALLBACK_RECIPE_EDITOR);
			}
			return sizeWithinBounds(b, RECIPE_EDITOR_WIDTH_FRAC, RECIPE_EDITOR_HEIGHT_FRAC,
				RECIPE_EDITOR_MIN_WIDTH, RECIPE_EDITOR_MIN_HEIGHT);
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.out);
			try
			{
				Brewday.getInstance().getLog().log(t);
			}
			catch (Throwable logEx)
			{
				logEx.printStackTrace(System.out);
			}
			return new Dimension(FALLBACK_RECIPE_EDITOR);
		}
	}

	private static Rectangle referenceBounds(Window owner)
	{
		if (owner != null && owner.isDisplayable())
		{
			GraphicsConfiguration gc = owner.getGraphicsConfiguration();
			if (gc != null)
			{
				return gc.getBounds();
			}
		}
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
	}

	private static Dimension sizeWithinBounds(Rectangle b, double wFrac, double hFrac,
		int minW, int minH)
	{
		int w = (int)Math.round(b.width * wFrac);
		int h = (int)Math.round(b.height * hFrac);
		w = Math.min(w, b.width);
		h = Math.min(h, b.height);
		w = Math.max(w, Math.min(minW, b.width));
		h = Math.max(h, Math.min(minH, b.height));
		return new Dimension(w, h);
	}
}
