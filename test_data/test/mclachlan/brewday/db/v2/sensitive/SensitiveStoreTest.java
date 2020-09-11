/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.db.v2.sensitive;

import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SensitiveStoreTest
{
	private String rootDir;

	@Before
	@After
	public void setUp() throws Exception
	{
		new File(rootDir, "test.sensitive.v2").delete();
		new File(rootDir, "test.dist.v2").delete();
	}

	private SensitiveStore getSensitiveStore() throws Exception
	{
		rootDir = "./test/resources/db/sensitive";
		SensitiveStore ss = new SensitiveStore(rootDir, "test");
		ss.init("SensitiveStoreTest");

		return ss;
	}

	@Test
	public void init() throws Exception
	{
		getSensitiveStore();
		return;
	}

	@Test
	public void getAndSet() throws Exception
	{
		SensitiveStore ss = getSensitiveStore();

		assertNull(ss.get("test"));

		ss.set("xxx", "yyy");

		assertEquals("yyy", ss.get("xxx"));
	}
}