package mclachlan.brewday.importexport;

import java.io.File;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import mclachlan.brewday.importexport.beerxml.BeerXmlRecipe;
import mclachlan.brewday.importexport.beerxml.BeerXmlRecipesHandler;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ImportDateParserTest
{
	@Test
	public void parseLocalDateSupportsBeerSmithAndIsoFormats()
	{
		assertEquals(LocalDate.of(2019, 9, 9), ImportDateParser.parseLocalDate("09 Sep 2019"));
		assertEquals(LocalDate.of(2020, 6, 6), ImportDateParser.parseLocalDate("2020-06-06"));
		assertEquals(LocalDate.of(2012, 12, 8), ImportDateParser.parseLocalDate("08 Dec 2012"));
	}

	@Test
	public void tryParseLocalDateReturnsNullForBlankOrGarbage()
	{
		assertNull(ImportDateParser.tryParseLocalDate(null));
		assertNull(ImportDateParser.tryParseLocalDate(""));
		assertNull(ImportDateParser.tryParseLocalDate("   "));
		assertNull(ImportDateParser.tryParseLocalDate("not-a-date"));
	}

	@Test(expected = DateTimeException.class)
	public void parseLocalDateThrowsForGarbage()
	{
		ImportDateParser.parseLocalDate("not-a-date");
	}

	@Test
	public void beerXmlFixtureRecipeDateParses() throws Exception
	{
		File fixture = new File("test_data/beerxml/test_keg.xml");
		Assume.assumeTrue("fixture missing: " + fixture.getPath(), fixture.isFile());

		BeerXmlRecipesHandler handler = new BeerXmlRecipesHandler(false);
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		saxParser.parse(fixture, handler);

		List<BeerXmlRecipe> recipes = handler.getResult();
		assertTrue(recipes.stream().anyMatch(r -> r.getDate() != null));
		assertTrue(recipes.stream().anyMatch(r -> LocalDate.of(2019, 9, 9).equals(r.getDate())));
	}
}
