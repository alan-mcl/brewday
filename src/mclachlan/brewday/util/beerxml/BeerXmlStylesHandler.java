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

package mclachlan.brewday.util.beerxml;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.style.Style;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class BeerXmlStylesHandler extends DefaultHandler
{
	private String currentElement;
	private Style current;
	private List<Style> result;

	/*-------------------------------------------------------------------------*/

	public List<Style> getResult()
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

		if (eName.equalsIgnoreCase("styles"))
		{
			result = new ArrayList<Style>();
		}
		if (eName.equalsIgnoreCase("style"))
		{
			current = new Style();
			current.setProfile("");
			current.setIngredients("");
			current.setExamples("");
		}
	}

	public void endElement(
		String namespaceURI,
		String sName, // simple name
		String qName // qualified name
	) throws SAXException
	{
		if (qName.equalsIgnoreCase("styles"))
		{

		}
		if (qName.equalsIgnoreCase("style"))
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
			if (currentElement.equalsIgnoreCase("name"))
			{
				current.setName(text);
			}
			if (currentElement.equalsIgnoreCase("category"))
			{
				current.setCategory(text);
			}
			if (currentElement.equalsIgnoreCase("category_number"))
			{
				current.setCategoryNumber(text);
			}
			if (currentElement.equalsIgnoreCase("style_letter"))
			{
				current.setStyleLetter(text);
			}
			if (currentElement.equalsIgnoreCase("style_guide"))
			{
				current.setStyleGuide(text);
			}
			if (currentElement.equalsIgnoreCase("type"))
			{
				current.setType(styleTypeFromBeerXml(text));
			}
			if (currentElement.equalsIgnoreCase("og_min"))
			{
				current.setOgMin(Double.parseDouble(text));
			}
			if (currentElement.equalsIgnoreCase("og_max"))
			{
				current.setOgMax(Double.parseDouble(text));
			}
			if (currentElement.equalsIgnoreCase("fg_min"))
			{
				current.setFgMin(Double.parseDouble(text));
			}
			if (currentElement.equalsIgnoreCase("fg_max"))
			{
				current.setFgMax(Double.parseDouble(text));
			}
			if (currentElement.equalsIgnoreCase("ibu_min"))
			{
				current.setIbuMin((int)Double.parseDouble(text));
			}
			if (currentElement.equalsIgnoreCase("ibu_max"))
			{
				current.setIbuMax((int)Double.parseDouble(text));
			}
			if (currentElement.equalsIgnoreCase("color_min"))
			{
				current.setColourMin((int)Double.parseDouble(text));
			}
			if (currentElement.equalsIgnoreCase("color_max"))
			{
				current.setColourMax((int)Double.parseDouble(text));
			}
			if (currentElement.equalsIgnoreCase("carb_min"))
			{
				current.setCarbMin(Double.parseDouble(text));
			}
			if (currentElement.equalsIgnoreCase("carb_max"))
			{
				current.setCarbMax(Double.parseDouble(text));
			}
			if (currentElement.equalsIgnoreCase("abv_min"))
			{
				current.setAbvMin(getPercentage(text));
			}
			if (currentElement.equalsIgnoreCase("abv_max"))
			{
				current.setAbvMax(getPercentage(text));
			}
			if (currentElement.equalsIgnoreCase("profile"))
			{
				current.setProfile(current.getProfile()+text);
			}
			if (currentElement.equalsIgnoreCase("ingredients"))
			{
				current.setIngredients(current.getIngredients()+text);
			}
			if (currentElement.equalsIgnoreCase("examples"))
			{
				current.setExamples(current.getExamples()+text);
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
	private Style.Type styleTypeFromBeerXml(String s)
	{
		if (s.equalsIgnoreCase("lager"))
		{
			return Style.Type.LAGER;
		}
		else if (s.equalsIgnoreCase("ale"))
		{
			return Style.Type.ALE;
		}
		else if (s.equalsIgnoreCase("mead"))
		{
			return Style.Type.MEAD;
		}
		else if (s.equalsIgnoreCase("wheat"))
		{
			return Style.Type.WHEAT;
		}
		else if (s.equalsIgnoreCase("mixed"))
		{
			return Style.Type.MIXED;
		}
		else if (s.equalsIgnoreCase("cider"))
		{
			return Style.Type.CIDER;
		}
		else
		{
			throw new BrewdayException("invalid BeerXML: ["+s+"]");
		}
	}

}
