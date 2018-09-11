package mclachlan.brewday.process;

import java.util.*;
import mclachlan.brewday.ingredients.Grain;
import mclachlan.brewday.ingredients.GrainBill;
import mclachlan.brewday.ingredients.Water;
import mclachlan.brewday.math.Const;
import mclachlan.brewday.math.Convert;
import mclachlan.brewday.math.Equations;

/**
 *
 */
public class MashIn extends ProcessStep
{
	private String grainBillVol;
	private String waterVol;
	private double mashTemp;

	public MashIn(
		String name,
		String description,
		String outputVolume,
		String grainBillVol,
		String waterVol,
		double mashTemp)
	{
		super(name, description, null, outputVolume);

		this.grainBillVol = grainBillVol;
		this.waterVol = waterVol;
		this.mashTemp = mashTemp;
	}

	@Override
	public java.util.List<String> apply(Volumes v)
	{
		// no input volume needed

		GrainBill grainBill = (GrainBill)v.getVolume(grainBillVol);
		Water water = (Water)v.getVolume(waterVol);

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

		double colourOut = Equations.calcSrmMoreyFormula(grainBill, volumeOut);

		v.addVolume(
			getOutputVolume(),
			new MashVolume(
				volumeOut,
				grainBill,
				water,
				mashTemp,
				gravityOut,
				colourOut));

		ArrayList<String> result = new ArrayList<String>();
		result.add(getOutputVolume());
		return result;
	}

	@Override
	public String describe(Volumes v)
	{
		Water w = (Water)v.getVolume(waterVol);

		return String.format("Mash in %.1fL at %.1fC", w.getVolume()/1000, mashTemp);
	}
}
