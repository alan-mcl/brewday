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

package mclachlan.brewday.recommend;

import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.style.Style;

/**
 * Broad sensory/style axes derived from BJCP reference ranges.
 */
public final class StyleCharacteristics
{
	public enum HopProfile
	{
		MALT_FORWARD, BALANCED, HOPPY
	}

	public enum ColourProfile
	{
		PALE, AMBER, DARK
	}

	public enum StrengthProfile
	{
		SESSION, STANDARD, STRONG
	}

	public enum FermentationProfile
	{
		CLEAN, EXPRESSIVE
	}

	private final HopProfile hopProfile;
	private final ColourProfile colourProfile;
	private final StrengthProfile strengthProfile;
	private final FermentationProfile fermentationProfile;
	private final boolean modern;
	private final double seasonalLightness;
	private final String categoryNumber;

	private StyleCharacteristics(
		HopProfile hopProfile,
		ColourProfile colourProfile,
		StrengthProfile strengthProfile,
		FermentationProfile fermentationProfile,
		boolean modern,
		double seasonalLightness,
		String categoryNumber)
	{
		this.hopProfile = hopProfile;
		this.colourProfile = colourProfile;
		this.strengthProfile = strengthProfile;
		this.fermentationProfile = fermentationProfile;
		this.modern = modern;
		this.seasonalLightness = seasonalLightness;
		this.categoryNumber = categoryNumber;
	}

	public static StyleCharacteristics fromStyle(Style style)
	{
		if (style == null)
		{
			return neutral();
		}

		double ibu = midpoint(style.getIbuMin(), style.getIbuMax(), 30D);
		double srm = midpointColour(style.getColourMin(), style.getColourMax(), 10D);
		double abv = midpointPercent(style.getAbvMin(), style.getAbvMax(), 0.05D);

		HopProfile hop = classifyHop(ibu, srm, style);
		ColourProfile colour = classifyColour(srm);
		StrengthProfile strength = classifyStrength(abv);
		FermentationProfile fermentation = classifyFermentation(style);
		boolean modern = isModern(style);
		double seasonalLightness = computeSeasonalLightness(srm, abv, strength, style);

		return new StyleCharacteristics(
			hop,
			colour,
			strength,
			fermentation,
			modern,
			seasonalLightness,
			style.getCategoryNumber());
	}

	public static StyleCharacteristics neutral()
	{
		return ofProfiles(
			HopProfile.BALANCED,
			ColourProfile.AMBER,
			StrengthProfile.STANDARD,
			FermentationProfile.CLEAN,
			false,
			0.5D,
			null);
	}

	public static StyleCharacteristics ofProfiles(
		HopProfile hopProfile,
		ColourProfile colourProfile,
		StrengthProfile strengthProfile,
		FermentationProfile fermentationProfile,
		boolean modern,
		double seasonalLightness,
		String categoryNumber)
	{
		return new StyleCharacteristics(
			hopProfile,
			colourProfile,
			strengthProfile,
			fermentationProfile,
			modern,
			seasonalLightness,
			categoryNumber);
	}

	public HopProfile getHopProfile()
	{
		return hopProfile;
	}

	public ColourProfile getColourProfile()
	{
		return colourProfile;
	}

	public StrengthProfile getStrengthProfile()
	{
		return strengthProfile;
	}

	public FermentationProfile getFermentationProfile()
	{
		return fermentationProfile;
	}

	public boolean isModern()
	{
		return modern;
	}

	public double getSeasonalLightness()
	{
		return seasonalLightness;
	}

	public String getCategoryNumber()
	{
		return categoryNumber;
	}

	public double contrastScore(StyleCharacteristics other)
	{
		if (other == null)
		{
			return 0D;
		}
		double score = 0D;
		if (hopProfile != other.hopProfile)
		{
			score += hopProfile == HopProfile.HOPPY && other.hopProfile == HopProfile.MALT_FORWARD ? 2D : 1D;
		}
		if (colourProfile != other.colourProfile)
		{
			score += colourProfile == ColourProfile.DARK && other.colourProfile == ColourProfile.PALE ? 2D : 1D;
		}
		if (strengthProfile != other.strengthProfile)
		{
			score += 0.75D;
		}
		if (fermentationProfile != other.fermentationProfile)
		{
			score += 1D;
		}
		if (modern != other.modern)
		{
			score += 0.5D;
		}
		return score;
	}

	public String describeContrastFrom(StyleCharacteristics recent)
	{
		if (recent == null)
		{
			return "";
		}
		if (colourProfile == ColourProfile.DARK && recent.colourProfile == ColourProfile.PALE
			&& hopProfile == HopProfile.MALT_FORWARD && recent.hopProfile == HopProfile.HOPPY)
		{
			return RecommendationUiSupport.contrastDarkMaltFromHoppy();
		}
		if (hopProfile == HopProfile.HOPPY && recent.hopProfile == HopProfile.MALT_FORWARD)
		{
			return RecommendationUiSupport.contrastHoppierFromMalt();
		}
		if (colourProfile == ColourProfile.PALE && recent.colourProfile == ColourProfile.DARK)
		{
			return RecommendationUiSupport.contrastPaleFromDark();
		}
		if (fermentationProfile == FermentationProfile.EXPRESSIVE
			&& recent.fermentationProfile == FermentationProfile.CLEAN)
		{
			return RecommendationUiSupport.contrastExpressiveFromClean();
		}
		if (strengthProfile == StrengthProfile.SESSION && recent.strengthProfile == StrengthProfile.STRONG)
		{
			return RecommendationUiSupport.contrastSessionFromStrong();
		}
		return RecommendationUiSupport.contrastGeneric();
	}

	private static HopProfile classifyHop(double ibu, double srm, Style style)
	{
		double ratio = ibu / Math.max(1D, srm);
		if (ratio >= 4.5D)
		{
			return HopProfile.HOPPY;
		}
		if (ratio <= 2D)
		{
			return HopProfile.MALT_FORWARD;
		}
		String cat = style.getCategoryNumber();
		if (cat != null && (cat.startsWith("21") || cat.startsWith("22") || cat.startsWith("23")))
		{
			return HopProfile.HOPPY;
		}
		return HopProfile.BALANCED;
	}

	private static ColourProfile classifyColour(double srm)
	{
		if (srm < 12D)
		{
			return ColourProfile.PALE;
		}
		if (srm < 22D)
		{
			return ColourProfile.AMBER;
		}
		return ColourProfile.DARK;
	}

	private static StrengthProfile classifyStrength(double abv)
	{
		if (abv < 0.045D)
		{
			return StrengthProfile.SESSION;
		}
		if (abv > 0.07D)
		{
			return StrengthProfile.STRONG;
		}
		return StrengthProfile.STANDARD;
	}

	private static FermentationProfile classifyFermentation(Style style)
	{
		String cat = style.getCategoryNumber();
		if (cat == null)
		{
			return FermentationProfile.CLEAN;
		}
		if (cat.startsWith("24") || cat.startsWith("25") || cat.startsWith("26")
			|| cat.startsWith("28") || cat.startsWith("29") || cat.startsWith("10"))
		{
			return FermentationProfile.EXPRESSIVE;
		}
		if (style.getType() == Style.Type.LAGER)
		{
			return FermentationProfile.CLEAN;
		}
		return FermentationProfile.CLEAN;
	}

	private static boolean isModern(Style style)
	{
		String cat = style.getCategoryNumber();
		if (cat == null)
		{
			return false;
		}
		return cat.startsWith("21") || cat.startsWith("22") || cat.startsWith("23")
			|| cat.startsWith("27") || cat.startsWith("28");
	}

	private static double computeSeasonalLightness(
		double srm,
		double abv,
		StrengthProfile strength,
		Style style)
	{
		double score = 1D - Math.min(1D, srm / 40D);
		if (strength == StrengthProfile.SESSION)
		{
			score += 0.15D;
		}
		if (style.getType() == Style.Type.WHEAT || style.getType() == Style.Type.LAGER)
		{
			score += 0.1D;
		}
		if (abv > 0.08D)
		{
			score -= 0.15D;
		}
		return Math.max(0D, Math.min(1D, score));
	}

	private static double midpoint(
		mclachlan.brewday.math.BitternessUnit min,
		mclachlan.brewday.math.BitternessUnit max,
		double fallback)
	{
		if (min != null && max != null)
		{
			return (min.get() + max.get()) / 2D;
		}
		if (min != null)
		{
			return min.get();
		}
		if (max != null)
		{
			return max.get();
		}
		return fallback;
	}

	private static double midpointColour(ColourUnit min, ColourUnit max, double fallback)
	{
		if (min != null && max != null)
		{
			return (min.get() + max.get()) / 2D;
		}
		if (min != null)
		{
			return min.get();
		}
		if (max != null)
		{
			return max.get();
		}
		return fallback;
	}

	private static double midpointPercent(PercentageUnit min, PercentageUnit max, double fallback)
	{
		if (min != null && max != null)
		{
			return (min.get() + max.get()) / 2D;
		}
		if (min != null)
		{
			return min.get();
		}
		if (max != null)
		{
			return max.get();
		}
		return fallback;
	}
}
