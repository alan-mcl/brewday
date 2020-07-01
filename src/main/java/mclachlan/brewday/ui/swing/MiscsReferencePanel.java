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

import java.util.*;
import javax.swing.*;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Misc;

/**
 *
 */
public class MiscsReferencePanel extends JPanel
{
	private MiscsTableModel model;
	private int dirtyFlag;

	public MiscsReferencePanel(int dirtyFlag)
	{
		this.dirtyFlag = dirtyFlag;

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		model = new MiscsTableModel();
		JTable table = new JTable(model);

		this.add(new JScrollPane(table));

		refresh();
	}

	public void refresh()
	{
		Map<String, Misc> dbMiscs = Database.getInstance().getMiscs();

		List<Misc> miscs = new ArrayList<Misc>(dbMiscs.values());
		miscs.sort(Comparator.comparing(Misc::getName));

		model.getData().clear();
		model.getData().addAll(miscs);
	}

}
