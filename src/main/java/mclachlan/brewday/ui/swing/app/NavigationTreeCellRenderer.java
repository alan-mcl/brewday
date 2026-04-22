package mclachlan.brewday.ui.swing.app;

import java.awt.Component;
import java.util.Map;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class NavigationTreeCellRenderer extends DefaultTreeCellRenderer
{
	private final Map<DefaultMutableTreeNode, ScreenKey> nodeMap;

	public NavigationTreeCellRenderer(Map<DefaultMutableTreeNode, ScreenKey> nodeMap)
	{
		this.nodeMap = nodeMap;
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
				setIcon(SwingIcons.navIcon(SwingIcons.navKey(key)));
			}
		}
		return this;
	}
}
