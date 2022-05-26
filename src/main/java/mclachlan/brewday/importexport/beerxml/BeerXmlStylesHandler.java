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
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.math.*;
import mclachlan.brewday.style.Style;
import mclachlan.brewday.util.Log;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class BeerXmlStylesHandler extends DefaultHandler implements V2DataObjectImporter<Style>
{
	private String currentElement;
	private Style current;
	private final List<Style> result = new ArrayList<>();
	private StringBuilder notesBuffer;
	private StringBuilder profileBuffer;
	private StringBuilder ingredientsBuffer;
	private StringBuilder examplesBuffer;
	private StringBuilder nameBuffer;
	private boolean parsing = false;

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
			parsing = true;
		}
		if (eName.equalsIgnoreCase("style"))
		{
			current = new Style();
			parsing = true;

			notesBuffer = new StringBuilder();
			profileBuffer = new StringBuilder();
			ingredientsBuffer = new StringBuilder();
			examplesBuffer = new StringBuilder();
			nameBuffer = new StringBuilder();

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
			parsing = false;
		}
		if (qName.equalsIgnoreCase("style"))
		{
			parsing = false;

			current.setNotes(notesBuffer.toString());
			current.setProfile(profileBuffer.toString());
			current.setIngredients(ingredientsBuffer.toString());
			current.setExamples(examplesBuffer.toString());
			current.setStyleGuideName(nameBuffer.toString());

			String s = current.getStyleLetter()==null?"":current.getStyleLetter();
			String uniqueName = current.getCategoryNumber()+s+"/"+current.getStyleGuideName()+"/"+current.getStyleGuide();
			String displayName = current.getCategoryNumber()+s+" "+current.getStyleGuideName();
			current.setName(uniqueName);
			current.setDisplayName(displayName);

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
					current.setOgMin(new DensityUnit(Double.parseDouble(text), DensityUnit.Unit.SPECIFIC_GRAVITY));
				}
				if (currentElement.equalsIgnoreCase("og_max"))
				{
					current.setOgMax(new DensityUnit(Double.parseDouble(text), DensityUnit.Unit.SPECIFIC_GRAVITY));
				}
				if (currentElement.equalsIgnoreCase("fg_min"))
				{
					current.setFgMin(new DensityUnit(Double.parseDouble(text), DensityUnit.Unit.SPECIFIC_GRAVITY));
				}
				if (currentElement.equalsIgnoreCase("fg_max"))
				{
					current.setFgMax(new DensityUnit(Double.parseDouble(text), DensityUnit.Unit.SPECIFIC_GRAVITY));
				}
				if (currentElement.equalsIgnoreCase("ibu_min"))
				{
					current.setIbuMin(new BitternessUnit(Double.parseDouble(text)));
				}
				if (currentElement.equalsIgnoreCase("ibu_max"))
				{
					current.setIbuMax(new BitternessUnit(Double.parseDouble(text)));
				}
				if (currentElement.equalsIgnoreCase("color_min"))
				{
					current.setColourMin(new ColourUnit(Double.parseDouble(text)));
				}
				if (currentElement.equalsIgnoreCase("color_max"))
				{
					current.setColourMax(new ColourUnit(Double.parseDouble(text)));
				}
				if (currentElement.equalsIgnoreCase("carb_min"))
				{
					current.setCarbMin(new CarbonationUnit(Double.parseDouble(text), Quantity.Unit.VOLUMES, false));
				}
				if (currentElement.equalsIgnoreCase("carb_max"))
				{
					current.setCarbMax(new CarbonationUnit(Double.parseDouble(text), Quantity.Unit.VOLUMES, false));
				}
				if (currentElement.equalsIgnoreCase("abv_min"))
				{
					current.setAbvMin(new PercentageUnit(getPercentage(text)));
				}
				if (currentElement.equalsIgnoreCase("abv_max"))
				{
					current.setAbvMax(new PercentageUnit(getPercentage(text)));
				}
				if (currentElement.equalsIgnoreCase("profile"))
				{
					profileBuffer.append(text);
				}
				if (currentElement.equalsIgnoreCase("ingredients"))
				{
					ingredientsBuffer.append(text);
				}
				if (currentElement.equalsIgnoreCase("examples"))
				{
					examplesBuffer.append(text);
				}
				if (currentElement.equalsIgnoreCase("notes"))
				{
					notesBuffer.append(text);
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
		Brewday.getInstance().getLog().log(Log.MEDIUM, x);
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
