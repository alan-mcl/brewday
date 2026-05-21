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
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.util.StringUtils;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 * Separates a Mash volume into a Lautered Mash and a First Runnings volume.
 * Supports first wort hops.
 */
public class Lauter extends ProcessStep
{
	private String inputMashVolume;
	private String outputFirstRunnings;
	private String outputLauteredMashVolume;

	/*-------------------------------------------------------------------------*/
	public Lauter()
	{
	}

	/*-------------------------------------------------------------------------*/
	public Lauter(
		String name,
		String description,
		String inputMashVolume, String outputLauteredMashVolume,
		String outputFirstRunnings)
	{
		super(name, description, Type.LAUTER);
		this.inputMashVolume = inputMashVolume;
		this.outputFirstRunnings = outputFirstRunnings;
		this.outputLauteredMashVolume = outputLauteredMashVolume;
	}

	/*-------------------------------------------------------------------------*/
	public Lauter(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.LAUTER), StringUtils.getProcessString("lauter.desc"), Type.LAUTER);

		inputMashVolume = recipe.getVolumes().getVolumeByType(Volume.Type.MASH, recipe);
		outputLauteredMashVolume = StringUtils.getProcessString("lauter.mash.vol", getName());
		outputFirstRunnings = StringUtils.getProcessString("lauter.first.runnings", getName());
	}

	/*-------------------------------------------------------------------------*/
	public Lauter(Lauter step)
	{
		super(step.getName(), step.getDescription(), Type.LAUTER);

		this.inputMashVolume = step.getInputMashVolume();
		this.outputLauteredMashVolume = step.getOutputLauteredMashVolume();
		this.outputFirstRunnings = step.getOutputFirstRunnings();
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes,  EquipmentProfile equipmentProfile, ProcessLog log)
	{
		//
		// Require the mash volume produced by mash (or infusion) before lautering.
		//
		if (!validateInputVolumes(volumes, log))
		{
			return;
		}

		//
		// Clone the mash and rename it to the spent-grain side of lauter; the sweet wort stream is
		// computed separately as first runnings.
		//
		Volume mashVolumeOut = volumes.getVolume(inputMashVolume).clone();
		mashVolumeOut.setName(outputLauteredMashVolume);

		//
		// First runnings: wort volume from grain and liquor at conversion efficiency, minus equipment
		// lauter dead-loss; gravity and colour carry from the mash, attenuation limit from mash temp.
		//
		Volume firstRunningsOut = getFirstRunningsOut(mashVolumeOut, equipmentProfile);

		//
		// Split the mash into first runnings wort and remaining mash liquor: volumes, hop acids, and
		// related metrics divide in proportion so both streams stay consistent with the pre-lauter mash.
		//
		VolumeUnit mashVolBefore = mashVolumeOut.getVolume();
		VolumeUnit firstRunningsVol = firstRunningsOut.getVolume();
		VolumeUnit mashVolAfter = new VolumeUnit(
			mashVolBefore.get() - firstRunningsVol.get(),
			mashVolBefore.getUnit(),
			mashVolBefore.isEstimated() || firstRunningsVol.isEstimated());

		HopAcidVolumes.applySplit(
			mashVolumeOut,
			mashVolBefore,
			firstRunningsVol,
			firstRunningsOut,
			mashVolAfter,
			mashVolumeOut);

		mashVolumeOut.setVolume(mashVolAfter);

		//
		// First-wort hops (FWH): isomerise in the first-runnings stream using the FWH utilisation
		// setting. Only incremental bitterness from this stand is stored here; boil steps add the rest.
		// There are better ways of doing this, see here for inspiration:
		// https://alchemyoverlord.wordpress.com/2016/03/06/an-analysis-of-sub-boiling-hop-utilization/
		//
		List<HopAddition> hopCharges = new ArrayList<>();
		for (HopAddition ia : getHopAdditions())
		{
			ia.setTime(new TimeUnit(60, MINUTES));
			hopCharges.add((HopAddition)ia);
		}

		if (!hopCharges.isEmpty())
		{
			EquipmentProfile tempEp = new EquipmentProfile(equipmentProfile);
			double fwhUtilisation = Double.valueOf(
				Database.getInstance().getSettings().get(Settings.FIRST_WORT_HOP_UTILISATION));
			tempEp.setHopUtilisation(new PercentageUnit(fwhUtilisation));

			for (HopAddition hop : hopCharges)
			{
				HopAcidVolumes.addHopAlpha(firstRunningsOut, hop);
				HopAcidVolumes.isomerize(
					firstRunningsOut,
					Equations.calcHopIsoAlphaAcidsMgTinseth(
						hop,
						hop.getTime(),
						firstRunningsOut.getGravity(),
						firstRunningsOut.getVolume(),
						fwhUtilisation));
			}

			for (Map.Entry<Settings.HopBitternessFormula, BitternessUnit> e :
				Brewday.getInstance().calcTotalIbuAllReported(
					tempEp,
					mashVolumeOut.getVolume(),
					mashVolumeOut.getGravity(),
					mashVolumeOut.getVolume(),
					mashVolumeOut.getGravity(),
					hopCharges).entrySet())
			{
				BitternessUnit bitternessIn = BitternessVolumes.getOrZero(firstRunningsOut, e.getKey());
				bitternessIn.add(e.getValue());
				firstRunningsOut.setBitterness(e.getKey(), bitternessIn);
			}

			List<IngredientAddition> hopAdditions = new ArrayList<>(hopCharges);
			firstRunningsOut.setIngredientAdditions(hopAdditions);
		}

		//
		// Reconcile reported IBU on both the wort and spent-mash volumes after the split and FWH pass.
		//
		List<Settings.HopBitternessFormula> reportedFormulas =
			Settings.parseReportedFormulas(Database.getInstance().getSettings());
		BitternessVolumes.syncReportedDerived(firstRunningsOut, reportedFormulas);
		BitternessVolumes.syncReportedDerived(mashVolumeOut, reportedFormulas);

		//
		// Publish first runnings and lautered mash for batch sparge or boil steps.
		//
		volumes.addOrUpdateVolume(outputFirstRunnings, firstRunningsOut);
		volumes.addOrUpdateVolume(outputLauteredMashVolume, mashVolumeOut);
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

		recipe.getVolumes().addVolume(outputLauteredMashVolume, new Volume(Volume.Type.MASH));
		recipe.getVolumes().addVolume(outputFirstRunnings, new Volume(Volume.Type.WORT));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		return Collections.singletonList(IngredientAddition.Type.HOPS);
	}

	/*-------------------------------------------------------------------------*/
	private Volume getFirstRunningsOut(
		Volume mashVolume,
		EquipmentProfile equipmentProfile)
	{
		List<FermentableAddition> grainBill = getGrainBill(mashVolume);

		WaterAddition waterAddition =
			(WaterAddition)mashVolume.getIngredientAddition(IngredientAddition.Type.WATER);

		VolumeUnit volumeOutMl = Equations.calcWortVolume(
			grainBill,
			waterAddition.getVolume(),
			equipmentProfile.getConversionEfficiency().get(PERCENTAGE));

		double outputVolMl = volumeOutMl.get(MILLILITRES) - equipmentProfile.getLauterLoss().get(MILLILITRES);

		volumeOutMl = new VolumeUnit(outputVolMl, MILLILITRES);
		// Always assume that the first running volume is estimated, despite the
		// grain and water additions being measured. We're doing this to ensure that
		// the chain of estimated quantities starts here.
		volumeOutMl.setEstimated(true);

		PercentageUnit fermentabilityOut = Equations.getWortAttenuationLimit(mashVolume.getTemperature());

		return new Volume(
			null,
			Volume.Type.WORT,
			volumeOutMl,
			mashVolume.getTemperature(),
			fermentabilityOut,
			mashVolume.getGravity(),
			new PercentageUnit(0D),
			mashVolume.getColour(),
			mashVolume.getBitterness());
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
	public String describe(Volumes v)
	{
		return StringUtils.getProcessString("lauter.desc", getName());
	}

	public String getOutputLauteredMashVolume()
	{
		return outputLauteredMashVolume;
	}

	public String getOutputFirstRunnings()
	{
		return outputFirstRunnings;
	}

	public void setOutputFirstRunnings(String outputFirstRunnings)
	{
		this.outputFirstRunnings = outputFirstRunnings;
	}

	public void setOutputLauteredMashVolume(String outputLauteredMashVolume)
	{
		this.outputLauteredMashVolume = outputLauteredMashVolume;
	}

	public String getInputMashVolume()
	{
		return inputMashVolume;
	}

	public void setInputMashVolume(String inputMashVolume)
	{
		this.inputMashVolume = inputMashVolume;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Collection<String> getInputVolumes()
	{
		return inputMashVolume==null?Collections.emptyList():Collections.singletonList(inputMashVolume);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		ArrayList<String> result = new ArrayList<>();

		if (outputLauteredMashVolume != null)
		{
			result.add(outputLauteredMashVolume);
		}
		if (outputFirstRunnings != null)
		{
			result.add(outputFirstRunnings);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<String> getInstructions()
	{
		List<String> result = new ArrayList<>();

		result.add(StringUtils.getDocString("lauter.doc"));

		String fr = this.getOutputFirstRunnings();
		Volume firstRunnings = getRecipe().getVolumes().getVolume(fr);

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == IngredientAddition.Type.HOPS)
			{
				result.add(
					StringUtils.getDocString(
						"lauter.hop.addition",
						ia.describe()));
			}
			else
			{
				result.add(StringUtils.getDocString("additions.generic", ia.describe()));
			}
		}

		result.add(StringUtils.getDocString("lauter.first.runnings",
			firstRunnings.getVolume().describe(LITRES),
			firstRunnings.getGravity().describe(SPECIFIC_GRAVITY),
			firstRunnings.getTemperature().describe(CELSIUS)));

		return result;
	}

	@Override
	public ProcessStep clone(String newName)
	{
		return new Lauter(
			newName,
			this.getDescription(),
			this.getInputMashVolume(),
			StringUtils.getProcessString("lauter.mash.vol", newName),
			StringUtils.getProcessString("lauter.first.runnings", newName));
	}
}

