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
public class BeerXmlHopsHandler extends DefaultHandler
{
	private String currentElement;
	private Hop h;
	private List<Hop> hops;

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
			hops = new ArrayList<Hop>();
		}
		if (eName.equalsIgnoreCase("hop"))
		{
			h = new Hop();
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

		}
		if (qName.equalsIgnoreCase("hop"))
		{
			hops.add(h);
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
				h.setDescription(text);
			}
			if (currentElement.equalsIgnoreCase("name"))
			{
				h.setName(text);
			}
			if (currentElement.equalsIgnoreCase("substitutes"))
			{
				h.setSubstitutes(text);
			}
			if (currentElement.equalsIgnoreCase("origin"))
			{
				h.setOrigin(text);
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
				h.setHopStorageIndex(new PercentageUnit(Double.parseDouble(text)));
			}
			if (currentElement.equalsIgnoreCase("alpha"))
			{
				double alphaAcidPerc = Double.parseDouble(text);
				h.setAlphaAcid(new PercentageUnit(alphaAcidPerc / 100D));
			}
			if (currentElement.equalsIgnoreCase("beta"))
			{
				double betaAcidPerc = Double.parseDouble(text);
				h.setBetaAcid(new PercentageUnit(betaAcidPerc / 100D));
			}
			if (currentElement.equalsIgnoreCase("humulene"))
			{
				double humulenePerc = Double.parseDouble(text);
				h.setHumulene(new PercentageUnit(humulenePerc / 100D));
			}
			if (currentElement.equalsIgnoreCase("caryophyllene"))
			{
				double caryophyllenePerc = Double.parseDouble(text);
				h.setCaryophyllene(new PercentageUnit(caryophyllenePerc / 100D));
			}
			if (currentElement.equalsIgnoreCase("cohumulone"))
			{
				double cohumulonePerc = Double.parseDouble(text);
				h.setCohumulone(new PercentageUnit(cohumulonePerc / 100D));
			}
			if (currentElement.equalsIgnoreCase("myrcene"))
			{
				double myrcenePerc = Double.parseDouble(text);
				h.setMyrcene(new PercentageUnit(myrcenePerc / 100D));
			}
			if (currentElement.equalsIgnoreCase("time"))
			{
				// ignore for DB import
//				h.setMinutes(new Double(s.trim()).intValue());
			}
			if (currentElement.equalsIgnoreCase("type"))
			{
				Hop.Type type = hopTypeFromBeerXml(text);
				h.setType(type);
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
			throw new BrewdayException("invalid BeerXML: ["+s+"]");
		}
	}

}
