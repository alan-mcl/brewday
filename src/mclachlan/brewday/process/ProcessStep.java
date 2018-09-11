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
