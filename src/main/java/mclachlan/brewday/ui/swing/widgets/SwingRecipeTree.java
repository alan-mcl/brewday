package mclachlan.brewday.ui.swing.widgets;

import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.ui.swing.app.DirtyStateService;
import mclachlan.brewday.ui.swing.app.SwingIcons;

/**
 * Swing port of JFX {@code RecipeTreeView} + {@code RecipeTreeViewModel}.
 */
public class SwingRecipeTree extends JPanel
{
	private final DirtyStateService dirtyState;
	private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private final DefaultTreeModel model = new DefaultTreeModel(root);
	private final JTree tree = new JTree(model);
	private Recipe recipe;
	private Consumer<Object> selectionListener;

	public SwingRecipeTree(DirtyStateService dirtyState)
	{
		super(new java.awt.BorderLayout());
		this.dirtyState = dirtyState;
		tree.setRootVisible(true);
		tree.setShowsRootHandles(true);
		tree.setRowHeight(SwingIcons.TREE_ROW_HEIGHT);
		tree.setCellRenderer(new RecipeTreeCellRenderer(dirtyState));
		tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener()
		{
			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				if (selectionListener == null)
				{
					return;
				}
				Object u = getSelectedUserObject();
				selectionListener.accept(u);
			}
		});
		add(new JScrollPane(tree), java.awt.BorderLayout.CENTER);
	}

	public void setSelectionListener(Consumer<Object> selectionListener)
	{
		this.selectionListener = selectionListener;
	}

	public void setRecipe(Recipe recipe)
	{
		this.recipe = recipe;
		root.removeAllChildren();
		root.setUserObject(recipe);
		if (recipe != null)
		{
			for (ProcessStep step : recipe.getSteps())
			{
				addStepInternal(step);
			}
			sortStepChildren();
		}
		model.reload();
		tree.expandRow(0);
	}

	public void addStep(ProcessStep step)
	{
		DefaultMutableTreeNode stepNode = createStepNode(step);
		int index = stepInsertIndex(step);
		model.insertNodeInto(stepNode, root, index);
	}

	private void addStepInternal(ProcessStep step)
	{
		root.add(createStepNode(step));
	}

	private DefaultMutableTreeNode createStepNode(ProcessStep step)
	{
		DefaultMutableTreeNode stepNode = new DefaultMutableTreeNode(step);
		List<IngredientAddition> adds = new ArrayList<>(step.getIngredientAdditions());
		adds.sort(UiUtils.getIngredientAdditionComparator());
		for (IngredientAddition a : adds)
		{
			stepNode.add(new DefaultMutableTreeNode(a));
		}
		return stepNode;
	}

	private int stepInsertIndex(ProcessStep step)
	{
		if (recipe == null)
		{
			return root.getChildCount();
		}
		int index = recipe.getSteps().indexOf(step);
		return index >= 0 ? index : root.getChildCount();
	}

	public void removeStep(ProcessStep step)
	{
		DefaultMutableTreeNode n = findNodeForUserObject(root, step);
		if (n != null)
		{
			model.removeNodeFromParent(n);
		}
	}

	public void addAddition(ProcessStep step, IngredientAddition addition)
	{
		DefaultMutableTreeNode stepNode = findNodeForUserObject(root, step);
		if (stepNode == null)
		{
			return;
		}
		DefaultMutableTreeNode addNode = new DefaultMutableTreeNode(addition);
		model.insertNodeInto(addNode, stepNode, sortedAdditionIndex(step, addition));
	}

	public void removeAddition(ProcessStep step, IngredientAddition addition)
	{
		DefaultMutableTreeNode stepNode = findNodeForUserObject(root, step);
		if (stepNode == null)
		{
			return;
		}
		DefaultMutableTreeNode addNode = findNodeForUserObject(stepNode, addition);
		if (addNode != null)
		{
			model.removeNodeFromParent(addNode);
		}
	}

	public void refreshNodeLabels()
	{
		notifyNodeChangedRecursive(root);
	}

	private void notifyNodeChangedRecursive(DefaultMutableTreeNode node)
	{
		model.nodeChanged(node);
		for (int i = 0; i < node.getChildCount(); i++)
		{
			notifyNodeChangedRecursive((DefaultMutableTreeNode)node.getChildAt(i));
		}
	}

	public void selectRoot()
	{
		TreePath path = new TreePath(root.getPath());
		tree.setSelectionPath(path);
	}

	/**
	 * Selects the tree node whose user object is {@code step}, if present.
	 */
	public void selectStep(ProcessStep step)
	{
		if (step == null)
		{
			return;
		}
		DefaultMutableTreeNode n = findNodeForUserObject(root, step);
		if (n != null)
		{
			TreePath path = new TreePath(n.getPath());
			tree.setSelectionPath(path);
		}
	}

	/**
	 * Selects the tree node whose user object equals {@code userObject} (step or ingredient addition), if present.
	 */
	public void selectUserObject(Object userObject)
	{
		if (userObject == null)
		{
			return;
		}
		DefaultMutableTreeNode n = findNodeForUserObject(root, userObject);
		if (n != null)
		{
			tree.setSelectionPath(new TreePath(n.getPath()));
		}
	}

	public Object getSelectedUserObject()
	{
		TreePath path = tree.getSelectionPath();
		if (path == null)
		{
			return null;
		}
		DefaultMutableTreeNode n = (DefaultMutableTreeNode)path.getLastPathComponent();
		return n.getUserObject();
	}

	public JTree getTree()
	{
		return tree;
	}

	public int rowFontStyle(int viewRow)
	{
		TreePath path = tree.getPathForRow(viewRow);
		if (path == null)
		{
			return Font.PLAIN;
		}
		DefaultMutableTreeNode n = (DefaultMutableTreeNode)path.getLastPathComponent();
		Object u = n.getUserObject();
		boolean bold = u != null && dirtyState.isDirty(u);
		return bold ? Font.BOLD : Font.PLAIN;
	}

	private void sortStepChildren()
	{
		if (recipe == null)
		{
			return;
		}
		List<DefaultMutableTreeNode> nodes = new ArrayList<>();
		for (int i = 0; i < root.getChildCount(); i++)
		{
			nodes.add((DefaultMutableTreeNode)root.getChildAt(i));
		}
		nodes.sort(Comparator.comparingInt(n ->
		{
			Object u = n.getUserObject();
			if (u instanceof ProcessStep ps)
			{
				return recipe.getSteps().indexOf(ps);
			}
			return 0;
		}));
		root.removeAllChildren();
		for (DefaultMutableTreeNode n : nodes)
		{
			root.add(n);
		}
	}

	private static int sortedAdditionIndex(ProcessStep step, IngredientAddition addition)
	{
		List<IngredientAddition> adds = new ArrayList<>(step.getIngredientAdditions());
		adds.sort(UiUtils.getIngredientAdditionComparator());
		int index = adds.indexOf(addition);
		return index >= 0 ? index : adds.size() - 1;
	}

	private static DefaultMutableTreeNode findNodeForUserObject(DefaultMutableTreeNode parent, Object target)
	{
		if (target.equals(parent.getUserObject()))
		{
			return parent;
		}
		for (int i = 0; i < parent.getChildCount(); i++)
		{
			DefaultMutableTreeNode ch = (DefaultMutableTreeNode)parent.getChildAt(i);
			DefaultMutableTreeNode found = findNodeForUserObject(ch, target);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}

	private static String labelText(Object userObject)
	{
		if (userObject instanceof Recipe r)
		{
			return r.getName();
		}
		if (userObject instanceof ProcessStep s)
		{
			return s.getName();
		}
		if (userObject instanceof IngredientAddition a)
		{
			return a.toString();
		}
		return String.valueOf(userObject);
	}

	private static Icon iconFor(Object userObject)
	{
		if (userObject instanceof Recipe)
		{
			return SwingIcons.treeIcon(SwingIcons.IconKey.RECIPE);
		}
		if (userObject instanceof ProcessStep s)
		{
			return SwingIcons.treeIcon(SwingIcons.stepTypeIcon(s.getType()));
		}
		if (userObject instanceof IngredientAddition addition)
		{
			return SwingIcons.iconForAddition(addition);
		}
		return SwingIcons.treeIcon(SwingIcons.IconKey.STEP);
	}

	private static final class RecipeTreeCellRenderer extends DefaultTreeCellRenderer
	{
		private final DirtyStateService dirtyState;

		RecipeTreeCellRenderer(DirtyStateService dirtyState)
		{
			this.dirtyState = dirtyState;
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			if (value instanceof DefaultMutableTreeNode node)
			{
				Object u = node.getUserObject();
				setText(labelText(u));
				setIcon(iconFor(u));
				Font base = tree.getFont();
				boolean bold = u != null && dirtyState.isDirty(u);
				if (u instanceof IngredientAddition && node.getParent() instanceof DefaultMutableTreeNode pn)
				{
					Object pu = ((DefaultMutableTreeNode)pn).getUserObject();
					if (pu instanceof ProcessStep && dirtyState.isDirty(pu))
					{
						bold = true;
					}
				}
				setFont(base.deriveFont(bold ? Font.BOLD : Font.PLAIN));
			}
			setBorder(new EmptyBorder(1, 2, 1, 2));
			return this;
		}
	}
}
