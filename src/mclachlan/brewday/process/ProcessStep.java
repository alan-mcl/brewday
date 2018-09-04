package mclachlan.brewday.process;

/**
 *
 */
public abstract class ProcessStep
{
	private String number;
	private String name;
	private String description;

	public ProcessStep(String number, String name, String description)
	{
		this.number = number;
		this.name = name;
		this.description = description;
	}

	/**
	 * Apply this process step to the input fluid volume.
	 * @return
	 * 	The fluid volume that is the output of this step
	 */
	public abstract FluidVolume apply(FluidVolume input);

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
}
