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
import mclachlan.brewday.ingredients.Yeast;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.TemperatureUnit;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class BeerXmlYeastsHandler extends DefaultHandler implements V2DataObjectImporter<Yeast>
{
	private String currentElement;
	private Yeast current;
	private final List<Yeast> result = new ArrayList<>();
	private boolean parsing = false;

	private StringBuilder nameBuffer, descBuffer, recommendedBuffer;

	/*-------------------------------------------------------------------------*/

	public List<Yeast> getResult()
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

		if (eName.equalsIgnoreCase("yeasts"))
		{
			parsing = true;
		}
		if (eName.equalsIgnoreCase("yeast"))
		{
			current = new Yeast();
			nameBuffer = new StringBuilder();
			descBuffer = new StringBuilder();
			recommendedBuffer = new StringBuilder();
		}
	}

	public void endElement(
		String namespaceURI,
		String sName, // simple name
		String qName // qualified name
	) throws SAXException
	{
		if (qName.equalsIgnoreCase("yeasts"))
		{
			parsing = false;
		}
		if (qName.equalsIgnoreCase("yeast"))
		{
			current.setName(nameBuffer.toString());
			current.setDescription(descBuffer.toString());
			current.setRecommendedStyles(recommendedBuffer.toString());
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
				else if (currentElement.equalsIgnoreCase("type"))
				{
					Yeast.Type type = yeastTypeFromBeerXml(text);
					current.setType(type);
				}
				else if (currentElement.equalsIgnoreCase("form"))
				{
					Yeast.Form form = yeastFormFromBeerXml(text);
					current.setForm(form);
				}
				else if (currentElement.equalsIgnoreCase("laboratory"))
				{
					current.setLaboratory(text);
				}
				else if (currentElement.equalsIgnoreCase("product_id"))
				{
					current.setProductId(text);
				}
				else if (currentElement.equalsIgnoreCase("min_temperature"))
				{
					current.setMinTemp(new TemperatureUnit(Double.parseDouble(text)));
				}
				else if (currentElement.equalsIgnoreCase("max_temperature"))
				{
					current.setMaxTemp(new TemperatureUnit(Double.parseDouble(text)));
				}
				else if (currentElement.equalsIgnoreCase("flocculation"))
				{
					Yeast.Flocculation floc = yeastFlocFromBeerXml(text);
					current.setFlocculation(floc);
				}
				else if (currentElement.equalsIgnoreCase("attenuation"))
				{
					current.setAttenuation(new PercentageUnit(getPercentage(text)));
				}
				else if (currentElement.equalsIgnoreCase("best_for"))
				{
					recommendedBuffer.append(text);
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
	private Yeast.Type yeastTypeFromBeerXml(String s)
	{
		if (s.equalsIgnoreCase("ale"))
		{
			return Yeast.Type.ALE;
		}
		else if (s.equalsIgnoreCase("lager"))
		{
			return Yeast.Type.LAGER;
		}
		else if (s.equalsIgnoreCase("wheat"))
		{
			return Yeast.Type.WHEAT;
		}
		else if (s.equalsIgnoreCase("wine"))
		{
			return Yeast.Type.WINE;
		}
		else if (s.equalsIgnoreCase("champagne"))
		{
			return Yeast.Type.CHAMPAGNE;
		}
		else
		{
			throw new BrewdayException("invalid BeerXML: ["+s+"]");
		}
	}

	/*-------------------------------------------------------------------------*/
	private Yeast.Form yeastFormFromBeerXml(String s)
	{
		if (s.equalsIgnoreCase("liquid"))
		{
			return Yeast.Form.LIQUID;
		}
		else if (s.equalsIgnoreCase("dry"))
		{
			return Yeast.Form.DRY;
		}
		else if (s.equalsIgnoreCase("slant"))
		{
			return Yeast.Form.SLANT;
		}
		else if (s.equalsIgnoreCase("culture"))
		{
			return Yeast.Form.CULTURE;
		}
		else
		{
			throw new BrewdayException("invalid BeerXML: ["+s+"]");
		}
	}

	/*-------------------------------------------------------------------------*/
	private Yeast.Flocculation yeastFlocFromBeerXml(String s)
	{
		if (s.equalsIgnoreCase("low"))
		{
			return Yeast.Flocculation.LOW;
		}
		else if (s.equalsIgnoreCase("medium"))
		{
			return Yeast.Flocculation.MEDIUM;
		}
		else if (s.equalsIgnoreCase("high"))
		{
			return Yeast.Flocculation.HIGH;
		}
		else if (s.equalsIgnoreCase("very high"))
		{
			return Yeast.Flocculation.VERY_HIGH;
		}
		else
		{
			throw new BrewdayException("invalid BeerXML: ["+s+"]");
		}
	}

}
