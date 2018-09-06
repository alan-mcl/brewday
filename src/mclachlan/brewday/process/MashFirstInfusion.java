package mclachlan.brewday.process;

import mclachlan.brewday.ingredients.Grain;
import mclachlan.brewday.ingredients.GrainBill;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.Convert;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class MashFirstInfusion extends ProcessStep
{
	private GrainBill grainBill;
	private Water water;
	private double mashTemp;

	public MashFirstInfusion(String number, String name,
		String description, String inputVolume,
		GrainBill grainBill, Water water, double mashTemp)
	{
		super(number, name, description, inputVolume);
		this.grainBill = grainBill;
		this.water = water;
		this.mashTemp = mashTemp;
	}

	@Override
	public void apply(Volumes v)
	{
		// no input volume needed

		// todo: account for different grains in the grain bill
		double volumeOut = Equations.calcMashVolume(grainBill.getGrainWeight(), water.getVolume());

		// source: https://byo.com/article/hitting-target-original-gravity-and-volume-advanced-homebrewing/
		double extractPoints = 0D;
		for (Grain g : grainBill.getGrains())
		{
			extractPoints += Convert.gramsToLbs(g.getWeight()) * g.getExtractPotential();
		}

		// todo: externalise mash efficiency
		double actualExtract = extractPoints * Const.MASH_EFFICIENCY;

		double gravityOut = actualExtract / Convert.mlToGallons(volumeOut);

		v.addVolume(
			getInputVolume(),
			new MashVolume(
				volumeOut,
				grainBill,
				water,
				mashTemp,
				gravityOut));
	}
}
