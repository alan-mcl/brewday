package mclachlan.brewday.test;

import java.util.*;
import mclachlan.brewday.process.*;

/**
 *
 */
public class ProcessRunner
{
	public static void main(String[] args) throws Exception
	{

		List<ProcessStep> process = new ArrayList<ProcessStep>();

		process.add(new Boil("1", "boil 60m", null, 60D));
		process.add(new Stand("2", "hop stand 30m", null, 30D));
		process.add(new Dilute("3", "dilute to 20l", null, 20, 5));
		process.add(new Cool("4", "cool to 20C", null, 20));
		process.add(new Ferment("5", "ferment to 1010", null, 10));
		process.add(new Cool("6", "cold crash", null, 1));

		FluidVolume brew = new FluidVolume(15, 20, 55, 0);

		for (ProcessStep s : process)
		{
			System.out.println("brew = [" + brew + "]");
			System.out.println(s.getNumber()+". "+s.getName());
			brew = s.apply(brew);
		}

		System.out.println("brew = [" + brew + "]");
	}
}
