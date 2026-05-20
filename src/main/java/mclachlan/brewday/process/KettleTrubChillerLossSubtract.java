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

import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.util.StringUtils;

/**
 * Kettle trub plus chiller loss from {@link EquipmentProfile} applied to an
 * in-memory volume snapshot used by Ferment-like {@code FluidVolumeProcessStep}s.
 */
final class KettleTrubChillerLossSubtract
{
	private KettleTrubChillerLossSubtract()
	{
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Subtracts kettle trub and chiller loss from {@code snapshot} when
	 * {@code removeTrubAndChillerLoss} is true.
	 *
	 * @return {@code false} when removal is requested but equipment profile is
	 * absent (after logging); {@code true} otherwise
	 */
	static boolean subtractIfEnabled(
		Volume snapshot,
		EquipmentProfile equipmentProfile,
		boolean removeTrubAndChillerLoss,
		ProcessLog log)
	{
		if (!removeTrubAndChillerLoss)
		{
			return true;
		}

		if (equipmentProfile == null)
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile", equipmentProfile));
			return false;
		}

		VolumeUnit volumeBefore = new VolumeUnit(snapshot.getVolume());
		snapshot.setVolume(new VolumeUnit(
			snapshot.getVolume().get()
				- equipmentProfile.getTrubAndChillerLoss().get()));
		HopAcidVolumes.applyVolumeLoss(snapshot, volumeBefore, snapshot.getVolume(), snapshot);
		return true;
	}
}
