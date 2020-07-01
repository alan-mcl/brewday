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
/** A class that wraps a container that contains components.
 */
public interface ContainerWrapper extends ComponentWrapper
{
	/** Returns the components of the container that wrapper is wrapping.
	 * @return The components of the container that wrapper is wrapping. Never <code>null</code>.
	 */
	public abstract ComponentWrapper[] getComponents();

	/** Returns the number of components that this parent has.
	 * @return The number of components that this parent has.
	 */
	public abstract int getComponentCount();

	/** Returns the <code>LayoutHandler</code> (in Swing terms) that is handling the layout of this container.
	 * If there exist no such class the method should return the same as {@link #getComponent()}, which is the
	 * container itself.
	 * @return The layout handler instance. Never <code>null</code>.
	 */
	public abstract Object getLayout();

	/** Returns if this container is using left-to-right component ordering.
	 * @return If this container is using left-to-right component ordering.
	 */
	public abstract boolean isLeftToRight();

	/** Paints a cell to indicate where it is.
	 * @param x The x coordinate to start the drawing.
	 * @param y The x coordinate to start the drawing.
	 * @param width The width to draw/fill
	 * @param height The height to draw/fill
	 */
	public abstract void paintDebugCell(int x, int y, int width, int height);
}
