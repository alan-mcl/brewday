package mclachlan.brewday.math;

import mclachlan.brewday.BrewdayException;

/**
 * Container for an arbitrary physical quantity. Defaults to grams
 */
public class ArbitraryPhysicalQuantity extends Quantity
{
	/** This defaults to grams */
	private double amount;

	/** The unit this is stored in */
	private Unit unit;

	public ArbitraryPhysicalQuantity(double amount, Unit unit)
	{
		set(amount, unit);
	}

	public ArbitraryPhysicalQuantity(WeightUnit weight)
	{
		this(weight.get(Unit.GRAMS), Unit.GRAMS);
	}

	/**
	 * @return
	 * 	amount in whatever unit this contains
	 */
	@Override
	public double get()
	{
		return amount;
	}

	@Override
	public double get(Unit unit)
	{
		if (unit != this.unit)
		{
			throw new BrewdayException("Invalid: "+unit);
		}

		return amount;
	}

	/**
	 * @param amount in grams
	 */
	@Override
	public void set(double amount)
	{
		this.amount = amount;
	}

	@Override
	public void set(double amount, Unit unit)
	{
		this.amount = amount;
		this.unit = unit;
	}

	public Unit getUnit()
	{
		return unit;
	}
}
