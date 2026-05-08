package mclachlan.brewday.ui.swing.widgets;

import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import mclachlan.brewday.BrewdayException;

/**
 * Swing analogue of JFX {@code CardGroup}: one visible child keyed by string.
 */
public class SwingCardStack extends JPanel
{
	private final CardLayout cardLayout = new CardLayout();
	private final Map<String, JComponent> childMap = new HashMap<>();
	private String visibleKey;

	public SwingCardStack()
	{
		super(new CardLayout());
		setLayout(cardLayout);
	}

	public void addCard(String key, JComponent child)
	{
		if (childMap.containsKey(key))
		{
			throw new BrewdayException("Duplicate card key: " + key);
		}
		childMap.put(key, child);
		add(child, key);
		child.setVisible(false);
	}

	public void setVisibleCard(String key)
	{
		if (!childMap.containsKey(key))
		{
			throw new BrewdayException("Invalid: " + key);
		}
		for (JComponent c : childMap.values())
		{
			c.setVisible(false);
		}
		JComponent node = childMap.get(key);
		node.setVisible(true);
		cardLayout.show(this, key);
		visibleKey = key;
	}

	public String getVisibleKey()
	{
		return visibleKey;
	}

	public JComponent getCard(String key)
	{
		return childMap.get(key);
	}
}
