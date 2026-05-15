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

package mclachlan.brewday.ui.swing.widgets;

import java.util.ArrayList;
import java.util.List;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.Boil;
import mclachlan.brewday.process.Cool;
import mclachlan.brewday.process.Ferment;
import mclachlan.brewday.process.Heat;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.MashInfusion;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Split;
import mclachlan.brewday.process.Stand;
import mclachlan.brewday.process.Volumes;
import mclachlan.brewday.recipe.IngredientAddition;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * HTML tooltips for process graph nodes: step properties and ingredient additions.
 * Volume names and state appear on edge tooltips, not here.
 */
public final class ProcessStepGraphTooltipBuilder
{
	private ProcessStepGraphTooltipBuilder()
	{
	}

	/*-------------------------------------------------------------------------*/
	public static String build(ProcessStep step, Volumes volumes)
	{
		if (step == null)
		{
			return null;
		}

		List<String> props = formatProperties(step, volumes);
		List<IngredientAddition> additions = step.getIngredientAdditions();

		StringBuilder html = new StringBuilder("<html>");
		html.append("<b>").append(escapeHtml(step.getName())).append("</b>");
		html.append(" &mdash; ").append(escapeHtml(step.getType().name()));

		for (String line : props)
		{
			html.append("<br>").append(escapeHtml(line));
		}

		if (additions != null && !additions.isEmpty())
		{
			html.append("<ul>");
			for (IngredientAddition ia : additions)
			{
				html.append("<li>").append(escapeHtml(ia.toString())).append("</li>");
			}
			html.append("</ul>");
		}

		html.append("</html>");
		return html.toString();
	}

	/*-------------------------------------------------------------------------*/
	static List<String> formatProperties(ProcessStep step, Volumes volumes)
	{
		List<String> lines = new ArrayList<>();

		switch (step.getType())
		{
			case MASH -> formatMash((Mash)step, lines);
			case MASH_INFUSION -> formatMashInfusion((MashInfusion)step, lines);
			case LAUTER, BATCH_SPARGE, DILUTE, COMBINE -> { }
			case BOIL -> formatBoil((Boil)step, lines);
			case FERMENT -> formatFerment((Ferment)step, lines);
			case SPLIT -> formatSplit((Split)step, lines);
			case PACKAGE -> formatPackage((PackageStep)step, lines);
			case HEAT -> formatHeat((Heat)step, lines);
			case COOL -> formatCool((Cool)step, lines);
			case STAND -> formatStand((Stand)step, lines);
			default ->
			{
				if (volumes != null)
				{
					String d = step.describe(volumes);
					if (d != null && !d.isBlank())
					{
						lines.add(d);
					}
				}
			}
		}

		return lines;
	}

	/*-------------------------------------------------------------------------*/
	private static void formatMash(Mash step, List<String> lines)
	{
		addQuantityLine(lines, "mash.grain.temp", step.getGrainTemp(), Quantity.Unit.CELSIUS);
		addQuantityLine(lines, "mash.duration", step.getDuration(), Quantity.Unit.MINUTES);
		if (step.getMashTemp() != null)
		{
			addQuantityLine(lines, "mash.temp", step.getMashTemp(), Quantity.Unit.CELSIUS);
		}
		if (step.getMashPh() != null)
		{
			addQuantityLine(lines, "mash.ph", step.getMashPh(), Quantity.Unit.PH);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void formatMashInfusion(MashInfusion step, List<String> lines)
	{
		if (step.getMashTemp() != null)
		{
			addQuantityLine(lines, "mash.temp", step.getMashTemp(), Quantity.Unit.CELSIUS);
		}
		addQuantityLine(lines, "mash.infusion.ramp.time", step.getRampTime(), Quantity.Unit.MINUTES);
		addQuantityLine(lines, "mash.infusion.duration", step.getStandTime(), Quantity.Unit.MINUTES);
	}

	/*-------------------------------------------------------------------------*/
	private static void formatBoil(Boil step, List<String> lines)
	{
		if (step.getTimeToBoil() != null)
		{
			addQuantityLine(lines, "boil.time.to.boil", step.getTimeToBoil(), Quantity.Unit.MINUTES);
		}
		addQuantityLine(lines, "boil.duration", step.getDuration(), Quantity.Unit.MINUTES);
		lines.add(getUiString("boil.remove.trub.and.chiller.loss") + ": " +
			(step.isRemoveTrubAndChillerLoss() ? getUiString("recipe.process.graph.yes")
				: getUiString("recipe.process.graph.no")));
	}

	/*-------------------------------------------------------------------------*/
	private static void formatFerment(Ferment step, List<String> lines)
	{
		addQuantityLine(lines, "ferment.temp", step.getTemperature(), Quantity.Unit.CELSIUS);
		addQuantityLine(lines, "ferment.duration", step.getDuration(), Quantity.Unit.DAYS);
		lines.add(getUiString("ferment.remove.trub.and.chiller.loss") + ": " +
			(step.isRemoveTrubAndChillerLoss() ? getUiString("recipe.process.graph.yes")
				: getUiString("recipe.process.graph.no")));
		if (step.getEstimatedFinalGravity() != null)
		{
			addQuantityLine(lines, "ferment.fg", step.getEstimatedFinalGravity(), Quantity.Unit.PLATO);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void formatSplit(Split step, List<String> lines)
	{
		if (step.getSplitType() == Split.Type.PERCENTAGE && step.getSplitPercent() != null)
		{
			addQuantityLine(lines, "split.by.percentage", step.getSplitPercent(),
				Quantity.Unit.PERCENTAGE_DISPLAY);
		}
		else if (step.getSplitVolume() != null)
		{
			addQuantityLine(lines, "split.by.volume", step.getSplitVolume(), Quantity.Unit.LITRES);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void formatPackage(PackageStep step, List<String> lines)
	{
		if (step.getStyleId() != null)
		{
			addLabelled(lines, "recipe.style", step.getStyleId());
		}
		if (step.getPackagingType() != null)
		{
			addLabelled(lines, "package.type", step.getPackagingType().name());
		}
		if (step.getForcedCarbonation() != null)
		{
			addQuantityLine(lines, "package.forced.carbonation", step.getForcedCarbonation(),
				Quantity.Unit.VOLUMES);
		}
		addQuantityLine(lines, "package.loss", step.getPackagingLoss(), Quantity.Unit.LITRES);
	}

	/*-------------------------------------------------------------------------*/
	private static void formatHeat(Heat step, List<String> lines)
	{
		addQuantityLine(lines, "heat.target.temp", step.getTargetTemp(), Quantity.Unit.CELSIUS);
		addQuantityLine(lines, "heat.ramp.time", step.getRampTime(), Quantity.Unit.MINUTES);
		addQuantityLine(lines, "heat.stand.time", step.getStandTime(), Quantity.Unit.MINUTES);
	}

	/*-------------------------------------------------------------------------*/
	private static void formatCool(Cool step, List<String> lines)
	{
		addQuantityLine(lines, "cool.target.temp", step.getTargetTemp(), Quantity.Unit.CELSIUS);
	}

	/*-------------------------------------------------------------------------*/
	private static void formatStand(Stand step, List<String> lines)
	{
		addQuantityLine(lines, "stand.duration", step.getDuration(), Quantity.Unit.MINUTES);
	}

	/*-------------------------------------------------------------------------*/
	private static void addQuantityLine(List<String> lines, String labelKey,
		mclachlan.brewday.math.Quantity q, Quantity.Unit unit)
	{
		if (q != null)
		{
			addLabelled(lines, labelKey, q.describe(unit));
		}
	}

	/*-------------------------------------------------------------------------*/
	private static void addLabelled(List<String> lines, String labelKey, String value)
	{
		if (value != null && !value.isBlank())
		{
			lines.add(getUiString(labelKey) + ": " + value);
		}
	}

	/*-------------------------------------------------------------------------*/
	private static String escapeHtml(String s)
	{
		if (s == null)
		{
			return "";
		}
		return s.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}
}
