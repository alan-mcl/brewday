/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.util.StringUtils;

/**
 *
 */
public class Settings
{
	/*-------------------------------------------------------------------------*/
	//------ brewing settings
	// equipment
	public static final String DEFAULT_EQUIPMENT_PROFILE = "default.equipment.profile";

	// mash ph
	/** @deprecated use {@link #MASH_PH_MODELS}; migrated on load */
	@Deprecated
	public static final String MASH_PH_MODEL = "mash.ph.model";
	public static final String MASH_PH_MODELS = "mash.ph.models";
	public static final String MPH_MALT_BUFFERING_CORRECTION_FACTOR = "mph.malt.buffering.correction.factor";

	// hops
	public static final String MASH_HOP_UTILISATION = "mash.hop.utilisation";
	public static final String FIRST_WORT_HOP_UTILISATION = "first.wort.hop.utilisation";
	public static final String LEAF_HOP_ADJUSTMENT = "hop.adjustment.leaf";
	public static final String PLUG_HOP_ADJUSTMENT = "hop.adjustment.plug";
	public static final String PELLET_HOP_ADJUSTMENT = "hop.adjustment.pellet";
	/** @deprecated use {@link #HOP_BITTERNESS_FORMULAS}; migrated on load */
	@Deprecated
	public static final String HOP_BITTERNESS_FORMULA = "hop.bitterness.formula";
	public static final String HOP_BITTERNESS_FORMULAS = "hop.bitterness.formulas";
	public static final String TINSETH_MAX_UTILISATION = "tinseth.max.utilisation";
	public static final String GARETZ_YEAST_FACTOR = "garetz.yeast.factor";
	public static final String GARETZ_PELLET_FACTOR = "garetz.pellet.factor";
	public static final String GARETZ_BAG_FACTOR = "garetz.bag.factor";
	public static final String GARETZ_FILTER_FACTOR = "garetz.filter.factor";

	// backend settings
	public static final String GOOGLE_DRIVE_DIRECTORY_NAME = "backend.google.drive.directory.name";
	public static final String GOOGLE_DRIVE_DIRECTORY_ID = "backend.google.drive.directory.id";
	public static final String GOOGLE_DRIVE_AUTO_SYNC = "backend.google.drive.auto.sync";

	public static final String GIT_BACKEND_ENABLED = "backend.git.enabled";
	public static final String GIT_AUTO_PUSH = "backend.git.auto.push";
	/** @deprecated migrated to git {@code origin}; cleared on load */
	@Deprecated
	public static final String GIT_REMOTE_REPO = "backend.git.remote.repo";


	// ui theme settings
	public static final String UI_THEME = "ui.theme";
	public static final String JMETRO_LIGHT = "jmetro.light";
	public static final String JMETRO_DARK = "jmetro.dark";
	public static final String MODENA = "modena";
	public static final String CASPIAN = "caspian";

	// swing look-and-feel only (independent of UI_THEME / JavaFX settings)
	public static final String SWING_LOOK_AND_FEEL = "swing.laf";
	public static final String SWING_LAF_FLAT_LIGHT = "flat.light";
	public static final String SWING_LAF_FLAT_DARK = "flat.dark";
	public static final String SWING_LAF_FLAT_DARCULA = "flat.darcula";
	public static final String SWING_LAF_FLAT_INTELLIJ = "flat.intellij";
	public static final String SWING_LAF_NIMBUS = "nimbus";
	public static final String SWING_LAF_METAL = "metal";
	public static final String SWING_LAF_SYSTEM = "system";

	// random ux settings
	public static final String INGREDIENT_ADDITIONS_FROM_INVENTORY_ONLY = "ux.ingredient.additions.from.inventory.only";
	public static final String WHAT_SHOULD_I_BREW_RECENT = "ux.what.should.i.brew.recent";

	// What Should I Brew? recommendation thresholds
	public static final String RECOMMEND_MIN_GROUP_SIZE = "ux.recommend.min.group.size";
	public static final String RECOMMEND_MAX_GROUP_SIZE = "ux.recommend.max.group.size";
	public static final String RECOMMEND_HEMISPHERE = "ux.recommend.hemisphere";
	public static final String RECOMMEND_SEASONAL_LEAD_MONTHS = "ux.recommend.seasonal.lead.months";
	public static final String RECOMMEND_BEST_INVENTORY_MIN_MATCH = "ux.recommend.best.inventory.min.match";
	public static final String RECOMMEND_DUE_REPEAT_GAP_MONTHS = "ux.recommend.due.repeat.gap.months";
	public static final String RECOMMEND_STYLE_REVISIT_GAP_MONTHS = "ux.recommend.style.revisit.gap.months";
	public static final String RECOMMEND_SOMETHING_DIFFERENT_MIN_CONTRAST = "ux.recommend.something.different.min.contrast";
	public static final String RECOMMEND_NEVER_BREWED_MIN_MATCH = "ux.recommend.never.brewed.min.match";
	public static final String RECOMMEND_FORGOTTEN_GAP_MONTHS = "ux.recommend.forgotten.gap.months";
	public static final String RECOMMEND_USE_IT_UP_MIN_MATCH = "ux.recommend.use.it.up.min.match";
	public static final String RECOMMEND_ONE_PURCHASE_MIN_MATCH = "ux.recommend.one.purchase.min.match";
	public static final String RECOMMEND_STRETCH_MIN_CONTRAST = "ux.recommend.stretch.min.contrast";
	public static final String RECOMMEND_STRETCH_MIN_MATCH = "ux.recommend.stretch.min.match";

	// import/export settings
	public static final String LAST_IMPORT_DIRECTORY = "last.import.directory";
	public static final String LAST_EXPORT_DIRECTORY = "last.export.directory";

	// feature toggles
	public static final String FEATURE_TOGGLE_REMOTE_BACKENDS = "feature.remote.backends";

	/*-------------------------------------------------------------------------*/
	private final Map<String, String> settings;

	public Settings(Map<String, String> settings)
	{
		this.settings = settings;
	}

	public String get(String name)
	{
		return settings.get(name);
	}

	public void set(String name, String value)
	{
		settings.put(name, value);
	}

	public Map<String, String> getSettings()
	{
		return settings;
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Ensures {@link #HOP_BITTERNESS_FORMULAS} exists; copies legacy single formula if needed.
	 */
	public static void migrateLegacyHopBitternessSettings(Map<String, String> settings)
	{
		if (settings.get(HOP_BITTERNESS_FORMULAS) != null)
		{
			return;
		}
		String legacy = settings.get(HOP_BITTERNESS_FORMULA);
		if (legacy != null && !legacy.isBlank())
		{
			settings.put(HOP_BITTERNESS_FORMULAS, legacy.trim());
		}
		else
		{
			settings.put(HOP_BITTERNESS_FORMULAS, HopBitternessFormula.TINSETH.name());
		}
	}

	/*-------------------------------------------------------------------------*/
	public static List<HopBitternessFormula> parseReportedFormulas(Settings settings)
	{
		migrateLegacyHopBitternessSettings(settings.getSettings());
		String raw = settings.get(HOP_BITTERNESS_FORMULAS);
		if (raw == null || raw.isBlank())
		{
			return List.of(HopBitternessFormula.TINSETH);
		}
		List<HopBitternessFormula> result = new ArrayList<>();
		for (String part : raw.split(","))
		{
			String name = part.trim();
			if (!name.isEmpty())
			{
				result.add(HopBitternessFormula.valueOf(name));
			}
		}
		if (result.isEmpty())
		{
			return List.of(HopBitternessFormula.TINSETH);
		}
		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static String formatReportedFormulas(List<HopBitternessFormula> formulas)
	{
		return formulas.stream()
			.map(HopBitternessFormula::name)
			.collect(Collectors.joining(","));
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Ensures {@link #MASH_PH_MODELS} exists; copies legacy single model if needed.
	 */
	public static void migrateLegacyMashPhSettings(Map<String, String> settings)
	{
		if (settings.get(MASH_PH_MODELS) != null)
		{
			settings.put(MASH_PH_MODELS,
				sanitiseReportedMashPhModels(settings.get(MASH_PH_MODELS)));
			return;
		}
		String legacy = settings.get(MASH_PH_MODEL);
		if (legacy != null && !legacy.isBlank())
		{
			settings.put(MASH_PH_MODELS,
				sanitiseReportedMashPhModels(legacy.trim()));
		}
		else
		{
			settings.put(MASH_PH_MODELS, MashPhModel.MPH.name());
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Drops removed/unknown model names (e.g. the retired {@code Z_PH} model)
	 * from a comma-separated {@link #MASH_PH_MODELS} value, falling back to
	 * {@link MashPhModel#MPH} if nothing valid remains.
	 */
	private static String sanitiseReportedMashPhModels(String raw)
	{
		if (raw == null || raw.isBlank())
		{
			return MashPhModel.MPH.name();
		}
		List<String> valid = new ArrayList<>();
		for (String part : raw.split(","))
		{
			String name = part.trim();
			if (name.isEmpty())
			{
				continue;
			}
			try
			{
				valid.add(MashPhModel.valueOf(name).name());
			}
			catch (IllegalArgumentException e)
			{
				// removed or unknown model name; drop it
			}
		}
		if (valid.isEmpty())
		{
			return MashPhModel.MPH.name();
		}
		return String.join(",", valid);
	}

	/*-------------------------------------------------------------------------*/
	public static List<MashPhModel> parseReportedModels(Settings settings)
	{
		migrateLegacyMashPhSettings(settings.getSettings());
		String raw = settings.get(MASH_PH_MODELS);
		if (raw == null || raw.isBlank())
		{
			return List.of(MashPhModel.MPH);
		}
		List<MashPhModel> result = new ArrayList<>();
		for (String part : raw.split(","))
		{
			String name = part.trim();
			if (name.isEmpty())
			{
				continue;
			}
			try
			{
				result.add(MashPhModel.valueOf(name));
			}
			catch (IllegalArgumentException e)
			{
				// removed or unknown model name; skip it
			}
		}
		if (result.isEmpty())
		{
			return List.of(MashPhModel.MPH);
		}
		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static String formatReportedModels(List<MashPhModel> models)
	{
		return models.stream()
			.map(MashPhModel::name)
			.collect(Collectors.joining(","));
	}

	/*-------------------------------------------------------------------------*/
	public boolean isFeatureOn(String toggle)
	{
		return Boolean.parseBoolean(this.get(toggle));
	}

	/*-------------------------------------------------------------------------*/
	public Quantity.Unit getUnitForStepAndIngredient(
		Quantity.Type quantityType,
		ProcessStep step,
		IngredientAddition.Type ingredient)
	{
		ProcessStep.Type stepType;
		if (step != null)
		{
			stepType = step.getType();
		}
		else
		{
			// default
			stepType = ProcessStep.Type.MASH;
		}

		return getUnitForStepAndIngredient(quantityType, stepType, ingredient);
	}

	/*-------------------------------------------------------------------------*/
	public Quantity.Unit getUnitForStepAndIngredient(Quantity.Type quantityType,
		ProcessStep.Type stepType, IngredientAddition.Type ingredient)
	{
		return switch (ingredient)
			{
				case FERMENTABLES -> switch (quantityType)
					{
						case WEIGHT -> Quantity.Unit.KILOGRAMS;
						case VOLUME -> Quantity.Unit.LITRES;
						case LENGTH -> Quantity.Unit.MILLILITRES;
						case TEMPERATURE -> Quantity.Unit.CELSIUS;
						case TIME -> switch (stepType)
							{
								case MASH, MASH_INFUSION, STEEP, LAUTER, BATCH_SPARGE, FLY_SPARGE, BOIL,
									DILUTE, HOP_STAND, YEAST_REHYDRATE, COOL, HEAT, STAND, SPLIT, COMBINE ->
									Quantity.Unit.MINUTES;
								case FREEZE_CONCENTRATE -> Quantity.Unit.HOURS;
								case FERMENT, PACKAGE -> Quantity.Unit.DAYS;
							};
						case FLUID_DENSITY -> Quantity.Unit.SPECIFIC_GRAVITY;
						case COLOUR -> Quantity.Unit.SRM;
						case BITTERNESS -> Quantity.Unit.IBU;
						case CARBONATION -> Quantity.Unit.VOLUMES;
						case PRESSURE -> Quantity.Unit.KPA;
						case SPECIFIC_HEAT -> Quantity.Unit.JOULE_PER_KG_CELSIUS;
						case DIASTATIC_POWER -> Quantity.Unit.LINTNER;
						case POWER -> Quantity.Unit.KILOWATT;
						case OTHER -> Quantity.Unit.PERCENTAGE;
					};
				case HOPS, MISC -> switch (quantityType)
					{
						case WEIGHT -> Quantity.Unit.GRAMS;
						case LENGTH, VOLUME -> Quantity.Unit.MILLILITRES;
						case TEMPERATURE -> Quantity.Unit.CELSIUS;
						case TIME -> switch (stepType)
							{
								case MASH, MASH_INFUSION, STEEP, LAUTER, BATCH_SPARGE, FLY_SPARGE,
									BOIL, DILUTE, HOP_STAND, YEAST_REHYDRATE, COOL, HEAT, STAND, SPLIT, COMBINE ->
									Quantity.Unit.MINUTES;
								case FREEZE_CONCENTRATE -> Quantity.Unit.HOURS;
								case FERMENT, PACKAGE -> Quantity.Unit.DAYS;
							};
						case FLUID_DENSITY -> Quantity.Unit.SPECIFIC_GRAVITY;
						case COLOUR -> Quantity.Unit.SRM;
						case BITTERNESS -> Quantity.Unit.IBU;
						case CARBONATION -> Quantity.Unit.VOLUMES;
						case PRESSURE -> Quantity.Unit.KPA;
						case SPECIFIC_HEAT -> Quantity.Unit.JOULE_PER_KG_CELSIUS;
						case DIASTATIC_POWER -> Quantity.Unit.LINTNER;
						case POWER -> Quantity.Unit.KILOWATT;
						case OTHER -> Quantity.Unit.PERCENTAGE;
					};
				case WATER -> switch (quantityType)
					{
						case WEIGHT -> Quantity.Unit.KILOGRAMS;
						case LENGTH -> Quantity.Unit.MILLILITRES;
						case VOLUME -> Quantity.Unit.LITRES;
						case TEMPERATURE -> Quantity.Unit.CELSIUS;
						case TIME -> switch (stepType)
							{
								case MASH, MASH_INFUSION, STEEP, LAUTER, BATCH_SPARGE, FLY_SPARGE, BOIL,
									DILUTE, HOP_STAND, YEAST_REHYDRATE, COOL, HEAT, STAND, SPLIT, COMBINE ->
									Quantity.Unit.MINUTES;
								case FREEZE_CONCENTRATE -> Quantity.Unit.HOURS;
								case FERMENT, PACKAGE -> Quantity.Unit.DAYS;
							};
						case FLUID_DENSITY -> Quantity.Unit.SPECIFIC_GRAVITY;
						case COLOUR -> Quantity.Unit.SRM;
						case BITTERNESS -> Quantity.Unit.IBU;
						case CARBONATION -> Quantity.Unit.VOLUMES;
						case PRESSURE -> Quantity.Unit.KPA;
						case SPECIFIC_HEAT -> Quantity.Unit.JOULE_PER_KG_CELSIUS;
						case DIASTATIC_POWER -> Quantity.Unit.LINTNER;
						case POWER -> Quantity.Unit.KILOWATT;
						case OTHER -> Quantity.Unit.PERCENTAGE;
					};
				case YEAST, YEAST_CULTURE -> switch (quantityType)
					{
						case WEIGHT -> Quantity.Unit.PACKET_11_G;
						case LENGTH, VOLUME -> Quantity.Unit.MILLILITRES;
						case TEMPERATURE -> Quantity.Unit.CELSIUS;
						case TIME -> Quantity.Unit.DAYS;
						case FLUID_DENSITY -> Quantity.Unit.SPECIFIC_GRAVITY;
						case COLOUR -> Quantity.Unit.SRM;
						case BITTERNESS -> Quantity.Unit.IBU;
						case CARBONATION -> Quantity.Unit.VOLUMES;
						case PRESSURE -> Quantity.Unit.KPA;
						case SPECIFIC_HEAT -> Quantity.Unit.JOULE_PER_KG_CELSIUS;
						case DIASTATIC_POWER -> Quantity.Unit.LINTNER;
						case POWER -> Quantity.Unit.KILOWATT;
						case OTHER -> Quantity.Unit.PERCENTAGE;
					};
				default -> throw new BrewdayException("invalid " + quantityType);
			};
	}

	/*-------------------------------------------------------------------------*/
	public String getDecimalFormatter(double v)
	{
		double abs = Math.abs(v);
		if (abs > 1000)
		{
			return "0";
		}
		else if (abs > 100)
		{
			return "0.#";
		}
		else if (abs > 2)
		{
			return "0.##";
		}
		else
		{
			return "0.###";
		}
	}

	/*-------------------------------------------------------------------------*/
	public enum HopBitternessFormula
	{
		TINSETH, TINSETH_BEERSMITH, RAGER, GARETZ, DANIELS, MIBU, SMPH, BREWDAY;

		public Volume.Metric toMetric()
		{
			return Volume.Metric.valueOf("BITTERNESS_" + name());
		}

		public static HopBitternessFormula fromMetric(Volume.Metric metric)
		{
			if (metric == null || !metric.name().startsWith("BITTERNESS_"))
			{
				throw new BrewdayException("not a bitterness metric: " + metric);
			}
			return valueOf(metric.name().substring("BITTERNESS_".length()));
		}

		public static boolean isBitternessMetric(Volume.Metric metric)
		{
			return metric != null && metric.name().startsWith("BITTERNESS_");
		}

		@Override
		public String toString()
		{
			return StringUtils.getUiString("hop.bitterness.formula." + name());
		}
	}

	/*-------------------------------------------------------------------------*/
	public enum MashPhModel
	{
		EZ_WATER, MPH, KAISER_WATER;

		public Volume.Metric toMetric()
		{
			return Volume.Metric.valueOf("PH_" + name());
		}

		public static MashPhModel fromMetric(Volume.Metric metric)
		{
			if (metric == null || !metric.name().startsWith("PH_"))
			{
				throw new BrewdayException("not a mash pH metric: " + metric);
			}
			return valueOf(metric.name().substring("PH_".length()));
		}

		public static boolean isPhMetric(Volume.Metric metric)
		{
			return metric != null && metric.name().startsWith("PH_");
		}

		@Override
		public String toString()
		{
			return StringUtils.getUiString("mash.ph.model."+name());
		}
	}
}
