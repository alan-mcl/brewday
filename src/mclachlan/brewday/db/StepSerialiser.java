package mclachlan.brewday.db;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.db.v2.V2Serialiser;
import mclachlan.brewday.db.v2.V2Utils;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 *
 */
public class StepSerialiser implements V2Serialiser<ProcessStep>
{
	private IngredientSerialiser ingredientSerialiser = new IngredientSerialiser();

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
				result.put("duration", ((Mash)processStep).getDuration());
				result.put("grainTemp", ((Mash)processStep).getGrainTemp());
				result.put("tunLoss", ((Mash)processStep).getTunLoss());
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
				result.put("duration", ((Boil)processStep).getDuration());
				break;
			case DILUTE:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				result.put("volumeTarget", ((Dilute)processStep).getVolumeTarget());
				result.put("additionTemp", ((Dilute)processStep).getAdditionTemp());
				break;
			case COOL:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				result.put("targetTemp", ((Cool)processStep).getTargetTemp());
				break;
			case FERMENT:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				result.put("temp", ((Ferment)processStep).getTemperature());
				break;
			case STAND:
				result.put("inputVolume", ((FluidVolumeProcessStep)processStep).getInputVolume());
				result.put("outputVolume", ((FluidVolumeProcessStep)processStep).getOutputVolume());
				result.put("duration", ((Stand)processStep).getDuration());
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
				result.put("packagingLoss", ((PackageStep)processStep).getPackagingLoss());
				break;
			default:
				throw new BrewdayException("Invalid process step: "+ processStep.getType());
		}

		result.put("ingredients",
			V2Utils.serialiseList(processStep.getIngredients(), ingredientSerialiser));

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
			(List)map.get("ingredients"), ingredientSerialiser);

		switch (type)
		{
			case MASH:
				return new Mash(
					name,
					desc,
					ingredientAdditions,
					(String)map.get("outputMashVolume"),
					(String)map.get("outputFirstRunnings"),
					(Double)map.get("duration"),
					(Double)map.get("grainTemp"),
					(Double)map.get("tunLoss"));

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
					(String)map.get("outputMashVolume"),
					(String)map.get("outputSpargeRunnings"),
					ingredientAdditions);

			case BOIL:
				return new Boil(
					name,
					desc,
					(String)map.get("inputWortVolume"),
					(String)map.get("outputWortVolume"),
					ingredientAdditions,
					(Double)map.get("duration"));

			case DILUTE:
				return new Dilute(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					(Double)map.get("volumeTarget"),
					(Double)map.get("additionTemp"));

			case COOL:
				return new Cool(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					(Double)map.get("targetTemp"));

			case FERMENT:
				return new Ferment(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					(Double)map.get("temp"),
					ingredientAdditions);

			case STAND:
				return new Stand(
					name,
					desc,
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					(Double)map.get("duration"));

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
					(String)map.get("inputVolume"),
					(String)map.get("outputVolume"),
					(Double)map.get("packagingLoss"));

			default:
				throw new BrewdayException("Invalid process step: "+ type);
		}
	}
}
