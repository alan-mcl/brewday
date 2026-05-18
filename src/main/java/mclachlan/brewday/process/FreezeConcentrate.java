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
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.util.StringUtils;

/**
 * Freeze concentration / freeze distillation of finished beer (e.g. Eisbock).
 */
public class FreezeConcentrate extends FluidVolumeProcessStep
{
	private TimeUnit duration;
	private TemperatureUnit freezerTemperature;
	private Double waterRemovalPercentOverride;
	private double processEfficiency;
	private double ethanolRetentionFactor;
	private double extractRetentionFactor;
	private double ibuRetentionFactor;
	private double co2RetentionFactor;
	private double vesselGeometryFactor;

	/*-------------------------------------------------------------------------*/
	public FreezeConcentrate()
	{
	}

	/*-------------------------------------------------------------------------*/
	public FreezeConcentrate(
		String name,
		String description,
		String inputVolume,
		String outputVolume,
		TimeUnit duration,
		TemperatureUnit freezerTemperature,
		Double waterRemovalPercentOverride,
		double processEfficiency,
		double ethanolRetentionFactor,
		double extractRetentionFactor,
		double ibuRetentionFactor,
		double co2RetentionFactor,
		double vesselGeometryFactor)
	{
		super(name, description, Type.FREEZE_CONCENTRATE, inputVolume, outputVolume);
		this.duration = duration;
		this.freezerTemperature = freezerTemperature;
		this.waterRemovalPercentOverride = waterRemovalPercentOverride;
		setProcessEfficiency(processEfficiency);
		setEthanolRetentionFactor(ethanolRetentionFactor);
		setExtractRetentionFactor(extractRetentionFactor);
		setIbuRetentionFactor(ibuRetentionFactor);
		setCo2RetentionFactor(co2RetentionFactor);
		this.vesselGeometryFactor = vesselGeometryFactor;
	}

	/*-------------------------------------------------------------------------*/
	public FreezeConcentrate(Recipe recipe)
	{
		super(
			recipe.getUniqueStepName(Type.FREEZE_CONCENTRATE),
			StringUtils.getProcessString("freeze.concentrate.desc"),
			Type.FREEZE_CONCENTRATE,
			null,
			null);

		setInputVolume(recipe.getVolumes().getVolumeByType(Volume.Type.BEER, recipe));
		setOutputVolume(StringUtils.getProcessString("freeze.concentrate.output", getName()));
		duration = new TimeUnit(2, Quantity.Unit.HOURS, false);
		freezerTemperature = new TemperatureUnit(-20, Quantity.Unit.CELSIUS);
		waterRemovalPercentOverride = null;
		processEfficiency = 0.6;
		ethanolRetentionFactor = 0.97;
		extractRetentionFactor = 0.995;
		ibuRetentionFactor = 0.98;
		co2RetentionFactor = 0.2;
		vesselGeometryFactor = 1.0;
	}

	/*-------------------------------------------------------------------------*/
	public FreezeConcentrate(FreezeConcentrate other)
	{
		super(
			other.getName(),
			other.getDescription(),
			Type.FREEZE_CONCENTRATE,
			other.getInputVolume(),
			other.getOutputVolume());

		this.duration = other.duration;
		this.freezerTemperature = other.freezerTemperature;
		this.waterRemovalPercentOverride = other.waterRemovalPercentOverride;
		this.processEfficiency = other.processEfficiency;
		this.ethanolRetentionFactor = other.ethanolRetentionFactor;
		this.extractRetentionFactor = other.extractRetentionFactor;
		this.ibuRetentionFactor = other.ibuRetentionFactor;
		this.co2RetentionFactor = other.co2RetentionFactor;
		this.vesselGeometryFactor = other.vesselGeometryFactor;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		double tempC = freezerTemperature.get(Quantity.Unit.CELSIUS);

		if (tempC > 0)
		{
			log.addWarning(StringUtils.getProcessString(
				"freeze.concentrate.warn.temp.above.zero",
				tempC));
		}

		if (tempC > -1)
		{
			log.addWarning(StringUtils.getProcessString(
				"freeze.concentrate.warn.temp.too.warm",
				tempC));
		}

		Volume input = getInputVolume(volumes);

		VolumeUnit volumeIn = input.getVolume();
		double volumeInMl = volumeIn.get(Quantity.Unit.MILLILITRES);

		PercentageUnit inputAbv = input.getAbv();
		double initialAbv = inputAbv == null ? 5.0 : inputAbv.get();

		if (inputAbv == null)
		{
			log.addWarning(StringUtils.getProcessString(
				"freeze.concentrate.warn.no.abv"));
		}

		double removalFraction;

		if (waterRemovalPercentOverride != null)
		{
			removalFraction =
				clamp01(waterRemovalPercentOverride / 100.0);

			log.addMessage(StringUtils.getProcessString(
				"freeze.concentrate.override.removal",
				removalFraction * 100.0));
		}
		else
		{
			double hours = duration.get(Quantity.Unit.HOURS);

			// crude equilibrium estimate based on freezer temp
			double equilibriumAbv =
				Math.abs(tempC) * 1.8;

			equilibriumAbv =
				Math.max(equilibriumAbv, initialAbv + 1.0);

			// maximum practical removable fraction
			double maxRemoval =
				1.0 - (initialAbv / equilibriumAbv);

			maxRemoval *= processEfficiency;

			maxRemoval = clamp01(maxRemoval);
			maxRemoval = Math.min(maxRemoval, 0.80);

			// freezing rate constant
			double k =
				0.04 *
					(Math.abs(tempC) / 18.0) *
					Math.max(0.1, vesselGeometryFactor);

			removalFraction =
				maxRemoval *
					(1.0 - Math.exp(-k * hours));

			removalFraction = clamp01(removalFraction);

			log.addMessage(StringUtils.getProcessString(
				"freeze.concentrate.derived.removal",
				removalFraction * 100.0,
				hours,
				tempC));
		}

		if (removalFraction > 0.5)
		{
			log.addWarning(StringUtils.getProcessString(
				"freeze.concentrate.warn.high.removal",
				removalFraction * 100.0));
		}

		double volumeOutMl =
			volumeInMl * (1.0 - removalFraction);

		if (volumeOutMl <= 100)
		{
			log.addError(StringUtils.getProcessString(
				"freeze.concentrate.error.output.volume.too.small",
				volumeOutMl));

			return;
		}

		VolumeUnit volumeOut =
			new VolumeUnit(volumeOutMl, Quantity.Unit.MILLILITRES);

		double concentrationFactor =
			volumeInMl / volumeOutMl;

		//
		// Gravity
		//

		DensityUnit gravityOut = null;

		if (input.getGravity() != null)
		{
			gravityOut = Equations.calcGravityWithVolumeChange(
				volumeIn,
				input.getGravity(),
				volumeOut);

			double gravityPoints =
				(gravityOut.get(Quantity.Unit.SPECIFIC_GRAVITY) - 1.0) * 1000.0;

			gravityPoints *= extractRetentionFactor;

			gravityOut = new DensityUnit(
				1.0 + (gravityPoints / 1000.0),
				Quantity.Unit.SPECIFIC_GRAVITY);
		}

		//
		// Original gravity
		// don't think we should change this, why?
		//

		DensityUnit ogOut = input.getOriginalGravity();
//
//		if (input.getOriginalGravity() != null)
//		{
//			ogOut = Equations.calcGravityWithVolumeChange(
//				volumeIn,
//				input.getOriginalGravity(),
//				volumeOut);
//
//			double ogPoints =
//				(ogOut.get(Quantity.Unit.SPECIFIC_GRAVITY) - 1.0) * 1000.0;
//
//			ogPoints *= extractRetentionFactor;
//
//			ogOut = new DensityUnit(
//				1.0 + (ogPoints / 1000.0),
//				Quantity.Unit.SPECIFIC_GRAVITY);
//		}

		//
		// ABV
		//

		PercentageUnit abvOut = null;

		if (input.getAbv() != null)
		{
			abvOut = Equations.calcAbvWithVolumeChange(
				volumeIn,
				input.getAbv(),
				volumeOut);

			abvOut = new PercentageUnit(
				abvOut.get() * ethanolRetentionFactor,
				abvOut.isEstimated());
//
//			if (abvOut.get() > 20)
//			{
//				log.addWarning(StringUtils.getProcessString(
//					"freeze.concentrate.warn.high.abv",
//					abvOut.get()));
//			}
		}

		//
		// Colour
		//

		ColourUnit colourOut = null;

		if (input.getColour() != null)
		{
			colourOut = Equations.calcColourWithVolumeChange(
				volumeIn,
				input.getColour(),
				volumeOut);
		}

		//
		// Bitterness
		//

		BitternessUnit bitternessOut = null;

		if (input.getBitterness() != null)
		{
			double ibu =
				input.getBitterness().get(Quantity.Unit.IBU);

			ibu *= concentrationFactor;
			ibu *= ibuRetentionFactor;

			bitternessOut =
				new BitternessUnit(ibu, Quantity.Unit.IBU);
		}

		//
		// Carbonation
		//

		CarbonationUnit carbonationOut = null;

		if (input.getCarbonation() != null)
		{
			carbonationOut = new CarbonationUnit(
				input.getCarbonation().get() * co2RetentionFactor,
				input.getCarbonation().getUnit(),
				input.getCarbonation().isEstimated());
		}

		//
		// Output temperature
		//

		TemperatureUnit outputTemp =
			new TemperatureUnit(0, Quantity.Unit.CELSIUS);

		//
		// Create output volume
		//

		Volume volOut = new Volume(
			getOutputVolume(),
			input.getType(),
			volumeOut,
			outputTemp,
			ogOut,
			gravityOut,
			abvOut,
			colourOut,
			bitternessOut);

		volOut.setCarbonation(carbonationOut);
		volOut.setPh(input.getPh());
		volOut.setFermentability(input.getFermentability());

		// preserve ingredient provenance
		volOut.setIngredientAdditions(
			new ArrayList<>(input.getIngredientAdditions()));

		volumes.addOrUpdateVolume(getOutputVolume(), volOut);

		log.addMessage(StringUtils.getProcessString(
			"freeze.concentrate.result",
			volumeIn.get(Quantity.Unit.LITRES),
			volumeOut.get(Quantity.Unit.LITRES),
			removalFraction * 100.0));

		if (input.getAbv() != null && abvOut != null)
		{
			log.addMessage(StringUtils.getProcessString(
				"freeze.concentrate.result.abv",
				input.getAbv().get(Quantity.Unit.PERCENTAGE_DISPLAY),
				abvOut.get(Quantity.Unit.PERCENTAGE_DISPLAY)));
		}

		if (input.getGravity() != null && gravityOut != null)
		{
			log.addMessage(StringUtils.getProcessString(
				"freeze.concentrate.result.gravity",
				input.getGravity().get(Quantity.Unit.SPECIFIC_GRAVITY),
				gravityOut.get(Quantity.Unit.SPECIFIC_GRAVITY)));
		}
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString(
			"freeze.concentrate.step.desc",
			duration.get(Quantity.Unit.HOURS),
			freezerTemperature.get(Quantity.Unit.CELSIUS));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		return List.of(
			StringUtils.getDocString(
				"freeze.concentrate.instructions",
				getInputVolume(),
				freezerTemperature.describe(Quantity.Unit.CELSIUS),
				duration.describe(Quantity.Unit.HOURS),
				getOutputVolume()));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Collections.emptyList();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep clone(String newName)
	{
		return new FreezeConcentrate(
			newName,
			getDescription(),
			getInputVolume(),
			StringUtils.getProcessString("freeze.concentrate.output", newName),
			new TimeUnit(duration),
			new TemperatureUnit(freezerTemperature),
			waterRemovalPercentOverride,
			processEfficiency,
			ethanolRetentionFactor,
			extractRetentionFactor,
			ibuRetentionFactor,
			co2RetentionFactor,
			vesselGeometryFactor);
	}

	/*-------------------------------------------------------------------------*/
	public TimeUnit getDuration()
	{
		return duration;
	}

	/*-------------------------------------------------------------------------*/
	public void setDuration(TimeUnit duration)
	{
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public TemperatureUnit getFreezerTemperature()
	{
		return freezerTemperature;
	}

	/*-------------------------------------------------------------------------*/
	public void setFreezerTemperature(TemperatureUnit freezerTemperature)
	{
		this.freezerTemperature = freezerTemperature;
	}

	/*-------------------------------------------------------------------------*/
	public Double getWaterRemovalPercentOverride()
	{
		return waterRemovalPercentOverride;
	}

	/*-------------------------------------------------------------------------*/
	public void setWaterRemovalPercentOverride(Double waterRemovalPercentOverride)
	{
		this.waterRemovalPercentOverride = waterRemovalPercentOverride;
	}

	/*-------------------------------------------------------------------------*/
	public double getProcessEfficiency()
	{
		return processEfficiency;
	}

	/*-------------------------------------------------------------------------*/
	public void setProcessEfficiency(double processEfficiency)
	{
		this.processEfficiency = clamp01(processEfficiency);
	}

	/*-------------------------------------------------------------------------*/
	public double getEthanolRetentionFactor()
	{
		return ethanolRetentionFactor;
	}

	/*-------------------------------------------------------------------------*/
	public void setEthanolRetentionFactor(double ethanolRetentionFactor)
	{
		this.ethanolRetentionFactor = clamp01(ethanolRetentionFactor);
	}

	/*-------------------------------------------------------------------------*/
	public double getExtractRetentionFactor()
	{
		return extractRetentionFactor;
	}

	/*-------------------------------------------------------------------------*/
	public void setExtractRetentionFactor(double extractRetentionFactor)
	{
		this.extractRetentionFactor = clamp01(extractRetentionFactor);
	}

	/*-------------------------------------------------------------------------*/
	public double getIbuRetentionFactor()
	{
		return ibuRetentionFactor;
	}

	/*-------------------------------------------------------------------------*/
	public void setIbuRetentionFactor(double ibuRetentionFactor)
	{
		this.ibuRetentionFactor = clamp01(ibuRetentionFactor);
	}

	/*-------------------------------------------------------------------------*/
	public double getCo2RetentionFactor()
	{
		return co2RetentionFactor;
	}

	/*-------------------------------------------------------------------------*/
	public void setCo2RetentionFactor(double co2RetentionFactor)
	{
		this.co2RetentionFactor = clamp01(co2RetentionFactor);
	}

	/*-------------------------------------------------------------------------*/
	public double getVesselGeometryFactor()
	{
		return vesselGeometryFactor;
	}

	/*-------------------------------------------------------------------------*/
	public void setVesselGeometryFactor(double vesselGeometryFactor)
	{
		this.vesselGeometryFactor = vesselGeometryFactor;
	}

	/*-------------------------------------------------------------------------*/
	private static double clamp01(double v)
	{
		return Math.max(0.0, Math.min(1.0, v));
	}
}
