
/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.math.WaterParameters;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.style.Style;

/**
 * Creates a clean database for shipping.
 */
public class CreateProdDb
{
	public static void main(String[] args) throws IOException
	{
		try
		{
			System.out.println(" *** Creating prod DB");

			Database testDb = new Database("test_data/test_db");
			testDb.loadAll();

			Database prodDb = new Database("data/db");
			prodDb.loadAll();

			// clear the prod db
			prodDb.getWaters().clear();
			prodDb.getFermentables().clear();
			prodDb.getHops().clear();
			prodDb.getYeasts().clear();
			prodDb.getMiscs().clear();
			prodDb.getInventory().clear();
			prodDb.getRecipes().clear();
			prodDb.getBatches().clear();
			prodDb.getEquipmentProfiles().clear();
			prodDb.getProcessTemplates().clear();
			prodDb.getStyles().clear();

			String key = null;

			// water
			BufferedReader waters = new BufferedReader(new FileReader(new File("src/dist/water.prod"), StandardCharsets.UTF_8));
			do
			{
				key = waters.readLine();
				if (key != null)
				{
					Water obj = testDb.getWaters().get(key.trim());
					if (obj == null)
					{
						throw new BrewdayException("not found: [" + key + "]");
					}
					prodDb.getWaters().put(key, obj);
				}
			}
			while (key != null);

			// ferm
			BufferedReader fermentables = new BufferedReader(new FileReader(new File("src/dist/fermentables.prod"), StandardCharsets.UTF_8));
			do
			{
				key = fermentables.readLine();
				if (key != null)
				{
					Fermentable obj = testDb.getFermentables().get(key.trim());
					if (obj == null)
					{
						throw new BrewdayException("not found: [" + key + "]");
					}
					prodDb.getFermentables().put(key, obj);
				}
			}
			while (key != null);

			// hops
			BufferedReader hops = new BufferedReader(new FileReader(new File("src/dist/hops.prod"), StandardCharsets.UTF_8));
			do
			{
				key = hops.readLine();
				if (key != null)
				{
					Hop obj = testDb.getHops().get(key.trim());
					if (obj == null)
					{
						throw new BrewdayException("not found: [" + key + "]");
					}
					prodDb.getHops().put(key, obj);
				}
			}
			while (key != null);

			// yeast
			BufferedReader yeasts = new BufferedReader(new FileReader(new File("src/dist/yeast.prod"), StandardCharsets.UTF_8));

			do
			{
				key = yeasts.readLine();
				if (key != null)
				{
					Yeast obj = testDb.getYeasts().get(key.trim());
					if (obj == null)
					{
						throw new BrewdayException("not found: [" + key + "]");
					}
					prodDb.getYeasts().put(key, obj);
				}
			}
			while (key != null);

			// misc
			BufferedReader miscs = new BufferedReader(new FileReader(new File("src/dist/miscs.prod"), StandardCharsets.UTF_8));
			do
			{
				key = miscs.readLine();
				if (key != null)
				{
					Misc obj = testDb.getMiscs().get(key.trim());
					if (obj == null)
					{
						throw new BrewdayException("not found: [" + key + "]");
					}
					prodDb.getMiscs().put(key, obj);
				}
			}
			while (key != null);

			// processTemplate
			BufferedReader processTemplates = new BufferedReader(new FileReader(new File("src/dist/processtemplates.prod"), StandardCharsets.UTF_8));
			do
			{
				key = processTemplates.readLine();
				if (key != null)
				{
					Recipe obj = testDb.getProcessTemplates().get(key.trim());
					if (obj == null)
					{
						throw new BrewdayException("not found: [" + key + "]");
					}
					prodDb.getProcessTemplates().put(key, obj);
				}
			}
			while (key != null);

			// style
			BufferedReader styles = new BufferedReader(new FileReader(new File("src/dist/styles.prod"), StandardCharsets.UTF_8));
			do
			{
				key = styles.readLine();
				if (key != null)
				{
					Style obj = testDb.getStyles().get(key.trim());
					if (obj == null)
					{
						throw new BrewdayException("not found: [" + key + "]");
					}
					prodDb.getStyles().put(key, obj);
				}
			}
			while (key != null);

			// equipmentProfile
			BufferedReader equipmentProfiles = new BufferedReader(new FileReader(new File("src/dist/equipmentprofiles.prod"), StandardCharsets.UTF_8));
			do
			{
				key = equipmentProfiles.readLine();
				if (key != null)
				{
					EquipmentProfile obj = testDb.getEquipmentProfiles().get(key.trim());
					if (obj == null)
					{
						throw new BrewdayException("not found: [" + key + "]");
					}
					prodDb.getEquipmentProfiles().put(key, obj);
				}
			}
			while (key != null);

			// water parameters
			BufferedReader waterParameters = new BufferedReader(new FileReader(new File("src/dist/waterParameters.prod"), StandardCharsets.UTF_8));
			do
			{
				key = waterParameters.readLine();
				if (key != null)
				{
					WaterParameters obj = testDb.getWaterParameters().get(key.trim());
					if (obj == null)
					{
						throw new BrewdayException("not found: [" + key + "]");
					}
					prodDb.getWaterParameters().put(key, obj);
				}
			}
			while (key != null);

			// settings
			// should we do this? not doing it for now

			prodDb.saveAll();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
