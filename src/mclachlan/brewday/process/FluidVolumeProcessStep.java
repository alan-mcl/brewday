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

/**
 *
 */
public abstract class FluidVolumeProcessStep extends ProcessStep
{
	private String inputVolume;
	private String outputVolume;

	public FluidVolumeProcessStep(String name, String description,
		String inputVolume, String outputVolume)
	{
		super(name, description);
		this.inputVolume = inputVolume;
		this.outputVolume = outputVolume;
	}

	public String getInputVolume()
	{
		return inputVolume;
	}

	public Volume getInputVolume(Volumes volumes)
	{
		return volumes.getVolume(getInputVolume());
	}

	public String getOutputVolume()
	{
		return outputVolume;
	}

	public void setOutputVolume(String outputVolume)
	{
		this.outputVolume = outputVolume;
	}

}
