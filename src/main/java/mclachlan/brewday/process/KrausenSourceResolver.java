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

package mclachlan.brewday.process;

import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.util.StringUtils;

/**
 * Resolves a read-only krausen liquid snapshot from another recipe (or the
 * current recipe run) without modifying the source recipe or database.
 */
public final class KrausenSourceResolver
{
	private KrausenSourceResolver()
	{
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * @return a copy of the krausen source volume, or null if resolution failed
	 * (errors added to {@code log}).
	 */
	public static Volume resolveSnapshot(
		String recipeName,
		String volumeName,
		Recipe packagingRecipe,
		Volumes packagingVolumes,
		ProcessLog log)
	{
		if (recipeName == null || recipeName.isBlank())
		{
			log.addError(StringUtils.getProcessString("package.krausen.no.recipe"));
			return null;
		}
		if (volumeName == null || volumeName.isBlank())
		{
			log.addError(StringUtils.getProcessString("package.krausen.no.volume"));
			return null;
		}

		Recipe source = Database.getInstance().getRecipes().get(recipeName);
		if (source == null)
		{
			log.addError(StringUtils.getProcessString("package.krausen.recipe.missing", recipeName));
			return null;
		}

		if (packagingRecipe != null
			&& recipeName.equals(packagingRecipe.getName())
			&& packagingVolumes != null
			&& packagingVolumes.contains(volumeName))
		{
			return copySnapshot(packagingVolumes.getVolume(volumeName));
		}

		EquipmentProfile equipment = Database.getInstance().getEquipmentProfiles()
			.get(source.getEquipmentProfile());
		if (equipment == null)
		{
			log.addError(StringUtils.getProcessString(
				"equipment.invalid.profile",
				source.getEquipmentProfile()));
			return null;
		}

		Volumes tmpVolumes = new Volumes();
		ProcessLog tmpLog = new ProcessLog();
		source.run(tmpVolumes, equipment, tmpLog);

		for (String msg : tmpLog.getErrors())
		{
			log.addError(StringUtils.getProcessString("package.krausen.source.run.error", recipeName, msg));
		}
		if (!tmpLog.getErrors().isEmpty())
		{
			return null;
		}

		if (!tmpVolumes.contains(volumeName))
		{
			log.addError(StringUtils.getProcessString(
				"package.krausen.volume.missing",
				volumeName,
				recipeName));
			return null;
		}

		return copySnapshot(tmpVolumes.getVolume(volumeName));
	}

	/*-------------------------------------------------------------------------*/
	private static Volume copySnapshot(Volume source)
	{
		return new Volume("_krausen", source);
	}
}
