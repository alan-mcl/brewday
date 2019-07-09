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
import mclachlan.brewday.equipment.EquipmentProfile;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class BeerXmlEquipmentsHandler extends DefaultHandler
{
	private String currentElement;
	private EquipmentProfile current;
	private List<EquipmentProfile> result;

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
			result = new ArrayList<EquipmentProfile>();
		}
		if (eName.equalsIgnoreCase("equipment"))
		{
			current = new EquipmentProfile();
			current.setDescription("");
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

		}
		if (qName.equalsIgnoreCase("equipment"))
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
			else if (currentElement.equalsIgnoreCase("notes"))
			{
				current.setDescription(current.getDescription()+text);
			}
			else if (currentElement.equalsIgnoreCase("boil_size"))
			{
				// assume beer xml "boil volume" + 20% as the kettle size
				current.setBoilKettleVolume(Double.parseDouble(text)*1.2 *1000);
			}
			else if (currentElement.equalsIgnoreCase("batch_size"))
			{
				// assume beer xml "batch size" + 20% as the fermenter size
				current.setFermenterVolume(Double.parseDouble(text)*1.2 *1000);
			}
			else if (currentElement.equalsIgnoreCase("tun_volume"))
			{
				// convert l to ml
				current.setMashTunVolume(Double.parseDouble(text)*1000);
			}
			else if (currentElement.equalsIgnoreCase("tun_weight"))
			{
				// convert kg to g
				current.setMashTunWeight(Double.parseDouble(text)*1000);
			}
			else if (currentElement.equalsIgnoreCase("tun_specific_heat"))
			{
				current.setMashTunSpecificHeat(Double.parseDouble(text));
			}
			else if (currentElement.equalsIgnoreCase("trub_chiller_loss"))
			{
				// convert l to ml
				current.setTrubAndChillerLoss(Double.parseDouble(text)*1000);
			}
			else if (currentElement.equalsIgnoreCase("evap_rate"))
			{
				// convert to %
				current.setBoilEvapourationRate(Double.parseDouble(text)/100D);
			}
			else if (currentElement.equalsIgnoreCase("lauter_deadspace"))
			{
				// convert l to ml
				current.setLauterLoss(Double.parseDouble(text)*1000);
			}
			else if (currentElement.equalsIgnoreCase("hop_utilization"))
			{
				// convert to %
				current.setHopUtilisation(Double.parseDouble(text)/100D);
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
}
