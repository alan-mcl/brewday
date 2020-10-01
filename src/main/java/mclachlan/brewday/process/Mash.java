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

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.*;

import static mclachlan.brewday.math.Quantity.Unit.*;

public class Mash extends ProcessStep
{
	private String outputMashVolume;

	/** duration of mash */
	private TimeUnit duration;

	/** grain volume temp */
	private TemperatureUnit grainTemp;

	// calculated
	private TemperatureUnit mashTemp;
	private PhUnit mashPh;

	/*-------------------------------------------------------------------------*/
	public Mash()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Mash(
		String name,
		String description,
		List<IngredientAddition> mashAdditions,
		String outputMashVolume,
		TimeUnit duration,
		TemperatureUnit grainTemp)
	{
		super(name, description, Type.MASH);
		setIngredients(mashAdditions);

		this.outputMashVolume = outputMashVolume;
		this.duration = duration;
		this.grainTemp = grainTemp;
	}

	/*-------------------------------------------------------------------------*/
	public Mash(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.MASH), StringUtils.getProcessString("mash.desc"), Type.MASH);

		duration = new TimeUnit(60, MINUTES, false);
		grainTemp = new TemperatureUnit(20);

		outputMashVolume = StringUtils.getProcessString("mash.mash.vol", getName());
	}

	/*-------------------------------------------------------------------------*/
	public Mash(Mash step)
	{
		super(step.getName(), step.getDescription(), Type.MASH);

		this.duration = step.getDuration();
		this.grainTemp = step.getGrainTemp();

		this.outputMashVolume = step.getOutputMashVolume();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		if (validateEquipmentProfile(equipmentProfile, log))
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile", equipmentProfile));
			return;
		}

		List<IngredientAddition> grainBill = new ArrayList<>();
		List<HopAddition> hopCharges = new ArrayList<>();
		WaterAddition strikeWater;

		strikeWater = organiseIngredients(grainBill, hopCharges);

		if (grainBill == null || grainBill.isEmpty())
		{
			log.addError(StringUtils.getProcessString("mash.no.fermentable.addition"));
			return;
		}
		if (strikeWater == null)
		{
			log.addError(StringUtils.getProcessString("mash.no.strike.water"));
			return;
		}

		Volume mashVolumeOut = getMashVolumeOut(equipmentProfile, grainBill, strikeWater);
		volumes.addOrUpdateVolume(outputMashVolume, mashVolumeOut);

		if (mashVolumeOut.getVolume().get() *1.1 > equipmentProfile.getMashTunVolume().get())
		{
			log.addWarning(
					StringUtils.getProcessString("mash.mash.tun.not.large.enough",
					equipmentProfile.getMashTunVolume().get(LITRES),
					mashVolumeOut.getVolume().get(LITRES)));
		}

		if (hopCharges != null && !hopCharges.isEmpty())
		{
			// mash hops
			BitternessUnit bitterness = Equations.calcTotalIbuTinseth(
				hopCharges,
				mashVolumeOut.getGravity(),
				mashVolumeOut.getVolume(),
				Double.valueOf(Database.getInstance().getSettings().get(Settings.MASH_HOP_UTILISATION)));

			mashVolumeOut.setBitterness(new BitternessUnit(bitterness));
		}
		else
		{
			mashVolumeOut.setBitterness(new BitternessUnit(0));
		}
	}

	/*-------------------------------------------------------------------------*/
	private WaterAddition organiseIngredients(
		List<IngredientAddition> grainBill,
		List<HopAddition> hopCharges)
	{
		WaterAddition strikeWater = null;

		for (IngredientAddition item : getIngredients())
		{
			// seek the grains and water with the same time as the mash,
			// these are the initial combination

			if ((int)item.getTime().get(MINUTES) == (int)this.getDuration().get(MINUTES))
			{
				if (item instanceof FermentableAddition)
				{
					grainBill.add(item);
				}
				else if (item instanceof WaterAddition)
				{
					strikeWater = (WaterAddition)item;
				}
			}

			// hop addition timings are added up in the Equations method
			if (item instanceof HopAddition)
			{
				hopCharges.add((HopAddition)item);
			}
		}

		return strikeWater;
	}

	/*-------------------------------------------------------------------------*/
	protected boolean validateEquipmentProfile(
		EquipmentProfile equipmentProfile,
		ProcessLog log)
	{
		if (equipmentProfile == null)
		{
			return true;
		}
		return false;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void dryRun(Recipe recipe, ProcessLog log)
	{
		EquipmentProfile equipmentProfile = Database.getInstance().
			getEquipmentProfiles().get(recipe.getEquipmentProfile());

		if (validateEquipmentProfile(equipmentProfile, log))
		{
			log.addError(StringUtils.getProcessString("equipment.invalid.profile",
				recipe.getEquipmentProfile()));
			return;
		}

		recipe.getVolumes().addVolume(outputMashVolume, new Volume(Volume.Type.MASH));
	}

	/*-------------------------------------------------------------------------*/
	private Volume getMashVolumeOut(
		EquipmentProfile equipmentProfile,
		List<IngredientAddition> grainBill,
		WaterAddition strikeWater)
	{
		WeightUnit grainWeight = Equations.calcTotalGrainWeight(grainBill);

		mashTemp = Equations.calcMashTemp(grainWeight, strikeWater, grainTemp);

		double conversionEfficiency = equipmentProfile.getConversionEfficiency().get();

		DensityUnit gravityOut = Equations.calcMashExtractContentFromYield(grainBill, conversionEfficiency, strikeWater);

		VolumeUnit volumeOut = Equations.calcMashVolume(grainBill, strikeWater.getVolume(), conversionEfficiency);

		ColourUnit colourOut = Equations.calcColourSrmMoreyFormula(grainBill, volumeOut);

		Settings.MashPhModel phModel = Settings.MashPhModel.valueOf(
			Database.getInstance().getSettings().get(Settings.MASH_PH_MODEL));

		switch (phModel)
		{
			case EZ_WATER:
				mashPh = Equations.calcMashPhEzWater(strikeWater, grainBill);
				break;
			default:
				throw new BrewdayException("invalid "+phModel);
		}

		return new Volume(
			null,
			Volume.Type.MASH,
			volumeOut,
			grainBill,
			strikeWater,
			mashTemp,
			gravityOut,
			colourOut,
			mashPh);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("mash.step.desc", getName());
	}

	public String getOutputMashVolume()
	{
		return outputMashVolume;
	}

	public void setOutputMashVolume(String outputMashVolume)
	{
		this.outputMashVolume = outputMashVolume;
	}

	public void setMashTemp(TemperatureUnit mashTemp)
	{
		this.mashTemp = mashTemp;
	}

	public TimeUnit getDuration()
	{
		return duration;
	}

	public TemperatureUnit getGrainTemp()
	{
		return grainTemp;
	}

	public void setGrainTemp(TemperatureUnit grainTemp)
	{
		this.grainTemp = grainTemp;
	}

	/*-------------------------------------------------------------------------*/
	public TemperatureUnit getMashTemp()
	{
		return mashTemp;
	}

	public PhUnit getMashPh()
	{
		return mashPh;
	}

	/*-------------------------------------------------------------------------*/

	public void setDuration(TimeUnit duration)
	{
		this.duration = duration;
	}

	@Override
	public Collection<String> getInputVolumes()
	{
		return Collections.emptyList();
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		ArrayList<String> result = new ArrayList<>();

		if (outputMashVolume != null)
		{
			result.add(outputMashVolume);
		}

		return result;
	}

	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Arrays.asList(
			IngredientAddition.Type.FERMENTABLES,
			IngredientAddition.Type.HOPS,
			IngredientAddition.Type.MISC,
			IngredientAddition.Type.WATER);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		for (IngredientAddition ia : getIngredientAdditions(IngredientAddition.Type.WATER))
		{
			WaterAddition wa = (WaterAddition)ia;

			result.add(
				StringUtils.getDocString(
					"mash.water.addition",
					wa.getQuantity().get(LITRES),
					wa.getName(),
					wa.getTemperature().get(Quantity.Unit.CELSIUS)));
		}

		for (IngredientAddition ia : getIngredientAdditions(IngredientAddition.Type.FERMENTABLES))
		{
			result.add(
				StringUtils.getDocString(
					"mash.fermentable.addition",
					ia.getQuantity().get(Quantity.Unit.KILOGRAMS),
					ia.getName(),
					this.grainTemp.get(Quantity.Unit.CELSIUS)));
		}

		for (IngredientAddition ia : getIngredientAdditions(IngredientAddition.Type.HOPS))
		{
			result.add(
				StringUtils.getDocString(
					"mash.hop.addition",
					ia.getQuantity().get(GRAMS),
					ia.getName(),
					ia.getTime().get(MINUTES)));
		}

		for (IngredientAddition ia : getIngredientAdditions(IngredientAddition.Type.MISC))
		{
			result.add(
				StringUtils.getDocString(
					"mash.misc.addition",
					ia.getQuantity().get(GRAMS),
					ia.getName(),
					ia.getTime().get(MINUTES)));
		}

		String outputMashVolume = this.getOutputMashVolume();
		Volume mashVol = getRecipe().getVolumes().getVolume(outputMashVolume);

		result.add(StringUtils.getDocString(
			"mash.volume",
			mashVol.getMetric(Volume.Metric.VOLUME).get(LITRES),
			mashVol.getMetric(Volume.Metric.TEMPERATURE).get(CELSIUS)));

		result.add(StringUtils.getDocString("mash.rest", this.duration.get(MINUTES)));

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public ProcessStep clone()
	{
		return new Mash(
			this.getName(),
			this.getDescription(),
			cloneIngredients(getIngredients()),
			this.getOutputMashVolume(),
			new TimeUnit(this.duration.get()),
			new TemperatureUnit(this.grainTemp.get()));
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Adjust the strike water volume to achieve the target mash volume.
	 */
	public void adjustWaterAdditionToMashVolume(
		VolumeUnit targetMashVol,
		double conversionEfficiency)
	{
		ArrayList<IngredientAddition> grainBill = new ArrayList<>();
		ArrayList<HopAddition> hopCharges = new ArrayList<>();
		WaterAddition strikeWater = organiseIngredients(grainBill, hopCharges);

		VolumeUnit volumeUnit = Equations.calcWaterVolumeToAchieveMashVolume(
			grainBill, conversionEfficiency, targetMashVol);

		strikeWater.setVolume(volumeUnit);
	}
}

