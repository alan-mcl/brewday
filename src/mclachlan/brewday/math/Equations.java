package mclachlan.brewday.math;

/**
 *
 */
public class Equations
{
	/**
	 * Calculates the new temperature of the body of fluid after an addition
	 * of some amount at a different temperature.
	 *
	 * @return
	 * 	New temp of the combined fluid volume.
	 */
	public static double calcNewFluidTemperature(
		double currentVolume,
		double currentTemperature,
		double volumeAddition,
		double tempAddition)
	{
		return ((currentVolume * currentTemperature * Const.SPECIFIC_HEAT_OF_WATER) +
			volumeAddition * tempAddition * Const.SPECIFIC_HEAT_OF_WATER)
		/
			(currentVolume * Const.SPECIFIC_HEAT_OF_WATER + volumeAddition * Const.SPECIFIC_HEAT_OF_WATER);
	}

	/**
	 * Calculates the gravity change when a volume change occurs.
	 *
	 * @return
	 * 	New gravity of the output volume.
	 */
	public static double calcGravityWithVolumeChange(
		double volumeIn,
		double gravityIn,
		double volumeOut)
	{
		return gravityIn * volumeIn / volumeOut;
	}

	/**
	 * Calculates the volume decrease due to cooling.
	 *
	 * @return
	 * 	The new volume
	 */
	public static double calcCoolingShrinkage(double volumeIn, double tempDecrease)
	{
		return volumeIn * (1 - (Const.COOLING_SHRINKAGE * tempDecrease));
	}

	/**
	 * Calculates the ABV change when a volume change occurs
	 *
	 * @return
	 * 	the new ABV
	 */
	public static double calcAbvWithVolumeChange(
		double volumeIn,
		double abvIn,
		double volumeOut)
	{
		return abvIn * volumeIn / volumeOut;
	}

	/**
	 * Calculates the ABV change when a gravity change occurs
	 * @return
	 * 	the new ABV
	 */
	public static double calcAvbWithGravityChange(
		double gravityIn,
		double gravityOut)
	{
		return (gravityIn - gravityOut) / gravityOut * Const.ABV_CONST;
	}
}
