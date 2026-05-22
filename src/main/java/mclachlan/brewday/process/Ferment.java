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
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.YeastAddition;
import mclachlan.brewday.recipe.YeastCulture;
import mclachlan.brewday.recipe.YeastSourceType;
import mclachlan.brewday.Settings;
import mclachlan.brewday.util.StringUtils;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 *
 */
public class Ferment extends FluidVolumeProcessStep
{
	/**
	 * Role of this fermentation step in the recipe.
	 */
	public enum FermentType
	{
		PRIMARY,
		SECONDARY,
		TERTIARY,
		STARTER,
		CONDITIONING,
		SOURING
	}

	/** fermentation time */
	private TimeUnit duration;

	/** fermentation temperature at phase start, in C */
	private TemperatureUnit startTemp;

	/** fermentation temperature at phase end, in C */
	private TemperatureUnit endTemp;

	/** calculated */
	private DensityUnit estimatedFinalGravity = new DensityUnit();

	/** should this step remove the equipment profile trub & chiller loss? */
	private boolean removeTrubAndChillerLoss;

	private FermentType fermentType = FermentType.PRIMARY;

	/*-------------------------------------------------------------------------*/
	public Ferment()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Ferment(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		TemperatureUnit startTemp,
		TemperatureUnit endTemp,
		TimeUnit duration,
		List<IngredientAddition> ingredientAdditions,
		boolean removeTrubAndChillerLoss)
	{
		this(name, description, inputVolume, outputVolume, startTemp, endTemp, duration,
			ingredientAdditions, removeTrubAndChillerLoss, FermentType.PRIMARY);
	}

	/*-------------------------------------------------------------------------*/
	public Ferment(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		TemperatureUnit startTemp,
		TemperatureUnit endTemp,
		TimeUnit duration,
		List<IngredientAddition> ingredientAdditions,
		boolean removeTrubAndChillerLoss,
		FermentType fermentType)
	{
		super(name, description, Type.FERMENT, inputVolume, outputVolume);
		this.startTemp = startTemp;
		this.endTemp = endTemp;
		this.duration = duration;
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
		this.fermentType = fermentType == null ? FermentType.PRIMARY : fermentType;
		super.setIngredients(ingredientAdditions);
		this.setOutputVolume(outputVolume);
	}

	/*-------------------------------------------------------------------------*/
	public Ferment(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.FERMENT), StringUtils.getProcessString("ferment.desc"), Type.FERMENT, null, null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.WORT, recipe));
		setOutputVolume(StringUtils.getProcessString("ferment.output", getName()));
		setStartTemp(new TemperatureUnit(20D));
		setEndTemp(new TemperatureUnit(20D));
		setDuration(new TimeUnit(14, DAYS, false));
		this.removeTrubAndChillerLoss = false;
	}

	/*-------------------------------------------------------------------------*/
	public Ferment(Ferment other)
	{
		super(other.getName(), other.getDescription(), Type.FERMENT, other.getInputVolume(), other.getOutputVolume());

		this.startTemp = other.startTemp == null ? null : new TemperatureUnit(other.startTemp);
		this.endTemp = other.endTemp == null ? null : new TemperatureUnit(other.endTemp);
		this.duration = other.duration;
		this.removeTrubAndChillerLoss = other.removeTrubAndChillerLoss;
		this.fermentType = other.fermentType;
	}

	/*-------------------------------------------------------------------------*/
	public static boolean shouldApplyWortToBeerChemistry(
		Volume.Type inputType,
		FermentType fermentType)
	{
		if (inputType != Volume.Type.WORT || fermentType == null)
		{
			return false;
		}
		return fermentType == FermentType.PRIMARY || fermentType == FermentType.SOURING;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		if (getInputVolume() == null)
		{
			if (fermentType != FermentType.STARTER)
			{
				log.addError(StringUtils.getProcessString("ferment.requires.input.volume"));
				return false;
			}
			if (getWaterAdditions().isEmpty())
			{
				log.addError(StringUtils.getProcessString("ferment.starter.no.water.additions"));
				return false;
			}
			return true;
		}
		return super.validateInputVolumes(volumes, log);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		//
		// Require a named input volume, or a STARTER liquor bootstrap from water additions.
		//
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		if (equipmentProfile == null)
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile", equipmentProfile));
			return;
		}

		Volume inputVolume;
		if (getInputVolume() == null)
		{
			inputVolume = createStarterBootstrapVolume();
			for (WaterAddition ia : getWaterAdditions())
			{
				inputVolume = Equations.dilute(inputVolume, ia, inputVolume.getName());
			}
		}
		else
		{
			inputVolume = getInputVolume(volumes).clone();

			//
			// Optionally remove kettle trub and chiller loss before the fermenter receives the wort.
			//
			if (!KettleTrubChillerLossSubtract.subtractIfEnabled(
				inputVolume, equipmentProfile, removeTrubAndChillerLoss, log))
			{
				return;
			}
		}

		//
		// Water on a named input dilutes/top-ups that wort. Liquor-bootstrap STARTER already
		// merged step water in the block above (same as Stand null-input behaviour).
		//
		if (getInputVolume() != null)
		{
			for (WaterAddition ia : getWaterAdditions())
			{
				inputVolume = Equations.dilute(inputVolume, ia, inputVolume.getName());
			}
		}

		inputVolume = applyFermentableAdditions(inputVolume);

		//
		// Warn when the volume headed for the fermenter exceeds fermenter capacity.
		//
		if (inputVolume.getVolume().get(Quantity.Unit.MILLILITRES)*1.2 >
			equipmentProfile.getFermenterVolume().get(MILLILITRES))
		{
			log.addWarning(
				StringUtils.getProcessString("ferment.fermenter.not.large.enough",
					equipmentProfile.getFermenterVolume().get(LITRES),
					inputVolume.getVolume().get(Quantity.Unit.LITRES)));
		}

		List<YeastAddition> stepPitches = getYeastAdditions();
		boolean hasYeast = !inputVolume.getYeastCultures().isEmpty()
			|| !stepPitches.isEmpty()
			|| volumeHasYeastAddition(inputVolume);

		if (!hasYeast && inputVolume.getType() == Volume.Type.WORT)
		{
			log.addError(StringUtils.getProcessString("ferment.no.yeast.addition"));
			estimatedFinalGravity = inputVolume.getGravity();
			return;
		}

		//
		// Run the fermentation model: yeast consumes extract over the step duration and temperature
		// profile, producing final gravity estimate, evolved cultures, and average ferment temp.
		//
		FermentationResult fermentation = FermentationCalculator.fermentPhase(
			inputVolume,
			stepPitches,
			startTemp,
			endTemp,
			duration,
			log);

		List<YeastCulture> evolvedCultures = fermentation.getEvolvedCultures();
		if (fermentType == FermentType.STARTER && fermentation.hasFermentation())
		{
			evolvedCultures = FermentationCalculator.applyStarterCellGrowthHeuristic(
				evolvedCultures,
				fermentation.getEffectiveAttenuation(),
				log);
		}

		boolean applyWortToBeerChemistry =
			shouldApplyWortToBeerChemistry(inputVolume.getType(), fermentType);

		Volume volOut;
		if (fermentation.hasFermentation())
		{
			//
			// A fraction of iso-alpha acids is lost during fermentation on primary/souring wort;
			// starter ferments skip packaging chemistry on small starter volumes.
			//
			if (applyWortToBeerChemistry)
			{
				HopAcidVolumes.applyIsoRetention(
					inputVolume,
					Const.ISO_ALPHA_RETENTION_DURING_FERMENTATION);
			}

			TemperatureUnit avgTemp = fermentation.getAverageTemp();
			if (avgTemp == null)
			{
				avgTemp = FermentationCalculator.calcAverageTempC(startTemp, endTemp);
			}

			ColourUnit colourOut = inputVolume.getColour() == null
				? null
				: applyWortToBeerChemistry
					? Equations.calcColourAfterFermentation(inputVolume.getColour())
					: new ColourUnit(inputVolume.getColour());

			CarbonationUnit carbonationOut = Equations.calcEquilibriumCo2(
				avgTemp,
				Const.ONE_ATMOSPHERE_IN_KPA);

			DensityUnit originalGravity = inputVolume.getOriginalGravity() != null
				? inputVolume.getOriginalGravity()
				: inputVolume.getGravity();

			if (inputVolume.getType() == Volume.Type.WORT)
			{
				volOut = new Volume(getOutputVolume(), Volume.Type.BEER);
				volOut.setVolume(inputVolume.getVolume());
				volOut.setTemperature(inputVolume.getTemperature());
				volOut.setOriginalGravity(originalGravity);
				volOut.setColour(colourOut);
				BitternessVolumes.copyAll(inputVolume, volOut);
				HopAcidVolumes.copyAll(inputVolume, volOut);
				volOut.setCarbonation(carbonationOut);
			}
			else
			{
				volOut = inputVolume.clone();
				volOut.setName(getOutputVolume());
				if (originalGravity != null)
				{
					volOut.setOriginalGravity(originalGravity);
				}
				volOut.setColour(colourOut);
				volOut.setCarbonation(carbonationOut);
			}

			volOut.setIngredientAdditions(
				buildOutputIngredients(inputVolume, evolvedCultures));
		}
		else
		{
			volOut = inputVolume.clone();
			volOut.setName(getOutputVolume());
		}

		//
		// Dry-hop or late hop additions on the ferment step add alpha acids without boil isomerisation.
		//
		for (HopAddition hop : getHopAdditions())
		{
			HopAcidVolumes.addHopAlpha(volOut, hop);
		}

		BitternessVolumes.syncReportedDerived(
			volOut,
			Settings.parseReportedFormulas(Database.getInstance().getSettings()));

		volumes.addOrUpdateVolume(getOutputVolume(), volOut);

		//
		// Final gravity and ABV: use measured FG when present, otherwise the fermentation estimate;
		// ABV is derived from OG to FG (plus any pre-ferment ABV on the input).
		//
		Volume beerVolume = volumes.getVolume(getOutputVolume());
		DensityUnit measuredFg = (DensityUnit)beerVolume.getMetric(Volume.Metric.GRAVITY);
		boolean estimatedFg = measuredFg == null || measuredFg.isEstimated();
		DensityUnit fg;
		if (estimatedFg && fermentation.hasFermentation() && fermentation.getEstimatedFg() != null)
		{
			estimatedFinalGravity = fermentation.getEstimatedFg();
			fg = estimatedFinalGravity;
		}
		else if (estimatedFg)
		{
			estimatedFinalGravity = inputVolume.getGravity();
			fg = estimatedFinalGravity;
		}
		else
		{
			fg = measuredFg;
		}

		PercentageUnit abvAdded;
		if (fermentation.hasFermentation())
		{
			abvAdded = Equations.calcAbvWithGravityChange(inputVolume.getGravity(), fg);
		}
		else
		{
			abvAdded = new PercentageUnit(0D, false);
		}
		beerVolume.setGravity(fg);

		double abvIn = inputVolume.getAbv() == null ? 0 : inputVolume.getAbv().get();
		beerVolume.setAbv(new PercentageUnit(abvIn + abvAdded.get(), abvAdded.isEstimated()));
	}

	/*-------------------------------------------------------------------------*/
	private static Volume createStarterBootstrapVolume()
	{
		return new Volume(
			"starter liquor",
			Volume.Type.WORT,
			new VolumeUnit(0),
			new TemperatureUnit(20, CELSIUS),
			new DensityUnit(1.000, SPECIFIC_GRAVITY),
			new DensityUnit(1.000, SPECIFIC_GRAVITY),
			new PercentageUnit(0),
			new ColourUnit(0, SRM),
			new BitternessUnit(0, IBU));
	}

	/*-------------------------------------------------------------------------*/
	private static boolean volumeHasYeastAddition(Volume volume)
	{
		for (IngredientAddition ia : volume.getIngredientAdditions())
		{
			if (ia instanceof YeastAddition)
			{
				return true;
			}
		}
		return false;
	}

	/*-------------------------------------------------------------------------*/
	private Volume applyFermentableAdditions(Volume inputVolume)
	{
		List<Settings.HopBitternessFormula> reportedFormulas =
			Settings.parseReportedFormulas(Database.getInstance().getSettings());

		DensityUnit gravityIn = inputVolume.getGravity();
		ColourUnit colourIn = inputVolume.getColour();
		Map<Settings.HopBitternessFormula, BitternessUnit> bitternessByFormula = new LinkedHashMap<>();
		for (Settings.HopBitternessFormula formula : reportedFormulas)
		{
			bitternessByFormula.put(formula, BitternessVolumes.copyOrZero(inputVolume, formula));
		}

		List<FermentableAddition> steepedGrains = new ArrayList<>();
		for (FermentableAddition fa : getFermentableAdditions())
		{
			DensityUnit gravity = Equations.calcSteepedFermentableAdditionGravity(fa, inputVolume.getVolume());
			gravityIn = new DensityUnit(gravityIn.get() + gravity.get());

			if (fa.getFermentable().getType() == Fermentable.Type.GRAIN
				|| fa.getFermentable().getType() == Fermentable.Type.ADJUNCT)
			{
				steepedGrains.add(fa);
			}
			else
			{
				ColourUnit col = Equations.calcSolubleFermentableAdditionColourContribution(
					fa, inputVolume.getVolume());
				colourIn = new ColourUnit(colourIn.get() + col.get());
			}

			BitternessUnit ibu = Equations.calcSolubleFermentableAdditionBitternessContribution(
				fa, inputVolume.getVolume());
			for (Settings.HopBitternessFormula formula : reportedFormulas)
			{
				bitternessByFormula.get(formula).add(ibu);
			}
		}
		if (!steepedGrains.isEmpty())
		{
			ColourUnit col = Equations.calcColourSrmMoreyFormula(steepedGrains, inputVolume.getVolume());
			colourIn = new ColourUnit(colourIn.get() + col.get());
		}

		inputVolume.setGravity(gravityIn);
		inputVolume.setColour(colourIn);
		for (Settings.HopBitternessFormula formula : reportedFormulas)
		{
			BitternessVolumes.set(inputVolume, formula, bitternessByFormula.get(formula));
		}
		BitternessVolumes.syncReportedDerived(inputVolume, reportedFormulas);
		return inputVolume;
	}

	/*-------------------------------------------------------------------------*/
	private List<IngredientAddition> buildOutputIngredients(
		Volume inputVolume,
		List<YeastCulture> evolvedCultures)
	{
		List<IngredientAddition> out = new ArrayList<>();

		for (IngredientAddition ia : inputVolume.getIngredientAdditions())
		{
			if (!(ia instanceof YeastCulture) && !(ia instanceof YeastAddition))
			{
				out.add(ia.clone());
			}
		}

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia instanceof YeastAddition || ia instanceof YeastCulture)
			{
				continue;
			}
			if (ia.getType() == IngredientAddition.Type.WATER)
			{
				continue;
			}
			out.add(ia.clone());
		}

		for (YeastCulture culture : evolvedCultures)
		{
			if (fermentType == FermentType.STARTER)
			{
				culture.setSourceType(YeastSourceType.STARTER);
			}
			out.add(culture);
		}
		return out;
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		if (!validateInputVolumes(recipe.getVolumes(), log))
		{
			return;
		}

		recipe.getVolumes().addVolume(getOutputVolume(), new Volume(Volume.Type.BEER));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		if (isConstantTemperature())
		{
			return StringUtils.getProcessString(
				"ferment.step.desc.constant",
				endTemp.describe(CELSIUS));
		}
		return StringUtils.getProcessString(
			"ferment.step.desc.ramp",
			startTemp.describe(CELSIUS),
			endTemp.describe(CELSIUS));
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(IngredientAddition.Type.values());
	}

	/*-------------------------------------------------------------------------*/
	public TemperatureUnit getStartTemp()
	{
		return startTemp;
	}

	public void setStartTemp(TemperatureUnit startTemp)
	{
		this.startTemp = startTemp;
	}

	public TemperatureUnit getEndTemp()
	{
		return endTemp;
	}

	public void setEndTemp(TemperatureUnit endTemp)
	{
		this.endTemp = endTemp;
	}

	/**
	 * Representative fermentation temperature for legacy calculations (e.g. equilibrium CO₂).
	 * Returns {@link #getEndTemp()} until fermentation ramp modelling is implemented.
	 */
	@Deprecated
	public TemperatureUnit getTemperature()
	{
		return endTemp;
	}

	/**
	 * Sets both start and end temperature to the same value.
	 */
	@Deprecated
	public void setTemperature(TemperatureUnit temp)
	{
		this.startTemp = temp;
		this.endTemp = temp;
	}

	public boolean isConstantTemperature()
	{
		if (startTemp == null || endTemp == null)
		{
			return true;
		}
		return Math.abs(startTemp.get(CELSIUS) - endTemp.get(CELSIUS)) < 0.05;
	}

	public TimeUnit getDuration()
	{
		return duration;
	}

	public void setDuration(TimeUnit duration)
	{
		this.duration = duration;
	}

	public DensityUnit getEstimatedFinalGravity()
	{
		return estimatedFinalGravity;
	}

	public boolean isRemoveTrubAndChillerLoss()
	{
		return removeTrubAndChillerLoss;
	}

	public void setRemoveTrubAndChillerLoss(boolean removeTrubAndChillerLoss)
	{
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
	}

	public FermentType getFermentType()
	{
		return fermentType;
	}

	public void setFermentType(FermentType fermentType)
	{
		this.fermentType = fermentType == null ? FermentType.PRIMARY : fermentType;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	protected void sortIngredients()
	{
		// sort ascending by time
		getIngredientAdditions().sort((o1, o2) -> (int)(o2.getTime().get() - o1.getTime().get()));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		result.add(StringUtils.getDocString(
			"ferment.duration",
			this.getInputVolume(),
			this.getDuration().describe(DAYS)));

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.HOPS || ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.hop.addition",
						ia.describe(),
						ia.getTime().describe(DAYS)));
			}
			else if (ia.getType() == IngredientAddition.Type.YEAST)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.yeast.addition",
						ia.describe()));
			}
			else if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.fermentable.addition",
						ia.describe(),
						ia.getTime().describe(DAYS)));
			}
			else if (ia.getType() == IngredientAddition.Type.WATER)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.water.addition",
						ia.describe(),
						ia.getTime().describe(DAYS)));
			}
			else if (ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"ferment.misc.addition",
						ia.describe(),
						ia.getTime().describe(DAYS)));
			}
			else
			{
				result.add(StringUtils.getDocString("additions.generic", ia.describe()));
			}
		}

		return result;
	}

	@Override
	public ProcessStep clone(String newName)
	{
		return new Ferment(
			newName,
			this.getDescription(),
			this.getInputVolume(),
			StringUtils.getProcessString("ferment.output", newName),
			new TemperatureUnit(getStartTemp()),
			new TemperatureUnit(getEndTemp()),
			new TimeUnit(getDuration().get()),
			cloneIngredients(getIngredientAdditions()),
			this.removeTrubAndChillerLoss,
			this.fermentType);
	}
}
