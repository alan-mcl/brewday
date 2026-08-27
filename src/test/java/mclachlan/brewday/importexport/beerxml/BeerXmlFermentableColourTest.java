package mclachlan.brewday.importexport.beerxml;

import java.io.StringReader;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import mclachlan.brewday.recipe.FermentableAddition;
import org.junit.Test;
import org.xml.sax.InputSource;

import static mclachlan.brewday.math.Quantity.Unit.SRM;
import static org.junit.Assert.assertEquals;

public class BeerXmlFermentableColourTest
{
	private static final String RECIPE_HEADER =
		"<?xml version=\"1.0\"?><RECIPES><RECIPE><NAME>Test</NAME><VERSION>1</VERSION>";

	private static final String RECIPE_FOOTER = "</RECIPE></RECIPES>";

	private List<FermentableAddition> parseFermentables(
		String fermentableXml,
		boolean fixBeerSmithBugs) throws Exception
	{
		String xml = RECIPE_HEADER +
			"<FERMENTABLES>" + fermentableXml + "</FERMENTABLES>" +
			RECIPE_FOOTER;

		BeerXmlRecipesHandler handler = new BeerXmlRecipesHandler(fixBeerSmithBugs);
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		saxParser.parse(new InputSource(new StringReader(xml)), handler);

		return handler.getResult().get(0).getFermentables();
	}

	@Test
	public void grainColourImportedAsLovibond() throws Exception
	{
		String ferm = "<FERMENTABLE><NAME>Test Grain</NAME><VERSION>1</VERSION>"
			+ "<TYPE>Grain</TYPE><AMOUNT>1.0</AMOUNT><YIELD>80</YIELD><COLOR>10</COLOR>"
			+ "</FERMENTABLE>";

		List<FermentableAddition> fermentables = parseFermentables(ferm, false);

		assertEquals(1.3546 * 10 - 0.6,
			fermentables.get(0).getFermentable().getColour().get(SRM), 0.0001);
	}

	@Test
	public void extractColourImportedAsSrm() throws Exception
	{
		String ferm = "<FERMENTABLE><NAME>Test Extract</NAME><VERSION>1</VERSION>"
			+ "<TYPE>Extract</TYPE><AMOUNT>1.0</AMOUNT><YIELD>80</YIELD><COLOR>10</COLOR>"
			+ "</FERMENTABLE>";

		List<FermentableAddition> fermentables = parseFermentables(ferm, false);

		assertEquals(10D, fermentables.get(0).getFermentable().getColour().get(SRM), 0.0001);
	}

	@Test
	public void grainColourImportedAsSrmWhenFixBeerSmithBugs() throws Exception
	{
		String ferm = "<FERMENTABLE><NAME>Test Grain</NAME><VERSION>1</VERSION>"
			+ "<TYPE>Grain</TYPE><AMOUNT>1.0</AMOUNT><YIELD>80</YIELD><COLOR>10</COLOR>"
			+ "</FERMENTABLE>";

		List<FermentableAddition> fermentables = parseFermentables(ferm, true);

		assertEquals(10D, fermentables.get(0).getFermentable().getColour().get(SRM), 0.0001);
	}
}
