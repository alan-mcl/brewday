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

package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.Settings.HopBitternessFormula;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.WaterAddition;
import mclachlan.brewday.recipe.YeastCulture;
import mclachlan.brewday.style.Style;

/**
 *
 */
public class Volume
{
	/**
	 * Unique name of this volume
	 */
	private String name;

	/**
	 * Type of this volume
	 */
	private Type type;

	/**
	 * A bag of metrics, not all apply to every volume type
	 */
	private Map<Metric, Quantity> metrics = new HashMap<>();

	/**
	 * Ingredient additions carried along in this volume as needed
	 */
	private List<IngredientAddition> ingredientAdditions = new ArrayList<>();

	/**
	 * Style of this volume, only applicable to beer
	 */
	private Style style;

	/*-------------------------------------------------------------------------*/
	protected Volume()
	{
	}

	protected Volume(Type type)
	{
		this.type = type;
	}

	public Volume(
		String name,
		Type type,
		Map<Metric, Quantity> metrics,
		List<IngredientAddition> ingredientAdditions)
	{
		this.name = name;
		this.type = type;
		this.metrics = new HashMap<>(metrics);
		this.ingredientAdditions = new ArrayList<>(ingredientAdditions);
	}

	/**
	 * Constructor with the typical Beer volume metrics
	 */
	public Volume(
		String name,
		Type type,
		VolumeUnit volume,
		TemperatureUnit temperature,
		DensityUnit originalGravity,
		DensityUnit gravity,
		PercentageUnit abv,
		ColourUnit colour,
		BitternessUnit bitterness)
	{
		setName(name);
		this.type = type;
		setMetric(Metric.VOLUME, volume);
		setMetric(Metric.TEMPERATURE, temperature);
		setMetric(Metric.ORIGINAL_GRAVITY, originalGravity);
		setMetric(Metric.GRAVITY, gravity);
		setMetric(Metric.ABV, abv);
		setMetric(Metric.COLOUR, colour);
		BitternessVolumes.setAllReported(this, bitterness);
	}

	/**
	 * Constructor with the typical Wort volume metrics
	 */
	public Volume(
		String name,
		Type type,
		VolumeUnit volume,
		TemperatureUnit temperature,
		PercentageUnit fermentability,
		DensityUnit gravity,
		PercentageUnit abv,
		ColourUnit colour,
		BitternessUnit bitterness)
	{
		setName(name);
		this.type = type;
		setMetric(Metric.VOLUME, volume);
		setMetric(Metric.TEMPERATURE, temperature);
		setMetric(Metric.FERMENTABILITY, fermentability);
		setMetric(Metric.GRAVITY, gravity);
		setMetric(Metric.ABV, abv);
		setMetric(Metric.COLOUR, colour);
		BitternessVolumes.setAllReported(this, bitterness);
	}

	/**
	 * Constructor with the typical Mash volume metrics
	 */
	public Volume(
		String name,
		Type type,
		VolumeUnit volume,
		List<? extends IngredientAddition> fermentables,
		WaterAddition water,
		TemperatureUnit temperature,
		DensityUnit gravity,
		ColourUnit colour,
		PhUnit ph)
	{
		setName(name);
		this.type = type;
		setMetric(Metric.VOLUME, volume);
		setMetric(Metric.TEMPERATURE, temperature);
		setMetric(Metric.GRAVITY, gravity);
		setMetric(Metric.COLOUR, colour);
		setMetric(Metric.PH, ph);

		this.ingredientAdditions = new ArrayList<>();
		ingredientAdditions.addAll(fermentables);
		ingredientAdditions.add(water);
	}

	public Volume(String name, Volume inputVolume)
	{
		this(
			name,
			inputVolume.getType(),
			new HashMap<>(inputVolume.getMetrics()),
			new ArrayList<>(inputVolume.getIngredientAdditions()));

		this.setStyle(inputVolume.style);
	}

	public Volume(String name, Type type)
	{
		setName(name);
		this.type = type;
	}

	/*-------------------------------------------------------------------------*/

	public Type getType()
	{
		return type;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Quantity getMetric(Metric metric)
	{
		return this.metrics.get(metric);
	}

	private void setMetric(Metric metric, Quantity quantity)
	{
		if (quantity == null)
		{
			this.metrics.remove(metric);
		}
		else
		{
			this.metrics.put(metric, quantity);
		}
	}

	public void setMetric(Metric metric, Quantity quantity, boolean estimated)
	{
		quantity.setEstimated(estimated);
		this.setMetric(metric, quantity);
	}

	public boolean hasMetric(Metric metric)
	{
		return this.metrics.containsKey(metric);
	}

	public Map<Metric, Quantity> getMetrics()
	{
		return metrics;
	}

	public void setMetrics(Map<Metric, Quantity> metrics)
	{
		this.metrics = metrics;
	}

	public List<IngredientAddition> getIngredientAdditions()
	{
		return Collections.unmodifiableList(ingredientAdditions);
	}

	public void removeIngredientAddition(IngredientAddition ia)
	{
		ingredientAdditions.remove(ia);
	}

	/*-------------------------------------------------------------------------*/
	public List<YeastCulture> getYeastCultures()
	{
		List<YeastCulture> result = new ArrayList<>();

		for (IngredientAddition ia : ingredientAdditions)
		{
			if (ia instanceof YeastCulture)
			{
				result.add((YeastCulture)ia);
			}
		}

		return Collections.unmodifiableList(result);
	}

	/*-------------------------------------------------------------------------*/
	public void addYeastCulture(YeastCulture culture)
	{
		ingredientAdditions.add(culture);
	}

	/*-------------------------------------------------------------------------*/
	public void setIngredientAdditions(
		List<IngredientAddition> ingredientAdditions)
	{
		this.ingredientAdditions = new ArrayList<>();

		for (IngredientAddition ia : ingredientAdditions)
		{
			this.ingredientAdditions.add(ia.clone());
		}
	}

	public Style getStyle()
	{
		return style;
	}

	public void setStyle(Style style)
	{
		this.style = style;
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return all ingredient additions of the given type
	 */
	protected List<IngredientAddition> getIngredientAdditions(
		IngredientAddition.Type type)
	{
		List<IngredientAddition> result = new ArrayList<>();

		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == type)
			{
				result.add(ia);
			}
		}

		return result;
	}


	/**
	 * @return one ingredient additions of the given type (first one found)
	 */
	protected IngredientAddition getIngredientAddition(
		IngredientAddition.Type type)
	{
		for (IngredientAddition ia : getIngredientAdditions())
		{
			if (ia.getType() == type)
			{
				return ia;
			}
		}

		return null;
	}

	/*-------------------------------------------------------------------------*/
	public DensityUnit getGravity()
	{
		return (DensityUnit)getMetric(Metric.GRAVITY);
	}

	public DensityUnit getOriginalGravity()
	{
		return (DensityUnit)getMetric(Metric.ORIGINAL_GRAVITY);
	}

	public ColourUnit getColour()
	{
		return (ColourUnit)getMetric(Metric.COLOUR);
	}

	public BitternessUnit getBitterness(HopBitternessFormula formula)
	{
		return BitternessVolumes.get(this, formula);
	}

	/**
	 * IBU from the first reported bitterness formula (style checks and legacy callers).
	 */
	public BitternessUnit getBitterness()
	{
		return BitternessVolumes.getPrimary(this);
	}

	public VolumeUnit getVolume()
	{
		return (VolumeUnit)getMetric(Metric.VOLUME);
	}

	public TemperatureUnit getTemperature()
	{
		return (TemperatureUnit)getMetric(Metric.TEMPERATURE);
	}

	public PercentageUnit getAbv()
	{
		return (PercentageUnit)getMetric(Metric.ABV);
	}

	public PercentageUnit getFermentability()
	{
		return (PercentageUnit)getMetric(Metric.FERMENTABILITY);
	}

	public CarbonationUnit getCarbonation()
	{
		return (CarbonationUnit)getMetric(Metric.CARBONATION);
	}

	public WeightUnit getExtract()
	{
		return (WeightUnit)getMetric(Metric.EXTRACT);
	}

	public PhUnit getPh()
	{
		return (PhUnit)getMetric(Metric.PH);
	}

	public WeightUnit getAlphaAcidsMg()
	{
		return (WeightUnit)getMetric(Metric.ALPHA_ACIDS_MG);
	}

	public WeightUnit getIsoAlphaAcidsMg()
	{
		return (WeightUnit)getMetric(Metric.ISO_ALPHA_ACIDS_MG);
	}

	/*-------------------------------------------------------------------------*/

	public void setVolume(VolumeUnit volume)
	{
		this.setMetric(Metric.VOLUME, volume);
	}

	public void setTemperature(TemperatureUnit temperature)
	{
		this.setMetric(Metric.TEMPERATURE, temperature);
	}

	public void setGravity(DensityUnit gravity)
	{
		this.setMetric(Metric.GRAVITY, gravity);
	}

	public void setOriginalGravity(DensityUnit gravity)
	{
		this.setMetric(Metric.ORIGINAL_GRAVITY, gravity);
	}

	public void setColour(ColourUnit colour)
	{
		this.setMetric(Metric.COLOUR, colour);
	}

	public void setBitterness(HopBitternessFormula formula, BitternessUnit bitterness)
	{
		setMetric(formula.toMetric(), bitterness);
	}

	public void setBitterness(BitternessUnit bitterness)
	{
		BitternessVolumes.setAllReported(this, bitterness);
	}

	public void setAbv(PercentageUnit abv)
	{
		this.setMetric(Metric.ABV, abv);
	}

	public void setFermentability(PercentageUnit fermentability)
	{
		this.setMetric(Metric.FERMENTABILITY, fermentability);
	}

	public void setCarbonation(CarbonationUnit carbonation)
	{
		this.setMetric(Metric.CARBONATION, carbonation);
	}

	public void setExtract(WeightUnit extract)
	{
		this.setMetric(Metric.EXTRACT, extract);
	}

	public void setPh(PhUnit ph)
	{
		this.setMetric(Metric.PH, ph);
	}

	public void setAlphaAcidsMg(WeightUnit alphaAcidsMg)
	{
		this.setMetric(Metric.ALPHA_ACIDS_MG, alphaAcidsMg);
	}

	public void setIsoAlphaAcidsMg(WeightUnit isoAlphaAcidsMg)
	{
		this.setMetric(Metric.ISO_ALPHA_ACIDS_MG, isoAlphaAcidsMg);
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * @return a deep clone of this volume
	 */
	public Volume clone()
	{
		return new Volume(
			name,
			type,
			new HashMap<>(metrics),
			new ArrayList<>(ingredientAdditions));
	}

	/*-------------------------------------------------------------------------*/

	public String describe()
	{
		List<HopBitternessFormula> reported =
			Settings.parseReportedFormulas(Database.getInstance().getSettings());
		String bitternessLines = BitternessVolumes.formatReportedLines(this, reported);

		double t = getTemperature() == null ? Double.NaN : getTemperature().get(Quantity.Unit.CELSIUS);
		double v = getVolume() == null ? Double.NaN : getVolume().get(Quantity.Unit.LITRES);
		double g = getGravity() == null ? Double.NaN : getGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY);
		double c = getColour() == null ? Double.NaN : getColour().get(Quantity.Unit.SRM);
		double og = getOriginalGravity() == null ? Double.NaN : getOriginalGravity().get(DensityUnit.Unit.SPECIFIC_GRAVITY);
		double abv = getAbv() == null ? 0D : getAbv().get() * 100;
		double carb = getCarbonation() == null ? Double.NaN : getCarbonation().get(DensityUnit.Unit.VOLUMES);
		double ph = getPh() == null ? Double.NaN : getPh().get(Quantity.Unit.PH);
		double f = getFermentability() == null ? Double.NaN : getFermentability().get(Quantity.Unit.PERCENTAGE_DISPLAY);
		double alphaMg = getAlphaAcidsMg() == null ? Double.NaN : getAlphaAcidsMg().get(Quantity.Unit.MILLIGRAMS);
		double isoAlphaMg = getIsoAlphaAcidsMg() == null ? Double.NaN : getIsoAlphaAcidsMg().get(Quantity.Unit.MILLIGRAMS);

		switch (type)
		{
			case MASH:
				return StringUtils.getProcessString("volumes.mash.format",
					getName(),
					getType().toString(),
					t,
					v,
					g,
					c,
					bitternessLines,
					alphaMg,
					isoAlphaMg,
					abv,
					carb,
					ph);

			case WORT:
				// Name: '%s'\n
				// Type: %s\n
				// Volume: %.1fL\n
				// Temperature: %.1fC\n
				// Gravity: %.3f\n
				// Fermentability: %.3f\n
				// Colour: %.1f SRM\n
				// %s\n
				// Alpha acids: %.0f mg\n
				// Iso-alpha: %.0f mg\n
				// ABV: %.1f%%\n
				// Carbonation: %.2f vols\n
				// pH: %.2f
				return StringUtils.getProcessString("volumes.wort.format",
					getName(),
					getType().toString(),
					v,
					t,
					g,
					f,
					c,
					bitternessLines,
					alphaMg,
					isoAlphaMg,
					abv,
					carb,
					ph);

			case BEER:
				return StringUtils.getProcessString("volumes.beer.format",
					getName(),
					getType().toString(),
					getStyle() == null ? "-" : getStyle().getName(),
					v,
					og,
					g,
					c,
					bitternessLines,
					alphaMg,
					isoAlphaMg,
					abv,
					carb,
					ph);
			default:
				throw new BrewdayException("invalid " + type);
		}
	}

	/*-------------------------------------------------------------------------*/
	public enum Metric
	{
		NAME,
		VOLUME,
		TEMPERATURE,
		GRAVITY,
		COLOUR,
		BITTERNESS_TINSETH,
		BITTERNESS_TINSETH_BEERSMITH,
		BITTERNESS_RAGER,
		BITTERNESS_GARETZ,
		BITTERNESS_DANIELS,
		BITTERNESS_MIBU,
		BITTERNESS_BREWDAY,
		ABV,
		FERMENTABILITY,
		ORIGINAL_GRAVITY,
		CARBONATION,
		EXTRACT,
		PH,
		ALPHA_ACIDS_MG,
		ISO_ALPHA_ACIDS_MG
	}

	/*-------------------------------------------------------------------------*/
	public enum Type
	{
		MASH("Mash", 1),
		WORT("Wort", 2),
		BEER("Beer", 3);

		private String name;
		private int sortOrder;

		Type(String name, int sortOrder)
		{
			this.name = name;
			this.sortOrder = sortOrder;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public int getSortOrder()
		{
			return sortOrder;
		}
	}
}
