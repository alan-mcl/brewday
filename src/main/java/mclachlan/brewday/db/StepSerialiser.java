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

package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.v2.V2SerialiserMap;
import mclachlan.brewday.db.v2.V2Utils;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.math.TemperatureUnit;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.math.VolumeUnit;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 *
 */
public class StepSerialiser implements V2SerialiserMap<ProcessStep>
{
	private IngredientAdditionSerialiser ingredientAdditionSerialiser = new IngredientAdditionSerialiser();

	/*-------------------------------------------------------------------------*/
	@Override
	public Map toMap(ProcessStep processStep)
	{
		Map result = new HashMap();

		result.put("name", processStep.getName());
		result.put("description", processStep.getDescription());
		result.put("type", processStep.getType().name());

		switch (processStep.getType())
		{
			case MASH:
				result.put("outputFirstRunnings", ((Mash)processStep).getOutputFirstRunnings());
				result.put("outputMashVolume", ((Mash)processStep).getOutputMashVolume());
				result.put("duration", ((Mash)processStep).getDuration().get(Quantity.Unit.MINUTES));
				result.put("grainTemp", ((Mash)processStep).getGrainTemp().get(Quantity.Unit.CELSIUS));
				break;
			case MASH_INFUSION:
				result.put("inputMashVolume", ((MashInfusion)processStep).getInputMashVolume());
				result.put("outputMashVolume", ((MashInfusion)processStep).getOutputMashVolume());
				result.put("duration", ((MashInfusion)processStep).getDuration());
				break;
			case BATCH_SPARGE:
				result.put("mashVolume", ((BatchSparge)processStep).getMashVolume());
				result.put("wortVolume", ((BatchSparge)processStep).getWortVolume());
				result.put("outputCombinedWortVolume", ((BatchSparge)processStep).getOutputCombinedWortVolume());
				result.put("outputMashVolume", ((BatchSparge)processStep).getOutputMashVolume());
				result.put("outputSpargeRunnings", ((BatchSparge)processStep).getOutputSpargeRunnings());
				break;
			case BOIL:
				result.put("inputWortVolume", ((Boil)processStep).getInputWortVolume());
				result.put("outputWortVolume", ((Boil)processStep).getOutputWortVolume());
				result.put("duration", ((Boil)processStep).getDuration().get(Quantity.Unit.MINUTES));
				break;
			case DILUTE:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				break;
			case COOL:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				result.put("targetTemp", ((Cool)processStep).getTargetTemp().get(Quantity.Unit.CELSIUS));
				break;
			case FERMENT:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				result.put("temp", ((Ferment)processStep).getTemperature().get(Quantity.Unit.CELSIUS));
				result.put("duration", ((Ferment)processStep).getDuration().get(Quantity.Unit.DAYS));
				break;
			case STAND:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				result.put("duration", ((Stand)processStep).getDuration().get(Quantity.Unit.MINUTES));
				break;
			case SPLIT_BY_PERCENT:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				result.put("splitPercent", ((SplitByPercent)processStep).getSplitPercent());
				result.put("outputVolume2", ((SplitByPercent)processStep).getOutputVolume2());
				break;
			case PACKAGE:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				result.put("packagingLoss", ((PackageStep)processStep).getPackagingLoss().get(Quantity.Unit.MILLILITRES));
				result.put("styleId", ((PackageStep)processStep).getStyleId());
				break;
			default:
				throw new BrewdayException("Invalid process step: "+ processStep.getType());
		}

		result.put("ingredients",
			V2Utils.serialiseList(processStep.getIngredients(), ingredientAdditionSerialiser));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep fromMap(Map map)
	{
		String name = (String)map.get("name");
		String desc = (String)map.get("description");
		ProcessStep.Type type = ProcessStep.Type.valueOf((String)map.get("type"));

		List<IngredientAddition> ingredientAdditions = V2Utils.deserialiseList(
			(List)map.get("ingredients"), ingredientAdditionSerialiser);

		switch (type)
		{
			case MASH:
				return new Mash(
					name,
					desc,
					ingredientAdditions,
					(String)map.get("outputMashVolume"),
					(String)map.get("outputFirstRunnings"),
					new TimeUnit((Double)map.get("duration"), Quantity.Unit.MINUTES, false),
					new TemperatureUnit((Double)map.get("grainTemp")));

			case MASH_INFUSION:
				return new MashInfusion(
					name,
					desc,
					(String)map.get("inputMashVolume"),
					(String)map.get("outputMashVolume"),
					(Double)map.get("duration"));

			case BATCH_SPARGE:
				return new BatchSparge(
					name,
					desc,
					(String)map.get("mashVolume"),
					(String)map.get("wortVolume"),
					(String)map.get("outputCombinedWortVolume"),
					(String)map.get("outputSpargeRunnings"),
					(String)map.get("outputMashVolume"),
					ingredientAdditions);

			case BOIL:
				return new Boil(
					name,
					desc,
					(String)map.get("inputWortVolume"),
					(String)map.get("outputWortVolume"),
					ingredientAdditions,
					new TimeUnit((Double)map.get("duration"), Quantity.Unit.MINUTES, false));

			case DILUTE:
				return new Dilute(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					ingredientAdditions);

			case COOL:
				return new Cool(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					new TemperatureUnit((Double)map.get("targetTemp")));

			case FERMENT:
				return new Ferment(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					new TemperatureUnit((Double)map.get("temp")),
					new TimeUnit((Double)map.get("duration"), Quantity.Unit.DAYS, false),
					ingredientAdditions);

			case STAND:
				return new Stand(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					new TimeUnit((Double)map.get("duration"), Quantity.Unit.MINUTES, false));

			case SPLIT_BY_PERCENT:
				return new SplitByPercent(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					(Double)map.get("splitPercent"),
					(String)map.get("outputVolume2"));

			case PACKAGE:
				return new PackageStep(
					name,
					desc,
					ingredientAdditions,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					new VolumeUnit((Double)map.get("packagingLoss")),
					(String)map.get("styleId"));

			default:
				throw new BrewdayException("Invalid process step: "+ type);
		}
	}
}
