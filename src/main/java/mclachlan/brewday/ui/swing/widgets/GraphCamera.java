/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing.widgets;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * World-to-component transform: scale then translate (logical pixels).
 */
public final class GraphCamera
{
	private double scale = 1.0;
	private double tx;
	private double ty;
	private double minScale = 0.04;
	private double maxScale = 4.5;

	public AffineTransform getWorldToComponent()
	{
		AffineTransform t = new AffineTransform();
		t.translate(tx, ty);
		t.scale(scale, scale);
		return t;
	}

	public AffineTransform getComponentToWorld()
	{
		try
		{
			return getWorldToComponent().createInverse();
		}
		catch (NoninvertibleTransformException e)
		{
			return new AffineTransform();
		}
	}

	public void fitWorld(Rectangle2D world, int viewW, int viewH, double paddingFraction)
	{
		if (world == null || viewW < 16 || viewH < 16)
		{
			return;
		}
		double bw = Math.max(1.0, world.getWidth());
		double bh = Math.max(1.0, world.getHeight());
		double inset = paddingFraction;
		double sx = viewW * inset / bw;
		double sy = viewH * inset / bh;
		scale = Math.min(sx, sy);
		scale = Math.max(minScale, Math.min(maxScale, scale));
		double cx = world.getCenterX();
		double cy = world.getCenterY();
		tx = viewW / 2.0 - cx * scale;
		ty = viewH / 2.0 - cy * scale;
	}

	public void panScreen(double dScreenX, double dScreenY)
	{
		tx += dScreenX;
		ty += dScreenY;
	}

	public void zoomTowardPoint(double screenX, double screenY, double factor)
	{
		double wx = (screenX - tx) / scale;
		double wy = (screenY - ty) / scale;
		double newScale = scale * factor;
		newScale = Math.max(minScale, Math.min(maxScale, newScale));
		scale = newScale;
		tx = screenX - wx * scale;
		ty = screenY - wy * scale;
	}

	public Point2D componentToWorld(double screenX, double screenY)
	{
		Point2D p = new Point2D.Double(screenX, screenY);
		return getComponentToWorld().transform(p, p);
	}
}
