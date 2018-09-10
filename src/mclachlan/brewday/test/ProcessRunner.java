package mclachlan.brewday.test;

import java.util.*;
import mclachlan.brewday.ingredients.*;
import mclachlan.brewday.process.*;

/**
 *
 */
public class ProcessRunner
{
	public static void main(String[] args) throws Exception
	{
		List<ProcessStep> process = new ArrayList<ProcessStep>();

		ArrayList<Grain> grains = new ArrayList<Grain>();
		grains.add(new Grain(34, 10, 6000));
		GrainBill grainBill = new GrainBill(grains);
		Water water = new Water(15000, 70);
		ArrayList<Hop> hops = new ArrayList<Hop>();
		hops.add(new Hop(0.05, 20));
		HopAddition hopAddition = new HopAddition(hops);

		process.add(new MashFirstInfusion("0", "single infusion mash", null, "THE MASH", grainBill, water, 66));
		process.add(new MashOut("0.1", "mash out, drain", null, "THE MASH", "THE WORT", 3));

		Water spargeWater = new Water(10, 75);
		process.add(new BatchSparge("0.2", "batch sparge", null, "THE WORT", "THE MASH", spargeWater));

		process.add(new Boil("1", "boil 60m", null, "THE WORT", hopAddition, 60D));
		process.add(new Stand("2", "hop stand 30m", null, "THE WORT", 30D));
		process.add(new Dilute("3", "dilute to 20l", null, "THE WORT", 20000, 5));
		process.add(new Cool("4", "cool to 20C", null, "THE WORT", 20));
		process.add(new Ferment("5", "ferment to 1010", null, "THE WORT", 10));
		process.add(new Cool("6", "cold crash", null, "THE WORT", 1));

		Volumes brew = new Volumes();

		for (ProcessStep s : process)
		{
			System.out.println("brew = [" + brew + "]");
			System.out.println(s.getNumber()+". "+s.getName());
			s.apply(brew);
		}

		System.out.println("brew = [" + brew + "]");
	}
}
