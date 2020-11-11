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

package mclachlan.brewday.importexport.beerxml;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.QuantityValueSerialiser;
import mclachlan.brewday.db.v2.ReflectiveSerialiser;
import mclachlan.brewday.db.v2.SimpleMapSilo;
import mclachlan.brewday.db.v2.V2SiloMap;
import mclachlan.brewday.math.*;
import mclachlan.brewday.style.Style;

public class ImportBeerXml
{
	public BeerXmlHopsHandler beerXmlHopsHandler;
	public BeerXmlFermentablesHandler beerXmlFermentablesHandler;
	public BeerXmlYeastsHandler beerXmlYeastsHandler;
	public BeerXmlMiscsHandler beerXmlMiscsHandler;
	public BeerXmlWatersHandler beerXmlWatersHandler;
	public BeerXmlEquipmentsHandler beerXmlEquipmentsHandler;
	public BeerXmlStylesHandler beerXmlStylesHandler;

	/*-------------------------------------------------------------------------*/
	public ImportBeerXml(String fileName, String type)
	{
		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try
		{
			String xmlContents = Files.readString(Paths.get(fileName));

			// Parse the input
			SAXParser saxParser = factory.newSAXParser();
			if (type.equalsIgnoreCase("hops"))
			{
				beerXmlHopsHandler = new BeerXmlHopsHandler();
				saxParser.parse(new StringBufferInputStream(xmlContents), beerXmlHopsHandler);
			}
			else if (type.equalsIgnoreCase("fermentables"))
			{
				beerXmlFermentablesHandler = new BeerXmlFermentablesHandler(true);
				saxParser.parse(new StringBufferInputStream(xmlContents), beerXmlFermentablesHandler);
			}
			else if (type.equalsIgnoreCase("yeasts"))
			{
				beerXmlYeastsHandler = new BeerXmlYeastsHandler();
				saxParser.parse(new StringBufferInputStream(xmlContents), beerXmlYeastsHandler);
			}
			else if (type.equalsIgnoreCase("miscs"))
			{
				beerXmlMiscsHandler = new BeerXmlMiscsHandler();
				saxParser.parse(new StringBufferInputStream(xmlContents), beerXmlMiscsHandler);
			}
			else if (type.equalsIgnoreCase("waters"))
			{
				beerXmlWatersHandler = new BeerXmlWatersHandler();
				saxParser.parse(new StringBufferInputStream(xmlContents), beerXmlWatersHandler);
			}
			else if (type.equalsIgnoreCase("equipments"))
			{
				beerXmlEquipmentsHandler = new BeerXmlEquipmentsHandler();
				saxParser.parse(new StringBufferInputStream(xmlContents), beerXmlEquipmentsHandler);
			}
			else if (type.equalsIgnoreCase("styles"))
			{
				beerXmlStylesHandler = new BeerXmlStylesHandler();
				saxParser.parse(new StringBufferInputStream(xmlContents), beerXmlStylesHandler);
			}
		}
		catch (Exception x)
		{
			x.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception
	{
		List<Style> input = new ImportBeerXml("beerxml/styles.xml", "styles")
			.beerXmlStylesHandler.getResult();

		ReflectiveSerialiser<Style> serialiser = new ReflectiveSerialiser<>(
			Style.class,
			"name",
			"styleGuideName",
			"category",
			"categoryNumber",
			"styleLetter",
			"styleGuide",
			"type",
			"ogMin",
			"ogMax",
			"fgMin",
			"fgMax",
			"ibuMin",
			"ibuMax",
			"colourMin",
			"colourMax",
			"carbMin",
			"carbMax",
			"abvMin",
			"abvMax",
			"notes",
			"profile",
			"ingredients",
			"examples");
		serialiser.addCustomSerialiser(DensityUnit.class, new QuantityValueSerialiser<DensityUnit>(DensityUnit.class));
		serialiser.addCustomSerialiser(ColourUnit.class, new QuantityValueSerialiser<ColourUnit>(ColourUnit.class));
		serialiser.addCustomSerialiser(BitternessUnit.class, new QuantityValueSerialiser<BitternessUnit>(BitternessUnit.class));
		serialiser.addCustomSerialiser(CarbonationUnit.class, new QuantityValueSerialiser<CarbonationUnit>(CarbonationUnit.class));
		serialiser.addCustomSerialiser(PercentageUnit.class, new QuantityValueSerialiser<PercentageUnit>(PercentageUnit.class));

		V2SiloMap silo = new SimpleMapSilo<>(serialiser);


		Map<String, Style> map = new HashMap<>();
		for (Style e : input)
		{
			map.put(e.getName(), e);
		}

		StringWriter buffer = new StringWriter();

		silo.save(new BufferedWriter(buffer), map, Database.getInstance());

		String fileContents = buffer.toString();

		// The SAX parser seems to output some decomposed Unicode characters.
		// These cause issues down the line with JFX text controls
		// So here we normalise the string to use composed form instead
		fileContents = Normalizer.normalize(fileContents, Normalizer.Form.NFC);

		String fileName = "db/styles.json";

		FileWriter fileWriter = new FileWriter(fileName);
		fileWriter.write(fileContents);
		fileWriter.flush();
		fileWriter.close();

	}

}