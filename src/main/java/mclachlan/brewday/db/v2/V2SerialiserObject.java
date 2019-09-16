package mclachlan.brewday.db.v2;

/**
 * Can serialise type E to and from a Object representation
 */
public interface V2SerialiserObject<E>
{
	/**
	 * Serialise this type to a Object.
	 */
	Object toObj(E e);

	/**
	 * Deserialise this type from an Object.
	 */
	E fromObj(Object obj);
}
