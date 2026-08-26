package mclachlan.brewday.recommend;

import java.time.LocalDate;
import java.time.Month;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SeasonalLightnessSupportTest
{
	@Test
	public void northernAprilWithLeadTargetsSummerLightness()
	{
		double target = SeasonalLightnessSupport.targetLightness(
			LocalDate.of(2026, 4, 1),
			RecommendationSettings.Hemisphere.NORTHERN,
			2);
		assertEquals(0.75D, target, 0.001D);
	}

	@Test
	public void southernJuneIsDarkSeasonTarget()
	{
		assertEquals(0.25D, SeasonalLightnessSupport.lightnessForMonth(Month.JUNE, false), 0.001D);
	}

	@Test
	public void shoulderMonthsAreNeutral()
	{
		assertEquals(0.5D, SeasonalLightnessSupport.lightnessForMonth(Month.APRIL, true), 0.001D);
	}
}
