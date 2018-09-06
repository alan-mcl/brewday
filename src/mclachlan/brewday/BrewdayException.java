package mclachlan.brewday;

/**
 *
 */
public class BrewdayException extends RuntimeException
{
	public BrewdayException(String message)
	{
		super(message);
	}

	public BrewdayException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public BrewdayException(Throwable cause)
	{
		super(cause);
	}
}
