package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.db.v2.V2Serialiser;
import mclachlan.brewday.equipment.EquipmentProfile;

/**
 *
 */
public class EquipmentProfileSerialiser implements V2Serialiser<EquipmentProfile>
{
	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(EquipmentProfile equipmentProfile)
	{
		Map result = new HashMap();

		result.put("name", equipmentProfile.getName());
		result.put("description", equipmentProfile.getDescription());

		result.put("mashTunVolume", equipmentProfile.getMashTunVolume());
		result.put("mashTunWeight", equipmentProfile.getMashTunWeight());
		result.put("mashTunSpecificHeat", equipmentProfile.getMashTunSpecificHeat());
		result.put("boilKettleVolume", equipmentProfile.getBoilKettleVolume());
		result.put("boilEvapourationRate", equipmentProfile.getBoilEvapourationRate());
		result.put("hopUtilisation", equipmentProfile.getHopUtilisation());
		result.put("fermenterVolume", equipmentProfile.getFermenterVolume());
		result.put("lauterLoss", equipmentProfile.getLauterLoss());
		result.put("trubAndChillerLoss", equipmentProfile.getTrubAndChillerLoss());

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public EquipmentProfile fromMap(Map<String, ?> map)
	{
		String name = (String)map.get("name");
		String description = (String)map.get("description");

		Double mashTunVolume = (Double)map.get("mashTunVolume");
		Double mashTunWeight = (Double)map.get("mashTunWeight");
		Double mashTunSpecificHeat = (Double)map.get("mashTunSpecificHeat");
		Double boilKettleVolume = (Double)map.get("boilKettleVolume");
		Double boilEvapourationRate = (Double)map.get("boilEvapourationRate");
		Double hopUtilisation = (Double)map.get("hopUtilisation");
		Double fermenterVolume = (Double)map.get("fermenterVolume");
		Double lauterLoss = (Double)map.get("lauterLoss");
		Double trubAndChillerLoss = (Double)map.get("trubAndChillerLoss");

		return new EquipmentProfile(
			name,
			description,
			mashTunVolume,
			mashTunWeight,
			mashTunSpecificHeat,
			boilKettleVolume,
			boilEvapourationRate,
			hopUtilisation,
			fermenterVolume,
			lauterLoss,
			trubAndChillerLoss);
	}
}
