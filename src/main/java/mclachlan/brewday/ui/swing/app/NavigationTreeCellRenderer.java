package mclachlan.brewday.ui.swing.app;

import java.awt.Component;
import java.awt.Font;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class NavigationTreeCellRenderer extends DefaultTreeCellRenderer
{
	private final Map<DefaultMutableTreeNode, ScreenKey> nodeMap;
	private final Map<DefaultMutableTreeNode, String> tagNodeMap;
	private final Predicate<DefaultMutableTreeNode> dirtyNodePredicate;

	public NavigationTreeCellRenderer(
		Map<DefaultMutableTreeNode, ScreenKey> nodeMap,
		Map<DefaultMutableTreeNode, String> tagNodeMap,
		Predicate<DefaultMutableTreeNode> dirtyNodePredicate)
	{
		this.nodeMap = nodeMap;
		this.tagNodeMap = tagNodeMap;
		this.dirtyNodePredicate = dirtyNodePredicate;
	}

	@Override
	public Component getTreeCellRendererComponent(
		JTree tree,
		Object value,
		boolean selected,
		boolean expanded,
		boolean leaf,
		int row,
		boolean hasFocus)
	{
		super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		if (value instanceof DefaultMutableTreeNode node)
		{
			String tag = tagNodeMap.get(node);
			if (tag != null)
			{
				setToolTipText(NavTooltipSupport.tooltipForRecipeTag(tag));
				return this;
			}
			ScreenKey key = nodeMap.get(node);
			if (key != null)
			{
				Font base = tree.getFont();
				setFont(base.deriveFont(dirtyNodePredicate.test(node) ? Font.BOLD : Font.PLAIN));
				setIcon(SwingIcons.navIcon(SwingIcons.navKey(key)));
				setToolTipText(NavTooltipSupport.tooltipFor(key));
			}
		}
		return this;
	}
}
