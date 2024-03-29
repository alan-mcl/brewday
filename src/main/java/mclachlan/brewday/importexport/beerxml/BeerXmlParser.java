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
import java.util.stream.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.*;
import mclachlan.brewday.style.Style;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class BeerXmlParser
{
	/*-------------------------------------------------------------------------*/
	public Map<Class<?>, Map<String, V2DataObject>> parse(List<File> files,
		boolean addImportedTag, boolean addOtherTags,
		boolean fixBeerSmithBugs) throws Exception
	{
		Map<Class<?>, Map<String, V2DataObject>> result = new HashMap<>();

		result.put(Water.class, new HashMap<>());
		result.put(Fermentable.class, new HashMap<>());
		result.put(Hop.class, new HashMap<>());
		result.put(Yeast.class, new HashMap<>());
		result.put(Misc.class, new HashMap<>());
		result.put(Style.class, new HashMap<>());
		result.put(EquipmentProfile.class, new HashMap<>());
		result.put(BeerXmlRecipe.class, new HashMap<>());

		for (File file : files)
		{
			parseFile(file, result, fixBeerSmithBugs);
		}

		Map<String, V2DataObject> beerXmlRecipes = result.remove(BeerXmlRecipe.class);
		result.put(Recipe.class, new HashMap<>());
		result.put(Batch.class, new HashMap<>());

		if (beerXmlRecipes.size() > 0)
		{
			buildBrewdayRecipes(beerXmlRecipes, result, addImportedTag, addOtherTags);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	private void parseFile(File file,
		Map<Class<?>, Map<String, V2DataObject>> map,
		boolean fixBeerSmithBugs) throws Exception
	{
		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();

		// Parse the input
		SAXParser saxParser = factory.newSAXParser();

		V2DataObjectImporter<?>[] parsers =
			{
				new BeerXmlHopsHandler(),
				new BeerXmlFermentablesHandler(fixBeerSmithBugs),
				new BeerXmlYeastsHandler(),
				new BeerXmlMiscsHandler(),
				new BeerXmlWatersHandler(),
				new BeerXmlEquipmentsHandler(),
				new BeerXmlStylesHandler(),
				new BeerXmlRecipesHandler(fixBeerSmithBugs),
			};

		for (V2DataObjectImporter<?> parser : parsers)
		{
			saxParser.parse(file, (DefaultHandler)parser);

			List parserResults = parser.getResult();

			if (parserResults.size() > 0)
			{
				Map<String, V2DataObject> v2DataObjects = map.get(parserResults.get(0).getClass());

				for (Object obj : parserResults)
				{
					V2DataObject dObj = (V2DataObject)obj;

					if (obj instanceof BeerXmlRecipe && v2DataObjects.containsKey(dObj.getName()))
					{
						DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

						// de-duplicate the recipes
						LocalDate date = ((BeerXmlRecipe)dObj).getDate();
						if (date != null)
						{
							dObj.setName(dObj.getName() + " (" + dtf.format(date) + ")");
						}
						else
						{
							int i = 1;
							String name;
							do
							{
								name = dObj.getName() + " (" + i + ")";
								i++;
							}
							while (v2DataObjects.containsKey(name));

							dObj.setName(name);
						}
						v2DataObjects.put(dObj.getName(), dObj);
					}
					else
					{
						// don't de-duplicate other types of equipment
						v2DataObjects.put(dObj.getName(), dObj);
					}
				}
			}
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Given a list of BeerXmlRecipes, build a set of Recipes + Batches and add
	 * them to the given result.
	 */
	private void buildBrewdayRecipes(
		Map<String, V2DataObject> beerXmlRecipes,
		Map<Class<?>, Map<String, V2DataObject>> result,
		boolean addImportedTag,
		boolean addOtherTags)
	{
		for (V2DataObject obj : beerXmlRecipes.values())
		{
			BeerXmlRecipe beerXmlRecipe = (BeerXmlRecipe)obj;

			Recipe recipe = new Recipe();
			String equipmentProfileName = beerXmlRecipe.getEquipment().getName();

			recipe.setName(beerXmlRecipe.getName());
			recipe.setEquipmentProfile(equipmentProfileName);
			recipe.setDescription(beerXmlRecipe.getNotes());

			Map<String, V2DataObject> equipmentProfiles = result.get(EquipmentProfile.class);
			if (equipmentProfiles != null)
			{
				EquipmentProfile ep = (EquipmentProfile)equipmentProfiles.get(equipmentProfileName);

				if (ep != null)
				{
					// BeerSmith assumes 100% conversion efficiency
					ep.setConversionEfficiency(new PercentageUnit(1));
				}
			}

			if (addImportedTag)
			{
				recipe.getTags().add(StringUtils.getUiString("tools.import.tag.imported"));
			}

			// partial mash: all the downsides of extract, all the effort of all grain
			switch (beerXmlRecipe.getType())
			{
				case EXTRACT:
					buildExtractRecipe(beerXmlRecipe, recipe);
					break;
				case PARTIAL_MASH:
					buildAllGrainRecipe(beerXmlRecipe, recipe);
					break;
				case ALL_GRAIN:
					buildAllGrainRecipe(beerXmlRecipe, recipe);
					break;
				default:
					throw new BrewdayException("invalid " + beerXmlRecipe.getType());
			}

			if (addOtherTags)
			{
				addOtherTags(beerXmlRecipe, recipe);
			}

			result.get(Recipe.class).put(recipe.getName(), recipe);

			// add any Carbonation fermentable to the DB too
			if (beerXmlRecipe.getCarbonation() != null)
			{
				if (!beerXmlRecipe.isForcedCarbonation())
				{
					String primingSugarName = mapCarbSugarName(beerXmlRecipe.getPrimingSugarName());

					if (!"None".equalsIgnoreCase(primingSugarName))
					{
						// only add it if not already in the DB, otherwise we'll overwrite
						// it every time.
						if (Database.getInstance().getFermentables().get(primingSugarName) == null)
						{
							Fermentable ferm = new Fermentable(primingSugarName);
							// the important bit
							ferm.setYield(new PercentageUnit(1 / beerXmlRecipe.getPrimingSugarEquiv()));
							// other stuff
							ferm.setRecommendMash(false);
							// guess the type
							if (primingSugarName.toLowerCase().contains("extract"))
							{
								if (primingSugarName.toLowerCase().contains("liquid"))
								{
									ferm.setType(Fermentable.Type.LIQUID_EXTRACT);
								}
								else
								{
									ferm.setType(Fermentable.Type.DRY_EXTRACT);
								}
							}
							else if (primingSugarName.toLowerCase().contains("juice"))
							{
								ferm.setType(Fermentable.Type.JUICE);
							}
							else
							{
								ferm.setType(Fermentable.Type.SUGAR);
							}

							ferm.setColour(new ColourUnit(0));

							// return it as an added fermentable
							result.get(Fermentable.class).put(ferm.getName(), ferm);
						}
					}
				}
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private String mapCarbSugarName(String primingSugarName)
	{
		// perform common BeerSmith mappings of the name
		if ("Corn Sugar".equalsIgnoreCase(primingSugarName))
		{
			primingSugarName = "Corn Sugar (Dextrose)";
		}
		else if ("Table Sugar".equalsIgnoreCase(primingSugarName))
		{
			primingSugarName = "Sugar, Table (Sucrose)";
		}
		else if ("Dry Malt Extract".equalsIgnoreCase(primingSugarName))
		{
			primingSugarName = "Light Dry Extract";
		}
		else if ("Honey".equalsIgnoreCase(primingSugarName))
		{
			primingSugarName = "Honey";
		}
		return primingSugarName;
	}


	/*-------------------------------------------------------------------------*/
	private void addOtherTags(BeerXmlRecipe beerXmlRecipe, Recipe recipe)
	{
		switch (beerXmlRecipe.getType())
		{
			case EXTRACT:
				recipe.getTags().add(StringUtils.getUiString("tools.import.tag.extract"));
				break;
			case PARTIAL_MASH:
				recipe.getTags().add(StringUtils.getUiString("tools.import.tag.partial.mash"));
				break;
			case ALL_GRAIN:
				recipe.getTags().add(StringUtils.getUiString("tools.import.tag.all.grain"));
				break;
		}

		for (YeastAddition ya : beerXmlRecipe.getYeasts())
		{
			switch (ya.getYeast().getType())
			{
				case ALE:
				case WHEAT:
					recipe.getTags().add(StringUtils.getUiString("tools.import.tag.ale"));
					break;
				case LAGER:
					recipe.getTags().add(StringUtils.getUiString("tools.import.tag.lager"));
					break;
			}
		}
	}

	/*-------------------------------------------------------------------------*/
	private void buildAllGrainRecipe(BeerXmlRecipe beerXmlRecipe, Recipe recipe)
	{
		List<IngredientAddition> mashAdditions = new ArrayList<>();
		List<IngredientAddition> lauterAdditions = new ArrayList<>();
		List<IngredientAddition> spargeAdditions = new ArrayList<>();
		List<IngredientAddition> boilAdditions = new ArrayList<>();
		List<IngredientAddition> standAdditions = new ArrayList<>();
		List<IngredientAddition> primaryAdditions = new ArrayList<>();
		List<IngredientAddition> secondaryAdditions = new ArrayList<>();
		List<IngredientAddition> packagingAdditions = new ArrayList<>();

		Water waterToUse = organiseIngredientAdditions(
			beerXmlRecipe,
			mashAdditions,
			lauterAdditions,
			spargeAdditions,
			boilAdditions,
			standAdditions,
			primaryAdditions,
			secondaryAdditions,
			packagingAdditions);

		String lastOutput = null;

		// Mash steps
		lastOutput = mashSteps(beerXmlRecipe, recipe, mashAdditions, lauterAdditions, spargeAdditions, waterToUse, lastOutput);

		// boiling
		lastOutput = boilSteps(beerXmlRecipe, recipe, boilAdditions, standAdditions, waterToUse, lastOutput);

		// fermenting
		lastOutput = fermentationSteps(beerXmlRecipe, recipe, primaryAdditions, secondaryAdditions, waterToUse, lastOutput);

		// finally a package step
		packageSteps(beerXmlRecipe, recipe, packagingAdditions, lastOutput);
	}

	/*-------------------------------------------------------------------------*/
	private void buildExtractRecipe(BeerXmlRecipe beerXmlRecipe, Recipe recipe)
	{
		// Extract recipes represent the following general steps:
		// - steep grains (if there are any such in the fermentables list)
		// - boil (unless it's 100% kit?)
		// - stand (if there are any hop steeps)
		// - dilute i.e. top-up water (if it's a partial boil)
		// - cool (if we boiled)
		// - ferment
		// - package

		List<IngredientAddition> mashAdditions = new ArrayList<>();
		List<IngredientAddition> lauterAdditions = new ArrayList<>();
		List<IngredientAddition> spargeAdditions = new ArrayList<>();
		List<IngredientAddition> boilAdditions = new ArrayList<>();
		List<IngredientAddition> standAdditions = new ArrayList<>();
		List<IngredientAddition> primaryAdditions = new ArrayList<>();
		List<IngredientAddition> secondaryAdditions = new ArrayList<>();
		List<IngredientAddition> packagingAdditions = new ArrayList<>();

		Water waterToUse = organiseIngredientAdditions(
			beerXmlRecipe,
			mashAdditions,
			lauterAdditions,
			spargeAdditions,
			boilAdditions,
			standAdditions,
			primaryAdditions,
			secondaryAdditions,
			packagingAdditions);

		String lastOutput = null;

		// Extract batch does not do a mash but may steep grains
		if (mashAdditions.size() > 0)
		{
			String steepOutput = StringUtils.getProcessString("import.steep.out");

			// assume that this means a steeping step
			// todo stand step build out
			Stand stand = new Stand(
				StringUtils.getProcessString("import.steep.name"),
				StringUtils.getProcessString("import.steep.desc"),
				lastOutput,
				steepOutput,
				new TimeUnit(20, Quantity.Unit.MINUTES),
				new ArrayList<>()); // no info from BeerXML, assume this
			stand.addIngredientAdditions(mashAdditions);

			if (lastOutput == null)
			{
				// if this is the first step, assume that we are steeping in the full boil volume
				// and add enough water to make that happen.
				WaterAddition steepingWater = new WaterAddition(
					waterToUse,
					new VolumeUnit(beerXmlRecipe.getBoilSize()),
					Quantity.Unit.LITRES,
					new TemperatureUnit(70, Quantity.Unit.CELSIUS), // no info from BeerXML, assume 70C
					new TimeUnit(20, Quantity.Unit.MINUTES));
				stand.addIngredientAddition(steepingWater);
			}

			recipe.getSteps().add(stand);
			lastOutput = steepOutput;
		}

		// boiling
		lastOutput = boilSteps(beerXmlRecipe, recipe, boilAdditions, standAdditions, waterToUse, lastOutput);

		// fermenting
		lastOutput = fermentationSteps(beerXmlRecipe, recipe, primaryAdditions, secondaryAdditions, waterToUse, lastOutput);

		// finally a package step
		packageSteps(beerXmlRecipe, recipe, packagingAdditions, lastOutput);
	}

	/*-------------------------------------------------------------------------*/
	private String mashSteps(
		BeerXmlRecipe beerXmlRecipe,
		Recipe recipe,
		List<IngredientAddition> mashAdditions,
		List<IngredientAddition> lauterAdditions,
		List<IngredientAddition> spargeAdditions,
		Water waterToUse,
		String lastOutput)
	{
		BeerXmlMashProfile mashProfile = beerXmlRecipe.getMash();
		List<BeerXmlMashStep> mashSteps = mashProfile.getMashSteps();

		VolumeUnit waterAdditions = new VolumeUnit(0);

		// try figure out the lauter loss
		VolumeUnit lauterLoss = new VolumeUnit(0);
		if (beerXmlRecipe.getEquipment() != null)
		{
			lauterLoss = beerXmlRecipe.getEquipment().getLauterLoss();
		}
		else
		{
			EquipmentProfile ep = Database.getInstance().getEquipmentProfiles().get(recipe.getEquipmentProfile());

			if (ep != null)
			{
				lauterLoss = ep.getLauterLoss();
			}
		}

		for (int i = 0; i < mashSteps.size(); i++)
		{
			BeerXmlMashStep beerXmlStep = mashSteps.get(i);

			if (i == 0)
			{
				// always treat the first step as an infusion, regardless of type
				String mashVolOutput = StringUtils.getProcessString("import.mash.vol.out", i);

				for (IngredientAddition ia : mashAdditions)
				{
					ia.setTime(new TimeUnit(beerXmlStep.getStepTime().get()));
				}

				Mash mash = new Mash(
					beerXmlStep.getName(),
					mashProfile.getNotes(),
					mashAdditions,
					null,
					mashVolOutput,
					beerXmlStep.getStepTime(), // todo ramp time
					mashProfile.getGrainTemp());

				if (beerXmlStep.getInfuseAmount() != null)
				{
					// BeerSmith has a "adjust mash vol" check box that does this.
					// TODO: this does not cater for BIAB mashes
					VolumeUnit volume = new VolumeUnit(beerXmlStep.getInfuseAmount().get() + lauterLoss.get());

					WaterAddition wa = new WaterAddition(
						waterToUse,
						volume,
						Quantity.Unit.LITRES,
						new TemperatureUnit(beerXmlStep.getStepTemp()), // todo adjust to hit this target
						new TimeUnit(beerXmlStep.getStepTime().get()));
					mash.getIngredientAdditions().add(wa);

					VolumeUnit vol = new VolumeUnit(beerXmlStep.getInfuseAmount());

					List<FermentableAddition> fermentableAdditions = mashAdditions.stream()
						.filter(ingredientAddition -> ingredientAddition instanceof FermentableAddition)
						.map(FermentableAddition.class::cast)
						.collect(Collectors.toList());

					VolumeUnit volumeOutMl = Equations.calcWortVolume(
						fermentableAdditions,
						vol,
						1D);

					waterAdditions = waterAdditions.add(volumeOutMl);
				}

				recipe.getSteps().add(mash);
				lastOutput = mashVolOutput;
			}
			else
			{
				switch (beerXmlStep.getType())
				{
					case INFUSION:
						String volOut = StringUtils.getProcessString("import.mash.infuse.out", i);

						MashInfusion mashInfusion = new MashInfusion(
							beerXmlStep.getName(),
							mashProfile.getNotes(),
							lastOutput,
							volOut,
							beerXmlStep.getRampTime(),
							beerXmlStep.getStepTime());

						if (beerXmlStep.getInfuseAmount() != null)
						{
							WaterAddition wa = new WaterAddition(
								waterToUse,
								beerXmlStep.getInfuseAmount(),
								Quantity.Unit.LITRES,
								new TemperatureUnit(beerXmlStep.getStepTemp()), // todo adjust to hit this target
								new TimeUnit(beerXmlStep.getStepTime().get()));
							mashInfusion.getIngredientAdditions().add(wa);

							waterAdditions.add(new VolumeUnit(beerXmlStep.getInfuseAmount()));
						}

						recipe.getSteps().add(mashInfusion);
						lastOutput = volOut;

						break;
					case TEMPERATURE:

						volOut = StringUtils.getProcessString("import.mash.temp.out", i);

						// model a temperature mash step with a heat step on the mash volume
						Heat heat = new Heat(
							beerXmlStep.getName(),
							mashProfile.getNotes(),
							lastOutput,
							volOut,
							beerXmlStep.getStepTemp(),
							beerXmlStep.getRampTime(),
							beerXmlStep.getStepTime());

						// beerxml supports water additions during a temp mash step
						if (beerXmlStep.getInfuseAmount() != null)
						{
							WaterAddition wa = new WaterAddition(
								waterToUse,
								beerXmlStep.getInfuseAmount(),
								Quantity.Unit.LITRES,
								new TemperatureUnit(beerXmlStep.getStepTemp()), // todo adjust to hit this target
								new TimeUnit(beerXmlStep.getStepTime().get()));
							heat.getIngredientAdditions().add(wa);

							waterAdditions = waterAdditions.add(new VolumeUnit(beerXmlStep.getInfuseAmount()));
						}

						recipe.getSteps().add(heat);

						lastOutput = volOut;

						break;
					case DECOCTION:
						// We represent a decoction by a split/boil/combine sequence
						// BeerXML does not cater for rests during heating the decocted volume to
						// a boil so no need for a split/heat/boil/combine sequence.
						// We do however need to work backwards to the decocted amount from the
						// BeerXML step temp - this seems to be the way that BeerSmith does it too.

						String splitOut1 = StringUtils.getProcessString("import.mash.decoct.split.out.1", i);
						String splitOut2 = StringUtils.getProcessString("import.mash.decoct.split.out.2", i);

						BeerXmlMashStep lastStep = mashSteps.get(i - 1);

						VolumeUnit mashVol;
						if (beerXmlRecipe.getEquipment() != null)
						{
							// todo this is not really the mash volume!
							mashVol = beerXmlRecipe.getEquipment().getMashTunVolume();
						}
						else
						{
							// wtf to do here, there is no good way of guessing the mash vol
							mashVol = new VolumeUnit(beerXmlRecipe.getBatchSize().get() / 2);
						}

						VolumeUnit decoctionVolume = Equations.calcDecoctionVolume(
							mashVol,
							lastStep.getStepTemp(),
							beerXmlStep.getStepTemp());

						// SPLIT

						Split split = new Split(
							StringUtils.getProcessString("import.mash.decoct.split", i),
							StringUtils.getProcessString("import.mash.decoct.split.desc"),
							lastOutput,
							splitOut1,
							Split.Type.ABSOLUTE,
							null,
							decoctionVolume,
							splitOut2);

						recipe.getSteps().add(split);

						// BOIL

						String boilOutput = StringUtils.getProcessString("import.mash.decoct.boil.out", i);

						Boil boil = new Boil(
							StringUtils.getProcessString("import.mash.decoct.boil.name", i),
							StringUtils.getProcessString("import.mash.decoct.boil.desc"),
							splitOut1,
							boilOutput,
							StringUtils.getProcessString("import.boil.out.trub"),
							null,
							beerXmlStep.getStepTime(),
							false);

						recipe.getSteps().add(boil);

						// COMBINE

						String combineOut = StringUtils.getProcessString("import.mash.decoct.combine.out", i);

						Combine combine = new Combine(
							StringUtils.getProcessString("import.mash.decoct.combine", i),
							StringUtils.getProcessString("import.mash.decoct.combine.desc"),
							boilOutput,
							splitOut2,
							combineOut);

						recipe.getSteps().add(combine);
						lastOutput = combineOut;

						break;
					default:
						throw new BrewdayException("Invalid " + beerXmlStep.getType());
				}
			}
		}

		// finish with a Lauter step
		if (mashSteps != null && mashSteps.size() > 0)
		{
			String mashVol = StringUtils.getProcessString("import.lauter.out.mash");
			String firstRunnings = StringUtils.getProcessString("import.lauter.out.first.runnings");
			Lauter lauter = new Lauter(
				StringUtils.getProcessString("import.lauter"),
				mashProfile.getNotes(),
				lastOutput,
				mashVol,
				firstRunnings);
			lauter.addIngredientAdditions(lauterAdditions);

			recipe.getSteps().add(lauter);

			// sparge step needed?
			if (mashProfile.getSpargeTemp() != null && mashProfile.getSpargeTemp().get() > 0)
			{
				// work out the sparge size
				VolumeUnit spargeWaterVol = new VolumeUnit(
					beerXmlRecipe.getBoilSize().get() -
						waterAdditions.get());

				// BeerSmith exports a value in MASH_PROFILE.SPARGE_TEMP even if
				// your mash profile has no sparge step.
				if (spargeWaterVol.get() > 0)
				{
					String outputCombinedRunnings = StringUtils.getProcessString("import.batch.sparge.out.combined");
					String outputSpargeRunnings = StringUtils.getProcessString("import.batch.sparge.out.sparge");
					String outputMashVolume = StringUtils.getProcessString("import.batch.sparge.out.mash");

					BatchSparge sparge = new BatchSparge(
						StringUtils.getProcessString("import.batch.sparge"),
						StringUtils.getProcessString("import.batch.sparge.desc"),
						mashVol,
						firstRunnings,
						outputCombinedRunnings,
						outputSpargeRunnings,
						outputMashVolume,
						spargeAdditions);

					WaterAddition wa = new WaterAddition(
						waterToUse,
						spargeWaterVol,
						Quantity.Unit.LITRES,
						new TemperatureUnit(mashProfile.getSpargeTemp()),
						new TimeUnit(0));
					sparge.getIngredientAdditions().add(wa);

					recipe.getSteps().add(sparge);

					lastOutput = outputCombinedRunnings;
				}
				else
				{
					// if no sparge then we will just be working with the wort from here
					lastOutput = firstRunnings;
				}
			}
			else
			{
				// if no sparge then we will just be working with the wort from here
				lastOutput = firstRunnings;
			}
		}


		return lastOutput;
	}

	/*-------------------------------------------------------------------------*/
	private String boilSteps(
		BeerXmlRecipe beerXmlRecipe,
		Recipe recipe,
		List<IngredientAddition> boilAdditions,
		List<IngredientAddition> standAdditions,
		Water waterToUse,
		String lastOutput)
	{
		// the boil
		if (boilAdditions.size() > 0 || (beerXmlRecipe.getBoilTime() != null && beerXmlRecipe.getBoilTime().get() > 0))
		{
			String boilOutput = StringUtils.getProcessString("import.boil.out");

			Boil boil = new Boil(
				StringUtils.getProcessString("import.boil.name"),
				StringUtils.getProcessString("import.boil.desc"),
				lastOutput,
				boilOutput,
				StringUtils.getProcessString("import.boil.out.trub"),
				boilAdditions,
				beerXmlRecipe.getBoilTime(),
				false);

			if (lastOutput == null)
			{
				// if this is the first step, assume that we are need to add the boil volume worth of water.
				WaterAddition steepingWater = new WaterAddition(
					waterToUse,
					new VolumeUnit(beerXmlRecipe.getBoilSize()),
					Quantity.Unit.LITRES,
					new TemperatureUnit(20, Quantity.Unit.CELSIUS),
					new TimeUnit(beerXmlRecipe.getBoilTime().get()));
				boil.addIngredientAddition(steepingWater);
			}

			recipe.getSteps().add(boil);

			lastOutput = boilOutput;
		}

		// typically with BeerXML this will be "aroma" hops that are steeped
		if (standAdditions.size() > 0)
		{
			String standOutput = StringUtils.getProcessString("import.hopstand.out");

			TimeUnit maxStandTime = new TimeUnit(0, Quantity.Unit.MINUTES);
			for (IngredientAddition ia : standAdditions)
			{
				if (ia.getTime().get(Quantity.Unit.MINUTES) > maxStandTime.get(Quantity.Unit.MINUTES))
				{
					maxStandTime = new TimeUnit(ia.getTime().get());
				}
			}

			Stand stand = new Stand(
				StringUtils.getProcessString("import.hopstand.name"),
				StringUtils.getProcessString("import.hopstand.desc"),
				lastOutput,
				standOutput,
				maxStandTime,
				new ArrayList<>());
			stand.addIngredientAdditions(standAdditions);

			recipe.getSteps().add(stand);

			lastOutput = standOutput;
		}

		// if we did a boil, do a cool
		if (boilAdditions.size() > 0)
		{
			TemperatureUnit primaryTemp = beerXmlRecipe.getPrimaryTemp();

			String coolOutput = StringUtils.getProcessString("import.cool.out");

			Cool cool = new Cool(
				StringUtils.getProcessString("import.cool.name"),
				StringUtils.getProcessString("import.cool.desc"),
				lastOutput,
				coolOutput,
				primaryTemp == null ? new TemperatureUnit(20, Quantity.Unit.CELSIUS) : primaryTemp);
			recipe.getSteps().add(cool);

			lastOutput = coolOutput;
		}
		return lastOutput;
	}

	/*-------------------------------------------------------------------------*/
	private void packageSteps(
		BeerXmlRecipe beerXmlRecipe,
		Recipe recipe,
		List<IngredientAddition> packagingAdditions,
		String lastOutput)
	{
		// There's no clear way to get packaging loss from BeerXML.
		// RECIPE.EQUIPMENT.BATCH_SIZE - RECIPE.BATCH_SIZE is not it!
		// Here I am assuming 10% based on a bunch of anecdotal evidence.
		// This is probably fine for homebrew scale but I doubt it scales up.

		VolumeUnit packagingLoss = new VolumeUnit(
			beerXmlRecipe.getEquipment().getBatchSize().get() * getPackagingLossRatio());

		PackageStep.PackagingType type;

		CarbonationUnit forcedCarb = null;
		if (beerXmlRecipe.isForcedCarbonation())
		{
			type = PackageStep.PackagingType.KEG;
			forcedCarb = new CarbonationUnit(beerXmlRecipe.getCarbonation());
		}
		else
		{
			// we need to fudge it somehow
			String carbonationUsed = beerXmlRecipe.getCarbonationUsed();
			if (carbonationUsed != null && carbonationUsed.toLowerCase().contains("keg"))
			{
				type = PackageStep.PackagingType.KEG_WITH_PRIMING;
			}
			else
			{
				type = PackageStep.PackagingType.BOTTLE;
			}
		}

		PackageStep packageStep = new PackageStep(
			StringUtils.getProcessString("import.package.name"),
			StringUtils.getProcessString("import.package.desc"),
			packagingAdditions,
			lastOutput,
			beerXmlRecipe.getName(),
			packagingLoss,
			beerXmlRecipe.getStyle().getName(),
			type,
			forcedCarb);
		recipe.getSteps().add(packageStep);
	}

	/*-------------------------------------------------------------------------*/
	private String fermentationSteps(
		BeerXmlRecipe beerXmlRecipe,
		Recipe recipe,
		List<IngredientAddition> primaryAdditions,
		List<IngredientAddition> secondaryAdditions,
		Water waterToUse,
		String lastOutput)
	{
		// check for top up water in the before fermentation
		if (beerXmlRecipe.getEquipment().getTopUpWater() > 0)
		{
			String diluteOutput = StringUtils.getProcessString("import.dilute.out");

			WaterAddition dilutionWater = new WaterAddition(
				waterToUse,
				new VolumeUnit(beerXmlRecipe.getEquipment().getTopUpWater(), Quantity.Unit.LITRES),
				Quantity.Unit.LITRES,
				new TemperatureUnit(20, Quantity.Unit.CELSIUS),
				new TimeUnit(0));

			Dilute dilute = new Dilute(
				StringUtils.getProcessString("import.dilute.name"),
				StringUtils.getProcessString("import.dilute.desc"),
				lastOutput,
				diluteOutput,
				new ArrayList<>());
			dilute.addIngredientAddition(dilutionWater);

			recipe.getSteps().add(dilute);
			lastOutput = diluteOutput;
		}

		// fermentation stages can be zero in BeerXML
		if (beerXmlRecipe.getFermentationStages() > 0)
		{
			String fermentOutput = StringUtils.getProcessString("import.primary.out");

			Ferment primaryFerm = new Ferment(
				StringUtils.getProcessString("import.primary.name"),
				StringUtils.getProcessString("import.primary.desc"),
				lastOutput,
				fermentOutput,
				beerXmlRecipe.getPrimaryTemp(),
				beerXmlRecipe.getPrimaryAge(),
				primaryAdditions,
				true);
			recipe.getSteps().add(primaryFerm);

			lastOutput = fermentOutput;
		}

		if (beerXmlRecipe.getFermentationStages() > 1)
		{
			String fermentOutput = StringUtils.getProcessString("import.secondary.out");

			Ferment secondaryFerm = new Ferment(
				StringUtils.getProcessString("import.secondary.name"),
				StringUtils.getProcessString("import.secondary.desc"),
				lastOutput,
				fermentOutput,
				beerXmlRecipe.getSecondaryTemp(),
				beerXmlRecipe.getSecondaryAge(),
				secondaryAdditions,
				false);
			recipe.getSteps().add(secondaryFerm);

			lastOutput = fermentOutput;
		}

		if (beerXmlRecipe.getFermentationStages() > 2)
		{
			String fermentOutput = StringUtils.getProcessString("import.tertiary.out");

			Ferment tertiary = new Ferment(
				StringUtils.getProcessString("import.tertiary.name"),
				StringUtils.getProcessString("import.tertiary.desc"),
				lastOutput,
				fermentOutput,
				beerXmlRecipe.getTertiaryTemp(),
				beerXmlRecipe.getTertiaryAge(),
				null,
				false);
			recipe.getSteps().add(tertiary);

			lastOutput = fermentOutput;
		}
		return lastOutput;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return The water to use to create water additions where needed.
	 */
	private Water organiseIngredientAdditions(
		BeerXmlRecipe beerXmlRecipe,
		List<IngredientAddition> mash,
		List<IngredientAddition> lauter,
		List<IngredientAddition> sparge,
		List<IngredientAddition> boil,
		List<IngredientAddition> stand,
		List<IngredientAddition> primary,
		List<IngredientAddition> secondary,
		List<IngredientAddition> packaging)
	{
		// fermentables

		// Water. Not all BeerXML recipes will include it. We fudge it a lot here by:
		// - taking the largest addition or Default Water if there is no such
		// - portioning out the "top up kettle" and "top up water" properties to the boil and primary steps respectively
		// - assign the rest to mash & sparge as appropriate

		Water waterToUse = new Water("Default Water", new PhUnit(7));
		waterToUse.setCalcium(new PpmUnit(0));
		waterToUse.setBicarbonate(new PpmUnit(0));
		waterToUse.setChloride(new PpmUnit(0));
		waterToUse.setSulfate(new PpmUnit(0));
		waterToUse.setSodium(new PpmUnit(0));
		waterToUse.setMagnesium(new PpmUnit(0));
		VolumeUnit biggestWater = new VolumeUnit(0);
		for (WaterAddition wa : beerXmlRecipe.getWaters())
		{
			if (wa.getVolume().get() > biggestWater.get())
			{
				biggestWater = wa.getVolume();
				waterToUse = wa.getWater();
			}
		}

		// Beer XML says: RECOMMEND_MASH
		// TRUE if it is recommended the grain be mashed, FALSE if it can be steeped.
		// A value of TRUE is only appropriate for a "Grain" or "Adjunct" types.
		// The default value is FALSE.  Note that this does NOT indicate whether
		// the grain is mashed or not – it is only a recommendation used in
		// recipe formulation.
		//
		for (FermentableAddition fa : beerXmlRecipe.getFermentables())
		{
			Fermentable fermentable = fa.getFermentable();
			if (fermentable.isRecommendMash() &&
				(fermentable.getType() == Fermentable.Type.GRAIN || fermentable.getType() == Fermentable.Type.ADJUNCT))
			{
				fa.setTime(new TimeUnit(0)); // mash time will be sorted out later
				mash.add(fa);
			}
			else if (fermentable.isAddAfterBoil())
			{
				fa.setTime(new TimeUnit(0));
				primary.add(fa);
			}
			else
			{
				if (fermentable.getType() == Fermentable.Type.GRAIN || fermentable.getType() == Fermentable.Type.ADJUNCT)
				{
					// buildExtractRecipe also expects these in the mash steps
					fa.setTime(new TimeUnit(0));
					mash.add(fa);
				}
				else
				{
					// nowhere else to put it
					fa.setTime(beerXmlRecipe.getBoilTime());
					boil.add(fa);
				}
			}
		}

		// hops...
		for (HopAddition ha : beerXmlRecipe.getHops())
		{
			switch (ha.getUse())
			{
				case BOIL:
					boil.add(ha);
					break;
				case DRY_HOP:
					// We assume that a dry hop addition is in secondary if possible
					// This will lose the hop addition if there are no fermentation
					// stages but WTF YOLO
					if (beerXmlRecipe.getFermentationStages() > 1)
					{
						secondary.add(ha);
					}
					else
					{
						primary.add(ha);
					}
					break;
				case MASH:
					mash.add(ha);
					break;
				case FIRST_WORT:
					lauter.add(ha);
					break;
				case AROMA:
					// we use a stand step to do the whirlpool steep
					stand.add(ha);
					break;
				default:
					throw new BrewdayException("Invalid: " + ha.getUse());
			}
		}

		// miscs can go just about anywhere
		for (MiscAddition ia : beerXmlRecipe.getMiscs())
		{
			switch (ia.getMisc().getUse())
			{
				case BOIL:
					ia.setTime(beerXmlRecipe.getBoilTime());
					boil.add(ia);
					break;
				case MASH:
					ia.setTime(new TimeUnit(0));
					mash.add(ia);
					break;
				case PRIMARY:
					ia.setTime(new TimeUnit(0));
					primary.add(ia);
					break;
				case SECONDARY:
					ia.setTime(new TimeUnit(0));
					secondary.add(ia);
					break;
				case BOTTLING:
					ia.setTime(new TimeUnit(0));
					packaging.add(ia);
					break;
				default:
					throw new BrewdayException("invalid " + ia.getMisc().getUse());
			}
		}

		// BeerXML does not provide for yeast added at packaging, so we just tag
		// it all as fermentation. Also tertiary ferm is SOL, thanks BeerXML
		for (YeastAddition ya : beerXmlRecipe.getYeasts())
		{
			ya.setTime(new TimeUnit(0));
			if (ya.getAddToSecondary())
			{
				secondary.add(ya);
			}
			else
			{
				primary.add(ya);
			}

			if (ya.getYeast().getForm() == Yeast.Form.DRY &&
				ya.getQuantity().getType() == Quantity.Type.VOLUME)
			{
				// BeerSmith exports every yeast as "50ml", even dry ones.
				// add a water volume here to reflect that
				VolumeUnit q = new VolumeUnit((VolumeUnit)ya.getQuantity());

				// guess 1 packet
				ya.setQuantity(new WeightUnit(11, Quantity.Unit.GRAMS));
				ya.setUnit(Quantity.Unit.GRAMS);

				WaterAddition wa = new WaterAddition(
					waterToUse,
					q,
					Quantity.Unit.MILLILITRES,
					new TemperatureUnit(30),
					new TimeUnit(ya.getTime()));

				if (ya.getAddToSecondary())
				{
					secondary.add(wa);
				}
				else
				{
					primary.add(wa);
				}
			}
		}

		// priming sugars, if any
		if (beerXmlRecipe.getCarbonation() != null)
		{
			if (!beerXmlRecipe.isForcedCarbonation())
			{
				CarbonationUnit carbonation = beerXmlRecipe.getCarbonation();

				VolumeUnit packagingVolume = new VolumeUnit(
					beerXmlRecipe.getBatchSize().get() * (1 - getPackagingLossRatio()));

				String primingSugarName = mapCarbSugarName(beerXmlRecipe.getPrimingSugarName());

				if (!"None".equalsIgnoreCase(primingSugarName) && primingSugarName != null)
				{
					// we only persist the name, so this is ok. See the section in
					// #buildBrewdayRecipes where we add new Fermentables to the ref DB
					Fermentable ferm = new Fermentable(primingSugarName);
					ferm.setYield(new PercentageUnit(1 / beerXmlRecipe.getPrimingSugarEquiv()));

					FermentableAddition packagingAddition = Equations.calcPrimingSugarAmount(
						packagingVolume,
						ferm,
						carbonation);

					packaging.add(packagingAddition);
				}
			}
		}

		// check for top up kettle water
		if (beerXmlRecipe.getEquipment().getTopUpKettle() > 0)
		{
			boil.add(
				new WaterAddition(
					waterToUse,
					new VolumeUnit(beerXmlRecipe.getEquipment().getTopUpKettle(), Quantity.Unit.LITRES),
					Quantity.Unit.LITRES,
					new TemperatureUnit(20, Quantity.Unit.CELSIUS),
					new TimeUnit(beerXmlRecipe.getBoilTime().get())));
		}

		return waterToUse;
	}

	/*-------------------------------------------------------------------------*/
	private double getPackagingLossRatio()
	{
		return 0.1;
	}
}
