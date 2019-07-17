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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;

/**
 *
 */
public class Boil extends ProcessStep
{
	/** boil duration in minutes */
	private double duration;

	private String inputWortVolume;
	private String outputWortVolume;

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
		List<IngredientAddition> ingredientAdditions,
		double duration)
	{
		super(name, description, Type.BOIL);
		this.inputWortVolume = inputWortVolume;
		this.outputWortVolume = outputWortVolume;
		setIngredients(ingredientAdditions);
		this.duration = duration;
	}

	/*-------------------------------------------------------------------------*/
	public Boil(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.BOIL), "Boil", Type.BOIL);

		// todo: find last wort vol?
		this.inputWortVolume = recipe.getVolumes().getVolumeByType(Volume.Type.WORT);
		this.outputWortVolume = getName()+" output";
		this.duration = 60;
	}

	/*-------------------------------------------------------------------------*/
	public Boil(Boil step)
	{
		super(step.getName(), step.getDescription(), Type.BOIL);

		this.inputWortVolume = step.inputWortVolume;
		this.outputWortVolume = step.outputWortVolume;
		this.duration = step.duration;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public void apply(Volumes volumes, Recipe recipe, ErrorsAndWarnings log)
	{
		if (!volumes.contains(inputWortVolume))
		{
			log.addError("Volume does not exist ["+inputWortVolume+"]");
			return;
		}

		EquipmentProfile equipmentProfile =
			Database.getInstance().getEquipmentProfiles().get(recipe.getEquipmentProfile());
		if (equipmentProfile == null)
		{
			log.addError("invalid equipment profile ["+equipmentProfile+"]");
			return;
		}

		WortVolume input = (WortVolume)(volumes.getVolume(inputWortVolume));

		if (input.getVolume()*1.2D >= equipmentProfile.getBoilKettleVolume())
		{
			log.addWarning(
				String.format(
					"Boil kettle (%.2f l) may not be large enough for boil volume (%.2f l)",
					equipmentProfile.getBoilKettleVolume()/1000, input.getVolume()/1000));
		}

		// todo: fermentable additions
		List<IngredientAddition> hopCharges = new ArrayList<IngredientAddition>();
		for (IngredientAddition item : getIngredients())
		{
			if (item instanceof HopAddition)
			{
				hopCharges.add(item);
			}
		}

		double tempOut = 100D;

		double boilEvapourationRatePerHour =
			equipmentProfile.getBoilEvapourationRate();

		double boiledOff = input.getVolume() * boilEvapourationRatePerHour * (duration/60D);

		double volumeOut = input.getVolume() - boiledOff;

		DensityUnit gravityOut = Equations.calcGravityWithVolumeChange(
			input.getVolume(), input.getGravity(), volumeOut);

		double abvOut = Equations.calcAbvWithVolumeChange(
			input.getVolume(), input.getAbv(), volumeOut);

		// todo: account for kettle caramelisation darkening?
		double colourOut = Equations.calcColourWithVolumeChange(
			input.getVolume(), input.getColour(), volumeOut);

		double bitternessOut = input.getBitterness();
		for (IngredientAddition hopCharge : hopCharges)
		{
			bitternessOut +=
				Equations.calcIbuTinseth(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					new DensityUnit((gravityOut.get() + input.getGravity().get()) / 2),
					(volumeOut + input.getVolume()) / 2,
					equipmentProfile.getHopUtilisation());
		}

		volumes.addVolume(
			outputWortVolume,
			new WortVolume(
				volumeOut,
				tempOut,
				input.getFermentability(),
				gravityOut,
				abvOut,
				colourOut,
				bitternessOut));
	}

	@Override
	public void dryRun(Recipe recipe, ErrorsAndWarnings log)
	{
		recipe.getVolumes().addVolume(outputWortVolume, new WortVolume());
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String describe(Volumes v)
	{
		return String.format("Boil: %.0f min", duration);
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

	public double getDuration()
	{
		return duration;
	}

	public void setDuration(double duration)
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
	@Override
	public Collection<String> getInputVolumes()
	{
		return new ArrayList<String>(Arrays.asList(inputWortVolume));
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public Collection<String> getOutputVolumes()
	{
		return Arrays.asList(outputWortVolume);
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public List<IngredientAddition.Type> getSupportedIngredientAdditions()
	{
		// todo: fermentable & misc additions
		return Arrays.asList(IngredientAddition.Type.HOPS);
	}
}
