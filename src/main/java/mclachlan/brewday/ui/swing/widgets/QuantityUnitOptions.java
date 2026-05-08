package mclachlan.brewday.ui.swing.widgets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.math.Quantity;

import static mclachlan.brewday.math.Quantity.Unit.BAR;
import static mclachlan.brewday.math.Quantity.Unit.CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.CENTIMETRE;
import static mclachlan.brewday.math.Quantity.Unit.DAYS;
import static mclachlan.brewday.math.Quantity.Unit.EBC;
import static mclachlan.brewday.math.Quantity.Unit.FAHRENHEIT;
import static mclachlan.brewday.math.Quantity.Unit.FOOT;
import static mclachlan.brewday.math.Quantity.Unit.GRAMS;
import static mclachlan.brewday.math.Quantity.Unit.GRAMS_PER_LITRE;
import static mclachlan.brewday.math.Quantity.Unit.GU;
import static mclachlan.brewday.math.Quantity.Unit.HOURS;
import static mclachlan.brewday.math.Quantity.Unit.IBU;
import static mclachlan.brewday.math.Quantity.Unit.INCH;
import static mclachlan.brewday.math.Quantity.Unit.JOULE_PER_KG_CELSIUS;
import static mclachlan.brewday.math.Quantity.Unit.KELVIN;
import static mclachlan.brewday.math.Quantity.Unit.KILOGRAMS;
import static mclachlan.brewday.math.Quantity.Unit.KILOMETER;
import static mclachlan.brewday.math.Quantity.Unit.KILOWATT;
import static mclachlan.brewday.math.Quantity.Unit.KPA;
import static mclachlan.brewday.math.Quantity.Unit.LINTNER;
import static mclachlan.brewday.math.Quantity.Unit.LITRES;
import static mclachlan.brewday.math.Quantity.Unit.LOVIBOND;
import static mclachlan.brewday.math.Quantity.Unit.MEQ_PER_KILOGRAM;
import static mclachlan.brewday.math.Quantity.Unit.METRE;
import static mclachlan.brewday.math.Quantity.Unit.MILE;
import static mclachlan.brewday.math.Quantity.Unit.MILLILITRES;
import static mclachlan.brewday.math.Quantity.Unit.MILLIMETRE;
import static mclachlan.brewday.math.Quantity.Unit.MINUTES;
import static mclachlan.brewday.math.Quantity.Unit.OUNCES;
import static mclachlan.brewday.math.Quantity.Unit.PACKET_11_G;
import static mclachlan.brewday.math.Quantity.Unit.PH;
import static mclachlan.brewday.math.Quantity.Unit.PLATO;
import static mclachlan.brewday.math.Quantity.Unit.POUNDS;
import static mclachlan.brewday.math.Quantity.Unit.PPM;
import static mclachlan.brewday.math.Quantity.Unit.PSI;
import static mclachlan.brewday.math.Quantity.Unit.SECONDS;
import static mclachlan.brewday.math.Quantity.Unit.SPECIFIC_GRAVITY;
import static mclachlan.brewday.math.Quantity.Unit.SRM;
import static mclachlan.brewday.math.Quantity.Unit.US_FLUID_OUNCE;
import static mclachlan.brewday.math.Quantity.Unit.US_GALLON;
import static mclachlan.brewday.math.Quantity.Unit.VOLUMES;
import static mclachlan.brewday.math.Quantity.Unit.YARD;

/**
 * Builds the list of selectable {@link Quantity.Unit} values for a set of
 * {@link Quantity.Type}s, matching {@code QuantitySelectAndEditWidget} in the JFX UI.
 */
final class QuantityUnitOptions
{
	private QuantityUnitOptions()
	{
	}

	static List<Quantity.Unit> unitsForTypes(Quantity.Type... typesAllowed)
	{
		List<Quantity.Unit> unitOptions = new ArrayList<>();
		Set<Quantity.Type> set = new HashSet<>(Arrays.asList(typesAllowed));

		for (Quantity.Type type : set)
		{
			switch (type)
			{
				case WEIGHT:
					unitOptions.add(GRAMS);
					unitOptions.add(KILOGRAMS);
					unitOptions.add(OUNCES);
					unitOptions.add(POUNDS);
					unitOptions.add(PACKET_11_G);
					break;

				case LENGTH:
					unitOptions.add(MILLIMETRE);
					unitOptions.add(CENTIMETRE);
					unitOptions.add(METRE);
					unitOptions.add(KILOMETER);
					unitOptions.add(INCH);
					unitOptions.add(FOOT);
					unitOptions.add(YARD);
					unitOptions.add(MILE);
					break;

				case VOLUME:
					unitOptions.add(MILLILITRES);
					unitOptions.add(LITRES);
					unitOptions.add(US_FLUID_OUNCE);
					unitOptions.add(US_GALLON);
					break;

				case TEMPERATURE:
					unitOptions.add(CELSIUS);
					unitOptions.add(KELVIN);
					unitOptions.add(FAHRENHEIT);
					break;

				case FLUID_DENSITY:
					unitOptions.add(GU);
					unitOptions.add(SPECIFIC_GRAVITY);
					unitOptions.add(PLATO);
					break;

				case COLOUR:
					unitOptions.add(SRM);
					unitOptions.add(LOVIBOND);
					unitOptions.add(EBC);
					break;

				case BITTERNESS:
					unitOptions.add(IBU);
					break;

				case CARBONATION:
					unitOptions.add(GRAMS_PER_LITRE);
					unitOptions.add(VOLUMES);
					break;

				case PRESSURE:
					unitOptions.add(KPA);
					unitOptions.add(PSI);
					unitOptions.add(BAR);
					break;

				case TIME:
					unitOptions.add(SECONDS);
					unitOptions.add(MINUTES);
					unitOptions.add(HOURS);
					unitOptions.add(DAYS);
					break;

				case SPECIFIC_HEAT:
					unitOptions.add(JOULE_PER_KG_CELSIUS);
					break;

				case DIASTATIC_POWER:
					unitOptions.add(LINTNER);
					break;

				case POWER:
					unitOptions.add(KILOWATT);
					break;

				case OTHER:
					unitOptions.add(PPM);
					unitOptions.add(PH);
					unitOptions.add(MEQ_PER_KILOGRAM);
					break;

				default:
					throw new BrewdayException("invalid " + type);
			}
		}

		return unitOptions;
	}
}
