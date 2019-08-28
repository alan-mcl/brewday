package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.db.v2.V2Utils;
import mclachlan.brewday.process.Volume;

/**
 *
 */
public class VolumeSerialiser implements V2SerialiserMap<Volume>
{
	private final QuantitySerialiser quantitySerialiser =
		new QuantitySerialiser(true);

	private final IngredientAdditionSerialiser ingredientAdditionSerialiser =
		new IngredientAdditionSerialiser();

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(Volume volume)
	{
		Map result = new HashMap();

		result.put("name", volume.getName());
		result.put("type", volume.getType().name());

		result.put("metrics",
			V2Utils.serialiseMap(volume.getMetrics(), quantitySerialiser));
//		result.put("ingredientAdditions",
//			V2Utils.serialiseList(volume.getIngredientAdditions(), ingredientAdditionSerialiser));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Volume fromMap(Map<String, ?> map)
	{
		String name = (String)map.get("name");
		Volume.Type type = Volume.Type.valueOf((String)map.get("type"));

		Map<String, Object> mm = (Map<String, Object>)map.get("metrics");
		Map<Volume.Metric, Object> mmm = new HashMap<>();
		for (Map.Entry<String, Object> e : mm.entrySet())
		{
			mmm.put(Volume.Metric.valueOf(e.getKey()), e.getValue());
		}

		Map metrics = V2Utils.deserialiseMap(mmm, quantitySerialiser);
//		List ingredientAdditions =
//			V2Utils.deserialiseList((List)map.get("ingredientAdditions"), ingredientAdditionSerialiser);

		return new Volume(name, type, metrics, new ArrayList<>());
	}
}
