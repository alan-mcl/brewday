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
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.PhUnit;
import mclachlan.brewday.math.PpmUnit;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class BeerXmlWatersHandler extends DefaultHandler implements V2DataObjectImporter<Water>
{
	private String currentElement;
	private Water current;
	private final List<Water> result = new ArrayList<>();
	private boolean parsing = false;

	private StringBuilder nameBuffer, descBuffer;

	/*-------------------------------------------------------------------------*/

	public List<Water> getResult()
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

		if (eName.equalsIgnoreCase("waters"))
		{
			parsing = true;
		}
		if (eName.equalsIgnoreCase("water"))
		{
			current = new Water();
			nameBuffer = new StringBuilder();
			descBuffer = new StringBuilder();
		}
	}

	public void endElement(
		String namespaceURI,
		String sName, // simple name
		String qName // qualified name
	) throws SAXException
	{
		if (qName.equalsIgnoreCase("waters"))
		{
			parsing = false;
		}
		if (qName.equalsIgnoreCase("water"))
		{
			current.setName(nameBuffer.toString());
			current.setDescription(descBuffer.toString());
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
				else if (currentElement.equalsIgnoreCase("calcium"))
				{
					current.setCalcium(new PpmUnit(Double.parseDouble(text)));
				}
				else if (currentElement.equalsIgnoreCase("bicarbonate"))
				{
					current.setBicarbonate(new PpmUnit(Double.parseDouble(text)));
				}
				else if (currentElement.equalsIgnoreCase("sulfate"))
				{
					current.setSulfate(new PpmUnit(Double.parseDouble(text)));
				}
				else if (currentElement.equalsIgnoreCase("chloride"))
				{
					current.setChloride(new PpmUnit(Double.parseDouble(text)));
				}
				else if (currentElement.equalsIgnoreCase("sodium"))
				{
					current.setSodium(new PpmUnit(Double.parseDouble(text)));
				}
				else if (currentElement.equalsIgnoreCase("magnesium"))
				{
					current.setMagnesium(new PpmUnit(Double.parseDouble(text)));
				}
				else if (currentElement.equalsIgnoreCase("ph"))
				{
					current.setPh(new PhUnit(Double.parseDouble(text)));
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
}
