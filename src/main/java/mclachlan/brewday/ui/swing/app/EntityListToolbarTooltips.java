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

import javax.swing.Action;

/**
 * Shared toolbar tooltips for entity-list screens (recipes, reference DB, etc.).
 */
public final class EntityListToolbarTooltips
{
	private EntityListToolbarTooltips()
	{
	}

	public static void wireFullToolbar(
		Action saveAction,
		Action undoAction,
		Action addAction,
		Action editAction,
		Action duplicateAction,
		Action renameAction,
		Action deleteAction,
		Action filterAction,
		Action exportAction)
	{
		ActionHotkeySupport.applyTooltipText(saveAction, "tooltip.toolbar.save.all");
		ActionHotkeySupport.applyTooltipText(undoAction, "tooltip.toolbar.undo.all");
		ActionHotkeySupport.applyTooltipText(addAction, "tooltip.toolbar.add");
		ActionHotkeySupport.applyTooltipText(editAction, "tooltip.toolbar.edit");
		ActionHotkeySupport.applyTooltipText(duplicateAction, "tooltip.toolbar.duplicate");
		ActionHotkeySupport.applyTooltipText(renameAction, "tooltip.toolbar.rename");
		ActionHotkeySupport.applyTooltipText(deleteAction, "tooltip.toolbar.delete");
		ActionHotkeySupport.applyTooltipText(filterAction, "tooltip.toolbar.filter");
		ActionHotkeySupport.applyTooltipText(exportAction, "tooltip.toolbar.export");
	}

	public static void wireInventoryToolbar(
		Action saveAction,
		Action undoAction,
		Action editAction,
		Action deleteAction,
		Action filterAction,
		Action exportAction)
	{
		ActionHotkeySupport.applyTooltipText(saveAction, "tooltip.toolbar.save.all");
		ActionHotkeySupport.applyTooltipText(undoAction, "tooltip.toolbar.undo.all");
		ActionHotkeySupport.applyTooltipText(editAction, "tooltip.toolbar.edit");
		ActionHotkeySupport.applyTooltipText(deleteAction, "tooltip.toolbar.delete");
		ActionHotkeySupport.applyTooltipText(filterAction, "tooltip.toolbar.filter");
		ActionHotkeySupport.applyTooltipText(exportAction, "tooltip.toolbar.export");
	}
}
