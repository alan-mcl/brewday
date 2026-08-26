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
import mclachlan.brewday.Brewday;
import mclachlan.brewday.Settings;
import mclachlan.brewday.Settings.HopBitternessFormula;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.ui.UiQuantityDisplay;

import static mclachlan.brewday.ingredients.Fermentable.Type.*;

/**
 *
 */
public class Boil extends ProcessStep
{
	/** boil duration */
	private TimeUnit duration;

	private String inputWortVolume;
	private String outputWortVolume;
	private String outputTrubVolume;

	// calculated
	private TimeUnit timeToBoil;

	/** should this step remove the equipment profile trub & chiller loss? */
	private boolean removeTrubAndChillerLoss;


	/*-------------------------------------------------------------------------*/
	public Boil()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Boil(
		String name,
		String description,
		String inputWortVolume,
		String outputWortVolume,
		String outputTrubVolume,
		List<IngredientAddition> ingredientAdditions,
		TimeUnit duration,
		boolean removeTrubAndChillerLoss)
	{
		super(name, description, Type.BOIL);
		this.inputWortVolume = inputWortVolume;
		this.outputWortVolume = outputWortVolume;
		this.outputTrubVolume = outputTrubVolume;
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;
		setIngredients(ingredientAdditions);
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public Boil(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.BOIL), StringUtils.getProcessString("boil.desc"), Type.BOIL);

		this.inputWortVolume = recipe.getVolumes().getVolumeByType(Volume.Type.WORT, recipe);
		this.outputWortVolume = StringUtils.getProcessString("boil.output", getName());
		this.outputTrubVolume = StringUtils.getProcessString("boil.output.trub", getName());
		this.duration = new TimeUnit(60, Quantity.Unit.MINUTES, false);
		this.removeTrubAndChillerLoss = false;
	}

	/*-------------------------------------------------------------------------*/
	public Boil(Boil other)
	{
		super(other.getName(), other.getDescription(), Type.BOIL);

		this.inputWortVolume = other.inputWortVolume;
		this.outputWortVolume = other.outputWortVolume;
		this.outputTrubVolume = other.outputTrubVolume;
		this.duration = other.duration;
		this.removeTrubAndChillerLoss = other.removeTrubAndChillerLoss;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		//
		// Input wort volume is optional (extract-first recipes); equipment profile is required for
		// evaporation, hop utilisation, and kettle limits.
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

		//
		// Resolve kettle charge: named pre-boil wort from lauter/sparge, or a zero placeholder when this
		// is the first step and liquor additions will build the boil volume (extract brew).
		//
		Volume inputVolume = null;

		if (inputWortVolume != null)
		{
			inputVolume = volumes.getVolume(inputWortVolume);
		}
		else
		{
			inputVolume = new Volume("water volume",
				Volume.Type.WORT,
				new VolumeUnit(0),
				new TemperatureUnit(20, Quantity.Unit.CELSIUS),
				new DensityUnit(1.000, Quantity.Unit.SPECIFIC_GRAVITY),
				new DensityUnit(1.000, Quantity.Unit.SPECIFIC_GRAVITY),
				new PercentageUnit(0),
				new ColourUnit(0, Quantity.Unit.SRM),
				new BitternessUnit(0, Quantity.Unit.IBU));
		}

		boolean foundWaterAddition = false;
		//
		// Water additions on the boil step (top-up liquor, extract dilution) merge into the kettle charge.
		//
		for (WaterAddition ia : getWaterAdditions())
		{
			foundWaterAddition = true;
			inputVolume = Equations.dilute(inputVolume, ia, inputVolume.getName());
		}

		if (inputWortVolume==null && !foundWaterAddition)
		{
			log.addError(StringUtils.getProcessString("boil.no.water.additions"));
			return;
		}

		//
		// Work on a clone of a named input volume so kettle IBU and hop-acid math do not alter the
		// registry entry still referenced by upstream steps (e.g. lauter first runnings).
		//
		if (inputWortVolume != null)
		{
			inputVolume = inputVolume.clone();
		}

		//
		// Warn when pre-boil volume is close to kettle capacity (boil-over headspace ~20%).
		//
		if (inputVolume.getVolume().get(Quantity.Unit.MILLILITRES) * 1.2D >=
			equipmentProfile.getBoilKettleVolume().get(Quantity.Unit.MILLILITRES))
		{
			log.addWarning(
				StringUtils.getProcessString("boil.kettle.too.small",
					equipmentProfile.getBoilKettleVolume().get(Quantity.Unit.LITRES),
					inputVolume.getVolume().get(Quantity.Unit.LITRES)));
		}

		//
		// Kettle hop schedule for this step. No need to consider earlier step
		// additions as the alpha acids and IBUs are conserved here.
		//
		List<HopAddition> hopCharges = new ArrayList<>(getHopAdditions());

		List<HopBitternessFormula> reportedFormulas =
			Settings.parseReportedFormulas(Database.getInstance().getSettings());

		DensityUnit gravityIn = inputVolume.getGravity();
		ColourUnit colourIn = inputVolume.getColour();
		Map<HopBitternessFormula, BitternessUnit> bitternessByFormula = new LinkedHashMap<>();
		for (HopBitternessFormula formula : reportedFormulas)
		{
			bitternessByFormula.put(formula, BitternessVolumes.copyOrZero(inputVolume, formula));
		}

		//
		// Soluble kettle additions (sugar, extract, honey, juice) raise gravity, colour, and any modeled
		// bitterness before evaporation and hop isomerisation; grain/adjunct types are ignored here.
		//
		for (FermentableAddition fa : getFermentableAdditions())
		{
			if (fa.getFermentable().getType() == JUICE ||
				fa.getFermentable().getType() == SUGAR ||
				fa.getFermentable().getType() == HONEY ||
				fa.getFermentable().getType() == LIQUID_EXTRACT ||
				fa.getFermentable().getType() == DRY_EXTRACT)
			{
				DensityUnit gravity = Equations.calcSteepedFermentableAdditionGravity(fa, inputVolume.getVolume());
				gravityIn = new DensityUnit(gravityIn.get() + gravity.get());

				ColourUnit col = Equations.calcSolubleFermentableAdditionColourContribution(fa, inputVolume.getVolume());
				colourIn = new ColourUnit(colourIn.get() + col.get());

				BitternessUnit ibu = Equations.calcSolubleFermentableAdditionBitternessContribution(fa, inputVolume.getVolume());
				for (HopBitternessFormula formula : reportedFormulas)
				{
					bitternessByFormula.get(formula).add(ibu);
				}
			}
		}

		//
		// Boil transforms wort: kettle reaches 100 °C, evaporation removes water per equipment rate and
		// boil duration (concentrating gravity, ABV, and colour), Maillard darkening applies, then each
		// hop charge adds IBU via the configured formulas. Trub loss is applied later so hop math uses
		// pre-trub volume where needed.
		//
		TemperatureUnit tempOut = new TemperatureUnit(100D, Quantity.Unit.CELSIUS, false);

		double boilEvapourationRatePerHour = equipmentProfile.getBoilEvapourationRate().get();

		double boiledOff = inputVolume.getVolume().get(Quantity.Unit.MILLILITRES) *
			boilEvapourationRatePerHour * (duration.get(Quantity.Unit.MINUTES)/60D);

		log.addVerboseMessage(StringUtils.getProcessString("boil.boil.off.vol", boiledOff/1000D));

		VolumeUnit volumeOut = new VolumeUnit(inputVolume.getVolume().get(Quantity.Unit.MILLILITRES) - boiledOff);

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			inputVolume.getVolume(), gravityIn, volumeOut);

		PercentageUnit abvOut = Equations.calcAbvWithVolumeChange(
			inputVolume.getVolume(), inputVolume.getAbv(), volumeOut);

		ColourUnit colourOut = Equations.calcColourAfterBoil(colourIn);
		colourOut = Equations.calcColourWithVolumeChange(
			inputVolume.getVolume(), colourOut, volumeOut);

		//
		// Sum kettle-hop IBU contributions from each charge at boil gravity and post-evaporation volume.
		//
		boolean reportSmph = reportedFormulas.contains(HopBitternessFormula.SMPH);
		VolumeUnit tinsethVolumeForSmph = new VolumeUnit(volumeOut.get());
		tinsethVolumeForSmph = Equations.calcCoolingShrinkage(
			tinsethVolumeForSmph, new TemperatureUnit(80, Quantity.Unit.CELSIUS));

		PhUnit kettlePh = PhVolumes.getPrimary(inputVolume);

		for (HopAddition hopCharge : hopCharges)
		{
			boolean preIsomerized = hopCharge.getForm() != null
				&& hopCharge.getForm().isPreIsomerized();

			Map<HopBitternessFormula, BitternessUnit> perHopIbu = new LinkedHashMap<>();
			for (HopBitternessFormula formula : reportedFormulas)
			{
				BitternessUnit hopAdditionIbu;
				if (formula == HopBitternessFormula.SMPH)
				{
					double cumulativeAaPpm = SmphEquations.calcCumulativeAaPpmAtAddition(
						hopCharges, hopCharge, tinsethVolumeForSmph);
					double hoppingRateFactor = SmphEquations.calcHoppingRateLossFactor(
						cumulativeAaPpm);

					hopAdditionIbu = Brewday.getInstance().getHopAdditionIBU(
						equipmentProfile,
						inputVolume.getVolume(),
						gravityIn,
						volumeOut,
						gravityOut,
						hopCharge,
						new TimeUnit(0),
						formula,
						hoppingRateFactor,
						kettlePh);
				}
				else
				{
					hopAdditionIbu = Brewday.getInstance().getHopAdditionIBU(
						equipmentProfile,
						inputVolume.getVolume(),
						gravityIn,
						volumeOut,
						gravityOut,
						hopCharge,
						formula);
				}
				bitternessByFormula.get(formula).add(hopAdditionIbu);
				perHopIbu.put(formula, hopAdditionIbu);
			}

			//
			// Pre-isomerized extracts bypass the IBU formulas (every model returns 0); report their
			// iso-alpha mass contribution instead so the log is relevant to the kettle chemistry.
			//
			if (preIsomerized)
			{
				log.addVerboseMessage(StringUtils.getProcessString("log.hop.addition.dryhop",
					describeHopAddition(hopCharge, Quantity.Unit.MINUTES),
					formatDryHopAlpha(Equations.calcHopAlphaAcidsMg(hopCharge), true)));
			}
			else
			{
				log.addVerboseMessage(StringUtils.getProcessString("log.hop.addition.ibu",
					describeHopAddition(hopCharge, Quantity.Unit.MINUTES),
					formatPerFormulaBitterness(reportedFormulas, perHopIbu)));
			}
		}

		if (reportSmph)
		{
			DensityUnit og = inputVolume.getOriginalGravity() != null
				? inputVolume.getOriginalGravity()
				: gravityOut;
			BitternessUnit maltPpIbu = SmphEquations.calcMaltPolyphenolIbu(og, kettlePh, kettlePh);
			bitternessByFormula.get(HopBitternessFormula.SMPH).add(maltPpIbu);
		}

		//
		// Track alpha and iso-alpha masses: new hops add alpha; isomerisation during the boil transfers
		// mass from alpha to iso up to available alpha. Pre-isomerized extracts bypass the kinetic
		// model and contribute directly to iso-alpha.
		//
		WeightUnit hopAcidsAlpha = HopAcidVolumes.copyOrZero(inputVolume, Volume.Metric.ALPHA_ACIDS_MG);
		WeightUnit hopAcidsIso = HopAcidVolumes.copyOrZero(inputVolume, Volume.Metric.ISO_ALPHA_ACIDS_MG);
		for (HopAddition hop : getHopAdditions())
		{
			if (hop.getForm() != null
				&& hop.getForm().isPreIsomerized())
			{
				hopAcidsIso.add(Equations.calcHopAlphaAcidsMg(hop));
			}
			else
			{
				hopAcidsAlpha.add(Equations.calcHopAlphaAcidsMg(hop));
			}
		}
		//
		// Optional, off-by-default empirical kettle-pH correction to hop utilisation. Returns 1.0
		// (no-op) when disabled or when the wort pH is unknown.
		//
		double boilPhUtilisation = Equations.calcBoilPhUtilisationFactor(
			PhVolumes.getPrimary(inputVolume));

		for (HopAddition hopCharge : hopCharges)
		{
			if (hopCharge.getForm() != null
				&& hopCharge.getForm().isPreIsomerized())
			{
				continue;
			}
			WeightUnit isoDelta = Brewday.getInstance().getHopAdditionIsoAlphaMg(
				equipmentProfile,
				inputVolume.getVolume(),
				gravityIn,
				volumeOut,
				gravityOut,
				hopCharge);
			if (boilPhUtilisation != 1.0D)
			{
				isoDelta = new WeightUnit(
					isoDelta.get(Quantity.Unit.MILLIGRAMS) * boilPhUtilisation,
					Quantity.Unit.MILLIGRAMS,
					isoDelta.isEstimated());
			}
			double transfer = Math.min(
				hopAcidsAlpha.get(Quantity.Unit.MILLIGRAMS),
				isoDelta.get(Quantity.Unit.MILLIGRAMS));
			WeightUnit transferUnit = new WeightUnit(
				transfer,
				Quantity.Unit.MILLIGRAMS,
				isoDelta.isEstimated());
			hopAcidsAlpha.subtract(transferUnit);
			hopAcidsIso.add(transferUnit);
		}

		//
		// Subtract kettle trub and chiller dead-volume from packaged wort when the step is configured
		// to model loss at boil end.
		//
		if (removeTrubAndChillerLoss)
		{
			volumeOut = new VolumeUnit(volumeOut.get() - equipmentProfile.getTrubAndChillerLoss().get());
		}

		VolumeUnit hopAbsorptionLoss = Equations.calcTotalHopAbsorptionLoss(getHopAdditions());
		if (hopAbsorptionLoss.get() > 0)
		{
			volumeOut = new VolumeUnit(volumeOut.get() - hopAbsorptionLoss.get());
			log.addVerboseMessage(StringUtils.getProcessString("boil.hop.absorption.loss",
				hopAbsorptionLoss.get(Quantity.Unit.LITRES)));
		}

		//
		// Assemble post-boil wort with concentrated metrics, hop bitterness, and hop-acid inventory.
		//
		Volume postBoilOut = new Volume(
			outputWortVolume,
			inputVolume.getType(),
			volumeOut,
			tempOut,
			inputVolume.getFermentability(),
			gravityOut,
			abvOut,
			colourOut,
			BitternessVolumes.zero());
		for (HopBitternessFormula formula : reportedFormulas)
		{
			BitternessVolumes.set(postBoilOut, formula, bitternessByFormula.get(formula));
		}
		PhVolumes.copyAll(inputVolume, postBoilOut);
		postBoilOut.setFermentability(inputVolume.getFermentability());

		WeightUnit wortAlpha = hopAcidsAlpha;
		WeightUnit wortIso = hopAcidsIso;
		WeightUnit trubAlpha = null;
		WeightUnit trubIso = null;
		//
		// When trub is removed, split hop-acid masses between wort and trub volume proportionally.
		//
		if (removeTrubAndChillerLoss)
		{
			VolumeUnit trubVolume = new VolumeUnit(equipmentProfile.getTrubAndChillerLoss());
			VolumeUnit preTrubVolume = new VolumeUnit(volumeOut.get() + trubVolume.get());
			Volume hopMasses = new Volume(null, inputVolume.getType());
			hopMasses.setAlphaAcidsMg(hopAcidsAlpha);
			hopMasses.setIsoAlphaAcidsMg(hopAcidsIso);
			Volume wortMasses = new Volume(null, inputVolume.getType());
			Volume trubMasses = new Volume(null, inputVolume.getType());
			HopAcidVolumes.applySplit(
				hopMasses,
				preTrubVolume,
				volumeOut,
				wortMasses,
				trubVolume,
				trubMasses);
			wortAlpha = HopAcidVolumes.getOrZero(wortMasses, Volume.Metric.ALPHA_ACIDS_MG);
			wortIso = HopAcidVolumes.getOrZero(wortMasses, Volume.Metric.ISO_ALPHA_ACIDS_MG);
			trubAlpha = HopAcidVolumes.getOrZero(trubMasses, Volume.Metric.ALPHA_ACIDS_MG);
			trubIso = HopAcidVolumes.getOrZero(trubMasses, Volume.Metric.ISO_ALPHA_ACIDS_MG);
		}

		postBoilOut.setAlphaAcidsMg(wortAlpha);
		postBoilOut.setIsoAlphaAcidsMg(wortIso);
		BitternessVolumes.syncReportedDerived(postBoilOut, reportedFormulas);
		volumes.addOrUpdateVolume(outputWortVolume, postBoilOut);

		List<HopAddition> hopsInVolume;

		//
		// Optional trub volume holds loss liquor, residual IBU, hop acids, and ingredient list; otherwise
		// all additions remain on the post-boil wort volume.
		//
		ArrayList<IngredientAddition> ingredientAdditions = new ArrayList<>(inputVolume.getIngredientAdditions());
		ingredientAdditions.addAll(this.getIngredientAdditions());
		if (removeTrubAndChillerLoss)
		{
			Volume trubOut = new Volume(
				outputTrubVolume,
				inputVolume.getType(),
				new VolumeUnit(equipmentProfile.getTrubAndChillerLoss()),
				new TemperatureUnit(tempOut),
				inputVolume.getFermentability() == null ? null : new PercentageUnit(inputVolume.getFermentability()),
				new DensityUnit(gravityOut),
				abvOut == null ? null : new PercentageUnit(abvOut),
				new ColourUnit(colourOut),
				BitternessVolumes.zero());
			for (HopBitternessFormula formula : reportedFormulas)
			{
				BitternessVolumes.set(trubOut, formula, bitternessByFormula.get(formula));
			}
			trubOut.setAlphaAcidsMg(trubAlpha);
			trubOut.setIsoAlphaAcidsMg(trubIso);
			BitternessVolumes.syncReportedDerived(trubOut, reportedFormulas);

			trubOut.setIngredientAdditions(ingredientAdditions);
			hopsInVolume = new ArrayList(trubOut.getIngredientAdditions(IngredientAddition.Type.HOPS));
			volumes.addOrUpdateVolume(outputTrubVolume, trubOut);
		}
		else
		{
			postBoilOut.setIngredientAdditions(ingredientAdditions);
			hopsInVolume = new ArrayList(postBoilOut.getIngredientAdditions(IngredientAddition.Type.HOPS));
		}

		//
		// Accumulate boiled time on hops for downstream stand or ferment steps that reference prior kettle time.
		//
		for (HopAddition ha : hopsInVolume)
		{
			ha.setBoiledTime(new TimeUnit(ha.getBoiledTime().get() + ha.getTime().get()));
		}

		//
		// Record time to reach boil for UI/scheduling from element power and starting temperature.
		//
		timeToBoil = Equations.calcHeatingTime(
			inputVolume.getVolume(),
			inputVolume.getTemperature(),
			new TemperatureUnit(100, Quantity.Unit.CELSIUS),
			equipmentProfile.getBoilElementPower());
	}

	/*-------------------------------------------------------------------------*/
	protected boolean validateInputVolumes(Volumes volumes, ProcessLog log)
	{
		if (inputWortVolume!=null && !volumes.contains(inputWortVolume))
		{
			log.addError(StringUtils.getProcessString("volumes.does.not.exist", inputWortVolume));
			return false;
		}
		return true;
	}

	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		if (!validateInputVolumes(recipe.getVolumes(), log))
		{
			return;
		}

		recipe.getVolumes().addVolume(outputWortVolume, new Volume(Volume.Type.WORT));
		if (removeTrubAndChillerLoss)
		{
			recipe.getVolumes().addVolume(outputTrubVolume, new Volume(Volume.Type.WORT));
		}
	}

	@Override
	protected void sortIngredients()
	{
		// sort ascending by time
		getIngredientAdditions().sort((o1, o2) -> (int)(o2.getTime().get() - o1.getTime().get()));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Map<String, String> describeProperties()
	{
		Map<String, String> result = new LinkedHashMap<>();
		result.put("duration", duration == null ? "null" : duration.get(Quantity.Unit.MINUTES) + "min");
		result.put("inputWortVolume", String.valueOf(inputWortVolume));
		result.put("outputWortVolume", String.valueOf(outputWortVolume));
		result.put("outputTrubVolume", String.valueOf(outputTrubVolume));
		result.put("removeTrubAndChillerLoss", String.valueOf(removeTrubAndChillerLoss));
		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("boil.step.desc", duration.get(Quantity.Unit.MINUTES));
	}

	/*-------------------------------------------------------------------------*/
	public String getInputWortVolume()
	{
		return inputWortVolume;
	}

	public String getOutputWortVolume()
	{
		return outputWortVolume;
	}

	public String getOutputTrubVolume()
	{
		return outputTrubVolume;
	}

	public TimeUnit getDuration()
	{
		return duration;
	}

	public void setDuration(TimeUnit duration)
	{
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public void setInputWortVolume(String inputWortVolume)
	{
		this.inputWortVolume = inputWortVolume;
	}

	/*-------------------------------------------------------------------------*/
	public void setOutputWortVolume(String outputWortVolume)
	{
		this.outputWortVolume = outputWortVolume;
	}

	/*-------------------------------------------------------------------------*/
	public void setOutputTrubVolume(String outputTrubVolume)
	{
		this.outputTrubVolume = outputTrubVolume;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Collection<String> getInputVolumes()
	{
		return inputWortVolume==null?Collections.emptyList():Collections.singletonList(inputWortVolume);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Collection<String> getOutputVolumes()
	{
		List<String> result = new ArrayList<>();

		if (outputWortVolume != null)
		{
			result.add(outputWortVolume);
		}
		if (isRemoveTrubAndChillerLoss() && outputTrubVolume != null)
		{
			result.add(outputTrubVolume);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(IngredientAddition.Type.values());
	}

	/*-------------------------------------------------------------------------*/

	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		Volume preBoilVol = getRecipe().getVolumes().getVolume(this.getInputWortVolume());
		result.add(StringUtils.getDocString(
			"boil.pre.boil",
			preBoilVol.getName(),
			UiQuantityDisplay.describe(preBoilVol.getVolume()),
			UiQuantityDisplay.describe(preBoilVol.getGravity())));

		result.add(StringUtils.getDocString("boil.duration", this.duration.describe(Quantity.Unit.MINUTES)));

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.HOPS || ia.getType() == IngredientAddition.Type.MISC)
			{
				result.add(
					StringUtils.getDocString(
						"boil.hop.addition",
						ia.describe(),
						ia.getTime().describe(Quantity.Unit.MINUTES)));
			}
			else if (ia.getType() == IngredientAddition.Type.FERMENTABLES)
			{
				result.add(
					StringUtils.getDocString(
						"boil.fermentable.addition",
						ia.describe(),
						ia.getTime().describe(Quantity.Unit.MINUTES)));
			}
			else
			{
				result.add(StringUtils.getDocString("additions.generic", ia.describe()));
			}
		}

		Volume postBoilVol = getRecipe().getVolumes().getVolume(this.getOutputWortVolume());
		result.add(StringUtils.getDocString(
			"boil.post.boil",
			UiQuantityDisplay.describe(postBoilVol.getVolume()),
			UiQuantityDisplay.describe(postBoilVol.getGravity())));

		return result;
	}

	@Override
	public ProcessStep clone(String newName)
	{
		return new Boil(
			newName,
			this.getDescription(),
			this.getInputWortVolume(),
			StringUtils.getProcessString("boil.output", newName),
			StringUtils.getProcessString("boil.output.trub", newName),
			cloneIngredients(this.getIngredientAdditions()),
			new TimeUnit(this.getDuration().get()),
			this.removeTrubAndChillerLoss);
	}

	/*-------------------------------------------------------------------------*/

	public TimeUnit getTimeToBoil()
	{
		return timeToBoil;
	}

	/*-------------------------------------------------------------------------*/

	public boolean isRemoveTrubAndChillerLoss()
	{
		return removeTrubAndChillerLoss;
	}

	public void setRemoveTrubAndChillerLoss(boolean removeTrubAndChillerLoss)
	{
		this.removeTrubAndChillerLoss = removeTrubAndChillerLoss;

		if (this.removeTrubAndChillerLoss && outputTrubVolume == null)
		{
			this.outputTrubVolume = StringUtils.getProcessString("boil.output.trub", getName());
		}
	}
}
