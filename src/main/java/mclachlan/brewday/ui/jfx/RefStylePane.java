/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the stylee that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import alphanum.AlphanumComparator;
import java.util.*;
import java.util.function.*;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;
import net.miginfocom.layout.AC;

/**
 *
 */
public class RefStylePane extends V2DataObjectPane<Style>
{
	private TableColumn<Style, String> nrCol, nameCol;

	/*-------------------------------------------------------------------------*/
	public RefStylePane(String dirtyFlag, TrackDirty parent)
	{
		super(dirtyFlag, parent, "style", Icons.stylesIcon, Icons.newIcon);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected V2ObjectEditor<Style> editItemDialog(Style obj, TrackDirty parent)
	{
		return new V2ObjectEditor<>(obj, parent)
		{
			@Override
			protected void buildUi(Style obj, TrackDirty parent)
			{
				this.setColumnConstraints(new AC().count(8).gap("20",1, 3, 5));
				this.setRowConstraints(new AC().count(8).gap("20",4, 7));

				this.add(new Label(StringUtils.getUiString("style.name")));
				this.add(new Label(obj.getName()), "wrap");

				// Display Name
				addTextField(obj, parent, "style.display.name",
					Style::getDisplayName, Style::setDisplayName, "wrap");

				// Style Guide
				addTextField(obj, parent, "style.guide",
					Style::getStyleGuide, Style::setStyleGuide, "wrap");

				// Category Number
				addTextField(obj, parent, "style.category.number",
					Style::getCategoryNumber, Style::setCategoryNumber, null);

				// Category Name
				addTextField(obj, parent, "style.category",
					Style::getCategory, Style::setCategory, "wrap");

				// Style Letter
				addTextField(obj, parent, "style.letter",
					Style::getStyleLetter, Style::setStyleLetter, null);

				// Style Guide Name
				addTextField(obj, parent, "style.guide.name",
					Style::getStyleGuideName, Style::setStyleGuideName, "wrap");

				// Type
				addComboBox(obj, parent, "style.type",
					(Function<Style, Style.Type>)Style::getType,
					(BiConsumer<Style, Style.Type>)Style::setType,
					Style.Type.values(),
					"wrap");

				// Min OG
				addQuantityWidget(obj, parent, "style.og.min",
					Style::getOgMin, (BiConsumer<Style, DensityUnit>)Style::setOgMin,
					Quantity.Unit.SPECIFIC_GRAVITY, null);

				// Max OG
				addQuantityWidget(obj, parent, "style.og.max",
					Style::getOgMax, (BiConsumer<Style, DensityUnit>)Style::setOgMax,
					Quantity.Unit.SPECIFIC_GRAVITY, null);

				// Min FG
				addQuantityWidget(obj, parent, "style.fg.min",
					Style::getFgMin, (BiConsumer<Style, DensityUnit>)Style::setFgMin,
					Quantity.Unit.SPECIFIC_GRAVITY, null);

				// Max FG
				addQuantityWidget(obj, parent, "style.fg.max",
					Style::getFgMax, (BiConsumer<Style, DensityUnit>)Style::setFgMax,
					Quantity.Unit.SPECIFIC_GRAVITY, "wrap");

				// Min IBU
				addQuantityWidget(obj, parent, "style.ibu.min",
					Style::getIbuMin, (BiConsumer<Style, BitternessUnit>)Style::setIbuMin,
					Quantity.Unit.IBU, null);

				// Min IBU
				addQuantityWidget(obj, parent, "style.ibu.max",
					Style::getIbuMax, (BiConsumer<Style, BitternessUnit>)Style::setIbuMax,
					Quantity.Unit.IBU, null);

				// Min colour
				addQuantityWidget(obj, parent, "style.colour.min",
					Style::getColourMin, (BiConsumer<Style, ColourUnit>)Style::setColourMin,
					Quantity.Unit.SRM, null);

				// Min colour
				addQuantityWidget(obj, parent, "style.colour.max",
					Style::getColourMax, (BiConsumer<Style, ColourUnit>)Style::setColourMax,
					Quantity.Unit.SRM, "wrap");

				// Min carb
				addQuantityWidget(obj, parent, "style.carb.min",
					Style::getCarbMin, (BiConsumer<Style, CarbonationUnit>)Style::setCarbMax,
					Quantity.Unit.VOLUMES, null);

				// Min carb
				addQuantityWidget(obj, parent, "style.carb.max",
					Style::getCarbMax, (BiConsumer<Style, CarbonationUnit>)Style::setCarbMax,
					Quantity.Unit.VOLUMES, null);

				// Min ABV
				addQuantityWidget(obj, parent, "style.abv.min",
					Style::getAbvMin, (BiConsumer<Style, PercentageUnit>)Style::setAbvMin,
					Quantity.Unit.PERCENTAGE_DISPLAY, null);

				// Min ABV
				addQuantityWidget(obj, parent, "style.abv.max",
					Style::getAbvMax, (BiConsumer<Style, PercentageUnit>)Style::setAbvMax,
					Quantity.Unit.PERCENTAGE_DISPLAY, "wrap");

				// Notes
				addTextField(obj, parent, "style.notes",
					Style::getNotes, Style::setNotes, "span, grow, wrap");

				// Profile
				addTextArea(obj, parent, "style.profile",
					Style::getProfile, Style::setProfile, "span, grow, wrap");

				// Ingredients
				addTextArea(obj, parent, "style.ingredients",
					Style::getIngredients, Style::setIngredients, "span, grow, wrap");

				// Examples
				addTextField(obj, parent, "style.examples",
					Style::getExamples, Style::setExamples, "span, grow, wrap");
			}
		};
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Style createDuplicateItem(Style current, String newName)
	{
		Style result = new Style(current);
		result.setName(newName);
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Style createNewItem(String name)
	{
		Style style = new Style(name);
		style.setType(Style.Type.ALE);
		return style;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected Map<String, Style> getMap(Database database)
	{
		return database.getStyles();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected TableColumn<Style, String>[] getTableColumns(String labelPrefix)
	{
		nameCol = (TableColumn<Style, String>)getTable().getColumns().get(0);
		nameCol.setComparator(new AlphanumComparator());

		nrCol = getTableBuilder().getStringPropertyValueCol(
			labelPrefix + ".number", "styleNumber");
		nrCol.setComparator(new AlphanumComparator());

		return (TableColumn<Style, String>[])new TableColumn[]
			{
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".guide", "styleGuide"),
				nrCol,
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".category", "category"),
				getTableBuilder().getStringPropertyValueCol(labelPrefix + ".type", "type"),
			};

	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void tableInitialSort(TableView<Style> table)
	{
		// start sorted by "styleNumber"
		nrCol.setSortType(TableColumn.SortType.ASCENDING);
		table.getSortOrder().setAll(nrCol);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeRename(String oldName, String newName)
	{
		Database db = Database.getInstance();

		// recipes
		for (Recipe recipe : db.getRecipes().values())
		{
			for (ProcessStep step : recipe.getSteps())
			{
				if (step instanceof PackageStep)
				{
					String styleId = ((PackageStep)step).getStyleId();
					if (oldName.equals(styleId))
					{
						((PackageStep)step).setStyleId(newName);
						JfxUi.getInstance().setDirty(JfxUi.RECIPES);
						JfxUi.getInstance().setDirty(recipe);
						JfxUi.getInstance().setDirty(step);
					}
				}
			}
		}

		// todo batches?
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void cascadeDelete(String deletedName)
	{
		Database db = Database.getInstance();

		// recipes
		for (Recipe recipe : db.getRecipes().values())
		{
			for (ProcessStep step : recipe.getSteps())
			{
				if (step instanceof PackageStep)
				{
					String styleId = ((PackageStep)step).getStyleId();
					if (deletedName.equals(styleId))
					{
						((PackageStep)step).setStyleId(null);
						JfxUi.getInstance().setDirty(JfxUi.RECIPES);
						JfxUi.getInstance().setDirty(recipe);
						JfxUi.getInstance().setDirty(step);
					}
				}
			}
		}

		// todo batches?
	}

	@Override
	protected Image getIcon(Style style)
	{
		return Icons.stylesIcon;
	}
}
