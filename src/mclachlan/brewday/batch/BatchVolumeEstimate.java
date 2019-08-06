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
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.batch;

import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.math.*;
import mclachlan.brewday.process.BeerVolume;
import mclachlan.brewday.process.MashVolume;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.process.WortVolume;

/**
 *
 */
public class BatchVolumeEstimate
{
	public static final String MEASUREMENTS_TEMPERATURE = "batch.measurements.temperature";
	public static final String MEASUREMENTS_VOLUME = "batch.measurements.volume";
	public static final String MEASUREMENTS_DENSITY = "batch.measurements.density";
	public static final String MEASUREMENTS_COLOUR = "batch.measurements.colour";


	private Volume estimateVolume;
	private Volume measuredVolume;
	private String metric;
	private Quantity estimated;
	private Quantity measured;

	public BatchVolumeEstimate(
		Volume estimateVolume,
		Volume measuredVolume,
		String metric,
		Quantity estimated,
		Quantity measured)
	{
		this.estimateVolume = estimateVolume;
		this.measuredVolume = measuredVolume;
		this.metric = metric;
		this.estimated = estimated;
		this.measured = measured;
	}

	public Volume getEstimateVolume()
	{
		return estimateVolume;
	}

	public void setEstimateVolume(Volume estimateVolume)
	{
		this.estimateVolume = estimateVolume;
	}

	public Volume getMeasuredVolume()
	{
		return measuredVolume;
	}

	public void setMeasuredVolume(Volume measuredVolume)
	{
		this.measuredVolume = measuredVolume;
	}

	public String getMetric()
	{
		return metric;
	}

	public void setMetric(String metric)
	{
		this.metric = metric;
	}

	public Quantity getEstimated()
	{
		return estimated;
	}

	public void setEstimated(Quantity estimated)
	{
		this.estimated = estimated;
	}

	public Quantity getMeasured()
	{
		return measured;
	}

	public void setMeasured(Quantity measured)
	{
		this.measured = measured;

		if (measured != null)
		{
			if (measuredVolume instanceof MashVolume)
			{
				if (MEASUREMENTS_VOLUME.equals(metric))
				{
					((MashVolume)measuredVolume).setVolume((VolumeUnit)measured);
				}
				else if (MEASUREMENTS_COLOUR.equals(metric))
				{
					((MashVolume)measuredVolume).setColour((ColourUnit)measured);
				}
				else if (MEASUREMENTS_DENSITY.equals(metric))
				{
					((MashVolume)measuredVolume).setGravity((DensityUnit)measured);
				}
				else if (MEASUREMENTS_TEMPERATURE.equals(metric))
				{
					((MashVolume)measuredVolume).setTemperature((TemperatureUnit)measured);
				}
				else
				{
					throw new BrewdayException("Invalid ["+metric+"]");
				}
			}
			else if (measuredVolume instanceof WortVolume)
			{
				if (MEASUREMENTS_VOLUME.equals(metric))
				{
					((WortVolume)measuredVolume).setVolume((VolumeUnit)measured);
				}
				else if (MEASUREMENTS_COLOUR.equals(metric))
				{
					((WortVolume)measuredVolume).setColour((ColourUnit)measured);
				}
				else if (MEASUREMENTS_DENSITY.equals(metric))
				{
					((WortVolume)measuredVolume).setGravity((DensityUnit)measured);
				}
				else if (MEASUREMENTS_TEMPERATURE.equals(metric))
				{
					((WortVolume)measuredVolume).setTemperature((TemperatureUnit)measured);
				}
				else
				{
					throw new BrewdayException("Invalid ["+metric+"]");
				}
			}
			else if (measuredVolume instanceof BeerVolume)
			{
				if (MEASUREMENTS_VOLUME.equals(metric))
				{
					((BeerVolume)measuredVolume).setVolume((VolumeUnit)measured);
				}
				else if (MEASUREMENTS_COLOUR.equals(metric))
				{
					((BeerVolume)measuredVolume).setColour((ColourUnit)measured);
				}
				else if (MEASUREMENTS_DENSITY.equals(metric))
				{
					((BeerVolume)measuredVolume).setGravity((DensityUnit)measured);
				}
				else if (MEASUREMENTS_TEMPERATURE.equals(metric))
				{
					((BeerVolume)measuredVolume).setTemperature((TemperatureUnit)measured);
				}
				else
				{
					throw new BrewdayException("Invalid ["+metric+"]");
				}
			}
		}
	}
}
