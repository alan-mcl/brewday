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

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.style.Style;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 *
 */
public class BeerXmlRecipesHandler extends DefaultHandler implements V2DataObjectImporter<BeerXmlRecipe>
{
	private String currentElement;
	private BeerXmlRecipe current;
	private final List<BeerXmlRecipe> result = new ArrayList<>();
	private boolean parsingRecipes = false;

	private final RecipeHandler recipeHandler = new RecipeHandler();
	private DefaultHandler currentHandler;

	private StringBuilder nameBuffer, notesBuffer, brewerBuffer, asstBrewerBuffer,
		tasteNotesBuffer, primingSugarNameBuffer, carbonationUsedBuffer;
	private boolean fixBeerSmithBugs;

	public BeerXmlRecipesHandler(boolean fixBeerSmithBugs)
	{
		this.fixBeerSmithBugs = fixBeerSmithBugs;
	}

	/*-------------------------------------------------------------------------*/

	public List<BeerXmlRecipe> getResult()
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

		if (eName.equalsIgnoreCase("recipes"))
		{
			parsingRecipes = true;
		}
		else if (eName.equalsIgnoreCase("recipe"))
		{
			current = new BeerXmlRecipe();

			nameBuffer = new StringBuilder();
			notesBuffer = new StringBuilder();
			tasteNotesBuffer = new StringBuilder();
			brewerBuffer = new StringBuilder();
			asstBrewerBuffer = new StringBuilder();
			primingSugarNameBuffer = new StringBuilder();
			carbonationUsedBuffer = new StringBuilder();
		}
		else if (eName.equalsIgnoreCase("equipment"))
		{
			currentHandler = new BeerXmlEquipmentsHandler();

			// yes it's a hack
			currentHandler.startElement(namespaceURI, "equipments", "equipments", attrs);
			currentHandler.startElement(namespaceURI, "equipment", "equipment", attrs);
		}
		else if (eName.equalsIgnoreCase("style"))
		{
			currentHandler = new BeerXmlStylesHandler();

			// yes it's a hack
			currentHandler.startElement(namespaceURI, "styles", "styles", attrs);
			currentHandler.startElement(namespaceURI, "style", "style", attrs);
		}
		else if (eName.equalsIgnoreCase("mash"))
		{
			currentHandler = new MashProfileHandler();
			currentHandler.startElement(namespaceURI, lName, "mash", attrs);
		}
		else if (eName.equalsIgnoreCase("waters"))
		{
			currentHandler = new WaterAdditionHandler();
			currentHandler.startElement(namespaceURI, lName, qName, attrs);
		}
		else if (eName.equalsIgnoreCase("fermentables"))
		{
			currentHandler = new FermentableAdditionHandler();
			currentHandler.startElement(namespaceURI, lName, qName, attrs);
		}
		else if (eName.equalsIgnoreCase("hops"))
		{
			currentHandler = new HopAdditionHandler();
			currentHandler.startElement(namespaceURI, lName, qName, attrs);
		}
		else if (eName.equalsIgnoreCase("yeasts"))
		{
			currentHandler = new YeastAdditionHandler();
			currentHandler.startElement(namespaceURI, lName, qName, attrs);
		}
		else if (eName.equalsIgnoreCase("miscs"))
		{
			currentHandler = new MiscAdditionHandler();
			currentHandler.startElement(namespaceURI, lName, qName, attrs);
		}
		else if (currentHandler != null)
		{
			currentHandler.startElement(namespaceURI, lName, qName, attrs);
		}
	}

	public void endElement(
		String namespaceURI,
		String sName, // simple name
		String qName // qualified name
	) throws SAXException
	{
		if (qName.equalsIgnoreCase("recipes"))
		{
			parsingRecipes = false;
		}
		else if (qName.equalsIgnoreCase("recipe"))
		{
			current.setName(nameBuffer.toString());
			current.setNotes(notesBuffer.toString());
			current.setTasteNotes(tasteNotesBuffer.toString());
			current.setBrewer(brewerBuffer.toString());
			current.setAsstBrewer(asstBrewerBuffer.toString());
			current.setPrimingSugarName(primingSugarNameBuffer.toString());
			current.setCarbonationUsed(carbonationUsedBuffer.toString());

			result.add(current);
		}
		else if (qName.equalsIgnoreCase("style"))
		{
			currentHandler.endElement(namespaceURI, sName, qName);
			currentHandler.endElement(namespaceURI, "styles", "styles");

			if (parsingRecipes)
			{
				current.setStyle((Style)((V2DataObjectImporter)currentHandler).getResult().get(0));
			}
			currentHandler = null;
		}
		else if (qName.equalsIgnoreCase("equipment"))
		{
			currentHandler.endElement(namespaceURI, sName, qName);
			currentHandler.endElement(namespaceURI, "equipments", "equipments");

			if (parsingRecipes)
			{
				EquipmentProfile equipmentProfile = (EquipmentProfile)((V2DataObjectImporter)currentHandler).getResult().get(0);
				current.setEquipment(equipmentProfile);
			}
			currentHandler = null;
		}
		else if (qName.equalsIgnoreCase("mash"))
		{
			currentHandler.endElement(namespaceURI, sName, qName);

			if (parsingRecipes)
			{
				current.setMash(((MashProfileHandler)currentHandler).getResult());
			}
			currentHandler = null;
		}
		else if (qName.equalsIgnoreCase("waters"))
		{
			currentHandler.endElement(namespaceURI, sName, qName);
			if (parsingRecipes)
			{
				current.setWaters(((V2DataObjectImporter)currentHandler).getResult());
			}
			currentHandler = null;
		}
		else if (qName.equalsIgnoreCase("fermentables"))
		{
			currentHandler.endElement(namespaceURI, sName, qName);
			if (parsingRecipes)
			{
				current.setFermentables(((V2DataObjectImporter)currentHandler).getResult());
			}
			currentHandler = null;
		}
		else if (qName.equalsIgnoreCase("hops"))
		{
			currentHandler.endElement(namespaceURI, sName, qName);
			if (parsingRecipes)
			{
				current.setHops(((V2DataObjectImporter)currentHandler).getResult());
			}
			currentHandler = null;
		}
		else if (qName.equalsIgnoreCase("yeasts"))
		{
			currentHandler.endElement(namespaceURI, sName, qName);
			if (parsingRecipes)
			{
				current.setYeasts(((V2DataObjectImporter)currentHandler).getResult());
			}
			currentHandler = null;
		}
		else if (qName.equalsIgnoreCase("miscs"))
		{
			currentHandler.endElement(namespaceURI, sName, qName);
			if (parsingRecipes)
			{
				current.setMiscs(((V2DataObjectImporter)currentHandler).getResult());
			}
			currentHandler = null;
		}
		else if (currentHandler != null)
		{
			currentHandler.endElement(namespaceURI, sName, qName);
		}
	}

	public void characters(char[] buf, int offset, int len) throws SAXException
	{
		if (parsingRecipes)
		{
			String s = new String(buf, offset, len);

			String text = s.trim();

			if (!text.equals(""))
			{
				if (currentHandler != null)
				{
					currentHandler.characters(buf, offset, len);
				}
				else
				{
					recipeHandler.handleRecipe(text);
				}
			}
		}
	}

	class RecipeHandler
	{
		protected void handleRecipe(String text)
		{
			if (currentElement.equalsIgnoreCase("name"))
			{
				nameBuffer.append(text);
			}
			else if (currentElement.equalsIgnoreCase("version"))
			{
				// ignore
			}
			else if (currentElement.equalsIgnoreCase("type"))
			{
				current.setType(recipeTypeFromText(text));
			}
			else if (currentElement.equalsIgnoreCase("style"))
			{
				// todo
			}
			else if (currentElement.equalsIgnoreCase("equipment"))
			{
				// todo
			}
			else if (currentElement.equalsIgnoreCase("brewer"))
			{
				brewerBuffer.append(text);
			}
			else if (currentElement.equalsIgnoreCase("asst_brewer"))
			{
				asstBrewerBuffer.append(text);
			}
			else if (currentElement.equalsIgnoreCase("batch_size"))
			{
				current.setBatchSize(getVolume(text));
			}
			else if (currentElement.equalsIgnoreCase("boil_size"))
			{
				current.setBoilSize(getVolume(text));
			}
			else if (currentElement.equalsIgnoreCase("boil_time"))
			{
				current.setBoilTime(getTimeUnit(text, Quantity.Unit.MINUTES));
			}
			else if (currentElement.equalsIgnoreCase("efficiency"))
			{
				current.setEfficiency(getPercentage(text));
			}
			else if (currentElement.equalsIgnoreCase("hops"))
			{
				// todo
			}
			else if (currentElement.equalsIgnoreCase("fermentables"))
			{
				// todo
			}
			else if (currentElement.equalsIgnoreCase("miscs"))
			{
				// todo
			}
			else if (currentElement.equalsIgnoreCase("yeasts"))
			{
				// todo
			}
			else if (currentElement.equalsIgnoreCase("waters"))
			{
				// todo
			}
			else if (currentElement.equalsIgnoreCase("mash"))
			{
				// todo
			}
			else if (currentElement.equalsIgnoreCase("notes"))
			{
				notesBuffer.append(text);
			}
			else if (currentElement.equalsIgnoreCase("taste_notes"))
			{
				tasteNotesBuffer.append(text);
			}
			else if (currentElement.equalsIgnoreCase("taste_rating"))
			{
				current.setTasteRating(Double.parseDouble(text));
			}
			else if (currentElement.equalsIgnoreCase("og"))
			{
				current.setOg(getDensity(text));
			}
			else if (currentElement.equalsIgnoreCase("fg"))
			{
				current.setFg(getDensity(text));
			}
			else if (currentElement.equalsIgnoreCase("fermentation_stages"))
			{
				current.setFermentationStages(Integer.parseInt(text));
			}
			else if (currentElement.equalsIgnoreCase("primary_age"))
			{
				current.setPrimaryAge(getTimeUnit(text, DAYS));
			}
			else if (currentElement.equalsIgnoreCase("primary_temp"))
			{
				current.setPrimaryTemp(getTemperature(text));
			}
			else if (currentElement.equalsIgnoreCase("secondary_age"))
			{
				current.setSecondaryAge(getTimeUnit(text, DAYS));
			}
			else if (currentElement.equalsIgnoreCase("secondary_temp"))
			{
				current.setSecondaryTemp(getTemperature(text));
			}
			else if (currentElement.equalsIgnoreCase("tertiary_age"))
			{
				current.setTertiaryAge(getTimeUnit(text, DAYS));

				if (fixBeerSmithBugs)
				{
					// BeerSmith seems to have a bug and does not export tertiary_temp
					// http://www.beersmith.com/forum/index.php/topic,21603.0.html

					if (current.getSecondaryTemp() != null)
					{
						current.setTertiaryTemp(new TemperatureUnit(current.getSecondaryTemp()));
					}
					else
					{
						// who knows? just set something
						current.setTertiaryTemp(new TemperatureUnit(20, CELSIUS));
					}
				}
			}
			else if (currentElement.equalsIgnoreCase("tertiary_temp"))
			{
				current.setTertiaryTemp(getTemperature(text));
			}
			else if (currentElement.equalsIgnoreCase("age"))
			{
				current.setAge(getTimeUnit(text, DAYS));
			}
			else if (currentElement.equalsIgnoreCase("age_temp"))
			{
				current.setAgeTemp(getTemperature(text));
			}
			else if (currentElement.equalsIgnoreCase("date"))
			{
				// todo, more robust date parsing. BeerSmith seems to export the below format
				DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");
				current.setDate(LocalDate.parse(text, dtf));
			}
			else if (currentElement.equalsIgnoreCase("carbonation"))
			{
				current.setCarbonation(new CarbonationUnit(Double.parseDouble(text), VOLUMES, false));
			}
			else if (currentElement.equalsIgnoreCase("carbonation_used"))
			{
				carbonationUsedBuffer.append(text);
			}
			else if (currentElement.equalsIgnoreCase("forced_carbonation"))
			{
				boolean b = Boolean.parseBoolean(text);

				if (fixBeerSmithBugs)
				{
					// BeerSmith flips these: http://www.beersmith.com/forum/index.php/topic,21604.0.html
					current.setForcedCarbonation(!b);
				}
				else
				{
					current.setForcedCarbonation(b);
				}
			}
			else if (currentElement.equalsIgnoreCase("priming_sugar_name"))
			{
				primingSugarNameBuffer.append(text);
			}
			else if (currentElement.equalsIgnoreCase("carbonation_temp"))
			{
				current.setCarbonationTemp(getTemperature(text));
			}
			else if (currentElement.equalsIgnoreCase("priming_sugar_equiv"))
			{
				current.setPrimingSugarEquiv(Double.parseDouble(text));
			}
			else if (currentElement.equalsIgnoreCase("keg_priming_factor"))
			{
				current.setKegPrimingFactor(Double.parseDouble(text));
			}
		}
	}

	static class MashProfileHandler extends DefaultHandler
	{
		private String currentElement;
		private BeerXmlMashProfile current;
		private BeerXmlMashStep currentStep;
		private List<BeerXmlMashStep> steps = new ArrayList<>();
		private boolean parsing = false;

		private StringBuilder nameBuffer, notesBuffer, mashStepNameBuffer;

		/*-------------------------------------------------------------------------*/

		public BeerXmlMashProfile getResult()
		{
			return current;
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

			if (eName.equalsIgnoreCase("mash"))
			{
				current = new BeerXmlMashProfile();

				nameBuffer = new StringBuilder();
				notesBuffer = new StringBuilder();

				parsing = true;
			}
			else if (eName.equalsIgnoreCase("mash_step"))
			{
				currentStep = new BeerXmlMashStep();
				mashStepNameBuffer = new StringBuilder();
			}
		}

		public void endElement(
			String namespaceURI,
			String sName, // simple name
			String qName // qualified name
		) throws SAXException
		{
			if (qName.equalsIgnoreCase("mash"))
			{
				current.setName(nameBuffer.toString());
				current.setNotes(notesBuffer.toString());
				current.setMashSteps(steps);

				parsing = false;
			}
			else if (qName.equalsIgnoreCase("mash_step"))
			{
				currentStep.setName(mashStepNameBuffer.toString());
				steps.add(currentStep);
				currentStep = null;
				mashStepNameBuffer = null;
			}
		}

		public void characters(char[] buf, int offset,
			int len) throws SAXException
		{
			if (parsing)
			{
				String s = new String(buf, offset, len);

				String text = s.trim();

				if (!text.equals(""))
				{
					if (currentElement.equalsIgnoreCase("name"))
					{
						if (currentStep == null)
						{
							nameBuffer.append(text);
						}
						else
						{
							mashStepNameBuffer.append(text);
						}
					}
					else if (currentElement.equalsIgnoreCase("grain_temp"))
					{
						current.setGrainTemp(getTemperature(text));
					}
					else if (currentElement.equalsIgnoreCase("mash_steps"))
					{
						// do nothing
					}
					else if (currentElement.equalsIgnoreCase("notes"))
					{
						notesBuffer.append(text);
					}
					else if (currentElement.equalsIgnoreCase("tun_temp"))
					{
						current.setTunTemp(getTemperature(text));
					}
					else if (currentElement.equalsIgnoreCase("sparge_temp"))
					{
						current.setSpargeTemp(getTemperature(text));
					}
					else if (currentElement.equalsIgnoreCase("ph"))
					{
						current.setPh(new PhUnit(Double.parseDouble(text)));
					}
					else if (currentElement.equalsIgnoreCase("tun_weight"))
					{
						current.setTunWeight(getWeight(text));
					}
					else if (currentElement.equalsIgnoreCase("tun_specific_heat"))
					{
						current.setTunSpecificHeat(new ArbitraryPhysicalQuantity(Double.parseDouble(text), JOULE_PER_KG_CELSIUS));
					}
					else if (currentElement.equalsIgnoreCase("equip_adjust"))
					{
						current.setEquipAdjust(Boolean.parseBoolean(text));
					}
					// mash step elements
					else if (currentElement.equalsIgnoreCase("type"))
					{
						if ("infusion".equalsIgnoreCase(text))
						{
							currentStep.setType(BeerXmlMashStep.MashStepType.INFUSION);
						}
						else if ("temperature".equalsIgnoreCase(text))
						{
							currentStep.setType(BeerXmlMashStep.MashStepType.TEMPERATURE);
						}
						else if ("decoction".equalsIgnoreCase(text))
						{
							currentStep.setType(BeerXmlMashStep.MashStepType.DECOCTION);
						}
						else
						{
							throw new BrewdayException("invalid: " + text);
						}
					}
					else if (currentElement.equalsIgnoreCase("infuse_amount"))
					{
						currentStep.setInfuseAmount(getVolume(text));
					}
					else if (currentElement.equalsIgnoreCase("step_temp"))
					{
						currentStep.setStepTemp(getTemperature(text));
					}
					else if (currentElement.equalsIgnoreCase("step_time"))
					{
						currentStep.setStepTime(getTimeUnit(text, MINUTES));
					}
					else if (currentElement.equalsIgnoreCase("ramp_time"))
					{
						currentStep.setRampTime(getTimeUnit(text, MINUTES));
					}
					else if (currentElement.equalsIgnoreCase("end_temp"))
					{
						currentStep.setEndTemp(getTemperature(text));
					}
				}
			}
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
			x.printStackTrace();
		}
	}

	static class MiscAdditionHandler extends DefaultHandler implements V2DataObjectImporter<MiscAddition>
	{
		private String currentElement;
		private MiscAddition current;
		private Misc currentMisc;
		private List<MiscAddition> result = new ArrayList<>();
		private boolean parsing = false;
		private boolean amountIsWeight;

		private StringBuilder nameBuffer, descBuffer, usageBuffer;

		/*-------------------------------------------------------------------------*/

		public List<MiscAddition> getResult()
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
				current = new MiscAddition();
				currentMisc = new Misc();
				amountIsWeight = false;

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
				currentMisc.setName(nameBuffer.toString());
				currentMisc.setDescription(descBuffer.toString());
				currentMisc.setUsageRecommendation(usageBuffer.toString());

				if (amountIsWeight)
				{
					current.setUnit(GRAMS);
				}
				else
				{
					current.setUnit(MILLILITRES);
				}

				current.setMisc(currentMisc);

				result.add(current);
			}
		}

		public void characters(char[] buf, int offset,
			int len) throws SAXException
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
						currentMisc.setType(type);
					}
					else if (currentElement.equalsIgnoreCase("use"))
					{
						Misc.Use type = miscUseFromBeerXml(text);
						currentMisc.setUse(type);
					}
					else if (currentElement.equalsIgnoreCase("time"))
					{
						current.setTime(new TimeUnit(Double.parseDouble(text), MINUTES, false));
					}
					else if (currentElement.equalsIgnoreCase("amount"))
					{
						if (!amountIsWeight)
						{
							current.setQuantity(new VolumeUnit(Double.parseDouble(text), LITRES));
						}
						else
						{
							current.setQuantity(new WeightUnit(Double.parseDouble(text), KILOGRAMS));
						}
					}
					else if (currentElement.equalsIgnoreCase("amount_is_weight"))
					{
						amountIsWeight = Boolean.parseBoolean(text);

						if (current.getQuantity() != null)
						{
							// we already set litres, reset to kilos
							current.setQuantity(new WeightUnit(current.getQuantity().get(LITRES), KILOGRAMS));
						}
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
				throw new BrewdayException("invalid BeerXML: [" + s + "]");
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
				throw new BrewdayException("invalid BeerXML: [" + s + "]");
			}
		}
	}


	static class YeastAdditionHandler extends DefaultHandler implements V2DataObjectImporter<YeastAddition>
	{
		private String currentElement;
		private YeastAddition current;
		private Yeast currentYeast;
		private final List<YeastAddition> result = new ArrayList<>();
		private boolean parsing = false;

		private boolean amountIsWeight = false;

		private StringBuilder nameBuffer, descBuffer, recommendedBuffer;

		/*-------------------------------------------------------------------------*/

		public List<YeastAddition> getResult()
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
				current = new YeastAddition();
				currentYeast = new Yeast();
				amountIsWeight = false;

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
				currentYeast.setName(nameBuffer.toString());
				currentYeast.setDescription(descBuffer.toString());
				currentYeast.setRecommendedStyles(recommendedBuffer.toString());

				if (amountIsWeight)
				{
					current.setUnit(GRAMS);
				}
				else
				{
					current.setUnit(MILLILITRES);
				}

				current.setYeast(currentYeast);

				result.add(current);
			}
		}

		public void characters(char[] buf, int offset,
			int len) throws SAXException
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
						currentYeast.setType(type);
					}
					else if (currentElement.equalsIgnoreCase("form"))
					{
						Yeast.Form form = yeastFormFromBeerXml(text);
						currentYeast.setForm(form);
					}
					else if (currentElement.equalsIgnoreCase("laboratory"))
					{
						currentYeast.setLaboratory(text);
					}
					else if (currentElement.equalsIgnoreCase("product_id"))
					{
						currentYeast.setProductId(text);
					}
					else if (currentElement.equalsIgnoreCase("min_temperature"))
					{
						currentYeast.setMinTemp(new TemperatureUnit(Double.parseDouble(text)));
					}
					else if (currentElement.equalsIgnoreCase("max_temperature"))
					{
						currentYeast.setMaxTemp(new TemperatureUnit(Double.parseDouble(text)));
					}
					else if (currentElement.equalsIgnoreCase("flocculation"))
					{
						Yeast.Flocculation floc = yeastFlocFromBeerXml(text);
						currentYeast.setFlocculation(floc);
					}
					else if (currentElement.equalsIgnoreCase("attenuation"))
					{
						currentYeast.setAttenuation(new PercentageUnit(getPercentage(text)));
					}
					else if (currentElement.equalsIgnoreCase("best_for"))
					{
						recommendedBuffer.append(text);
					}
					else if (currentElement.equalsIgnoreCase("amount"))
					{
						if (!amountIsWeight)
						{
							current.setQuantity(new VolumeUnit(Double.parseDouble(text), LITRES));
						}
						else
						{
							current.setQuantity(new WeightUnit(Double.parseDouble(text), KILOGRAMS));
						}
					}
					else if (currentElement.equalsIgnoreCase("amount_is_weight"))
					{
						amountIsWeight = Boolean.parseBoolean(text);

						if (amountIsWeight && current.getQuantity() != null)
						{
							// we already set litres, reset to kilos
							current.setQuantity(new WeightUnit(current.getQuantity().get(LITRES), KILOGRAMS));
						}
					}
					else if (currentElement.equalsIgnoreCase("add_to_secondary"))
					{
						current.setAddToSecondary(Boolean.parseBoolean(text));
					}
					else if (currentElement.equalsIgnoreCase("times_cultured"))
					{
						// todo
					}
					else if (currentElement.equalsIgnoreCase("max_reuse"))
					{
						// todo
					}
				}
			}
		}

		protected double getPercentage(String text)
		{
			return Double.parseDouble(text) / 100D;
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
				throw new BrewdayException("invalid BeerXML: [" + s + "]");
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
				throw new BrewdayException("invalid BeerXML: [" + s + "]");
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
				throw new BrewdayException("invalid BeerXML: [" + s + "]");
			}
		}

	}


	static class HopAdditionHandler extends DefaultHandler implements V2DataObjectImporter<HopAddition>
	{
		private String currentElement;
		private HopAddition current;
		private Hop currentHop;
		private final List<HopAddition> result = new ArrayList<>();
		private boolean parsing = false;

		private StringBuilder nameBuffer, descBuffer, originBuffer, subsBuffer;

		/*-------------------------------------------------------------------------*/

		public List<HopAddition> getResult()
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

			if (eName.equalsIgnoreCase("hops"))
			{
				parsing = true;
			}
			if (eName.equalsIgnoreCase("hop"))
			{
				current = new HopAddition();
				currentHop = new Hop();

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
				currentHop.setName(nameBuffer.toString());
				currentHop.setDescription(descBuffer.toString());
				currentHop.setOrigin(originBuffer.toString());
				currentHop.setSubstitutes(subsBuffer.toString());

				current.setUnit(GRAMS);

				current.setHop(currentHop);

				result.add(current);
			}
		}

		public void characters(char[] buf, int offset,
			int len) throws SAXException
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
					else if (currentElement.equalsIgnoreCase("name"))
					{
						nameBuffer.append(text);
					}
					else if (currentElement.equalsIgnoreCase("substitutes"))
					{
						subsBuffer.append(text);
					}
					else if (currentElement.equalsIgnoreCase("origin"))
					{
						originBuffer.append(text);
					}
					else if (currentElement.equalsIgnoreCase("amount"))
					{
						current.setQuantity(new WeightUnit(Double.parseDouble(s.trim()), KILOGRAMS));
					}
					else if (currentElement.equalsIgnoreCase("form"))
					{
						HopAddition.Form form;
						if ("pellet".equalsIgnoreCase(text))
						{
							form = HopAddition.Form.PELLET;
						}
						else if ("plug".equalsIgnoreCase(text))
						{
							form = HopAddition.Form.PLUG;
						}
						else if ("leaf".equalsIgnoreCase(text))
						{
							form = HopAddition.Form.LEAF;
						}
						else
						{
							throw new BrewdayException("Invalid: " + text);
						}
						current.setForm(form);
					}
					else if (currentElement.equalsIgnoreCase("use"))
					{
						HopAddition.Use use;
						if ("boil".equalsIgnoreCase(text))
						{
							use = HopAddition.Use.BOIL;
						}
						else if ("dry hop".equalsIgnoreCase(text))
						{
							use = HopAddition.Use.DRY_HOP;
						}
						else if ("mash".equalsIgnoreCase(text))
						{
							use = HopAddition.Use.MASH;
						}
						else if ("first wort".equalsIgnoreCase(text))
						{
							use = HopAddition.Use.FIRST_WORT;
						}
						else if ("aroma".equalsIgnoreCase(text))
						{
							use = HopAddition.Use.AROMA;
						}
						else
						{
							throw new BrewdayException("Invalid: " + text);
						}
						current.setUse(use);
					}
					else if (currentElement.equalsIgnoreCase("hsi"))
					{
						currentHop.setHopStorageIndex(new PercentageUnit(Double.parseDouble(text)));
					}
					else if (currentElement.equalsIgnoreCase("alpha"))
					{
						double alphaAcidPerc = Double.parseDouble(text);
						currentHop.setAlphaAcid(new PercentageUnit(alphaAcidPerc / 100D));
					}
					else if (currentElement.equalsIgnoreCase("beta"))
					{
						double betaAcidPerc = Double.parseDouble(text);
						currentHop.setBetaAcid(new PercentageUnit(betaAcidPerc / 100D));
					}
					else if (currentElement.equalsIgnoreCase("humulene"))
					{
						double humulenePerc = Double.parseDouble(text);
						currentHop.setHumulene(new PercentageUnit(humulenePerc / 100D));
					}
					else if (currentElement.equalsIgnoreCase("caryophyllene"))
					{
						double caryophyllenePerc = Double.parseDouble(text);
						currentHop.setCaryophyllene(new PercentageUnit(caryophyllenePerc / 100D));
					}
					else if (currentElement.equalsIgnoreCase("cohumulone"))
					{
						double cohumulonePerc = Double.parseDouble(text);
						currentHop.setCohumulone(new PercentageUnit(cohumulonePerc / 100D));
					}
					else if (currentElement.equalsIgnoreCase("myrcene"))
					{
						double myrcenePerc = Double.parseDouble(text);
						currentHop.setMyrcene(new PercentageUnit(myrcenePerc / 100D));
					}
					else if (currentElement.equalsIgnoreCase("time"))
					{
						current.setTime(new TimeUnit(new Double(s.trim()).intValue(), MINUTES, false));
					}
					else if (currentElement.equalsIgnoreCase("type"))
					{
						Hop.Type type = hopTypeFromBeerXml(text);
						currentHop.setType(type);
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


	static class WaterAdditionHandler extends DefaultHandler implements V2DataObjectImporter<WaterAddition>
	{
		private String currentElement;
		private WaterAddition current;
		private Water currentWater;
		private final List<WaterAddition> result = new ArrayList<>();
		private boolean parsing = false;

		private StringBuilder nameBuffer, descBuffer;

		/*-------------------------------------------------------------------------*/

		public List<WaterAddition> getResult()
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
				current = new WaterAddition();
				currentWater = new Water();

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
				currentWater.setName(nameBuffer.toString());
				currentWater.setDescription(descBuffer.toString());

				current.setUnit(LITRES);
				current.setWater(currentWater);

				result.add(current);
			}
		}

		public void characters(char[] buf, int offset,
			int len) throws SAXException
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
					else if (currentElement.equalsIgnoreCase("amount"))
					{
						current.setVolume(getVolume(text));
					}
					else if (currentElement.equalsIgnoreCase("notes"))
					{
						descBuffer.append(text);
					}
					else if (currentElement.equalsIgnoreCase("calcium"))
					{
						currentWater.setCalcium(new PpmUnit(Double.parseDouble(text)));
					}
					else if (currentElement.equalsIgnoreCase("bicarbonate"))
					{
						currentWater.setBicarbonate(new PpmUnit(Double.parseDouble(text)));
					}
					else if (currentElement.equalsIgnoreCase("sulfate"))
					{
						currentWater.setSulfate(new PpmUnit(Double.parseDouble(text)));
					}
					else if (currentElement.equalsIgnoreCase("chloride"))
					{
						currentWater.setChloride(new PpmUnit(Double.parseDouble(text)));
					}
					else if (currentElement.equalsIgnoreCase("sodium"))
					{
						currentWater.setSodium(new PpmUnit(Double.parseDouble(text)));
					}
					else if (currentElement.equalsIgnoreCase("magnesium"))
					{
						currentWater.setMagnesium(new PpmUnit(Double.parseDouble(text)));
					}
					else if (currentElement.equalsIgnoreCase("ph"))
					{
						currentWater.setPh(new PhUnit(Double.parseDouble(text)));
					}
				}
			}
		}


		protected double getPercentage(String text)
		{
			return Double.parseDouble(text) / 100D;
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


	static class FermentableAdditionHandler extends DefaultHandler implements V2DataObjectImporter<FermentableAddition>
	{
		private String currentElement;
		private FermentableAddition current;
		private Fermentable currentFermentable;
		private final List<FermentableAddition> result = new ArrayList<>();
		private boolean parsing = false;

		private StringBuilder nameBuffer, descBuffer;

		/*-------------------------------------------------------------------------*/

		public List<FermentableAddition> getResult()
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
				current = new FermentableAddition();
				currentFermentable = new Fermentable();
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
			if (qName.equalsIgnoreCase("fermentables"))
			{
				parsing = false;
			}
			if (qName.equalsIgnoreCase("fermentable"))
			{
				currentFermentable.setName(nameBuffer.toString());
				currentFermentable.setDescription(descBuffer.toString());

				current.setUnit(KILOGRAMS);
				current.setFermentable(currentFermentable);

				result.add(current);
			}
		}

		public void characters(char[] buf, int offset,
			int len) throws SAXException
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
						current.setQuantity(new WeightUnit(Double.parseDouble(s.trim()), KILOGRAMS));
					}
					if (currentElement.equalsIgnoreCase("type"))
					{
						Fermentable.Type type = fermentableTypeFromBeerXml(text);
						currentFermentable.setType(type);
					}
					if (currentElement.equalsIgnoreCase("origin"))
					{
						currentFermentable.setOrigin(text);
					}
					if (currentElement.equalsIgnoreCase("supplier"))
					{
						currentFermentable.setSupplier(text);
					}
					if (currentElement.equalsIgnoreCase("yield"))
					{
						currentFermentable.setYield(getPercentage(text));
					}
					if (currentElement.equalsIgnoreCase("color"))
					{
						// todo, the spec says "The color of the item in Lovibond Units (SRM for liquid extracts)."
						// should be converting from Lovibond here for everything except extract
						// still the difference is small so it's probably ok.
						currentFermentable.setColour(new ColourUnit(Double.parseDouble(text)));
					}
					if (currentElement.equalsIgnoreCase("add_after_boil"))
					{
						currentFermentable.setAddAfterBoil(Boolean.parseBoolean(text));
					}
					if (currentElement.equalsIgnoreCase("coarse_fine_diff"))
					{
						currentFermentable.setCoarseFineDiff(getPercentage(text));
					}
					if (currentElement.equalsIgnoreCase("moisture"))
					{
						currentFermentable.setMoisture(getPercentage(text));
					}
					if (currentElement.equalsIgnoreCase("diastatic_power"))
					{
						currentFermentable.setDiastaticPower(new DiastaticPowerUnit(Double.parseDouble(text)));
					}
					if (currentElement.equalsIgnoreCase("protein"))
					{
						currentFermentable.setProtein(getPercentage(text));
					}
					if (currentElement.equalsIgnoreCase("max_in_batch"))
					{
						currentFermentable.setMaxInBatch(getPercentage(text));
					}
					if (currentElement.equalsIgnoreCase("recommend_mash"))
					{
						currentFermentable.setRecommendMash(Boolean.parseBoolean(text));
					}
					if (currentElement.equalsIgnoreCase("ibu_gal_per_lb"))
					{
						currentFermentable.setIbuGalPerLb(Double.parseDouble(text));
					}
				}
			}
		}

		protected PercentageUnit getPercentage(String text)
		{
			return new PercentageUnit(Double.parseDouble(text) / 100D);
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
			else
			{
				throw new BrewdayException("invalid BeerXML: [" + s + "]");
			}
		}

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
		x.printStackTrace();
	}

	/*-------------------------------------------------------------------------*/
	protected static VolumeUnit getVolume(String text)
	{
		return new VolumeUnit(Double.parseDouble(text), Quantity.Unit.LITRES);
	}

	protected static WeightUnit getWeight(String text)
	{
		return new WeightUnit(Double.parseDouble(text), Quantity.Unit.KILOGRAMS);
	}

	protected static PercentageUnit getPercentage(String text)
	{
		return new PercentageUnit(Double.parseDouble(text) / 100D);
	}

	protected static TimeUnit getTimeUnit(String text, Quantity.Unit unit)
	{
		return new TimeUnit(Double.parseDouble(text), unit, false);
	}

	protected static DensityUnit getDensity(String text)
	{
		return new DensityUnit(Double.parseDouble(text), Quantity.Unit.SPECIFIC_GRAVITY);
	}

	protected static TemperatureUnit getTemperature(String text)
	{
		return new TemperatureUnit(Double.parseDouble(text), Quantity.Unit.CELSIUS, false);
	}


	/*-------------------------------------------------------------------------*/
	private BeerXmlRecipe.Type recipeTypeFromText(String s)
	{
		if (s.equalsIgnoreCase("extract"))
		{
			return BeerXmlRecipe.Type.EXTRACT;
		}
		else if (s.equalsIgnoreCase("partial mash"))
		{
			return BeerXmlRecipe.Type.PARTIAL_MASH;
		}
		else if (s.equalsIgnoreCase("all grain"))
		{
			return BeerXmlRecipe.Type.ALL_GRAIN;
		}
		else
		{
			throw new BrewdayException("invalid BeerXML: [" + s + "]");
		}
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();

		File file = new File(args[0]);

		BeerXmlRecipesHandler handler = new BeerXmlRecipesHandler(true);
		parser.parse(file, handler);

		List<BeerXmlRecipe> result = handler.getResult();

		System.out.println("nr recipes = [" + result.size() + "]");

		BeerXmlRecipe r = result.get(0);

		System.out.println("r = [" + r + "]");
	}
}
