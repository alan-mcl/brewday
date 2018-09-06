package mclachlan.brewday.process;

/**
 *
 */
public abstract class ProcessStep
{
	private String number;
	private String name;
	private String description;
	private String inputVolume;

	public ProcessStep(String number, String name, String description,
		String inputVolume)
	{
		this.number = number;
		this.name = name;
		this.description = description;
		this.inputVolume = inputVolume;
	}

	/**
	 * Apply this process step to the input fluid volume.
	 */
	public abstract void apply(Volumes volumes);

	public String getNumber()
	{
		return number;
	}

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
}
