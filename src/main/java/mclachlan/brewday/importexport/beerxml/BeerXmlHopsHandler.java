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
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.PercentageUnit;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class BeerXmlHopsHandler extends DefaultHandler implements V2DataObjectImporter<Hop>
{
	private String currentElement;
	private Hop current;
	private final List<Hop> hops = new ArrayList<>();
	private boolean parsing = false;

	private StringBuilder nameBuffer, descBuffer, originBuffer, subsBuffer;

	/*-------------------------------------------------------------------------*/

	public List<Hop> getResult()
	{
		return hops;
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

		if (eName.equalsIgnoreCase("hops"))
		{
			parsing = true;
		}
		if (eName.equalsIgnoreCase("hop"))
		{
			current = new Hop();
			nameBuffer = new StringBuilder();
			descBuffer = new StringBuilder();
			originBuffer = new StringBuilder();
			subsBuffer = new StringBuilder();
		}
	}

	public void endElement(
		String namespaceURI,
		String sName, // simple name
		String qName // qualified name
	) throws SAXException
	{
		if (qName.equalsIgnoreCase("hops"))
		{
			parsing = false;
		}
		if (qName.equalsIgnoreCase("hop"))
		{
			current.setName(nameBuffer.toString());
			current.setDescription(descBuffer.toString());
			current.setOrigin(originBuffer.toString());
			current.setSubstitutes(subsBuffer.toString());
			hops.add(current);
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
				if (currentElement.equalsIgnoreCase("notes"))
				{
					descBuffer.append(text);
				}
				if (currentElement.equalsIgnoreCase("name"))
				{
					nameBuffer.append(text);
				}
				if (currentElement.equalsIgnoreCase("substitutes"))
				{
					subsBuffer.append(text);
				}
				if (currentElement.equalsIgnoreCase("origin"))
				{
					originBuffer.append(text);
				}
				if (currentElement.equalsIgnoreCase("amount"))
				{
					// ignore for db import
//				double weightKg = Double.parseDouble(s.trim());
				}
				if (currentElement.equalsIgnoreCase("form"))
				{
					// ignore for db import
//				h.setType(s.trim());
				}
				if (currentElement.equalsIgnoreCase("hsi"))
				{
					current.setHopStorageIndex(new PercentageUnit(Double.parseDouble(text)/100D));
				}
				if (currentElement.equalsIgnoreCase("alpha"))
				{
					double alphaAcidPerc = Double.parseDouble(text);
					current.setAlphaAcid(new PercentageUnit(alphaAcidPerc / 100D));
				}
				if (currentElement.equalsIgnoreCase("beta"))
				{
					double betaAcidPerc = Double.parseDouble(text);
					current.setBetaAcid(new PercentageUnit(betaAcidPerc / 100D));
				}
				if (currentElement.equalsIgnoreCase("humulene"))
				{
					double humulenePerc = Double.parseDouble(text);
					current.setHumulene(new PercentageUnit(humulenePerc / 100D));
				}
				if (currentElement.equalsIgnoreCase("caryophyllene"))
				{
					double caryophyllenePerc = Double.parseDouble(text);
					current.setCaryophyllene(new PercentageUnit(caryophyllenePerc / 100D));
				}
				if (currentElement.equalsIgnoreCase("cohumulone"))
				{
					double cohumulonePerc = Double.parseDouble(text);
					current.setCohumulone(new PercentageUnit(cohumulonePerc / 100D));
				}
				if (currentElement.equalsIgnoreCase("myrcene"))
				{
					double myrcenePerc = Double.parseDouble(text);
					current.setMyrcene(new PercentageUnit(myrcenePerc / 100D));
				}
				if (currentElement.equalsIgnoreCase("time"))
				{
					// ignore for DB import
//					h.setMinutes(new Double(s.trim()).intValue());
				}
				if (currentElement.equalsIgnoreCase("type"))
				{
					Hop.Type type = hopTypeFromBeerXml(text);
					current.setType(type);
				}
			}
		}
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
	private Hop.Type hopTypeFromBeerXml(String s)
	{
		if (s.equalsIgnoreCase("bittering"))
		{
			return Hop.Type.BITTERING;
		}
		else if (s.equalsIgnoreCase("aroma"))
		{
			return Hop.Type.AROMA;
		}
		else if (s.equalsIgnoreCase("both"))
		{
			return Hop.Type.BOTH;
		}
		else
		{
			throw new BrewdayException("invalid BeerXML: [" + s + "]");
		}
	}
}
