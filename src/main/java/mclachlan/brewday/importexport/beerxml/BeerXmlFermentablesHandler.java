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
import mclachlan.brewday.ingredients.Fermentable;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.DiastaticPowerUnit;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.util.Log;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class BeerXmlFermentablesHandler extends DefaultHandler implements V2DataObjectImporter<Fermentable>
{
	private String currentElement;
	private Fermentable current;
	private final List<Fermentable> result = new ArrayList<>();
	private boolean parsing = false;

	private StringBuilder nameBuffer, descBuffer, colourBuffer;

	private boolean fixBeerSmithBugs;

	/*-------------------------------------------------------------------------*/
	public BeerXmlFermentablesHandler(boolean fixBeerSmithBugs)
	{
		this.fixBeerSmithBugs = fixBeerSmithBugs;
	}

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
			parsing = true;
		}
		if (eName.equalsIgnoreCase("fermentable"))
		{
			current = new Fermentable();
			nameBuffer = new StringBuilder();
			descBuffer = new StringBuilder();
			colourBuffer = new StringBuilder();
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
			parsing = false;
		}
		if (qName.equalsIgnoreCase("fermentable"))
		{
			current.setName(nameBuffer.toString());
			current.setDescription(descBuffer.toString());

			Quantity.Unit colourUnit;
			// BeerSmith just exports all its shit as SRM, nvm the spec
			if (fixBeerSmithBugs ||
				current.getType() == Fermentable.Type.LIQUID_EXTRACT ||
				current.getType() == Fermentable.Type.JUICE)
			{
				// BeerXML only specifies SRM for LME, Lovibond for the rest
				// but hey surely JUICE is in SRM too

				colourUnit = Quantity.Unit.SRM;
			}
			else
			{
				colourUnit = Quantity.Unit.LOVIBOND;
			}

			current.setColour(new ColourUnit(
				Double.parseDouble(colourBuffer.toString()),
				colourUnit,
				true));

			// BeerSmith has a "honey" type, but exports an empty Type element for it!
			if (current.getType() == null)
			{
				if (current.getName().toLowerCase().contains("honey"))
				{
					current.setType(Fermentable.Type.HONEY);
				}
				else
				{
					// need some kind of type, default to this
					current.setType(Fermentable.Type.ADJUNCT);
				}
			}

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
				if (currentElement.equalsIgnoreCase("notes"))
				{
					descBuffer.append(text);
				}
				if (currentElement.equalsIgnoreCase("name"))
				{
					nameBuffer.append(text);
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
					colourBuffer.append(text);
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
		Brewday.getInstance().getLog().log(Log.MEDIUM, x);
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
		else if (s.equalsIgnoreCase("honey"))
		{
			return Fermentable.Type.HONEY;
		}
		else
		{
			throw new BrewdayException("invalid BeerXML: ["+s+"]");
		}
	}

}
