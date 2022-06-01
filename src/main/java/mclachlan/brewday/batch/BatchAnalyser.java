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

package mclachlan.brewday.batch;

import java.util.*;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.Recipe;

public class BatchAnalyser
{
	/**
	 * Return a list of strings representing the analysis of estimates vs
	 * measurements for the given batch.
	 *
	 * @param batch The batch to analyse
	 * @return A list of strings. These have already been pulled out of the
	 * resource bundle and are ready for rendering on the UI.
	 */
	public List<String> getBatchAnalysis(Batch batch)
	{
		List<String> result = new ArrayList<String>();
		Recipe recipe = Database.getInstance().getRecipes().get(batch.getRecipe());
		EquipmentProfile equipmentProfile = Database.getInstance().getEquipmentProfiles().get(recipe.getEquipmentProfile());
		Set<String> outputVolumes = recipe.getVolumes().getOutputVolumes();

		// output analysis of mash steps
		for (ProcessStep step : recipe.getSteps())
		{
			if (step instanceof Lauter)
			{
				Volumes fullConversionVolumes = new Volumes();

				EquipmentProfile fullConversionProfile = new EquipmentProfile(equipmentProfile);
				fullConversionProfile.setConversionEfficiency(new PercentageUnit(1));

				recipe.run(fullConversionVolumes, fullConversionProfile, new ProcessLog());

				String firstRunningsVolName = ((Lauter)step).getOutputFirstRunnings();
				Volume firstRunnings = fullConversionVolumes.getVolume(firstRunningsVolName);

				// theoretical max mash efficiency
				DensityUnit gravityMax = firstRunnings.getGravity();

				Volume measFirstRunnings = batch.getActualVolumes().getVolume(firstRunningsVolName);
				DensityUnit gravityMeas = measFirstRunnings.getGravity();

				Double mashConversionEfficiency = null;

				if (!gravityMeas.isEstimated())
				{
					mashConversionEfficiency = (gravityMeas.get(Quantity.Unit.PLATO) * measFirstRunnings.getVolume().get(Quantity.Unit.LITRES)) /
						(gravityMax.get(Quantity.Unit.PLATO) * firstRunnings.getVolume().get(Quantity.Unit.LITRES));
				}

				result.add(StringUtils.getUiString("batch.analysis.mash", step.getName()));
				result.add(
					getMsg(
						equipmentProfile.getConversionEfficiency().get(),
						mashConversionEfficiency,
						"batch.analysis.mash.conversion.efficiency"));
			}
		}

		result.add("");

		// output analysis for the volumes packaged
		for (String outputVolume : outputVolumes)
		{
			Volume estV = recipe.getVolumes().getVolume(outputVolume);
			Volume measV = batch.getActualVolumes().getVolume(outputVolume);

			result.add(StringUtils.getUiString("batch.analysis.packaged", estV.getName()));

			if (measV.getType() == Volume.Type.BEER)
			{
				// ABV

				Double measuredAbv = null;
				if (measV.getAbv() != null && !measV.getAbv().isEstimated())
				{
					measuredAbv = measV.getAbv().get();
				}
				result.add(getMsg(estV.getAbv().get(), measuredAbv, "batch.analysis.abv"));


				// Apparent Attenuation

				double estApparentAtten = Equations.calcAttenuation(
					estV.getOriginalGravity(), estV.getGravity());
				Double measApparentAtten = null;

				if (measV.getOriginalGravity() != null && measV.getGravity() != null)
				{
					if (!measV.getOriginalGravity().isEstimated() && !measV.getGravity().isEstimated())
					{
						measApparentAtten = Equations.calcAttenuation(
							measV.getOriginalGravity(), measV.getGravity());
					}
				}
				result.add(getMsg(estApparentAtten, measApparentAtten, "batch.analysis.apparent.attenuation"));

				// carbonation
				result.add(StringUtils.getUiString("batch.analysis.carbonation",
					measV.getCarbonation().get(Quantity.Unit.VOLUMES)));
			}

			result.add("");
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	private String getMsg(
		Double estimated,
		Double measured,
		String msgKey)
	{
		return StringUtils.getUiString(msgKey,
			estimated * 100,
			measured == null ?
				StringUtils.getUiString("quantity.unknown")
				: StringUtils.getUiString("quantity.percent", measured * 100));
	}
}