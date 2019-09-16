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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.ui.swing;

import java.util.*;
import javax.swing.*;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Fermentable;

/**
 *
 */
public class FermentablesReferencePanel extends JPanel
{
	private FermentablesTableModel model;
	private int dirtyFlag;

	public FermentablesReferencePanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		model = new FermentablesTableModel();
		JTable table = new JTable(model);

		this.add(new JScrollPane(table));

		refresh();
	}

	public void refresh()
	{
		Map<String, Fermentable> dbFermentables = Database.getInstance().getFermentables();

		List<Fermentable> fermentables = new ArrayList<Fermentable>(dbFermentables.values());
		fermentables.sort(Comparator.comparing(Fermentable::getName));

		model.clear();
		model.addAll(fermentables);
	}

}
