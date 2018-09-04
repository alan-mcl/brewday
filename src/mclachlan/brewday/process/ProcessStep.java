package mclachlan.brewday.process;

/**
 *
 */
public abstract class ProcessStep
{
	private String number;
	private String name;
	private String description;

	protected ProcessStep(String number, String name, String description)
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
}
