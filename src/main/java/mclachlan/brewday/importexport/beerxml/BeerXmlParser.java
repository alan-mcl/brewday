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
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.batch.Batch;
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
	public Map<Class<?>, Map<String, V2DataObject>> parse(List<File> files) throws Exception
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
			parseFile(file, result);
		}

		Map<String, V2DataObject> beerXmlRecipes = result.remove(BeerXmlRecipe.class);
		result.put(Recipe.class, new HashMap<>());
		result.put(Batch.class, new HashMap<>());

		if (beerXmlRecipes.size() > 0)
		{
			buildBrewdayRecipes(beerXmlRecipes, result);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
	private void parseFile(File file,
		Map<Class<?>, Map<String, V2DataObject>> map) throws Exception
	{
		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();

		// Parse the input
		SAXParser saxParser = factory.newSAXParser();
		
		V2DataObjectImporter<?>[] parsers =
		{
			new BeerXmlHopsHandler(),
			new BeerXmlFermentablesHandler(),
			new BeerXmlYeastsHandler(),
			new BeerXmlMiscsHandler(),
			new BeerXmlWatersHandler(),
			new BeerXmlEquipmentsHandler(),
			new BeerXmlStylesHandler(),
			new BeerXmlRecipesHandler(),
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
					v2DataObjects.put(dObj.getName(), dObj);
				}
			}
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Given a list of BeerXmlRecipes, build a set of Recipes + Batches and
	 * add them to the given result.
	 */
	private void buildBrewdayRecipes(
		Map<String, V2DataObject> beerXmlRecipes,
		Map<Class<?>, Map<String, V2DataObject>> result)
	{
		for (V2DataObject obj : beerXmlRecipes.values())
		{
			BeerXmlRecipe beerXmlRecipe = (BeerXmlRecipe)obj;

			Recipe recipe = new Recipe();

			recipe.setName(beerXmlRecipe.getName());
			String equipmentProfileName = beerXmlRecipe.getEquipment().getName();
			recipe.setEquipmentProfile(equipmentProfileName);

			Map<String, V2DataObject> equipmentProfiles = result.get(EquipmentProfile.class);
			if (equipmentProfiles != null)
			{
				EquipmentProfile ep = (EquipmentProfile)equipmentProfiles.get(equipmentProfileName);

				if (ep != null)
				{
					// if we are also importing this Equipment Profile, set the mash
					// efficiency on it from this recipe

					ep.setMashEfficiency(beerXmlRecipe.getEfficiency());
				}
			}

			recipe.getTags().add(StringUtils.getUiString("tools.import.tag.imported"));

			switch (beerXmlRecipe.getType())
			{
				case EXTRACT:
					buildExtractRecipe(beerXmlRecipe, recipe);
					recipe.getTags().add(StringUtils.getUiString("tools.import.tag.extract"));
					break;
				case PARTIAL_MASH:
					recipe.getTags().add(StringUtils.getUiString("tools.import.tag.partial.mash"));
					// partial mash: all the downsides of extract, all the effort of all grain
					buildAllGrainRecipe(beerXmlRecipe, recipe);
					break;
				case ALL_GRAIN:
					recipe.getTags().add(StringUtils.getUiString("tools.import.tag.all.grain"));
					buildAllGrainRecipe(beerXmlRecipe, recipe);
					break;
				default:
					throw new BrewdayException("invalid "+beerXmlRecipe.getType());
			}

			// BeerXML spec includes no observed values, so there's nothing we can do here
			Batch batch = Brewday.getInstance().createNewBatch(recipe, beerXmlRecipe.getDate());
			String desc = StringUtils.getProcessString("import.beerxml.batch.desc", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
			batch.setDescription(desc);

			result.get(Recipe.class).put(recipe.getName(), recipe);
			result.get(Batch.class).put(batch.getName(), batch);
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
				new TimeUnit(20, Quantity.Unit.MINUTES)); // no info from BeerXML, assume this
			stand.addIngredientAdditions(mashAdditions);

			if (lastOutput == null)
			{
				// if this is the first step, assume that we are steeping in the full boil volume
				// and add enough water to make that happen.
				WaterAddition steepingWater = new WaterAddition(
					waterToUse,
					new VolumeUnit(beerXmlRecipe.getBoilSize()),
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

		for (int i = 0; i < mashSteps.size(); i++)
		{
			BeerXmlMashStep step = mashSteps.get(i);

			if (i == 0)
			{
				// always treat the first step as an infusion, regardless of type
				String mashVolOutput = StringUtils.getProcessString("import.mash.vol.out", i);

				for (IngredientAddition ia : mashAdditions)
				{
					ia.setTime(new TimeUnit(step.getStepTime().get()));
				}

				Mash mash = new Mash(
					step.getName(),
					mashProfile.getNotes(),
					mashAdditions,
					mashVolOutput,
					step.getStepTime(), // todo ramp time
					mashProfile.getGrainTemp());

				if (step.getInfuseAmount() != null)
				{
					WaterAddition wa = new WaterAddition(
						waterToUse,
						step.getInfuseAmount(),
						new TemperatureUnit(step.getStepTemp()), // todo adjust to hit this target
						new TimeUnit(step.getStepTime().get()));
					mash.getIngredients().add(wa);
				}

				recipe.getSteps().add(mash);
				lastOutput = mashVolOutput;
			}
			else
			{
				switch (step.getType())
				{
					case INFUSION:
						String volOut = StringUtils.getProcessString("import.mash.infuse.out", i);

						MashInfusion mashInfusion = new MashInfusion(
							step.getName(),
							mashProfile.getNotes(),
							lastOutput,
							volOut,
							step.getRampTime(),
							step.getStepTime());

						if (step.getInfuseAmount() != null)
						{
							WaterAddition wa = new WaterAddition(
								waterToUse,
								step.getInfuseAmount(),
								new TemperatureUnit(step.getStepTemp()), // todo adjust to hit this target
								new TimeUnit(step.getStepTime().get()));
							mashInfusion.getIngredients().add(wa);
						}

						recipe.getSteps().add(mashInfusion);
						lastOutput = volOut;

						break;
					case TEMPERATURE:

						volOut = StringUtils.getProcessString("import.mash.temp.out", i);

						// model a temperature mash step with a heat step on the mash volume
						Heat heat = new Heat(
							step.getName(),
							mashProfile.getNotes(),
							lastOutput,
							volOut,
							step.getStepTemp(),
							step.getRampTime(),
							step.getStepTime());

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
							step.getStepTemp());

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
							null,
							step.getStepTime());

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
						throw new BrewdayException("Invalid "+step.getType());
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

			if (mashProfile.getSpargeTemp() != null && mashProfile.getSpargeTemp().get() > 0)
			{
				// if there's a sparge we will need the mash volume out
				lastOutput = mashVol;
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
				boilAdditions,
				beerXmlRecipe.getBoilTime());

			if (lastOutput == null)
			{
				// if this is the first step, assume that we are need to add the boil volume worth of water.
				WaterAddition steepingWater = new WaterAddition(
					waterToUse,
					new VolumeUnit(beerXmlRecipe.getBoilSize()),
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
				maxStandTime);
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
		// there's no clear way to get packaging loss from BeerXML. I guess
		// that RECIPE.EQUIPMENT.BATCH_SIZE - RECIPE.BATCH_SIZE should be the packaging loss,
		// but that is not how BeerSmith exports it. WTF let's do that anyway.

		VolumeUnit packagingLoss = new VolumeUnit(
			beerXmlRecipe.getEquipment().getBatchSize().get() - beerXmlRecipe.getBatchSize().get());

		PackageStep packageStep = new PackageStep(
			StringUtils.getProcessString("import.package.name"),
			StringUtils.getProcessString("import.package.desc"),
			packagingAdditions,
			lastOutput,
			beerXmlRecipe.getName(),
			packagingLoss,
			beerXmlRecipe.getStyle().getName());
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
				primaryAdditions);
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
				secondaryAdditions);
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
				null);
			tertiary.setTemperature(beerXmlRecipe.getTertiaryTemp());
			tertiary.setDuration(beerXmlRecipe.getTertiaryAge());
			recipe.getSteps().add(tertiary);
		}
		return lastOutput;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return
	 * 	The water to use to create water additions where needed.
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
		List<IngredientAddition> packaging
	)
	{
		// fermentables
		for (FermentableAddition fa : beerXmlRecipe.getFermentables())
		{
			Fermentable fermentable = fa.getFermentable();
			if (fermentable.isRecommendMash() && fermentable.getType() == Fermentable.Type.GRAIN)
			{
				fa.setTime(new TimeUnit(0)); // todo mash time
				mash.add(fa);
			}
			else if (fermentable.isAddAfterBoil())
			{
				fa.setTime(new TimeUnit(0));
				primary.add(fa);
			}
			else
			{
				fa.setTime(beerXmlRecipe.getBoilTime());
				boil.add(fa);
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
					throw new BrewdayException("Invalid: "+ha.getUse());
			}
			boil.add(ha);
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
					throw new BrewdayException("invalid "+ia.getMisc().getUse());
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
		}

		// Water. Not all BeerXML recipes will include it. We fudge it a lot here by:
		// - taking the largest addition or Default Water if there is no such
		// - portioning out the "top up kettle" and "top up water" properties to the boil and primary steps respectively
		// - assign the rest to mash & sparge as appropriate

		Water waterToUse = new Water("Default Water", new PhUnit(7));
		VolumeUnit biggestWater = new VolumeUnit(0);
		for (WaterAddition wa : beerXmlRecipe.getWaters())
		{
			if (wa.getVolume().get() > biggestWater.get())
			{
				biggestWater = wa.getVolume();
				waterToUse = wa.getWater();
			}
		}

		// check for top up kettle water
		if (beerXmlRecipe.getEquipment().getTopUpKettle() > 0)
		{
			boil.add(
				new WaterAddition(
					waterToUse,
					new VolumeUnit(beerXmlRecipe.getEquipment().getTopUpKettle(), Quantity.Unit.LITRES),
					new TemperatureUnit(20, Quantity.Unit.CELSIUS),
					new TimeUnit(beerXmlRecipe.getBoilTime().get())));
		}

		return waterToUse;
	}
}
