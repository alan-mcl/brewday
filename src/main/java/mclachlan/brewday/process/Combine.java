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
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.ui.UiQuantityDisplay;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.YeastAddition;
import mclachlan.brewday.recipe.YeastCulture;

import static mclachlan.brewday.math.Quantity.Unit.MILLILITRES;

/**
 * Combines two volumes into one.
 */
public class Combine extends FluidVolumeProcessStep
{
	private static final double PITCH_COMBINE_STARTER_FRACTION_WARN = 0.15D;

	private String inputVolume2;

	/** When true, allows WORT + BEER blend (starter into main wort) with WORT output. */
	private boolean pitchCombine;

	/*-------------------------------------------------------------------------*/
	public Combine()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Combine(
		String name,
		String description,
		String inputVolume,
		String inputVolume2,
		String outputVolume)
	{
		this(name, description, inputVolume, inputVolume2, outputVolume, false);
	}

	/*-------------------------------------------------------------------------*/
	public Combine(
		String name,
		String description,
		String inputVolume,
		String inputVolume2,
		String outputVolume,
		boolean pitchCombine)
	{
		super(name, description, Type.COMBINE, inputVolume, outputVolume);
		setInputVolume2(inputVolume2);
		this.pitchCombine = pitchCombine;
	}

	/*-------------------------------------------------------------------------*/
	public Combine(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.COMBINE),
			StringUtils.getProcessString("combine.desc"), Type.COMBINE, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT, recipe));
		setInputVolume2(recipe.getVolumes().getVolumeByType(Volume.Type.WORT, recipe));

		setOutputVolume(StringUtils.getProcessString("combine.output", getName()));
	}

	/*-------------------------------------------------------------------------*/
	public Combine(Combine other)
	{
		super(other.getName(), other.getDescription(), Type.COMBINE, other.getInputVolume(), other.getOutputVolume());
		this.inputVolume2 = other.inputVolume2;
		this.pitchCombine = other.pitchCombine;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, EquipmentProfile equipmentProfile,
		ProcessLog log)
	{
		//
		// Both input volumes must exist (primary input plus a second named volume).
		//
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		Volume input = getInputVolume(volumes);
		Volume input2 = volumes.getVolume(inputVolume2);

		Volume result = blendLikeCombine(
			input,
			input2,
			getOutputVolume(),
			resolveCombineOutputType(input, input2),
			pitchCombine,
			log);

		if (result == null)
		{
			return;
		}

		volumes.addOrUpdateVolume(getOutputVolume(), result);
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Blends two liquid streams using the same rules as {@link #apply(Volumes, EquipmentProfile, ProcessLog)}.
	 * The source volumes are not modified. Returns null when the pair cannot be combined.
	 */
	static Volume blendLikeCombine(
		Volume input,
		Volume input2,
		String outputName,
		Volume.Type typeOut)
	{
		return blendLikeCombine(input, input2, outputName, typeOut, false, null);
	}

	/*-------------------------------------------------------------------------*/
	static Volume blendLikeCombine(
		Volume input,
		Volume input2,
		String outputName,
		Volume.Type typeOut,
		boolean pitchCombine,
		ProcessLog log)
	{
		Volume.Type typeA = input.getType();
		Volume.Type typeB = input2.getType();

		Volume wortStream = null;
		Volume beerStream = null;

		if (typeA != typeB)
		{
			if (isWortBeerPitchPair(typeA, typeB) && typeOut == Volume.Type.BEER)
			{
				// Speise: beer + wort reserved for packaging, output typed as beer
			}
			else if (pitchCombine && isWortBeerPitchPair(typeA, typeB) && typeOut == Volume.Type.WORT)
			{
				wortStream = typeA == Volume.Type.WORT ? input : input2;
				beerStream = typeA == Volume.Type.BEER ? input : input2;
			}
			else
			{
				if (log != null)
				{
					log.addError(StringUtils.getProcessString(
						"combine.different.volume.types",
						typeA,
						typeB));
				}
				return null;
			}
		}

		ColourUnit colourOut = Equations.calcCombinedColour(
			input.getVolume(), input.getColour(),
			input2.getVolume(), input2.getColour());

		DensityUnit densityOut = Equations.calcCombinedGravity(
			input.getVolume(), input.getGravity(),
			input2.getVolume(), input2.getGravity());

		List<Settings.HopBitternessFormula> reportedFormulas =
			Settings.parseReportedFormulas(Database.getInstance().getSettings());

		TemperatureUnit tempOut = Equations.calcCombinedTemperature(
			input.getVolume(), input.getTemperature(),
			input2.getVolume(), input2.getTemperature());

		PercentageUnit abvOut = (PercentageUnit)Equations.calcCombinedLinearInterpolation(
			input.getVolume(), input.getAbv(),
			input2.getVolume(), input2.getAbv());

		PercentageUnit fermOut = (PercentageUnit)Equations.calcCombinedLinearInterpolation(
			input.getVolume(), input.getFermentability(),
			input2.getVolume(), input2.getFermentability());

		CarbonationUnit carbOut = (CarbonationUnit)Equations.calcCombinedLinearInterpolation(
			input.getVolume(), input.getCarbonation(),
			input2.getVolume(), input2.getCarbonation());

		VolumeUnit volOut = new VolumeUnit(input.getVolume().get() + input2.getVolume().get());

		Volume result = new Volume(
			outputName,
			typeOut,
			volOut,
			tempOut,
			fermOut,
			densityOut,
			abvOut,
			colourOut,
			BitternessVolumes.zero());

		BitternessVolumes.applyCombined(
			input.getVolume(),
			input,
			input2.getVolume(),
			input2,
			result,
			reportedFormulas);

		HopAcidVolumes.applyCombined(
			input,
			input.getVolume(),
			input2,
			input2.getVolume(),
			result);

		PhVolumes.applyCombined(
			input,
			input.getVolume(),
			input2,
			input2.getVolume(),
			result);

		BitternessVolumes.syncReportedDerived(result, reportedFormulas);

		result.setCarbonation(carbOut);
		result.setIngredientAdditions(mergeCombinedIngredients(input, input2));

		if (wortStream != null && beerStream != null)
		{
			DensityUnit og = wortStream.getOriginalGravity() != null
				? wortStream.getOriginalGravity()
				: wortStream.getGravity();
			if (og != null)
			{
				result.setOriginalGravity(new DensityUnit(og));
			}

			double totalMl = volOut.get(MILLILITRES);
			if (totalMl > 0D && log != null)
			{
				double starterFraction =
					beerStream.getVolume().get(MILLILITRES) / totalMl;
				if (starterFraction > PITCH_COMBINE_STARTER_FRACTION_WARN)
				{
					log.addWarning(StringUtils.getProcessString(
						"combine.pitch.combine.large.starter",
						starterFraction * 100D));
				}
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	private Volume.Type resolveCombineOutputType(Volume input, Volume input2)
	{
		if (input.getType() == input2.getType())
		{
			return input.getType();
		}
		if (pitchCombine && isWortBeerPitchPair(input.getType(), input2.getType()))
		{
			return Volume.Type.WORT;
		}
		return input.getType();
	}

	/*-------------------------------------------------------------------------*/
	static boolean isWortBeerPitchPair(Volume.Type a, Volume.Type b)
	{
		return (a == Volume.Type.WORT && b == Volume.Type.BEER)
			|| (a == Volume.Type.BEER && b == Volume.Type.WORT);
	}

	/*-------------------------------------------------------------------------*/
	static List<IngredientAddition> mergeCombinedIngredients(Volume input, Volume input2)
	{
		List<IngredientAddition> out = new ArrayList<>();
		List<YeastCulture> cultures = new ArrayList<>();
		collectYeastCulturesFromVolume(input, cultures);
		collectYeastCulturesFromVolume(input2, cultures);

		for (IngredientAddition ia : input.getIngredientAdditions())
		{
			if (!(ia instanceof YeastCulture) && !(ia instanceof YeastAddition))
			{
				out.add(ia.clone());
			}
		}
		for (IngredientAddition ia : input2.getIngredientAdditions())
		{
			if (!(ia instanceof YeastCulture) && !(ia instanceof YeastAddition))
			{
				out.add(ia.clone());
			}
		}

		out.addAll(FermentationCalculator.mergeCultures(cultures));
		return out;
	}

	/*-------------------------------------------------------------------------*/
	static void collectYeastCulturesFromVolume(Volume volume, List<YeastCulture> cultures)
	{
		cultures.addAll(volume.getYeastCultures());
		for (IngredientAddition ia : volume.getIngredientAdditions())
		{
			if (ia instanceof YeastAddition pitch)
			{
				cultures.add(YeastCulture.fromPitch(pitch));
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		if (!super.validateInputVolumes(volumes, log) || !volumes.contains(inputVolume2))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputVolume2));
			return false;
		}
		return true;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Map<String, String> describeProperties()
	{
		Map<String, String> result = new LinkedHashMap<>();
		result.put("inputVolume", String.valueOf(getInputVolume()));
		result.put("inputVolume2", String.valueOf(getInputVolume2()));
		result.put("outputVolume", String.valueOf(getOutputVolume()));
		result.put("pitchCombine", String.valueOf(pitchCombine));
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString(
			"combine.step.desc",
			getInputVolume(),
			getInputVolume2());
	}

	/*-------------------------------------------------------------------------*/

	public String getInputVolume2()
	{
		return inputVolume2;
	}

	public void setInputVolume2(String inputVolume2)
	{
		this.inputVolume2 = inputVolume2;
	}

	public boolean isPitchCombine()
	{
		return pitchCombine;
	}

	public void setPitchCombine(boolean pitchCombine)
	{
		this.pitchCombine = pitchCombine;
	}

	@Override
	public Collection<String> getInputVolumes()
	{
		return Arrays.asList(getInputVolume(), getInputVolume2());
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		Volume volume = getRecipe().getVolumes().getVolume(getOutputVolume());

		return List.of(
			StringUtils.getDocString(
				"combine.doc",
				this.getInputVolume(),
				this.getInputVolume2(),
				UiQuantityDisplay.describe(volume.getVolume())));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep clone(String newName)
	{
		Combine c = new Combine(
			newName,
			getDescription(),
			getInputVolume(),
			getInputVolume2(),
			StringUtils.getProcessString("combine.output", newName),
			pitchCombine);
		return c;
	}
}
