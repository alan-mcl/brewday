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

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.ingredients.Misc;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class BeerXmlMiscsHandler extends DefaultHandler implements V2DataObjectImporter<Misc>
{
	private String currentElement;
	private Misc current;
	private List<Misc> result = new ArrayList<>();
	private boolean parsing = false;

	private StringBuilder nameBuffer, descBuffer, usageBuffer;

	/*-------------------------------------------------------------------------*/

	public List<Misc> getResult()
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

		if (eName.equalsIgnoreCase("miscs"))
		{
			parsing = true;
		}
		if (eName.equalsIgnoreCase("misc"))
		{
			current = new Misc();
			nameBuffer = new StringBuilder();
			descBuffer = new StringBuilder();
			usageBuffer = new StringBuilder();
		}
	}

	public void endElement(
		String namespaceURI,
		String sName, // simple name
		String qName // qualified name
	) throws SAXException
	{
		if (qName.equalsIgnoreCase("miscs"))
		{
			parsing = false;
		}
		if (qName.equalsIgnoreCase("misc"))
		{
			current.setName(nameBuffer.toString());
			current.setDescription(descBuffer.toString());
			current.setUsageRecommendation(usageBuffer.toString());
			result.add(current);
		}
	}

	public void characters(char[] buf, int offset, int len) throws SAXException
	{
		if (parsing)
		{
			String s = new String(buf, offset, len);

			String text = s.trim();

			if (!text.equals(""))
			{
				if (currentElement.equalsIgnoreCase("name"))
				{
					nameBuffer.append(text);
				}
				else if (currentElement.equalsIgnoreCase("notes"))
				{
					descBuffer.append(text);
				}
				else if (currentElement.equalsIgnoreCase("use_for"))
				{
					usageBuffer.append(text);
				}
				else if (currentElement.equalsIgnoreCase("type"))
				{
					Misc.Type type = miscTypeFromBeerXml(text);
					current.setType(type);
				}
				else if (currentElement.equalsIgnoreCase("use"))
				{
					Misc.Use type = miscUseFromBeerXml(text);
					current.setUse(type);
				}
			}
		}
	}


	protected double getPercentage(String text)
	{
		return Double.parseDouble(text)/100D;
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
	private Misc.Type miscTypeFromBeerXml(String s)
	{
		if (s.equalsIgnoreCase("spice"))
		{
			return Misc.Type.SPICE;
		}
		else if (s.equalsIgnoreCase("fining"))
		{
			return Misc.Type.FINING;
		}
		else if (s.equalsIgnoreCase("water agent"))
		{
			return Misc.Type.WATER_AGENT;
		}
		else if (s.equalsIgnoreCase("herb"))
		{
			return Misc.Type.HERB;
		}
		else if (s.equalsIgnoreCase("flavor"))
		{
			return Misc.Type.FLAVOUR;
		}
		else if (s.equalsIgnoreCase("other"))
		{
			return Misc.Type.OTHER;
		}
		else
		{
			throw new BrewdayException("invalid BeerXML: ["+s+"]");
		}
	}

	/*-------------------------------------------------------------------------*/
	private Misc.Use miscUseFromBeerXml(String s)
	{
		if (s.equalsIgnoreCase("boil"))
		{
			return Misc.Use.BOIL;
		}
		else if (s.equalsIgnoreCase("mash"))
		{
			return Misc.Use.MASH;
		}
		else if (s.equalsIgnoreCase("primary"))
		{
			return Misc.Use.PRIMARY;
		}
		else if (s.equalsIgnoreCase("secondary"))
		{
			return Misc.Use.SECONDARY;
		}
		else if (s.equalsIgnoreCase("bottling"))
		{
			return Misc.Use.BOTTLING;
		}
		else
		{
			throw new BrewdayException("invalid BeerXML: ["+s+"]");
		}
	}
}
