package mclachlan.brewday.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import mclachlan.brewday.process.Batch;
import mclachlan.brewday.process.ProcessStep;

import static mclachlan.brewday.ui.swing.EditorPanel.dodgyGridBagShite;

/**
 *
 */
public class ProcessStepPanel extends JPanel implements ActionListener
{
	protected JTextField name;
	protected JTextArea desc;
	protected JComboBox<String> inputVolume;
	protected JComboBox<String> outputVolume;

	public ProcessStepPanel(boolean addMode)
	{
		super(new GridBagLayout());
		outputVolume = new JComboBox<String>();
		inputVolume = new JComboBox<String>();
		name = new JTextField(20);
		desc = new JTextArea(4, 30);

		GridBagConstraints gbc = EditorPanel.createGridBagConstraints();

		name.addActionListener(this);
		dodgyGridBagShite(this, new JLabel("Name:"), name, gbc);

		inputVolume.addActionListener(this);
		inputVolume.setEditable(addMode);
		dodgyGridBagShite(this, new JLabel("Input Volume:"), inputVolume, gbc);

		buildStepInternal(gbc, addMode);

		outputVolume.addActionListener(this);
		outputVolume.setEditable(addMode);
		dodgyGridBagShite(this, new JLabel("Output Volume:"), outputVolume, gbc);

		dodgyGridBagShite(this, new JLabel("Description:"), new JLabel(), gbc);
		desc.setLineWrap(true);
		desc.setWrapStyleWord(true);
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.gridx = 0;
		gbc.gridy++;
		add(desc, gbc);
	}

	protected void buildStepInternal(GridBagConstraints gbc, boolean addMode)
	{

	}

	public void refresh(ProcessStep step, Batch batch)
	{
		inputVolume.setModel(getVolumesOptions(batch));
		outputVolume.setModel(getVolumesOptions(batch));

		name.setText(step.getName());
		inputVolume.setSelectedItem(step.getInputVolume());
		outputVolume.setSelectedItem(step.getOutputVolume());
		desc.setText(step.getDescription());

		refreshInternal(step, batch);
	}

	protected void refreshInternal(ProcessStep step, Batch batch)
	{

	}

	protected DefaultComboBoxModel<String> getVolumesOptions(Batch batch)
	{
		Vector<String> vec = new Vector<String>(batch.getVolumes().getVolumes().keySet());
		Collections.sort(vec);
		vec.add(0, EditorPanel.NONE);
		return new DefaultComboBoxModel<String>(vec);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{

	}
}
