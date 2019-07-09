package mclachlan.brewday.ui.swing;

import java.awt.Container;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.equipment.EquipmentProfile;
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
		result.add(new JLabel("Mash Efficiency (%):"));
		result.add(mashEfficiency, "wrap");

		mashTunVolume = new JSpinner(new SpinnerNumberModel(20D, 0.1D, 1000D, 0.1D));
		mashTunVolume.addChangeListener(this);
		result.add(new JLabel("Mash Tun Volume (l):"));
		result.add(mashTunVolume, "wrap");

		mashTunWeight = new JSpinner(new SpinnerNumberModel(1D, 0.1D, 1000D, 0.1D));
		mashTunWeight.addChangeListener(this);
		result.add(new JLabel("Mash Tun Weight (kg):"));
		result.add(mashTunWeight, "wrap");

		mashTunSpecificHeat = new JSpinner(new SpinnerNumberModel(0.2D, 0.01D, 1D, 0.01D));
		mashTunSpecificHeat .addChangeListener(this);
		result.add(new JLabel("Mash Tun Specific Heat:"));
		result.add(mashTunSpecificHeat, "wrap");

		boilKettleVolume = new JSpinner(new SpinnerNumberModel(20D, 0.1D, 1000D, 0.1D));
		boilKettleVolume.addChangeListener(this);
		result.add(new JLabel("Boil Kettle Volume:"));
		result.add(boilKettleVolume, "wrap");

		boilEvapourationRate = new JSpinner(new SpinnerNumberModel(4D, 0.1D, 100D, 0.1D));
		boilEvapourationRate.addChangeListener(this);
		result.add(new JLabel("Evapouration (%):"));
		result.add(boilEvapourationRate, "wrap");

		hopUtilisation = new JSpinner(new SpinnerNumberModel(100D, 0.1D, 100D, 0.1D));
		hopUtilisation.addChangeListener(this);
		result.add(new JLabel("Hop Utilisation:"));
		result.add(hopUtilisation, "wrap");

		fermenterVolume = new JSpinner(new SpinnerNumberModel(20D, 0.1D, 1000D, 0.1D));
		fermenterVolume.addChangeListener(this);
		result.add(new JLabel("Fermenter Volume:"));
		result.add(fermenterVolume, "wrap");

		lauterLoss = new JSpinner(new SpinnerNumberModel(2D, 0.1D, 1000D, 0.1D));
		lauterLoss.addChangeListener(this);
		result.add(new JLabel("Lauter Loss (l):"));
		result.add(lauterLoss, "wrap");

		trubAndChillerLoss = new JSpinner(new SpinnerNumberModel(2D, 0.1D, 1000D, 0.1D));
		trubAndChillerLoss.addChangeListener(this);
		result.add(new JLabel("Trub & Chiller Loss (l):"));
		result.add(trubAndChillerLoss, "wrap");

		description = new JTextArea(8, 30);
		description.setWrapStyleWord(true);
		description.setLineWrap(true);
		description.addKeyListener(this);
		result.add(new JLabel("Description:"));
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

		mashEfficiency.setValue(equipmentProfile.getMashEfficiency() *100);
		mashTunVolume.setValue(equipmentProfile.getMashTunVolume() /1000);
		mashTunWeight.setValue(equipmentProfile.getMashTunWeight() /1000);
		mashTunSpecificHeat.setValue(equipmentProfile.getMashTunSpecificHeat());
		boilKettleVolume.setValue(equipmentProfile.getBoilKettleVolume() /1000);
		boilEvapourationRate.setValue(equipmentProfile.getBoilEvapourationRate() *100);
		hopUtilisation.setValue(equipmentProfile.getHopUtilisation() *100);
		fermenterVolume.setValue(equipmentProfile.getFermenterVolume() /1000);
		lauterLoss.setValue(equipmentProfile.getLauterLoss() /1000);
		trubAndChillerLoss.setValue(equipmentProfile.getTrubAndChillerLoss() /1000);
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

		current.setMashEfficiency((Double)mashEfficiency.getValue() /100D);
		current.setMashTunVolume((Double)mashTunVolume.getValue() *1000D);
		current.setMashTunWeight((Double)mashTunWeight.getValue() *1000D);
		current.setMashTunSpecificHeat((Double)mashTunSpecificHeat.getValue());
		current.setBoilKettleVolume((Double)boilKettleVolume.getValue() *1000D);
		current.setBoilEvapourationRate((Double)boilEvapourationRate.getValue() /100D);
		current.setHopUtilisation((Double)hopUtilisation.getValue() /100D);
		current.setFermenterVolume((Double)fermenterVolume.getValue() *1000D);
		current.setLauterLoss((Double)lauterLoss.getValue() *1000D);
		current.setTrubAndChillerLoss((Double)trubAndChillerLoss.getValue() *1000D);
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

		EquipmentProfile equipmentProfile = new EquipmentProfile(
			newName,
			current.getDescription(),
			current.getMashEfficiency(),
			current.getMashTunVolume(),
			current.getMashTunWeight(),
			current.getMashTunSpecificHeat(),
			current.getBoilKettleVolume(),
			current.getBoilEvapourationRate(),
			current.getHopUtilisation(),
			current.getFermenterVolume(),
			current.getLauterLoss(),
			current.getTrubAndChillerLoss());

		Database.getInstance().getEquipmentProfiles().put(newName, equipmentProfile);
	}

	@Override
	public void deleteItem()
	{
		Database.getInstance().getEquipmentProfiles().remove(currentName);
	}
}
