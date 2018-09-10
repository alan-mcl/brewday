package mclachlan.brewday.ingredients;

/**
 *
 */
public class Hop
{
	/** AA in % */
	private double alphaAcid;

	/** weight in g */
	private double weight;

	public Hop(double alphaAcid, double weight)
	{
		this.alphaAcid = alphaAcid;
		this.weight = weight;
	}

	public double getAlphaAcid()
	{
		return alphaAcid;
	}

	public double getWeight()
	{
		return weight;
	}
}
