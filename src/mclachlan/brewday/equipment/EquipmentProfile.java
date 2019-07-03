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
	 * process.
	 */
	private double lauterLoss;

	/**
	 * The amount of wort normally lost during transition from the boiler to the
	 * fermentation vessel.  Includes both unusable wort due to trub and wort
	 * lost to the chiller and transfer systems.
	 */
	private double trubAndChillerLoss;

	/*-------------------------------------------------------------------------*/
	public EquipmentProfile()
	{
	}

	public EquipmentProfile(
		String name,
		String description,
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
		this.name = name;
		this.description = description;
		this.mashTunVolume = mashTunVolume;
		this.mashTunWeight = mashTunWeight;
		this.mashTunSpecificHeat = mashTunSpecificHeat;
		this.boilKettleVolume = boilKettleVolume;
		this.boilEvapourationRate = boilEvapourationRate;
		this.hopUtilisation = hopUtilisation;
		this.fermenterVolume = fermenterVolume;
		this.lauterLoss = lauterLoss;
		this.trubAndChillerLoss = trubAndChillerLoss;
	}

	/*-------------------------------------------------------------------------*/

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

	public double getMashTunVolume()
	{
		return mashTunVolume;
	}

	public void setMashTunVolume(double mashTunVolume)
	{
		this.mashTunVolume = mashTunVolume;
	}

	public double getMashTunWeight()
	{
		return mashTunWeight;
	}

	public void setMashTunWeight(double mashTunWeight)
	{
		this.mashTunWeight = mashTunWeight;
	}

	public double getMashTunSpecificHeat()
	{
		return mashTunSpecificHeat;
	}

	public void setMashTunSpecificHeat(double mashTunSpecificHeat)
	{
		this.mashTunSpecificHeat = mashTunSpecificHeat;
	}

	public double getBoilKettleVolume()
	{
		return boilKettleVolume;
	}

	public void setBoilKettleVolume(double boilKettleVolume)
	{
		this.boilKettleVolume = boilKettleVolume;
	}

	public double getBoilEvapourationRate()
	{
		return boilEvapourationRate;
	}

	public void setBoilEvapourationRate(double boilEvapourationRate)
	{
		this.boilEvapourationRate = boilEvapourationRate;
	}

	public double getHopUtilisation()
	{
		return hopUtilisation;
	}

	public void setHopUtilisation(double hopUtilisation)
	{
		this.hopUtilisation = hopUtilisation;
	}

	public double getFermenterVolume()
	{
		return fermenterVolume;
	}

	public void setFermenterVolume(double fermenterVolume)
	{
		this.fermenterVolume = fermenterVolume;
	}

	public double getLauterLoss()
	{
		return lauterLoss;
	}

	public void setLauterLoss(double lauterLoss)
	{
		this.lauterLoss = lauterLoss;
	}

	public double getTrubAndChillerLoss()
	{
		return trubAndChillerLoss;
	}

	public void setTrubAndChillerLoss(double trubAndChillerLoss)
	{
		this.trubAndChillerLoss = trubAndChillerLoss;
	}

	@Override
	public String toString()
	{
		return "EquipmentProfile{" +
			"name='" + name + '\'' +
			",\n description='" + description + '\'' +
			",\n mashTunVolume=" + mashTunVolume +
			",\n mashTunWeight=" + mashTunWeight +
			",\n mashTunSpecificHeat=" + mashTunSpecificHeat +
			",\n boilKettleVolume=" + boilKettleVolume +
			",\n boilEvapourationRate=" + boilEvapourationRate +
			",\n hopUtilisation=" + hopUtilisation +
			",\n fermenterVolume=" + fermenterVolume +
			",\n lauterLoss=" + lauterLoss +
			",\n trubAndChillerLoss=" + trubAndChillerLoss +
			'}';
	}
}
