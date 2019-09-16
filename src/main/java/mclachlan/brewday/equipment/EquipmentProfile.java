package mclachlan.brewday.equipment;

import mclachlan.brewday.db.v2.V2DataObject;

/**
 *
 */
public class EquipmentProfile implements V2DataObject
{
	private String name;
	private String description;

	/** mash tun capacity in ml */
	private double mashTunVolume;

	/** weight of the mash tun in g */
	private double mashTunWeight;

	/** Mash efficiency in % */
	private double mashEfficiency;

	/**
	 * Specific heat of the mash tun which is usually a function of the material
	 * it is made of.  Typical ranges are 0.1-0.25 for metal and 0.2-0.5 for
	 * plastic materials. In Cal/gram-degC.
	 */
	private double mashTunSpecificHeat;

	/** boil kettle capacity in ml */
	private double boilKettleVolume;

	/** % of wort lost per hour of boil */
	private double boilEvapourationRate;

	/** Hop utilisation % in the boil */
	private double hopUtilisation;

	/** fermenter capacity in ml */
	private double fermenterVolume;

	/**
	 * Amount lost to the lauter tun and equipment associated with the lautering
	 * process. In ml.
	 */
	private double lauterLoss;

	/**
	 * The amount of wort normally lost during transition from the boiler to the
	 * fermentation vessel.  Includes both unusable wort due to trub and wort
	 * lost to the chiller and transfer systems. In ml.
	 */
	private double trubAndChillerLoss;

	/*-------------------------------------------------------------------------*/
	public EquipmentProfile()
	{
	}

	public EquipmentProfile(
		String name,
		String description,
		double mashEfficiency,
		double mashTunVolume,
		double mashTunWeight,
		double mashTunSpecificHeat,
		double boilKettleVolume,
		double boilEvapourationRate,
		double hopUtilisation,
		double fermenterVolume,
		double lauterLoss,
		double trubAndChillerLoss)
	{
		this.setName(name);
		this.setDescription(description);
		this.setMashTunVolume(mashTunVolume);
		this.setMashTunWeight(mashTunWeight);
		this.setMashTunSpecificHeat(mashTunSpecificHeat);
		this.setBoilKettleVolume(boilKettleVolume);
		this.setBoilEvapourationRate(boilEvapourationRate);
		this.setHopUtilisation(hopUtilisation);
		this.setFermenterVolume(fermenterVolume);
		this.setLauterLoss(lauterLoss);
		this.setTrubAndChillerLoss(trubAndChillerLoss);
		this.setMashEfficiency(mashEfficiency);
	}

	/*-------------------------------------------------------------------------*/


	@Override
	public String toString()
	{
		return "EquipmentProfile{" +
			"name='" + getName() + '\'' +
			",\n description='" + getDescription() + '\'' +
			",\n mashTunVolume=" + getMashTunVolume() +
			",\n mashTunWeight=" + getMashTunWeight() +
			",\n mashTunSpecificHeat=" + getMashTunSpecificHeat() +
			",\n boilKettleVolume=" + getBoilKettleVolume() +
			",\n boilEvapourationRate=" + getBoilEvapourationRate() +
			",\n hopUtilisation=" + getHopUtilisation() +
			",\n fermenterVolume=" + getFermenterVolume() +
			",\n lauterLoss=" + getLauterLoss() +
			",\n trubAndChillerLoss=" + getTrubAndChillerLoss() +
			'}';
	}

	@Override
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	/** mash tun capacity in ml */
	public double getMashTunVolume()
	{
		return mashTunVolume;
	}

	public void setMashTunVolume(double mashTunVolume)
	{
		this.mashTunVolume = mashTunVolume;
	}

	/** weight of the mash tun in g */
	public double getMashTunWeight()
	{
		return mashTunWeight;
	}

	public void setMashTunWeight(double mashTunWeight)
	{
		this.mashTunWeight = mashTunWeight;
	}

	/**
	 * Specific heat of the mash tun which is usually a function of the material
	 * it is made of.  Typical ranges are 0.1-0.25 for metal and 0.2-0.5 for
	 * plastic materials. In Cal/gram-degC.
	 */
	public double getMashTunSpecificHeat()
	{
		return mashTunSpecificHeat;
	}

	public void setMashTunSpecificHeat(double mashTunSpecificHeat)
	{
		this.mashTunSpecificHeat = mashTunSpecificHeat;
	}

	/** boil kettle capacity in ml */
	public double getBoilKettleVolume()
	{
		return boilKettleVolume;
	}

	public void setBoilKettleVolume(double boilKettleVolume)
	{
		this.boilKettleVolume = boilKettleVolume;
	}

	/** % of wort lost per hour of boil */
	public double getBoilEvapourationRate()
	{
		return boilEvapourationRate;
	}

	public void setBoilEvapourationRate(double boilEvapourationRate)
	{
		this.boilEvapourationRate = boilEvapourationRate;
	}

	/** Hop utilisation % in the boil */
	public double getHopUtilisation()
	{
		return hopUtilisation;
	}

	public void setHopUtilisation(double hopUtilisation)
	{
		this.hopUtilisation = hopUtilisation;
	}

	/** fermenter capacity in ml */
	public double getFermenterVolume()
	{
		return fermenterVolume;
	}

	public void setFermenterVolume(double fermenterVolume)
	{
		this.fermenterVolume = fermenterVolume;
	}

	/**
	 * Amount lost to the lauter tun and equipment associated with the lautering
	 * process.
	 */
	public double getLauterLoss()
	{
		return lauterLoss;
	}

	public void setLauterLoss(double lauterLoss)
	{
		this.lauterLoss = lauterLoss;
	}

	/**
	 * The amount of wort normally lost during transition from the boiler to the
	 * fermentation vessel.  Includes both unusable wort due to trub and wort
	 * lost to the chiller and transfer systems.
	 */
	public double getTrubAndChillerLoss()
	{
		return trubAndChillerLoss;
	}

	public void setTrubAndChillerLoss(double trubAndChillerLoss)
	{
		this.trubAndChillerLoss = trubAndChillerLoss;
	}

	/**
	 * @return
	 * 	Mash efficiency in %
	 */
	public double getMashEfficiency()
	{
		return mashEfficiency;
	}

	public void setMashEfficiency(double mashEfficiency)
	{
		this.mashEfficiency = mashEfficiency;
	}
}
