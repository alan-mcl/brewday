package mclachlan.brewday.ingredients;

/**
 *
 */
public class Grain
{
	/**
	 * Extract potential in USA units:
	 * GU that can be achieved with 1.00 pound (455 g) of malt mashed in 1.00 gallon (3.78 L) of water.
	 * source: https://byo.com/article/understanding-malt-spec-sheets-advanced-brewing/
	 */
	private double extractPotential;

	/** colour in SRM */
	private double colour;

	/** weight in g */
	private double weight;

	public Grain(double extractPotential, double colour, double weight)
	{
		this.extractPotential = extractPotential;
		this.colour = colour;
		this.weight = weight;
	}

	public double getExtractPotential()
	{
		return extractPotential;
	}

	public double getColour()
	{
		return colour;
	}

	public double getWeight()
	{
		return weight;
	}

	public void setWeight(double weight)
	{
		this.weight = weight;
	}
}
