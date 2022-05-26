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
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.util.Log;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class BeerXmlEquipmentsHandler extends DefaultHandler implements V2DataObjectImporter<EquipmentProfile>
{
	private String currentElement;
	private EquipmentProfile current;
	private final List<EquipmentProfile> result = new ArrayList<>();
	private boolean parsing = false;

	private StringBuilder nameBuffer, descBuffer;

	/*-------------------------------------------------------------------------*/

	public List<EquipmentProfile> getResult()
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

		if (eName.equalsIgnoreCase("equipments"))
		{
			parsing = true;
		}
		if (eName.equalsIgnoreCase("equipment"))
		{
			parsing = true;
			current = new EquipmentProfile();
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
		if (qName.equalsIgnoreCase("equipments"))
		{
			parsing = false;
		}
		if (qName.equalsIgnoreCase("equipment"))
		{
			parsing = false;
			current.setName(nameBuffer.toString());
			current.setDescription(descBuffer.toString());

			// BeerXML (well, at least BeerSmith) assumes 100% conversion efficiency
			current.setConversionEfficiency(new PercentageUnit(1D));

			// BeerXML does not have these elements, so we set some defaults
			// These rather odd values are to help me spot when they are set here
			current.setBoilElementPower(new PowerUnit(4.56D, Quantity.Unit.KILOWATT, true));

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
				else if (currentElement.equalsIgnoreCase("boil_size"))
				{
					// assume beer xml "boil volume" + 20% as the kettle size
					current.setBoilKettleVolume(new VolumeUnit(Double.parseDouble(text) * 1.2, Quantity.Unit.LITRES));
				}
				else if (currentElement.equalsIgnoreCase("batch_size"))
				{
					// assume beer xml "batch size" + 20% as the fermenter size
					current.setFermenterVolume(new VolumeUnit(Double.parseDouble(text) * 1.2, Quantity.Unit.LITRES));

					current.setBatchSize(new VolumeUnit(Double.parseDouble(text), Quantity.Unit.LITRES));
				}
				else if (currentElement.equalsIgnoreCase("tun_volume"))
				{
					// convert l to ml
					current.setMashTunVolume(getVolume(text));
				}
				else if (currentElement.equalsIgnoreCase("tun_weight"))
				{
					// convert kg to g
					current.setMashTunWeight(getWeight(text));
				}
				else if (currentElement.equalsIgnoreCase("tun_specific_heat"))
				{
					current.setMashTunSpecificHeat(new ArbitraryPhysicalQuantity(Double.parseDouble(text), Quantity.Unit.JOULE_PER_KG_CELSIUS));
				}
				else if (currentElement.equalsIgnoreCase("top_up_water"))
				{
					// water added prior to fermentation
					current.setTopUpWater(Double.parseDouble(text));
				}
				else if (currentElement.equalsIgnoreCase("top_up_kettle"))
				{
					// top up water into the boil kettle before boiling
					current.setTopUpKettle(Double.parseDouble(text));
				}
				else if (currentElement.equalsIgnoreCase("trub_chiller_loss"))
				{
					// convert l to ml
					current.setTrubAndChillerLoss(getVolume(text));
				}
				else if (currentElement.equalsIgnoreCase("evap_rate"))
				{
					// convert to %
					current.setBoilEvapourationRate(getPercentage(text));
				}
				else if (currentElement.equalsIgnoreCase("lauter_deadspace"))
				{
					// convert l to ml
					current.setLauterLoss(getVolume(text));
				}
				else if (currentElement.equalsIgnoreCase("hop_utilization"))
				{
					// convert to %
					current.setHopUtilisation(getPercentage(text));
				}
			}
		}
	}

	protected WeightUnit getWeight(String text)
	{
		return new WeightUnit(Double.parseDouble(text), Quantity.Unit.KILOGRAMS);
	}

	protected PercentageUnit getPercentage(String text)
	{
		return new PercentageUnit(Double.parseDouble(text) / 100D);
	}

	protected VolumeUnit getVolume(String text)
	{
		return new VolumeUnit(Double.parseDouble(text), Quantity.Unit.LITRES);
	}

	public void ignorableWhitespace(char[] buf, int offset,
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
}
