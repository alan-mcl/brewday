package mclachlan.brewday;

import java.util.*;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Volume;
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
		Set<String> outputVolumes = recipe.getVolumes().getOutputVolumes();

		for (String outputVolume : outputVolumes)
		{
			Volume estV = recipe.getVolumes().getVolume(outputVolume);
			Volume measV = batch.getActualVolumes().getVolume(outputVolume);

			result.add(StringUtils.getUiString("batch.analysis.packaged", estV.getName()));

			if (measV.getType() == Volume.Type.BEER)
			{
				// ABV

				Double measuredAbv = null;
				if (!measV.getAbv().isEstimated())
				{
					measuredAbv = measV.getAbv().get();
				}
				result.add(getMsg(estV.getAbv().get(), measuredAbv, "batch.analysis.abv"));


				// Apparent Attenuation

				double estApparentAtten = Equations.calcAttenuation(
					estV.getOriginalGravity(), estV.getGravity());
				Double measApparentAtten = null;

				if (!measV.getOriginalGravity().isEstimated() && !measV.getGravity().isEstimated())
				{
					measApparentAtten = Equations.calcAttenuation(
						measV.getOriginalGravity(), measV.getGravity());
				}
				result.add(getMsg(estApparentAtten, measApparentAtten, "batch.analysis.apparent.attenuation"));

				// carbonation
				result.add(StringUtils.getUiString("batch.analysis.carbonation",
					measV.getCarbonation().get(Quantity.Unit.VOLUMES)));
			}
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