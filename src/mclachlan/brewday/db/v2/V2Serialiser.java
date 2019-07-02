package mclachlan.brewday.db.v2;

import java.util.*;

/**
 * Can serialise type E to and from a Map representation
 */
public interface V2Serialiser<E>
{
	/**
	 * Serialise this type to a Map.
	 */
	Map toMap(E e);

	/**
	 * Deserialise this type from a Map.
	 */
	E fromMap(Map<String, ?> map);
}
