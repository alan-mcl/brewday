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

package mclachlan.brewday.ui.swing.app;

import javax.swing.JButton;

/**
 * Standard OK/Cancel/Close button tooltips for modal dialogs.
 */
public final class DialogButtonTooltips
{
	private DialogButtonTooltips()
	{
	}

	public static void wireOkCancel(JButton ok, JButton cancel)
	{
		ok.setToolTipText(mclachlan.brewday.util.StringUtils.getUiString("ui.ok.tooltip"));
		cancel.setToolTipText(mclachlan.brewday.util.StringUtils.getUiString("ui.cancel.tooltip"));
	}

	public static void wireClose(JButton close)
	{
		close.setToolTipText(mclachlan.brewday.util.StringUtils.getUiString("ui.close.tooltip"));
	}

	public static void wireAdd(JButton add)
	{
		add.setToolTipText(mclachlan.brewday.util.StringUtils.getUiString("common.add.tooltip"));
	}
}
