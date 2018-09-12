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
public abstract class ProcessStep
{
	private String name;
	private String description;
	private String inputVolume;
	private String outputVolume;

	public ProcessStep(String name, String description, String inputVolume,
		String outputVolume)
	{
		this.name = name;
		this.description = description;
		this.inputVolume = inputVolume;
		this.outputVolume = outputVolume;
	}

	/**
	 * Apply this process step to the input fluid volume.
	 * @return any output volumes of this step
	 */
	public abstract java.util.List<String> apply(Volumes volumes);

	public String getName()
	{
		return name;
	}

	public String getDescription()
	{
		return description;
	}

	public String getInputVolume()
	{
		return inputVolume;
	}

	public Volume getInputVolume(Volumes volumes)
	{
		return volumes.getVolume(getInputVolume());
	}

	public abstract String describe(Volumes v);

	public String getOutputVolume()
	{
		return outputVolume;
	}

	public void setOutputVolume(String outputVolume)
	{
		this.outputVolume = outputVolume;
	}

	public static enum Type
	{
		BATCH_SPARGE("Batch Sparge"),
		BOIL("Boil"),
		COOL("Cool"),
		DILUTE("Dilute"),
		FERMENT("Ferment"),
		MASH_IN("Mash In"),
		MASH_OUT("Mash Out"),
		STAND("Stand");

		private String name;

		Type(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}
}
