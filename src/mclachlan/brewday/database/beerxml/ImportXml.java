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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.database.beerxml;

import java.io.File;
import java.util.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import mclachlan.brewday.ingredients.Hop;

/**
 * This is the "driver" for xml import.  It sets up the parser, catches
 * exceptions, and associates our XmlHandler class with the parser so it can
 * listen for events.  You create one of these and pass it an xml file name
 * (with path).  You get the resultant recipe with ImportXml.handler.getRecipe().
 *  I wish I could figure out how to just use ImportXml.getRecipe(), but it
 * doesn't seem to work.
 */

public class ImportXml
{
	public BeerXmlHopsHandler beerXmlHopsHandler;
	public BeerXmlFermentablesHandler beerXmlFermentablesHandler;
	public BeerXmlYeastsHandler beerXmlYeastsHandler;
	public BeerXmlMiscsHandler beerXmlMiscsHandler;
	public BeerXmlWatersHandler beerXmlWatersHandler;

	public ImportXml(String fileName, String type)
	{
		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try
		{
			// Parse the input
			SAXParser saxParser = factory.newSAXParser();
			if (type.equalsIgnoreCase("hops"))
			{
				beerXmlHopsHandler = new BeerXmlHopsHandler();
				saxParser.parse(new File(fileName), beerXmlHopsHandler);
			}
			else if (type.equalsIgnoreCase("fermentables"))
			{
				beerXmlFermentablesHandler = new BeerXmlFermentablesHandler();
				saxParser.parse(new File(fileName), beerXmlFermentablesHandler);
			}
			else if (type.equalsIgnoreCase("yeasts"))
			{
				beerXmlYeastsHandler = new BeerXmlYeastsHandler();
				saxParser.parse(new File(fileName), beerXmlYeastsHandler);
			}
			else if (type.equalsIgnoreCase("miscs"))
			{
				beerXmlMiscsHandler = new BeerXmlMiscsHandler();
				saxParser.parse(new File(fileName), beerXmlMiscsHandler);
			}
			else if (type.equalsIgnoreCase("waters"))
			{
				beerXmlWatersHandler = new BeerXmlWatersHandler();
				saxParser.parse(new File(fileName), beerXmlWatersHandler);
			}
		}
		catch (Exception x)
		{
			x.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception
	{
		List<Hop> hops = new ImportXml("beerxml/hops.xml", "hops").beerXmlHopsHandler.getResult();

		System.out.println("hops = [" + hops + "]");
	}
}