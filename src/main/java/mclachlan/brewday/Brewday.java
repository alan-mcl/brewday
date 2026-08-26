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

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.batch.BatchAnalyser;
import mclachlan.brewday.batch.BatchVolumeEstimate;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.*;
import mclachlan.brewday.recipe.HopAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.util.AppContentRoot;
import mclachlan.brewday.util.Log;
import mclachlan.brewday.util.StringUtils;

import static mclachlan.brewday.math.Quantity.Unit.*;

/**
 *
 */
public class Brewday
{
	private static final Brewday instance = new Brewday();
	private static Log log;

	private final BatchAnalyser batchAnalyser = new BatchAnalyser();
	private final Properties appConfig;

	public static final String BREWDAY_VERSION = "mclachlan.brewday.version";
	public static final String BREWDAY_DB = "mclachlan.brewday.db";
	public static final String LOG_IMPL = "mclachlan.brewday.log.impl";
	public static final String LOG_LEVEL = "mclachlan.brewday.log.level";
	public static final String LOG_BUFFER_SIZE = "mclachlan.brewday.log.buffer.size";

	private static final Pattern QUANTITY_TEXT =
		Pattern.compile("^([+-]?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+))\\s*(.*)$");
	private static final Map<Quantity.Type, List<UnitAlias>> UNIT_ALIASES = buildUnitAliases();

	/*-------------------------------------------------------------------------*/
	public static Brewday getInstance()
	{
		return instance;
	}

	/*-------------------------------------------------------------------------*/
	private Brewday()
	{
		// read app config
		appConfig = new Properties();
		try
		{
			File cfg = AppContentRoot.resolveFile("brewday.cfg");
			if (!cfg.isFile())
			{
				cfg = new File("brewday.cfg");
			}
			FileInputStream inStream = new FileInputStream(cfg);
			appConfig.load(inStream);
			inStream.close();

			log = createLog(appConfig);
		}
		catch (Exception e)
		{
			throw new BrewdayException(e);
		}
	}

	/*-------------------------------------------------------------------------*/
	public Log getLog()
	{
		return log;
	}

	/*-------------------------------------------------------------------------*/
	private Log createLog(Properties config)
		throws ClassNotFoundException, IllegalAccessException, InstantiationException
	{
		String log_impl = (String)config.get(LOG_IMPL);
		Class log_class = Class.forName(log_impl);
		Log log = (Log)log_class.newInstance();
		int logLevel = Integer.parseInt((String)config.get(LOG_LEVEL));
		log.setLevel(logLevel);
		int bufferSize = Integer.parseInt((String)config.get(LOG_BUFFER_SIZE));
		log.setBufferSize(bufferSize);

		return log;
	}


	/*-------------------------------------------------------------------------*/

	/**
	 * @return The total IBUs from the whole hop bill for the given formula.
	 */
	public BitternessUnit calcTotalIbu(
		EquipmentProfile equipmentProfile,
		VolumeUnit volumeStart,
		DensityUnit gravityStart,
		VolumeUnit volumeEnd,
		DensityUnit gravityEnd,
		List<HopAddition> hopAdditions,
		Settings.HopBitternessFormula formula)
	{
		BitternessUnit bitternessOut = new BitternessUnit(0);
		for (HopAddition hopCharge : hopAdditions)
		{
			bitternessOut.add(
				getHopAdditionIBU(
					equipmentProfile,
					volumeStart,
					gravityStart,
					volumeEnd,
					gravityEnd,
					hopCharge,
					formula));
		}

		return bitternessOut;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return Total IBU per reported bitterness formula for the hop bill.
	 */
	public Map<Settings.HopBitternessFormula, BitternessUnit> calcTotalIbuAllReported(
		EquipmentProfile equipmentProfile,
		VolumeUnit volumeStart,
		DensityUnit gravityStart,
		VolumeUnit volumeEnd,
		DensityUnit gravityEnd,
		List<HopAddition> hopAdditions)
	{
		List<Settings.HopBitternessFormula> formulas =
			Settings.parseReportedFormulas(Database.getInstance().getSettings());
		Map<Settings.HopBitternessFormula, BitternessUnit> result = new LinkedHashMap<>();
		for (Settings.HopBitternessFormula formula : formulas)
		{
			result.put(
				formula,
				calcTotalIbu(
					equipmentProfile,
					volumeStart,
					gravityStart,
					volumeEnd,
					gravityEnd,
					hopAdditions,
					formula));
		}
		return result;
	}


	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the IBU contribution of the given hop charge for the given formula.
	 */
	public BitternessUnit getHopAdditionIBU(
		EquipmentProfile equipmentProfile,
		VolumeUnit volumeStart,
		DensityUnit gravityStart,
		VolumeUnit volumeEnd,
		DensityUnit gravityEnd,
		HopAddition hopCharge,
		Settings.HopBitternessFormula hopBitternessFormula)
	{
		return getHopAdditionIBU(
			equipmentProfile,
			volumeStart,
			gravityStart,
			volumeEnd,
			gravityEnd,
			hopCharge,
			new TimeUnit(0),
			hopBitternessFormula,
			1.0D,
			null);
	}

	/*-------------------------------------------------------------------------*/

	public BitternessUnit getHopAdditionIBU(
		EquipmentProfile equipmentProfile,
		VolumeUnit volumeStart,
		DensityUnit gravityStart,
		VolumeUnit volumeEnd,
		DensityUnit gravityEnd,
		HopAddition hopCharge,
		TimeUnit postBoilCoolTime,
		Settings.HopBitternessFormula hopBitternessFormula)
	{
		return getHopAdditionIBU(
			equipmentProfile,
			volumeStart,
			gravityStart,
			volumeEnd,
			gravityEnd,
			hopCharge,
			postBoilCoolTime,
			hopBitternessFormula,
			1.0D,
			null);
	}

	/*-------------------------------------------------------------------------*/

	public BitternessUnit getHopAdditionIBU(
		EquipmentProfile equipmentProfile,
		VolumeUnit volumeStart,
		DensityUnit gravityStart,
		VolumeUnit volumeEnd,
		DensityUnit gravityEnd,
		HopAddition hopCharge,
		TimeUnit postBoilCoolTime,
		Settings.HopBitternessFormula hopBitternessFormula,
		double smphSolubilityScale,
		PhUnit wortPh)
	{
		// Pre-isomerized extracts contribute directly to iso-alpha mass;
		// no IBU formula applies.
		if (hopCharge.getForm() != null
			&& hopCharge.getForm().isPreIsomerized())
		{
			return new BitternessUnit(0);
		}

		VolumeUnit trubAndChillerLoss = equipmentProfile.getTrubAndChillerLoss();

		BitternessUnit hopAdditionIbu;

		//
		// Note that the 'reduce the contribution if already boiled' steps are not
		// really accurate since we do not know the gravity/volume conditions of
		// the earlier boil(s). Some kind of tracking of utilisation rather than
		// just boiled time might be better, but let's just roll with this for now.
		//

		switch (hopBitternessFormula)
		{
			case TINSETH_BEERSMITH:
				// Tinseth's equation is based on the "volume of finished beer"
				// BeerSmith interprets this as "Pre boil vol - trub&chiller loss"
				// which is frankly odd. And it also uses the pre-boil gravity, instead
				// of the average wort gravity.

				// see http://www.beersmith.com/forum/index.php/topic,21613.0.html
				VolumeUnit tinsethVolume = new VolumeUnit(volumeStart.get() - trubAndChillerLoss.get());

				DensityUnit tinsethGravity = new DensityUnit(gravityStart.get());

				hopAdditionIbu = Equations.calcIbuTinseth(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					tinsethGravity,
					tinsethVolume,
					equipmentProfile.getHopUtilisation().get());

				// reduce the contribution if already boiled
				if (hopCharge.getBoiledTime().get(MINUTES) > 0)
				{
					BitternessUnit temp = Equations.calcIbuTinseth(
						(HopAddition)hopCharge,
						new TimeUnit(hopCharge.getTime().get() + hopCharge.getBoiledTime().get()),
						tinsethGravity,
						tinsethVolume,
						equipmentProfile.getHopUtilisation().get());

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;
			case TINSETH:
				// Tinseth's article is not entirely clear about which volume to
				// use, but we have word from the Prof himself:
				// "We are concerned with the mg/L and any portions of a liter lost
				// post boil doesn’t affect the calculation. Post boil volume is correct."
				// See the comments here: https://alchemyoverlord.wordpress.com/2015/05/12/a-modified-ibu-measurement-especially-for-late-hopping/

				// post-boil volume
				tinsethVolume = new VolumeUnit(volumeEnd.get());
				// we assume that Prof Tinseth would have cooled this batch to 20C
				tinsethVolume = Equations.calcCoolingShrinkage(
					tinsethVolume, new TemperatureUnit(80, CELSIUS));

				// "Use an average gravity value for the entire boil to account for changes in the wort volume"
				tinsethGravity = new DensityUnit((gravityEnd.get() + gravityStart.get()) / 2);

				hopAdditionIbu = Equations.calcIbuTinseth(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					tinsethGravity,
					tinsethVolume,
					equipmentProfile.getHopUtilisation().get());

				// reduce the contribution if already boiled
				if (hopCharge.getBoiledTime().get(MINUTES) > 0)
				{
					BitternessUnit temp = Equations.calcIbuTinseth(
						(HopAddition)hopCharge,
						new TimeUnit(hopCharge.getTime().get() + hopCharge.getBoiledTime().get()),
						tinsethGravity,
						tinsethVolume,
						equipmentProfile.getHopUtilisation().get());

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;

			case RAGER:

				// Here's another one that uses "batch volume". Let's go with the
				// same post-boil vol that Prof Tinseth suggests

				VolumeUnit ragerVol = new VolumeUnit(volumeEnd.get());
				ragerVol = Equations.calcCoolingShrinkage(
					ragerVol, new TemperatureUnit(80, CELSIUS));

				// Suggestion is that one uses the pre-boil gravity.
				// See here: https://straighttothepint.com/ibu-calculator/
				// Wish I could find Jackie Rager's original Zymurgy article to work it out.
				DensityUnit ragerGravity = new DensityUnit(gravityStart.get());

				hopAdditionIbu = Equations.calcIbuRager(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					ragerGravity,
					ragerVol,
					equipmentProfile.getHopUtilisation().get());

				// reduce the contribution if already boiled
				if (hopCharge.getBoiledTime().get(MINUTES) > 0)
				{
					BitternessUnit temp = Equations.calcIbuRager(
						(HopAddition)hopCharge,
						new TimeUnit(hopCharge.getTime().get() + hopCharge.getBoiledTime().get()),
						ragerGravity,
						ragerVol,
						equipmentProfile.getHopUtilisation().get());

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;

			case GARETZ:

				// Even more wacky, Mark Garetz wants us to pass in the "final volume"
				// to account for concentrated extract batch brews.
				// No way to get that here so just use the post-boil vol minus losses.
				// This makes this worse for extract brews, but Garetz already
				// produces estimates on the low end so WTF YOLO

				// pre boil
				VolumeUnit boilVol = new VolumeUnit(volumeStart.get());
				// post boil
				VolumeUnit finalVol = new VolumeUnit(
					volumeEnd.get() - equipmentProfile.getTrubAndChillerLoss().get());

				DensityUnit garetzGravity = new DensityUnit(gravityStart.get());

				hopAdditionIbu = Equations.calcIbuGaretz(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					garetzGravity,
					finalVol,
					boilVol,
					equipmentProfile.getHopUtilisation().get(),
					equipmentProfile.getElevation().get(FOOT));

				// reduce the contribution if already boiled
				if (hopCharge.getBoiledTime().get(MINUTES) > 0)
				{
					BitternessUnit temp = Equations.calcIbuGaretz(
						(HopAddition)hopCharge,
						new TimeUnit(hopCharge.getTime().get() + hopCharge.getBoiledTime().get()),
						garetzGravity,
						finalVol,
						boilVol,
						equipmentProfile.getHopUtilisation().get(),
						equipmentProfile.getElevation().get(FOOT));

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;

			case DANIELS:

				// Lets use the same approach to volume and gravity as Tinseth

				// post-boil volume
				tinsethVolume = new VolumeUnit(volumeEnd.get());
				// we assume that Prof Tinseth would have cooled this batch to 20C
				tinsethVolume = Equations.calcCoolingShrinkage(
					tinsethVolume, new TemperatureUnit(80, CELSIUS));

				// "Use an average gravity value for the entire boil to account for changes in the wort volume"
				tinsethGravity = new DensityUnit((gravityEnd.get() + gravityStart.get()) / 2);

				hopAdditionIbu = Equations.calcIbuDaniels(
					(HopAddition)hopCharge,
					hopCharge.getTime(),
					tinsethGravity,
					tinsethVolume,
					equipmentProfile.getHopUtilisation().get());

				// reduce the contribution if already boiled
				if (hopCharge.getBoiledTime().get(MINUTES) > 0)
				{
					BitternessUnit temp = Equations.calcIbuRager(
						(HopAddition)hopCharge,
						new TimeUnit(hopCharge.getTime().get() + hopCharge.getBoiledTime().get()),
						tinsethGravity,
						tinsethVolume,
						equipmentProfile.getHopUtilisation().get());

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;

			case MIBU:
				tinsethVolume = new VolumeUnit(volumeEnd.get());
				tinsethVolume = Equations.calcCoolingShrinkage(
					tinsethVolume, new TemperatureUnit(80, CELSIUS));

				tinsethGravity = new DensityUnit((gravityEnd.get() + gravityStart.get()) / 2);

				double kettleDiameterCm = equipmentProfile.getEffectiveBoilKettleDiameterCm();
				double openingDiameterCm = equipmentProfile.getEffectiveBoilKettleOpeningDiameterCm();
				double equipUtil = equipmentProfile.getHopUtilisation().get();

				hopAdditionIbu = Equations.calcIbuMibu(
					hopCharge,
					hopCharge.getTime(),
					postBoilCoolTime,
					tinsethGravity,
					tinsethVolume,
					kettleDiameterCm,
					openingDiameterCm,
					equipUtil);

				if (hopCharge.getBoiledTime().get(MINUTES) > 0)
				{
					TimeUnit totalBoilTime = new TimeUnit(
						hopCharge.getTime().get() + hopCharge.getBoiledTime().get());
					BitternessUnit temp = Equations.calcIbuMibu(
						hopCharge,
						totalBoilTime,
						postBoilCoolTime,
						tinsethGravity,
						tinsethVolume,
						kettleDiameterCm,
						openingDiameterCm,
						equipUtil);

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;

			case SMPH:
				tinsethVolume = new VolumeUnit(volumeEnd.get());
				tinsethVolume = Equations.calcCoolingShrinkage(
					tinsethVolume, new TemperatureUnit(80, CELSIUS));

				tinsethGravity = new DensityUnit((gravityEnd.get() + gravityStart.get()) / 2);

				hopAdditionIbu = SmphEquations.calcKettleHopIbuSmph(
					hopCharge,
					hopCharge.getTime(),
					tinsethGravity,
					tinsethVolume,
					wortPh,
					equipmentProfile.getElevation().get(FOOT),
					equipmentProfile.getHopUtilisation().get(),
					smphSolubilityScale);

				if (hopCharge.getBoiledTime().get(MINUTES) > 0)
				{
					TimeUnit totalSteep = new TimeUnit(
						hopCharge.getTime().get() + hopCharge.getBoiledTime().get());
					BitternessUnit temp = SmphEquations.calcKettleHopIbuSmph(
						hopCharge,
						totalSteep,
						tinsethGravity,
						tinsethVolume,
						wortPh,
						equipmentProfile.getElevation().get(FOOT),
						equipmentProfile.getHopUtilisation().get(),
						smphSolubilityScale);

					hopAdditionIbu = new BitternessUnit(temp.get() - hopAdditionIbu.get());
				}

				break;

			case BREWDAY:
				hopAdditionIbu = new BitternessUnit(0);
				break;

			default:
				throw new BrewdayException("invalid: " + hopBitternessFormula);
		}
		return hopAdditionIbu;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Calculates the isomerized alpha acid mass contributed by the given hop charge,
	 * using the Tinseth utilisation model and the same volume/gravity conventions as
	 * {@link Settings.HopBitternessFormula#TINSETH}.
	 */
	public WeightUnit getHopAdditionIsoAlphaMg(
		EquipmentProfile equipmentProfile,
		VolumeUnit volumeStart,
		DensityUnit gravityStart,
		VolumeUnit volumeEnd,
		DensityUnit gravityEnd,
		HopAddition hopCharge)
	{
		return getHopAdditionIsoAlphaMg(
			equipmentProfile,
			volumeStart,
			gravityStart,
			volumeEnd,
			gravityEnd,
			hopCharge,
			new TimeUnit(0));
	}

	/**
	 * Estimates the mass of iso-alpha acids produced by a hop addition during
	 * boiling using a simple kinetic isomerization model.
	 *
	 * <p>This implementation replaces the traditional Tinseth utilisation curve
	 * with a local reaction model based on first-order kinetics:</p>
	 *
	 * <ul>
	 *     <li>Alpha acids are converted into iso-alpha acids over time</li>
	 *     <li>Iso-alpha acids simultaneously degrade during boiling</li>
	 * </ul>
	 *
	 * <p>The model numerically integrates the coupled differential equations:</p>
	 *
	 * <pre>
	 * dA/dt = -k1 * A
	 * dI/dt =  k1 * A - k2 * I
	 * </pre>
	 *
	 * <p>Where:</p>
	 *
	 * <ul>
	 *     <li>A = alpha acid mass</li>
	 *     <li>I = iso-alpha acid mass</li>
	 *     <li>k1 = isomerization rate constant</li>
	 *     <li>k2 = iso-alpha degradation rate constant</li>
	 * </ul>
	 *
	 * <p>The current implementation assumes a constant boiling temperature and
	 * uses simple Euler integration. Rate constants are provisional and should
	 * later be calibrated against empirical utilisation models such as Tinseth
	 * for standard boil scenarios.</p>
	 *
	 * <p>This method computes iso-alpha production local to the boil process
	 * only. It does not model downstream losses such as trub adsorption,
	 * fermentation adsorption, or packaging losses.</p>
	 *
	 * <p>The {@code boiledTime} field on the hop addition is treated as a delayed
	 * activation interval and is subtracted from the integrated result.</p>
	 *
	 * source:
	 * Malowicki, Michael G.<br>
	 * Hop Bitter Acid Isomerization and Degradation Kinetics in a Model Wort-Boiling System<br>
	 * PhD Dissertation, Oregon State University, 2005.<br>
	 */
	public WeightUnit getHopAdditionIsoAlphaMg(
		EquipmentProfile equipmentProfile,
		VolumeUnit volumeStart,
		DensityUnit gravityStart,
		VolumeUnit volumeEnd,
		DensityUnit gravityEnd,
		HopAddition hopCharge,
		TimeUnit postBoilCoolTime)
	{
		// Pre-isomerized extracts contribute their full alpha mass directly
		// as iso-alpha acids, bypassing the kinetic model.
		if (hopCharge.getForm() != null
			&& hopCharge.getForm().isPreIsomerized())
		{
			return Equations.calcHopAlphaAcidsMg(hopCharge);
		}

		final double DT_MIN = 0.25;

		// Provisional kinetic constants at boiling temperature.
		final double K1_BOIL = 0.0060;
		final double K2_BOIL = 0.0005;

		double totalTimeMin = hopCharge.getTime().get(MINUTES);

		if (hopCharge.getBoiledTime().get(MINUTES) > 0)
		{
			totalTimeMin += hopCharge.getBoiledTime().get(MINUTES);
		}

		double startTimeMin = hopCharge.getBoiledTime().get(MINUTES);

		double availability = hopCharge.getForm() != null
			? hopCharge.getForm().getAlphaAvailability()
			: 1.0;
		double initialAlphaMg =
			hopCharge.getHop().getAlphaAcid().get(PERCENTAGE) *
				hopCharge.getQuantity().get(GRAMS) *
				1000.0 *
				availability;

		double alphaAcidsMg = initialAlphaMg;
		double isoAlphaMg = 0.0;

		double t = 0.0;

		while (t < totalTimeMin)
		{
			double dt = Math.min(DT_MIN, totalTimeMin - t);

			double converted = alphaAcidsMg * K1_BOIL * dt;
			double degraded = isoAlphaMg * K2_BOIL * dt;

			alphaAcidsMg -= converted;

			isoAlphaMg += converted;
			isoAlphaMg -= degraded;

			t += dt;
		}

		// Remove contribution before the addition becomes active.
		if (startTimeMin > 0)
		{
			double alphaPreMg = initialAlphaMg;
			double isoPreMg = 0.0;

			double tPre = 0.0;

			while (tPre < startTimeMin)
			{
				double dt = Math.min(DT_MIN, startTimeMin - tPre);

				double converted = alphaPreMg * K1_BOIL * dt;
				double degraded = isoPreMg * K2_BOIL * dt;

				alphaPreMg -= converted;

				isoPreMg += converted;
				isoPreMg -= degraded;

				tPre += dt;
			}

			isoAlphaMg -= isoPreMg;
		}

		// Retain existing equipment calibration behaviour for compatibility.
		double equipUtil = equipmentProfile.getHopUtilisation().get();

		isoAlphaMg *= equipUtil;

		return new WeightUnit(
			isoAlphaMg,
			MILLIGRAMS,
			false);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Post-flameout isomerized alpha mass for mIBU hop-stand contributions.
	 */
	public WeightUnit getHopAdditionIsoAlphaMgMibuPostBoil(
		EquipmentProfile equipmentProfile,
		VolumeUnit volumeStart,
		DensityUnit gravityStart,
		VolumeUnit volumeEnd,
		DensityUnit gravityEnd,
		HopAddition hopCharge,
		TimeUnit postBoilCoolTime)
	{
		if (hopCharge.getForm() != null
			&& hopCharge.getForm().isPreIsomerized())
		{
			return Equations.calcHopAlphaAcidsMg(hopCharge);
		}

		VolumeUnit tinsethVolume = new VolumeUnit(volumeEnd.get());
		tinsethVolume = Equations.calcCoolingShrinkage(
			tinsethVolume, new TemperatureUnit(80, CELSIUS));

		DensityUnit tinsethGravity = new DensityUnit((gravityEnd.get() + gravityStart.get()) / 2);

		double kettleDiameterCm = equipmentProfile.getEffectiveBoilKettleDiameterCm();
		double openingDiameterCm = equipmentProfile.getEffectiveBoilKettleOpeningDiameterCm();
		double equipUtil = equipmentProfile.getHopUtilisation().get();

		TimeUnit boilTime = new TimeUnit(
			hopCharge.getTime().get(MINUTES) + hopCharge.getBoiledTime().get(MINUTES));

		BitternessUnit ibu = Equations.calcIbuMibuPostBoil(
			hopCharge,
			boilTime,
			postBoilCoolTime,
			tinsethGravity,
			tinsethVolume,
			kettleDiameterCm,
			openingDiameterCm,
			equipUtil);

		return Equations.calcIsoAlphaAcidsMgFromIbu(ibu, tinsethVolume);
	}

	/*-------------------------------------------------------------------------*/
	public Properties getAppConfig()
	{
		return appConfig;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param name                The unique name of the new recipe
	 * @param processTemplateName The process template to use for this recipe
	 * @return A new recipe with the given name and configured defaults.
	 */
	public Recipe createNewRecipe(String name, String processTemplateName)
	{
		ArrayList<ProcessStep> steps = new ArrayList<>();

		String equipmentProfile =
			Database.getInstance().getSettings().get(Settings.DEFAULT_EQUIPMENT_PROFILE);

		Recipe template = Database.getInstance().getProcessTemplates().get(processTemplateName);

		Recipe recipe = new Recipe(
			name,
			StringUtils.getUiString("recipe.created.from.process.template", processTemplateName),
			equipmentProfile,
			new ArrayList<>(),
			steps);
		if (template != null)
		{
			recipe.applyProcessTemplate(template);
		}

		return recipe;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Parses user-entered free text into a {@link Quantity}. Matching is scoped
	 * by {@code type}: the same suffix (for example {@code L}) can mean litres
	 * or Lovibond depending on context. See the data-model document for the
	 * alias table.
	 *
	 * @param quantityString Whatever the user typed in.
	 * @param type           Required measurement category; suffixes are resolved
	 *                       only against this type's aliases.
	 * @param unitHint       Default unit when no suffix is present. Must belong
	 *                       to {@code type}.
	 * @return a quantity of the resolved unit, or {@code null} if the string
	 * is blank, cannot be parsed, or the suffix does not match {@code type}.
	 */
	public Quantity parseQuantity(String quantityString, Quantity.Type type,
		Quantity.Unit unitHint)
	{
		if (type == null)
		{
			throw new BrewdayException("quantity type is null");
		}
		if (unitHint == null)
		{
			throw new BrewdayException("unit hint is null");
		}
		if (unitHint.getType() != type)
		{
			throw new BrewdayException(
				"unit hint ["+unitHint.name()+"] does not match type ["+type.name()+"]");
		}

		if (quantityString == null || quantityString.isBlank())
		{
			return null;
		}

		Matcher matcher = QUANTITY_TEXT.matcher(quantityString.trim());
		if (!matcher.matches())
		{
			return null;
		}

		double amount;
		try
		{
			amount = Double.parseDouble(matcher.group(1).replace(',', '.'));
		}
		catch (NumberFormatException e)
		{
			return null;
		}

		String suffix = matcher.group(2);
		Quantity.Unit resolved = unitHint;
		if (suffix != null && !suffix.isBlank())
		{
			resolved = resolveUnitAlias(type, suffix);
			if (resolved == null)
			{
				return null;
			}
		}
		else if (type == Quantity.Type.FLUID_DENSITY
			&& unitHint == SPECIFIC_GRAVITY
			&& amount >= 2D)
		{
			// no-decimal SG entry, e.g. "1050" meaning 1.050
			amount = amount / 1000D;
		}

		return Quantity.parseQuantity(Double.toString(amount), resolved);
	}

	/*-------------------------------------------------------------------------*/
	private static Quantity.Unit resolveUnitAlias(Quantity.Type type, String suffix)
	{
		String normalised = normaliseUnitSuffix(suffix);
		if (normalised.isEmpty())
		{
			return null;
		}

		List<UnitAlias> aliases = UNIT_ALIASES.get(type);
		if (aliases == null)
		{
			return null;
		}

		for (UnitAlias alias : aliases)
		{
			if (alias.token.equals(normalised))
			{
				return alias.unit;
			}
		}
		return null;
	}

	/*-------------------------------------------------------------------------*/
	private static String normaliseUnitSuffix(String suffix)
	{
		String s = suffix.trim().toLowerCase(Locale.ROOT);
		s = s.replace("\u00b0", "").replace("\u00ba", "");
		s = s.replaceAll("\\s+", " ");
		return s;
	}

	/*-------------------------------------------------------------------------*/
	private static Map<Quantity.Type, List<UnitAlias>> buildUnitAliases()
	{
		Map<Quantity.Type, List<UnitAlias>> map = new EnumMap<>(Quantity.Type.class);
		addAliases(map, Quantity.Type.WEIGHT, MILLIGRAMS,
			"mg", "milligram", "milligrams");
		addAliases(map, Quantity.Type.WEIGHT, GRAMS,
			"g", "gram", "grams");
		addAliases(map, Quantity.Type.WEIGHT, KILOGRAMS,
			"kg", "kilo", "kilos", "kilogram", "kilograms");
		addAliases(map, Quantity.Type.WEIGHT, OUNCES,
			"oz", "ounce", "ounces");
		addAliases(map, Quantity.Type.WEIGHT, POUNDS,
			"lb", "lbs", "pound", "pounds");

		addAliases(map, Quantity.Type.LENGTH, MILLIMETRE,
			"mm", "millimetre", "millimeter", "millimetres", "millimeters");
		addAliases(map, Quantity.Type.LENGTH, CENTIMETRE,
			"cm", "centimetre", "centimeter", "centimetres", "centimeters");
		addAliases(map, Quantity.Type.LENGTH, METRE,
			"m", "metre", "meter", "metres", "meters");
		addAliases(map, Quantity.Type.LENGTH, KILOMETER,
			"km", "kilometre", "kilometer", "kilometres", "kilometers");
		addAliases(map, Quantity.Type.LENGTH, INCH,
			"in", "inch", "inches");
		addAliases(map, Quantity.Type.LENGTH, FOOT,
			"ft", "foot", "feet");
		addAliases(map, Quantity.Type.LENGTH, YARD,
			"yd", "yard", "yards");
		addAliases(map, Quantity.Type.LENGTH, MILE,
			"mi", "mile", "miles");

		addAliases(map, Quantity.Type.VOLUME, MILLILITRES,
			"ml", "millilitre", "milliliter", "millilitres", "milliliters");
		addAliases(map, Quantity.Type.VOLUME, LITRES,
			"l", "litre", "liter", "litres", "liters");
		addAliases(map, Quantity.Type.VOLUME, US_FLUID_OUNCE,
			"fl oz", "floz", "fluid ounce", "fluid ounces");
		addAliases(map, Quantity.Type.VOLUME, US_GALLON,
			"gal", "gallon", "gallons");

		addAliases(map, Quantity.Type.TEMPERATURE, CELSIUS,
			"c", "celsius", "centigrade");
		addAliases(map, Quantity.Type.TEMPERATURE, KELVIN,
			"k", "kelvin");
		addAliases(map, Quantity.Type.TEMPERATURE, FAHRENHEIT,
			"f", "fahrenheit");

		addAliases(map, Quantity.Type.FLUID_DENSITY, GU, "gu");
		addAliases(map, Quantity.Type.FLUID_DENSITY, SPECIFIC_GRAVITY,
			"sg", "specific gravity");
		addAliases(map, Quantity.Type.FLUID_DENSITY, PLATO,
			"p", "plato");

		addAliases(map, Quantity.Type.COLOUR, SRM, "srm");
		addAliases(map, Quantity.Type.COLOUR, LOVIBOND,
			"l", "lovibond");
		addAliases(map, Quantity.Type.COLOUR, EBC, "ebc");

		addAliases(map, Quantity.Type.BITTERNESS, IBU, "ibu");

		addAliases(map, Quantity.Type.CARBONATION, GRAMS_PER_LITRE, "g/l");
		addAliases(map, Quantity.Type.CARBONATION, VOLUMES,
			"vol", "vols", "volumes");

		addAliases(map, Quantity.Type.PRESSURE, KPA, "kpa");
		addAliases(map, Quantity.Type.PRESSURE, PSI, "psi");
		addAliases(map, Quantity.Type.PRESSURE, BAR, "bar");

		addAliases(map, Quantity.Type.TIME, SECONDS,
			"s", "sec", "secs", "second", "seconds");
		addAliases(map, Quantity.Type.TIME, MINUTES,
			"min", "mins", "minute", "minutes");
		addAliases(map, Quantity.Type.TIME, HOURS,
			"h", "hr", "hrs", "hour", "hours");
		addAliases(map, Quantity.Type.TIME, DAYS,
			"d", "day", "days");

		addAliases(map, Quantity.Type.SPECIFIC_HEAT, JOULE_PER_KG_CELSIUS, "j/kgc");
		addAliases(map, Quantity.Type.DIASTATIC_POWER, LINTNER,
			"l", "lintner");
		addAliases(map, Quantity.Type.POWER, KILOWATT,
			"kw", "kilowatt", "kilowatts");

		addAliases(map, Quantity.Type.OTHER, PERCENTAGE_DISPLAY,
			"%", "percent", "pct");
		addAliases(map, Quantity.Type.OTHER, PPM, "ppm");
		addAliases(map, Quantity.Type.OTHER, PH, "ph");
		addAliases(map, Quantity.Type.OTHER, MEQ_PER_KILOGRAM, "meq/kg");

		for (List<UnitAlias> aliases : map.values())
		{
			aliases.sort((a, b) -> Integer.compare(b.token.length(), a.token.length()));
		}
		return map;
	}

	/*-------------------------------------------------------------------------*/
	private static void addAliases(
		Map<Quantity.Type, List<UnitAlias>> map,
		Quantity.Type type,
		Quantity.Unit unit,
		String... tokens)
	{
		List<UnitAlias> list = map.computeIfAbsent(type, t -> new ArrayList<>());
		for (String token : tokens)
		{
			list.add(new UnitAlias(token, unit));
		}
	}

	/*-------------------------------------------------------------------------*/
	private static final class UnitAlias
	{
		private final String token;
		private final Quantity.Unit unit;

		private UnitAlias(String token, Quantity.Unit unit)
		{
			this.token = token;
			this.unit = unit;
		}
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @param recipeName The name of the recipe to use
	 * @param date       The date of the brew session
	 * @return A new batch of the given recipe, uniquely named, on the given date.
	 */
	public Batch createNewBatch(String recipeName, LocalDate date)
	{
		Recipe recipe = Database.getInstance().getRecipes().get(recipeName);
		return createNewBatch(recipe, date);
	}

	public Batch createNewBatch(Recipe recipe, LocalDate date)
	{
		recipe.run();

		// copy the estimated volumes
		Volumes vols = new Volumes(recipe.getVolumes());

		// null out the fields that need to be measured
		for (Volume v : vols.getVolumes().values())
		{
			v.setMetrics(new HashMap<>());
		}

		String id = recipe.getName() + " (1)";

		// detect duplicates
		if (Database.getInstance().getBatches().get(id) != null)
		{
			id = recipe.getName() + " (%d)";
			int count = 1;
			while (Database.getInstance().getBatches().get(
				String.format(id, count)) != null)
			{
				count++;
			}
			id = String.format(id, count);
		}

		return new Batch(id, StringUtils.getProcessString("batch.new.desc", recipe.getName()), recipe.getName(), date, vols, false);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Given a batch, return the list of estimates to be considered for analysis.
	 * The list will be sorted in order of recipe process output.
	 */
	public List<BatchVolumeEstimate> getBatchVolumeEstimates(Batch batch)
	{
		List<BatchVolumeEstimate> result = new ArrayList<>();

		Recipe recipe = Database.getInstance().getRecipes().get(batch.getRecipe());

		if (recipe == null)
		{
			// recipe not there, we can't make any estimates
			return result;
		}

		// run the recipe first in case it has no estimates yet
		recipe.run();

		EquipmentProfile equipmentProfile = Database.getInstance().getEquipmentProfiles().get(recipe.getEquipmentProfile());

		// copy over ingredient additions from the recipe to the batch
		Volumes recipeVols = recipe.getVolumes();
		for (Volume v : batch.getActualVolumes().getVolumes().values())
		{
			if (recipeVols.contains(v.getName()))
			{
				v.setIngredientAdditions(recipeVols.getVolume(v.getName()).getIngredientAdditions());
			}
		}

		ProcessLog log = new ProcessLog();
		recipe.sortSteps(log);

		// re-run with the actual volumes
		recipe.run(batch.getActualVolumes(), equipmentProfile, log);

		Set<String> keyVolumes = new HashSet<>();

		//
		// find all the volumes for key measurements as follows
		// - pre-boil volume and gravity
		// - OG at fermentation time
		// - FG and volume at packaging time
		//
		for (ProcessStep step : recipe.getSteps())
		{
			if (step instanceof Boil)
			{
				String preBoil = ((Boil)step).getInputWortVolume();

				// check for WORT type to avoid decoction boils
				if (recipeVols.contains(preBoil) &&
					recipeVols.getVolume(preBoil).getType() == Volume.Type.WORT)
				{
					keyVolumes.add(preBoil);
				}
			}
			else if (step instanceof Ferment)
			{
				// in the case of multiple fermentation stages we only want the first one

				String fermentInput = ((Ferment)step).getInputVolume();

				ProcessStep prevStep = recipe.getStepProducingVolume(fermentInput);
				if (!(prevStep instanceof Ferment))
				{
					keyVolumes.add(fermentInput);
					// need this to work out attenuation
					keyVolumes.add(((Ferment)step).getOutputVolume());
				}
			}
			else if (step instanceof PackageStep)
			{
				keyVolumes.addAll(step.getOutputVolumes());
			}
		}

		//
		// create  all the step volume measurements
		//
		for (ProcessStep step : recipe.getSteps())
		{
			for (String volName : step.getOutputVolumes())
			{
				Volume estVol = recipe.getVolumes().getVolume(volName);
				Volume measuredVol = batch.getActualVolumes().getVolumes().get(volName);

				if (estVol.getType() == Volume.Type.MASH)
				{
					if (measuredVol == null)
					{
						measuredVol = new Volume(volName, Volume.Type.MASH);
						batch.getActualVolumes().addVolume(estVol.getName(), measuredVol);
					}

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_TEMPERATURE,
							estVol.getTemperature(),
							measuredVol.getTemperature(),
							false));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_VOLUME,
							estVol.getVolume(),
							measuredVol.getVolume(),
							false));
				}
				else if (estVol.getType() == Volume.Type.WORT)
				{
					if (measuredVol == null)
					{
						measuredVol = new Volume(volName, Volume.Type.WORT);
						batch.getActualVolumes().addVolume(estVol.getName(), measuredVol);
					}

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_TEMPERATURE,
							estVol.getTemperature(),
							measuredVol.getTemperature(),
							false));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_VOLUME,
							estVol.getVolume(),
							measuredVol.getVolume(),
							keyVolumes.contains(volName)));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_DENSITY,
							estVol.getGravity(),
							measuredVol.getGravity(),
							keyVolumes.contains(volName)));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_COLOUR,
							estVol.getColour(),
							measuredVol.getColour(),
							false));
				}
				else if (estVol.getType() == Volume.Type.BEER)
				{
					if (measuredVol == null)
					{
						measuredVol = new Volume(volName, Volume.Type.BEER);
						batch.getActualVolumes().addVolume(estVol.getName(), measuredVol);
					}

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_VOLUME,
							estVol.getVolume(),
							measuredVol.getVolume(),
							keyVolumes.contains(volName)));

					// not a key metric because it's not needed to work out the attenuation
					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_DENSITY,
							estVol.getGravity(),
							measuredVol.getGravity(),
							keyVolumes.contains(volName)));

					result.add(
						new BatchVolumeEstimate(
							estVol,
							measuredVol,
							BatchVolumeEstimate.MEASUREMENTS_COLOUR,
							estVol.getColour(),
							measuredVol.getColour(),
							false));
				}
			}
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Return a list of strings representing the analysis of estimates vs
	 * measurements for the given batch.
	 *
	 * @param batch The batch to analyse
	 * @return A list of strings. These have already been pulled out of the resource
	 * bundle and are ready for rendering on the UI.
	 */
	public List<String> getBatchAnalysis(Batch batch)
	{
		return batchAnalyser.getBatchAnalysis(batch);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return All available recipe tags
	 */
	public List<String> getRecipeTags()
	{
		Set<String> tags = new HashSet<>();

		for (Recipe r : Database.getInstance().getRecipes().values())
		{
			tags.addAll(r.getTags());
		}

		ArrayList<String> result = new ArrayList<>(tags);
		result.sort(String::compareTo);
		return result;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Number of recipes whose tag list contains the given tag (exact string match on at least one list element).
	 */
	public int countRecipesWithTag(String tag)
	{
		if (tag == null)
		{
			return 0;
		}
		int n = 0;
		for (Recipe r : Database.getInstance().getRecipes().values())
		{
			if (recipeHasTag(r, tag))
			{
				n++;
			}
		}
		return n;
	}

	/*-------------------------------------------------------------------------*/

	private static boolean recipeHasTag(Recipe recipe, String tag)
	{
		for (String t : recipe.getTags())
		{
			if (tag.equals(t))
			{
				return true;
			}
		}
		return false;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Adds the tag to the recipe if not already present (exact match against list elements).
	 *
	 * @return true if the recipe was modified
	 */
	public boolean addTagToRecipeIfAbsent(Recipe recipe, String tag)
	{
		if (recipe == null || tag == null)
		{
			return false;
		}
		List<String> tags = recipe.getTags();
		if (!tags.contains(tag))
		{
			tags.add(tag);
			return true;
		}
		return false;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Removes all occurrences of {@code tag} from the recipe's tag list.
	 *
	 * @return true if the recipe was modified
	 */
	public boolean removeTagFromRecipe(Recipe recipe, String tag)
	{
		if (recipe == null || tag == null)
		{
			return false;
		}
		List<String> tags = recipe.getTags();
		boolean changed = false;
		while (tags.remove(tag))
		{
			changed = true;
		}
		return changed;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Adds {@code tag} to each recipe if absent.
	 *
	 * @return recipes that were modified
	 */
	public List<Recipe> addTagToRecipesIfAbsent(String tag, Collection<Recipe> recipes)
	{
		List<Recipe> touched = new ArrayList<>();
		if (tag == null || recipes == null)
		{
			return touched;
		}
		for (Recipe r : recipes)
		{
			if (addTagToRecipeIfAbsent(r, tag))
			{
				touched.add(r);
			}
		}
		return touched;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Removes {@code tag} from each recipe.
	 *
	 * @return recipes that were modified
	 */
	public List<Recipe> removeTagFromRecipes(String tag, Collection<Recipe> recipes)
	{
		List<Recipe> touched = new ArrayList<>();
		if (tag == null || recipes == null)
		{
			return touched;
		}
		for (Recipe r : recipes)
		{
			if (removeTagFromRecipe(r, tag))
			{
				touched.add(r);
			}
		}
		return touched;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Renames {@code fromTag} to {@code toTag} on every recipe (exact {@code fromTag} list elements removed;
	 * {@code toTag} added if missing after removals). When a recipe already had {@code toTag}, occurrences of
	 * {@code fromTag} are removed only.
	 *
	 * @return recipes that were modified
	 */
	public List<Recipe> renameRecipeTagAcrossAll(String fromTag, String toTag)
	{
		List<Recipe> touched = new ArrayList<>();
		if (fromTag == null || toTag == null || fromTag.equals(toTag))
		{
			return touched;
		}
		for (Recipe r : Database.getInstance().getRecipes().values())
		{
			List<String> tags = r.getTags();
			boolean removedAny = false;
			while (tags.remove(fromTag))
			{
				removedAny = true;
			}
			if (removedAny)
			{
				if (!tags.contains(toTag))
				{
					tags.add(toTag);
				}
				touched.add(r);
			}
		}
		return touched;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Removes {@code tag} from every recipe (all occurrences per recipe).
	 *
	 * @return recipes that were modified
	 */
	public List<Recipe> deleteRecipeTagEverywhere(String tag)
	{
		List<Recipe> touched = new ArrayList<>();
		if (tag == null)
		{
			return touched;
		}
		for (Recipe r : Database.getInstance().getRecipes().values())
		{
			if (removeTagFromRecipe(r, tag))
			{
				touched.add(r);
			}
		}
		return touched;
	}
}
