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

package mclachlan.brewday.ui.swing;

import java.awt.Container;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
import mclachlan.brewday.math.*;
import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class EquipmentProfilePanel extends EditorPanel
{
	private JTextArea description;
	private JSpinner mashEfficiency;
	private JSpinner mashTunVolume;
	private JSpinner mashTunWeight;
	private JSpinner mashTunSpecificHeat;
	private JSpinner boilKettleVolume;
	private JSpinner boilEvapourationRate;
	private JSpinner hopUtilisation;
	private JSpinner fermenterVolume;
	private JSpinner lauterLoss;
	private JSpinner trubAndChillerLoss;

	public EquipmentProfilePanel(int dirtyFlag)
	{
		super(dirtyFlag);
	}

	@Override
	protected Container getEditControls()
	{
		JPanel result = new JPanel();
		result.setLayout(new MigLayout());

		mashEfficiency = new JSpinner(new SpinnerNumberModel(70D, 0.1D, 100D, 0.1D));
		mashEfficiency.addChangeListener(this);
		result.add(new JLabel(StringUtils.getUiString("equipment.mash.efficiency")));
		result.add(mashEfficiency, "wrap");

		mashTunVolume = new JSpinner(new SpinnerNumberModel(20D, 0.1D, 1000D, 0.1D));
		mashTunVolume.addChangeListener(this);
		result.add(new JLabel(StringUtils.getUiString("equipment.mash.tun.volume")));
		result.add(mashTunVolume, "wrap");

		mashTunWeight = new JSpinner(new SpinnerNumberModel(1D, 0.1D, 1000D, 0.1D));
		mashTunWeight.addChangeListener(this);
		result.add(new JLabel(StringUtils.getUiString("equipment.mash.tun.weight")));
		result.add(mashTunWeight, "wrap");

		mashTunSpecificHeat = new JSpinner(new SpinnerNumberModel(0.2D, 0.01D, 1D, 0.01D));
		mashTunSpecificHeat .addChangeListener(this);
		result.add(new JLabel(StringUtils.getUiString("equipment.mash.tun.specific.heat")));
		result.add(mashTunSpecificHeat, "wrap");

		boilKettleVolume = new JSpinner(new SpinnerNumberModel(20D, 0.1D, 1000D, 0.1D));
		boilKettleVolume.addChangeListener(this);
		result.add(new JLabel(StringUtils.getUiString("equipment.boil.kettle.volume")));
		result.add(boilKettleVolume, "wrap");

		boilEvapourationRate = new JSpinner(new SpinnerNumberModel(4D, 0.1D, 100D, 0.1D));
		boilEvapourationRate.addChangeListener(this);
		result.add(new JLabel(StringUtils.getUiString("equipment.evapouration")));
		result.add(boilEvapourationRate, "wrap");

		hopUtilisation = new JSpinner(new SpinnerNumberModel(100D, 0.1D, 100D, 0.1D));
		hopUtilisation.addChangeListener(this);
		result.add(new JLabel(StringUtils.getUiString("equipment.hop.utilisation")));
		result.add(hopUtilisation, "wrap");

		fermenterVolume = new JSpinner(new SpinnerNumberModel(20D, 0.1D, 1000D, 0.1D));
		fermenterVolume.addChangeListener(this);
		result.add(new JLabel(StringUtils.getUiString("equipment.fermenter.volume")));
		result.add(fermenterVolume, "wrap");

		lauterLoss = new JSpinner(new SpinnerNumberModel(2D, 0.1D, 1000D, 0.1D));
		lauterLoss.addChangeListener(this);
		result.add(new JLabel(StringUtils.getUiString("equipment.lauter.loss")));
		result.add(lauterLoss, "wrap");

		trubAndChillerLoss = new JSpinner(new SpinnerNumberModel(2D, 0.1D, 1000D, 0.1D));
		trubAndChillerLoss.addChangeListener(this);
		result.add(new JLabel(StringUtils.getUiString("equipment.trub.chiller.loss")));
		result.add(trubAndChillerLoss, "wrap");

		description = new JTextArea(8, 30);
		description.setWrapStyleWord(true);
		description.setLineWrap(true);
		description.addKeyListener(this);
		result.add(new JLabel(StringUtils.getUiString("equipment.desc")));
		result.add(new JScrollPane(description), "span");

		return result;
	}

	@Override
	public void refresh(String name)
	{
		refresh(Database.getInstance().getEquipmentProfiles().get(name));
	}

	private void refresh(EquipmentProfile equipmentProfile)
	{
		mashEfficiency.removeChangeListener(this);
		mashTunVolume.removeChangeListener(this);
		mashTunWeight.removeChangeListener(this);
		mashTunSpecificHeat.removeChangeListener(this);
		boilKettleVolume.removeChangeListener(this);
		boilEvapourationRate.removeChangeListener(this);
		hopUtilisation.removeChangeListener(this);
		fermenterVolume.removeChangeListener(this);
		lauterLoss.removeChangeListener(this);
		trubAndChillerLoss.removeChangeListener(this);
		description.removeKeyListener(this);

		mashEfficiency.setValue(equipmentProfile.getConversionEfficiency().get());
		mashTunVolume.setValue(equipmentProfile.getMashTunVolume().get(Quantity.Unit.LITRES));
		mashTunWeight.setValue(equipmentProfile.getMashTunWeight().get(Quantity.Unit.KILOGRAMS));
		mashTunSpecificHeat.setValue(equipmentProfile.getMashTunSpecificHeat().get());
		boilKettleVolume.setValue(equipmentProfile.getBoilKettleVolume().get(Quantity.Unit.LITRES));
		boilEvapourationRate.setValue(equipmentProfile.getBoilEvapourationRate().get());
		hopUtilisation.setValue(equipmentProfile.getHopUtilisation().get());
		fermenterVolume.setValue(equipmentProfile.getFermenterVolume().get(Quantity.Unit.LITRES));
		lauterLoss.setValue(equipmentProfile.getLauterLoss().get(Quantity.Unit.LITRES));
		trubAndChillerLoss.setValue(equipmentProfile.getTrubAndChillerLoss().get(Quantity.Unit.LITRES));
		description.setText(equipmentProfile.getDescription());

		mashEfficiency.addChangeListener(this);
		mashTunVolume.addChangeListener(this);
		mashTunWeight.addChangeListener(this);
		mashTunSpecificHeat.addChangeListener(this);
		boilKettleVolume.addChangeListener(this);
		boilEvapourationRate.addChangeListener(this);
		hopUtilisation.addChangeListener(this);
		fermenterVolume.addChangeListener(this);
		lauterLoss.addChangeListener(this);
		trubAndChillerLoss.addChangeListener(this);
		description.addKeyListener(this);
	}

	@Override
	public void commit(String name)
	{
		EquipmentProfile current = Database.getInstance().getEquipmentProfiles().get(name);

		current.setConversionEfficiency(new PercentageUnit((Double)mashEfficiency.getValue()));
		current.setMashTunVolume(new VolumeUnit((Double)mashTunVolume.getValue(), Quantity.Unit.LITRES));
		current.setMashTunWeight(new WeightUnit((Double)mashTunWeight.getValue(), Quantity.Unit.KILOGRAMS));
		current.setMashTunSpecificHeat(new ArbitraryPhysicalQuantity((Double)mashTunSpecificHeat.getValue(), Quantity.Unit.JOULE_PER_KG_CELSIUS));
		current.setBoilKettleVolume(new VolumeUnit((Double)boilKettleVolume.getValue() *1000D));
		current.setBoilEvapourationRate(new PercentageUnit((Double)boilEvapourationRate.getValue()));
		current.setHopUtilisation((new PercentageUnit((Double)hopUtilisation.getValue())));
		current.setFermenterVolume(new VolumeUnit((Double)fermenterVolume.getValue(), Quantity.Unit.LITRES));
		current.setLauterLoss(new VolumeUnit((Double)lauterLoss.getValue(), Quantity.Unit.LITRES));
		current.setTrubAndChillerLoss(new VolumeUnit((Double)trubAndChillerLoss.getValue(), Quantity.Unit.LITRES));
		current.setDescription(description.getText());
	}

	@Override
	public Collection<String> loadData()
	{
		List<String> result = new ArrayList<>(
			Database.getInstance().getEquipmentProfiles().keySet());

		Collections.sort(result);

		return result;
	}

	@Override
	public void newItem(String name)
	{
		EquipmentProfile equipmentProfile = new EquipmentProfile(
			name,
			"",
			0.7D,
			20000D,
			2000D,
			0.3D,
			30000D,
			0.4D,
			10D,
			1D,
			25000D,
			2000D,
			2000D);

		Database.getInstance().getEquipmentProfiles().put(name, equipmentProfile);
	}

	@Override
	public void renameItem(String newName)
	{
		EquipmentProfile current = Database.getInstance().getEquipmentProfiles().remove(currentName);
		current.setName(newName);
		Database.getInstance().getEquipmentProfiles().put(newName, current);
	}

	@Override
	public void copyItem(String newName)
	{
		EquipmentProfile current = Database.getInstance().getEquipmentProfiles().get(currentName);

		EquipmentProfile equipmentProfile = new EquipmentProfile(current);
		equipmentProfile.setName(newName);

		Database.getInstance().getEquipmentProfiles().put(newName, equipmentProfile);
	}

	@Override
	public void deleteItem()
	{
		Database.getInstance().getEquipmentProfiles().remove(currentName);
	}

	@Override
	public boolean hasItem(String name)
	{
		return Database.getInstance().getEquipmentProfiles().containsKey(name);
	}
}
