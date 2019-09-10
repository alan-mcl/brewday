package mclachlan.brewday.math;

import mclachlan.brewday.BrewdayException;

/**
 *
 */
public class PercentageUnit extends Quantity
{
	double percentage;

	public PercentageUnit()
	{
	}

	public PercentageUnit(double percentage)
	{
		this.percentage = percentage;
	}

	public PercentageUnit(double amount, boolean estimated)
	{
		this(amount);
		setEstimated(estimated);
	}

	@Override
	public double get()
	{
		return percentage;
	}

	@Override
	public double get(Unit unit)
	{
		if (unit != Unit.PERCENTAGE)
		{
			throw new BrewdayException("invalid: "+unit);
		}

		return percentage;
	}

	@Override
	public void set(double amount)
	{
		this.percentage = amount;
	}

	@Override
	public void set(double amount, Unit unit)
	{
		if (unit != Unit.PERCENTAGE)
		{
			throw new BrewdayException("invalid: "+unit);
		}

		this.percentage = amount;
	}

	@Override
	public Unit getUnit()
	{
		return Unit.PERCENTAGE;
	}

	@Override
	public String toString()
	{
		return "PercentageUnit{percentage=" + percentage + "}";
	}
}
