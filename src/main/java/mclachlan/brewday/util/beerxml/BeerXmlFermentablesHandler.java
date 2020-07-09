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

package mclachlan.brewday.util.beerxml;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.DiastaticPowerUnit;
import mclachlan.brewday.math.PercentageUnit;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class BeerXmlFermentablesHandler extends DefaultHandler
{
	private String currentElement;
	private Fermentable current;
	private List<Fermentable> result;

	/*-------------------------------------------------------------------------*/

	public List<Fermentable> getResult()
	{
		return result;
	}

	/*-------------------------------------------------------------------------*/
	public void setDocumentLocator(Locator l)
	{
	}

	public void startDocument() throws SAXException
	{
	}

	public void endDocument() throws SAXException
	{
	}

	public void startElement(
		String namespaceURI,
		String lName, // local unit
		String qName, // qualified unit
		Attributes attrs) throws SAXException
	{
		String eName = lName; // element unit

		if ("".equals(eName))
		{
			eName = qName; // namespaceAware = false
		}

		currentElement = eName;
		// currentAttributes = attrs;

		if (eName.equalsIgnoreCase("fermentables"))
		{
			result = new ArrayList<>();
		}
		if (eName.equalsIgnoreCase("fermentable"))
		{
			current = new Fermentable();
		}
	}

	public void endElement(
		String namespaceURI,
		String sName, // simple name
		String qName // qualified name
	) throws SAXException
	{
		if (qName.equalsIgnoreCase("fermentables"))
		{

		}
		if (qName.equalsIgnoreCase("fermentable"))
		{
			result.add(current);
		}
	}

	public void characters(char buf[], int offset, int len) throws SAXException
	{
		String s = new String(buf, offset, len);

		String text = s.trim();

		if (!text.equals(""))
		{
			if (currentElement.equalsIgnoreCase("notes"))
			{
				current.setDescription(text);
			}
			if (currentElement.equalsIgnoreCase("name"))
			{
				current.setName(text);
			}
			if (currentElement.equalsIgnoreCase("amount"))
			{
				// ignore for db import
//				double weightKg = Double.parseDouble(s.trim());
			}
			if (currentElement.equalsIgnoreCase("type"))
			{
				Fermentable.Type type = fermentableTypeFromBeerXml(text);
				current.setType(type);
			}
			if (currentElement.equalsIgnoreCase("origin"))
			{
				current.setOrigin(text);
			}
			if (currentElement.equalsIgnoreCase("supplier"))
			{
				current.setSupplier(text);
			}
			if (currentElement.equalsIgnoreCase("yield"))
			{
				current.setYield(getPercentage(text));
			}
			if (currentElement.equalsIgnoreCase("color"))
			{
				// todo, the spec says "The color of the item in Lovibond Units (SRM for liquid extracts)."
				// should be converting from Lovibond here for everything except extract
				// still the difference is small so it's probably ok.
				current.setColour(new ColourUnit(Double.parseDouble(text)));
			}
			if (currentElement.equalsIgnoreCase("add_after_boil"))
			{
				current.setAddAfterBoil(Boolean.parseBoolean(text));
			}
			if (currentElement.equalsIgnoreCase("coarse_fine_diff"))
			{
				current.setCoarseFineDiff(getPercentage(text));
			}
			if (currentElement.equalsIgnoreCase("moisture"))
			{
				current.setMoisture(getPercentage(text));
			}
			if (currentElement.equalsIgnoreCase("diastatic_power"))
			{
				current.setDiastaticPower(new DiastaticPowerUnit(Double.parseDouble(text)));
			}
			if (currentElement.equalsIgnoreCase("protein"))
			{
				current.setProtein(getPercentage(text));
			}
			if (currentElement.equalsIgnoreCase("max_in_batch"))
			{
				current.setMaxInBatch(getPercentage(text));
			}
			if (currentElement.equalsIgnoreCase("recommend_mash"))
			{
				current.setRecommendMash(Boolean.parseBoolean(text));
			}
			if (currentElement.equalsIgnoreCase("ibu_gal_per_lb"))
			{
				current.setIbuGalPerLb(Double.parseDouble(text));
			}
		}
	}

	protected PercentageUnit getPercentage(String text)
	{
		return new PercentageUnit(Double.parseDouble(text)/100D);
	}

	public void ignorableWhitespace(char buf[], int offset,
		int len) throws SAXException
	{
	}

	public void processingInstruction(String target,
		String data) throws SAXException
	{
	}

	public void error(SAXParseException x) throws SAXParseException
	{
		throw x;
	}

	public void warning(SAXParseException x) throws SAXParseException
	{
		x.printStackTrace();
	}

	/*-------------------------------------------------------------------------*/
	private Fermentable.Type fermentableTypeFromBeerXml(String s)
	{
		if (s.equalsIgnoreCase("grain"))
		{
			return Fermentable.Type.GRAIN;
		}
		else if (s.equalsIgnoreCase("sugar"))
		{
			return Fermentable.Type.SUGAR;
		}
		else if (s.equalsIgnoreCase("extract"))
		{
			return Fermentable.Type.LIQUID_EXTRACT;
		}
		else if (s.equalsIgnoreCase("dry extract"))
		{
			return Fermentable.Type.DRY_EXTRACT;
		}
		else if (s.equalsIgnoreCase("adjunct"))
		{
			return Fermentable.Type.ADJUNCT;
		}
		else if (s.equalsIgnoreCase("juice"))
		{
			return Fermentable.Type.JUICE;
		}
		else
		{
			throw new BrewdayException("invalid BeerXML: ["+s+"]");
		}
	}

}
