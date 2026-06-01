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

import java.util.*;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.util.StringUtils;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * Continuous (fly) sparge: rinses a single mash bed in one pass, producing the
 * entire collected pre-boil wort plus an informational spent-grain volume.
 * <p>
 * Unlike the {@link Lauter} + {@link BatchSparge} workflow there are no first
 * runnings or sparge runnings; the output is the complete kettle wort.
 */
public class FlySparge extends ProcessStep
{
	private String inputMashVolume;
	private String outputCollectedWort;
	private String outputSpentGrain;

	/*-------------------------------------------------------------------------*/
	public FlySparge()
	{
	}

	/*-------------------------------------------------------------------------*/
	public FlySparge(
		String name,
		String description,
		String inputMashVolume,
		String outputCollectedWort,
		String outputSpentGrain,
		List<IngredientAddition> ingredients)
	{
		super(name, description, Type.FLY_SPARGE);
		this.inputMashVolume = inputMashVolume;
		this.outputCollectedWort = outputCollectedWort;
		this.outputSpentGrain = outputSpentGrain;
		setIngredients(ingredients);
	}

	/*-------------------------------------------------------------------------*/
	public FlySparge(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.FLY_SPARGE), StringUtils.getProcessString("fly.sparge.desc"), Type.FLY_SPARGE);

		this.inputMashVolume = recipe.getVolumes().getVolumeByType(Volume.Type.MASH, recipe);
		this.outputCollectedWort = StringUtils.getProcessString("fly.sparge.collected.wort", getName());
		this.outputSpentGrain = StringUtils.getProcessString("fly.sparge.spent.grain", getName());
	}

	/*-------------------------------------------------------------------------*/
	public FlySparge(FlySparge step)
	{
		super(step.getName(), step.getDescription(), Type.FLY_SPARGE);

		this.inputMashVolume = step.inputMashVolume;
		this.outputCollectedWort = step.outputCollectedWort;
		this.outputSpentGrain = step.outputSpentGrain;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, EquipmentProfile equipmentProfile, ProcessLog log)
	{
		//
		// Require the mash volume produced by mash (or infusion) before sparging.
		//
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		WaterAddition spargeWater = getCombinedWaterProfile(null);
		if (spargeWater == null)
		{
			log.addError(StringUtils.getProcessString("fly.sparge.no.water.additions"));
			return;
		}

		Volume mashVolumeIn = volumes.getVolume(inputMashVolume);

		List<FermentableAddition> grainBill = getGrainBill(mashVolumeIn);
		WaterAddition mashWater =
			(WaterAddition)mashVolumeIn.getIngredientAddition(IngredientAddition.Type.WATER);

		DensityUnit mashGravity = mashVolumeIn.getGravity();
		VolumeUnit mashVol = mashVolumeIn.getVolume();
		TemperatureUnit mashTemp = mashVolumeIn.getTemperature();
		ColourUnit mashColour = mashVolumeIn.getColour();

		//
		// Drainable wort: the same first-runnings model used by Lauter (grain and liquor at
		// conversion efficiency, minus equipment lauter dead-loss).
		//
		VolumeUnit drainableWortVol = Equations.calcWortVolume(
			grainBill,
			mashWater.getVolume(),
			equipmentProfile.getConversionEfficiency().get(PERCENTAGE));
		double drainableMl = drainableWortVol.get(MILLILITRES) - equipmentProfile.getLauterLoss().get(MILLILITRES);
		drainableWortVol = new VolumeUnit(drainableMl, MILLILITRES, true);

		// liquor held back in the grain bed after the wort drains
		double retainedMl = mashVol.get(MILLILITRES) - drainableWortVol.get(MILLILITRES);
		VolumeUnit retainedMashLiquorVol = new VolumeUnit(Math.max(0, retainedMl), MILLILITRES, true);

		VolumeUnit spargeWaterVol = spargeWater.getVolume();

		// the brewer collects the drained wort plus all the sparge liquor
		VolumeUnit collectedWortVolume = new VolumeUnit(
			drainableWortVol.get(MILLILITRES) + spargeWaterVol.get(MILLILITRES),
			MILLILITRES,
			true);

		//
		// Extract recovery: the drainable wort already carries the first-runnings extract; continuous
		// washing recovers a diminishing-returns fraction of the extract left behind in the retained
		// liquor (ideal displacement-washing approximation).
		//
		WeightUnit firstRunningsExtract = Equations.getExtractContent(drainableWortVol, mashGravity);
		WeightUnit totalMashExtract = Equations.getExtractContent(mashVol, mashGravity);

		double remainingExtractKg = Math.max(0,
			totalMashExtract.get(KILOGRAMS) - firstRunningsExtract.get(KILOGRAMS));

		double recoveryFraction = 0D;
		double retainedL = retainedMashLiquorVol.get(LITRES);
		if (retainedL > 0)
		{
			recoveryFraction = 1D - Math.exp(-spargeWaterVol.get(LITRES) / retainedL);
		}

		double recoveredExtractKg = firstRunningsExtract.get(KILOGRAMS)
			+ (remainingExtractKg * recoveryFraction);
		double spentExtractKg = Math.max(0, totalMashExtract.get(KILOGRAMS) - recoveredExtractKg);

		WeightUnit recoveredExtract = new WeightUnit(recoveredExtractKg, KILOGRAMS, true);
		WeightUnit spentExtract = new WeightUnit(spentExtractKg, KILOGRAMS, true);

		//
		// Collected wort: gravity derived from recovered extract over the collected volume; temperature
		// blends the drained wort with the sparge liquor; fermentability and colour carry from the mash.
		//
		DensityUnit collectedGravity = Equations.calcGravityFromExtract(recoveredExtract, collectedWortVolume);

		TemperatureUnit tempOut = Equations.calcCombinedTemperature(
			drainableWortVol,
			mashTemp,
			spargeWaterVol,
			spargeWater.getTemperature());

		PercentageUnit fermentabilityOut = Equations.getWortAttenuationLimit(mashTemp);

		ColourUnit collectedColour = Equations.calcColourWithVolumeChange(mashVol, mashColour, collectedWortVolume);

		List<Settings.HopBitternessFormula> reportedFormulas =
			Settings.parseReportedFormulas(Database.getInstance().getSettings());

		Volume collectedWortOut = new Volume(
			outputCollectedWort,
			Volume.Type.WORT,
			collectedWortVolume,
			tempOut,
			fermentabilityOut,
			collectedGravity,
			new PercentageUnit(0D),
			collectedColour,
			BitternessVolumes.zero());

		//
		// Spent grain: informational MASH volume holding the retained liquor, the un-recovered extract,
		// and residual colour, not intended for further extraction.
		//
		DensityUnit spentGravity = Equations.calcGravityFromExtract(spentExtract, retainedMashLiquorVol);

		Volume spentGrainOut = new Volume(
			outputSpentGrain,
			Volume.Type.MASH,
			retainedMashLiquorVol,
			mashVolumeIn.getIngredientAdditions(IngredientAddition.Type.FERMENTABLES),
			mashWater,
			mashTemp,
			spentGravity,
			collectedColour,
			null);
		PhVolumes.copyAll(mashVolumeIn, spentGrainOut);

		//
		// Conserve hop acids and bitterness: partition the mash's hop-acid masses between the two
		// outputs in proportion to the extract each receives, then reconcile reported IBU on both.
		//
		BitternessVolumes.applyVolumeChange(mashVolumeIn, collectedWortOut, collectedWortVolume, reportedFormulas);
		BitternessVolumes.applyVolumeChange(mashVolumeIn, spentGrainOut, retainedMashLiquorVol, reportedFormulas);

//		HopAcidVolumes.applySplit(
//			mashVolumeIn,
//			mashVol,
//			new VolumeUnit(recoveredExtractKg, KILOGRAMS, true),
//			collectedWortOut,
//			new VolumeUnit(spentExtractKg, KILOGRAMS, true),
//			spentGrainOut);

		HopAcidVolumes.applySplit(
			mashVolumeIn,
			mashVolumeIn.getVolume(),
			collectedWortVolume,
			collectedWortOut,
			spentGrainOut.getVolume(),
			spentGrainOut);

		BitternessVolumes.syncReportedDerived(collectedWortOut, reportedFormulas);
		BitternessVolumes.syncReportedDerived(spentGrainOut, reportedFormulas);

		log.addVerboseMessage(StringUtils.getProcessString("fly.sparge.recovery",
			spargeWaterVol.describe(LITRES),
			collectedWortVolume.describe(LITRES),
			collectedGravity.describe(SPECIFIC_GRAVITY),
			String.format("%.3f", recoveryFraction)));

		volumes.addOrUpdateVolume(outputCollectedWort, collectedWortOut);
		volumes.addOrUpdateVolume(outputSpentGrain, spentGrainOut);
	}

	/*-------------------------------------------------------------------------*/
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		if (!volumes.contains(inputMashVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputMashVolume));
			return false;
		}
		return true;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		if (!validateInputVolumes(recipe.getVolumes(), log))
		{
			return;
		}

		recipe.getVolumes().addVolume(outputCollectedWort, new Volume(Volume.Type.WORT));
		recipe.getVolumes().addVolume(outputSpentGrain, new Volume(Volume.Type.MASH));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(IngredientAddition.Type.values());
	}

	/*-------------------------------------------------------------------------*/
	private List<FermentableAddition> getGrainBill(Volume mashVolume)
	{
		List<FermentableAddition> result = new ArrayList<>();
		for (IngredientAddition ia : mashVolume.getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add((FermentableAddition)ia);
			}
		}
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Map<String, String> describeProperties()
	{
		Map<String, String> result = new LinkedHashMap<>();
		result.put("inputMashVolume", String.valueOf(inputMashVolume));
		result.put("outputCollectedWort", String.valueOf(outputCollectedWort));
		result.put("outputSpentGrain", String.valueOf(outputSpentGrain));
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("fly.sparge.step.desc");
	}

	/*-------------------------------------------------------------------------*/
	public String getInputMashVolume()
	{
		return inputMashVolume;
	}

	public void setInputMashVolume(String inputMashVolume)
	{
		this.inputMashVolume = inputMashVolume;
	}

	public String getOutputCollectedWort()
	{
		return outputCollectedWort;
	}

	public void setOutputCollectedWort(String outputCollectedWort)
	{
		this.outputCollectedWort = outputCollectedWort;
	}

	public String getOutputSpentGrain()
	{
		return outputSpentGrain;
	}

	public void setOutputSpentGrain(String outputSpentGrain)
	{
		this.outputSpentGrain = outputSpentGrain;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Collection<String> getInputVolumes()
	{
		return inputMashVolume == null ? Collections.emptyList() : Collections.singletonList(inputMashVolume);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		ArrayList<String> result = new ArrayList<>();
		if (outputCollectedWort != null)
		{
			result.add(outputCollectedWort);
		}
		if (outputSpentGrain != null)
		{
			result.add(outputSpentGrain);
		}
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.WATER)
			{
				WaterAddition wa = (WaterAddition)ia;
				result.add(
					StringUtils.getDocString(
						"fly.sparge.water",
						wa.describe(),
						wa.getTemperature().describe(CELSIUS)));
			}
			else
			{
				result.add(StringUtils.getDocString("additions.generic", ia.describe()));
			}
		}

		Volume collectedWort = getRecipe().getVolumes().getVolume(outputCollectedWort);
		result.add(StringUtils.getDocString(
			"fly.sparge.collected.wort",
			collectedWort.getVolume().describe(LITRES),
			collectedWort.getGravity().describe(SPECIFIC_GRAVITY)));

		Volume spentGrain = getRecipe().getVolumes().getVolume(outputSpentGrain);
		result.add(StringUtils.getDocString(
			"fly.sparge.spent.grain",
			spentGrain.getVolume().describe(LITRES)));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep clone(String newName)
	{
		return new FlySparge(
			newName,
			this.getDescription(),
			this.inputMashVolume,
			StringUtils.getProcessString("fly.sparge.collected.wort", newName),
			StringUtils.getProcessString("fly.sparge.spent.grain", newName),
			cloneIngredients(this.getIngredientAdditions()));
	}
}
