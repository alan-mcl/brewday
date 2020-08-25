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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.equipment;

import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.*;

/**
 *
 */
public class EquipmentProfile implements V2DataObject
{
	private String name;
	private String description;

	/** mash tun capacity in ml */
	private VolumeUnit mashTunVolume;

	/** weight of the mash tun in g */
	private WeightUnit mashTunWeight;

	/** Mash efficiency in % */
	private PercentageUnit mashEfficiency;

	/**
	 * Specific heat of the mash tun which is usually a function of the material
	 * it is made of.  Typical ranges are 0.1-0.25 for metal and 0.2-0.5 for
	 * plastic materials. In Cal/gram-degC.
	 */
	private ArbitraryPhysicalQuantity mashTunSpecificHeat;

	/** boil kettle capacity in ml */
	private VolumeUnit boilKettleVolume;

	/** boil element rating in kW */
	private PowerUnit boilElementPower;

	/** % of wort lost per hour of boil */
	private PercentageUnit boilEvapourationRate;

	/** Hop utilisation % in the boil */
	private PercentageUnit hopUtilisation;

	/** fermenter capacity in ml */
	private VolumeUnit fermenterVolume;

	/**
	 * Amount lost to the lauter tun and equipment associated with the lautering
	 * process. In ml.
	 */
	private VolumeUnit lauterLoss;

	/**
	 * The amount of wort normally lost during transition from the boiler to the
	 * fermentation vessel.  Includes both unusable wort due to trub and wort
	 * lost to the chiller and transfer systems. In ml.
	 */
	private VolumeUnit trubAndChillerLoss;

	// used to support BeerXML only
	private double topUpWater;
	private double topUpKettle;
	private VolumeUnit batchSize;

	/*-------------------------------------------------------------------------*/
	public EquipmentProfile()
	{
	}

	/*-------------------------------------------------------------------------*/
	public EquipmentProfile(
		String name,
		String description,
		double mashEfficiency,
		double mashTunVolume,
		double mashTunWeight,
		double mashTunSpecificHeat,
		double boilKettleVolume,
		double boilEvapourationRate,
		double boilElementPower,
		double hopUtilisation,
		double fermenterVolume,
		double lauterLoss,
		double trubAndChillerLoss)
	{
		this.setName(name);
		this.setDescription(description);
		this.setMashTunVolume(new VolumeUnit(mashTunVolume));
		this.setMashTunWeight(new WeightUnit(mashTunWeight));
		this.setMashTunSpecificHeat(new ArbitraryPhysicalQuantity(mashTunSpecificHeat, Quantity.Unit.JOULE_PER_KG_CELSIUS));
		this.setBoilKettleVolume(new VolumeUnit(boilKettleVolume));
		this.setBoilEvapourationRate(new PercentageUnit(boilEvapourationRate));
		this.setBoilElementPower(new PowerUnit(boilElementPower));
		this.setHopUtilisation(new PercentageUnit(hopUtilisation));
		this.setFermenterVolume(new VolumeUnit(fermenterVolume));
		this.setLauterLoss(new VolumeUnit(lauterLoss));
		this.setTrubAndChillerLoss(new VolumeUnit(trubAndChillerLoss));
		this.setMashEfficiency(new PercentageUnit(mashEfficiency));
	}

	/*-------------------------------------------------------------------------*/
	public EquipmentProfile(EquipmentProfile other)
	{
		this.setName(other.name);
		this.setDescription(other.description);
		this.setMashTunVolume(other.mashTunVolume);
		this.setMashTunWeight(other.mashTunWeight);
		this.setMashTunSpecificHeat(other.mashTunSpecificHeat);
		this.setBoilKettleVolume(other.boilKettleVolume);
		this.setBoilEvapourationRate(other.boilEvapourationRate);
		this.setBoilElementPower(other.boilElementPower);
		this.setHopUtilisation(other.hopUtilisation);
		this.setFermenterVolume(other.fermenterVolume);
		this.setLauterLoss(other.lauterLoss);
		this.setTrubAndChillerLoss(other.trubAndChillerLoss);
		this.setMashEfficiency(other.mashEfficiency);
	}

	/*-------------------------------------------------------------------------*/
	public EquipmentProfile(String name)
	{
		this.name = name;
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
			",\n boilElementPower=" + getBoilElementPower() +
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
	public VolumeUnit getMashTunVolume()
	{
		return mashTunVolume;
	}

	public void setMashTunVolume(VolumeUnit mashTunVolume)
	{
		this.mashTunVolume = mashTunVolume;
	}

	/** weight of the mash tun in g */
	public WeightUnit getMashTunWeight()
	{
		return mashTunWeight;
	}

	public void setMashTunWeight(WeightUnit mashTunWeight)
	{
		this.mashTunWeight = mashTunWeight;
	}

	/**
	 * Specific heat of the mash tun which is usually a function of the material
	 * it is made of.  Typical ranges are 0.1-0.25 for metal and 0.2-0.5 for
	 * plastic materials. In Cal/gram-degC.
	 */
	public ArbitraryPhysicalQuantity getMashTunSpecificHeat()
	{
		return mashTunSpecificHeat;
	}

	public void setMashTunSpecificHeat(ArbitraryPhysicalQuantity mashTunSpecificHeat)
	{
		this.mashTunSpecificHeat = new ArbitraryPhysicalQuantity(
			mashTunSpecificHeat.get(), Quantity.Unit.JOULE_PER_KG_CELSIUS);
	}

	/** boil kettle capacity in ml */
	public VolumeUnit getBoilKettleVolume()
	{
		return boilKettleVolume;
	}

	public void setBoilKettleVolume(VolumeUnit boilKettleVolume)
	{
		this.boilKettleVolume = boilKettleVolume;
	}

	public PowerUnit getBoilElementPower()
	{
		return boilElementPower;
	}

	public void setBoilElementPower(PowerUnit boilElementPower)
	{
		this.boilElementPower = boilElementPower;
	}

	/** % of wort lost per hour of boil */
	public PercentageUnit getBoilEvapourationRate()
	{
		return boilEvapourationRate;
	}

	public void setBoilEvapourationRate(PercentageUnit boilEvapourationRate)
	{
		this.boilEvapourationRate = boilEvapourationRate;
	}

	/** Hop utilisation % in the boil */
	public PercentageUnit getHopUtilisation()
	{
		return hopUtilisation;
	}

	public void setHopUtilisation(PercentageUnit hopUtilisation)
	{
		this.hopUtilisation = hopUtilisation;
	}

	/** fermenter capacity in ml */
	public VolumeUnit getFermenterVolume()
	{
		return fermenterVolume;
	}

	public void setFermenterVolume(VolumeUnit fermenterVolume)
	{
		this.fermenterVolume = fermenterVolume;
	}

	/**
	 * Amount lost to the lauter tun and equipment associated with the lautering
	 * process.
	 */
	public VolumeUnit getLauterLoss()
	{
		return lauterLoss;
	}

	public void setLauterLoss(VolumeUnit lauterLoss)
	{
		this.lauterLoss = lauterLoss;
	}

	/**
	 * The amount of wort normally lost during transition from the boiler to the
	 * fermentation vessel.  Includes both unusable wort due to trub and wort
	 * lost to the chiller and transfer systems.
	 */
	public VolumeUnit getTrubAndChillerLoss()
	{
		return trubAndChillerLoss;
	}

	public void setTrubAndChillerLoss(VolumeUnit trubAndChillerLoss)
	{
		this.trubAndChillerLoss = trubAndChillerLoss;
	}

	/**
	 * @return
	 * 	Mash efficiency in %
	 */
	public PercentageUnit getMashEfficiency()
	{
		return mashEfficiency;
	}

	public void setMashEfficiency(PercentageUnit mashEfficiency)
	{
		this.mashEfficiency = mashEfficiency;
	}

	public void setTopUpWater(double topUpWater)
	{
		this.topUpWater = topUpWater;
	}

	public double getTopUpWater()
	{
		return topUpWater;
	}

	public void setTopUpKettle(double topUpKettle)
	{
		this.topUpKettle = topUpKettle;
	}

	public double getTopUpKettle()
	{
		return topUpKettle;
	}

	public VolumeUnit getBatchSize()
	{
		return batchSize;
	}

	public void setBatchSize(VolumeUnit batchSize)
	{
		this.batchSize = batchSize;
	}
}
