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
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 *
 */
public class StepSerialiser implements V2SerialiserMap<ProcessStep>
{
	private final IngredientAdditionSerialiser ingredientAdditionSerialiser = new IngredientAdditionSerialiser();

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
				result.put("outputMashVolume", ((Mash)processStep).getOutputMashVolume());
				result.put("duration", ((Mash)processStep).getDuration().get(Quantity.Unit.MINUTES));
				result.put("grainTemp", ((Mash)processStep).getGrainTemp().get(Quantity.Unit.CELSIUS));
				break;
			case MASH_INFUSION:
				result.put("inputMashVolume", ((MashInfusion)processStep).getInputMashVolume());
				result.put("outputMashVolume", ((MashInfusion)processStep).getOutputMashVolume());
				result.put("rampTime", ((MashInfusion)processStep).getRampTime().get(Quantity.Unit.MINUTES));
				result.put("standTime", ((MashInfusion)processStep).getStandTime().get(Quantity.Unit.MINUTES));
				break;
			case LAUTER:
				result.put("inputMashVolume", ((Lauter)processStep).getInputMashVolume());
				result.put("outputLauteredMashVolume", ((Lauter)processStep).getOutputLauteredMashVolume());
				result.put("outputFirstRunnings", ((Lauter)processStep).getOutputFirstRunnings());
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
			case HEAT:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				result.put("targetTemp", ((Heat)processStep).getTargetTemp().get(Quantity.Unit.CELSIUS));
				result.put("rampTime", ((Heat)processStep).getRampTime().get(Quantity.Unit.MINUTES));
				result.put("standTime", ((Heat)processStep).getStandTime().get(Quantity.Unit.MINUTES));
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
			case SPLIT:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				result.put("splitType", ((Split)processStep).getSplitType());
				if (((Split)processStep).getSplitPercent() != null)
				{
					result.put("splitPercent", ((Split)processStep).getSplitPercent().get(Quantity.Unit.PERCENTAGE));
				}
				if (((Split)processStep).getSplitVolume() != null)
				{
					result.put("splitVolume", ((Split)processStep).getSplitVolume().get(Quantity.Unit.LITRES));
				}
				result.put("outputVolume2", ((Split)processStep).getOutputVolume2());
				break;
			case COMBINE:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("inputVolume2", ((Combine)processStep).getInputVolume2());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
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
					new TimeUnit((Double)map.get("duration"), Quantity.Unit.MINUTES, false),
					new TemperatureUnit((Double)map.get("grainTemp")));

			case MASH_INFUSION:
				return new MashInfusion(
					name,
					desc,
					(String)map.get("inputMashVolume"),
					(String)map.get("outputMashVolume"),
					new TimeUnit((Double)map.get("rampTime"), Quantity.Unit.MINUTES),
					new TimeUnit((Double)map.get("standTime"), Quantity.Unit.MINUTES));

			case LAUTER:
				return new Lauter(
					name,
					desc,
					(String)map.get("inputMashVolume"),
					(String)map.get("outputLauteredMashVolume"),
					(String)map.get("outputFirstRunnings"));

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

			case HEAT:
				return new Heat(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					new TemperatureUnit((Double)map.get("targetTemp")),
					new TimeUnit((Double)map.get("rampTime"), Quantity.Unit.MINUTES),
					new TimeUnit((Double)map.get("standTime"), Quantity.Unit.MINUTES));

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

			case SPLIT:
				String st = (String)map.get("splitType");

				Split.Type splitType = st==null ? Split.Type.PERCENTAGE : Split.Type.valueOf(st);

				Double splitPercent = (Double)map.get("splitPercent");
				Double splitVolume = (Double)map.get("splitVolume");

				return new Split(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					splitType,
					splitPercent == null ? null : new PercentageUnit(splitPercent),
					splitVolume == null ? null : new VolumeUnit(splitVolume, Quantity.Unit.LITRES),
					(String)map.get("outputVolume2"));

			case COMBINE:
				return new Combine(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("inputVolume2"),
					(String)map.get("outputVolume"));

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
