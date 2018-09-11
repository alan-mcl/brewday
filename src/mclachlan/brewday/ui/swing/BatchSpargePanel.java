package mclachlan.brewday.ui.swing;

import java.awt.GridBagConstraints;
import javax.swing.*;
import mclachlan.brewday.process.Batch;
import mclachlan.brewday.process.BatchSparge;
import mclachlan.brewday.process.ProcessStep;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class BatchSpargePanel extends ProcessStepPanel
{
	protected JComboBox<String> spargeWaterVolume;

	public BatchSpargePanel(boolean addMode)
	{
		super(addMode);
	}

	@Override
	protected void buildStepInternal(GridBagConstraints gbc, boolean addMode)
	{
		spargeWaterVolume = new JComboBox<String>();
		spargeWaterVolume.addActionListener(this);
		spargeWaterVolume.setEditable(addMode);
		dodgyGridBagShite(this, new JLabel("Sparge Water Volume:"), spargeWaterVolume, gbc);
	}

	@Override
	protected void refreshInternal(ProcessStep step, Batch batch)
	{
		spargeWaterVolume.setModel(getVolumesOptions(batch));

		spargeWaterVolume.setSelectedItem(((BatchSparge)step).getSpargeWaterVolume());
	}
}
