/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import mclachlan.brewday.BrewdayException;

/**
 * A panel that manages multiple components as cards, showing only one at a time.
 * This is the Swing equivalent of the JavaFX CardGroup.
 */
public class CardPanel extends JPanel
{
	/** Map of card keys to components */
	private final Map<String, Component> cardMap = new HashMap<>();
	
	/** The card layout used to manage the components */
	private final CardLayout cardLayout;
	
	/**
	 * Constructor.
	 */
	public CardPanel()
	{
		cardLayout = new CardLayout();
		setLayout(cardLayout);
	}
	
	/**
	 * Add a component as a card with the given key.
	 */
	public void addCard(String key, Component component)
	{
		cardMap.put(key, component);
		super.add(component, key);
	}
	
	/**
	 * Show the card with the given key.
	 */
	public void setVisible(String key)
	{
		if (cardMap.containsKey(key))
		{
			cardLayout.show(this, key);
		}
else
{
			throw new BrewdayException("Invalid card key: " + key);
		}
	}
	
	/**
	 * Get the component for the given key.
	 */
	public Component getCard(String key)
	{
		return cardMap.get(key);
	}
	
	/**
	 * Get the currently visible component.
	 */
	public Component getVisibleCard()
	{
		for (Map.Entry<String, Component> entry : cardMap.entrySet())
		{
			if (entry.getValue().isVisible())
			{
				return entry.getValue();
			}
		}
		
		throw new BrewdayException("No card is visible");
	}
}
