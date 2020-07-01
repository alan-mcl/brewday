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

package net.miginfocom.layout;

/** An interface to implement if you want to decide the gaps between two types of components within the same cell.
 * <p>
 * E.g.:
 *
 * <pre>
 * {@code
 * if (adjacentComp == null || adjacentSide == SwingConstants.LEFT || adjacentSide == SwingConstants.TOP)
 *	  return null;
 *
 * boolean isHor = (adjacentSide == SwingConstants.LEFT || adjacentSide == SwingConstants.RIGHT);
 *
 * if (adjacentComp.getComponentType(false) == ComponentWrapper.TYPE_LABEL && comp.getComponentType(false) == ComponentWrapper.TYPE_TEXT_FIELD)
 *    return isHor ? UNRELATED_Y : UNRELATED_Y;
 *
 * return (adjacentSide == SwingConstants.LEFT || adjacentSide == SwingConstants.RIGHT) ? RELATED_X : RELATED_Y;
 * }
 * </pre>
 */
public interface InCellGapProvider
{
	/** Returns the default gap between two components that <b>are in the same cell</b>.
	 * @param comp The component that the gap is for. Never <code>null</code>.
	 * @param adjacentComp The adjacent component if any. May be <code>null</code>.
	 * @param adjacentSide What side the <code>adjacentComp</code> is on. {@link javax.swing.SwingUtilities#TOP} or
	 * {@link javax.swing.SwingUtilities#LEFT} or {@link javax.swing.SwingUtilities#BOTTOM} or {@link javax.swing.SwingUtilities#RIGHT}.
	 * @param tag The tag string that the component might be tagged with in the component constraints. May be <code>null</code>.
	 * @param isLTR If it is left-to-right.
	 * @return The default gap between two components or <code>null</code> if there should be no gap.
	 */
	public abstract BoundSize getDefaultGap(ComponentWrapper comp, ComponentWrapper adjacentComp, int adjacentSide, String tag, boolean isLTR);
}
