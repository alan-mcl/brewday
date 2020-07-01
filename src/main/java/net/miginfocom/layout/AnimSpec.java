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

import java.io.Serializable;

/**
 * @author Mikael Grev, MiG InfoCom AB
 *         Date: 14-09-24
 *         Time: 17:05
 */
public class AnimSpec implements Serializable
{
//	public static final AnimSpec OFF = new AnimSpec(-1, 0, 0);
	public static final AnimSpec DEF = new AnimSpec(0, 0, 0.2f, 0.2f);

	private final int prio;
	private final int durMillis;
	private final float easeIn, easeOut;

	/**
	 * @param prio The animation priority. When added with the general animation priority of the layout the animation will
	 * be done if the resulting value is &gt; 0.
	 * @param durMillis Duration in milliseconds. &lt;=0 means default value should be used and &gt; 0 is the number of millis
	 * @param easeIn 0 is linear (no ease). 1 is max ease. Always clamped between these values.
	 * @param easeOut 0 is linear (no ease). 1 is max ease. Always clamped between these values.
	 */
	public AnimSpec(int prio, int durMillis, float easeIn, float easeOut)
	{
		this.prio = prio;
		this.durMillis = durMillis;
		this.easeIn = LayoutUtil.clamp(easeIn, 0, 1);
		this.easeOut = LayoutUtil.clamp(easeOut, 0, 1);
	}

	/**
	 * @return The animation priority. When added with the general animation priority of the layout the animation will
	 * be done if the resulting value is &gt; 0.
	 */
	public int getPriority()
	{
		return prio;
	}

	/**
	 * @param defMillis Default used if the millis in the spec is set to "default".
	 * @return Duration in milliseconds. &lt;=0 means default value should be used and &gt; 0 is the number of millis
	 */
	public int getDurationMillis(int defMillis)
	{
		return durMillis > 0 ? durMillis : defMillis;
	}

	/**
	 * @return Duration in milliseconds. &lt;= 0 means default value should be used and &gt; 0 is the number of millis
	 */
	public int getDurationMillis()
	{
		return durMillis;
	}

	/**
	 * @return A value between 0 and 1 where 0 is no ease in and 1 is maximum ease in.
	 */
	public float getEaseIn()
	{
		return easeIn;
	}

	/**
	 * @return A value between 0 and 1 where 0 is no ease out and 1 is maximum ease out.
	 */
	public float getEaseOut()
	{
		return easeOut;
	}
}
