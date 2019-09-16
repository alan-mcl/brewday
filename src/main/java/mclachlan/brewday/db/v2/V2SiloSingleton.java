package mclachlan.brewday.db.v2;


import java.io.BufferedReader;
import java.io.BufferedWriter;

/**
 * A Silo that stores a a single object.
 *
 * @param <V>
 *    The type of the values in this silo
 */
public interface V2SiloSingleton<V>
{
	/**
	 * Load up this silo from the given input stream.
	 */
	V load(BufferedReader reader) throws Exception;

	/**
	 * Save this silo to the given output stream.
	 */
	void save(BufferedWriter writer, V obj) throws Exception;
}
