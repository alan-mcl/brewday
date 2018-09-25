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
import mclachlan.brewday.math.Equations;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.FermentableAdditionList;

/**
 * Gather the first running from a MashVolume
 */
public class MashOut extends ProcessStep
{
	private String mashVolume;
	private String outputWortVolume;

	/** mash tun loss in ml */
	private double tunLoss;

	public MashOut(
		String name,
		String description,
		String mashVolume,
		String outputWortVolume,
		double tunLoss)
	{
		super(name, description, Type.MASH_OUT);
		this.mashVolume = mashVolume;
		this.outputWortVolume = outputWortVolume;
		this.tunLoss = tunLoss;
	}

	public MashOut(Recipe recipe)
	{
		super(recipe.getUniqueStepName(Type.MASH_OUT), "Mash out", Type.MASH_OUT);

		this.mashVolume = recipe.getVolumes().getVolumeByType(Volume.Type.MASH);
		this.tunLoss = 3000;
		this.outputWortVolume = getName() + " output";
	}

	@Override
	public void apply(Volumes volumes, Recipe recipe,
		ErrorsAndWarnings log)
	{
		if (!volumes.contains(mashVolume))
		{
			log.addError("volume does not exist ["+mashVolume+"]");
			return;
		}

		MashVolume mashVolume = (MashVolume)(volumes.getVolume(this.mashVolume));

		FermentableAdditionList ingredientAddition = mashVolume.getFermentables();

		double grainWeight = 0D;
		for (FermentableAddition fermentableAddition : ingredientAddition.getIngredients())
		{
			grainWeight += fermentableAddition.getWeight();
		}

		double volumeOut =
			Equations.calcWortVolume(
				grainWeight,
				mashVolume.getWater().getVolume())
			- tunLoss;

		volumes.addVolume(
			outputWortVolume,
			new WortVolume(
				volumeOut,
				mashVolume.getTemperature(),
				mashVolume.getGravity(),
				0D,
				mashVolume.getColour(),
				0D));
	}

	@Override
	public String describe(Volumes v)
	{
		return String.format("Mash Out");
	}

	public String getMashVolume()
	{
		return mashVolume;
	}

	public String getOutputWortVolume()
	{
		return outputWortVolume;
	}

	public double getTunLoss()
	{
		return tunLoss;
	}

	public void setMashVolume(String mashVolume)
	{
		this.mashVolume = mashVolume;
	}

	public void setTunLoss(double tunLoss)
	{
		this.tunLoss = tunLoss;
	}

	@Override
	public Collection<String> getInputVolumes()
	{
		return Arrays.asList(mashVolume);
	}

	@Override
	public Collection<String> getOutputVolumes()
	{
		return Arrays.asList(outputWortVolume);
	}

}
