package mclachlan.brewday.db.v2;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.*;

/**
 * A Silo that stores a map of Strings to complex types.
 *
 * @param <V>
 *    The type of the values in this silo
 */
public interface V2SiloMap<V>
{
	/**
	 * Load up this silo from the given input stream.
	 */
	Map<String,V> load(BufferedReader reader) throws Exception;

	/**
	 * Save this silo to the given output stream.
	 */
	void save(BufferedWriter writer, Map<String,V> map) throws Exception;
}
