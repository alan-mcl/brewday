/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the recipee that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class ProcessTemplatePane extends V2DataObjectPane<Recipe>
{
	/*-------------------------------------------------------------------------*/
	public ProcessTemplatePane(String dirtyFlag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "process.template", Icons.processTemplateIcon, Icons.newIcon);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Parent editItemDialog(Recipe obj, TrackDirty parent)
	{
		return new RecipeEditor(obj, this, true);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Recipe createDuplicateItem(Recipe current, String newName)
	{
		Recipe result = new Recipe(current);
		result.setName(newName);
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Recipe createNewItem(String name)
	{
		return new Recipe(name);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Map<String, Recipe> getMap(Database database)
	{
		return database.getProcessTemplates();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected TableColumn<Recipe, String>[] getTableColumns(String labelPrefix)
	{
		return (TableColumn<Recipe, String>[])new TableColumn[]
			{
//				getStringPropertyValueCol(labelPrefix + ".equipment.profile", "equipmentProfile"),
			};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeRename(String oldName, String newName)
	{
		// no FKs on process templates
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeDelete(String deletedName)
	{
		// no FKs on process templates
	}

	@Override
	protected Image getIcon(Recipe recipe)
	{
		return Icons.processTemplateIcon;
	}
}
