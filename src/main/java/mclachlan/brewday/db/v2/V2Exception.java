package mclachlan.brewday.db.v2;

/**
 *
 */
public class V2Exception extends RuntimeException
{
	public V2Exception()
	{
	}

	public V2Exception(String message)
	{
		super(message);
	}

	public V2Exception(String message, Throwable cause)
	{
		super(message, cause);
	}

	public V2Exception(Throwable cause)
	{
		super(cause);
	}

	public V2Exception(String message, Throwable cause,
		boolean enableSuppression,
		boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
