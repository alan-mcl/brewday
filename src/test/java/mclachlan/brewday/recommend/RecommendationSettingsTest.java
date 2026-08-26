package mclachlan.brewday.recommend;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import mclachlan.brewday.Settings;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RecommendationSettingsTest
{
	@Test
	public void missingKeysUseDefaults()
	{
		RecommendationSettings settings = RecommendationSettings.from(new Settings(Map.of()));
		RecommendationSettings defaults = RecommendationSettings.defaults();

		assertEquals(defaults.getMinGroupSize(), settings.getMinGroupSize());
		assertEquals(defaults.getMaxPerGroup(), settings.getMaxPerGroup());
		assertEquals(defaults.getHemisphere(), settings.getHemisphere());
		assertEquals(defaults.getSeasonalLeadMonths(), settings.getSeasonalLeadMonths());
		assertEquals(defaults.getBestInventoryMinMatch(), settings.getBestInventoryMinMatch());
		assertEquals(defaults.getDueRepeatGapMonths(), settings.getDueRepeatGapMonths());
		assertEquals(defaults.getStyleRevisitGapMonths(), settings.getStyleRevisitGapMonths());
		assertEquals(defaults.getSomethingDifferentMinContrast(), settings.getSomethingDifferentMinContrast(), 0.001D);
		assertEquals(defaults.getNeverBrewedMinMatch(), settings.getNeverBrewedMinMatch());
		assertEquals(defaults.getForgottenGapMonths(), settings.getForgottenGapMonths());
		assertEquals(defaults.getUseItUpMinMatch(), settings.getUseItUpMinMatch());
		assertEquals(defaults.getOnePurchaseMinMatch(), settings.getOnePurchaseMinMatch());
		assertEquals(defaults.getStretchMinContrast(), settings.getStretchMinContrast(), 0.001D);
		assertEquals(defaults.getStretchMinMatch(), settings.getStretchMinMatch());
	}

	@Test
	public void invalidKeysFallBackToDefaults()
	{
		Map<String, String> raw = new HashMap<>();
		raw.put(Settings.RECOMMEND_MIN_GROUP_SIZE, "x");
		raw.put(Settings.RECOMMEND_MAX_GROUP_SIZE, "9");
		raw.put(Settings.RECOMMEND_BEST_INVENTORY_MIN_MATCH, "999");
		raw.put(Settings.RECOMMEND_DUE_REPEAT_GAP_MONTHS, "-1");
		raw.put(Settings.RECOMMEND_HEMISPHERE, "invalid");
		raw.put(Settings.RECOMMEND_SEASONAL_LEAD_MONTHS, "99");

		RecommendationSettings settings = RecommendationSettings.from(new Settings(raw));

		assertEquals(1, settings.getMinGroupSize());
		assertEquals(3, settings.getMaxPerGroup());
		assertEquals(RecommendationSettings.Hemisphere.NORTHERN, settings.getHemisphere());
		assertEquals(6, settings.getSeasonalLeadMonths());
		assertEquals(100, settings.getBestInventoryMinMatch());
		assertEquals(1L, settings.getDueRepeatGapMonths());
	}

	@Test
	public void minGroupSizeClampedToMax()
	{
		Map<String, String> raw = new HashMap<>();
		raw.put(Settings.RECOMMEND_MIN_GROUP_SIZE, "3");
		raw.put(Settings.RECOMMEND_MAX_GROUP_SIZE, "1");

		RecommendationSettings settings = RecommendationSettings.from(new Settings(raw));

		assertEquals(1, settings.getMinGroupSize());
		assertEquals(1, settings.getMaxPerGroup());
	}

	@Test
	public void clearPersistedRemovesKeys()
	{
		Settings settings = new Settings(new HashMap<>());
		RecommendationSettings.defaults().persist(settings);
		RecommendationSettings.clearPersisted(settings);
		assertNull(settings.get(Settings.RECOMMEND_MIN_GROUP_SIZE));
		assertNull(settings.get(Settings.RECOMMEND_MAX_GROUP_SIZE));
		assertNull(settings.get(Settings.RECOMMEND_HEMISPHERE));
	}
}
