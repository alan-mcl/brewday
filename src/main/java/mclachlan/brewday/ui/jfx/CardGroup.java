package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.scene.Group;
import javafx.scene.Node;

/**
 *
 */
public class CardGroup extends Group
{
	private Map<String, Node> childMap = new HashMap<>();

	public void add(String key, Node child)
	{
		childMap.put(key, child);
		this.getChildren().add(child);
		child.setVisible(false);
	}

	public void setVisible(String key)
	{
		childMap.values().forEach(node -> node.setVisible(false));
		Node node = childMap.get(key);
		if (node != null)
		{
			node.setVisible(true);
		}
	}

	public Node getChild(String key)
	{
		return childMap.get(key);
	}
}
