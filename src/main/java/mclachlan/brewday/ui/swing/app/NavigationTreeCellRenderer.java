package mclachlan.brewday.ui.swing.app;

import java.awt.Component;
import java.awt.Font;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class NavigationTreeCellRenderer extends DefaultTreeCellRenderer
{
	private final Map<DefaultMutableTreeNode, ScreenKey> nodeMap;
	private final Predicate<DefaultMutableTreeNode> dirtyNodePredicate;

	public NavigationTreeCellRenderer(Map<DefaultMutableTreeNode, ScreenKey> nodeMap, Predicate<DefaultMutableTreeNode> dirtyNodePredicate)
	{
		this.nodeMap = nodeMap;
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
			ScreenKey key = nodeMap.get(node);
			if (key != null)
			{
				Font base = tree.getFont();
				setFont(base.deriveFont(dirtyNodePredicate.test(node) ? Font.BOLD : Font.PLAIN));
				setIcon(SwingIcons.navIcon(SwingIcons.navKey(key)));
				if (key == ScreenKey.ABOUT)
				{
					setToolTipText(getUiString("menu.help.about") + " (F1)");
				}
				else
				{
					setToolTipText(null);
				}
			}
		}
		return this;
	}
}
